/**
 * Copyright (c) 2018, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 */

package org.eclipse.kura.internal.driver.opcua;

import static org.eclipse.kura.internal.driver.opcua.Utils.fillRecord;
import static org.eclipse.kura.internal.driver.opcua.Utils.fillStatus;
import static org.eclipse.kura.internal.driver.opcua.Utils.runSafe;

import java.security.KeyPair;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.internal.driver.opcua.auth.KeyStoreLoader;
import org.eclipse.kura.internal.driver.opcua.request.ReadParams;
import org.eclipse.kura.internal.driver.opcua.request.Request;
import org.eclipse.kura.internal.driver.opcua.request.WriteParams;
import org.eclipse.kura.util.base.StringUtil;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
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

public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final OpcUaClient client;
    private final BiConsumer<ConnectionManager, Throwable> failureHandler;
    private final AsyncTaskQueue queue;

    private final SubscriptionManager subscriptionManager;
    private final SubtreeSubscriptionManager subtreeSubscriptionManager;
    private final OpcUaOptions options;

    public ConnectionManager(final OpcUaClient client, final OpcUaOptions options,
            final BiConsumer<ConnectionManager, Throwable> failureHandler,
            final ListenerRegistrationRegistry registrations,
            final ListenerRegistrationRegistry subtreeListenerRegistrations) {
        this.options = options;
        this.client = client;
        this.queue = new AsyncTaskQueue();
        this.failureHandler = failureHandler;
        this.queue.onFailure(ex -> failureHandler.accept(this, ex));
        this.subscriptionManager = new SubscriptionManager(options, client, this.queue, registrations);
        this.subtreeSubscriptionManager = new SubtreeSubscriptionManager(options, client, this.queue,
                subtreeListenerRegistrations);
    }

    public static CompletableFuture<ConnectionManager> connect(final OpcUaOptions options,
            final BiConsumer<ConnectionManager, Throwable> failureHandler,
            final ListenerRegistrationRegistry registrations,
            final ListenerRegistrationRegistry subtreeListenerRegistrations) {

        logger.info("Connecting to OPC-UA...");

        final String endpointString = getEndpointString(options);

        logger.debug("Fetching endpoint list for: {}", endpointString);

        return DiscoveryClient.getEndpoints(endpointString)
                .thenCompose(endpoints -> tryConnectToEndpoints(options, endpoints)) //
                .thenApply(client -> new ConnectionManager((OpcUaClient) client, options, failureHandler, registrations,
                        subtreeListenerRegistrations)) //
                .whenComplete((ok, err) -> {
                    if (err != null) {
                        logger.warn("Connecting to OPC-UA...failed", err);
                    } else {
                        logger.info("Connecting to OPC-UA...done");
                    }
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

        final ReadResponse response = runSafe(this.client.read(0.0, TimestampsToReturn.Both, tempList),
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

        final WriteResponse response = runSafe(this.client.write(tempList), this.options.getRequestTimeout(),
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

        this.queue.close(
                () -> CompletableFuture.allOf(this.subscriptionManager.close(), this.subtreeSubscriptionManager.close()) //
                        .whenComplete((ok, ex) -> this.client.disconnect() //
                                .handle((o, e) -> {
                                    if (e == null) {
                                        logger.info("Disconnecting from OPC-UA...Done");
                                    } else {
                                        logger.warn("Failed to close client", e);
                                    }
                                    return (Void) null;
                                })));
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

    private static void dumpEndpoints(final String message, final Collection<EndpointDescription> availableEndpoints) {
        if (logger.isDebugEnabled()) {
            for (final EndpointDescription desc : availableEndpoints) {
                logger.debug("{}: {}", message, desc.getEndpointUrl());
            }
        } else if (logger.isTraceEnabled()) {
            for (final EndpointDescription desc : availableEndpoints) {
                logger.trace("{}: {}", message, desc);
            }
        }
    }

    private static CompletableFuture<UaClient> tryConnectToEndpoints(final OpcUaOptions options,
            final Collection<EndpointDescription> availableEndpoints) {

        dumpEndpoints("found endpoint", availableEndpoints);

        List<EndpointDescription> endpoints = filterEndpoints(options, availableEndpoints);

        dumpEndpoints("endpoint matches configuration", availableEndpoints);

        if (endpoints.isEmpty()) {
            final CompletableFuture<UaClient> result = new CompletableFuture<>();
            result.completeExceptionally(new ConnectionException("Unable to Connect...No desired Endpoints returned"));
            return result;
        }

        final EndpointDescription forced = forceEndpointUrl(endpoints.get(0), getEndpointString(options));

        if (options.shouldForceEndpointUrl()) {
            endpoints = Collections.singletonList(forced);
        } else {
            endpoints.add(forced);
        }

        Optional<KeyStoreLoader> keyStoreLoader;

        try {
            keyStoreLoader = Optional.of(new KeyStoreLoader(options.getKeyStorePath(), options.getKeystoreType(),
                    options.getKeystorePassword(), options.getKeystoreClientAlias(),
                    options.isServerAuthenticationEnabled()));
        } catch (final Exception e) {
            logger.warn("Failed to load certificates");
            logger.debug("Failed to load certificates", e);
            keyStoreLoader = Optional.empty();
        }

        final ConnectionAttempt connection = new ConnectionAttempt(options, keyStoreLoader, endpoints);

        return connection.connect();
    }

    private static List<EndpointDescription> filterEndpoints(final OpcUaOptions options,
            final Collection<EndpointDescription> availableEndpoints) {

        return availableEndpoints.stream()
                .filter(e -> e.getSecurityPolicyUri().equals(options.getSecurityPolicy().getUri()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static EndpointDescription forceEndpointUrl(final EndpointDescription original, final String endpointUrl) {
        return new EndpointDescription(endpointUrl, original.getServer(), original.getServerCertificate(),
                original.getSecurityMode(), original.getSecurityPolicyUri(), original.getUserIdentityTokens(),
                original.getTransportProfileUri(), original.getSecurityLevel());
    }

    private static final class ConnectionAttempt {

        private final Iterator<EndpointDescription> endpoints;
        private final Optional<KeyStoreLoader> certificateManager;
        private final CompletableFuture<UaClient> future;
        private final OpcUaOptions options;

        public ConnectionAttempt(final OpcUaOptions options, final Optional<KeyStoreLoader> certificateManager,
                final List<EndpointDescription> endpoints) {
            this.certificateManager = certificateManager;
            this.endpoints = endpoints.iterator();
            this.options = options;
            this.future = new CompletableFuture<>();
        }

        public CompletableFuture<UaClient> connect() {
            tryNextEndpoint();
            return this.future;
        }

        private void tryNextEndpoint() {
            if (!this.endpoints.hasNext()) {
                this.future.completeExceptionally(
                        new IllegalStateException("failed to connect to any of the matching endpoints"));
                return;
            }

            final EndpointDescription endpoint = this.endpoints.next();

            logger.info("connecting to endpoint: {}", endpoint.getEndpointUrl());

            OpcUaClientConfigBuilder clientConfigBuilder = OpcUaClientConfig.builder().setEndpoint(endpoint)
                    .setApplicationName(LocalizedText.english(this.options.getApplicationName()))
                    .setApplicationUri(this.options.getApplicationUri())
                    .setRequestTimeout(UInteger.valueOf(this.options.getRequestTimeout()))
                    .setAcknowledgeTimeout(UInteger.valueOf(this.options.getAcknowledgeTimeout()))
                    .setSessionTimeout(UInteger.valueOf(this.options.getSessionTimeout()))
                    .setIdentityProvider(this.options.getIdentityProvider());

            if (certificateManager.isPresent()) {

                clientConfigBuilder = clientConfigBuilder
                        .setCertificateValidator(certificateManager.get().getCertificateValidator());

                final Optional<PrivateKeyEntry> privateKeyEntry = certificateManager.get().getPrivateKeyEntry();

                if (privateKeyEntry.isPresent()) {
                    final X509Certificate certificate = (X509Certificate) privateKeyEntry.get().getCertificate();
                    final KeyPair clientKeyPair = new KeyPair(certificate.getPublicKey(),
                            privateKeyEntry.get().getPrivateKey());

                    clientConfigBuilder = clientConfigBuilder.setCertificate(certificate).setKeyPair(clientKeyPair);
                }

            }

            final OpcUaClientConfig config = clientConfigBuilder.build();

            final OpcUaClient cl;

            try {
                cl = OpcUaClient.create(config);
            } catch (final Exception e) {
                logger.warn("failed to create client", e);
                tryNextEndpoint();
                return;
            }

            cl.connect() //
                    .whenComplete((c, err) -> {
                        if (err != null) {
                            logger.warn("failed to connect to endpoint", err);
                            cl.disconnect();
                            tryNextEndpoint();
                            return;
                        }

                        if (!this.endpoints.hasNext() && !this.options.shouldForceEndpointUrl()) {
                            logger.info("Connection has been established by forcing endpoint URL from configuration, "
                                    + "setting \"Force endpoint URL\" to true in driver configuration might reduce connection time for this server.");
                        }

                        this.future.complete(c);
                    });
        }

    }
}
