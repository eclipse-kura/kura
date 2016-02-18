/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */

package org.eclipse.kura.net.admin.modem.ublox.generic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.linux.net.modem.ModemDriver;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.hspa.HspaModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemRegistrationStatus;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UbloxModem extends HspaModem {

	private static final Logger s_logger = LoggerFactory.getLogger(UbloxModem.class);

	public UbloxModem(ModemDevice device, String platform,
			ConnectionFactory connectionFactory) {
		super(device, platform, connectionFactory);
	}
	
	@Override
	public void reset() throws KuraException {
		ModemDriver modemDriver = getModemDriver();
		if (!modemDriver.resetModem()) {
			s_logger.warn("Modem reset failed");
		}
	}
	    
    @Override
    public long getCallTxCounter() throws KuraException {
    	
    	long txCnt = 0;
    	synchronized (s_atLock) {
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: {}", UbloxModemAtCommands.getGprsSessionDataVolume.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
	    	}
			try {
				reply = commAtConnection.sendCommand(UbloxModemAtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String [] splitPdp = null;
				String [] splitData = null;
				String sDataVolume = this.getResponseString(reply);
				splitPdp = sDataVolume.split("+UGCNTRD:");
				if (splitPdp.length > 1) {
					for (String pdp : splitPdp) {
						if (pdp.trim().length() > 0) {
							splitData = pdp.trim().split(",");
							if (splitData.length >= 5) {
								int pdpNo = Integer.parseInt(splitData[0]);
								if (pdpNo == m_pdpContext) {
									txCnt = Integer.parseInt(splitData[1]);
								}
							}
						}
					}
				}
				reply = null;
			}
    	}
        return txCnt;
    }

    @Override
    public long getCallRxCounter() throws KuraException {
    	long rxCnt = 0;
    	synchronized (s_atLock) {
	    	s_logger.debug("sendCommand getGprsSessionDataVolume :: {}", UbloxModemAtCommands.getGprsSessionDataVolume.getCommand());
	    	byte[] reply = null;
	    	CommConnection commAtConnection = openSerialPort(getAtPort());
	    	if (!isAtReachable(commAtConnection)) {
	    		closeSerialPort(commAtConnection);
	    		throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands");
	    	}
			try {
				reply = commAtConnection.sendCommand(UbloxModemAtCommands.getGprsSessionDataVolume.getCommand().getBytes(), 1000, 100);
			} catch (IOException e) {
				closeSerialPort(commAtConnection);
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
			closeSerialPort(commAtConnection);
			if (reply != null) {
				String [] splitPdp = null;
				String [] splitData = null;
				String sDataVolume = this.getResponseString(reply);
				splitPdp = sDataVolume.split("+UGCNTRD:");
				if (splitPdp.length > 1) {
					for (String pdp : splitPdp) {
						if (pdp.trim().length() > 0) {
							splitData = pdp.trim().split(",");
							if (splitData.length >= 4) {
								int pdpNo = Integer.parseInt(splitData[0]);
								if (pdpNo == m_pdpContext) {
									rxCnt = Integer.parseInt(splitData[2]);
								}
							}
						}
					}
				}
				reply = null;
			}
    	}
        return rxCnt;
    }
}
