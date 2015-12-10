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

import org.eclipse.kura.usb.UsbDevice;

public class SupportedUsbModemsInfo {
 
    public static SupportedUsbModemInfo getModem(UsbDevice usbDevice) {
        if(usbDevice == null) {
            return null;
        }
        return getModem(usbDevice.getVendorId(), usbDevice.getProductId());
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
        return SupportedUsbModems.isSupported(vendorId, productId);
    }
}

