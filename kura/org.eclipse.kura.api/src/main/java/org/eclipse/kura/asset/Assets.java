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

import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.type.DataType;

/**
 * The Class Assets is an utility class to provide useful static factory methods
 * for asset and drivers
 */
public final class Assets {
	
	/** Constructor */
	private Assets() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Prepares a new asset configuration.
	 *
	 * @param name
	 *            the name of the asset
	 * @param description
	 *            the description of the asset
	 * @param driverId
	 *            the driver id
	 * @param channels
	 *            the map of all channel configurations
	 * @return the asset configuration
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public static AssetConfiguration newAssetConfigruation(final String name, final String description,
			final String driverId, final Map<Long, Channel> channels) {
		return new AssetConfiguration(name, description, driverId, channels);
	}

	/**
	 * Prepares new asset event.
	 *
	 * @param assetRecord
	 *            the associated asset record
	 * @return the asset event
	 */
	public static AssetEvent newAssetEvent(final AssetRecord assetRecord) {
		return new AssetEvent(assetRecord);
	}

	/**
	 * Prepares new asset record.
	 *
	 * @param channelName
	 *            the channel name
	 * @return the asset record
	 */
	public static AssetRecord newAssetRecord(final String channelName) {
		return new AssetRecord(channelName);
	}

	/**
	 * Creates a new channel with the provided values
	 *
	 * @param id
	 *            the ID of the channel
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
	public static Channel newChannel(final long id, final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> configuration) {
		return new Channel(id, name, type, valueType, configuration);
	}

	/**
	 * Prepares new driver event.
	 *
	 * @param driverRecord
	 *            the associated driver record
	 * @return the driver event
	 */
	public static DriverEvent newDriverEvent(final DriverRecord driverRecord) {
		return new DriverEvent(driverRecord);
	}

	/**
	 * Prepares new driver record.
	 *
	 * @param channelName
	 *            the channel name
	 * @return the driver record
	 */
	public static DriverRecord newDriverRecord(final String channelName) {
		return new DriverRecord(channelName);
	}

}
