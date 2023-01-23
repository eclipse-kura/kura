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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.JournalMode;
import org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceOptions.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

public class SqliteDbServiceImpl implements BaseDbService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(SqliteDbServiceImpl.class);

    static {
        try {
            DriverManager.registerDriver(new org.sqlite.JDBC());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to register driver");
        }
    }

    private SqliteDebugShell debugShell;

    public void setDebugShell(final SqliteDebugShell debugShell) {
        this.debugShell = debugShell;
    }

    private Optional<DbState> state = Optional.empty();

    public void activate(final Map<String, Object> properties) {
        logger.info("activating...");

        updated(properties);

        logger.info("activating...done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.info("updating...");

        final SqliteDbServiceOptions newOptions = new SqliteDbServiceOptions(properties);
        this.debugShell.setPidAllowed(newOptions.getKuraServicePid(), newOptions.isDebugShellAccessEnabled());

        if (!this.state.map(DbState::getOptions).equals(Optional.of(newOptions))) {
            shutdown();
            try {
                this.state = Optional.of(new DbState(newOptions));
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
        }
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {

        return this.state.orElseThrow(() -> new SQLException("Database is not initialized")).getConnection();
    }

    private static class DbState {

        private static final String DEFRAG_STATEMENT = "END TRANSACTION; VACUUM;";

        private static final String WAL_CHECKPOINT_STATEMENT = "PRAGMA wal_checkpoint(TRUNCATE);";

        private static final Set<String> OPEN_URLS = new HashSet<>();

        private final Optional<ScheduledExecutorService> executor;
        private final ConnectionPoolManager connectionPool;
        private final SqliteDbServiceOptions options;

        public DbState(SqliteDbServiceOptions options) throws SQLException {
            this.options = options;
            tryClaimFile();

            try {
                logger.info("opening database with url: {}...", options.getDbUrl());

                final SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
                dataSource.setUrl(options.getDbUrl());

                if (options.getMode() == Mode.PERSISTED) {
                    dataSource.setJournalMode(
                            options.getJournalMode() == JournalMode.ROLLBACK_JOURNAL ? "DELETE" : "WAL");
                }

                int maxConnectionCount = options.getMode() == Mode.PERSISTED ? options.getConnectionPoolMaxSize() : 1;

                this.connectionPool = new ConnectionPoolManager(dataSource, maxConnectionCount);

                this.connectionPool.getConnection().close();

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
            logger.info("performing WAL checkpoint on database with url: {}...", getOptions().getDbUrl());

            try (final Connection connection = getConnection();
                    final Statement statement = connection.createStatement()) {
                statement.execute(WAL_CHECKPOINT_STATEMENT);
            } catch (final Exception e) {
                logger.warn("WAL checkpoint failed", e);
            }

            logger.info("performing WAL checkpoint on database with url: {}...done", getOptions().getDbUrl());
        }

        private void defrag() {
            this.connectionPool.withExclusiveConnection(conn -> {
                logger.info("defragmenting database with url: {}...", getOptions().getDbUrl());

                try (final Statement statement = conn.createStatement()) {

                    statement.executeUpdate(DEFRAG_STATEMENT);

                } catch (final Exception e) {
                    logger.warn("VACUUM command failed", e);
                }

                if (options.getJournalMode() == JournalMode.WAL) {
                    try (final Statement statement = conn.createStatement()) {

                        statement.executeUpdate(WAL_CHECKPOINT_STATEMENT);

                    } catch (final Exception e) {
                        logger.warn("WAL checkpoint after defrag failed", e);
                    }
                }

                logger.info("defragmenting database with url: {}...done", getOptions().getDbUrl());
            });
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

}
