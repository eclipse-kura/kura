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

/**
 * Representation of USB TTY devices
 */
public class UsbTtyDevice extends AbstractUsbDevice {

	/** The device node of the TTY device **/
	private String m_deviceNode;

	public UsbTtyDevice(String vendorId, String productId, String manufacturerName, String productName, String usbBusNumber, String usbDevicePath, String deviceNode) {
		super(vendorId, productId, manufacturerName, productName, usbBusNumber, usbDevicePath);
		m_deviceNode = deviceNode;
	}

	public String getDeviceNode() {
		return m_deviceNode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((m_deviceNode == null) ? 0 : m_deviceNode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		UsbTtyDevice other = (UsbTtyDevice) obj;
		if (m_deviceNode == null) {
			if (other.m_deviceNode != null)
				return false;
		} else if (!m_deviceNode.equals(other.m_deviceNode))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UsbTtyDevice [getDeviceNode()=" + getDeviceNode()
				+ ", getVendorId()=" + getVendorId()
				+ ", getProductId()=" + getProductId()
				+ ", getManufacturerName()=" + getManufacturerName()
				+ ", getProductName()=" + getProductName()
				+ ", getUsbBusNumber()=" + getUsbBusNumber()
				+ ", getUsbDevicePath()=" + getUsbDevicePath()
				+ ", getUsbPort()=" + getUsbPort() + "]";
	}
}
