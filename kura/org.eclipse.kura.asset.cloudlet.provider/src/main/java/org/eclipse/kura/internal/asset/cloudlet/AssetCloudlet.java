/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
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

import static org.eclipse.kura.cloudconnection.request.RequestHandlerConstants.ARGS_KEY;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.asset.cloudlet.serialization.request.MetadataRequest;
import org.eclipse.kura.internal.asset.cloudlet.serialization.request.ReadRequest;
import org.eclipse.kura.internal.asset.cloudlet.serialization.request.WriteRequest;
import org.eclipse.kura.internal.asset.cloudlet.serialization.response.ChannelOperationResponse;
import org.eclipse.kura.internal.asset.cloudlet.serialization.response.MetadataResponse;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

public final class AssetCloudlet implements RequestHandler {

    private static final String UNKNOWN_ERROR_MESSAGE = "Unknown error";
    private static final String ASSET_TOPIC_RESOURCE = "assets";
    private static final String READ_TOPIC_RESOURCE = "read";
    private static final String WRITE_TOPIC_RESOURCE = "write";

    private static final String APP_ID = "ASSET-V1";

    private static final Logger logger = LoggerFactory.getLogger(AssetCloudlet.class);

    private Map<String, Asset> assets;

    private volatile AssetService assetService;

    private AssetTrackerCustomizer assetTrackerCustomizer;

    private ServiceTracker<Asset, Asset> assetServiceTracker;

    protected synchronized void bindAssetService(final AssetService assetService) {
        if (this.assetService == null) {
            this.assetService = assetService;
        }
    }

    protected synchronized void unbindAssetService(final AssetService assetService) {
        if (this.assetService == assetService) {
            this.assetService = null;
        }
    }

    public void setRequestHandlerRegistry(RequestHandlerRegistry requestHandlerRegistry) {
        try {
            requestHandlerRegistry.registerRequestHandler(APP_ID, this);
        } catch (KuraException e) {
            logger.info("Unable to register cloudlet {} in {}", APP_ID, requestHandlerRegistry.getClass().getName());
        }
    }

    public void unsetRequestHandlerRegistry(RequestHandlerRegistry requestHandlerRegistry) {
        try {
            requestHandlerRegistry.unregister(APP_ID);
        } catch (KuraException e) {
            logger.info("Unable to register cloudlet {} in {}", APP_ID, requestHandlerRegistry.getClass().getName());
        }
    }

