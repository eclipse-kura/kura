/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc 
 *******************************************************************************/

package org.eclipse.kura.net.admin.modem.ublox.generic;

import java.io.IOException;

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

public class UbloxModem extends HspaModem {

    private static final Logger logger = LoggerFactory.getLogger(UbloxModem.class);

    public UbloxModem(ModemDevice device, String platform, ConnectionFactory connectionFactory) {
        super(device, platform, connectionFactory);
    }

    @Override
    public void reset() throws KuraException {
        UsbModemDriver modemDriver = getModemDriver();
        try {
            modemDriver.reset((UsbModemDevice) getModemDevice());
        } catch (KuraException e) {
            logger.warn("Modem reset failed");
            throw e;
        }
    }

    @Override
    public long getCallTxCounter() throws KuraException {

        long txCnt = 0;
        synchronized (this.atLock) {
            logger.debug("sendCommand getGprsSessionDataVolume :: {}",
                    UbloxModemAtCommands.getGprsSessionDataVolume.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection
                        .sendCommand(UbloxModemAtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] splitPdp;
                String[] splitData;
                String sDataVolume = this.getResponseString(reply);
                splitPdp = sDataVolume.split("+UGCNTRD:");
                if (splitPdp.length > 1) {
                    for (String pdp : splitPdp) {
                        if (pdp.trim().length() > 0) {
                            splitData = pdp.trim().split(",");
                            if (splitData.length >= 5) {
                                int pdpNo = Integer.parseInt(splitData[0]);
                                if (pdpNo == this.pdpContext) {
                                    txCnt = Integer.parseInt(splitData[1]);
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
                    UbloxModemAtCommands.getGprsSessionDataVolume.getCommand());
            byte[] reply;
            CommConnection commAtConnection = openSerialPort(getAtPort());
            if (!isAtReachable(commAtConnection)) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.NOT_CONNECTED, MODEM_NOT_AVAILABLE_FOR_AT_CMDS_MSG);
            }
            try {
                reply = commAtConnection
                        .sendCommand(UbloxModemAtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
            } catch (IOException e) {
                closeSerialPort(commAtConnection);
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
            closeSerialPort(commAtConnection);
            if (reply != null) {
                String[] splitPdp;
                String[] splitData;
                String sDataVolume = this.getResponseString(reply);
                splitPdp = sDataVolume.split("+UGCNTRD:");
                if (splitPdp.length > 1) {
                    for (String pdp : splitPdp) {
                        if (pdp.trim().length() > 0) {
                            splitData = pdp.trim().split(",");
                            if (splitData.length >= 4) {
                                int pdpNo = Integer.parseInt(splitData[0]);
                                if (pdpNo == this.pdpContext) {
                                    rxCnt = Integer.parseInt(splitData[2]);
                                }
                            }
                        }
                    }
                }
            }
        }
        return rxCnt;
    }
}
