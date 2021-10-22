/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.net.admin.modem.huawei;

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

public class HuaweiModem extends HspaModem {

    private static final Logger logger = LoggerFactory.getLogger(HuaweiModem.class);

    private boolean initialized;

    public HuaweiModem(ModemDevice device, String platform, ConnectionFactory connectionFactory) {
        super(device, platform, connectionFactory);
    }

    @Override
    public String getIntegratedCirquitCardId() throws KuraException {
        synchronized (this.atLock) {
            if (this.iccid == null && isSimCardReady()) {
                logger.debug("sendCommand getICCID :: {}", HuaweiModemAtCommands.GET_ICCID.getCommand());

                final byte[] reply;
                CommConnection commAtConnection = openSerialPort(getAtPort());
                if (!isAtReachable(commAtConnection)) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
                }

                try {
                    reply = commAtConnection.sendCommand(HuaweiModemAtCommands.GET_ICCID.getCommand().getBytes(), 1000,
                            100);
                } catch (IOException e) {
                    closeSerialPort(commAtConnection);
                    throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
                } finally {
                    closeSerialPort(commAtConnection);
                }

                if (reply == null) {
                    throw new KuraException(KuraErrorCode.TIMED_OUT, HuaweiModemAtCommands.GET_ICCID.getCommand());
                }
                final String response = getResponseString(reply);
                if (response.startsWith("^ICCID:")) {
                    this.iccid = response.substring("^ICCID:".length()).trim();
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
            logger.debug("sendCommand getSimType :: {}", HuaweiModemAtCommands.GET_SIM_TYPE.getCommand());

            final byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }

            try {
                reply = commAtConnection.sendCommand(HuaweiModemAtCommands.GET_SIM_TYPE.getCommand().getBytes(), 1000,
                        100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }

            if (reply == null) {
                throw new KuraException(KuraErrorCode.TIMED_OUT, HuaweiModemAtCommands.GET_SIM_TYPE.getCommand());
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
        while (true) {
            sleep(5000);
            try {
                UsbModemDriver modemDriver = getModemDriver();
                modemDriver.disable((UsbModemDevice) getModemDevice());
                sleep(1000);
                modemDriver.enable((UsbModemDevice) getModemDevice());
                logger.info("reset() :: modem reset successful");
                break;
            } catch (Exception e) {
                logger.error("Failed to reset the modem", e);
            }
        }
    }

    @Override
    public String getFirmwareVersion() throws KuraException {
        return "N/A";
    }

    private void disableURC() throws KuraException {
        synchronized (this.atLock) {
            logger.debug("sendCommand disableURC :: {}", HuaweiModemAtCommands.DISABLE_URC.getCommand());

            final byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            try {
                reply = commAtConnection.sendCommand(HuaweiModemAtCommands.DISABLE_URC.getCommand().getBytes(), 1000,
                        100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
            } finally {
                closeSerialPort(commAtConnection);
            }

            if (reply == null) {
                throw new KuraException(KuraErrorCode.TIMED_OUT, HuaweiModemAtCommands.DISABLE_URC.getCommand());
            }
            final String response = new String(reply, StandardCharsets.US_ASCII);
            if (!response.contains("OK")) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, response);
            }
        }
    }
}
