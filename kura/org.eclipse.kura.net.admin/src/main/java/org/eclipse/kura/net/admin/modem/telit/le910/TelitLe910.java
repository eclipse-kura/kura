/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910AtCommands;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelitLe910 extends TelitHe910 implements HspaCellularModem {

	private static final Logger s_logger = LoggerFactory.getLogger(TelitLe910.class);
	
	public TelitLe910(ModemDevice device, String platform,
			ConnectionFactory connectionFactory) {
		super(device, platform, connectionFactory);
	}
	
	/* Don't need this since we can now use AT$GPSP=1
	public void enableGps() throws KuraException {
		enableGps(TelitLe910AtCommands.gpsPowerUp.getCommand());
	}
	*/
	
	@Override
	public List<ModemTechnologyType> getTechnologyTypes() throws KuraException {
		
		List<ModemTechnologyType>modemTechnologyTypes = null;
		ModemDevice device = getModemDevice();
		if (device == null) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No modem device");
		}
		if (device instanceof UsbModemDevice) {
    		SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)device);
    		if (usbModemInfo != null)  {
    			modemTechnologyTypes = usbModemInfo.getTechnologyTypes();
    		} else {
    			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "No usbModemInfo available");
    		}
    	} else {
    		throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported modem device");
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

    	synchronized (s_atLock) {
    		s_logger.debug("sendCommand getSimStatus :: {} command to port {}", TelitHe910AtCommands.getSimStatus.getCommand(), port);
	    	byte[] reply = null;
	    	CommConnection commAtConnection = null;
	    	try {

	    	    commAtConnection = openSerialPort(port);
	    	    if (!isAtReachable(commAtConnection)) {	    		
	    	        throw new KuraException(KuraErrorCode.NOT_CONNECTED, "Modem not available for AT commands: " + TelitHe910.class.getName());
	    	    }
	    	    
				reply = commAtConnection.sendCommand(TelitHe910AtCommands.getSimStatus.getCommand().getBytes(), 1000, 100);
    	        if (reply != null) {
    	            String simStatus = getResponseString(reply);
    	            String[] simStatusSplit = simStatus.split(",");
    	            if((simStatusSplit.length > 1) && (Integer.valueOf(simStatusSplit[1]) > 0)) {
    	                simReady = true;
    	            } 
    	        }
	    	}
        	catch (IOException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        	catch (KuraException e) {
        	    throw e;
        	}
        	finally {	        
    	        closeSerialPort(commAtConnection);
        	}
    	}
    	return simReady;
	}
}
