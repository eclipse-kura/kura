/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.telit.he910;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.generic.TelitModem;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemPdpContext;
import org.eclipse.kura.net.modem.ModemPdpContextType;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines Telit HE910 modem
 */
public class TelitHe910 extends TelitModem implements HspaCellularModem {

	private static final Logger logger = LoggerFactory.getLogger(TelitHe910.class);

	/**
	 * TelitHe910 modem constructor
	 *
	 * @param usbDevice         - modem USB device as {@link UsbModemDevice}
	 * @param platform          - hardware platform as {@link String}
	 * @param connectionFactory - connection factory {@link ConnectionFactory}
	 */
	public TelitHe910(ModemDevice device, String platform, ConnectionFactory connectionFactory) {

		super(device, platform, connectionFactory);

		try {
			String atPort = getAtPort();
			String gpsPort = getGpsPort();
			if (atPort != null && (atPort.equals(getDataPort()) || atPort.equals(gpsPort))) {
				this.serialNumber = getSerialNumber();
				this.imsi = getMobileSubscriberIdentity();
				this.iccid = getIntegratedCirquitCardId();
                this.model = getModel();
				this.manufacturer = getManufacturer();
				this.revisionId = getRevisionID();
				this.gpsSupported = isGpsSupported();
				this.rssi = getSignalStrength();

				logger.trace("{} :: Serial Number={}", getClass().getName(), this.serialNumber);
				logger.trace("{} :: IMSI={}", getClass().getName(), this.imsi);
				logger.trace("{} :: ICCID={}", getClass().getName(), this.iccid);
				logger.trace("{} :: Model={}", getClass().getName(), this.model);
				logger.trace("{} :: Manufacturer={}", getClass().getName(), this.manufacturer);
				logger.trace("{} :: Revision ID={}", getClass().getName(), this.revisionId);
				logger.trace("{} :: GPS Supported={}", getClass().getName(), this.gpsSupported);
				logger.trace("{} :: RSSI={}", getClass().getName(), this.rssi);
			}
		} catch (KuraException e) {
			logger.error("Failed to initialize TelitHe910", e);
		}
	}

