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

package org.eclipse.kura.net.admin.modem.telit.le910;

import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.telit.he910.TelitHe910;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.io.ConnectionFactory;

public class TelitLe910 extends TelitHe910 implements HspaCellularModem {

	public TelitLe910(ModemDevice device, String platform,
			ConnectionFactory connectionFactory) {
		super(device, platform, connectionFactory);
	}
	
	public void enableGps() throws KuraException {
		enableGps(TelitLe910AtCommands.gpsPowerUp.getCommand());
	}
	
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
}
