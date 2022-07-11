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

import static java.util.Objects.isNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.internal.wire.db.store.DbDataTypeMapper;
import org.eclipse.kura.internal.wire.db.store.DbDataTypeMapper.JdbcType;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireRecord;

public class BaseDbServiceProviderImpl implements DbServiceProvider {

    private static final Logger logger = LogManager.getLogger(BaseDbServiceProviderImpl.class);
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String DATA_TYPE = "DATA_TYPE";
    private static final String SQL_ADD_COLUMN = "ALTER TABLE {0} ADD COLUMN {1} {2};";
    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS {0} (ID BIGINT AUTO_INCREMENT, TIMESTAMP BIGINT, primary key (ID));";
    private static final String SQL_CREATE_TABLE_INDEX = "CREATE INDEX {0} ON {1} {2};";
    private static final String SQL_ROW_COUNT_TABLE = "SELECT COUNT(*) FROM {0};";
    private static final String SQL_DELETE_RANGE_TABLE = "DELETE FROM {0} ORDER BY ID ASC LIMIT {1};";
    private static final String SQL_DROP_COLUMN = "ALTER TABLE {0} DROP COLUMN {1};";
    private static final String SQL_INSERT_RECORD = "INSERT INTO {0} ({1}) VALUES ({2});";
    private static final String SQL_TRUNCATE_TABLE = "TRUNCATE TABLE {0};";
    private static final String SQL_SET_AUTOINCREMENT = "ALTER TABLE {0} AUTO_INCREMENT = {1};";
    private static final String[] TABLE_TYPE = new String[] { "TABLE" };

    private final DbServiceHelper dbHelper;

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
            try (final Statement stmt = c.createStatement();
                    final ResultSet rset = stmt.executeQuery(format(SQL_ROW_COUNT_TABLE, sqlTableName))) {
                rset.next();
                return rset.getInt(1);
            }
        }
    }

    @Override
    public void reconcileTable(String tableName) throws SQLException {
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);

        try (Connection c = this.dbHelper.getConnection()) {
            // check for the table that would collect the data of this emitter
            final String catalog = c.getCatalog();
            final DatabaseMetaData dbMetaData = c.getMetaData();
            try (final ResultSet rsTbls = dbMetaData.getTables(catalog, null, tableName, TABLE_TYPE)) {
                if (!rsTbls.next()) {
                    // table does not exist, create it
                    logger.info("Creating table {}...", sqlTableName);
                    this.dbHelper.execute(c, format(SQL_CREATE_TABLE, sqlTableName));
                    createIndex(this.dbHelper.sanitizeSqlTableAndColumnName(tableName + "_TIMESTAMP"), sqlTableName,
                            "(TIMESTAMP DESC)");
                }
            }
        }
    }

    private void createIndex(String indexname, String table, String order) throws SQLException {
        try (Connection c = this.dbHelper.getConnection()) {
            this.dbHelper.execute(c, format(SQL_CREATE_TABLE_INDEX, indexname, table, order));
        }
        logger.info("Index {} created, order is {}", indexname, order);
    }

    @Override
    public void reconcileColumns(String tableName, WireRecord wireRecord) throws SQLException {
        final Map<String, Integer> columns = CollectionUtil.newHashMap();

        try (Connection c = this.dbHelper.getConnection()) {
            final String catalog = c.getCatalog();
            final DatabaseMetaData dbMetaData = c.getMetaData();
            try (final ResultSet rsColumns = dbMetaData.getColumns(catalog, null, tableName, null)) {
                // map the columns
                while (rsColumns.next()) {
                    final String colName = rsColumns.getString(COLUMN_NAME);
                    final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(colName);
                    final int colType = rsColumns.getInt(DATA_TYPE);
                    columns.put(sqlColName, colType);
                }
            }

            for (Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {
                final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(entry.getKey());
                final Integer sqlColType = columns.get(sqlColName);
                final JdbcType jdbcType = DbDataTypeMapper.getJdbcType(entry.getValue().getType());
                final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
                if (isNull(sqlColType)) {
                    // add column
                    this.dbHelper.execute(c,
                            format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString()));
                } else if (sqlColType != jdbcType.getType()) {
                    // drop old column and add new one
                    this.dbHelper.execute(c, format(SQL_DROP_COLUMN, sqlTableName, sqlColName));
                    this.dbHelper.execute(c,
                            format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString()));
                }
            }
        }

    }

    @Override
    public void insertDataRecord(String tableName, WireRecord wireRecord) throws SQLException {
        final Map<String, TypedValue<?>> wireRecordProperties = wireRecord.getProperties();

        try (Connection c = this.dbHelper.getConnection()) {
            try (final PreparedStatement stmt = prepareStatement(c, tableName, wireRecordProperties,
                    new Date().getTime())) {
                stmt.execute();
                c.commit();
            }
        }
        logger.debug("Stored typed value");
    }

    private PreparedStatement prepareStatement(Connection connection, String tableName,
            final Map<String, TypedValue<?>> properties, long timestamp) throws SQLException {

        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        final StringBuilder sbCols = new StringBuilder();
        final StringBuilder sbVals = new StringBuilder();

        // add the timestamp
        sbCols.append("TIMESTAMP");
        sbVals.append("?");

        int i = 2;
        for (Entry<String, TypedValue<?>> entry : properties.entrySet()) {
            final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(entry.getKey());
            sbCols.append(", ").append(sqlColName);
            sbVals.append(", ?");
        }

        logger.debug("Storing data into table {}...", sqlTableName);
        final String sqlInsert = format(SQL_INSERT_RECORD, sqlTableName, sbCols.toString(), sbVals.toString());
        final PreparedStatement stmt = connection.prepareStatement(sqlInsert);
        stmt.setLong(1, timestamp);

        for (Entry<String, TypedValue<?>> entry : properties.entrySet()) {
            final DataType dataType = entry.getValue().getType();
            final Object value = entry.getValue();
            switch (dataType) {
            case BOOLEAN:
                stmt.setBoolean(i, ((BooleanValue) value).getValue());
                break;
            case FLOAT:
                stmt.setFloat(i, ((FloatValue) value).getValue());
                break;
            case DOUBLE:
                stmt.setDouble(i, ((DoubleValue) value).getValue());
                break;
            case INTEGER:
                stmt.setInt(i, ((IntegerValue) value).getValue());
                break;
            case LONG:
                stmt.setLong(i, ((LongValue) value).getValue());
                break;
            case BYTE_ARRAY:
                byte[] byteArrayValue = ((ByteArrayValue) value).getValue();
                InputStream is = new ByteArrayInputStream(byteArrayValue);
                stmt.setBlob(i, is);
                break;
            case STRING:
                stmt.setString(i, ((StringValue) value).getValue());
                break;
            default:
                break;
            }
            i++;
        }
        return stmt;

    }

    private String format(String message, Object... arguments) {
        return MessageFormat.format(message, arguments).replace("\"", "");
    }
}
