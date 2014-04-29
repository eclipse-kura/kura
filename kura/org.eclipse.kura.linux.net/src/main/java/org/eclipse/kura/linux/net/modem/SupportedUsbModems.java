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
package org.eclipse.kura.linux.net.modem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.linux.util.ProcessStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportedUsbModems {
	
	private static final Logger s_logger = LoggerFactory.getLogger(SupportedUsbModems.class);
	
	static {
		for (SupportedUsbModemInfo modem : SupportedUsbModemInfo.values()) {
			try {
				if(isAttached(modem.getVendorId(), modem.getProductId())) {
					// modprobe driver
					s_logger.info("The " + modem.getVendorId() + ":" + modem.getProductId() + "USB modem device attached");
					List<? extends UsbModemDriver> drivers = modem.getDeviceDrivers();
					for (UsbModemDriver driver : drivers) {
						driver.install();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
    
    public static SupportedUsbModemInfo getModem(String vendorId, String productId) {
        if (vendorId == null || productId == null)
            return null;
        
        for (SupportedUsbModemInfo modem : SupportedUsbModemInfo.values()) {
            if (vendorId.equals(modem.getVendorId()) && productId.equals(modem.getProductId())) {
                return modem;
            }
        }
        
        return null;
    }
    
    public static boolean isSupported(String vendorId, String productId) {
        return (SupportedUsbModems.getModem(vendorId, productId) != null);
    }
    
    private static boolean isAttached (String vendor, String product) throws Exception {
    	// lsusb -d 1bc7:1010
    	boolean attached = false;
    	ProcessStats processStats = LinuxProcessUtil.startWithStats(formLsusbCommand(vendor, product));
    	BufferedReader br = new BufferedReader(new InputStreamReader(processStats.getInputStream()));
    	String line = null;
    	while ((line = br.readLine()) != null) {
    		if (line.indexOf(vendor + ":" + product) > 0) {
    			attached = true;
    			break;
    		}
    	}
    	ProcessUtil.destroy(processStats.getProcess());
    	return attached;
    }
    
	private static String formLsusbCommand(String vendor, String product) {
		StringBuffer sb = new StringBuffer();
		sb.append("lsusb -d ").append(vendor).append(":").append(product);
		return sb.toString();
	}
}