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

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.type.DataType;

/**
 * The Class AssetConfiguration is responsible for storing the configuration for
 * an industrial device (also known as Asset in the context of Eclipse Kura).
 *
 * The properties as provided to an Asset must conform to the following
 * specifications. The properties must have the following.
 *
 * <ul>
 * <li>the value associated with <b><i>driver.id</i></b> key in the map denotes
 * the driver instance name to be consumed by this asset</li>
 * <li>A value associated with key <b><i>asset.name</i></b> must be present to
 * denote the asset name</li>
 * <li>A value associated with <b><i>asset.desc</i></b> key denotes the asset
 * description</li>
 * <li>x.CH.[property]</li> where x is any number denoting the channel's unique
 * ID and the {@code [property]} denotes the protocol specific properties. (Note
 * that the format includes at least two ".") denotes map object containing a
 * channel configuration</li>
 *
 * For example, 1.CH.name, 1.CH.value.type etc.
 *
 * The representation in the provided properties as prepended by a number
 * signifies a single channel and it should conform to the following
 * specification.
 *
 * The properties should contain the following keys
 * <ul>
 * <li>name</li>
 * <li>type</li>
 * <li>value.type</li>
 * <li>[more configuration]</li> as mentioned by the driver in the format which
 * begins with <b><i>DRIVER.</i></b>
 * </ul>
 *
 * For example, [more configuration] would be 1.CH.DRIVER.modbus.register,
 * 1.CH.DRIVER.modbus.unit.id etc.
 *
 * The key <b><i>name</i></b> must be String.
 *
 * The key <b><i>value.type</i></b> must be in one of types from
 * {@link DataType} in String representation format (case-insensitive)
 *
 * The channel {@code type} should be one of the types from {@link ChannelType}
 * in String representation format (case-insensitive)
 *
 * @see Channel
 * @see ChannelType
 * @see DataType
 */
@NotThreadSafe
public final class AssetConfiguration {

	/**
	 * The list of channels associated with this asset. The association denotes
	 * channel ID and its actual object reference pair.
	 */
	private final Map<Long, Channel> channels;

	/** the asset description. */
	private String description;

	/** the driver ID as associated with this asset. */
	private final String driverId;

	/** the name of the asset. */
	private String name;

	/**
	 * Instantiates a new asset configuration.
	 *
	 * @param name
	 *            the name of the asset
	 * @param description
	 *            the description of the asset
	 * @param driverId
	 *            the driver id
	 * @param channels
	 *            the map of all channel configurations
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public AssetConfiguration(final String name, final String description, final String driverId,
			final Map<Long, Channel> channels) {
		checkNull(description, "Asset name cannot be null");
		checkNull(description, "Asset description cannot be null");
		checkNull(description, "Asset driver ID cannot be null");
		checkNull(description, "Asset channel configurations cannot be null");

		this.description = description;
		this.driverId = driverId;
		this.name = name;
		this.channels = channels;
	}

	/**
	 * Gets the channels.
	 *
	 * @return the channels
	 */
	public Map<Long, Channel> getChannels() {
		return this.channels;
	}

	/**
	 * Gets the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Gets the driver id.
	 *
	 * @return the driver id
	 */
	public String getDriverId() {
		return this.driverId;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the description.
	 *
	 * @param description
	 *            the new description
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void setDescription(final String description) {
		checkNull(description, "Asset description cannot be null");
		this.description = description;
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
		checkNull(this.description, "Asset name cannot be null");
		this.name = name;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "AssetConfiguration [channels=" + this.channels + ", description=" + this.description + ", driverId="
				+ this.driverId + ", name=" + this.name + "]";
	}
}
