/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.db.H2DbService;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.DeleteDbFiles;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DbServiceImpl implements H2DbService, ConfigurableComponent {

    private static final String ANONYMOUS_MEM_INSTANCE_JDBC_URL = "jdbc:h2:mem:";
    private static Map<String, H2DbServiceImpl> activeInstances = Collections.synchronizedMap(new HashMap<>());

    private static Logger logger = LoggerFactory.getLogger(H2DbServiceImpl.class);

    static {

        // load the driver
        // Use this way of loading the driver as it is required for OSGi
        // Just loading the class with Class.forName is not sufficient.
        try {
            DriverManager.registerDriver(new org.h2.Driver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private H2DbServiceOptions configuration;

    private JdbcDataSource dataSource;
    private JdbcConnectionPool connectionPool;

    private char[] lastSessionPassword = null;

    private CryptoService cryptoService;

    private ScheduledExecutorService executor;

    private ScheduledFuture<?> checkpointTask;
    private ScheduledFuture<?> defragTask;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final AtomicInteger pendingUpdates = new AtomicInteger();

    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(0, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    public void activate(final Map<String, Object> properties) {
        logger.info("activating...");

        this.executor = Executors.newSingleThreadScheduledExecutor();
        updated(properties);

        logger.info("activating...done");
    }

    public void updated(Map<String, Object> properties) {
        this.pendingUpdates.incrementAndGet();
        this.executor.submit(() -> updateInternal(properties));
    }

    public void deactivate() {
        logger.info("deactivate...");
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e1) {
            logger.warn("Interrupted while waiting for db shutdown");
            Thread.currentThread().interrupt();
        }

        this.executorService.shutdown();
        awaitExecutorServiceTermination();
        try {
            shutdownDb();
        } catch (SQLException e) {
            logger.warn("got exception while shutting down the database", e);
        }
        logger.info("deactivate...done");
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public Connection getConnection() throws SQLException {
        if (this.pendingUpdates.get() > 0) {
            syncWithExecutor();
        }

        final Lock lock = this.rwLock.readLock();
        lock.lock();
        try {
            return getConnectionInternal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T withConnection(ConnectionCallable<T> callable) throws SQLException {
        if (this.pendingUpdates.get() > 0) {
            syncWithExecutor();
        }

        final Future<T> result = this.executorService.submit(() -> {
            final Lock executorlock = this.rwLock.readLock();
            executorlock.lock();
            Connection connection = null;
            try {
                connection = getConnectionInternal();
                return callable.call(connection);
            } catch (final SQLException e) {
                logger.warn("Db operation failed");
                rollback(connection);
                throw e;
            } finally {
                close(connection);
                executorlock.unlock();
            }
        });

        try {
            return result.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            } else if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new IllegalStateException(e);
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
        if (rss != null) {
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
    }

    @Override
    public void close(Statement... stmts) {
        if (stmts != null) {
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

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private void updateInternal(final Map<String, Object> properties) {
        final Lock lock = this.rwLock.writeLock();
        lock.lock();
        try {
            logger.info("updating...");

            H2DbServiceOptions newConfiguration = new H2DbServiceOptions(properties);

            if (this.configuration != null) {
                final boolean urlChanged = !this.configuration.getDbUrl().equals(newConfiguration.getDbUrl());
                final boolean userChanged = !this.configuration.getUser().equalsIgnoreCase(newConfiguration.getUser());
                if (urlChanged || userChanged) {
                    shutdownDb();
                }
            }

            if (newConfiguration.isRemote()) {
                throw new IllegalArgumentException("Remote databases are not supported");
            }

            final String baseUrl = newConfiguration.getBaseUrl();
            if (baseUrl.equals(ANONYMOUS_MEM_INSTANCE_JDBC_URL)) {
                throw new IllegalArgumentException("Anonymous in-memory databases instances are not supported");
            }

            if (isManagedByAnotherInstance(baseUrl)) {
                throw new IllegalStateException("Another H2DbService instance is managing the same DB URL,"
                        + " please change the DB URL or deactivate the other instance");
            }

            final char[] passwordFromConfig = newConfiguration.getEncryptedPassword();
            final char[] password = this.lastSessionPassword != null ? this.lastSessionPassword : passwordFromConfig;

            if (this.connectionPool == null) {
                openConnectionPool(newConfiguration, decryptPassword(password));
                this.lastSessionPassword = password;
            }
            setParameters(newConfiguration);

            if (!newConfiguration.isZipBased() && !Arrays.equals(password, passwordFromConfig)) {
                final String decryptedPassword = decryptPassword(passwordFromConfig);
                changePassword(newConfiguration.getUser(), decryptedPassword);
                this.dataSource.setPassword(decryptedPassword);
                this.lastSessionPassword = passwordFromConfig;
            }

            if (newConfiguration.isFileBased()) {
                restartCheckpointTask(newConfiguration);
                restartDefragTask(newConfiguration);
            }

            if (this.configuration == null
                    || newConfiguration.getConnectionPoolMaxSize() != this.configuration.getConnectionPoolMaxSize()) {
                this.executorService.setMaximumPoolSize(newConfiguration.getConnectionPoolMaxSize());
            }

            this.configuration = newConfiguration;
            activeInstances.put(baseUrl, this);

            logger.info("updating...done");
        } catch (Exception e) {
            disposeConnectionPool();
            stopCheckpointTask();
            logger.error("Database initialization failed", e);
        } finally {
            lock.unlock();
            this.pendingUpdates.decrementAndGet();
        }
    }

    private void awaitExecutorServiceTermination() {
        try {
            this.executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e1) {
            logger.warn("Interrupted while waiting for db shutdown");
            Thread.currentThread().interrupt();
        }
    }

    private void setParameters(H2DbServiceOptions configuration) throws SQLException {
        if (!configuration.isFileBasedLogLevelSpecified()) {
            executeInternal("SET TRACE_LEVEL_FILE 0");
        }

        this.connectionPool.setMaxConnections(configuration.getConnectionPoolMaxSize());
    }

    private void syncWithExecutor() {
        try {
            this.executor.submit(() -> {
            }).get();
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Connection getConnectionInternal() throws SQLException {
        if (this.connectionPool == null) {
            throw new SQLException("Database instance not initialized");
        }

        Connection conn = null;
        try {
            conn = this.connectionPool.getConnection();
        } catch (SQLException e) {
            logger.error("Error getting connection", e);
            throw e;
        }
        return conn;
    }

    private void executeInternal(String sql) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnectionInternal();
            stmt = conn.createStatement();
            stmt.execute(sql);
            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw e;
        } finally {
            close(stmt);
            close(conn);
        }
    }

    private void shutdownDb() throws SQLException {
        this.lastSessionPassword = null;
        if (this.connectionPool == null) {
            return;
        }

        stopDefragTask();
        stopCheckpointTask();

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = this.dataSource.getConnection();
            stmt = conn.createStatement();
            stmt.execute("SHUTDOWN");
        } finally {
            close(stmt);
            close(conn);
        }

        disposeConnectionPool();
        activeInstances.remove(this.configuration.getBaseUrl());
    }

    private void openConnectionPool(H2DbServiceOptions configuration, String password) {
        logger.info("Opening database with url: {}", configuration.getDbUrl());

        this.dataSource = new JdbcDataSource();

        this.dataSource.setURL(configuration.getDbUrl());
        this.dataSource.setUser(configuration.getUser());
        this.dataSource.setPassword(password);

        this.connectionPool = JdbcConnectionPool.create(this.dataSource);

        openDatabase(configuration, true);
    }

    private void openDatabase(H2DbServiceOptions configuration, boolean deleteDbOnError) {
        Connection conn = null;
        try {
            conn = getConnectionInternal();
        } catch (SQLException e) {
            logger.error("Failed to open database", e);
            if (deleteDbOnError && configuration.isFileBased()) {
                logger.warn("Deleting database files...");
                deleteDbFiles(configuration);
                logger.warn("Deleting database files...done");
                openDatabase(configuration, false);
            } else {
                disposeConnectionPool();
                throw new ComponentException(e);
            }
        } finally {
            close(conn);
        }
    }

    private void deleteDbFiles(H2DbServiceOptions configuration) {
        try {
            final String directory = configuration.getDbDirectory();
            final String dbName = configuration.getDatabaseName();
            if (directory == null || dbName == null) {
                logger.warn("Failed to determine database directory or name, not deleting db");
                return;
            }
            DeleteDbFiles.execute(directory, dbName, false);
        } catch (Exception e) {
            logger.warn("Failed to remove DB files", e);
        }
    }

    private void disposeConnectionPool() {
        if (this.connectionPool != null) {
            this.connectionPool.dispose();
            this.connectionPool = null;
        }
    }

    private String decryptPassword(char[] encryptedPassword) throws KuraException {
        final char[] decodedPasswordChars = this.cryptoService.decryptAes(encryptedPassword);
        return new String(decodedPasswordChars);
    }

    private void changePassword(String user, String newPassword) throws SQLException {
        executeInternal("ALTER USER " + user + " SET PASSWORD '" + newPassword + "'");
    }

    private boolean isManagedByAnotherInstance(String baseUrl) {
        final H2DbServiceImpl owner = activeInstances.get(baseUrl);
        return owner != null && owner != this;
    }

    private void restartCheckpointTask(final H2DbServiceOptions config) {
        stopCheckpointTask();
        final long delaySeconds = config.getCheckpointIntervalSeconds();
        if (delaySeconds <= 0) {
            return;
        }
        this.checkpointTask = this.executor.scheduleWithFixedDelay(new CheckpointTask(), delaySeconds, delaySeconds,
                TimeUnit.SECONDS);
    }

    private void stopCheckpointTask() {
        if (this.checkpointTask != null) {
            this.checkpointTask.cancel(false);
            this.checkpointTask = null;
        }
    }

    private void restartDefragTask(final H2DbServiceOptions config) {
        stopDefragTask();
        final long delayMinutes = config.getDefragIntervalMinutes();
        if (delayMinutes <= 0) {
            return;
        }
        this.checkpointTask = this.executor.scheduleWithFixedDelay(new DefragTask(config), delayMinutes, delayMinutes,
                TimeUnit.MINUTES);
    }

    private void stopDefragTask() {
        if (this.defragTask != null) {
            this.defragTask.cancel(false);
            this.defragTask = null;
        }
    }

    private class CheckpointTask implements Runnable {

        @Override
        public void run() {
            try {
                logger.info("performing checkpoint...");
                executeInternal("CHECKPOINT SYNC");
                logger.info("performing checkpoint...done");
            } catch (final SQLException e) {
                logger.error("checkpoint failed", e);
            }
        }
    }

    private class DefragTask implements Runnable {

        private final H2DbServiceOptions configuration;

        public DefragTask(final H2DbServiceOptions configuration) {
            this.configuration = configuration;
        }

        private void shutdownDefrag() throws SQLException {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = H2DbServiceImpl.this.dataSource.getConnection();
                stmt = conn.createStatement();
                stmt.execute("SHUTDOWN DEFRAG");
            } finally {
                close(stmt);
                close(conn);
            }
        }

        @Override
        public void run() {
            final Lock lock = H2DbServiceImpl.this.rwLock.writeLock();
            lock.lock();
            try {
                logger.info("shutting down and defragmenting db...");
                shutdownDefrag();
                disposeConnectionPool();
                final String password = decryptPassword(this.configuration.getEncryptedPassword());
                openConnectionPool(this.configuration, password);
                logger.info("shutting down and defragmenting db...done");
            } catch (final Exception e) {
                logger.error("failed to shutdown and defrag db", e);
            } finally {
                lock.unlock();
            }
        }
    }
}
