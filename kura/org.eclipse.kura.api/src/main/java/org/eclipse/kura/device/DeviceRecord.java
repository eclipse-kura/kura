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

import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.TypedValue;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/**
 * The Class DeviceRecord represents a record to perform read/write/monitor
 * operation on the provided channel using the associated device driver.
 */
@NotThreadSafe
public final class DeviceRecord implements Comparable<DeviceRecord> {

	/**
	 * The associated channel name. The channel name for any device must be
	 * unique.
	 */
	private String channelName;

	/** The device flag. */
	private DeviceFlag deviceFlag;

	/** The timestamp of the record. */
	private long timestamp;

	/**
	 * Represents the value as read by the driver during a read or a monitor
	 * operation. It can also represent the value which needs to be written by
	 * the driver to the actual device.
	 */
	private TypedValue<?> value;

	/**
	 * Instantiates a new device record.
	 *
	 * @param channelName
	 *            the channel name
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public DeviceRecord(final String channelName) {
		checkNull(channelName, "Channel name cannot be null");
		this.channelName = channelName;
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(final DeviceRecord otherDeviceRecord) {
		checkNull(otherDeviceRecord, "Provided device record to compare is null");
		return ComparisonChain.start().compare(this.channelName, otherDeviceRecord.getChannelName())
				.compare(this.value, otherDeviceRecord.getValue())
				.compare(this.deviceFlag, otherDeviceRecord.getDeviceFlag())
				.compare(this.timestamp, otherDeviceRecord.getTimestamp()).result();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof DeviceRecord) {
			final DeviceRecord rec = (DeviceRecord) obj;
			return Objects.equal(rec.getChannelName(), this.channelName)
					&& Objects.equal(rec.getValue(), this.value)
					&& Objects.equal(rec.getDeviceFlag(), this.deviceFlag)
					&& Objects.equal(rec.getTimestamp(), this.timestamp);
		}
		return false;
	}

	/**
	 * Gets the channel name.
	 *
	 * @return the channel name
	 */
	public String getChannelName() {
		return this.channelName;
	}

	/**
	 * Gets the device flag.
	 *
	 * @return the device flag
	 */
	public DeviceFlag getDeviceFlag() {
		return this.deviceFlag;
	}

	/**
	 * Gets the timestamp.
	 *
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public TypedValue<?> getValue() {
		return this.value;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hashCode(this.channelName, this.value, this.deviceFlag, this.timestamp);
	}

	/**
	 * Sets the channel name as provided.
	 *
	 * @param channelName
	 *            the new channel name
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setChannelName(final String channelName) {
		checkNull(channelName, "Channel name cannot be null");
		this.channelName = channelName;
	}

	/**
	 * Sets the device flag as provided.
	 *
	 * @param deviceFlag
	 *            the new device flag
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setDeviceFlag(final DeviceFlag deviceFlag) {
		checkNull(deviceFlag, "Device flag cannot be null");
		this.deviceFlag = deviceFlag;
	}

	/**
	 * Sets the timestamp as provided.
	 *
	 * @param timestamp
	 *            the new timestamp
	 */
	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Sets the value as provided.
	 *
	 * @param value
	 *            the new value
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setValue(final TypedValue<?> value) {
		checkNull(value, "Value type cannot be null");
		this.value = value;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("channel_name", this.channelName)
				.add("device_flag", this.deviceFlag).add("timestamp", this.timestamp).add("value", this.value)
				.toString();
	}

}
