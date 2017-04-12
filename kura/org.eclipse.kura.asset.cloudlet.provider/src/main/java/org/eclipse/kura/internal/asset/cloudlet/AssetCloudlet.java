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

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.internal.asset.cloudlet.serialization.request.MetadataRequest;
import org.eclipse.kura.internal.asset.cloudlet.serialization.request.ReadRequest;
import org.eclipse.kura.internal.asset.cloudlet.serialization.request.WriteRequest;
import org.eclipse.kura.internal.asset.cloudlet.serialization.response.ChannelOperationResponse;
import org.eclipse.kura.internal.asset.cloudlet.serialization.response.MetadataResponse;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetCloudletMessages;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

public final class AssetCloudlet extends Cloudlet {

    private static final String ASSET_TOPIC_RESOURCE = "assets";
    private static final String READ_TOPIC_RESOURCE = "read";
    private static final String WRITE_TOPIC_RESOURCE = "write";

    private static final String APP_ID = "ASSET-V1";

    private static final Logger logger = LoggerFactory.getLogger(AssetCloudlet.class);

    private static final AssetCloudletMessages message = LocalizationAdapter.adapt(AssetCloudletMessages.class);

    private Map<String, Asset> assets;

    private volatile AssetService assetService;

    private AssetTrackerCustomizer assetTrackerCustomizer;

    private ServiceTracker<Asset, Asset> assetServiceTracker;

    public AssetCloudlet() {
        super(APP_ID);
    }

    protected synchronized void bindAssetService(final AssetService assetService) {
        if (this.assetService == null) {
            this.assetService = assetService;
        }
    }

    protected synchronized void bindCloudService(final CloudService cloudService) {
        if (this.getCloudService() == null) {
            super.setCloudService(cloudService);
        }
    }

    protected synchronized void unbindAssetService(final AssetService assetService) {
        if (this.assetService == assetService) {
            this.assetService = null;
        }
    }

    protected synchronized void unbindCloudService(final CloudService cloudService) {
        if (this.getCloudService() == cloudService) {
            super.unsetCloudService(cloudService);
        }
    }

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

    @Override
    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivating());
        super.deactivate(componentContext);
        this.assetServiceTracker.close();
        logger.debug(message.deactivatingDone());
    }

    private void findAssets() {
        this.assets = this.assetTrackerCustomizer.getRegisteredAssets();
    }

    private JsonArray parseRequest(KuraRequestPayload reqPayload) {
        final byte[] rawBody = reqPayload.getBody();
        if (rawBody == null) {
            return null;
        }
        return Json.parse(new String(rawBody, StandardCharsets.UTF_8)).asArray();
    }

    private void getAssetMetadata(final KuraResponsePayload respPayload, final Iterator<String> assetNames) {
        MetadataResponse response = new MetadataResponse();
        assetNames.forEachRemaining((assetName) -> {
            final Asset asset = this.assets.get(assetName);
            if (asset == null) {
                response.reportAssetNotFound(assetName);
            } else {
                response.addAssetMetadata(assetName, asset);
            }
        });
        respPayload.setBody(response.serialize());
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
    }

    @Override
    protected void doGet(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
            final KuraResponsePayload respPayload) {
        final String[] resources = reqTopic.getResources();

        logger.info(message.cloudGETReqReceived());

        if (resources.length != 1 || !ASSET_TOPIC_RESOURCE.equals(resources[0])) {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        this.findAssets();

        JsonArray request;

        try {
            request = parseRequest(reqPayload);
        } catch (Exception e) {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        if (request == null || request.isEmpty()) {
            getAssetMetadata(respPayload, this.assets.keySet().iterator());
            return;
        }

        MetadataRequest parsedRequest;
        try {
            parsedRequest = new MetadataRequest(request);
        } catch (Exception e) {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        getAssetMetadata(respPayload, parsedRequest.getAssetNames().iterator());
    }

    @Override
    protected void doExec(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
            final KuraResponsePayload respPayload) {
        logger.info(message.cloudEXECReqReceived());
        final String[] resources = reqTopic.getResources();

        if (resources.length != 1) {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        if (READ_TOPIC_RESOURCE.equals(resources[0])) {
            read(reqPayload, respPayload);
        } else if (WRITE_TOPIC_RESOURCE.equals(resources[0])) {
            write(reqPayload, respPayload);
        } else {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }
    }

    private void readAsset(String assetName, Set<String> channelNames, ChannelOperationResponse response) {
        final Asset asset = this.assets.get(assetName);
        if (asset == null) {
            response.reportAssetNotFound(assetName);
            return;
        }
        try {
            if (channelNames.isEmpty()) {
                response.reportResult(assetName, asset.readAllChannels());
            } else {
                response.reportResult(assetName, asset.read(channelNames));
            }
        } catch (Exception e) {
            response.reportAllFailed(assetName, channelNames.iterator(),
                    Optional.ofNullable(e.getMessage()).orElse(message.unknownError()));
        }
    }

    private ChannelOperationResponse readAllAssets(final KuraResponsePayload respPayload) {
        ChannelOperationResponse response = new ChannelOperationResponse();
        for (Entry<String, Asset> entry : this.assets.entrySet()) {
            final String assetName = entry.getKey();
            final Asset asset = entry.getValue();
            try {
                response.reportResult(assetName, asset.readAllChannels());
            } catch (Exception e) {
                response.reportAllFailed(assetName,
                        asset.getAssetConfiguration().getAssetChannels().keySet().iterator(),
                        Optional.ofNullable(e.getMessage()).orElse(message.unknownError()));
            }
        }
        return response;
    }

    private void read(final KuraRequestPayload reqPayload, final KuraResponsePayload respPayload) {
        this.findAssets();

        JsonArray request;

        try {
            request = parseRequest(reqPayload);
        } catch (Exception e) {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        ChannelOperationResponse response;

        if (request == null || request.isEmpty()) {
            response = readAllAssets(respPayload);
        } else {
            List<ReadRequest> readRequests;
            try {
                readRequests = ReadRequest.parseAll(request);
            } catch (Exception e) {
                respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
                return;
            }
            response = new ChannelOperationResponse();
            for (ReadRequest readRequest : readRequests) {
                readAsset(readRequest.getAssetName(), readRequest.getChannelNames(), response);
            }
        }

        respPayload.setBody(response.serialize());
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
    }

    private void writeAsset(String assetName, List<ChannelRecord> channelRecords, ChannelOperationResponse response) {
        final Asset asset = this.assets.get(assetName);
        if (asset == null) {
            response.reportAssetNotFound(assetName);
            return;
        }
        try {
            if (!channelRecords.isEmpty()) {
                asset.write(channelRecords);
            }
            response.reportResult(assetName, channelRecords);
        } catch (Exception e) {
            response.reportAllFailed(assetName,
                    channelRecords.stream().map((record) -> record.getChannelName()).iterator(),
                    Optional.ofNullable(e.getMessage()).orElse(message.unknownError()));
        }
    }

    private void write(final KuraRequestPayload reqPayload, final KuraResponsePayload respPayload) {
        this.findAssets();

        List<WriteRequest> writeRequests;

        try {
            writeRequests = WriteRequest.parseAll(parseRequest(reqPayload));
        } catch (Exception e) {
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        ChannelOperationResponse response = new ChannelOperationResponse();
        for (WriteRequest writeRequest : writeRequests) {
            writeAsset(writeRequest.getAssetName(), writeRequest.getChannelRecords(), response);
        }

        respPayload.setBody(response.serialize());
        respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
    }
}
