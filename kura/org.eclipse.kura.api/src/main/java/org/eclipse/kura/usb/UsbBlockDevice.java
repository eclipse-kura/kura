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
 ******************************************************************************/
package org.eclipse.kura.usb;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Representation of a USB block device. This includes storage type USB devices.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class UsbBlockDevice extends AbstractUsbDevice {

    private final String deviceNode;

    public UsbBlockDevice(String vendorId, String productId, String manufacturerName, String productName,
            String usbBusNumber, String usbDevicePath, String deviceNode) {
        super(vendorId, productId, manufacturerName, productName, usbBusNumber, usbDevicePath);
        this.deviceNode = deviceNode;
    }

    public String getDeviceNode() {
        return this.deviceNode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.deviceNode == null ? 0 : this.deviceNode.hashCode());
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
        if (this.deviceNode == null) {
            if (other.deviceNode != null) {
                return false;
            }
        } else if (!this.deviceNode.equals(other.deviceNode)) {
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
