/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.h2db.common;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DbServiceHelper is responsible for providing {@link H2DbService}
 * instance dependent helper methods for quick database related operations
 */
public final class H2DbServiceHelper {

    private static final Logger logger = LoggerFactory.getLogger(H2DbServiceHelper.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private final H2DbService dbService;

    /**
     * Instantiates a new DB Service Helper.
     *
     * @param dbService
     *            the DB service
     * @throws NullPointerException
     *             if argument is null
     */
    private H2DbServiceHelper(final H2DbService dbService) {
        requireNonNull(dbService, message.dbServiceNonNull());
        this.dbService = dbService;
    }

    /**
     * Creates instance of {@link H2DbServiceHelper}
     *
     * @param dbService
     *            the {@link H2DbService}
     * @return the instance of {@link H2DbServiceHelper}
     * @throws KuraRuntimeException
     *             if argument is null
     */
    public static H2DbServiceHelper of(final H2DbService dbService) {
        return new H2DbServiceHelper(dbService);
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
     * Encloses the provided String between double quotes and escapes
     * any double quote present in the string.
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
        final String sanitizedName = string.replaceAll("\"", "\"\"");
        return "\"" + sanitizedName + "\"";
    }
}
