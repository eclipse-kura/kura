/*******************************************************************************
 * Copyright (c) 2022, 2023 Eurotech and/or its affiliates and others
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

public class ConnectionPoolManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolManager.class);

    private static final Long ACTIVE_CONNECTION_WAIT_TIMEOUT = 30000L;

    private HikariDataSource hikariDatasource;
    private SQLiteDataSource sqliteDataSource;

    public ConnectionPoolManager(final SQLiteDataSource sqliteDataSource, final int maxConnectionCount) {

        HikariConfig config = new HikariConfig();

        config.setDataSource(sqliteDataSource);
        config.setMaximumPoolSize(maxConnectionCount);
        config.setConnectionTimeout(ACTIVE_CONNECTION_WAIT_TIMEOUT);
        config.setAllowPoolSuspension(true);
        config.setIdleTimeout(0);
        config.setMaxLifetime(0);

        this.sqliteDataSource = sqliteDataSource;
        this.hikariDatasource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        logger.debug("getting connection");

        return this.hikariDatasource.getConnection();
    }

    void withExclusiveConnection(final Consumer<Connection> consumer) {

        HikariPoolMXBean hikariPoolMXBean = this.hikariDatasource.getHikariPoolMXBean();

        waitNoActiveConnections(hikariPoolMXBean, Optional.empty());

        try {
            try (final Connection conn = this.sqliteDataSource.getConnection()) {
                consumer.accept(conn);
            }

        } catch (final Exception e) {
            logger.warn("Exception while running task with exclusive connection", e);
        } finally {
            hikariPoolMXBean.resumePool();
        }
    }

    private void waitNoActiveConnections(HikariPoolMXBean hikariPoolMXBean, Optional<Long> timeoutMs) {
        hikariPoolMXBean.suspendPool();

        waitCondition(() -> hikariPoolMXBean.getActiveConnections() <= 0, timeoutMs);
    }

    public void shutdown(final Optional<Long> waitIdleTimeoutMs) {

        HikariPoolMXBean hikariPoolMXBean = this.hikariDatasource.getHikariPoolMXBean();

        if (waitIdleTimeoutMs.isPresent()) {
            waitNoActiveConnections(hikariPoolMXBean, waitIdleTimeoutMs);
        }

        if (hikariPoolMXBean.getActiveConnections() > 0) {
            logger.warn("Closing connection pool with {} active connections", hikariPoolMXBean.getActiveConnections());
        }

        this.hikariDatasource.close();
    }

    private boolean waitCondition(final BooleanSupplier condition, final Optional<Long> timeoutMs) {

        final long end = timeoutMs.map(t -> System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(t))
                .orElse(Long.MAX_VALUE);

        while (System.nanoTime() < end) {

            if (condition.getAsBoolean()) {
                return true;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return false;
    }
}
