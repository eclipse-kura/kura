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
 *******************************************************************************/
package org.eclipse.kura.linux.net.modem;

import org.eclipse.kura.usb.UsbDevice;

public class SupportedUsbModemsInfo {

    private SupportedUsbModemsInfo() {

    }

    public static SupportedUsbModemInfo getModem(UsbDevice usbDevice) {
        if (usbDevice == null) {
            return null;
        }
        return getModem(usbDevice.getVendorId(), usbDevice.getProductId(), usbDevice.getProductName());
    }

    @Deprecated
    public static SupportedUsbModemInfo getModem(String vendorId, String productId, String productName) {
        if (vendorId == null || productId == null) {
            return null;
        }

        for (SupportedUsbModemInfo modem : SupportedUsbModemInfo.values()) {
            if (vendorId.equals(modem.getVendorId()) && productId.equals(modem.getProductId())
                    && (modem.getProductName().isEmpty() || productName.equals(modem.getProductName()))) {
                return modem;
            }
        }

        return null;
    }

    @Deprecated
    public static boolean isSupported(String vendorId, String productId, String productName) {
        return SupportedUsbModems.isSupported(vendorId, productId, productName);
    }

    public static boolean isSupported(UsbDevice usbDevice) {
        return SupportedUsbModems.isSupported(usbDevice);
    }
}
