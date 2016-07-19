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
package org.eclipse.kura.internal.asset;

import java.util.Map;

import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetHelperService;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.BaseAsset;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.asset.DriverEvent;
import org.eclipse.kura.asset.DriverRecord;
import org.eclipse.kura.type.DataType;
import org.osgi.service.component.ComponentContext;

/**
 * The Class AssetHelperServiceImpl is an implementation of the utility API
 * AssetHelperService to provide useful static factory methods for asset and
 * drivers
 */
public final class AssetHelperServiceImpl implements AssetHelperService {

	/** {@inheritDoc} */
	@Override
	public AssetConfiguration newAssetConfigruation(final String name, final String description, final String driverId,
			final Map<Long, Channel> channels) {
		return new AssetConfiguration(name, description, driverId, channels);
	}

	/** {@inheritDoc} */
	@Override
	public AssetEvent newAssetEvent(final AssetRecord assetRecord) {
		return new AssetEvent(assetRecord);
	}

	/** {@inheritDoc} */
	@Override
	public AssetRecord newAssetRecord(final String channelName) {
		return new AssetRecord(channelName);
	}

	/** {@inheritDoc} */
	@Override
	public BaseAsset newBaseAsset(final ComponentContext context, final AssetHelperService assetHelperService) {
		return new BaseAssetImpl(context, assetHelperService);
	}

	/** {@inheritDoc} */
	@Override
	public Channel newChannel(final long id, final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> configuration) {
		return new Channel(id, name, type, valueType, configuration);
	}

	/** {@inheritDoc} */
	@Override
	public DriverEvent newDriverEvent(final DriverRecord driverRecord) {
		return new DriverEvent(driverRecord);
	}

	/** {@inheritDoc} */
	@Override
	public DriverRecord newDriverRecord(final String channelName) {
		return new DriverRecord(channelName);
	}

}
