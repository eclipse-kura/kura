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

import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.type.DataType;

import com.google.common.base.MoreObjects;

/**
 * The Class Channel represents a communication channel of a device. The
 * communication channel has all the required configuration to perform specific
 * operation (read/write/monitor). The channel names must be unique to each
 * other.
 */
public final class Channel {

	/**
	 * Creates a new channel with the provided values
	 *
	 * @param name
	 *            the name of the channel
	 * @param type
	 *            the type of the channel
	 * @param valueType
	 *            the value type of the channel
	 * @param configuration
	 *            the configuration to be read
	 * @return the channel
	 */
	public static Channel of(final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> configuration) {
		return new Channel(name, type, valueType, configuration);
	}

	/** The communication channel configuration. */
	private Map<String, Object> m_config;

	/** The name of the communication channel. */
	private String m_name;

	/** The type of the channel. */
	private ChannelType m_type;

	/**
	 * The data type of the value as expected for the operations
	 * (read/write/monitor).
	 */
	private DataType m_valueType;

	/**
	 * Instantiates a new channel.
	 *
	 * @param name
	 *            the name
	 * @param type
	 *            the type
	 * @param valueType
	 *            the value type
	 * @param config
	 *            the config
	 */
	public Channel(final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> config) {
		this.m_config = config;
		this.m_name = name;
		this.m_type = type;
		this.m_valueType = valueType;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object otherChannel) {
		return Objects.equals(this.m_name, ((Channel) (otherChannel)).getName());
	}

	/**
	 * Gets the configuration of the communication channel.
	 *
	 * @return the configuration of the communication channel
	 */
	public Map<String, Object> getConfig() {
		return this.m_config;
	}

	/**
	 * Gets the name of the communication channel.
	 *
	 * @return the name of the communication channel
	 */
	public String getName() {
		return this.m_name;
	}

	/**
	 * Gets the type of the communication channel.
	 *
	 * @return the type of the communication channel
	 */
	public ChannelType getType() {
		return this.m_type;
	}

	/**
	 * Gets the value type as expected for operations.
	 *
	 * @return the value type
	 */
	public DataType getValueType() {
		return this.m_valueType;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hash(this.m_name, this.m_type, this.m_valueType, this.m_config);
	}

	/**
	 * Sets the configuration of the communication channel.
	 *
	 * @param config
	 *            the configuration of the communication channel
	 */
	public void setConfig(final Map<String, Object> config) {
		this.m_config = config;
	}

	/**
	 * Sets the name of the communication channel.
	 *
	 * @param name
	 *            the new name of the communication channel
	 */
	public void setName(final String name) {
		this.m_name = name;
	}

	/**
	 * Sets the type of the communication channel.
	 *
	 * @param type
	 *            the new type of the communication channel
	 */
	public void setType(final ChannelType type) {
		this.m_type = type;
	}

	/**
	 * Sets the value type as expected for performing the operations.
	 *
	 * @param valueType
	 *            the new value type
	 */
	public void setValueType(final DataType valueType) {
		this.m_valueType = valueType;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", this.m_name).add("channel_type", this.m_type)
				.add("value_type", this.m_valueType).add("channel_configuration", this.m_config).toString();
	}

}
