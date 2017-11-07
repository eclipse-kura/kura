/**
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal
 */
package org.eclipse.kura.internal.driver.opcua;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.KuraErrorCode.OPERATION_NOT_SUPPORTED;
import static org.eclipse.kura.channel.ChannelFlag.FAILURE;
import static org.eclipse.kura.channel.ChannelFlag.SUCCESS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.driver.opcua.localization.OpcUaMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.TypeUtil;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.StringUtil;

/**
 * The Class {@link OpcUaDriver} is an OPC-UA Driver implementation for Kura Asset-Driver
 * Topology. Currently it only supports reading and writing from/to a specific
 * node. As of now, it doesn't support method execution or history read.
 * <br/>
 * <br/>
 * This OPC-UA Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.
 * <br/>
 * <br/>
 * The required properties are enlisted in {@link OpcUaChannelDescriptor} and
 * the driver connection specific properties are enlisted in
 * {@link OpcUaOptions}
 *
 * @see Driver
 * @see OpcUaOptions
 * @see OpcUaChannelDescriptor
 *
 */
public final class OpcUaDriver implements Driver, ConfigurableComponent {

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(OpcUaDriver.class);

    /** Localization Resource. */
    private static final OpcUaMessages message = LocalizationAdapter.adapt(OpcUaMessages.class);

    /** OPC-UA Client Connector */
    private OpcUaClient client;

    /** Dependency for password decryption. */
    private volatile CryptoService cryptoService;

    /** OPC-UA Configuration Options. */
    private OpcUaOptions options;

    private final AtomicBoolean isBusy = new AtomicBoolean();

