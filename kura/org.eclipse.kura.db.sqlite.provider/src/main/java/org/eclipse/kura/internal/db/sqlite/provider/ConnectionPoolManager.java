/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.db.sqlite.provider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

public class ConnectionPoolManager implements ConnectionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolManager.class);

    private final Set<PooledConnection> availableConnections = new HashSet<>();
    private final SQLiteConnectionPoolDataSource dataSource;
    private final int maxConnectionCount;

    private int activeConnections = 0;

    public ConnectionPoolManager(final SQLiteConnectionPoolDataSource dataSource, final int maxConnectionCount) {
        this.dataSource = dataSource;
        this.maxConnectionCount = maxConnectionCount;
    }

    public synchronized Connection getConnection() throws SQLException {
        logger.debug("getting connection");

        final Connection result;

        if (!availableConnections.isEmpty()) {
            logger.debug("reusing available connection");

            final Iterator<PooledConnection> iter = this.availableConnections.iterator();

            result = iter.next().getConnection();

            iter.remove();
        } else {
            if (activeConnections == maxConnectionCount) {
                throw new SQLException("Maximum connection count reached");
            }

            logger.debug("no idle connections available in the pool, creating new one");

            final PooledConnection newConnection = this.dataSource.getPooledConnection();

            newConnection.addConnectionEventListener(this);

            result = newConnection.getConnection();
        }

        addToActiveConnectionCount(1);
        this.notifyAll();

        result.setAutoCommit(false);

        return result;
    }

    @Override
    public synchronized void connectionClosed(final ConnectionEvent event) {

        try {
            final PooledConnection conn = (PooledConnection) event.getSource();

            if (availableConnections.add(conn)) {
                logger.debug("connection released");

                addToActiveConnectionCount(-1);
            }
        } catch (final Exception e) {
            logger.warn("Unexpected exception handling connection closed", e);
        }

    }

    @Override
    public synchronized void connectionErrorOccurred(final ConnectionEvent event) {

        try {
            logger.warn("Connection error", event.getSQLException());
        } catch (final Exception e) {
            logger.warn("Unexpected exception handling connection error", e);
        }
    }

    synchronized void withExclusiveConnection(final Consumer<Connection> consumer) {
        waitCondition(() -> this.activeConnections <= 0, Optional.empty());

        try {
            try (final Connection conn = this.dataSource.getConnection()) {
                conn.setAutoCommit(false);

                consumer.accept(conn);
            }

        } catch (final Exception e) {
            logger.warn("Exception while running task with exclusive connection", e);
        }
    }

    private synchronized void addToActiveConnectionCount(final int count) {
        final int newCount = this.activeConnections + count;
        logger.debug("active connection count changed: {} -> {}", this.activeConnections, newCount);
        this.activeConnections = newCount;
        this.notifyAll();
    }

    public synchronized void shutdown(final Optional<Long> waitIdleTimeoutMs) {
        if (waitIdleTimeoutMs.isPresent()) {
            waitCondition(() -> this.activeConnections <= 0, waitIdleTimeoutMs);
        }

        if (activeConnections > 0) {
            logger.warn("Closing connection pool with {} active connections", activeConnections);
        }

        for (final PooledConnection conn : this.availableConnections) {
            try {
                conn.close();
            } catch (Exception e) {
                logger.warn("failed to close pooled connection", e);
            }
        }
    }

    private synchronized void waitCondition(final BooleanSupplier condition, final Optional<Long> timeoutMs) {

        final long end = timeoutMs.map(t -> System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(t))
                .orElse(Long.MAX_VALUE);

        while (true) {
            long waitTime = TimeUnit.NANOSECONDS.toMillis(end - System.nanoTime());

            if (waitTime <= 0 || condition.getAsBoolean()) {
                break;
            }

            try {
                this.wait(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
