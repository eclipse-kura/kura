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
package org.eclipse.kura.asset.internal;

import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.AssetEvent;
import org.eclipse.kura.asset.AssetFlag;
import org.eclipse.kura.asset.AssetListener;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.Assets;
import org.eclipse.kura.asset.DriverEvent;
import org.eclipse.kura.asset.DriverFlag;
import org.eclipse.kura.asset.DriverListener;
import org.eclipse.kura.asset.DriverRecord;
import org.eclipse.kura.localization.AssetMessages;
import org.eclipse.kura.localization.LocalizationAdapter;

/**
 * This is a basic driver listener used to listen for driver events so that it
 * can be propagated upwards to the respective asset listener
 *
 * @see AssetListener
 * @see DriverListener
 * @see AssetEvent
 * @see DriverEvent
 */
final class BaseDriverListener implements DriverListener {

	/** Localization Resource */
	private static final AssetMessages s_message = LocalizationAdapter.adapt(AssetMessages.class);

	/** The asset listener instance. */
	private final AssetListener m_assetListener;

	/** The channel name. */
	private final String m_channelName;

	/**
	 * Instantiates a new base driver listener.
	 *
	 * @param channelName
	 *            the channel name as provided
	 * @param assetListener
	 *            the asset listener
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	BaseDriverListener(final String channelName, final AssetListener assetListener) {
		checkNull(channelName, s_message.channelNameNonNull());
		checkNull(assetListener, s_message.listenerNonNull());

		this.m_channelName = channelName;
		this.m_assetListener = assetListener;
	}

	/** {@inheritDoc} */
	@Override
	public void onDriverEvent(final DriverEvent event) {
		checkNull(event, s_message.driverEventNonNull());
		final DriverRecord driverRecord = event.getDriverRecord();
		final AssetRecord assetRecord = Assets.newAssetRecord(this.m_channelName);
		final DriverFlag driverFlag = driverRecord.getDriverFlag();

		switch (driverFlag) {
		case READ_SUCCESSFUL:
			assetRecord.setAssetFlag(AssetFlag.READ_SUCCESSFUL);
			break;
		case WRITE_SUCCESSFUL:
			assetRecord.setAssetFlag(AssetFlag.WRITE_SUCCESSFUL);
			break;
		case DRIVER_ERROR_UNSPECIFIED:
			assetRecord.setAssetFlag(AssetFlag.ASSET_ERROR_UNSPECIFIED);
			break;
		case UNKNOWN:
			assetRecord.setAssetFlag(AssetFlag.UNKNOWN);
			break;
		default:
			break;
		}
		assetRecord.setTimestamp(driverRecord.getTimestamp());
		assetRecord.setValue(driverRecord.getValue());
		final AssetEvent assetEvent = Assets.newAssetEvent(assetRecord);
		this.m_assetListener.onAssetEvent(assetEvent);
	}
}