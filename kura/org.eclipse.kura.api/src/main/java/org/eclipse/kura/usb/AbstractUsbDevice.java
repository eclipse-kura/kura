/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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
 * Base class for USB devices
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public abstract class AbstractUsbDevice implements UsbDevice {

    /** The vendor ID of the USB device **/
    private final String m_vendorId;

    /** The product ID of the USB device **/
    private final String m_productId;

    /** The manufacturer name **/
    private final String m_manufacturerName;

    /** The product name **/
    private final String m_productName;

    private final String m_usbBusNumber;
    private final String m_usbDevicePath;

    public AbstractUsbDevice(String vendorId, String productId, String manufacturerName, String productName,
            String usbBusNumber, String usbDevicePath) {
        this.m_vendorId = vendorId;
        this.m_productId = productId;
        this.m_manufacturerName = manufacturerName;
        this.m_productName = productName;
        this.m_usbBusNumber = usbBusNumber;
        this.m_usbDevicePath = usbDevicePath;
    }

    public AbstractUsbDevice(AbstractUsbDevice usbDevice) {
        this.m_vendorId = usbDevice.getVendorId();
        this.m_productId = usbDevice.getProductId();
        this.m_manufacturerName = usbDevice.getManufacturerName();
        this.m_productName = usbDevice.getProductName();
        this.m_usbBusNumber = usbDevice.getUsbBusNumber();
        this.m_usbDevicePath = usbDevice.getUsbDevicePath();
    }

    @Override
    public String getVendorId() {
        return this.m_vendorId;
    }

    @Override
    public String getProductId() {
        return this.m_productId;
    }

    @Override
    public String getManufacturerName() {
        return this.m_manufacturerName;
    }

    @Override
    public String getProductName() {
        return this.m_productName;
    }

    @Override
    public String getUsbBusNumber() {
        return this.m_usbBusNumber;
    }

    @Override
    public String getUsbDevicePath() {
        return this.m_usbDevicePath;
    }

    @Override
    public String getUsbPort() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.m_usbBusNumber).append("-").append(this.m_usbDevicePath);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.m_manufacturerName == null ? 0 : this.m_manufacturerName.hashCode());
        result = prime * result + (this.m_productId == null ? 0 : this.m_productId.hashCode());
        result = prime * result + (this.m_productName == null ? 0 : this.m_productName.hashCode());
        result = prime * result + (this.m_usbBusNumber == null ? 0 : this.m_usbBusNumber.hashCode());
        result = prime * result + (this.m_usbDevicePath == null ? 0 : this.m_usbDevicePath.hashCode());
        result = prime * result + (this.m_vendorId == null ? 0 : this.m_vendorId.hashCode());
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
        if (this.m_manufacturerName == null) {
            if (other.m_manufacturerName != null) {
                return false;
            }
        } else if (!this.m_manufacturerName.equals(other.m_manufacturerName)) {
            return false;
        }
        if (this.m_productId == null) {
            if (other.m_productId != null) {
                return false;
            }
        } else if (!this.m_productId.equals(other.m_productId)) {
            return false;
        }
        if (this.m_productName == null) {
            if (other.m_productName != null) {
                return false;
            }
        } else if (!this.m_productName.equals(other.m_productName)) {
            return false;
        }
        if (this.m_usbBusNumber == null) {
            if (other.m_usbBusNumber != null) {
                return false;
            }
        } else if (!this.m_usbBusNumber.equals(other.m_usbBusNumber)) {
            return false;
        }
        if (this.m_usbDevicePath == null) {
            if (other.m_usbDevicePath != null) {
                return false;
            }
        } else if (!this.m_usbDevicePath.equals(other.m_usbDevicePath)) {
            return false;
        }
        if (this.m_vendorId == null) {
            if (other.m_vendorId != null) {
                return false;
            }
        } else if (!this.m_vendorId.equals(other.m_vendorId)) {
            return false;
        }
        return true;
    }
}
