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
package org.eclipse.kura.driver;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;

/**
 * The Class DriverRecord represents records needed for read, write or a monitor
 * operation on the provided channel configuration by the Kura specific device
 * driver.
 *
 * A Driver Record must contain the specific channel configuration that it needs
 * to operate on. The most basic requirements to provide the configuration is
 * the channel ID as long value. This channel ID is essentially needed to map an
 * asset record with a driver record and hence this channel ID needs to be set.
 *
 * The channel ID must be set in the map as a key of :
 * {@link DriverConstants#CHANNEL_ID} or {@code channel.id}
 *
 * In case you are using driver in isolation (i.e without using Assets), then
 * the channel ID need not to be set.
 *
 * But in both the scenarios, you must set the value type in the configuration
 * map. The value type is the channel value type in case you are using Assets
 * but not the Drivers directly.
 *
 * But if you are accessing Drivers directly, this value type must be set to one
 * of the types from {@link DataType}.
 *
 * The value type must be set in the map as a key of :
 * {@link DriverConstants#CHANNEL_VALUE_TYPE} or {@code channel.value.type}
 *
 */
@NotThreadSafe
public final class DriverRecord {

	/**
	 * Provided channel configuration to perform read or write or monitor
	 * operation.
	 */
	private Map<String, Object> channelConfiguration;

	/**
	 * Represents a driver specific flag which signifies the status of the read
	 * or write or monitor operation.
	 */
	private DriverFlag driverFlag;

	/** Represents the timestamp of the operation performed. */
	private long timestamp;

	/**
	 * Represents the value as read by the driver during a read or a monitor
	 * operation. It can also represent the value which needs to be written by
	 * the driver to the actual asset.
	 */
	private TypedValue<?> value;

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final DriverRecord other = (DriverRecord) obj;
		if (this.channelConfiguration == null) {
			if (other.channelConfiguration != null) {
				return false;
			}
		} else if (!this.channelConfiguration.equals(other.channelConfiguration)) {
			return false;
		}
		if (this.driverFlag != other.driverFlag) {
			return false;
		}
		if (this.timestamp != other.timestamp) {
			return false;
		}
		if (this.value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!this.value.equals(other.value)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the channel configuration as provided.
	 *
	 * @return the channel configuration
	 */
	public Map<String, Object> getChannelConfig() {
		return this.channelConfiguration;
	}

	/**
	 * Returns the driver flag.
	 *
	 * @return the driver flag
	 */
	public DriverFlag getDriverFlag() {
		return this.driverFlag;
	}

	/**
	 * Returns the associated timestamp.
	 *
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Returns the associated value.
	 *
	 * @return the value
	 */
	public TypedValue<?> getValue() {
		return this.value;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.channelConfiguration == null) ? 0 : this.channelConfiguration.hashCode());
		result = (prime * result) + ((this.driverFlag == null) ? 0 : this.driverFlag.hashCode());
		result = (prime * result) + (int) (this.timestamp ^ (this.timestamp >>> 32));
		result = (prime * result) + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	/**
	 * Sets the channel configuration as provided.
	 *
	 * @param channelConfig
	 *            the channel configuration
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setChannelConfig(final Map<String, Object> channelConfig) {
		checkNull(channelConfig, "Channel configuration cannot be null");
		this.channelConfiguration = new HashMap<String, Object>(channelConfig);
	}

	/**
	 * Sets the driver flag as provided.
	 *
	 * @param driverFlag
	 *            the new driver flag
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setDriverFlag(final DriverFlag driverFlag) {
		checkNull(driverFlag, "Driver flag cannot be null");
		this.driverFlag = driverFlag;
	}

	/**
	 * Sets the timestamp as provided.
	 *
	 * @param timetstamp
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
		return "DriverRecord [channelConfiguration=" + this.channelConfiguration + ", driverFlag=" + this.driverFlag
				+ ", timestamp=" + this.timestamp + ", value=" + this.value + "]";
	}

}
