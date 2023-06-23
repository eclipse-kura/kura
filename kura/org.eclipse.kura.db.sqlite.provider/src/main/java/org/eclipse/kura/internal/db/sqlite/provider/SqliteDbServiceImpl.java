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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.connection.listener.ConnectionListener;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.Mode;
import org.eclipse.kura.message.store.provider.MessageStore;
import org.eclipse.kura.message.store.provider.MessageStoreProvider;
import org.eclipse.kura.util.jdbc.SQLFunction;
import org.eclipse.kura.util.store.listener.ConnectionListenerManager;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.store.provider.QueryableWireRecordStoreProvider;
import org.eclipse.kura.wire.store.provider.WireRecordStore;
import org.eclipse.kura.wire.store.provider.WireRecordStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteJDBCLoader;

public class SqliteDbServiceImpl implements BaseDbService, ConfigurableComponent, MessageStoreProvider,
        WireRecordStoreProvider, QueryableWireRecordStoreProvider {

    private static final Set<String> OPEN_URLS = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(SqliteDbServiceImpl.class);

    private CryptoService cryptoService;
    private SqliteDebugShell debugShell;

    private Optional<DbState> state = Optional.empty();
    private ConnectionListenerManager listenerManager = new ConnectionListenerManager();

    public void setDebugShell(final SqliteDebugShell debugShell) {
        this.debugShell = debugShell;
    }

    public void setCryptoService(final CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void activate(final Map<String, Object> properties) {

        logger.info("activating...");
        try {
            logger.info("SQLite driver is in native mode: {}", SQLiteJDBCLoader.isNativeMode());
        } catch (Exception e) {
            logger.info("Failed to determine if SQLite driver is in native mode", e);
        }

        updated(properties);

        logger.info("activating...done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.info("updating...");

        final SqliteDbServiceOptions newOptions = new SqliteDbServiceOptions(properties);
        this.debugShell.setPidAllowed(newOptions.getKuraServicePid(), newOptions.isDebugShellAccessEnabled());

        final Optional<SqliteDbServiceOptions> oldOptions = this.state.map(DbState::getOptions);

        if (!oldOptions.equals(Optional.of(newOptions))) {
            shutdown();
            try {
                this.state = Optional.of(new DbState(newOptions, oldOptions, cryptoService));
            } catch (final Exception e) {
                logger.warn("Failed to initialize the database instance", e);
            }
        }

        logger.info("updating...done");
    }

    public synchronized void deactivate() {
        logger.info("deactivating...");

        shutdown();

        logger.info("deactivating...done");
    }

    private void shutdown() {
        if (this.state.isPresent()) {
            this.state.get().shutdown();
            this.state = Optional.empty();
            this.listenerManager.dispatchDisconnected();
        }
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {

        if (this.state.isPresent()) {
            Connection connection;
            try {
                connection = this.state.get().getConnection();
            } catch (SQLException e) {
                this.listenerManager.dispatchDisconnected();
                throw e;
            }
            return connection;
        } else {
            this.listenerManager.dispatchDisconnected();
            throw new SQLException("Database is not initialized");
        }

    }

    private class DbState {

        private final Optional<ScheduledExecutorService> executor;
        private final ConnectionPoolManager connectionPool;
        private final SqliteDbServiceOptions options;

        public DbState(SqliteDbServiceOptions options, final Optional<SqliteDbServiceOptions> oldOptions,
                final CryptoService cryptoService) throws SQLException, KuraException {
            this.options = options;
            tryClaimFile();

            try {
                logger.info("opening database with url: {}...", options.getDbUrl());

                final SQLiteDataSource dataSource = new DatabaseLoader(options, oldOptions, cryptoService)
                        .openDataSource();

                int maxConnectionCount = options.getMode() == Mode.PERSISTED ? options.getConnectionPoolMaxSize() : 1;

                this.connectionPool = new ConnectionPoolManager(dataSource, maxConnectionCount);

                if (options.isPeriodicDefragEnabled() || options.isPeriodicWalCheckpointEnabled()) {
                    this.executor = Optional.of(Executors.newSingleThreadScheduledExecutor());
                } else {
                    this.executor = Optional.empty();
                }

                if (options.isPeriodicDefragEnabled()) {
                    this.executor.get().scheduleWithFixedDelay(this::defrag, options.getDefragIntervalSeconds(),
                            options.getDefragIntervalSeconds(), TimeUnit.SECONDS);
                }

                if (options.isPeriodicWalCheckpointEnabled()) {
                    this.executor.get().scheduleWithFixedDelay(this::walCheckpoint,
                            options.getWalCheckpointIntervalSeconds(), options.getWalCheckpointIntervalSeconds(),
                            TimeUnit.SECONDS);
                }

                logger.info("opening database with url: {}...done", options.getDbUrl());
                SqliteDbServiceImpl.this.listenerManager.dispatchConnected();
            } catch (final Exception e) {
                releaseFile();
                throw e;
            }
        }

        public SqliteDbServiceOptions getOptions() {
            return this.options;
        }

        public Connection getConnection() throws SQLException {
            return this.connectionPool.getConnection();
        }

        private void walCheckpoint() {
            try (final Connection connection = getConnection()) {
                SqliteUtil.walCeckpoint(connection, options);
            } catch (Exception e) {
                logger.warn("failed to close connection", e);
            }
        }

        private void defrag() {
            this.connectionPool.withExclusiveConnection(conn -> SqliteUtil.vacuum(conn, options));
        }

        private void tryClaimFile() {
            if (options.getMode() != Mode.PERSISTED) {
                return;
            }

            synchronized (OPEN_URLS) {
                if (OPEN_URLS.contains(options.getPath())) {
                    throw new IllegalStateException("Another database instance is managing the same database file");
                }
                OPEN_URLS.add(options.getPath());
            }
        }

        private void releaseFile() {
            if (options.getMode() != Mode.PERSISTED) {
                return;
            }

            synchronized (OPEN_URLS) {
                OPEN_URLS.remove(options.getPath());
            }
        }

        public void shutdown() {

            try {
                if (this.executor.isPresent()) {
                    this.executor.get().shutdown();
                    try {
                        this.executor.get().awaitTermination(120, TimeUnit.SECONDS);
                    } catch (final InterruptedException e) {
                        logger.warn("Interrupted while waiting for executor shutdown");
                        Thread.currentThread().interrupt();
                    }
                }

                logger.info("closing database with url: {}...", options.getDbUrl());
                this.connectionPool.shutdown(Optional.of(120 * 1000L));
                logger.info("closing database with url: {}...done", options.getDbUrl());
            } finally {
                releaseFile();
            }
        }
    }

    @Override
    public void rollback(Connection conn) {
        try {
            if (conn != null) {
                conn.rollback();
            }
        } catch (SQLException e) {
            logger.error("Error during Connection rollback.", e);
        }
    }

    @Override
    public void close(ResultSet... rss) {
        if (rss == null) {
            return;
        }

        for (ResultSet rs : rss) {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                logger.error("Error during ResultSet closing", e);
            }
        }
    }

    @Override
    public void close(Statement... stmts) {
        if (stmts == null) {
            return;
        }

        for (Statement stmt : stmts) {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                logger.error("Error during Statement closing", e);
            }
        }
    }

    @Override
    public void close(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("Error during Connection closing", e);
        }
    }

    @Override
    public MessageStore openMessageStore(String name) throws KuraStoreException {

        return new SqliteMessageStoreImpl(this::withConnection, name);
    }

    @Override
    public WireRecordStore openWireRecordStore(String name) throws KuraStoreException {

        return new SqliteWireRecordStoreImpl(this::withConnection, name);
    }

    @Override
    @SuppressWarnings("restriction")
    public List<WireRecord> performQuery(String query) throws KuraStoreException {

        return new SqliteQueryableWireRecordStoreImpl(this::withConnection).performQuery(query);
    }

    @SuppressWarnings("restriction")
    private <T> T withConnection(final SQLFunction<Connection, T> callable) throws SQLException {

        try (final Connection conn = this.getConnection()) {
            return callable.call(conn);
        }
    }

    @Override
    public void addListener(ConnectionListener listener) {
        this.listenerManager.add(listener);

    }

    @Override
    public void removeListener(ConnectionListener listener) {
        this.listenerManager.remove(listener);

    }

}
