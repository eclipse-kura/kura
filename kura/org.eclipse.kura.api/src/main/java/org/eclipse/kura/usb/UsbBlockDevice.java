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
package org.eclipse.kura.usb;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Representation of a USB block device. This includes storage type USB devices.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class UsbBlockDevice extends AbstractUsbDevice {

    private final String m_deviceNode;

    public UsbBlockDevice(String vendorId, String productId, String manufacturerName, String productName,
            String usbBusNumber, String usbDevicePath, String deviceNode) {
        super(vendorId, productId, manufacturerName, productName, usbBusNumber, usbDevicePath);
        this.m_deviceNode = deviceNode;
    }

    public String getDeviceNode() {
        return this.m_deviceNode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.m_deviceNode == null ? 0 : this.m_deviceNode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UsbBlockDevice other = (UsbBlockDevice) obj;
        if (this.m_deviceNode == null) {
            if (other.m_deviceNode != null) {
                return false;
            }
        } else if (!this.m_deviceNode.equals(other.m_deviceNode)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UsbBlockDevice [getDeviceNode()=" + getDeviceNode() + ", getVendorId()=" + getVendorId()
                + ", getProductId()=" + getProductId() + ", getManufacturerName()=" + getManufacturerName()
                + ", getProductName()=" + getProductName() + ", getUsbBusNumber()=" + getUsbBusNumber()
                + ", getUsbDevicePath()=" + getUsbDevicePath() + ", getUsbPort()=" + getUsbPort() + "]";
    }
}
