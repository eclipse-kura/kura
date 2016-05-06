/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.device;

import com.google.common.base.MoreObjects;

/**
 * This class represents an event occurred while monitoring specific channel
 * configuration by the device.
 */
public final class DeviceEvent {

	/**
	 * Represents the device record as triggered due to the device specific
	 * monitor operation.
	 */
	private final DeviceRecord m_deviceRecord;

	/**
	 * Instantiates a new device event.
	 *
	 * @param deviceRecord
	 *            the device record
	 */
	public DeviceEvent(final DeviceRecord deviceRecord) {
		super();
		this.m_deviceRecord = deviceRecord;
	}

	/**
	 * Returns the associated device record.
	 *
	 * @return the device record
	 */
	public DeviceRecord getDeviceRecord() {
		return this.m_deviceRecord;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("device_record", this.m_deviceRecord).toString();
	}

}
