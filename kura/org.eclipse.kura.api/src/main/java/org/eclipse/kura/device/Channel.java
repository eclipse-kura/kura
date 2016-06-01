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

import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.eclipse.kura.type.DataType;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

/**
 * The Class Channel represents a communication channel of a device. The
 * communication channel has all the required configuration to perform specific
 * operation (read/write/monitor). The channel names must be unique to each
 * other.
 */
@Immutable
@ThreadSafe
public final class Channel implements Comparable<Channel> {

	/** The communication channel configuration. */
	private final Map<String, Object> m_config;

	/** The name of the communication channel. */
	private final String m_name;

	/** The type of the channel. */
	private final ChannelType m_type;

	/**
	 * The data type of the value as expected for the operations
	 * (read/write/monitor).
	 */
	private final DataType m_valueType;

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
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public Channel(final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> config) {
		checkNull(name, "Channel name cannot be null");
		checkNull(type, "Channel type cannot be null");
		checkNull(valueType, "Channel value type cannot be null");
		checkNull(config, "Channel configuration cannot be null");

		this.m_config = config;
		this.m_name = name;
		this.m_type = type;
		this.m_valueType = valueType;
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(final Channel otherChannel) {
		checkNull(otherChannel, "Provided channel to compare is null");
		return ComparisonChain.start().compare(this.m_name, otherChannel.getName())
				.compare(this.m_type, otherChannel.getType()).compare(this.m_valueType, otherChannel.getValueType())
				.result();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object otherChannel) {
		if (otherChannel instanceof Channel) {
			final Channel ch = (Channel) otherChannel;
			return Objects.equal(this.m_name, ch.getName()) && Objects.equal(this.m_type, ch.getType())
					&& Objects.equal(this.m_valueType, ch.getValueType());
		}
		return false;
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
		return Objects.hashCode(this.m_name, this.m_type, this.m_valueType, this.m_config);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", this.m_name).add("channel_type", this.m_type)
				.add("value_type", this.m_valueType).add("channel_configuration", this.m_config).toString();
	}

}
