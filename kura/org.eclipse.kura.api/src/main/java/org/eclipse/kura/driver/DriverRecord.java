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
 * driver.<br/>
 * <br/>
 *
 * A Driver Record must contain the specific channel configuration that it needs
 * to operate on. The most basic requirements to provide the configuration is
 * the channel ID as long value. This channel ID is essentially needed to map an
 * asset record with a driver record and hence this channel ID needs to be
 * set.<br/>
 * <br/>
 *
 * The channel ID must be set in the map as a key of :
 * {@link DriverConstants#CHANNEL_ID} or {@code channel.id}<br/>
 * <br/>
 *
 * In case you are using driver in isolation (i.e without using Assets), then
 * the channel ID need not to be set.<br/>
 * <br/>
 *
 * But in both the scenarios, you must set the value type in the configuration
 * map. The value type is the channel value type in case you are using Assets
 * but not the Drivers directly.<br/>
 * <br/>
 *
 * But if you are accessing Drivers directly, this value type must be set to one
 * of the types from {@link DataType}.<br/>
 * <br/>
 *
 * The value type must be set in the map as a key of :
 * {@link DriverConstants#CHANNEL_VALUE_TYPE} or {@code channel.value.type}<br/>
 * <br/>
 *
 * The Driver Record also contain driver status of the operation on the provided
 * channel. This status contains a flag, an exception message and an exception
 * instance. Generally for any exceptional circumstance, the driver will never
 * throw any exception but it would definitely set the driver flag and relevant
 * exception message or the exception instance if any in the status instance.
 * So, in the lower level, the driver never throws any exception, Rather it must
 * set the relevant flag in the driver status.
 */
@NotThreadSafe
public final class DriverRecord {

	/**
	 * Provided channel configuration to perform read or write or monitor
	 * operation.
	 */
	private Map<String, Object> channelConfiguration;

	/**
	 * Represents a driver specific status which signifies the status of the
	 * read or write or monitor operation.
	 */
	private DriverStatus driverStatus;

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
		if (this.driverStatus == null) {
			if (other.driverStatus != null) {
				return false;
			}
		} else if (!this.driverStatus.equals(other.driverStatus)) {
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
	 * Returns the driver operation status.
	 *
	 * @return the driver status
	 */
	public DriverStatus getDriverStatus() {
		return this.driverStatus;
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
		result = (prime * result) + ((this.driverStatus == null) ? 0 : this.driverStatus.hashCode());
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
	 * Sets the status.
	 *
	 * @param status
	 *            the new driver status
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setDriverStatus(final DriverStatus status) {
		checkNull(this.value, "Driver Status cannot be null");
		this.driverStatus = status;
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
		return "DriverRecord [channelConfiguration=" + this.channelConfiguration + ", driverStatus=" + this.driverStatus
				+ ", timestamp=" + this.timestamp + ", value=" + this.value + "]";
	}

}
