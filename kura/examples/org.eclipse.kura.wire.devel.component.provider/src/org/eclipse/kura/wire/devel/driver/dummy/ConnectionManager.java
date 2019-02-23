/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.devel.driver.dummy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.driver.Driver.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final AtomicBoolean isConnected = new AtomicBoolean();
    private final AtomicBoolean isShuttingDown = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Future<?> connectionAttempt = CompletableFuture.completedFuture(null);
    private DummyDriverOptions options;

    public Future<?> connectAsync() {
        synchronized (this) {
            if (isConnecting()) {
                return connectionAttempt;
            }

            this.connectionAttempt = this.executor.submit(() -> {
                if (isShuttingDown.get()) {
                    return (Void) null;
                }
                this.connectInternal();
                return (Void) null;
            });
            return this.connectionAttempt;
        }
    }

    public Future<?> disconnectAsync() {
        return this.executor.submit(() -> {
            if (isShuttingDown.get()) {
                return;
            }
            this.disconnectInternal();
        });
    }

    public synchronized void reconnectAsync() {
        if (isConnected() || isConnecting()) {
            this.executor.submit(() -> {
                disconnectInternal();
                connectAsync();
            });
        }
    }

    public void connectSync() throws ConnectionException {
        try {
            connectAsync().get();
        } catch (final Exception e) {
            throw new ConnectionException(e);
        }
    }

    public void disconnectSync() throws ConnectionException {
        try {
            this.disconnectAsync().get();
        } catch (final Exception e) {
            throw new ConnectionException(e);
        }
    }

    private void connectInternal() throws ConnectionException {
        if (isConnected.get()) {
            logger.debug("already connected");
            return;
        }

        final ConnectionIssue issue = this.options.getConnectionIssues();

        if (issue == ConnectionIssue.THROW) {
            throw new ConnectionException("Simulated connection exception");
        }

        final int connectionDelay = this.options.getConnectionDelay();

        logger.info("connecting...");

        if (connectionDelay > 0) {
            try {
                Thread.sleep(connectionDelay * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        isConnected.set(true);
        logger.info("connecting...done");
    }

    private void disconnectInternal() {
        if (!isConnected.get()) {
            logger.debug("already disconnected");
            return;
        }

        logger.info("disconnecting...");
        isConnected.set(false);

        final int connectionDelay = this.options.getConnectionDelay();

        if (connectionDelay > 0) {
            try {
                Thread.sleep(connectionDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.info("disconnecting...done");
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public boolean isConnecting() {
        return !this.connectionAttempt.isDone();
    }

    public void setOptions(final DummyDriverOptions options) {
        this.options = options;
    }

    public void shutdown() {
        isShuttingDown.set(true);
        try {
            this.executor.submit(this::disconnectInternal).get();
        } catch (Exception e) {
            logger.warn("disconnection failed", e);
        }
        this.executor.shutdown();
    }
}
