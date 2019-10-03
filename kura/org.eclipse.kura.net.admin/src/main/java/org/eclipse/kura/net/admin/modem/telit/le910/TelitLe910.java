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

package org.eclipse.kura.net.admin.modem.telit.le910;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910AtCommands;
import org.eclipse.kura.net.admin.modem.telit.le910v2.TelitLe910v2AtCommands;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelitLe910 extends TelitHe910 implements HspaCellularModem {

    private static final Logger logger = LoggerFactory.getLogger(TelitLe910.class);

    private boolean diversityEnabled;

    public TelitLe910(ModemDevice device, String platform, ConnectionFactory connectionFactory) {
        super(device, platform, connectionFactory);
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

    @Override
    public boolean isSimCardReady() throws KuraException {

        boolean simReady = false;
        String port = null;

        if (isGpsEnabled() && getAtPort().equals(getGpsPort()) && !getAtPort().equals(getDataPort())) {
            port = getDataPort();
        } else {
            port = getAtPort();
        }

        synchronized (this.atLock) {
            logger.debug("sendCommand getSimStatus :: {} command to port {}",
                    TelitHe910AtCommands.GET_SIM_STATUS.getCommand(), port);
            byte[] reply = null;
            CommConnection commAtConnection = null;
            try {

                commAtConnection = openSerialPort(port);
                if (!isAtReachable(commAtConnection)) {
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }

                reply = commAtConnection.sendCommand(TelitHe910AtCommands.GET_SIM_STATUS.getCommand().getBytes(), 1000,
                        100);
                if (reply != null) {
                    String simStatus = getResponseString(reply);
                    String[] simStatusSplit = simStatus.split(",");
                    if (simStatusSplit.length > 1 && Integer.valueOf(simStatusSplit[1]) > 0) {
                        simReady = true;
                    }
                }
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } catch (KuraException e) {
                throw e;
            } finally {
                closeSerialPort(commAtConnection);
            }
        }
        return simReady;
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
