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

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.ChannelType;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.type.DataType;

/**
 * The Class AssetServiceImpl is an implementation of the utility API
 * {@link AssetService} to provide useful factory methods for assets
 */
public final class AssetServiceImpl implements AssetService {

	/** The Driver Service instance. */
	private volatile DriverService m_driverService;

	/**
	 * Binds the Driver Service.
	 *
	 * @param driverService
	 *            the Driver Service instance
	 */
	public synchronized void bindDriverService(final DriverService driverService) {
		if (this.m_driverService == null) {
			this.m_driverService = driverService;
		}
	}
	
	/**
	 * Unbinds the Driver Service.
	 *
	 * @param driverService
	 *            the Driver Service instance
	 */
	public synchronized void unbindDriverService(final DriverService driverService) {
		if (this.m_driverService == driverService) {
			this.m_driverService = null;
		}
	}

	/** {@inheritDoc} */
	@Override
	public Asset newAsset() {
		return new AssetImpl(this, this.m_driverService);
	}

	/** {@inheritDoc} */
	@Override
	public AssetConfiguration newAssetConfiguration(final String name, final String description, final String driverId,
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
	public AssetRecord newAssetRecord(final long channelId) {
		return new AssetRecord(channelId);
	}

	/** {@inheritDoc} */
	@Override
	public Channel newChannel(final long id, final String name, final ChannelType type, final DataType valueType,
			final Map<String, Object> configuration) {
		return new Channel(id, name, type, valueType, configuration);
	}

}
