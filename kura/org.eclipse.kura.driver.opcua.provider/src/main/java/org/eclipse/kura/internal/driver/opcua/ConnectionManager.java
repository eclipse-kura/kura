/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua;

import static org.eclipse.kura.internal.driver.opcua.Utils.fillRecord;
import static org.eclipse.kura.internal.driver.opcua.Utils.fillStatus;
import static org.eclipse.kura.internal.driver.opcua.Utils.runSafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.internal.driver.opcua.auth.CertificateManager;
import org.eclipse.kura.internal.driver.opcua.request.ReadParams;
import org.eclipse.kura.internal.driver.opcua.request.Request;
import org.eclipse.kura.internal.driver.opcua.request.WriteParams;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.StringUtil;

public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final OpcUaClient client;
    private final BiConsumer<ConnectionManager, Throwable> failureHandler;
    private final AsyncTaskQueue queue;

    private SubscriptionManager subscriptionManager;
    private OpcUaOptions options;

    public ConnectionManager(final OpcUaClient client, final OpcUaOptions options,
            final BiConsumer<ConnectionManager, Throwable> failureHandler, final ListenerRegistrations registrations) {
        this.options = options;
        this.client = client;
        this.queue = new AsyncTaskQueue();
        this.failureHandler = failureHandler;
        this.queue.onFailure(ex -> failureHandler.accept(this, ex));
        this.subscriptionManager = new SubscriptionManager(options, client, queue, registrations);
    }

    public static CompletableFuture<ConnectionManager> connect(final OpcUaOptions options,
            final BiConsumer<ConnectionManager, Throwable> failureHandler, final ListenerRegistrations registrations) {

        logger.info("Connecting to OPC-UA...");

        final String endpointString = getEndpointString(options);

        logger.debug("Connecting to endpoint: {}", endpointString);

        return UaTcpStackClient.getEndpoints(endpointString)
                .thenCompose(endpoints -> connectToEndpoint(options, endpoints)).thenApply(client -> {
                    logger.info("Connecting to OPC-UA...Done");
                    return new ConnectionManager((OpcUaClient) client, options, failureHandler, registrations);
                });
    }

    public synchronized void start() {
        this.subscriptionManager.onRegistrationsChanged();
    }

    public void read(final List<Request<ReadParams>> requests) throws Exception {

        final int maxItemsPerRequest = this.options.getMaxItemCountPerRequest();
        final ArrayList<ReadValueId> tempList = new ArrayList<>(maxItemsPerRequest);

        for (int i = 0; i < requests.size(); i += maxItemsPerRequest) {
            this.read(requests.subList(i, Math.min(i + maxItemsPerRequest, requests.size())), tempList);
        }
    }

    public void write(final List<Request<WriteParams>> requests) throws Exception {

        final int maxItemsPerRequest = this.options.getMaxItemCountPerRequest();
        final ArrayList<WriteValue> tempList = new ArrayList<>(maxItemsPerRequest);

        for (int i = 0; i < requests.size(); i += maxItemsPerRequest) {
            this.write(requests.subList(i, Math.min(i + maxItemsPerRequest, requests.size())), tempList);
        }
    }

    private void read(final List<Request<ReadParams>> requests, final List<ReadValueId> tempList) throws Exception {
        tempList.clear();

        for (final Request<ReadParams> request : requests) {
            tempList.add(request.getParameters().getReadValueId());
        }

        final ReadResponse response = runSafe(client.read(0.0, TimestampsToReturn.Both, tempList),
                this.options.getRequestTimeout(), ex -> this.failureHandler.accept(this, ex));

        final DataValue[] results = response.getResults();
        for (int i = 0; i < requests.size(); i++) {
            fillRecord(results[i], requests.get(i).getRecord());
        }

        logger.debug("Read Successful");
    }

    private void write(final List<Request<WriteParams>> requests, final List<WriteValue> tempList) throws Exception {
        tempList.clear();

        for (final Request<WriteParams> request : requests) {
            tempList.add(request.getParameters().getWriteValue());
        }

        final WriteResponse response = runSafe(client.write(tempList), this.options.getRequestTimeout(),
                ex -> this.failureHandler.accept(this, ex));

        final StatusCode[] results = response.getResults();
        for (int i = 0; i < requests.size(); i++) {
            final ChannelRecord record = requests.get(i).getRecord();
            fillStatus(results[i], record);
            record.setTimestamp(System.currentTimeMillis());
        }

        logger.debug("Write Successful");
    }

    public synchronized void close() {

        logger.info("Disconnecting from OPC-UA...");
        queue.close(() -> subscriptionManager.close().thenCompose(ok -> client.disconnect()).handle((ok, err) -> {
            if (err == null) {
                logger.info("Disconnecting from OPC-UA...Done");
            } else {
                logger.info("Unable to Disconnect...");
            }
            return (Void) null;
        }));
    }

    private static String getEndpointString(final OpcUaOptions options) {
        // opc.tcp://<IP>:<PORT>/<SERVER_NAME>
        final String serverName = options.getServerName();

        final StringBuilder endPointBuilder = new StringBuilder();
        endPointBuilder.append("opc.tcp://").append(options.getIp());
        endPointBuilder.append(":").append(options.getPort());

        if (!StringUtil.isNullOrEmpty(serverName)) {
            endPointBuilder.append("/").append(serverName);
        }

        return endPointBuilder.toString();
    }

    private static CompletableFuture<UaClient> connectToEndpoint(final OpcUaOptions options,
            final EndpointDescription[] availableEndpoints) {

        logger.debug("Returned endpoints: {}", (Object[]) availableEndpoints);

        final Optional<EndpointDescription> endpoint = Arrays.stream(availableEndpoints)
                .filter(e -> e.getSecurityPolicyUri().equals(options.getSecurityPolicy().getSecurityPolicyUri()))
                .findAny();

        if (!endpoint.isPresent()) {
            throw new RuntimeException("Unable to Connect...No desired Endpoints returned");
        }

        logger.debug("Connecting to endpoint: {}", endpoint.get());

        final CertificateManager certificateManager = options.getCertificateManager();

        try {
            certificateManager.load();
        } catch (final Exception e) {
            logger.warn("Failed to load certificates");
            logger.debug("Failed to load certificates", e);
        }

        final OpcUaClientConfigBuilder clientConfigBuilder = OpcUaClientConfig.builder();

        clientConfigBuilder.setEndpoint(endpoint.get()).setCertificateValidator(certificateManager)
                .setApplicationName(LocalizedText.english(options.getApplicationName()))
                .setApplicationUri(options.getApplicationUri())
                .setRequestTimeout(UInteger.valueOf(options.getRequestTimeout()))
                .setSessionTimeout(UInteger.valueOf(options.getSessionTimeout()))
                .setIdentityProvider(options.getIdentityProvider()).setKeyPair(certificateManager.getClientKeyPair())
                .setCertificate(certificateManager.getClientCertificate()).build();

        final OpcUaClient client = new OpcUaClient(clientConfigBuilder.build());
        return client.connect();
    }
}
