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
package org.eclipse.kura.usb;

/**
 * Representation of a USB block device.  This includes storage type USB devices.
 */
public class UsbBlockDevice extends AbstractUsbDevice {

	private String m_deviceNode;

	public UsbBlockDevice(String vendorId, String productId, String manufacturerName, String productName, String usbBusNumber, String usbDevicePath, String deviceNode) {
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
		UsbBlockDevice other = (UsbBlockDevice) obj;
		if (m_deviceNode == null) {
			if (other.m_deviceNode != null)
				return false;
		} else if (!m_deviceNode.equals(other.m_deviceNode))
			return false;
		return true;
	}
}
