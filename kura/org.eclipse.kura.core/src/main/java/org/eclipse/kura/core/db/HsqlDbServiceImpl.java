/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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
import java.util.Map;

import org.eclipse.kura.core.db.pool.KuraJDBCConnectionPool;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HsqlDbServiceImpl implements DbService {

    private static Logger logger = LoggerFactory.getLogger(HsqlDbServiceImpl.class);

    static {

        // load the driver
        // Use this way of loading the driver as it is required for OSGi
        // Just loading the class with Class.forName is not sufficient.
        try {
            DriverManager.registerDriver(new org.hsqldb.jdbcDriver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String username = "sa";
    private static final String password = "";
    private static final Object init_lock = "init lock";
    private static boolean inited = false;

    @SuppressWarnings("unused")
    private ComponentContext ctx;
    private SystemService systemService;
    private KuraJDBCConnectionPool connPool;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("activate...");

        //
        // save the bundle context
        this.ctx = componentContext;

        synchronized (init_lock) {
            if (!inited) {

                // get a connection for this Message Store
                // If this is the first connection, this will result into starting up the database
                Connection conn = null;
                try {
                    conn = getConnection();
                } catch (SQLException e) {
                    rollback(conn);
                    logger.error("Error during HsqdbService startup", e);
                    throw new ComponentException(e);
                } finally {
                    close(conn);
                }

                // init the database
                // get a connection for this Message Store
                // If this is the first connection, this will result into starting up the database
                try {
                    init();
                    inited = true;
                } catch (SQLException e) {
                    logger.error("Error during HsqdbService init", e);
                    throw new ComponentException(e);
                }
            }
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("deactivate...");
        try {
            execute("SHUTDOWN");
            inited = false;
        } catch (SQLException e) {
            logger.error("Error during HsqlDbService shutdown", e);
            throw new ComponentException(e);
        }

        try {
            if (this.connPool != null) {
                this.connPool.close(0); // no wait
                this.connPool = null;
            }
        } catch (SQLException e) {
            logger.error("Error during HsqlDbService connection close", e);
            throw new ComponentException(e);
        }
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (this.connPool == null) {

            String url = this.systemService.getProperties().getProperty(SystemService.DB_URL_PROPNAME);
            logger.info("Opening database with url: " + url);

            // m_connPool = new JDBCPool();
            this.connPool = new KuraJDBCConnectionPool();
            this.connPool.setUrl(url);
            this.connPool.setUser(username);
            this.connPool.setPassword(password);
        }

        Connection conn = null;
        try {
            conn = this.connPool.getConnection();
        } catch (SQLException e) {
            logger.error("Error getting connection", e);
            closeSilently();
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

    public boolean isLogDataEnabled() {
        boolean isLogDataEnabled = true;
        String sIsLogDataEnabled = this.systemService.getProperties().getProperty(SystemService.DB_LOG_DATA_PROPNAME);

        if (sIsLogDataEnabled != null && !sIsLogDataEnabled.isEmpty()) {
            isLogDataEnabled = new Boolean(sIsLogDataEnabled);
        }

        return isLogDataEnabled;
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private void init() throws SQLException {
        // concurrency control
        // Switching from concurrency control MVCC to LOCKS (2PL)
        // 2PL will lock the whole table on a write but it makes count(*) extremely fast.
        // As we serialize database access in the DbDataStore, 2PL is a better choice than MVCC.
        execute("SET DATABASE TRANSACTION CONTROL LOCKS");

        // Transaction Level
        execute("SET TRANSACTION READ WRITE, ISOLATION LEVEL READ COMMITTED");

        // set auto-commit
        execute("SET AUTOCOMMIT FALSE");

        // Sets the write delay_millies property, delay in milliseconds.
        String writeDelayMillies = this.systemService.getProperties()
                .getProperty(SystemService.DB_WRITE_DELAY_MILLIES_PROPNAME);
        if (writeDelayMillies == null || writeDelayMillies.isEmpty()) {
            writeDelayMillies = "500";
        }
        execute("SET FILES WRITE DELAY " + writeDelayMillies + " MILLIS");

        // use cache tables by default as they load only part of the data in mem
        execute("SET DATABASE DEFAULT TABLE TYPE CACHED");

        String cacheRows = this.systemService.getProperties().getProperty(SystemService.DB_CACHE_ROWS_PROPNAME);
        if (cacheRows != null && !cacheRows.isEmpty()) {
            execute("SET FILES CACHE ROWS " + cacheRows);
        }

        String lobScale = this.systemService.getProperties().getProperty(SystemService.DB_LOB_FILE_PROPNAME);
        if (lobScale != null && !lobScale.isEmpty()) {
            execute("SET FILES LOB SCALE " + lobScale);
        }

        String defragLimit = this.systemService.getProperties().getProperty(SystemService.DB_DEFRAG_LIMIT_PROPNAME);
        if (defragLimit != null && !defragLimit.isEmpty()) {
            execute("SET FILES DEFRAG " + defragLimit);
        }

        String logData = this.systemService.getProperties().getProperty(SystemService.DB_LOG_DATA_PROPNAME);
        if (logData != null && !logData.isEmpty()) {
            execute("SET FILES LOG " + logData.toUpperCase());
        }

        String logSize = this.systemService.getProperties().getProperty(SystemService.DB_LOG_SIZE_PROPNAME);
        if (logSize != null && !logSize.isEmpty()) {
            execute("SET FILES LOG SIZE " + logSize);
        }

        String useNio = this.systemService.getProperties().getProperty(SystemService.DB_NIO_PROPNAME);
        if (useNio != null && !useNio.isEmpty()) {
            execute("SET FILES NIO " + useNio.toUpperCase());
        }

        // Note: an automatic checkpoint is performed every time the DB is started.

        // TODO: defrag?

        // for encryption
        // ResultSet rs = stmt.executeQuery("select CRYPT_KEY('AES', null) from some_table");
        // String key = rs.next().getString(1);
        // Store the key in a secure place. Now you can create an encrypted DB like so:
        // DriverManager.getConnection("jdbc:hsqldb:file:_some_encrypted_db;crypt_key="+key+";crypt_type=AES", "SA", "")
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

    private void closeSilently() {
        try {
            if (this.connPool != null) {
                this.connPool.close(0);
            }
        } catch (Exception e) {
            logger.warn("Error during HsqlDbService connection close", e);
        } finally {
            this.connPool = null;
        }
    }
}
