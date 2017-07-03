/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.db.H2DbService;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.DeleteDbFiles;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DbServiceImpl implements H2DbService, ConfigurableComponent {

    private static final String ANONYMOUS_MEM_INSTANCE_JDBC_URL = "jdbc:h2:mem:";

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

    private DbConfiguration configuration;

    private JdbcDataSource dataSource;
    private JdbcConnectionPool connectionPool;

    private CheckpointTask checkpointTask;
    private static Map<String, H2DbServiceImpl> activeInstances = Collections.synchronizedMap(new HashMap<>());

    private char[] lastSessionPassword = null;

    private CryptoService cryptoService;

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

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("activating...");

        updated(properties);

        logger.info("activating...done");
    }

    protected synchronized void updated(Map<String, Object> properties) {
        try {
            logger.info("updating...");

            DbConfiguration newConfiguration = new DbConfiguration(properties);

            if (this.configuration != null) {
                if (!this.configuration.getDbUrl().equals(newConfiguration.getDbUrl())) {
                    shutdownDb();
                } else if (!this.configuration.getUser().equalsIgnoreCase(newConfiguration.getUser())) {
                    lastSessionPassword = null;
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
            final char[] password = lastSessionPassword != null ? lastSessionPassword : passwordFromConfig;

            if (connectionPool == null) {
                openConnectionPool(newConfiguration, decryptPassword(password));
                lastSessionPassword = password;
            }
            setParameters(newConfiguration);

            if (!newConfiguration.isZipBased() && !Arrays.equals(password, passwordFromConfig)) {
                final String decryptedPassword = decryptPassword(passwordFromConfig);
                changePassword(newConfiguration.getUser(), decryptedPassword);
                dataSource.setPassword(decryptedPassword);
                lastSessionPassword = passwordFromConfig;
            }

            if (newConfiguration.isFileBased()) {
                restartCheckpointTask(newConfiguration.getCheckpointIntervalSeconds() * 1000);
            }

            this.configuration = newConfiguration;
            activeInstances.put(baseUrl, this);

            logger.info("updating...done");
        } catch (Exception e) {
            disposeConnectionPool();
            stopCheckpointTask();
            logger.warn("Database initialization failed", e);
        }
    }

    protected synchronized void deactivate(ComponentContext componentContext) {
        logger.info("deactivate...");
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
    public synchronized Connection getConnection() throws SQLException {
        if (this.connectionPool == null) {
            throw new SQLException("Database instance not initialized");
        }

        Connection conn = null;
        try {
            conn = this.connectionPool.getConnection();
        } catch (SQLException e) {
            logger.error("Error getting connection", e);
            rollback(conn);
            throw e;
        }
        return conn;
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

    private void setParameters(DbConfiguration configuration) throws SQLException {
        // use slf4j for logging
        execute("SET TRACE_LEVEL_FILE 4");

        this.connectionPool.setMaxConnections(configuration.getConnectionPoolMaxSize());
    }

    private void execute(String sql) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
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
        lastSessionPassword = null;
        if (connectionPool == null) {
            return;
        }
        stopCheckpointTask();

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            stmt.execute("SHUTDOWN");
        } finally {
            close(stmt);
            close(conn);
        }

        disposeConnectionPool();
        activeInstances.remove(configuration.getBaseUrl());
    }

    private void restartCheckpointTask(long interval) {
        stopCheckpointTask();
        this.checkpointTask = new CheckpointTask(interval);
        this.checkpointTask.start();
    }

    private void stopCheckpointTask() {
        if (this.checkpointTask != null) {
            this.checkpointTask.interrupt();
            this.checkpointTask = null;
        }
    }

    private void openConnectionPool(DbConfiguration configuration, String password) {
        logger.info("Opening database with url: {}", configuration.getDbUrl());

        dataSource = new JdbcDataSource();

        dataSource.setURL(configuration.getDbUrl());
        dataSource.setUser(configuration.getUser());
        dataSource.setPassword(password);

        connectionPool = JdbcConnectionPool.create(dataSource);

        openDatabase(configuration, true);
    }

    private void openDatabase(DbConfiguration configuration, boolean deleteDbOnError) {
        Connection conn = null;
        try {
            conn = getConnection();
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

    private void deleteDbFiles(DbConfiguration configuration) {
        try {
            final String directory = configuration.getDbDirectory();
            final String dbName = configuration.getDatabaseName();
            if (directory == null || dbName == null) {
                logger.warn("Failed to determine database directory or name, not deleting db");
                return;
            }
            DeleteDbFiles.execute(configuration.getDbDirectory(), configuration.getDatabaseName(), false);
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
        final char[] decodedPasswordChars = cryptoService.decryptAes(encryptedPassword);
        return new String(decodedPasswordChars);
    }

    private void changePassword(String user, String newPassword) throws SQLException {
        execute("ALTER USER " + user + " SET PASSWORD '" + newPassword + "'");
    }

    private boolean isManagedByAnotherInstance(String baseUrl) {
        final H2DbServiceImpl owner = activeInstances.get(baseUrl);
        return owner != null && owner != this;
    }

    private class CheckpointTask extends Thread {

        private final long delay;

        public CheckpointTask(long delay) {
            this.delay = delay;
        }

        @Override
        public void run() {
            try {
                while (!this.isInterrupted()) {
                    Thread.sleep(delay);
                    try {
                        logger.info("performing checkpoint...");
                        execute("CHECKPOINT SYNC");
                        logger.info("performing checkpoint...done");
                    } catch (SQLException e) {
                        logger.error("checkpoint failed", e);
                    }
                }
            } catch (InterruptedException e) {
                // stop if interrupted
            }
            logger.info("checkpoint task exiting");
        }
    }
}
