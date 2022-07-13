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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.wire.WireRecord;

public class BaseDbServiceProviderImpl extends CommonDbServiceProvider implements DbServiceProvider {

    private static final Logger logger = LogManager.getLogger(BaseDbServiceProviderImpl.class);
    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS {0} (ID BIGINT AUTO_INCREMENT, TIMESTAMP BIGINT, primary key (ID));";
    private static final String SQL_DELETE_RANGE_TABLE = "DELETE FROM {0} ORDER BY ID ASC LIMIT {1};";
    private static final String SQL_SET_AUTOINCREMENT = "ALTER TABLE {0} AUTO_INCREMENT = {1};";

    public BaseDbServiceProviderImpl(BaseDbService dbService) {
        this.dbHelper = DbServiceHelper.of(dbService);
    }

    @Override
    public void truncate(final int noOfRecordsToKeep, final String tableName, final int maxTableSize)
            throws SQLException {
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);

        int tableSize = getTableSize(tableName);
        int entriesToDeleteCount = tableSize + 1; // +1 to make room for the new record
        if (maxTableSize < noOfRecordsToKeep) {
            entriesToDeleteCount -= maxTableSize;
        } else {
            entriesToDeleteCount -= noOfRecordsToKeep;
        }

        final String limit = Integer.toString(entriesToDeleteCount);

        try (Connection c = this.dbHelper.getConnection()) {
            final String catalog = c.getCatalog();
            final DatabaseMetaData dbMetaData = c.getMetaData();
            try (final ResultSet rsTbls = dbMetaData.getTables(catalog, null, tableName, TABLE_TYPE)) {
                if (rsTbls.next()) {
                    // table does exist, truncate it
                    if (noOfRecordsToKeep == 0) {
                        logger.info("Truncating table {}...", sqlTableName);
                        this.dbHelper.execute(c, format(SQL_TRUNCATE_TABLE, sqlTableName));
                        this.dbHelper.execute(c, format(SQL_SET_AUTOINCREMENT, sqlTableName, tableSize + 1));
                    } else {
                        logger.info("Partially emptying table {}", sqlTableName);
                        this.dbHelper.execute(c, format(SQL_DELETE_RANGE_TABLE, sqlTableName, limit));
                    }
                }
            }
        }

    }

    @Override
    public int getTableSize(final String tableName) throws SQLException {
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        try (Connection c = this.dbHelper.getConnection()) {
            return getTableSize(sqlTableName, c, this::format);
        }
    }

    @Override
    public void reconcileTable(String tableName) throws SQLException {
        try (Connection c = this.dbHelper.getConnection()) {
            reconcileTable(c, tableName, SQL_CREATE_TABLE, this::format);
        }
    }

    @Override
    public void reconcileColumns(String tableName, WireRecord wireRecord) throws SQLException {
        try (Connection c = this.dbHelper.getConnection()) {
            reconcileColumns(c, tableName, wireRecord, this::format);
        }

    }

    @Override
    public void insertDataRecord(String tableName, WireRecord wireRecord) throws SQLException {
        try (Connection c = this.dbHelper.getConnection()) {
            insertDataRecord(c, tableName, wireRecord, this::format);
        }
    }

    @Override
    public List<WireRecord> performSQLQuery(String query) throws SQLException {
        try (Connection c = this.dbHelper.getConnection()) {
            return performSQLQuery(c, query);
        }
    }

    private String format(String message, Object... arguments) {
        return MessageFormat.format(message, arguments).replace("\"", "");
    }
}
