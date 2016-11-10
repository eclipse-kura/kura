/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.asset;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.DataType;

/**
 * The Class Channel represents a communication channel of an asset. The
 * communication channel has all the required configuration to perform specific
 * operation (read/write/monitor).
 *
 * @see AssetConfiguration
 */
@NotThreadSafe
public final class Channel {

	/** The communication channel configuration. */
	private final Map<String, Object> configuration;

	/** The unique identifier of the channel. */
	private final long id;

	/** The name of the communication channel. */
	private String name;

	/** The type of the channel. (READ/WRITE/READ_WRITE) */
	private ChannelType type;

	/**
	 * The data type of the value as expected from the operation
	 */
	private DataType valueType;

	/**
	 * Instantiates a new channel.
	 *
	 * @param id
	 *            the identifier
	 * @param name
	 *            the name
	 * @param type
	 *            the type
	 * @param valueType
	 *            the value type
	 * @param config
	 *            the configuration
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null or channel ID is 0 or
	 *             negative
	 */
	public Channel(final long id, final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> config) {
		checkCondition(id <= 0, "Channel ID cannot be 0 or negative");
		checkNull(name, "Channel name cannot be null");
		checkNull(type, "Channel type cannot be null");
		checkNull(valueType, "Channel value type cannot be null");
		checkNull(config, "Channel configuration cannot be null");

		this.id = id;
		this.configuration = config;
		this.name = name;
		this.type = type;
		this.valueType = valueType;
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
		final Channel other = (Channel) obj;
		if (this.configuration == null) {
			if (other.configuration != null) {
				return false;
			}
		} else if (!this.configuration.equals(other.configuration)) {
			return false;
		}
		if (this.id != other.id) {
			return false;
		}
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		if (this.type != other.type) {
			return false;
		}
		if (this.valueType != other.valueType) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the configuration of the communication channel.
	 *
	 * @return the configuration of the communication channel
	 */
	public Map<String, Object> getConfiguration() {
		return this.configuration;
	}

	/**
	 * Gets the ID.
	 *
	 * @return the id
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * Gets the name of the communication channel.
	 *
	 * @return the name of the communication channel
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the type of the communication channel.
	 *
	 * @return the type of the communication channel
	 */
	public ChannelType getType() {
		return this.type;
	}

	/**
	 * Gets the value type as expected for operations.
	 *
	 * @return the value type
	 */
	public DataType getValueType() {
		return this.valueType;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.configuration == null) ? 0 : this.configuration.hashCode());
		result = (prime * result) + (int) (this.id ^ (this.id >>> 32));
		result = (prime * result) + ((this.name == null) ? 0 : this.name.hashCode());
		result = (prime * result) + ((this.type == null) ? 0 : this.type.hashCode());
		result = (prime * result) + ((this.valueType == null) ? 0 : this.valueType.hashCode());
		return result;
	}

	/**
	 * Sets the name.
	 *
	 * @param name
	 *            the new name
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setName(final String name) {
		checkNull(name, "Channel name cannot be null");
		this.name = name;
	}

	/**
	 * Sets the type.
	 *
	 * @param type
	 *            the new type
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setType(final ChannelType type) {
		checkNull(type, "Channel type cannot be null");
		this.type = type;
	}

	/**
	 * Sets the value type.
	 *
	 * @param valueType
	 *            the new value type
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setValueType(final DataType valueType) {
		checkNull(valueType, "Channel value type cannot be null");
		this.valueType = valueType;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "Channel [configuration=" + this.configuration + ", id=" + this.id + ", name=" + this.name + ", type="
				+ this.type + ", valueType=" + this.valueType + "]";
	}
}
