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
package org.eclipse.kura.asset.cloudlet;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetHelperService;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.internal.BaseAsset;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.localization.AssetCloudletMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class AssetCloudlet is used to provide MQTT read/write operations on the
 * asset. The application id is configured as {@code Assetlet}.
 *
 * The available {@code GET} commands are as follows
 * <ul>
 * <li>/assets</li> : to retrieve all the assets
 * <li>/assets/asset_name</li> : to retrieve all the channels of the provided
 * asset name
 * <li>/assets/asset_name/channel_name</li> : to retrieve the value of the
 * specified channel from the provided asset name
 * </ul>
 *
 * The available {@code PUT} commands are as follows
 * <ul>
 * <li>/assets/asset_name/channel_name</li> : to write the provided
 * {@code value} in the payload to the specified channel of the provided asset
 * name. The payload must also include the {@code type} of the {@code value}
 * provided.
 * </ul>
 *
 * The {@code type} key in the request payload can be one of the following
 * (case-insensitive)
 * <ul>
 * <li>INTEGER</li>
 * <li>LONG</li>
 * <li>STRING</li>
 * <li>BOOLEAN</li>
 * <li>BYTE</li>
 * <li>SHORT</li>
 * <li>DOUBLE</li>
 * </ul>
 *
 * @see Cloudlet
 * @see CloudClient
 * @see AssetCloudlet#doGet(CloudletTopic, KuraRequestPayload,
 *      KuraResponsePayload)
 * @see AssetCloudlet#doPut(CloudletTopic, KuraRequestPayload,
 *      KuraResponsePayload)
 */
public final class AssetCloudlet extends Cloudlet {

	/** Application Identifier for Cloudlet. */
	private static final String APP_ID = "Assetlet";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(AssetCloudlet.class);

	/** Localization Resource */
	private static final AssetCloudletMessages s_message = LocalizationAdapter.adapt(AssetCloudletMessages.class);

	/** The Asset Helper Service instance. */
	private volatile AssetHelperService m_assetHelper;

	/** The map of assets present in the OSGi service registry. */
	private Map<String, Asset> m_assets;

	/** Asset Tracker Customizer */
	private AssetTrackerCustomizer m_assetTrackerCustomizer;

	/** Asset Tracker. */
	private ServiceTracker<Asset, Asset> m_serviceTracker;

	/**
	 * Instantiates a new asset cloudlet.
	 */
	public AssetCloudlet() {
		super(APP_ID);
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void activate(final ComponentContext componentContext) {
		s_logger.debug(s_message.activating());
		super.activate(componentContext);
		try {
			this.m_assetTrackerCustomizer = new AssetTrackerCustomizer(componentContext.getBundleContext());
			this.m_serviceTracker = new ServiceTracker<Asset, Asset>(componentContext.getBundleContext(),
					Asset.class.getName(), this.m_assetTrackerCustomizer);
			this.m_serviceTracker.open();
		} catch (final InvalidSyntaxException e) {
			s_logger.error(s_message.activationFailed(e));
		}
		s_logger.debug(s_message.activatingDone());
	}

	/**
	 * Binds the Asset Helper Service.
	 *
	 * @param assetHelperService
	 *            the new Asset Helper Service
	 */
	public synchronized void bindAssetHelperService(final AssetHelperService assetHelperService) {
		if (this.m_assetHelper == null) {
			this.m_assetHelper = assetHelperService;
		}
	}

	/**
	 * Cloud Service registration callback
	 *
	 * @param cloudService
	 *            the cloud service dependency
	 */
	protected synchronized void bindCloudService(final CloudService cloudService) {
		if (this.getCloudService() == null) {
			super.setCloudService(cloudService);
		}
	}

	/**
	 * Checks if the provided channel is present in the provided map
	 *
	 * @param channelName
	 *            the name of channel
	 * @param channels
	 *            the provided container of channels
	 * @return the id of the channel if found or else 0
	 * @throws KuraRuntimeException
	 *             any of the arguments is null or the provided is empty
	 */
	private long checkChannelAvailability(final String channelName, final Map<Long, Channel> channels) {
		checkNull(channelName, s_message.channelNameNonNull());
		checkNull(channels, s_message.channelsNonNull());
		checkCondition(channels.isEmpty(), s_message.channelsNonEmpty());

		for (final Map.Entry<Long, Channel> channel : channels.entrySet()) {
			final String chName = channel.getValue().getName();
			if (channelName.equals(chName)) {
				return channel.getKey();
			}
		}
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivating());
		super.deactivate(componentContext);
		super.setCloudService(null);
		this.m_serviceTracker.close();
		s_logger.debug(s_message.deactivatingDone());
	}

	/** {@inheritDoc} */
	@Override
	protected void doGet(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
			final KuraResponsePayload respPayload) throws KuraException {
		s_logger.info(s_message.cloudGETReqReceiving());
		if ("assets".equals(reqTopic.getResources()[0])) {
			// perform a search operation at the beginning
			this.findAssets();
			if (reqTopic.getResources().length == 1) {
				int index = 1;
				for (final Map.Entry<String, Asset> assetEntry : this.m_assets.entrySet()) {
					final Asset asset = assetEntry.getValue();
					respPayload.addMetric(String.valueOf(index++),
							((BaseAsset) asset).getAssetConfiguration().getName());
				}
			}
			// Checks if the name of the asset is provided
			if (reqTopic.getResources().length == 2) {
				final String assetName = reqTopic.getResources()[1];
				final Asset asset = this.m_assets.get(assetName);
				final AssetConfiguration configuration = ((BaseAsset) asset).getAssetConfiguration();
				final Map<Long, Channel> assetConfiguredChannels = configuration.getChannels();
				int index = 1;
				for (final Map.Entry<Long, Channel> entry : assetConfiguredChannels.entrySet()) {
					final Channel channel = entry.getValue();
					respPayload.addMetric(String.valueOf(index++), channel);
				}
			}
			// Checks if the name of the asset and the name of the channel are
			// provided
			if (reqTopic.getResources().length == 3) {
				final String assetName = reqTopic.getResources()[1];
				final String channelName = reqTopic.getResources()[2];
				final Asset asset = this.m_assets.get(assetName);
				final AssetConfiguration configuration = ((BaseAsset) asset).getAssetConfiguration();
				final Map<Long, Channel> assetConfiguredChannels = configuration.getChannels();
				final long id = this.checkChannelAvailability(channelName, assetConfiguredChannels);
				if ((assetConfiguredChannels != null) && (id != 0)) {
					final List<AssetRecord> assetRecords = asset.read(Arrays.asList(channelName));
					this.prepareResponse(respPayload, assetRecords);
				}
			}
		}
		s_logger.info(s_message.cloudGETReqReceived());
	}

