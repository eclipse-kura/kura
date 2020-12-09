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
 * Representation of USB network devices
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class UsbNetDevice extends AbstractUsbDevice {

    /** The interface name associated with this device **/
    private final String interfaceName;

    public UsbNetDevice(String vendorId, String productId, String manufacturerName, String productName,
            String usbBusNumber, String usbDevicePath, String interfaceName) {
        super(vendorId, productId, manufacturerName, productName, usbBusNumber, usbDevicePath);
        this.interfaceName = interfaceName;
    }

    public String getInterfaceName() {
        return this.interfaceName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.interfaceName == null ? 0 : this.interfaceName.hashCode());
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
        UsbNetDevice other = (UsbNetDevice) obj;
        if (this.interfaceName == null) {
            if (other.interfaceName != null) {
                return false;
            }
        } else if (!this.interfaceName.equals(other.interfaceName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UsbNetDevice [getInterfaceName()=" + getInterfaceName() + ", getVendorId()=" + getVendorId()
                + ", getProductId()=" + getProductId() + ", getManufacturerName()=" + getManufacturerName()
                + ", getProductName()=" + getProductName() + ", getUsbBusNumber()=" + getUsbBusNumber()
                + ", getUsbDevicePath()=" + getUsbDevicePath() + ", getUsbPort()=" + getUsbPort() + "]";
    }
}
