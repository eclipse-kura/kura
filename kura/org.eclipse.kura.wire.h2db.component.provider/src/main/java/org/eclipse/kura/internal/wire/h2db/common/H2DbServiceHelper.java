/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
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
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.db.H2DbService;

/**
 * The Class DbServiceHelper is responsible for providing {@link H2DbService}
 * instance dependent helper methods for quick database related operations
 */
public final class H2DbServiceHelper {

    private static final Logger logger = LogManager.getLogger(H2DbServiceHelper.class);

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
        requireNonNull(dbService, "DB Service cannot be null");
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

    public <T> T withConnection(final H2DbService.ConnectionCallable<T> callable) throws SQLException {
        return dbService.withConnection(callable);
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
