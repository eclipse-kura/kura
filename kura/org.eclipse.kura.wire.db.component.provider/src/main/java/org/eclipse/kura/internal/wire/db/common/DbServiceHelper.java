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
package org.eclipse.kura.internal.wire.db.common;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.db.H2DbService.ConnectionCallable;

/**
 * The Class DbServiceHelper is responsible for providing {@link H2DbService}
 * instance dependent helper methods for quick database related operations
 */
public final class DbServiceHelper {

    private static final Logger logger = LogManager.getLogger(DbServiceHelper.class);

    private final BaseDbService dbService;

    /**
     * Instantiates a new DB Service Helper.
     *
     * @param dbService
     *            the DB service
     * @throws NullPointerException
     *             if argument is null
     */
    private DbServiceHelper(final BaseDbService dbService) {
        requireNonNull(dbService, "DB Service cannot be null");
        this.dbService = dbService;
    }

    /**
     * Creates instance of {@link DbServiceHelper}
     *
     * @param dbService
     *            the {@link H2DbService}
     * @return the instance of {@link DbServiceHelper}
     * @throws org.eclipse.kura.KuraRuntimeException
     *             if argument is null
     */
    public static DbServiceHelper of(final BaseDbService dbService) {
        return new DbServiceHelper(dbService);
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
    public synchronized void execute(final Connection c, final String sql, final Integer... params)
            throws SQLException {
        requireNonNull(sql, "SQL query cannot be null");
        logger.debug("Executing SQL query... {}", sql);

        try (final PreparedStatement stmt = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setInt(1 + i, params[i]);
            }
            stmt.execute();
            c.commit();
        }

        logger.debug("Executing SQL query... Done");
    }

    public Connection getConnection() throws SQLException {
        return this.dbService.getConnection();
    }

    public <T> T withConnection(final ConnectionCallable<T> callable) throws SQLException {
        if (this.dbService instanceof H2DbService) {
            return ((H2DbService) this.dbService).withConnection(callable);
        } else {
            throw new UnsupportedOperationException(
                    "This operation is not supported by the current DB Service implementation");
        }
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
        requireNonNull(string, "Provided string cannot be null");
        logger.debug("Sanitizing the provided string... {}", string);
        final String sanitizedName = string.replaceAll("\"", "\"\"");
        return "\"" + sanitizedName + "\"";
    }
}