	@Override
	public boolean isTelitSimCardReady() throws KuraException {
		boolean simReady = false;
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

				simReady = isSimCardReady(commAtConnection);
				if (!simReady) {
					simReady = simultateInsertSimCard(commAtConnection);
				}
			} catch (IOException e) {
				throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
			} finally {
				closeSerialPort(commAtConnection);
			}
		}
		return simReady;
	}

	@Override
	public boolean isSimCardReady() throws KuraException {
		return isTelitSimCardReady();
	}

	@Override
	public ModemRegistrationStatus getRegistrationStatus() throws KuraException {

		ModemRegistrationStatus modemRegistrationStatus = ModemRegistrationStatus.UNKNOWN;
		synchronized (this.atLock) {
			logger.debug("sendCommand getRegistrationStatus :: {}",
					TelitHe910AtCommands.GET_REGISTRATION_STATUS.getCommand());
			byte[] reply;
			CommConnection commAtConnection = openSerialPort(getAtPort());
			if (!isAtReachable(commAtConnection)) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
			}
			try {
				reply = commAtConnection.sendCommand(
						TelitHe910AtCommands.GET_REGISTRATION_STATUS.getCommand().getBytes(StandardCharsets.US_ASCII),
						1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
			}
			if (reply != null) {
				String sRegStatus = getResponseString(reply);
				String[] regStatusSplit = sRegStatus.split(",");
				if (regStatusSplit.length >= 2) {
					int status = Integer.parseInt(regStatusSplit[1]);
					switch (status) {
					case 0:
						modemRegistrationStatus = ModemRegistrationStatus.NOT_REGISTERED;
						break;
					case 1:
						modemRegistrationStatus = ModemRegistrationStatus.REGISTERED_HOME;
						break;
					case 3:
						modemRegistrationStatus = ModemRegistrationStatus.REGISTRATION_DENIED;
						break;
					case 5:
						modemRegistrationStatus = ModemRegistrationStatus.REGISTERED_ROAMING;
						break;
					default:
						break;
					}
				}
			}
		}
		return modemRegistrationStatus;
	}

	@Override
	public long getCallTxCounter() throws KuraException {

		long txCnt = 0;
		synchronized (this.atLock) {
			logger.debug("sendCommand getGprsSessionDataVolume :: {}",
					TelitHe910AtCommands.GET_GPRS_SESSION_DATA_VOLUME.getCommand());
			byte[] reply;
			CommConnection commAtConnection = openSerialPort(getAtPort());
			if (!isAtReachable(commAtConnection)) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
			}
			try {
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.GET_GPRS_SESSION_DATA_VOLUME.getCommand()
						.getBytes(StandardCharsets.US_ASCII), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String[] splitPdp;
				String[] splitData;
				String sDataVolume = this.getResponseString(reply);
				splitPdp = sDataVolume.split("#GDATAVOL:");
				if (splitPdp.length > 1) {
					for (String pdp : splitPdp) {
						if (pdp.trim().length() > 0) {
							splitData = pdp.trim().split(",");
							if (splitData.length >= 4) {
								int pdpNo = Integer.parseInt(splitData[0]);
								if (pdpNo == Integer.valueOf(getContextId())) {
									txCnt = Integer.parseInt(splitData[2]);
								}
							}
						}
					}
				}
			}
		}
		return txCnt;
	}

	@Override
	public long getCallRxCounter() throws KuraException {
		long rxCnt = 0;
		synchronized (this.atLock) {
			logger.debug("sendCommand getGprsSessionDataVolume :: {}",
					TelitHe910AtCommands.GET_GPRS_SESSION_DATA_VOLUME.getCommand());
			byte[] reply;
			CommConnection commAtConnection = openSerialPort(getAtPort());
			if (!isAtReachable(commAtConnection)) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
			}
			try {
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.GET_GPRS_SESSION_DATA_VOLUME.getCommand()
						.getBytes(StandardCharsets.US_ASCII), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String[] splitPdp;
				String[] splitData;
				String sDataVolume = this.getResponseString(reply);
				splitPdp = sDataVolume.split("#GDATAVOL:");
				if (splitPdp.length > 1) {
					for (String pdp : splitPdp) {
						if (pdp.trim().length() > 0) {
							splitData = pdp.trim().split(",");
							if (splitData.length >= 4) {
								int pdpNo = Integer.parseInt(splitData[0]);
								if (pdpNo == Integer.valueOf(getContextId())) {
									rxCnt = Integer.parseInt(splitData[3]);
								}
							}
						}
					}
				}
			}
		}
		return rxCnt;
	}

	@Override
	public List<ModemPdpContext> getPdpContextInfo() throws KuraException {
		synchronized (this.atLock) {
			CommConnection commAtConnection = openSerialPort(getAtPort());
			try {
				return getPdpContextInfo(commAtConnection);
			} finally {
				closeSerialPort(commAtConnection);
			}
		}
	}

	@Override
	public String getServiceType() throws KuraException {
		String serviceType = null;
		synchronized (this.atLock) {
			logger.debug("sendCommand getMobileStationClass :: {}",
					TelitHe910AtCommands.GET_MOBILE_STATION_CLASS.getCommand());
			byte[] reply;
			CommConnection commAtConnection = openSerialPort(getAtPort());
			if (!isAtReachable(commAtConnection)) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
			}
			try {
				reply = commAtConnection.sendCommand(
						TelitHe910AtCommands.GET_MOBILE_STATION_CLASS.getCommand().getBytes(StandardCharsets.US_ASCII),
						1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String sCgclass = this.getResponseString(reply);
				if (sCgclass.startsWith("+CGCLASS:")) {
					sCgclass = sCgclass.substring("+CGCLASS:".length()).trim();
					if ("\"A\"".equals(sCgclass)) {
						serviceType = "UMTS";
					} else if ("\"B\"".equals(sCgclass)) {
						serviceType = "GSM/GPRS";
					} else if ("\"CG\"".equals(sCgclass)) {
						serviceType = "GPRS";
					} else if ("\"CC\"".equals(sCgclass)) {
						serviceType = "GSM";
					}
				}
			}
		}

		return serviceType;
	}

	@Override
	public List<ModemTechnologyType> getTechnologyTypes() throws KuraException {

		List<ModemTechnologyType> modemTechnologyTypes;
		ModemDevice device = getModemDevice();
		if (device == null) {
			throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "No modem device");
		}
		if (device instanceof UsbModemDevice) {
			SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) device);
			if (usbModemInfo != null) {
				modemTechnologyTypes = usbModemInfo.getTechnologyTypes();
			} else {
				throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "No usbModemInfo available");
			}
		} else {
			throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, "Unsupported modem device");
		}
		return modemTechnologyTypes;
	}

	protected String getUnusedAtPort() throws KuraException {
		String port;
		if (isGpsEnabled() && getAtPort().equals(getGpsPort()) && !getAtPort().equals(getDataPort())) {
			port = getDataPort();
		} else {
			port = getAtPort();
		}
		return port;
	}

	protected List<ModemPdpContext> getPdpContextInfo(CommConnection comm) throws KuraException {
		List<ModemPdpContext> pdpContextInfo = new ArrayList<>();
		byte[] reply;
		if (!isAtReachable(comm)) {
			closeSerialPort(comm);
			throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
		}
		try {
			reply = comm.sendCommand(formGetPdpContextAtCommand().getBytes(StandardCharsets.US_ASCII), 1000, 100);
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
		}
		if (reply != null) {
			String sreply = this.getResponseString(reply);
			Scanner scanner = new Scanner(sreply);
			while (scanner.hasNextLine()) {
				String[] tokens = scanner.nextLine().split(",");
				if (!tokens[0].startsWith("+CGDCONT:")) {
					continue;
				}
				int contextNo = Integer.parseInt(tokens[0].substring("+CGDCONT:".length()).trim());
				ModemPdpContextType pdpType = ModemPdpContextType
						.getContextType(tokens[1].substring(1, tokens[1].length() - 1));
				String apn = tokens[2].substring(1, tokens[2].length() - 1);
				ModemPdpContext modemPdpContext = new ModemPdpContext(contextNo, pdpType, apn);
				pdpContextInfo.add(modemPdpContext);
			}
			scanner.close();
		}
		return pdpContextInfo;
	}

	protected boolean isSimCardReady(CommConnection comm) throws KuraException, IOException {
		boolean simReady = false;
		byte[] reply = comm.sendCommand(
				TelitHe910AtCommands.GET_SIM_STATUS.getCommand().getBytes(StandardCharsets.US_ASCII), 1000, 100);
		if (reply != null) {
			String simStatus = getResponseString(reply);
			String[] simStatusSplit = simStatus.split(",");
			if (simStatusSplit.length > 1 && Integer.valueOf(simStatusSplit[1]) > 0) {
				simReady = true;
			}
		}
		return simReady;
	}

	protected boolean simultateInsertSimCard(CommConnection comm) throws KuraException, IOException {
		boolean simReady = false;
		byte[] reply = comm.sendCommand(
				TelitHe910AtCommands.SIMULATE_SIM_NOT_INSERTED.getCommand().getBytes(StandardCharsets.US_ASCII), 1000,
				100);
		if (reply != null) {
			sleep(5000);
			reply = comm.sendCommand(
					TelitHe910AtCommands.SIMULATE_SIM_INSERTED.getCommand().getBytes(StandardCharsets.US_ASCII), 1000,
					100);
			if (reply != null) {
				sleep(1000);
				simReady = isSimCardReady(comm);
			}
		}
		return simReady;
	}

	protected boolean execCommand(CommConnection comm, byte[] command, int timeout) throws KuraException, IOException {
		boolean ok;
		byte[] reply = comm.sendCommand(command, timeout, 100);
		if (reply != null) {
			ok = new String(reply, StandardCharsets.US_ASCII).contains("OK");
		} else {
			throw new KuraException(KuraErrorCode.TIMED_OUT);
		}
		return ok;
	}

	protected String formSetPdpContextAtCommand() {
		String result = null;
		List<NetConfig> configs = getConfiguration();
		for (NetConfig config : configs) {
			if (config instanceof ModemConfig) {
				ModemConfig modemConfig = (ModemConfig) config;
				// a little bit silly
				String cid = parseContextIdFromDialString(modemConfig.getDialString());
				String apn = modemConfig.getApn();
				String pdpType = modemConfig.getPdpType().name();
				StringBuilder sb = new StringBuilder(TelitHe910AtCommands.PDP_CONTEXT.getCommand()).append("=")
						.append(cid).append(",").append('"').append(pdpType).append('"').append(",").append('"')
						.append(apn).append('"').append("\r\n");
				result = sb.toString();
				break;
			}
		}
		return result;
	}

	protected String parseContextIdFromDialString(String dialString) {
		int start = dialString.lastIndexOf('*');
		return dialString.substring(start + 1, dialString.length() - 1);
	}

	protected ModemPdpContext getConfigPdpContext() {
		ModemPdpContext result = null;
		List<NetConfig> configs = getConfiguration();
		for (NetConfig config : configs) {
			if (config instanceof ModemConfig) {
				ModemConfig modemConfig = (ModemConfig) config;
				// a little bit silly
				int cid = Integer.parseInt(parseContextIdFromDialString(modemConfig.getDialString()));
				String apn = modemConfig.getApn();
				PdpType pdpType = modemConfig.getPdpType();

				ModemPdpContextType modemPdpType;
				switch (pdpType) {
				case IP:
					modemPdpType = ModemPdpContextType.IP;
					break;
				case IPv6:
					modemPdpType = ModemPdpContextType.IPV6;
					break;
				case PPP:
					modemPdpType = ModemPdpContextType.PPP;
					break;
				default:
					modemPdpType = ModemPdpContextType.IPV4IPV6;
				}

				result = new ModemPdpContext(cid, modemPdpType, apn);
			}
		}
		return result;
	}

	protected String getContextId() {
		String cid = null;
		List<NetConfig> configs = getConfiguration();
		for (NetConfig config : configs) {
			if (config instanceof ModemConfig) {
				ModemConfig modemConfig = (ModemConfig) config;
				// a little bit silly
				cid = parseContextIdFromDialString(modemConfig.getDialString());
				break;
			}
		}
		return cid;
	}

	protected String formGetPdpContextAtCommand() {
		StringBuilder sb = new StringBuilder(TelitHe910AtCommands.PDP_CONTEXT.getCommand());
		sb.append("?\r\n");
		return sb.toString();
	}

	@Override
	public boolean hasDiversityAntenna() {
		return false;
	}

	@Override
	public boolean isDiversityEnabled() {
		return false;
	}

	@Override
	public void enableDiversity() throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}

	@Override
	public void disableDiversity() throws KuraException {
        throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}
}
