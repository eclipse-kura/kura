/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.usb;

/**
 * Base class for USB devices
 */
public abstract class AbstractUsbDevice implements UsbDevice {

	/** The vendor ID of the USB device **/
	private String m_vendorId;
	
	/** The product ID of the USB device **/
	private String m_productId;
	
	/** The manufacturer name **/
	private String m_manufacturerName;
	
	/** The product name **/
	private String m_productName;

	private String m_usbBusNumber;
	private String m_usbDevicePath;

	public AbstractUsbDevice(String vendorId, String productId,
			String manufacturerName, String productName, String usbBusNumber,
			String usbDevicePath) {
		m_vendorId = vendorId;
		m_productId = productId;
		m_manufacturerName = manufacturerName;
		m_productName = productName;
		m_usbBusNumber = usbBusNumber;
		m_usbDevicePath = usbDevicePath;
	}
	
	public AbstractUsbDevice(AbstractUsbDevice usbDevice) {
	    m_vendorId = usbDevice.getVendorId();
        m_productId = usbDevice.getProductId();
        m_manufacturerName = usbDevice.getManufacturerName();
        m_productName = usbDevice.getProductName();
        m_usbBusNumber = usbDevice.getUsbBusNumber();
        m_usbDevicePath = usbDevice.getUsbDevicePath();
	}

	public String getVendorId() {
		return m_vendorId;
	}

	public String getProductId() {
		return m_productId;
	}

	public String getManufacturerName() {
		return m_manufacturerName;
	}

	public String getProductName() {
		return m_productName;
	}

	public String getUsbBusNumber() {
		return m_usbBusNumber;
	}

	public String getUsbDevicePath() {
		return m_usbDevicePath;
	}

	public String getUsbPort() {
		StringBuffer sb = new StringBuffer();
		sb.append(m_usbBusNumber).append("-").append(m_usbDevicePath);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((m_manufacturerName == null) ? 0 : m_manufacturerName
						.hashCode());
		result = prime * result
				+ ((m_productId == null) ? 0 : m_productId.hashCode());
		result = prime * result
				+ ((m_productName == null) ? 0 : m_productName.hashCode());
		result = prime * result
				+ ((m_usbBusNumber == null) ? 0 : m_usbBusNumber.hashCode());
		result = prime * result
				+ ((m_usbDevicePath == null) ? 0 : m_usbDevicePath.hashCode());
		result = prime * result
				+ ((m_vendorId == null) ? 0 : m_vendorId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractUsbDevice other = (AbstractUsbDevice) obj;
		if (m_manufacturerName == null) {
			if (other.m_manufacturerName != null)
				return false;
		} else if (!m_manufacturerName.equals(other.m_manufacturerName))
			return false;
		if (m_productId == null) {
			if (other.m_productId != null)
				return false;
		} else if (!m_productId.equals(other.m_productId))
			return false;
		if (m_productName == null) {
			if (other.m_productName != null)
				return false;
		} else if (!m_productName.equals(other.m_productName))
			return false;
		if (m_usbBusNumber == null) {
			if (other.m_usbBusNumber != null)
				return false;
		} else if (!m_usbBusNumber.equals(other.m_usbBusNumber))
			return false;
		if (m_usbDevicePath == null) {
			if (other.m_usbDevicePath != null)
				return false;
		} else if (!m_usbDevicePath.equals(other.m_usbDevicePath))
			return false;
		if (m_vendorId == null) {
			if (other.m_vendorId != null)
				return false;
		} else if (!m_vendorId.equals(other.m_vendorId))
			return false;
		return true;
	}
}
