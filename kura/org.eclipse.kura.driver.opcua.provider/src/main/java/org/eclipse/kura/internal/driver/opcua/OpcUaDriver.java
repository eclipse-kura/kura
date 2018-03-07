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
    private Optional<CompletableFuture<Void>> connectTask = Optional.empty();
    private ListenerRegistrations registrations = new ListenerRegistrations();

    private volatile CryptoService cryptoService;
    private OpcUaOptions options;
    private long connectAttempt = 0;

    protected synchronized void activate(final Map<String, Object> properties) {
        logger.debug("Activating OPC-UA Driver...");
        this.extractProperties(properties);
        logger.debug("Activating OPC-UA Driver... Done");
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

    public synchronized void connectAsync() {
        if (connectionManager.isPresent() || (connectTask.isPresent() && !connectTask.get().isDone())) {
            return;
        }
        this.connectAttempt++;
        final long currentConnectAttempt = this.connectAttempt;
        this.connectTask = Optional
                .of(ConnectionManager.connect(options, this::onFailure, registrations).handle((manager, ex) -> {
                    onConnectionResult(currentConnectAttempt, manager, ex);
                    return null;
                }));
    }

    @Override
    public void connect() throws ConnectionException {
        try {
            final CompletableFuture<Void> connectTask;
            synchronized (this) {
                if (connectionManager.isPresent()) {
                    return;
                }
                this.connectAsync();
                connectTask = this.connectTask.get();
            }
            connectTask.get(this.options.getRequestTimeout(), TimeUnit.SECONDS);
        } catch (final Exception e) {
            logger.debug("Unable to Connect...No desired Endpoints returned", e);
            throw new ConnectionException(e);
        }
    }

    protected synchronized void deactivate() {
        logger.debug("Deactivating OPC-UA Driver...");
        try {
            this.disconnect();
        } catch (final ConnectionException e) {
            logger.error("Error while disconnecting....", e);
        }
        logger.debug("Deactivating OPC-UA Driver... Done");
    }

    @Override
    public synchronized void disconnect() throws ConnectionException {
        try {
            this.connectAttempt++;
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
        try {
            this.connect();
            this.connectionManager.get().write(Request.extractWriteRequests(records));
        } catch (Exception e) {
            throw new ConnectionException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void read(final List<ChannelRecord> records) throws ConnectionException {
        try {
            this.connect();
            this.connectionManager.get().read(Request.extractReadRequests(records));
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
        logger.debug("Updating OPC-UA Driver...");

        this.extractProperties(properties);
        try {
            if (connectionManager.isPresent()) {
                this.disconnect();
                this.connectAsync();
            }
        } catch (ConnectionException e) {
            logger.warn("Unable to Disconnect...");
        }

        logger.debug("Updating OPC-UA Driver... Done");
    }

    private synchronized void onFailure(final ConnectionManager manager, final Throwable ex) {
        if (connectionManager.isPresent() && connectionManager.get() == manager) {
            logger.debug("Unrecoverable failure, forcing disconnect", ex);
            try {
                if (connectionManager.isPresent()) {
                    this.disconnect();
                }
            } catch (ConnectionException e) {
                logger.warn("Unable to Disconnect...");
            }
        } else {
            logger.debug("Ignoring failure from old connection", ex);
        }
    }

    private synchronized void onConnectionResult(final long currentConnectAttempt, final ConnectionManager manager,
            final Throwable ex) {
        if (this.connectAttempt != currentConnectAttempt) {
            if (manager != null) {
                manager.close();
            }
            throw new RuntimeException("Unable to Connect...No desired Endpoints returned");
        } else {
            this.connectTask = Optional.empty();
            if (manager != null) {
                manager.start();
                this.connectionManager = Optional.of(manager);
            } else {
                throw new RuntimeException("Unable to Connect...No desired Endpoints returned");
            }
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
        public synchronized List<ChannelRecord> execute() throws ConnectionException {
            try {
                connect();
                connectionManager.get().read(requests);
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
            // TODO Auto-generated method stub
        }
    }
}
