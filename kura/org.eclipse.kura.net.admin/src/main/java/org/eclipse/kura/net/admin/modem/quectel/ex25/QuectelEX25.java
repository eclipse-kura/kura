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
    public void enableGps() throws KuraException {
        synchronized (this.atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED,
                        MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG + QuectelEX25.class.getName());
            }
            String gpsEnableCommand = QuectelGenericAtCommands.ENABLE_GPS.getCommand();
            String gpsEnableNMEAcommand = QuectelGenericAtCommands.ENABLE_NMEA_GPS.getCommand();

            byte[] reply;
            int numAttempts = 3;
            while (numAttempts > 0) {
                getAtPort();
                getGpsPort();
                try {
                    logger.debug("enableGps() :: sendCommand gpsEnable :: {}", gpsEnableCommand);
                    commAtConnection.sendCommand(gpsEnableCommand.getBytes(), 1000, 100);

                    logger.debug("enableGps() :: sendCommand gpsEnableNMEA :: {}", gpsEnableNMEAcommand);
                    reply = commAtConnection.sendCommand(gpsEnableNMEAcommand.getBytes(), 3000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, e);
                }

                if (reply != null && reply.length > 0) {
                    String sReply = getResponseString(reply);
                    if (sReply != null && sReply.isEmpty()) {
                        logger.info("enableGps() :: Modem replied to the {} command with 'OK'", gpsEnableNMEAcommand);
                        logger.info("enableGps() :: !!! Modem GPS enabled !!!");
                        break;
                    }
                }
                numAttempts--;
                sleep(2000);
            }

            closeSerialPort(commAtConnection);
        }
    }

    @Override
    public void disableGps() throws KuraException {
        synchronized (this.atLock) {
            CommConnection commAtConnection = openSerialPort(getAtPort());
            try {
                // String atPort = getAtPort();
                // String gpsPort = getGpsPort();
                // if (atPort.equals(gpsPort) && !isAtReachable(commAtConnection)) {
                // int numAttempts = 3;
                // while (numAttempts > 0) {
                // logger.debug("disableGps() :: sendCommand escapeSequence {}",
                // TelitModemAtCommands.escapeSequence.getCommand());
                //
                // sleep(1000); // do not send anything for 1 second before the escape sequence
                // byte[] reply = commAtConnection
                // .sendCommand(TelitModemAtCommands.escapeSequence.getCommand().getBytes(), 1000, 1100);
                //
                // if (reply != null && reply.length > 0) {
                // String sReply = new String(reply);
                // if (sReply.contains("NO CARRIER")) {
                // logger.info(
                // "disableGps() :: Modem replied with 'NO CARRIER' to the +++ escape sequence");
                // sleep(2000);
                // if (isAtReachable(commAtConnection)) {
                // logger.info("disableGps() :: !!! Modem GPS disabled !!!, OK");
                // break;
                // } else {
                // logger.error("disableGps() :: [1] Failed to disable modem GPS");
                // numAttempts--;
                // }
                // } else {
                // if (isAtReachable(commAtConnection)) {
                // logger.warn("disableGps() :: Modem didn't reply with 'NO CARRIER' "
                // + "to the +++ escape sequence but port is AT reachable");
                // logger.info("disableGps() :: Will assume that GPS is disabled");
                // break;
                // } else {
                // logger.error("disableGps() :: [2] Failed to disable modem GPS");
                // numAttempts--;
                // }
                // }
                // } else {
                // logger.error("disableGps() :: [3] Failed to disable modem GPS");
                // numAttempts--;
                // }
                // sleep(2000);
                // }
                // } else {
                // logger.warn("disableGps() :: Modem GPS has already been disabled");
                // }

                logger.debug("disableGps() :: sendCommand gpsPowerDown :: {}",
                        QuectelGenericAtCommands.DISABLE_GPS.getCommand());
                commAtConnection.sendCommand(QuectelGenericAtCommands.DISABLE_GPS.getCommand().getBytes(), 1000, 100);

            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.UNAVAILABLE_DEVICE, e);
            }
            closeSerialPort(commAtConnection);
        }
    }
}
