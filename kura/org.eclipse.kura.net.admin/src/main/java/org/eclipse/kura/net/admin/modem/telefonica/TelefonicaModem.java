/*******************************************************************************
 * Copyright (c) 2020, 2022 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.net.admin.modem.telefonica;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.net.admin.modem.hspa.HspaModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelefonicaModem extends HspaModem {

    private static final Logger logger = LoggerFactory.getLogger(TelefonicaModem.class);

    private boolean initialized;

    public TelefonicaModem(ModemDevice device, String platform, ConnectionFactory connectionFactory) {
        super(device, platform, connectionFactory);
        initModemParameters();
    }

    @Override
    public String getIntegratedCirquitCardId() throws KuraException {
        synchronized (this.atLock) {
            if (this.iccid == null && isSimCardReady()) {
                logger.debug("sendCommand getICCID :: {}", TelefonicaModemAtCommands.getICCID.getCommand());

                final byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }

                try {
                    reply = commAtConnection.sendCommand(TelefonicaModemAtCommands.getICCID.getCommand().getBytes(),
                            1000, 100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                } finally {
                    closeSerialPort(commAtConnection);
                }

                if (reply == null) {
                    throw new KuraException(KuraErrorCode.TIMED_OUT, TelefonicaModemAtCommands.getICCID.getCommand());
                }
                final String response = getResponseString(reply);
                if (response.startsWith("ICCID:")) {
                    this.iccid = response.substring("ICCID:".length()).trim();
                } else {
                    throw new KuraException(KuraErrorCode.BAD_REQUEST, response);
                }
            }
        }
        return this.iccid;
    }

    @Override
    public boolean isSimCardReady() throws KuraException {
        if (!this.initialized) {
            disableURC();
            this.initialized = true;
        }

        synchronized (this.atLock) {
            logger.debug("sendCommand getSimType :: {}", TelefonicaModemAtCommands.getSimType.getCommand());

            final byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }

            try {
                reply = commAtConnection.sendCommand(TelefonicaModemAtCommands.getSimType.getCommand().getBytes(), 1000,
                        100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }

            if (reply == null) {
                throw new KuraException(KuraErrorCode.TIMED_OUT, TelefonicaModemAtCommands.getSimType.getCommand());
            }
            final String response = this.getResponseString(reply);
            if (response.startsWith("^CARDMODE:")) {
                return !"0".equals(response.substring("^CARDMODE:".length()).trim());
            } else {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, response);
            }
        }
    }

    @Override
    public void reset() throws KuraException {
        UsbModemDriver modemDriver = getModemDriver();
        modemDriver.disable((UsbModemDevice) getModemDevice());
        modemDriver.enable((UsbModemDevice) getModemDevice());
    }

    @Override
    public String getFirmwareVersion() throws KuraException {
        return "N/A";
    }

    private void disableURC() throws KuraException {
        synchronized (this.atLock) {
            logger.debug("sendCommand disableURC :: {}", TelefonicaModemAtCommands.disableURC.getCommand());

            final byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            try {
                reply = commAtConnection.sendCommand(TelefonicaModemAtCommands.disableURC.getCommand().getBytes(), 1000,
                        100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }

            if (reply == null) {
                throw new KuraException(KuraErrorCode.TIMED_OUT, TelefonicaModemAtCommands.disableURC.getCommand());
            }
            final String response = new String(reply, StandardCharsets.US_ASCII);
            if (!response.contains("OK")) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, response);
            }
        }
    }
}
