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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
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
 * <b>/assets</b><br>
 * Retrieves the list of all assets available on this device.
 * <b>/assets/${asset_name}</b><br>
 * Can be used to perform the following operations on the Asset named ${asset_name},
 * depending on the value of the <b>channel.name</b> metric in the payload:
 * <ul>
 * <li>If the <b>channel.name</b> metric is not present, the metadata for all the defined channels will be returned.
 * The payload will contain, for each channel, the following two metrics:<br>
 * 
 * {@code channel.name}_type = {@code channel.value.type}<br>
 * {@code channel.name}_mode = {@code channel.mode}<br>
 * 
 * where {@code channel.name} is the name of the channel, {@code channel.value.type} and {@code channel.mode} are
 * respectively the channel value type and mode encoded as described below.
 * </li>
 * <li>
 * If a metric named <b>channel.name</b> of String type is present in the payload and its value contains
 * the <b>#</b> character, the values of all channels will be returned. The format of the response is
 * described below.
 * </li>
 * <li>
 * If a metric named <b>channel.name</b> of String type is present in the payload and its value is a
 * comma separated list of channel names, the values for the specified channels will be returned.
 * </li>
 * </ul>
 * <br>
 * 
 * The available {@code PUT} commands are the following:<br>
 * <b>/assets/${asset_name}</b></br>
 * Performs a write operation on a specific set of channels.
 * 
 * A metric with the following structure must be present in the request payload for each channel to be written:<br>
 * 
 * {@code channel.name}={@code value}<br>
 * 
 * where {@code channel.name} is the name of the channel and {@code value} is the value to be written, the type of
 * {@code value} must match the channel value type.
 *
 * The metrics available in the response payload for read or write operations are described below.
 * <ul>
 * <li>
 * <b>{@code channel.name}_value</b> contains the value that has been read/written from/to the channel. The type of this
 * metric depends on the channel value type. This metric is present if and only if the requested operation completed
 * successfully.
 * </li>
 * <li>
 * <b>{@code channel.name}_error</b> is a String metric that reports an error message. This metric is present if and
 * only if the requested operation failed.
 * </li>
 * <li>
 * <b>{@code channel.name}_timestamp</b> is a Long metric that reports a device timestamp,
 * obtained when the request completed.
 * The value of the timestamp is the number of milliseconds since Unix Epoch.
 * </li>
 * </ul>
 * 
 *
 * The channel value type is encoded as a String as follows:
 * <ul>
 * <li>INTEGER</li>
 * <li>LONG</li>
 * <li>STRING</li>
 * <li>BOOLEAN</li>
 * <li>FLOAT</li>
 * <li>DOUBLE</li>
 * </ul>
 *
 * The channel mode is encoded as a String as follows:
 * <ul>
 * <li>READ</li>
 * <li>WRITE</li>
 * <li>READ_WRITE</li>
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

    private static final String APP_ID = "ASSET-V1";

    private static final String CHANNEL_NAME_METRIC_NAME = "channel.name";

    private static final String TIMESTAMP_METRIC_SUFFIX = "timestamp";
    private static final String VALUE_METRIC_SUFFIX = "value";
    private static final String ERROR_METRIC_SUFFIX = "error";

    private static final String CHANNEL_TYPE_SUFFIX = "type";
    private static final String CHANNEL_MODE_SUFFIX = "mode";

    private static final String ALL_CHANNELS_WILDCARD = "#";
    private static final String REQUEST_CHANNEL_NAME_DELIMITER = ",";

    private static final String RESPONSE_METRIC_DELIMITER = "_";

    private static final Logger logger = LoggerFactory.getLogger(AssetCloudlet.class);

    private static final AssetCloudletMessages message = LocalizationAdapter.adapt(AssetCloudletMessages.class);

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

    private String extractChannelName(final KuraRequestPayload reqPayload) {
        Object channelName = reqPayload.getMetric(CHANNEL_NAME_METRIC_NAME);
        if (channelName == null || !(channelName instanceof String)) {
            return null;
        }
        return (String) channelName;
    }

    /** {@inheritDoc} */
    @Override
    protected void doGet(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
            final KuraResponsePayload respPayload) {
        final String[] resources = reqTopic.getResources();

        logger.info(message.cloudGETReqReceiving());
        if ("assets".equals(resources[0])) {
            // perform a search operation at the beginning
            this.findAssets();
            if (resources.length == 1) {
                this.getAllAssets(respPayload);
                return;
            }
            // Checks if the name of the asset is provided
            if (resources.length != 2) {
                respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
                return;
            }

            final Asset asset = this.assets.get(resources[1].trim());
            if (asset == null) {
                respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
                return;
            }

            final String channelName = extractChannelName(reqPayload);
            if (channelName == null) {
                getChannelMetadata(respPayload, asset);
            } else if (channelName.indexOf(ALL_CHANNELS_WILDCARD) != -1) {
                readAllChannels(respPayload, asset);
            } else {
                readChannels(respPayload, asset, channelName);
            }

        } else {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
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
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
    }

    private void getChannelMetadata(final KuraResponsePayload respPayload, final Asset asset) {
        for (final Map.Entry<String, Channel> entry : asset.getAssetConfiguration().getAssetChannels().entrySet()) {
            final Channel channel = entry.getValue();
            String channelName = entry.getValue().getName();

            respPayload.addMetric(channelName + RESPONSE_METRIC_DELIMITER + CHANNEL_TYPE_SUFFIX,
                    channel.getValueType().toString());
            respPayload.addMetric(channelName + RESPONSE_METRIC_DELIMITER + CHANNEL_MODE_SUFFIX,
                    channel.getType().toString());
        }
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
    }

    /** {@inheritDoc} */
    @Override
    protected void doPut(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
            final KuraResponsePayload respPayload) {
        logger.info(message.cloudPUTReqReceiving());
        // Checks if the name of the asset and the name of the channel are
        // provided
        final String[] resources = reqTopic.getResources();
        if ("assets".equalsIgnoreCase(resources[0]) && (resources.length > 1)) {
            doPutAssets(reqPayload, respPayload, resources);
        }
        logger.info(message.cloudPUTReqReceived());
    }

    private void encodeSuccess(KuraResponsePayload response, String channelName, long timestamp, Object value) {
        response.addMetric(channelName + RESPONSE_METRIC_DELIMITER + TIMESTAMP_METRIC_SUFFIX, timestamp);
        response.addMetric(channelName + RESPONSE_METRIC_DELIMITER + VALUE_METRIC_SUFFIX, value);
    }

    private void encodeFailure(KuraResponsePayload response, String channelName, long timestamp, String reason) {
        response.addMetric(channelName + RESPONSE_METRIC_DELIMITER + TIMESTAMP_METRIC_SUFFIX, timestamp);
        response.addMetric(channelName + RESPONSE_METRIC_DELIMITER + ERROR_METRIC_SUFFIX, reason);
    }

    private void encodeFailure(KuraResponsePayload response, Iterator<String> iterator, long timestamp, String reason) {
        while (iterator.hasNext()) {
            String channelName = iterator.next();
            response.addMetric(channelName + RESPONSE_METRIC_DELIMITER + TIMESTAMP_METRIC_SUFFIX, timestamp);
            response.addMetric(channelName + RESPONSE_METRIC_DELIMITER + ERROR_METRIC_SUFFIX, reason);
        }
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

        final Asset asset = this.assets.get(resources[1].trim());
        if (asset == null) {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
            return;
        }

        final ArrayList<ChannelRecord> writeRecords = new ArrayList<>();

        for (Entry<String, Object> entry : reqPayload.metrics().entrySet()) {

            if (KuraRequestPayload.METRIC_REQUEST_ID.equals(entry.getKey())
                    || KuraRequestPayload.REQUESTER_CLIENT_ID.equals(entry.getKey())) {
                continue;
            }

            try {
                writeRecords.add(
                        ChannelRecord.createWriteRecord(entry.getKey(), TypedValues.newTypedValue(entry.getValue())));
            } catch (Exception e) {
                encodeFailure(respPayload, entry.getKey(), System.currentTimeMillis(),
                        message.valueTypeConversionError());
            }
        }

        try {
            asset.write(writeRecords);
            this.prepareResponse(respPayload, writeRecords);
        } catch (final Exception e) {
            final String exceptionMessage = Optional.of(e.getMessage()).orElse(message.unknownError());
            encodeFailure(respPayload, writeRecords.stream().map((record) -> record.getChannelName()).iterator(),
                    System.currentTimeMillis(), exceptionMessage);
        }
    }

    /**
     * Searches for all the currently available assets in the service registry
     */
    private void findAssets() {
        this.assets = this.assetTrackerCustomizer.getRegisteredAssets();
    }

    private void readChannels(final KuraResponsePayload respPayload, final Asset asset, final String channelName) {

        Set<String> channelNames = Arrays.stream(channelName.split(REQUEST_CHANNEL_NAME_DELIMITER))
                .map((name) -> name.trim()).filter((name) -> !name.isEmpty()).collect(Collectors.toSet());

        if (channelNames.isEmpty()) {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        try {
            this.prepareResponse(respPayload, asset.read(channelNames));
        } catch (final Exception e) {
            final String exceptionMessage = Optional.of(e.getMessage()).orElse(message.unknownError());
            encodeFailure(respPayload, channelNames.iterator(), System.currentTimeMillis(), exceptionMessage);
        }
    }

    private void readAllChannels(final KuraResponsePayload respPayload, final Asset asset) {
        try {
            this.prepareResponse(respPayload, asset.readAllChannels());
        } catch (final Exception e) {
            final Map<String, Channel> channels = asset.getAssetConfiguration().getAssetChannels();
            final String exceptionMessage = Optional.of(e.getMessage()).orElse(message.unknownError());
            encodeFailure(respPayload, channels.keySet().iterator(), System.currentTimeMillis(), exceptionMessage);
        }
    }

    private void prepareResponse(final KuraResponsePayload respPayload,
            final List<? extends ChannelRecord> channelRecords) {

        if (channelRecords == null) {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            return;
        }

        for (final ChannelRecord channelRecord : channelRecords) {
            final ChannelStatus channelStatus = channelRecord.getChannelStatus();
            final ChannelFlag channelFlag = channelStatus.getChannelFlag();

            final TypedValue<?> assetValue;

            if (channelFlag == ChannelFlag.FAILURE || (assetValue = channelRecord.getValue()) == null) {
                encodeFailure(respPayload, channelRecord.getChannelName(), channelRecord.getTimestamp(),
                        Optional.of(channelStatus.getExceptionMessage()).orElse(message.unknownError()));
            } else {
                encodeSuccess(respPayload, channelRecord.getChannelName(), channelRecord.getTimestamp(),
                        assetValue.getValue());
            }
        }
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
    }
}
