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

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.TypedValue;

/**
 * The Class DriverRecord represents records needed for read, write or a monitor
 * operation on the provided channel configuration by the Kura specific device
 * driver.
 */
@NotThreadSafe
public final class DriverRecord {

	/**
	 * Provided channel configuration to perform read or write or monitor
	 * operation.
	 */
	private Map<String, Object> channelConfiguration;

	/**
	 * The associated channel identifier. The channel identifier for any asset
	 * must be unique.
	 */
	private final long channelId;

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

	/**
	 * Instantiates a new driver record.
	 *
	 * @param channelId
	 *            the channel identifier
	 * @throws KuraRuntimeException
	 *             if the channel identifier is less than or equal to zero
	 */
	public DriverRecord(final long channelId) {
		checkCondition(channelId <= 0, "Channel ID cannot be zero or less");
		this.channelId = channelId;
	}

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
		if (this.channelId != other.channelId) {
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
	 * Gets the channel identifier.
	 *
	 * @return the channel identifier
	 */
	public long getChannelId() {
		return this.channelId;
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
		result = (prime * result) + (int) (this.channelId ^ (this.channelId >>> 32));
		result = (prime * result) + ((this.driverFlag == null) ? 0 : this.driverFlag.hashCode());
		result = (prime * result) + (int) (this.timestamp ^ (this.timestamp >>> 32));
		result = (prime * result) + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	/**
	 * Sets the channel configuration as provided.
	 *
	 * @param channelConfig
	 *            the channel config
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
		return "DriverRecord [channelConfiguration=" + this.channelConfiguration + ", channelId=" + this.channelId
				+ ", driverFlag=" + this.driverFlag + ", timestamp=" + this.timestamp + ", value=" + this.value + "]";
	}

}
