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
 * The interface AssetHelperService is an utility service API to provide useful
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
	 * @return the new asset event
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public AssetEvent newAssetEvent(final AssetRecord assetRecord);

	/**
	 * Prepares new asset record.
	 *
	 * @param channelName
	 *            the channel name
	 * @return the new asset record
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public AssetRecord newAssetRecord(final String channelName);

	/**
	 * Prepares the new basic asset instance
	 *
	 * @return the newly created Base Asset instance
	 */
	public BaseAsset newBaseAsset();

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
	 * @return the newly created channel
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public Channel newChannel(final long id, final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> configuration);

	/**
	 * Prepares new driver event.
	 *
	 * @param driverRecord
	 *            the associated driver record
	 * @return the newly created driver event
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public DriverEvent newDriverEvent(final DriverRecord driverRecord);

	/**
	 * Prepares new driver record.
	 *
	 * @param channelName
	 *            the channel name
	 * @return the newly created driver record
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public DriverRecord newDriverRecord(final String channelName);

}