    private <U> U runSafe(final Future<U> future) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            return future.get(this.options.getRequestTimeout(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    /**
     * OSGi service component callback while activation.
     *
     * @param properties
     *            the service properties
     */
    protected synchronized void activate(final Map<String, Object> properties) {
        logger.debug("Activating OPC-UA Driver...");
        this.extractProperties(properties);
        logger.debug("Activating OPC-UA Driver... Done");
    }

    /**
     * {@link CryptoService} registration callback
     *
     * @param cryptoService
     *            the {@link CryptoService} dependency
     */
    protected synchronized void bindCryptoService(final CryptoService cryptoService) {
        if (isNull(this.cryptoService)) {
            this.cryptoService = cryptoService;
        }
    }

    /**
     * {@link CryptoService} deregistration callback
     *
     * @param cryptoService
     *            the {@link CryptoService} dependency
     */
    protected synchronized void unbindCryptoService(final CryptoService cryptoService) {
        if (this.cryptoService == cryptoService) {
            this.cryptoService = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void connect() throws ConnectionException {
        if (!this.isBusy.compareAndSet(false, true)) {
            throw new ConnectionException(message.errorDriverBusy());
        }

        OpcUaClient newClient = null;
        try {
            logger.info(message.connecting());

            // opc.tcp://<IP>:<PORT>/<SERVER_NAME>
            final String serverName = this.options.getServerName();

            final StringBuilder endPointBuilder = new StringBuilder();
            endPointBuilder.append("opc.tcp://").append(this.options.getIp());
            endPointBuilder.append(":").append(this.options.getPort());

            if (!StringUtil.isNullOrEmpty(serverName)) {
                endPointBuilder.append("/").append(serverName);
            }

            final String endpointString = endPointBuilder.toString();
            logger.debug("Connecting to endpoint: {}", endpointString);

            logger.debug("Fetching endpoint descriptions");
            final EndpointDescription[] endpoints = runSafe(UaTcpStackClient.getEndpoints(endpointString));

            final EndpointDescription endpoint = Arrays.stream(endpoints).filter(
                    e -> e.getSecurityPolicyUri().equals(this.options.getSecurityPolicy().getSecurityPolicyUri()))
                    .findFirst().orElseThrow(() -> new ConnectionException(message.connectionProblem()));

            final KeyStoreLoader loader = new KeyStoreLoader(this.options.getKeystoreType(),
                    this.options.getKeystoreClientAlias(), this.options.getKeystoreServerAlias(),
                    this.options.getKeystorePassword(), this.options.getApplicationCertificate());

            final OpcUaClientConfigBuilder clientConfigBuilder = OpcUaClientConfig.builder();

            clientConfigBuilder.setEndpoint(endpoint)
                    .setApplicationName(LocalizedText.english(this.options.getApplicationName()))
                    .setApplicationUri(this.options.getApplicationUri())
                    .setRequestTimeout(UInteger.valueOf(this.options.getRequestTimeout()))
                    .setSessionTimeout(UInteger.valueOf(this.options.getSessionTimeout()))
                    .setIdentityProvider(this.options.getIdentityProvider()).setKeyPair(loader.getClientKeyPair())
                    .setCertificate(loader.getClientCertificate()).build();

            logger.debug("Connecting...");
            newClient = new OpcUaClient(clientConfigBuilder.build());
            this.client = (OpcUaClient) runSafe(newClient.connect());

            logger.info(message.connectingDone());
        } catch (final Exception e) {
            logger.error(message.connectionProblem(), e);
            this.client = null;
            shutdownClient(newClient);
            throw new ConnectionException(e);
        } finally {
            this.isBusy.set(false);
        }
    }

    /**
     * OSGi service component callback while deactivation.
     *
     */
    protected synchronized void deactivate() {
        logger.debug("Deactivating OPC-UA Driver...");
        try {
            this.disconnect();
        } catch (final ConnectionException e) {
            logger.error(message.errorDisconnecting(), e);
        }
        this.client = null;
        logger.debug("Deactivating OPC-UA Driver... Done");
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect() throws ConnectionException {
        if (!this.isBusy.compareAndSet(false, true)) {
            throw new ConnectionException(message.errorDriverBusy());
        }
        try {
            logger.info(message.disconnecting());
            shutdownClient(this.client);
            this.client = null;
            logger.info(message.disconnectingDone());
        } finally {
            this.isBusy.set(false);
        }
    }

    private void shutdownClient(final OpcUaClient client) throws ConnectionException {
        if (isNull(this.client)) {
            return;
        }
        try {
            runSafe(client.disconnect());
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            throw new ConnectionException(message.disconnectionProblem(), e);
        }
    }

    /**
     * Extract the OPC-UA specific configurations from the provided properties.
     *
     * @param properties
     *            the provided properties to parse
     * @throws NullPointerException
     *             if the provided map is null
     */
    private void extractProperties(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        this.options = new OpcUaOptions(properties, this.cryptoService);
    }

    /** {@inheritDoc} */
    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new OpcUaChannelDescriptor();
    }

    private Optional<TypedValue<?>> getTypedValue(final DataType expectedValueType, final Object containedValue) {
        try {
            switch (expectedValueType) {
            case LONG:
                return Optional.of(TypedValues.newLongValue(Long.parseLong(containedValue.toString())));
            case FLOAT:
                return Optional.of(TypedValues.newFloatValue(Float.parseFloat(containedValue.toString())));
            case DOUBLE:
                return Optional.of(TypedValues.newDoubleValue(Double.parseDouble(containedValue.toString())));
            case INTEGER:
                return Optional.of(TypedValues.newIntegerValue(Integer.parseInt(containedValue.toString())));
            case BOOLEAN:
                return Optional.of(TypedValues.newBooleanValue(Boolean.parseBoolean(containedValue.toString())));
            case STRING:
                return Optional.of(TypedValues.newStringValue(containedValue.toString()));
            case BYTE_ARRAY:
                return Optional.of(TypedValues.newByteArrayValue(TypeUtil.objectToByteArray(containedValue)));
            default:
                return Optional.empty();
            }
        } catch (final Exception ex) {
            return Optional.empty();
        }
    }

    private Object extractValue(final DataValue value) {
        final Variant variant = value.getValue();
        requireNonNull(variant, message.errorNullVariant());

        final Object result = variant.getValue();
        requireNonNull(result, message.errorNullResult());
        return result;
    }

    private void checkStatus(final StatusCode status) throws IOException {
        requireNonNull(status, message.errorNullStatus());
        if (!status.isGood()) {
            throw new IOException(message.errorBadResultStatus(status.getValue()));
        }
    }

    private void runReadRequest(OpcUaRequestInfo requestInfo) {
        ChannelRecord record = requestInfo.channelRecord;
        final VariableNode node = this.client.getAddressSpace().createVariableNode(requestInfo.nodeId);
        Object readResult = null;
        try {
            logger.debug("reading:  namespace index: {} node id: {}", requestInfo.nodeNamespaceIndex,
                    requestInfo.nodeId);
            readResult = extractValue(runSafe(node.readValue()));
            logger.debug("Read Successful");
        } catch (final Exception e) {
            record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, message.readFailed(), e));
            record.setTimestamp(System.currentTimeMillis());
            logger.warn(message.readFailed(), e);
            return;
        }

        final Optional<TypedValue<?>> typedValue = this.getTypedValue(requestInfo.dataType, readResult);
        if (!typedValue.isPresent()) {
            record.setChannelStatus(new ChannelStatus(FAILURE, message.errorValueTypeConversion(), null));
            record.setTimestamp(System.currentTimeMillis());
            return;
        }
        record.setValue(typedValue.get());
        record.setChannelStatus(new ChannelStatus(SUCCESS));
        record.setTimestamp(System.currentTimeMillis());
    }

    /** {@inheritDoc} */
    @Override
    public void read(final List<ChannelRecord> records) throws ConnectionException {
        if (this.isBusy.get()) {
            throw new ConnectionException(message.errorDriverBusy());
        }
        if (isNull(this.client)) {
            this.connect();
        }
        for (final ChannelRecord record : records) {
            OpcUaRequestInfo.extract(record).ifPresent(this::runReadRequest);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void registerChannelListener(final Map<String, Object> channelConfig, final ChannelListener listener)
            throws ConnectionException {
        throw new KuraRuntimeException(OPERATION_NOT_SUPPORTED);
    }

    /** {@inheritDoc} */
    @Override
    public void unregisterChannelListener(final ChannelListener listener) throws ConnectionException {
        throw new KuraRuntimeException(OPERATION_NOT_SUPPORTED);
    }

    /**
     * OSGi service component callback while updating.
     *
     * @param properties
     *            the properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating OPC-UA Driver...");
        if (nonNull(this.client)) {
            try {
                disconnect();
            } catch (final ConnectionException e) {
                logger.error(message.disconnectionProblem(), e);
            }
        }
        this.extractProperties(properties);
        logger.debug("Updating OPC-UA Driver... Done");
    }

    private void runWriteRequest(OpcUaRequestInfo requestInfo) {
        ChannelRecord record = requestInfo.channelRecord;
        final TypedValue<?> value = record.getValue();
        final VariableNode node = this.client.getAddressSpace().createVariableNode(requestInfo.nodeId);
        final DataValue newValue = new DataValue(new Variant(value.getValue()));
        try {
            logger.debug("writing: {} namespace index: {} node id: {}..", value, requestInfo.nodeNamespaceIndex,
                    requestInfo.nodeId);
            checkStatus(runSafe(node.writeValue(newValue)));
            record.setChannelStatus(new ChannelStatus(SUCCESS));
            logger.debug("Write Successful");
        } catch (final Exception e) {
            record.setChannelStatus(new ChannelStatus(FAILURE, message.writeFailed(), e));
            logger.warn(message.writeFailed(), e);
        }
        record.setTimestamp(System.currentTimeMillis());
    }

    /** {@inheritDoc} */
    @Override
    public void write(final List<ChannelRecord> records) throws ConnectionException {
        if (this.isBusy.get()) {
            throw new ConnectionException(message.errorDriverBusy());
        }
        if (this.client == null) {
            this.connect();
        }
        for (final ChannelRecord record : records) {
            OpcUaRequestInfo.extract(record).ifPresent(this::runWriteRequest);
        }
    }

    private static class OpcUaRequestInfo {

        private final DataType dataType;
        private final int nodeNamespaceIndex;
        private final NodeId nodeId;
        private final ChannelRecord channelRecord;

        public OpcUaRequestInfo(final ChannelRecord channelRecord, final DataType dataType,
                final int nodeNamespaceIndex, final NodeId nodeId) {
            this.dataType = dataType;
            this.nodeNamespaceIndex = nodeNamespaceIndex;
            this.nodeId = nodeId;
            this.channelRecord = channelRecord;
        }

        private static void fail(final ChannelRecord record, final String message) {
            record.setChannelStatus(new ChannelStatus(FAILURE, message, null));
            record.setTimestamp(System.currentTimeMillis());
        }

        public static Optional<OpcUaRequestInfo> extract(final ChannelRecord record) {
            final Map<String, Object> channelConfig = record.getChannelConfig();
            final int nodeNamespaceIndex;
            final NodeIdType nodeIdType;
            final NodeId nodeId;

            try {
                nodeNamespaceIndex = OpcUaChannelDescriptor.getNodeNamespaceIndex(channelConfig);
            } catch (final Exception e) {
                fail(record, message.errorRetrievingNodeNamespace());
                return Optional.empty();
            }

            try {
                nodeIdType = OpcUaChannelDescriptor.getNodeIdType(channelConfig);
            } catch (final Exception e) {
                fail(record, message.errorRetrievingNodeIdType());
                return Optional.empty();
            }

            try {
                nodeId = OpcUaChannelDescriptor.getNodeId(channelConfig, nodeNamespaceIndex, nodeIdType);
            } catch (final Exception e) {
                fail(record, message.errorRetrievingNodeId());
                return Optional.empty();
            }

            final DataType dataType = record.getValueType();

            if (isNull(dataType)) {
                fail(record, message.errorRetrievingValueType());
                return Optional.empty();
            }

            return Optional.of(new OpcUaRequestInfo(record, dataType, nodeNamespaceIndex, nodeId));
        }
    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> channelRecords) {
        requireNonNull(channelRecords, message.recordListNonNull());

        OpcUaPreparedRead preparedRead = new OpcUaPreparedRead();
        preparedRead.channelRecords = channelRecords;

        for (ChannelRecord record : channelRecords) {
            OpcUaRequestInfo.extract(record).ifPresent(preparedRead.requestInfos::add);
        }
        return preparedRead;
    }

    private class OpcUaPreparedRead implements PreparedRead {

        private List<OpcUaRequestInfo> requestInfos = new ArrayList<>();
        private volatile List<ChannelRecord> channelRecords;

        @Override
        public synchronized List<ChannelRecord> execute() throws ConnectionException {
            if (OpcUaDriver.this.isBusy.get()) {
                throw new ConnectionException(message.errorDriverBusy());
            }
            if (OpcUaDriver.this.client == null) {
                OpcUaDriver.this.connect();
            }

            for (OpcUaRequestInfo requestInfo : requestInfos) {
                OpcUaDriver.this.runReadRequest(requestInfo);
            }

            return Collections.unmodifiableList(channelRecords);
        }

        @Override
        public List<ChannelRecord> getChannelRecords() {
            return Collections.unmodifiableList(channelRecords);
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
        }
    }
}
