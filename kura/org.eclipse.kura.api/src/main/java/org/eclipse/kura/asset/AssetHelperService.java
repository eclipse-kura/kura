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
 * The interface AssetHelperService is an utility service to provide useful
 * methods for asset and drivers
 */
public interface AssetHelperService {

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
	public AssetConfiguration newAssetConfigruation(final String name, final String description, final String driverId,
			final Map<Long, Channel> channels);

	/**
	 * Prepares new asset event.
	 *
	 * @param assetRecord
	 *            the associated asset record
	 * @return the asset event
	 */
	public AssetEvent newAssetEvent(final AssetRecord assetRecord);

	/**
	 * Prepares new asset record.
	 *
	 * @param channelName
	 *            the channel name
	 * @return the asset record
	 */
	public AssetRecord newAssetRecord(final String channelName);

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
	public Channel newChannel(final long id, final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> configuration);

	/**
	 * Prepares new driver event.
	 *
	 * @param driverRecord
	 *            the associated driver record
	 * @return the driver event
	 */
	public DriverEvent newDriverEvent(final DriverRecord driverRecord);

	/**
	 * Prepares new driver record.
	 *
	 * @param channelName
	 *            the channel name
	 * @return the driver record
	 */
	public DriverRecord newDriverRecord(final String channelName);

}
