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
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.jdbc.ConnectionProvider;
import org.eclipse.kura.util.jdbc.JdbcUtil;
import org.eclipse.kura.wire.WireRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlWireRecordStoreHelper {

    private static final Logger logger = LoggerFactory.getLogger(SqlWireRecordStoreHelper.class);

    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String TYPE_NAME = "TYPE_NAME";

    private final String tableName;
    private final String sanitizedTableName;
    private final ConnectionProvider connectionProvider;
    private final SqlWireRecordStoreQueries queries;
    private final Function<TypedValue<?>, Optional<String>> sqlTypeMapper;
    private final UnaryOperator<String> sanitizer;
    private final boolean isExplicitCommitEnabled;

    private SqlWireRecordStoreHelper(final Builder builder) {
        this.tableName = requireNonNull(builder.tableName);
        this.sanitizer = requireNonNull(builder.sanitizer);
        this.connectionProvider = requireNonNull(builder.connectionProvider);
        this.queries = requireNonNull(builder.queries);
        this.sqlTypeMapper = requireNonNull(builder.sqlTypeMapper);
        this.isExplicitCommitEnabled = builder.isExplicitCommitEnabled;

        this.sanitizedTableName = builder.sanitizer.apply(tableName);
    }

    public void createTable() throws KuraStoreException {
        this.connectionProvider.withConnection(c -> {
            execute(c, this.queries.getSqlCreateTable());
            return null;
        }, "failed to create table");
    }

    public void createTimestampIndex() throws KuraStoreException {
        this.connectionProvider.withConnection(c -> {
            execute(c, this.queries.getSqlCreateTimestampIndex());
            return null;
        }, "failed to create index");
    }

    public void truncate(final int noOfRecordsToKeep) throws KuraStoreException {

        this.connectionProvider.withConnection(c -> {
            if (noOfRecordsToKeep == 0) {
                logger.info("Truncating table {}...", sanitizedTableName);
                execute(c, this.queries.getSqlTruncateTable());
            } else {
                final int tableSize = getTableSize(c);
                final int deleteCount = Math.max(0, tableSize - noOfRecordsToKeep);

                if (deleteCount == 0) {
                    return null;
                }

                logger.info("Partially emptying table {}", sanitizedTableName);
                execute(c, MessageFormat.format(this.queries.getSqlDeleteRangeTable(), deleteCount));
            }

            return null;
        }, "failed to truncate table");

    }

    public int getSize() throws KuraStoreException {
        return this.connectionProvider.withConnection(this::getTableSize, "failed to determine table size");
    }

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

    public void createColumns(final Connection c, final WireRecord wireRecord) throws SQLException {

        final Map<String, String> columnTypes = probeColumnTypes(c);

        for (Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {

            createColumn(c, entry.getKey(), entry.getValue(), columnTypes);

        }
    }

    public void createColumn(final Connection c, final String name, final TypedValue<?> value,
            final Map<String, String> columnTypes) throws SQLException {

        final Optional<String> expectedType = this.sqlTypeMapper.apply(value);

        if (!expectedType.isPresent()) {
            logger.warn("Unsupported typed value: {}", value);
            return;
        }

        final String sqlColName = sanitizer.apply(name);

        if (!columnTypes.containsKey(name)) {

            logger.debug("creating new column: {} {}", name, expectedType.get());
            execute(c, MessageFormat.format(queries.getSqlAddColumn(), sqlColName,
                    expectedType.get()));

        } else {
            final Optional<String> columnType = Optional.ofNullable(columnTypes.get(name));

            if (!expectedType.equals(columnType)) {

                logger.debug("changing column type: {} {}", name, expectedType.get());

                execute(c, MessageFormat.format(queries.getSqlDropColumn(), sqlColName));
                execute(c, MessageFormat.format(queries.getSqlAddColumn(), sqlColName,
                        expectedType.get()));
            }
        }
    }

    public final Map<String, String> probeColumnTypes(final Connection c) throws SQLException {
        final Map<String, String> result = new HashMap<>();

        final String catalog = c.getCatalog();
        final DatabaseMetaData dbMetaData = c.getMetaData();
        try (final ResultSet rsColumns = dbMetaData.getColumns(catalog, null, tableName, null)) {

            while (rsColumns.next()) {
                final String colName = rsColumns.getString(COLUMN_NAME);
                final String type = rsColumns.getString(TYPE_NAME);

                result.put(colName, type);
            }
        }

        return result;
    }

    public void insertRecord(Connection connection, final WireRecord wireRecord) throws SQLException {

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

            if (isExplicitCommitEnabled) {
                connection.commit();
            }

            logger.debug("Stored typed value");
        }

    }

    public String buildInsertQuerySql(final Map<String, TypedValue<?>> properties) {
        final StringBuilder sbCols = new StringBuilder();
        final StringBuilder sbVals = new StringBuilder();

        sbCols.append("TIMESTAMP");
        sbVals.append("?");

        for (final Entry<String, TypedValue<?>> entry : properties.entrySet()) {
            final String sqlColName = sanitizer.apply(entry.getKey());
            sbCols.append(", ").append(sqlColName);
            sbVals.append(", ?");
        }

        return MessageFormat.format(queries.getSqlInsertRecord(), sbCols.toString(),
                sbVals.toString());
    }

    public void setParameterValue(final PreparedStatement stmt, final int index, final Object value)
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

    public int getTableSize(final Connection c) throws SQLException {
        try (final Statement stmt = c.createStatement();
                final ResultSet rset = stmt
                        .executeQuery(this.queries.getSqlRowCount())) {
            return JdbcUtil.getFirstColumnValue(() -> stmt
                    .executeQuery(this.queries.getSqlRowCount()), ResultSet::getInt);
        }
    }

    public void execute(final Connection c, final String sql, final Object... params) throws SQLException {
        try (final PreparedStatement stmt = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                setParameterValue(stmt, 1 + i, params[i]);
            }
            stmt.execute();

            if (isExplicitCommitEnabled) {
                c.commit();
            }
        }
    }

    public interface ConnectionCallable<T> {

        public T call(Connection connection) throws SQLException;
    }

    public interface SqlTypeMapper {
        public Optional<String> getMappedType(final TypedValue<?> value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String tableName;
        private ConnectionProvider connectionProvider;
        private SqlWireRecordStoreQueries queries;
        private Function<TypedValue<?>, Optional<String>> sqlTypeMapper;
        private UnaryOperator<String> sanitizer;
        private boolean isExplicitCommitEnabled;

        private Builder() {
        }

        public Builder withTableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder withConnectionProvider(ConnectionProvider connectionProvider) {
            this.connectionProvider = connectionProvider;
            return this;
        }

        public Builder withQueries(SqlWireRecordStoreQueries queries) {
            this.queries = queries;
            return this;
        }

        public Builder withSqlTypeMapper(Function<TypedValue<?>, Optional<String>> sqlTypeMapper) {
            this.sqlTypeMapper = sqlTypeMapper;
            return this;
        }

        public Builder withSanitizer(UnaryOperator<String> sanitizer) {
            this.sanitizer = sanitizer;
            return this;
        }

        public Builder withExplicitCommitEnabled(boolean isExplicitCommitEnabled) {
            this.isExplicitCommitEnabled = isExplicitCommitEnabled;
            return this;
        }

        public SqlWireRecordStoreHelper build() {
            return new SqlWireRecordStoreHelper(this);
        }
    }

}
