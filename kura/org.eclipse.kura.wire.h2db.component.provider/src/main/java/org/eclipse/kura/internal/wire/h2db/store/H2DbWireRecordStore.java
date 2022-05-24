/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.wire.h2db.store;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.isNull;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.internal.wire.h2db.common.H2DbServiceHelper;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.basedb.store.BaseDbDataTypeMapper;
import org.eclipse.kura.wire.basedb.store.BaseDbDataTypeMapper.JdbcType;
import org.eclipse.kura.wire.basedb.store.BaseDbWireRecordStore;
import org.eclipse.kura.wire.basedb.store.BaseDbWireRecordStoreOptions;

/**
 * The Class DbWireRecordStore is a wire component which is responsible to store
 * the received {@link WireRecord}.
 */
public class H2DbWireRecordStore extends BaseDbWireRecordStore
        implements WireEmitter, WireReceiver, ConfigurableComponent {

    @Override
    protected void truncate(final int noOfRecordsToKeep) {
        final String tableName = this.wireRecordStoreOptions.getTableName();
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        final int maxTableSize = this.wireRecordStoreOptions.getMaximumTableSize();

        try {

            int entriesToDeleteCount = getTableSize() + 1; // +1 to make room for the new record
            if (maxTableSize < noOfRecordsToKeep) {
                logger.info("{} > {}, using {} = {}.", BaseDbWireRecordStoreOptions.CLEANUP_RECORDS_KEEP,
                        BaseDbWireRecordStoreOptions.MAXIMUM_TABLE_SIZE,
                        BaseDbWireRecordStoreOptions.CLEANUP_RECORDS_KEEP,
                        BaseDbWireRecordStoreOptions.MAXIMUM_TABLE_SIZE);
                entriesToDeleteCount -= maxTableSize;
            } else {
                entriesToDeleteCount -= noOfRecordsToKeep;
            }

            final String limit = Integer.toString(entriesToDeleteCount);

            ((H2DbServiceHelper) this.dbHelper).withConnection(c -> {
                final String catalog = c.getCatalog();
                final DatabaseMetaData dbMetaData = c.getMetaData();
                try (final ResultSet rsTbls = dbMetaData.getTables(catalog, null, tableName, getTableType())) {
                    if (rsTbls.next()) {
                        // table does exist, truncate it
                        if (noOfRecordsToKeep == 0) {
                            logger.info("Truncating table {}...", sqlTableName);
                            this.dbHelper.execute(c, MessageFormat.format(getTruncateTableQuery(), sqlTableName));
                        } else {
                            logger.info("Partially emptying table {}", sqlTableName);
                            this.dbHelper.execute(c,
                                    MessageFormat.format(getDeleteRangeTableQuery(), sqlTableName, limit));
                        }
                    }
                }
                return null;
            });
        } catch (final SQLException sqlException) {
            logger.error("Error in truncating the table {}...", sqlTableName, sqlException);
        }
    }

    @Override
    protected int getTableSize() throws SQLException {
        final String tableName = this.wireRecordStoreOptions.getTableName();
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);

        return ((H2DbServiceHelper) this.dbHelper).withConnection(c -> {
            try (final Statement stmt = c.createStatement();
                    final ResultSet rset = stmt
                            .executeQuery(MessageFormat.format(getRowCountTableQuery(), sqlTableName))) {
                rset.next();
                return rset.getInt(1);
            }
        });
    }

    @Override
    protected void reconcileTable(final String tableName) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);

        ((H2DbServiceHelper) this.dbHelper).withConnection(c -> {
            // check for the table that would collect the data of this emitter
            final String catalog = c.getCatalog();
            final DatabaseMetaData dbMetaData = c.getMetaData();
            try (final ResultSet rsTbls = dbMetaData.getTables(catalog, null,
                    this.wireRecordStoreOptions.getTableName(), getTableType())) {
                if (!rsTbls.next()) {
                    // table does not exist, create it
                    logger.info("Creating table {}...", sqlTableName);
                    this.dbHelper.execute(c, MessageFormat.format(getCreateTableQuery(), sqlTableName));
                    createIndex(this.dbHelper.sanitizeSqlTableAndColumnName(tableName + "_TIMESTAMP"), sqlTableName,
                            "(TIMESTAMP DESC)");
                }
            }
            return null;
        });
    }

    @Override
    protected void createIndex(String indexname, String table, String order) throws SQLException {
        ((H2DbServiceHelper) this.dbHelper).withConnection(c -> {
            this.dbHelper.execute(c, MessageFormat.format(getCreateTableIndexQuery(), indexname, table, order));
            return null;
        });
        logger.info("Index {} created, order is {}", indexname, order);
    }

    @Override
    protected void reconcileColumns(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);

        final Map<String, Integer> columns = CollectionUtil.newHashMap();

        ((H2DbServiceHelper) this.dbHelper).withConnection(c -> {
            final String catalog = c.getCatalog();
            final DatabaseMetaData dbMetaData = c.getMetaData();
            try (final ResultSet rsColumns = dbMetaData.getColumns(catalog, null, tableName, null)) {
                // map the columns
                while (rsColumns.next()) {
                    final String colName = rsColumns.getString(getColumnNameString());
                    final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(colName);
                    final int colType = rsColumns.getInt(getDataTypeString());
                    columns.put(sqlColName, colType);
                }
            }

            for (Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {
                final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(entry.getKey());
                final Integer sqlColType = columns.get(sqlColName);
                final JdbcType jdbcType = BaseDbDataTypeMapper.getJdbcType(entry.getValue().getType());
                final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
                if (isNull(sqlColType)) {
                    // add column
                    this.dbHelper.execute(c, MessageFormat.format(getAddColumnQuery(), sqlTableName, sqlColName,
                            jdbcType.getTypeString()));
                } else if (sqlColType != jdbcType.getType()) {
                    // drop old column and add new one
                    this.dbHelper.execute(c, MessageFormat.format(getDropColumnQuery(), sqlTableName, sqlColName));
                    this.dbHelper.execute(c, MessageFormat.format(getAddColumnQuery(), sqlTableName, sqlColName,
                            jdbcType.getTypeString()));
                }
            }

            return null;
        });
    }

    @Override
    protected void insertDataRecord(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);

        final Map<String, TypedValue<?>> wireRecordProperties = wireRecord.getProperties();

        ((H2DbServiceHelper) this.dbHelper).withConnection(c -> {
            try (final PreparedStatement stmt = prepareStatement(c, tableName, wireRecordProperties,
                    new Date().getTime())) {
                stmt.execute();
                c.commit();
                return null;
            }

        });

        logger.debug("Stored typed value");
    }
}
