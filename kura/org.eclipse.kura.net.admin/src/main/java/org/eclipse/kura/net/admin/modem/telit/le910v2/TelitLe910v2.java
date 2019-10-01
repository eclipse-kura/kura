/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.telit.le910v2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910AtCommands;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelitLe910v2 extends TelitHe910 implements HspaCellularModem {

	private static final Logger logger = LoggerFactory.getLogger(TelitLe910v2.class);
	private boolean initialized;
	private boolean diversityEnabled;

	public TelitLe910v2(ModemDevice device, String platform, ConnectionFactory connectionFactory) {
		super(device, platform, connectionFactory);
	}

	@Override
	public void enableGps() throws KuraException {
		throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}

	@Override
	public void disableGps() throws KuraException {
		throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}

	@Override
	public boolean isGpsEnabled() {
		return false;
	}

	@Override
	public boolean isGpsSupported() throws KuraException {
		// GPS devices attached to the modem are not yet supported
		return false;
	}

	@Override
	public void setConfiguration(List<NetConfig> netConfigs) {
		super.setConfiguration(netConfigs);
		this.initialized = false;
		this.diversityEnabled = true;
	}

	@Override
	public boolean isSimCardReady() throws KuraException {
		synchronized (this.atLock) {
			CommConnection commAtConnection = null;
			try {
				String port = getUnusedAtPort();
				logger.debug("sendCommand getSimStatus :: {} command to port {}",
						TelitHe910AtCommands.GET_SIM_STATUS.getCommand(), port);

				commAtConnection = openSerialPort(port);
				if (!isAtReachable(commAtConnection)) {
					throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
				}

				// TODO: we need an explicit initialization method.
				if (!this.initialized) {
					initialize(commAtConnection);
				}
			} finally {
				closeSerialPort(commAtConnection);
			}
		}
		return this.initialized;
	}

	protected void initialize(CommConnection comm) throws KuraException {
		try {
			boolean simReady = isSimCardReady(comm);
			if (!simReady) {
				simReady = simultateInsertSimCard(comm);
			}
			if (simReady) {
				// SIM must be ready before configuring PDP context
				configurePdpContext(comm);
				this.initialized = true;
			}
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
		}
	}

	// This is tricky.
	// In an LTE network, context #1 is special as it is automatically activated on
	// network registration
	// and it is assigned the "default EPS bearer".
	// Usually it's not recommended to update this context unless explicitly
	// required by the mobile network
	// operator.
	//
	// Setting the context is usually done by the PPP application, for example by
	// the 'pppd' dialer program
	// 'chat'.
	// With LE910-EU1 fw v.20.00.412 this is not recommended by Telit as the modem
	// will attempt to update
	// the context
	// on the network when
	// PPP starts. It has been observed that this may often lead to an unresponsive
	// modem.
	// According to Telit, this behavior will change in LE910-EU1 fw v.20.00.413
	// where setting the context
	// will take effect only on the next network registration.
	//
	// The current fix tries to set the context only when the ModemConfig does not
	// match the modem NVM
	// where the context is saved and used automatically by modem on network
	// registration, for example
	// after a modem reset or a system reboot.
	// There's a defect however: the authentication password cannot be read back
	// from the NVM, so
	// only the APN is used for the check.
	// In practice, with context #1, to make sure the NVM is in sync with the
	// ModemConfig, the APN must be
	// changed twice.
	// This may still lead to an unresponsive modem but it will be recovered by the
	// ESF modem monitor.
	protected void configurePdpContext(CommConnection comm) throws KuraException {
		try {
			String setPdpContextAtCommand = formSetPdpContextAtCommand();
			String setPdpAuthAtCommand = formSetPdpAuthAtCommand();
			String testPdpAuthAtCommand = formTestPdpAuthAtCommand();

			if (setPdpContextAtCommand == null || setPdpAuthAtCommand == null) {
				throw new IllegalStateException("Null PDP context or authentication");
			}

			List<ModemPdpContext> contexts = getPdpContextInfo(comm);
			ModemPdpContext configContext = getConfigPdpContext();
			boolean changed = contexts.stream().noneMatch(t -> t.getNumber() == configContext.getNumber()
					&& t.getType().equals(configContext.getType()) && t.getApn().equals(configContext.getApn()));

			if (configContext.getNumber() != 1 || changed) {
				// Authentication command is not always supported (e.g. LE910-V2 NA with AT&T
				// firmware)
				if (execCommand(comm, testPdpAuthAtCommand.getBytes(StandardCharsets.US_ASCII), 1000)
						&& !execCommand(comm, setPdpAuthAtCommand.getBytes(StandardCharsets.US_ASCII), 1000)) {
					throw new KuraException(KuraErrorCode.CONNECTION_FAILED, "Set PDP authentication command failed");
				}
				if (!execCommand(comm, setPdpContextAtCommand.getBytes(StandardCharsets.US_ASCII), 1000)) {
					throw new KuraException(KuraErrorCode.CONNECTION_FAILED, "Set PDP context command failed");
				}
				if (configContext.getNumber() == 1) {
					String message = "FIXME: context #1 has been modified and will be updated on the network when PPP starts (e.g. LE910-EU1 fw v.20.00.412). This may lead to an unreasponsive modem";
					logger.warn(message);
				}
			}
		} catch (IOException | IllegalStateException e) {
			throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
		}
	}

	protected String formTestPdpAuthAtCommand() {
		StringBuilder sb = new StringBuilder(TelitLe910v2AtCommands.PDP_AUTH.getCommand()).append("=?\r\n");
		return sb.toString();
	}

	protected String formSetPdpAuthAtCommand() {
		String result = null;
		List<NetConfig> configs = getConfiguration();
		for (NetConfig config : configs) {
			if (config instanceof ModemConfig) {
				ModemConfig modemConfig = (ModemConfig) config;
				AuthType authType = modemConfig.getAuthType();

				int auth;
				switch (authType) {
				case NONE:
					auth = 0;
					break;
				case PAP:
					auth = 1;
					break;
				case CHAP:
					auth = 2;
					break;
				default:
					logger.warn("{} Authentication not supported. Falling back to PAP", authType);
					auth = 1;
					break;
				}

				String username = modemConfig.getUsername();
				char[] password = modemConfig.getPasswordAsPassword().getPassword();
				// a little bit silly
				String cid = parseContextIdFromDialString(modemConfig.getDialString());

				StringBuilder sb = new StringBuilder(TelitLe910v2AtCommands.PDP_AUTH.getCommand()).append("=")
						.append(cid).append(",").append(auth).append(",").append('"').append(username).append('"')
						.append(",").append('"').append(password).append('"').append("\r\n");
				result = sb.toString();
				break;
			}
		}
		return result;
	}

	@Override
	public boolean hasDiversityAntenna() {
		return true;
	}

	@Override
	public boolean isDiversityEnabled() {
		return this.diversityEnabled;
	}

	public void setDiversityEnabled(boolean diversityEnabled) {
		this.diversityEnabled = diversityEnabled;
	}

    @Override
    public void enableDiversity() throws KuraException {
        programDiversity(true);
    }

    @Override
    public void disableDiversity() throws KuraException {
        programDiversity(false);
    }
	
    private void programDiversity(boolean enabled) throws KuraException {
        synchronized (this.atLock) {
            CommConnection commAtConnection = null;
            try {
                String port = getUnusedAtPort();
                if (enabled) {
                    logger.info("sendCommand enable CELL Diversity antenna :: {} command to port {}",
                            TelitLe910v2AtCommands.ENABLE_CELL_DIV.getCommand(), port);
                } else {
                    logger.info("sendCommand disable CELL Diversity antenna :: {} command to port {}",
                            TelitLe910v2AtCommands.DISABLE_CELL_DIV.getCommand(), port);
                }

                commAtConnection = openSerialPort(port);
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }

                byte[] command;
                if (enabled) {
                    command = TelitLe910v2AtCommands.ENABLE_CELL_DIV.getCommand().getBytes(StandardCharsets.US_ASCII);
                } else {
                    command = TelitLe910v2AtCommands.DISABLE_CELL_DIV.getCommand().getBytes(StandardCharsets.US_ASCII);
                }
                byte[] reply = commAtConnection.sendCommand(command, 1000, 100);
                if (reply != null) {
                    String resp = new String(reply);
                    if (resp.contains("OK")) {
                        if (enabled) {
                            logger.info("CELL DIV successfully enabled");
                            this.setDiversityEnabled(true);
                        } else {
                            logger.info("CELL DIV successfully disabled");
                            this.setDiversityEnabled(false);
                        }
                    } else
                        logger.info("Command returns : {}", resp);
                } else {
                    logger.error("No answer");
                }
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }
        }
    }
}
