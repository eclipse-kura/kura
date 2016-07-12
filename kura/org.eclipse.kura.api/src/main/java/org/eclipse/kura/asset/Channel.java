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
package org.eclipse.kura.asset;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.DataType;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The Class Channel represents a communication channel of an asset. The
 * communication channel has all the required configuration to perform specific
 * operation (read/write/monitor).
 */
@NotThreadSafe
public final class Channel {

	/** The communication channel configuration. */
	private final Map<String, Object> configuration;

	/** The unique identifier of the channel */
	private final long id;

	/** The name of the communication channel. */
	private String name;

	/** The type of the channel. */
	private ChannelType type;

	/**
	 * The data type of the value as expected for the operations
	 * (read/write/monitor).
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
	 *             if any of the arguments is null
	 */
	public Channel(final long id, final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> config) {
		checkCondition(id == 0, "Channel ID cannot be 0");
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
	public Map<String, Object> getConfiguration() {
		return this.configuration;
	}

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
		return Objects.hashCode(this.name, this.type, this.valueType, this.configuration);
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
		return MoreObjects.toStringHelper(this).add("ID", this.id).add("name", this.name).add("channel_type", this.type)
				.add("value_type", this.valueType).add("channel_configuration", this.configuration).toString();
	}

}
