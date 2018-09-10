/**
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
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
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.internal.driver.opcua.request.ListenRequest;
import org.eclipse.kura.internal.driver.opcua.request.ReadParams;
import org.eclipse.kura.internal.driver.opcua.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(OpcUaDriver.class);

    private Optional<ConnectionManager> connectionManager = Optional.empty();
    private Optional<CompletableFuture<ConnectionManager>> connectTask = Optional.empty();
    private ListenerRegistrations registrations = new ListenerRegistrations();

    private volatile CryptoService cryptoService;
    private OpcUaOptions options;
    private long connectAttempt = 0;

    protected synchronized void activate(final Map<String, Object> properties) {
        logger.info("Activating OPC-UA Driver...");
        this.extractProperties(properties);
        logger.info("Activating OPC-UA Driver... Done");
    }

    protected synchronized void bindCryptoService(final CryptoService cryptoService) {
        if (isNull(this.cryptoService)) {
            this.cryptoService = cryptoService;
        }
    }

    protected synchronized void unbindCryptoService(final CryptoService cryptoService) {
        if (this.cryptoService == cryptoService) {
            this.cryptoService = null;
        }
    }

    protected synchronized CompletableFuture<ConnectionManager> connectAsync() {
        if (connectionManager.isPresent()) {
            return CompletableFuture.completedFuture(this.connectionManager.get());
        }
        if (connectTask.isPresent() && !connectTask.get().isDone()) {
            return this.connectTask.get();
        }
        this.connectAttempt++;
        final long currentConnectAttempt = this.connectAttempt;
        final CompletableFuture<ConnectionManager> currentConnectTask = ConnectionManager
                .connect(options, this::onFailure, registrations).thenApply(manager -> {
                    synchronized (this) {
                        if (this.connectAttempt != currentConnectAttempt) {
                            manager.close();
                            throw new IllegalStateException("Connection attempt has been cancelled");
                        } else {
                            this.connectTask = Optional.empty();
                            manager.start();
                            this.connectionManager = Optional.of(manager);
                        }
                        return manager;
                    }
                });
        this.connectTask = Optional.of(currentConnectTask);
        return currentConnectTask;
    }

    protected ConnectionManager connectSync() throws ConnectionException {
        try {
            return connectAsync().get(this.options.getRequestTimeout(), TimeUnit.SECONDS);
        } catch (final Exception e) {
            logger.debug("Unable to Connect...", e);
            throw new ConnectionException(e);
        }
    }

    @Override
    public void connect() throws ConnectionException {
        connectSync();
    }

    protected synchronized void deactivate() {
        logger.info("Deactivating OPC-UA Driver...");
        try {
            this.disconnect();
        } catch (final ConnectionException e) {
            logger.error("Error while disconnecting....", e);
        }
        logger.info("Deactivating OPC-UA Driver... Done");
    }

    @Override
    public synchronized void disconnect() throws ConnectionException {
        try {
            this.connectAttempt++;
            if (this.connectTask.isPresent()) {
                this.connectTask = Optional.empty();
            }
            if (this.connectionManager.isPresent()) {
                this.connectionManager.get().close();
                this.connectionManager = Optional.empty();
            }
        } catch (Exception e) {
            throw new ConnectionException(e);
        }
    }

    private void extractProperties(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.options = new OpcUaOptions(properties, this.cryptoService);
    }

    /** {@inheritDoc} */
    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new OpcUaChannelDescriptor();
    }

    /** {@inheritDoc} */
    @Override
    public void write(final List<ChannelRecord> records) throws ConnectionException {
        final ConnectionManager connection = this.connectSync();
        try {
            connection.write(Request.extractWriteRequests(records));
        } catch (Exception e) {
            throw new ConnectionException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void read(final List<ChannelRecord> records) throws ConnectionException {
        final ConnectionManager connection = this.connectSync();
        try {
            connection.read(Request.extractReadRequests(records));
        } catch (Exception e) {
            throw new ConnectionException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void registerChannelListener(final Map<String, Object> channelConfig, final ChannelListener listener)
            throws ConnectionException {
        this.registrations.registerListener(ListenRequest.extractListenRequest(channelConfig, listener));
        connectAsync();
    }

    /** {@inheritDoc} */
    @Override
    public void unregisterChannelListener(final ChannelListener listener) throws ConnectionException {
        this.registrations.unregisterListener(listener);
    }

    /**
     * OSGi service component callback while updating.
     *
     * @param properties
     *            the properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.info("Updating OPC-UA Driver...");

        this.extractProperties(properties);

        try {
            final boolean reconnect = connectionManager.isPresent();
            this.disconnect();

            if (reconnect) {
                this.connectAsync();
            }
        } catch (ConnectionException e) {
            logger.warn("Unable to Disconnect...");
        }

        logger.info("Updating OPC-UA Driver... Done");
    }

    private synchronized void onFailure(final ConnectionManager manager, final Throwable ex) {
        if (connectionManager.isPresent() && connectionManager.get() == manager) {
            logger.debug("Unrecoverable failure, forcing disconnect", ex);
            try {
                this.disconnect();
            } catch (ConnectionException e) {
                logger.warn("Unable to Disconnect...");
            }
        } else {
            logger.debug("Ignoring failure from old connection", ex);
        }
    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> channelRecords) {
        requireNonNull(channelRecords, "Channel Record list cannot be null");

        return new OpcUaPreparedRead(Request.extractReadRequests(channelRecords), channelRecords);
    }

    private class OpcUaPreparedRead implements PreparedRead {

        private final List<Request<ReadParams>> requests;
        private final List<ChannelRecord> channelRecords;

        public OpcUaPreparedRead(final List<Request<ReadParams>> requests, final List<ChannelRecord> records) {
            this.requests = requests;
            this.channelRecords = records;
        }

        @Override
        public List<ChannelRecord> execute() throws ConnectionException {
            try {
                final ConnectionManager connection = connectSync();
                connection.read(requests);
                return Collections.unmodifiableList(channelRecords);
            } catch (Exception e) {
                throw new ConnectionException(e);
            }
        }

        @Override
        public List<ChannelRecord> getChannelRecords() {
            return Collections.unmodifiableList(channelRecords);
        }

        @Override
        public void close() {
            // no need
        }
    }
}
