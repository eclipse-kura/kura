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

import org.eclipse.kura.type.TypedValue;

import com.google.common.base.MoreObjects;

/**
 * The Class DeviceRecord represents a record to perform read/write/monitor
 * operation on the provided channel using the associated device driver
 */
public final class DeviceRecord {

	/**
	 * The channel name. The channel name for a device must be unique.
	 */
	private String m_channelName;

	/** The device flag. */
	private DeviceFlag m_deviceFlag;

	/** The timetstamp of the record */
	private long m_timetstamp;

	/**
	 * Represents the value as read by the driver during a read or a monitor
	 * operation. It can also represent the value which needs to be written by
	 * the driver to the actual device.
	 */
	private TypedValue<?> m_value;

	/**
	 * Gets the channel name.
	 *
	 * @return the channel name
	 */
	public String getChannelName() {
		return this.m_channelName;
	}

	/**
	 * Gets the device flag.
	 *
	 * @return the device flag
	 */
	public DeviceFlag getDeviceFlag() {
		return this.m_deviceFlag;
	}

	/**
	 * Gets the timetstamp.
	 *
	 * @return the timetstamp
	 */
	public long getTimetstamp() {
		return this.m_timetstamp;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public TypedValue<?> getValue() {
		return this.m_value;
	}

	/**
	 * Sets the channel name as provided
	 *
	 * @param channelName
	 *            the new channel name
	 */
	public void setChannelName(final String channelName) {
		this.m_channelName = channelName;
	}

	/**
	 * Sets the device flag as provided
	 *
	 * @param deviceFlag
	 *            the new device flag
	 */
	public void setDeviceFlag(final DeviceFlag deviceFlag) {
		this.m_deviceFlag = deviceFlag;
	}

	/**
	 * Sets the timetstamp as provided
	 *
	 * @param timetstamp
	 *            the new timetstamp
	 */
	public void setTimetstamp(final long timetstamp) {
		this.m_timetstamp = timetstamp;
	}

	/**
	 * Sets the value as provided
	 *
	 * @param value
	 *            the new value
	 */
	public void setValue(final TypedValue<?> value) {
		this.m_value = value;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("channel_name", this.m_channelName)
				.add("device_flag", this.m_deviceFlag).add("timestamp", this.m_timetstamp).add("value", this.m_value)
				.toString();
	}

}