	/** {@inheritDoc} */
	@Override
	protected void doPut(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
			final KuraResponsePayload respPayload) throws KuraException {
		s_logger.info(s_message.cloudPUTReqReceiving());
		// Checks if the name of the asset and the name of the channel are
		// provided
		if ("assets".equals(reqTopic.getResources()[0]) && (reqTopic.getResources().length > 2)) {
			// perform a search operation at the beginning
			this.findAssets();
			final String assetName = reqTopic.getResources()[1];
			final String channelName = reqTopic.getResources()[2];
			final Asset asset = this.m_assets.get(assetName);
			final AssetConfiguration configuration = ((BaseAsset) asset).getAssetConfiguration();
			final Map<Long, Channel> assetConfiguredChannels = configuration.getChannels();
			final long id = this.checkChannelAvailability(channelName, assetConfiguredChannels);
			if ((assetConfiguredChannels != null) && (id != 0)) {
				final AssetRecord assetRecord = this.m_assetHelper.newAssetRecord(channelName);
				final String userValue = (String) reqPayload.getMetric("value");
				final String userType = (String) reqPayload.getMetric("type");
				this.wrapValue(assetRecord, userValue, userType);
				final List<AssetRecord> assetRecords = asset.write(Arrays.asList(assetRecord));
				this.prepareResponse(respPayload, assetRecords);
			}
		}
		s_logger.info(s_message.cloudPUTReqReceived());
	}

	/**
	 * Searches for all the currently available assets in the service registry
	 */
	private void findAssets() {
		this.m_assets = this.m_assetTrackerCustomizer.getRegisteredAssets();
	}

	/**
	 * Prepares the response payload based on the asset records as provided
	 *
	 * @param respPayload
	 *            the response payload to prepare
	 * @param assetRecords
	 *            the list of asset records
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private void prepareResponse(final KuraResponsePayload respPayload, final List<AssetRecord> assetRecords) {
		checkNull(respPayload, s_message.respPayloadNonNull());
		checkNull(assetRecords, s_message.assetRecordsNonNull());

		for (final AssetRecord assetRecord : assetRecords) {
			respPayload.addMetric(s_message.flag(), assetRecord.getAssetFlag());
			respPayload.addMetric(s_message.timestamp(), assetRecord.getTimestamp());
			respPayload.addMetric(s_message.value(), assetRecord.getValue());
			respPayload.addMetric(s_message.channel(), assetRecord.getChannelName());
		}
	}

	/**
	 * Unbinds the Asset Helper Service.
	 *
	 * @param assetHelperService
	 *            the new Asset Helper Service
	 */
	public synchronized void unbindAssetHelperService(final AssetHelperService assetHelperService) {
		if (this.m_assetHelper == assetHelperService) {
			this.m_assetHelper = null;
		}
	}

	/**
	 * Cloud Service deregistration callback
	 *
	 * @param cloudService
	 *            the cloud service dependency
	 */
	protected synchronized void unbindCloudService(final CloudService cloudService) {
		if (this.getCloudService() == cloudService) {
			super.unsetCloudService(cloudService);
		}
	}

	/**
	 * Wraps the provided user provided value to the an instance of
	 * {@link TypedValue} in the asset record
	 *
	 * @param assetRecord
	 *            the asset record to contain the typed value
	 * @param userValue
	 *            the value to wrap
	 * @param userType
	 *            the type to use
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private void wrapValue(final AssetRecord assetRecord, final String userValue, final String userType) {
		checkNull(assetRecord, s_message.assetRecordNonNull());
		checkNull(userValue, s_message.valueNonNull());
		checkNull(userType, s_message.typeNonNull());

		TypedValue<?> value = null;
		if ("INTEGER".equalsIgnoreCase(userType)) {
			value = TypedValues.newIntegerValue(Integer.valueOf(userValue));
		}
		if ("BOOLEAN".equalsIgnoreCase(userType)) {
			value = TypedValues.newBooleanValue(Boolean.valueOf(userValue));
		}
		if ("BYTE".equalsIgnoreCase(userType)) {
			value = TypedValues.newByteValue(Byte.valueOf(userValue));
		}
		if ("DOUBLE".equalsIgnoreCase(userType)) {
			value = TypedValues.newDoubleValue(Double.valueOf(userValue));
		}
		if ("LONG".equalsIgnoreCase(userType)) {
			value = TypedValues.newLongValue(Long.valueOf(userValue));
		}
		if ("SHORT".equalsIgnoreCase(userType)) {
			value = TypedValues.newShortValue(Short.valueOf(userValue));
		}
		if ("STRING".equalsIgnoreCase(userType)) {
			value = TypedValues.newStringValue(userValue);
		}
		if (userValue != null) {
			assetRecord.setValue(value);
		}
	}

}
