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
 * Base class for USB devices
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public abstract class AbstractUsbDevice implements UsbDevice {

    /** The vendor ID of the USB device **/
    private final String vendorId;

    /** The product ID of the USB device **/
    private final String productId;

    /** The manufacturer name **/
    private final String manufacturerName;

    /** The product name **/
    private final String productName;

    private final String usbBusNumber;
    private final String usbDevicePath;

    public AbstractUsbDevice(String vendorId, String productId, String manufacturerName, String productName,
            String usbBusNumber, String usbDevicePath) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.manufacturerName = manufacturerName;
        this.productName = productName;
        this.usbBusNumber = usbBusNumber;
        this.usbDevicePath = usbDevicePath;
    }

    public AbstractUsbDevice(AbstractUsbDevice usbDevice) {
        this.vendorId = usbDevice.getVendorId();
        this.productId = usbDevice.getProductId();
        this.manufacturerName = usbDevice.getManufacturerName();
        this.productName = usbDevice.getProductName();
        this.usbBusNumber = usbDevice.getUsbBusNumber();
        this.usbDevicePath = usbDevice.getUsbDevicePath();
    }

    @Override
    public String getVendorId() {
        return this.vendorId;
    }

    @Override
    public String getProductId() {
        return this.productId;
    }

    @Override
    public String getManufacturerName() {
        return this.manufacturerName;
    }

    @Override
    public String getProductName() {
        return this.productName;
    }

    @Override
    public String getUsbBusNumber() {
        return this.usbBusNumber;
    }

    @Override
    public String getUsbDevicePath() {
        return this.usbDevicePath;
    }

    @Override
    public String getUsbPort() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.usbBusNumber).append("-").append(this.usbDevicePath);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.manufacturerName == null ? 0 : this.manufacturerName.hashCode());
        result = prime * result + (this.productId == null ? 0 : this.productId.hashCode());
        result = prime * result + (this.productName == null ? 0 : this.productName.hashCode());
        result = prime * result + (this.usbBusNumber == null ? 0 : this.usbBusNumber.hashCode());
        result = prime * result + (this.usbDevicePath == null ? 0 : this.usbDevicePath.hashCode());
        result = prime * result + (this.vendorId == null ? 0 : this.vendorId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractUsbDevice other = (AbstractUsbDevice) obj;
        if (this.manufacturerName == null) {
            if (other.manufacturerName != null) {
                return false;
            }
        } else if (!this.manufacturerName.equals(other.manufacturerName)) {
            return false;
        }
        if (this.productId == null) {
            if (other.productId != null) {
                return false;
            }
        } else if (!this.productId.equals(other.productId)) {
            return false;
        }
        if (this.productName == null) {
            if (other.productName != null) {
                return false;
            }
        } else if (!this.productName.equals(other.productName)) {
            return false;
        }
        if (this.usbBusNumber == null) {
            if (other.usbBusNumber != null) {
                return false;
            }
        } else if (!this.usbBusNumber.equals(other.usbBusNumber)) {
            return false;
        }
        if (this.usbDevicePath == null) {
            if (other.usbDevicePath != null) {
                return false;
            }
        } else if (!this.usbDevicePath.equals(other.usbDevicePath)) {
            return false;
        }
        if (this.vendorId == null) {
            if (other.vendorId != null) {
                return false;
            }
        } else if (!this.vendorId.equals(other.vendorId)) {
            return false;
        }
        return true;
    }
}
