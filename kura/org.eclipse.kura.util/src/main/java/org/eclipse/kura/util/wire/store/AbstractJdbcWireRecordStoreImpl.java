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
package org.eclipse.kura.util.wire.store;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.connection.listener.ConnectionListener;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.jdbc.JdbcUtil;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.store.provider.WireRecordStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJdbcWireRecordStoreImpl implements WireRecordStore {

    private static final Logger logger = LoggerFactory.getLogger(AbstractJdbcWireRecordStoreImpl.class);

    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String TYPE_NAME = "TYPE_NAME";

    protected final String tableName;
    protected final String escapedTableName;
    protected final ConnectionProvider connectionProvider;
    protected final JdbcWireRecordStoreQueries queries;

    private Set<ConnectionListener> connectionListeners;

    protected AbstractJdbcWireRecordStoreImpl(final ConnectionProvider connectionProvider, final String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty.");
        }
        this.tableName = tableName;
        this.connectionProvider = requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.escapedTableName = escapeIdentifier(tableName);
        this.queries = buildSqlWireRecordStoreQueries();
    }

    protected AbstractJdbcWireRecordStoreImpl(ConnectionProvider connectionProvider, String tableName,
            Set<ConnectionListener> listeners) {
        this(connectionProvider, tableName);

        this.connectionListeners = listeners;
    }

    protected abstract Optional<String> getMappedSqlType(final TypedValue<?> value);

    protected abstract JdbcWireRecordStoreQueries buildSqlWireRecordStoreQueries();

    protected String escapeIdentifier(final String string) {
        final String escapedName = string.replace("\"", "\"\"");
        return "\"" + escapedName + "\"";
    }

    protected void createTable() throws KuraStoreException {
        this.connectionProvider.withConnection(c -> {
            execute(c, this.queries.getSqlCreateTable());
            return null;
        }, "failed to create table");
    }

    protected void createTimestampIndex() throws KuraStoreException {
        this.connectionProvider.withConnection(c -> {
            execute(c, this.queries.getSqlCreateTimestampIndex());
            return null;
        }, "failed to create index");
    }

    @Override
    public synchronized void truncate(final int noOfRecordsToKeep) throws KuraStoreException {

        this.connectionProvider.withConnection(c -> {
            if (noOfRecordsToKeep == 0) {
                logger.info("Truncating table {}...", escapedTableName);
                execute(c, this.queries.getSqlTruncateTable());
            } else {
                final int tableSize = getTableSize(c);
                final int deleteCount = Math.max(0, tableSize - noOfRecordsToKeep);

                if (deleteCount == 0) {
                    return null;
                }

                logger.info("Partially emptying table {}", escapedTableName);
                execute(c, MessageFormat.format(this.queries.getSqlDeleteRangeTable(), deleteCount));
            }

            return null;
        }, "failed to truncate table");

    }

    @Override
    public synchronized int getSize() throws KuraStoreException {
        return this.connectionProvider.withConnection(this::getTableSize, "failed to determine table size");
    }

    @Override
    public synchronized void insertRecords(final List<WireRecord> records) throws KuraStoreException {
        this.connectionProvider.withConnection(c -> {

            for (final WireRecord r : records) {
                try {
                    createColumns(c, r);
                    insertRecord(c, r);
                } catch (final SQLException e) {
                    logger.info("Reconciling table and columns");
                    execute(c, this.queries.getSqlCreateTable());
                    createColumns(c, r);
                    insertRecord(c, r);
                }
            }

            return null;
        }, "failed to insert records");
    }

    @Override
    public void close() {
        // nothing to close
    }

    protected void createColumns(final Connection c, final WireRecord wireRecord) throws SQLException {

        final Map<String, String> columnTypes = probeColumnTypes(c);

        for (Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {

            createColumn(c, entry.getKey(), entry.getValue(), columnTypes);

        }
    }

    protected void createColumn(final Connection c, final String name, final TypedValue<?> value,
            final Map<String, String> columnTypes) throws SQLException {

        final Optional<String> mappedType = getMappedSqlType(value);

        if (!mappedType.isPresent()) {
            logger.warn("Unsupported typed value: {}", value);
            return;
        }

        final String escapedColName = escapeIdentifier(name);

        if (!columnTypes.containsKey(escapedColName)) {

            logger.debug("creating new column: {} {}", name, mappedType.get());
            execute(c, MessageFormat.format(queries.getSqlAddColumn(), escapedColName, mappedType.get()));

        } else {
            final String actualColumnType = columnTypes.get(escapedColName);

            if (!isCorrectColumnType(value, mappedType.get(), actualColumnType)) {

                logger.debug("changing column type: {} {}", name, mappedType.get());

                execute(c, MessageFormat.format(queries.getSqlDropColumn(), escapedColName));
                execute(c, MessageFormat.format(queries.getSqlAddColumn(), escapedColName, mappedType.get()));
            }
        }
    }

    protected boolean isCorrectColumnType(final TypedValue<?> value, final String mappedType, final String actualType) {
        return mappedType.equals(actualType);
    }

    protected Map<String, String> probeColumnTypes(final Connection c) throws SQLException {
        final Map<String, String> result = new HashMap<>();

        final String catalog = c.getCatalog();
        final DatabaseMetaData dbMetaData = c.getMetaData();
        try (final ResultSet rsColumns = dbMetaData.getColumns(catalog, null, tableName, null)) {

            while (rsColumns.next()) {
                final String colName = getEscapedColumnName(rsColumns);
                final String type = getColumnType(rsColumns);

                result.put(colName, type);
            }
        }

        return result;
    }

    protected String getEscapedColumnName(final ResultSet columnMetadata) throws SQLException {
        return escapeIdentifier(columnMetadata.getString(COLUMN_NAME));
    }

    protected String getColumnType(final ResultSet columnMetadata) throws SQLException {
        return columnMetadata.getString(TYPE_NAME);
    }

    protected void insertRecord(Connection connection, final WireRecord wireRecord) throws SQLException {

        final String insertQuery = buildInsertQuerySql(wireRecord.getProperties());

        final long timestamp = System.currentTimeMillis();

        logger.debug("Storing data into table {}...", escapedTableName);

        try (final PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
            stmt.setLong(1, timestamp);

            int i = 2;

            for (Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {

                setParameterValue(stmt, i, entry.getValue().getValue());

                i++;
            }

            stmt.execute();

            if (isExplicitCommitEnabled()) {
                connection.commit();
            }

            logger.debug("Stored typed value");
        }

    }

    protected String buildInsertQuerySql(final Map<String, TypedValue<?>> properties) {
        final StringBuilder sbCols = new StringBuilder();
        final StringBuilder sbVals = new StringBuilder();

        sbCols.append("TIMESTAMP");
        sbVals.append("?");

        for (final Entry<String, TypedValue<?>> entry : properties.entrySet()) {
            final String escapedColName = escapeIdentifier(entry.getKey());
            sbCols.append(", ").append(escapedColName);
            sbVals.append(", ?");
        }

        return MessageFormat.format(queries.getSqlInsertRecord(), sbCols.toString(), sbVals.toString());
    }

    protected void setParameterValue(final PreparedStatement stmt, final int index, final Object value)
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

    protected int getTableSize(final Connection c) throws SQLException {
        try (final Statement stmt = c.createStatement();
                final ResultSet rset = stmt.executeQuery(this.queries.getSqlRowCount())) {
            return JdbcUtil.getFirstColumnValue(() -> stmt.executeQuery(this.queries.getSqlRowCount()),
                    ResultSet::getInt);
        }
    }

    protected void execute(final Connection c, final String sql, final Object... params) throws SQLException {
        try (final PreparedStatement stmt = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                setParameterValue(stmt, 1 + i, params[i]);
            }
            stmt.execute();

            if (isExplicitCommitEnabled()) {
                c.commit();
            }
        }
    }

    protected boolean isExplicitCommitEnabled() {
        return false;
    }

}
