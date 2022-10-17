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
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireRecord;

public class CommonDbServiceProvider {

    private static final Logger logger = LogManager.getLogger(CommonDbServiceProvider.class);
    protected static final String COLUMN_NAME = "COLUMN_NAME";
    protected static final String DATA_TYPE = "DATA_TYPE";
    protected static final String SQL_ADD_COLUMN = "ALTER TABLE {0} ADD COLUMN {1} {2};";
    protected static final String SQL_CREATE_TABLE_INDEX = "CREATE INDEX {0} ON {1} {2};";
    protected static final String SQL_ROW_COUNT_TABLE = "SELECT COUNT(*) FROM {0};";
    protected static final String SQL_DROP_COLUMN = "ALTER TABLE {0} DROP COLUMN {1};";
    protected static final String SQL_INSERT_RECORD = "INSERT INTO {0} ({1}) VALUES ({2});";
    protected static final String SQL_TRUNCATE_TABLE = "TRUNCATE TABLE {0};";
    protected static final String[] TABLE_TYPE = new String[] { "TABLE" };

    protected DbServiceHelper dbHelper;

    protected Integer getTableSize(final String sqlTableName, Connection c,
            BiFunction<String, Object[], String> formatter) throws SQLException {
        try (final Statement stmt = c.createStatement();
                final ResultSet rset = stmt
                        .executeQuery(formatter.apply(SQL_ROW_COUNT_TABLE, new String[] { sqlTableName }))) {
            rset.next();
            return rset.getInt(1);
        }
    }

    protected void reconcileTable(Connection c, final String tableName, final String createTableQuery,
            BiFunction<String, Object[], String> formatter) throws SQLException {
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);

        // check for the table that would collect the data of this emitter
        final String catalog = c.getCatalog();
        final DatabaseMetaData dbMetaData = c.getMetaData();
        try (final ResultSet rsTbls = dbMetaData.getTables(catalog, null, tableName, TABLE_TYPE)) {
            if (!rsTbls.next()) {
                // table does not exist, create it
                logger.info("Creating table {}...", sqlTableName);
                this.dbHelper.execute(c, formatter.apply(createTableQuery, new String[] { sqlTableName }));
                createIndex(c, this.dbHelper.sanitizeSqlTableAndColumnName(tableName + "_TIMESTAMP"), sqlTableName,
                        "(TIMESTAMP DESC)", formatter);
            }
        }
    }

    private void createIndex(Connection c, String indexname, String table, String order,
            BiFunction<String, Object[], String> formatter) throws SQLException {
        this.dbHelper.execute(c, formatter.apply(SQL_CREATE_TABLE_INDEX, new String[] { indexname, table, order }));
        logger.info("Index {} created, order is {}", indexname, order);
    }

    protected void reconcileColumns(Connection c, String tableName, WireRecord wireRecord,
            BiFunction<String, Object[], String> formatter) throws SQLException {
        final Map<String, Integer> columns = CollectionUtil.newHashMap();
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
                this.dbHelper.execute(c, formatter.apply(SQL_ADD_COLUMN,
                        new String[] { sqlTableName, sqlColName, jdbcType.getTypeString() }));
            } else if (sqlColType != jdbcType.getType()) {
                // drop old column and add new one
                this.dbHelper.execute(c, formatter.apply(SQL_DROP_COLUMN, new String[] { sqlTableName, sqlColName }));
                this.dbHelper.execute(c, formatter.apply(SQL_ADD_COLUMN,
                        new String[] { sqlTableName, sqlColName, jdbcType.getTypeString() }));
            }
        }
    }

    protected void insertDataRecord(Connection c, String tableName, WireRecord wireRecord,
            BiFunction<String, Object[], String> formatter) throws SQLException {
        final Map<String, TypedValue<?>> wireRecordProperties = wireRecord.getProperties();
        try (final PreparedStatement stmt = prepareStatement(c, tableName, wireRecordProperties, new Date().getTime(),
                formatter)) {
            stmt.execute();
            c.commit();
            logger.debug("Stored typed value");
        }
    }

    private PreparedStatement prepareStatement(Connection connection, String tableName,
            final Map<String, TypedValue<?>> properties, long timestamp, BiFunction<String, Object[], String> formatter)
            throws SQLException {

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
        final String sqlInsert = formatter.apply(SQL_INSERT_RECORD,
                new String[] { sqlTableName, sbCols.toString(), sbVals.toString() });
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
                stmt.setBlob(i, is, byteArrayValue.length);
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

    protected List<WireRecord> performSQLQuery(Connection c, String query) throws SQLException {
        final List<WireRecord> dataRecords = new ArrayList<>();

        try (final Statement stmt = c.createStatement(); final ResultSet rset = stmt.executeQuery(query)) {
            while (rset.next()) {
                final WireRecord wireRecord = new WireRecord(convertSQLRowToWireRecord(rset));
                dataRecords.add(wireRecord);
            }
        }
        logger.debug("Refreshed typed values");
        return dataRecords;
    }

    private Map<String, TypedValue<?>> convertSQLRowToWireRecord(ResultSet rset) throws SQLException {
        final Map<String, TypedValue<?>> wireRecordProperties = new HashMap<>();
        final ResultSetMetaData rmet = rset.getMetaData();
        for (int i = 1; i <= rmet.getColumnCount(); i++) {
            String fieldName = rmet.getColumnLabel(i);
            Object dbExtractedData = rset.getObject(i);

            if (isNull(fieldName)) {
                fieldName = rmet.getColumnName(i);
            }

            if (isNull(dbExtractedData)) {
                continue;
            }

            if (dbExtractedData instanceof Blob) {
                final Blob dbExtractedBlob = (Blob) dbExtractedData;
                final int dbExtractedBlobLength = (int) dbExtractedBlob.length();
                dbExtractedData = dbExtractedBlob.getBytes(1, dbExtractedBlobLength);
            }

            try {
                final TypedValue<?> value = TypedValues.newTypedValue(dbExtractedData);
                wireRecordProperties.put(fieldName, value);
            } catch (final Exception e) {
                logger.error(
                        "Failed to convert result for column {} (SQL type {}, Java type {}) "
                                + "to any of the supported Wires data type, "
                                + "please consider using a conversion function like CAST in your query. "
                                + "The result for this column will not be included in emitted envelope",
                        fieldName, rmet.getColumnTypeName(i), dbExtractedData.getClass().getName(), e);
            }

        }
        return wireRecordProperties;
    }
}
