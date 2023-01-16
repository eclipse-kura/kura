/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem.quectel.ex25;

import java.io.IOException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.net.admin.modem.quectel.generic.QuectelGeneric;
import org.eclipse.kura.net.admin.modem.quectel.generic.QuectelGenericAtCommands;
import org.eclipse.kura.net.modem.ModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuectelEX25 extends QuectelGeneric {

    private static final Logger logger = LoggerFactory.getLogger(QuectelEX25.class);

    public QuectelEX25(ModemDevice device, String platform, ConnectionFactory connectionFactory) {
        super(device, platform, connectionFactory);
    }

    @Override
    public boolean isGpsSupported() throws KuraException {
        return true;
    }

    @Override
    public boolean isGpsEnabled() {
        synchronized (this.atLock) {
            boolean isEnabled = false;
            CommConnection commAtConnection = null;
            try {
                commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                            MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + QuectelEX25.class.getName());
                }
                String reply;
                String isGpsEnabledCommand = QuectelGenericAtCommands.IS_GPS_ENABLED.getCommand();
                reply = sendCommand(commAtConnection, isGpsEnabledCommand);
                isEnabled = reply != null && reply.contains("+QGPS: 1");
            } catch (KuraException e) {
                logger.error("Failed to connect to modem", e);
            } finally {
                try {
                    if (commAtConnection != null) {
                        closeSerialPort(commAtConnection);
                    }
                } catch (KuraException e) {
                    logger.error("Failed to close CommConnection", e);
                }
            }
            return isEnabled;
        }
    }

    @Override
    public void enableGps() throws KuraException {
        if (!isGpsEnabled()) {
            synchronized (this.atLock) {
                CommConnection commAtConnection = getCommConnection();
                String reply;
                String gpsEnableCommand = QuectelGenericAtCommands.ENABLE_GPS.getCommand();
                reply = sendCommand(commAtConnection, gpsEnableCommand);
                if (logger.isInfoEnabled()) {
                    if (reply != null && reply.isEmpty()) {
                        logger.info("enableGps() :: Modem replied to the {} command with 'OK'",
                                gpsEnableCommand.replace("\r\n", ""));
                        logger.info("enableGps() :: !!! Modem GPS enabled !!!");
                    } else {
                        logger.info("enableGps() :: Modem replied to the {} command with '{}'",
                                gpsEnableCommand.replace("\r\n", ""), reply);
                        logger.info("enableGps() :: !!! Modem GPS NOT enabled !!!");
                    }
                }

                String gpsEnableNMEACommand = QuectelGenericAtCommands.ENABLE_NMEA_GPS.getCommand();
                reply = sendCommand(commAtConnection, gpsEnableNMEACommand);
                if (logger.isInfoEnabled()) {
                    if (reply != null && reply.isEmpty()) {
                        logger.info("enableGps() :: Modem replied to the {} command with 'OK'",
                                gpsEnableNMEACommand.replace("\r\n", ""));
                    } else {
                        logger.info("enableGps() :: Modem replied to the {} command with '{}'",
                                gpsEnableNMEACommand.replace("\r\n", ""), reply);
                    }
                }
                closeSerialPort(commAtConnection);
            }
        }
    }

    @Override
    public void disableGps() throws KuraException {
        if (isGpsEnabled()) {
            synchronized (this.atLock) {
                CommConnection commAtConnection = getCommConnection();
                String reply;
                String gpsDisableCommand = QuectelGenericAtCommands.DISABLE_GPS.getCommand();
                reply = sendCommand(commAtConnection, gpsDisableCommand);
                if (logger.isInfoEnabled()) {
                    if (reply != null && reply.isEmpty()) {
                        logger.info("disableGps() :: Modem replied to the {} command with 'OK'",
                                gpsDisableCommand.replace("\r\n", ""));
                        logger.info("disableGps() :: !!! Modem GPS disabled !!!");
                    } else {
                        logger.info("disableGps() :: Modem replied to the {} command with '{}'",
                                gpsDisableCommand.replace("\r\n", ""), reply);
                        logger.info("disableGps() :: !!! Modem GPS NOT disabled !!!");
                    }
                }
                String gpsDisableNMEACommand = QuectelGenericAtCommands.DISABLE_NMEA_GPS.getCommand();
                reply = sendCommand(commAtConnection, gpsDisableNMEACommand);
                if (logger.isInfoEnabled()) {
                    if (reply != null && reply.isEmpty()) {
                        logger.info("disableGps() :: Modem replied to the {} command with 'OK'",
                                gpsDisableNMEACommand.replace("\r\n", ""));
                    } else {
                        logger.info("disableGps() :: Modem replied to the {} command with '{}'",
                                gpsDisableNMEACommand.replace("\r\n", ""), reply);
                    }
                }
                closeSerialPort(commAtConnection);
            }
        }
    }

    private CommConnection getCommConnection() throws KuraException {
        CommConnection commAtConnection = openSerialPort(getAtPort());
        if (!isAtReachable(commAtConnection)) {
            closeSerialPort(commAtConnection);
            throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                    MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + QuectelEX25.class.getName());
        }
        return commAtConnection;
    }

    private String sendCommand(CommConnection commAtConnection, String command) throws KuraException {
        byte[] reply;
        String stringReply = "";
        int numAttempts = 3;
        while (numAttempts > 0) {
            try {
                logger.debug("sendCommand :: {}", command);
                reply = commAtConnection.sendCommand(command.getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, e);
            }

            if (reply != null && reply.length > 0) {
                stringReply = getResponseString(reply);
                if (stringReply != null && !stringReply.contains("ERROR")) {
                    return stringReply;
                }
            }
            numAttempts--;
            sleep(2000);
        }
        return stringReply;
    }
}
