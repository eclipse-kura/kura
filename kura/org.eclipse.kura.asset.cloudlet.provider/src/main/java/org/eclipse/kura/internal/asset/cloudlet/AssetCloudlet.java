/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  
 *******************************************************************************/
package org.eclipse.kura.internal.asset.cloudlet;

import static java.util.Objects.requireNonNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetFlag;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.asset.AssetStatus;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetCloudletMessages;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class AssetCloudlet is used to provide MQTT read/write operations on the
 * asset. The application id is configured as {@code ASSET-V1}.
 *
 * The available {@code GET} commands are as follows
 * <ul>
 * <li>/assets</li> : to retrieve all the assets
 * <li>/assets/asset_pid</li> : to retrieve all the channels of the provided
 * asset PID
 * <li>/assets/asset_pid/channel_id</li> : to retrieve the value of the
 * specified channel from the provided asset PID
 * <li>/assets/asset_pid/channel_id1-channel_id2-channel_id3</li> : to retrieve
 * the value of the several channels from the provided asset PID. Any number of
 * channels can be provided as well. Also note that {@code "-"} delimiter must
 * be used to separate the channel IDs.
 * </ul>
 *
 * The available {@code PUT} commands are as follows
 * <ul>
 * <li>/assets/asset_pid/channel_id</li> : to write the provided {@code value}
 * in the payload to the specified channel of the provided asset PID. The
 * payload must also include the {@code type} of the {@code value} provided.
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
 * The {@code value} key in the request payload must contain the value to be
 * written
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
    private static final String APP_ID = "ASSET-V1";

    private static final Logger logger = LoggerFactory.getLogger(AssetCloudlet.class);

    private static final AssetCloudletMessages message = LocalizationAdapter.adapt(AssetCloudletMessages.class);

    /** The map of assets present in the OSGi service registry. */
    private Map<String, Asset> assets;

    private volatile AssetService assetService;

    private AssetTrackerCustomizer assetTrackerCustomizer;

    private ServiceTracker<Asset, Asset> assetServiceTracker;

    public AssetCloudlet() {
        super(APP_ID);
    }

    /**
     * Asset Service registration callback
     *
     * @param assetService
     *            the asset service dependency
     */
    protected synchronized void bindAssetService(final AssetService assetService) {
        if (this.assetService == null) {
            this.assetService = assetService;
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
     * Asset Service deregistration callback
     *
     * @param assetService
     *            the asset service dependency
     */
    protected synchronized void unbindAssetService(final AssetService assetService) {
        if (this.assetService == assetService) {
            this.assetService = null;
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

    /** {@inheritDoc} */
    @Override
    protected synchronized void activate(final ComponentContext componentContext) {
        logger.debug(message.activating());
        super.activate(componentContext);
        try {
            this.assetTrackerCustomizer = new AssetTrackerCustomizer(componentContext.getBundleContext(),
                    this.assetService);
            this.assetServiceTracker = new ServiceTracker<Asset, Asset>(componentContext.getBundleContext(),
                    Asset.class.getName(), this.assetTrackerCustomizer);
            this.assetServiceTracker.open();
        } catch (final InvalidSyntaxException e) {
            logger.error(message.activationFailed(e));
        }
        logger.debug(message.activatingDone());
    }

    /** {@inheritDoc} */
    @Override
    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivating());
        super.deactivate(componentContext);
        this.assetServiceTracker.close();
        logger.debug(message.deactivatingDone());
    }

    /** {@inheritDoc} */
    @Override
    protected void doGet(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
            final KuraResponsePayload respPayload) {
        logger.info(message.cloudGETReqReceiving());
        if ("assets".equals(reqTopic.getResources()[0])) {
            // perform a search operation at the beginning
            this.findAssets();
            if (reqTopic.getResources().length == 1) {
                this.getAllAssets(respPayload);
            }
            // Checks if the name of the asset is provided
            if (reqTopic.getResources().length == 2) {
                final String assetPid = reqTopic.getResources()[1];
                this.getAllChannelsByAssetPid(respPayload, assetPid);
            }
            // Checks if the name of the asset and the name of the channel are
            // provided
            if (reqTopic.getResources().length == 3) {
                final String assetPid = reqTopic.getResources()[1];
                final String channelId = reqTopic.getResources()[2];
                this.readChannelsByIds(respPayload, assetPid, channelId);
            }
        }
        logger.info(message.cloudGETReqReceived());
    }

    /**
     * Get all assets
     *
     * @param respPayload
     *            the response to prepare
     * @throws NullPointerException
     *             if the argument is null
     */
    private void getAllAssets(final KuraResponsePayload respPayload) {
        requireNonNull(respPayload, message.respPayloadNonNull());
        int i = 0;
        for (final Map.Entry<String, Asset> assetEntry : this.assets.entrySet()) {
            respPayload.addMetric(String.valueOf(++i), assetEntry.getKey());
        }
    }

    /**
     * Get all channels by the provided Asset PID
     *
     * @param respPayload
     *            the response to prepare
     * @param assetPid
     *            the provided Asset PID
     * @throws NullPointerException
     *             if any of the argument is null
     */
    private void getAllChannelsByAssetPid(final KuraResponsePayload respPayload, final String assetPid) {
        requireNonNull(respPayload, message.respPayloadNonNull());
        requireNonNull(assetPid, message.assetPidNonNull());

        final Asset asset = this.assets.get(assetPid);
        final AssetConfiguration configuration = asset.getAssetConfiguration();
        final Map<Long, Channel> assetConfiguredChannels = configuration.getAssetChannels();
        for (final Map.Entry<Long, Channel> entry : assetConfiguredChannels.entrySet()) {
            final Channel channel = entry.getValue();
            respPayload.addMetric(String.valueOf(channel.getId()), channel.getName());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doPut(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
            final KuraResponsePayload respPayload) {
        logger.info(message.cloudPUTReqReceiving());
        // Checks if the name of the asset and the name of the channel are
        // provided
        final String[] resources = reqTopic.getResources();
        if ("assets".equalsIgnoreCase(resources[0]) && (resources.length > 2)) {
            doPutAssets(reqPayload, respPayload, resources);
        }
        logger.info(message.cloudPUTReqReceived());
    }

    /**
     * Put operation specific for assets
     * 
     * @param reqPayload
     *            a KuraRequestPayload
     * @param respPayload
     *            a KuraResponsePayload
     * @param resources
     *            String array representing the topic chunks
     */
    private void doPutAssets(final KuraRequestPayload reqPayload, final KuraResponsePayload respPayload,
            final String[] resources) {
        this.findAssets();
        final String assetPid = resources[1];
        final String channelId = resources[2];
        final Asset asset = this.assets.get(assetPid);
        final AssetConfiguration configuration = asset.getAssetConfiguration();
        final Map<Long, Channel> assetConfiguredChannels = configuration.getAssetChannels();
        final long id = Long.parseLong(channelId);
        if ((assetConfiguredChannels != null) && (id != 0)) {
            final AssetRecord assetRecord = new AssetRecord(id);
            final String userValue = (String) reqPayload.getMetric("value");
            final String userType = (String) reqPayload.getMetric("type");
            boolean flag = true;
            try {
                this.wrapValue(assetRecord, userValue, userType);
            } catch (final NumberFormatException nfe) {
                flag = false;
                assetRecord.setAssetStatus(new AssetStatus(AssetFlag.FAILURE, message.valueTypeConversionError(), nfe));
                assetRecord.setTimestamp(System.currentTimeMillis());
            }

            List<AssetRecord> assetRecords = Arrays.asList(assetRecord);
            try {
                if (flag) {
                    assetRecords = asset.write(assetRecords);
                }
            } catch (final KuraException e) {
                // if connection exception occurs
                respPayload.addMetric(message.errorMessage(), message.connectionException());
            }
            this.prepareResponse(respPayload, assetRecords);
        }
    }

    /**
     * Searches for all the currently available assets in the service registry
     */
    private void findAssets() {
        this.assets = this.assetTrackerCustomizer.getRegisteredAssets();
    }

    /**
     * Read channels based on provided Asset PID
     *
     * @param respPayload
     *            the response paylaod to be prepared
     * @param assetPid
     *            the Asset PID
     * @param channelId
     *            the channel ID (might contain {@code -} for multiple reads)
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    private void readChannelsByIds(final KuraResponsePayload respPayload, final String assetPid,
            final String channelId) {
        requireNonNull(respPayload, message.respPayloadNonNull());
        requireNonNull(assetPid, message.assetPidNonNull());
        requireNonNull(channelId, message.channelIdNonNull());

        final String channelDelim = ",";
        Set<String> channelIds = null;
        if (channelId.contains(channelDelim)) {
            channelIds = CollectionUtil.newHashSet(Arrays.asList(channelId.split(channelDelim)));
            channelIds.removeAll(Collections.singleton(""));
        }
        final Asset asset = this.assets.get(assetPid);
        final AssetConfiguration configuration = asset.getAssetConfiguration();
        final Map<Long, Channel> assetConfiguredChannels = configuration.getAssetChannels();

        final List<Long> channelIdsToRead = CollectionUtil.newArrayList();
        long id;
        if (channelIds == null) {
            id = Long.parseLong(channelId);
            channelIdsToRead.add(id);
        } else {
            for (final String chId : channelIds) {
                id = Long.parseLong(chId);
                channelIdsToRead.add(id);
            }
        }
        if (assetConfiguredChannels != null) {
            List<AssetRecord> assetRecords = null;
            try {
                assetRecords = asset.read(channelIdsToRead);
            } catch (final KuraException e) {
                // if connection exception occurs
                logger.warn(message.connectionException() + e);
                respPayload.addMetric(message.errorMessage(), message.connectionException());
            }
            if (assetRecords != null) {
                this.prepareResponse(respPayload, assetRecords);
            }
        }
    }

    /**
     * Prepares the response payload based on the asset records as provided
     *
     * @param respPayload
     *            the response payload to prepare
     * @param assetRecords
     *            the list of asset records
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    private void prepareResponse(final KuraResponsePayload respPayload, final List<AssetRecord> assetRecords) {
        requireNonNull(respPayload, message.respPayloadNonNull());
        requireNonNull(assetRecords, message.assetRecordsNonNull());

        for (final AssetRecord assetRecord : assetRecords) {
            final TypedValue<?> assetValue = assetRecord.getValue();
            final String value = (assetValue != null) ? String.valueOf(assetValue.getValue()) : "ERROR";
            String errorText;
            final AssetStatus assetStatus = assetRecord.getAssetStatus();
            final AssetFlag assetFlag = assetStatus.getAssetFlag();

            final String prefix = assetRecord.getChannelId() + ".";
            respPayload.addMetric(prefix + message.flag(), assetFlag.toString());
            respPayload.addMetric(prefix + message.channel(), assetRecord.getChannelId());
            respPayload.addMetric(prefix + message.timestamp(), assetRecord.getTimestamp());
            respPayload.addMetric(prefix + message.value(), value);

            if (assetFlag == AssetFlag.FAILURE) {
                final String exceptionMessage = assetStatus.getExceptionMessage();
                errorText = (exceptionMessage != null) ? exceptionMessage : "";
                respPayload.addMetric(prefix + message.errorMessage(), errorText);
            }
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
     * @throws NullPointerException
     *             if any of the provided arguments is null
     * @throws NumberFormatException
     *             if the provided value cannot be parsed
     */
    private void wrapValue(final AssetRecord assetRecord, final String userValue, final String userType) {
        requireNonNull(assetRecord, message.assetRecordNonNull());
        requireNonNull(userValue, message.valueNonNull());
        requireNonNull(userType, message.typeNonNull());

        TypedValue<?> value = null;
        try {
            if ("INTEGER".equalsIgnoreCase(userType)) {
                value = TypedValues.newIntegerValue(Integer.parseInt(userValue));
            }
            if ("BOOLEAN".equalsIgnoreCase(userType)) {
                value = TypedValues.newBooleanValue(Boolean.parseBoolean(userValue));
            }
            if ("BYTE".equalsIgnoreCase(userType)) {
                value = TypedValues.newByteValue(Byte.parseByte(userValue));
            }
            if ("DOUBLE".equalsIgnoreCase(userType)) {
                value = TypedValues.newDoubleValue(Double.parseDouble(userValue));
            }
            if ("LONG".equalsIgnoreCase(userType)) {
                value = TypedValues.newLongValue(Long.parseLong(userValue));
            }
            if ("SHORT".equalsIgnoreCase(userType)) {
                value = TypedValues.newShortValue(Short.parseShort(userValue));
            }
            if ("STRING".equalsIgnoreCase(userType)) {
                value = TypedValues.newStringValue(userValue);
            }
        } catch (final NumberFormatException nfe) {
            throw nfe;
        }

        assetRecord.setValue(value);
    }
}