    protected synchronized void activate(final ComponentContext componentContext) {
        logger.debug("Activating Asset Cloudlet...");

        this.assetTrackerCustomizer = new AssetTrackerCustomizer(componentContext.getBundleContext(),
                this.assetService);
        this.assetServiceTracker = new ServiceTracker<>(componentContext.getBundleContext(), Asset.class.getName(),
                this.assetTrackerCustomizer);
        this.assetServiceTracker.open();
        logger.debug("Activating Asset Cloudlet...Done");
    }

    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug("Deactivating Asset Cloudlet...");
        this.assetServiceTracker.close();
        logger.debug("Deactivating Asset Cloudlet...Done");
    }

    private void findAssets() {
        this.assets = this.assetTrackerCustomizer.getRegisteredAssets();
    }

    private JsonArray parseRequest(KuraPayload reqPayload) {
        final byte[] rawBody = reqPayload.getBody();
        if (rawBody == null) {
            return null;
        }
        return Json.parse(new String(rawBody, StandardCharsets.UTF_8)).asArray();
    }

    private KuraPayload getAssetMetadata(final Iterator<String> assetNames) {
        KuraResponsePayload responsePayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        MetadataResponse response = new MetadataResponse();
        assetNames.forEachRemaining(assetName -> {
            final Asset asset = this.assets.get(assetName);
            if (asset == null) {
                response.reportAssetNotFound(assetName);
            } else {
                response.addAssetMetadata(assetName, asset);
            }
        });
        responsePayload.setBody(response.serialize());
        return responsePayload;
    }

    @Override
    public KuraMessage doGet(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {
        logger.info("Cloudlet GET Request received on the Asset Cloudlet...");

        List<String> resources = getRequestResources(reqMessage);

        if (resources.size() != 1 || !ASSET_TOPIC_RESOURCE.equals(resources.get(0))) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        findAssets();

        JsonArray request;

        try {
            request = parseRequest(reqMessage.getPayload());
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        KuraPayload responsePayload;
        if (request == null || request.isEmpty()) {
            responsePayload = getAssetMetadata(this.assets.keySet().iterator());
        } else {
            MetadataRequest parsedRequest;
            try {
                parsedRequest = new MetadataRequest(request);
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
            }

            responsePayload = getAssetMetadata(parsedRequest.getAssetNames().iterator());
        }

        return new KuraMessage(responsePayload);
    }

    @SuppressWarnings("unchecked")
    private List<String> getRequestResources(KuraMessage reqMessage) throws KuraException {
        Object requestObject = reqMessage.getProperties().get(ARGS_KEY.value());
        List<String> resources;
        if (requestObject instanceof List) {
            resources = (List<String>) requestObject;
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }
        return resources;
    }

    @Override
    public KuraMessage doExec(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {
        logger.info("Cloudlet EXEC Request received on the Asset Cloudlet...");

        List<String> resources = getRequestResources(reqMessage);

        if (resources.size() != 1) {
            logger.error("Bad request topic: {}", resources);
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        KuraPayload resPayload;
        if (READ_TOPIC_RESOURCE.equals(resources.get(0))) {
            resPayload = read(reqMessage.getPayload());
        } else if (WRITE_TOPIC_RESOURCE.equals(resources.get(0))) {
            resPayload = write(reqMessage.getPayload());
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        return new KuraMessage(resPayload);
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
                    Optional.ofNullable(e.getMessage()).orElse(UNKNOWN_ERROR_MESSAGE));
        }
    }

    private ChannelOperationResponse readAllAssets() {
        ChannelOperationResponse response = new ChannelOperationResponse();
        for (Entry<String, Asset> entry : this.assets.entrySet()) {
            final String assetName = entry.getKey();
            final Asset asset = entry.getValue();
            try {
                response.reportResult(assetName, asset.readAllChannels());
            } catch (Exception e) {
                response.reportAllFailed(assetName,
                        asset.getAssetConfiguration().getAssetChannels().keySet().iterator(),
                        Optional.ofNullable(e.getMessage()).orElse(UNKNOWN_ERROR_MESSAGE));
            }
        }
        return response;
    }

    private KuraPayload read(final KuraPayload reqPayload) throws KuraException {
        findAssets();

        JsonArray request;
        try {
            request = parseRequest(reqPayload);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        ChannelOperationResponse response;

        if (request == null || request.isEmpty()) {
            response = readAllAssets();
        } else {
            List<ReadRequest> readRequests;
            try {
                readRequests = ReadRequest.parseAll(request);
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
            }
            response = new ChannelOperationResponse();
            for (ReadRequest readRequest : readRequests) {
                readAsset(readRequest.getAssetName(), readRequest.getChannelNames(), response);
            }
        }

        KuraResponsePayload responsePayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        responsePayload.setBody(response.serialize());
        return responsePayload;
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
            response.reportAllFailed(assetName, channelRecords.stream().map(ChannelRecord::getChannelName).iterator(),
                    Optional.ofNullable(e.getMessage()).orElse(UNKNOWN_ERROR_MESSAGE));
        }
    }

    private KuraPayload write(final KuraPayload reqPayload) throws KuraException {
        findAssets();

        List<WriteRequest> writeRequests;

        try {
            writeRequests = WriteRequest.parseAll(parseRequest(reqPayload));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        ChannelOperationResponse response = new ChannelOperationResponse();
        for (WriteRequest writeRequest : writeRequests) {
            writeAsset(writeRequest.getAssetName(), writeRequest.getChannelRecords(), response);
        }

        KuraResponsePayload responsePayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        responsePayload.setBody(response.serialize());
        return responsePayload;
    }
}
