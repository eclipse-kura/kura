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

import java.sql.SQLException;
import java.util.List;

import org.eclipse.kura.wire.WireRecord;

public interface DbServiceProvider {

    /**
     * Truncates the records in the table
     * 
     * @param noOfRecordsToKeep
     *            the no of records to keep in the table
     * @param tableName
     *            the name of the table
     * @param maxTableSize
     *            the maximum no of records in the table
     * @throws SQLException
     */
    public void truncate(final int noOfRecordsToKeep, final String tableName, final int maxTableSize)
            throws SQLException;

    /**
     * Return the size of the table
     * 
     * @param tableName
     *            the name of the table
     * @return the no of records currently stored in the given table
     * @throws SQLException
     */
    public int getTableSize(final String tableName) throws SQLException;

    /**
     * Reconcile table.
     *
     * @param tableName
     *            the table name
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if the provided argument is null
     */
    public void reconcileTable(final String tableName) throws SQLException;

    /**
     * Reconcile columns.
     *
     * @param tableName
     *            the table name
     * @param wireRecord
     *            the data record
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    public void reconcileColumns(final String tableName, final WireRecord wireRecord) throws SQLException;

    /**
     * Insert the provided {@link WireRecord} to the specified table
     *
     * @param tableName
     *            the table name
     * @param wireRecord
     *            the {@link WireRecord}
     * @throws SQLException
     *             the SQL exception
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    public void insertDataRecord(final String tableName, final WireRecord wireRecord) throws SQLException;

    /**
     * Perform the given query
     * 
     * @param query
     *            the query to be run
     * @return a List of WireRecords that contains the result of the query
     * @throws SQLException
     */
    public List<WireRecord> performSQLQuery(String query) throws SQLException;
}
