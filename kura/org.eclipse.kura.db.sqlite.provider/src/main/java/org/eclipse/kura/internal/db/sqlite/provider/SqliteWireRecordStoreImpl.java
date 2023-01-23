/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.db.sqlite.provider;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.store.provider.WireRecordStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqliteWireRecordStoreImpl implements WireRecordStore {

    private static final Logger logger = LoggerFactory.getLogger(SqliteWireRecordStoreImpl.class);

    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String TYPE_NAME = "TYPE_NAME";

    private static final String SQL_ADD_COLUMN = "ALTER TABLE {0} ADD COLUMN {1} {2};";

    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS {0} (ID INTEGER PRIMARY KEY, TIMESTAMP BIGINT);";

    private static final String SQL_CREATE_TABLE_INDEX = "CREATE INDEX IF NOT EXISTS {0} ON {1} {2};";

    private static final String SQL_ROW_COUNT_TABLE = "SELECT COUNT(*) FROM {0};";

    private static final String SQL_DELETE_RANGE_TABLE = "DELETE FROM {0} WHERE ID IN (SELECT ID FROM {0} ORDER BY ID ASC LIMIT {1});";

    private static final String SQL_DROP_COLUMN = "ALTER TABLE {0} DROP COLUMN {1};";

    private static final String SQL_INSERT_RECORD = "INSERT INTO {0} ({1}) VALUES ({2});";

    private static final String SQL_TRUNCATE_TABLE = "TRUNCATE TABLE {0};";

    private final String tableName;
    private final String sanitizedTableName;
    private final SqliteDbServiceImpl sqliteDbService;

    public SqliteWireRecordStoreImpl(final SqliteDbServiceImpl sqliteDbService, final String tableName)
            throws KuraStoreException {
        this.tableName = tableName;
        this.sanitizedTableName = sanitizeSqlTableAndColumnName(tableName);
        this.sqliteDbService = sqliteDbService;

        withConnection(c -> {
            this.createTable(c);
            return null;
        });
    }

    @Override
    public synchronized void truncate(final int noOfRecordsToKeep) throws KuraStoreException {

        withConnection(c -> {
            if (noOfRecordsToKeep == 0) {
                logger.info("Truncating table {}...", sanitizedTableName);
                execute(c, MessageFormat.format(SQL_TRUNCATE_TABLE, sanitizedTableName));
            } else {
                final int tableSize = getTableSize(c);
                final int deleteCount = Math.max(0, tableSize - noOfRecordsToKeep);

                if (deleteCount == 0) {
                    return null;
                }

                logger.info("Partially emptying table {}", sanitizedTableName);
                execute(c, MessageFormat.format(SQL_DELETE_RANGE_TABLE, sanitizedTableName, deleteCount));
            }

            return null;
        });

    }

    @Override
    public synchronized int getSize() throws KuraStoreException {
        return withConnection(this::getTableSize);
    }

    @Override
    public synchronized void insertRecords(final List<WireRecord> records) throws KuraStoreException {
        withConnection(c -> {

            for (final WireRecord r : records) {
                try {
                    createColumns(c, r);
                    insertRecord(c, r);
                } catch (final SQLException e) {
                    logger.info("Reconciling table and columns");
                    createTable(c);
                    createColumns(c, r);
                    insertRecord(c, r);
                }
            }

            return null;
        });
    }

    @Override
    public void close() {
        // nothing to shutdown
    }

    private void createTable(final Connection c) throws SQLException {
        execute(c, MessageFormat.format(SQL_CREATE_TABLE, sanitizedTableName));
        execute(c, MessageFormat.format(SQL_CREATE_TABLE_INDEX, sanitizeSqlTableAndColumnName(tableName + "_TIMESTAMP"),
                sanitizedTableName, "(TIMESTAMP DESC)"));
    }

    private void createColumns(final Connection c, final WireRecord wireRecord) throws SQLException {

        final Map<String, Optional<SqliteType>> columnTypes = probeColumnTypes(c);

        for (Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {

            createColumn(c, entry.getKey(), entry.getValue(), columnTypes);

        }
    }

    private void createColumn(final Connection c, final String name, final TypedValue<?> value,
            final Map<String, Optional<SqliteType>> columnTypes) throws SQLException {

        final Optional<SqliteType> expectedType = SqliteType.forTypedValue(value);

        if (!expectedType.isPresent()) {
            logger.warn("Unsupported typed value: {}", value);
            return;
        }

        final String sqlColName = sanitizeSqlTableAndColumnName(name);

        if (!columnTypes.containsKey(name)) {

            logger.debug("creating new column: {} {}", name, expectedType.get());
            execute(c, MessageFormat.format(SQL_ADD_COLUMN, sanitizedTableName, sqlColName, expectedType.get().name()));

        } else {
            final Optional<SqliteType> columnType = columnTypes.get(name);

            if (!expectedType.equals(columnType)) {

                logger.debug("changing column type: {} {}", name, expectedType.get());

                execute(c, MessageFormat.format(SQL_DROP_COLUMN, sanitizedTableName, sqlColName));
                execute(c, MessageFormat.format(SQL_ADD_COLUMN, sanitizedTableName, sqlColName, expectedType.get()));
            }
        }
    }

    private final Map<String, Optional<SqliteType>> probeColumnTypes(final Connection c) throws SQLException {
        final Map<String, Optional<SqliteType>> result = new HashMap<>();

        final String catalog = c.getCatalog();
        final DatabaseMetaData dbMetaData = c.getMetaData();
        try (final ResultSet rsColumns = dbMetaData.getColumns(catalog, null, tableName, null)) {

            while (rsColumns.next()) {
                final String colName = rsColumns.getString(COLUMN_NAME);
                final Optional<SqliteType> type = SqliteType.fromString(rsColumns.getString(TYPE_NAME));

                result.put(colName, type);
            }
        }

        return result;
    }

    private void insertRecord(Connection connection, final WireRecord wireRecord) throws SQLException {

        final String insertQuery = buildInsertQuerySql(wireRecord.getProperties());

        final long timestamp = System.currentTimeMillis();

        logger.debug("Storing data into table {}...", sanitizedTableName);

        try (final PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
            stmt.setLong(1, timestamp);

            int i = 2;

            for (Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {

                setParameterValue(stmt, i, entry.getValue().getValue());

                i++;
            }

            stmt.execute();
            connection.commit();
            logger.debug("Stored typed value");
        }

    }

    private String buildInsertQuerySql(final Map<String, TypedValue<?>> properties) {
        final StringBuilder sbCols = new StringBuilder();
        final StringBuilder sbVals = new StringBuilder();

        sbCols.append("TIMESTAMP");
        sbVals.append("?");

        for (final Entry<String, TypedValue<?>> entry : properties.entrySet()) {
            final String sqlColName = sanitizeSqlTableAndColumnName(entry.getKey());
            sbCols.append(", ").append(sqlColName);
            sbVals.append(", ?");
        }

        return MessageFormat.format(SQL_INSERT_RECORD, sanitizedTableName, sbCols.toString(), sbVals.toString());
    }

    private void setParameterValue(final PreparedStatement stmt, final int index, final Object value)
            throws SQLException {
        if (value instanceof String) {
            stmt.setString(index, (String) value);
        } else if (value instanceof Integer) {
            stmt.setInt(index, (int) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (double) value);
        } else if (value instanceof Boolean) {
            stmt.setBoolean(index, (boolean) value);
        } else if (value instanceof Float) {
            stmt.setFloat(index, (float) value);
        } else if (value instanceof Long) {
            stmt.setLong(index, (long) value);
        } else if (value instanceof byte[]) {
            stmt.setBytes(index, (byte[]) value);
        } else {
            logger.warn("Unsupported value type {}", value.getClass());
        }
    }

    private int getTableSize(final Connection c) throws SQLException {
        try (final Statement stmt = c.createStatement();
                final ResultSet rset = stmt
                        .executeQuery(MessageFormat.format(SQL_ROW_COUNT_TABLE, sanitizedTableName))) {
            rset.next();
            return rset.getInt(1);
        }
    }

    private void execute(final Connection c, final String sql, final Object... params) throws SQLException {
        try (final PreparedStatement stmt = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                setParameterValue(stmt, 1 + i, params[i]);
            }
            stmt.execute();
            c.commit();
        }
    }

    private <T> T withConnection(final ConnectionCallable<T> callable) throws KuraStoreException {
        try {
            try (final Connection c = this.sqliteDbService.getConnection()) {
                return callable.call(c);
            }
        } catch (Exception e) {
            throw new KuraStoreException(e, null);
        }
    }

    private String sanitizeSqlTableAndColumnName(final String string) {
        final String sanitizedName = string.replace("\"", "\"\"");
        return "\"" + sanitizedName + "\"";
    }

    public interface ConnectionCallable<T> {

        public T call(Connection connection) throws SQLException;
    }

    private enum SqliteType {

        TEXT,
        INT,
        BIGINT,
        BOOLEAN,
        DOUBLE,
        FLOAT,
        BLOB;

        private static final Map<Class<? extends TypedValue<?>>, SqliteType> TYPE_MAPPING = buildTypeMapping();

        private static Map<Class<? extends TypedValue<?>>, SqliteType> buildTypeMapping() {
            final Map<Class<? extends TypedValue<?>>, SqliteType> result = new HashMap<>();

            result.put(StringValue.class, TEXT);
            result.put(IntegerValue.class, INT);
            result.put(LongValue.class, BIGINT);
            result.put(BooleanValue.class, BOOLEAN);
            result.put(DoubleValue.class, DOUBLE);
            result.put(FloatValue.class, FLOAT);
            result.put(ByteArrayValue.class, BLOB);

            return Collections.unmodifiableMap(result);
        }

        public static Optional<SqliteType> forTypedValue(final TypedValue<?> typedValue) {
            return Optional.ofNullable(typedValue).flatMap(t -> Optional.ofNullable(TYPE_MAPPING.get(t.getClass())));
        }

        public static Optional<SqliteType> fromString(final String asString) {
            try {
                return Optional.of(SqliteType.valueOf(asString));
            } catch (final Exception e) {
                return Optional.empty();
            }
        }
    }
}
