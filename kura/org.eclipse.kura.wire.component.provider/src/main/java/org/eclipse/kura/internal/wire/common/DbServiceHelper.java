/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.common;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DbServiceHelper is responsible for providing {@link DbService}
 * instance dependent helper methods for quick database related operations
 */
public final class DbServiceHelper {

    private static final Logger logger = LoggerFactory.getLogger(DbServiceHelper.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private final DbService dbService;

    /**
     * Instantiates a new DB Service Helper.
     *
     * @param dbService
     *            the DB service
     * @throws NullPointerException
     *             if argument is null
     */
    private DbServiceHelper(final DbService dbService) {
        requireNonNull(dbService, message.dbServiceNonNull());
        this.dbService = dbService;
    }

    /**
     * Gets the single instance of DB Service Helper.
     *
     * @param dbService
     *            the DB service
     * @return single instance of Service Helper
     * @throws KuraRuntimeException
     *             if argument is null
     */
    public static DbServiceHelper getInstance(final DbService dbService) {
        return new DbServiceHelper(dbService);
    }

    /**
     * Close the connection instance.
     *
     * @param conn
     *            the connection instance to be closed
     */
    public void close(final Connection conn) {
        logger.debug(message.closingConnection() + conn);
        this.dbService.close(conn);
        logger.debug(message.closingConnectionDone());
    }

    /**
     * close the result sets.
     *
     * @param rss
     *            the result sets
     */
    public void close(final ResultSet... rss) {
        logger.debug(message.closingResultSet() + Arrays.toString(rss));
        this.dbService.close(rss);
        logger.debug(message.closingResultSetDone());
    }

    /**
     * Close the SQL statements.
     *
     * @param stmts
     *            the SQL statements
     */
    public void close(final Statement... stmts) {
        logger.debug(message.closingStatement() + Arrays.toString(stmts));
        this.dbService.close(stmts);
        logger.debug(message.closingStatementDone());
    }

    /**
     * Executes the provided SQL query.
     *
     * @param sql
     *            the SQL query to execute
     * @param params
     *            the extra parameters needed for the query
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if SQL query argument is null
     */
    public synchronized void execute(final String sql, final Integer... params) throws SQLException {
        requireNonNull(sql, message.sqlQueryNonNull());
        logger.debug(message.execSql() + sql);
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setInt(1 + i, params[i]);
            }
            stmt.execute();
            conn.commit();
        } catch (final SQLException e) {
            rollback(conn);
            throw e;
        } finally {
            this.close(stmt);
            this.close(conn);
        }
        logger.debug(message.execSqlDone());
    }

    /**
     * Gets the connection.
     *
     * @return the connection instance
     * @throws SQLException
     *             the SQL exception
     */
    public Connection getConnection() throws SQLException {
        return this.dbService.getConnection();
    }

    /**
     * Rollback the connection.
     *
     * @param conn
     *            the connection instance
     * @throws NullPointerException
     *             if argument is null
     */
    public void rollback(final Connection conn) {
        requireNonNull(conn, message.connectionNonNull());
        logger.debug(message.rollback() + conn);
        this.dbService.rollback(conn);
        logger.debug(message.rollbackDone());
    }

    /**
     * Perform basic SQL table name and column name validation on input string.
     * This is to allow safe encoding of parameters that must contain quotes,
     * while still protecting users from SQL injection on the table names and
     * column names.
     *
     * (any disallowed character --> '_')
     *
     * @param string
     *            the string to be sanitized
     * @return the escaped string
     * @throws NullPointerException
     *             if argument is null
     */
    public String sanitizeSqlTableAndColumnName(final String string) {
        requireNonNull(string, message.stringNonNull());
        logger.debug(message.sanitize() + string);
        String sanitizedName = string.replaceAll("(?=[]\\[+&|!(){}^\"~*?:\\\\-])", "_");
        return "\"" + sanitizedName + "\"";
    }
}
