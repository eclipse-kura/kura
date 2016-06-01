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
	private final Map<String, Object> configuration;

	/** The name of the communication channel. */
	private final String name;

	/** The type of the channel. */
	private final ChannelType type;

	/**
	 * The data type of the value as expected for the operations
	 * (read/write/monitor).
	 */
	private final DataType valueType;

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

		this.configuration = config;
		this.name = name;
		this.type = type;
		this.valueType = valueType;
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(final Channel otherChannel) {
		checkNull(otherChannel, "Provided channel to compare is null");
		return ComparisonChain.start().compare(this.name, otherChannel.getName())
				.compare(this.type, otherChannel.getType()).compare(this.valueType, otherChannel.getValueType())
				.result();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object otherChannel) {
		if (otherChannel instanceof Channel) {
			final Channel ch = (Channel) otherChannel;
			return Objects.equal(this.name, ch.getName()) && Objects.equal(this.type, ch.getType())
					&& Objects.equal(this.valueType, ch.getValueType());
		}
		return false;
	}

	/**
	 * Gets the configuration of the communication channel.
	 *
	 * @return the configuration of the communication channel
	 */
	public Map<String, Object> getConfig() {
		return this.configuration;
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
		return Objects.hashCode(this.name, this.type, this.valueType, this.configuration);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", this.name).add("channel_type", this.type)
				.add("value_type", this.valueType).add("channel_configuration", this.configuration).toString();
	}

}
