/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.osgi.annotation.versioning.ProviderType;

/**
 * {@link BaseDbService} offers APIs to acquire and use a JDBC Connection to the embedded SQL database running in the
 * framework.
 * The configuration of the {@link BaseDbService} will determine the configuration of the embedded SQL database.
 * The usage of API is typical for JDBC Connections; the connection is first acquired with getConnection(),
 * and it must be released when the operation is completed with close(). The implementation of the
 * DbService and the returned JdbcConnection will manage the concurrent access into the database appropriately.
 *
 * @since 1.3
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface BaseDbService {

    /**
     * Returns the JDBC Connection to be used to communicate with the embedded SQL database.
     * For each acquired connection, the DbService.close() method must be called.
     *
     * @return Connection to be used
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException;

    /**
     * Releases a previously acquired JDCB connection to the DbService.
     *
     * @param conn
     *            to be released
     */
    public void close(Connection conn);

    /**
     * Utility method to silently rollback a JDBC Connection without throwing SQLExcepton.
     * The method will catch any SQLExcepton thrown and log it.
     */
    public void rollback(Connection conn);

    /**
     * Utility method to silently close a JDBC ResultSet without throwing SQLExcepton.
     * The method will catch any SQLExcepton thrown and log it.
     */
    public void close(ResultSet... rss);

    /**
     * Utility method to silently close a JDBC Statement without throwing SQLExcepton.
     * The method will catch any SQLExcepton thrown and log it.
     */
    public void close(Statement... stmts);
}
