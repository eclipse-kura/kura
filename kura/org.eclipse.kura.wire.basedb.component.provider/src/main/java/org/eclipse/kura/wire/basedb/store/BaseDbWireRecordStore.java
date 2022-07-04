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
package org.eclipse.kura.wire.basedb.store;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.BaseDBWireComponentQueries;
import org.eclipse.kura.db.BaseDbService;
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
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.basedb.common.BaseDbServiceHelper;
import org.eclipse.kura.wire.basedb.store.BaseDbDataTypeMapper.JdbcType;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class DbWireRecordStore is a wire component which is responsible to store
 * the received {@link WireRecord}.
 */
public class BaseDbWireRecordStore implements WireEmitter, WireReceiver, ConfigurableComponent {

    protected static final Logger logger = LogManager.getLogger(BaseDbWireRecordStore.class);
    protected static final String NULL_TABLE_NAME_ERROR_MSG = "Table name cannot be null";
    protected static final String NULL_WIRE_RECORD_ERROR_MSG = "WireRecord cannot be null";

    protected BaseDbServiceHelper dbHelper;

    private BaseDbService dbService;

    protected BaseDbWireRecordStoreOptions wireRecordStoreOptions;

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    private BaseDBWireComponentQueries dbQueries;

    public synchronized void bindDbService(BaseDbService dbService) {
        this.dbService = dbService;
        this.dbHelper = BaseDbServiceHelper.of(dbService);
        if (nonNull(this.dbService) && nonNull(this.wireRecordStoreOptions)) {
            reconcileDB(this.wireRecordStoreOptions.getTableName());
        }
    }

    public synchronized void unbindDbService(BaseDbService dbService) {
        if (this.dbService == dbService) {
            this.dbHelper = null;
            this.dbService = null;
            this.wireRecordStoreOptions = null;
        }
    }

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    public void bindDbQueries(final BaseDBWireComponentQueries dbQueries) {
        if (isNull(this.dbQueries)) {
            this.dbQueries = dbQueries;
        }
    }

    public void unbindDbQueriese(final BaseDBWireComponentQueries dbQueries) {
        if (this.dbQueries == dbQueries) {
            this.dbQueries = null;
        }
    }

    /**
     * OSGi Service Component callback for activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    public void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug("Activating DB Wire Record Store...");
        this.wireRecordStoreOptions = new BaseDbWireRecordStoreOptions(properties);

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        if (nonNull(this.dbService)) {
            reconcileDB(this.wireRecordStoreOptions.getTableName());
        }

        logger.debug("Activating DB Wire Record Store... Done");
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated service component properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating DB Wire Record Store...");

        this.wireRecordStoreOptions = new BaseDbWireRecordStoreOptions(properties);

        reconcileDB(this.wireRecordStoreOptions.getTableName());

        logger.debug("Updating DB Wire Record Store... Done");
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    public void deactivate(final ComponentContext componentContext) {
        logger.debug("Deactivating DB Wire Record Store...");
        this.dbHelper = null;
        this.dbService = null;
        this.wireRecordStoreOptions = null;
        logger.debug("Deactivating DB Wire Record Store... Done");
    }

    /**
     * Truncates tables containing {@link WireRecord}s
     */
    protected void truncate() {
        final int noOfRecordsToKeep = this.wireRecordStoreOptions.getNoOfRecordsToKeep();

        truncate(noOfRecordsToKeep);
    }

    /**
     * Truncates the records in the table
     *
     * @param noOfRecordsToKeep
     *            the no of records to keep in the table
     */
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

            try (Connection c = this.dbHelper.getConnection()) {
                final String catalog = c.getCatalog();
                final DatabaseMetaData dbMetaData = c.getMetaData();
                try (final ResultSet rsTbls = dbMetaData.getTables(catalog, null, tableName,
                        this.dbQueries.getTableType())) {
                    if (rsTbls.next()) {
                        // table does exist, truncate it
                        if (noOfRecordsToKeep == 0) {
                            logger.info("Truncating table {}...", sqlTableName);
                            this.dbHelper.execute(c, format(this.dbQueries.getTruncateTableQuery(), sqlTableName));
                        } else {
                            logger.info("Partially emptying table {}", sqlTableName);
                            this.dbHelper.execute(c,
                                    format(this.dbQueries.getDeleteRangeTableQuery(), sqlTableName, limit));
                        }
                    }
                }
            }
        } catch (final SQLException sqlException) {
            logger.error("Error in truncating the table {}...", sqlTableName, sqlException);
        }
    }

    protected int getTableSize() throws SQLException {
        final String tableName = this.wireRecordStoreOptions.getTableName();
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);

        try (Connection c = this.dbHelper.getConnection()) {
            try (final Statement stmt = c.createStatement();
                    final ResultSet rset = stmt
                            .executeQuery(format(this.dbQueries.getRowCountTableQuery(), sqlTableName))) {
                rset.next();
                return rset.getInt(1);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEvelope) {
        requireNonNull(wireEvelope, "Wire Envelope cannot be null");
        final List<WireRecord> records = wireEvelope.getRecords();

        if (this.dbHelper == null) {
            logger.warn("H2DbService instance not attached");
            return;
        }

        try {
            if (getTableSize() >= this.wireRecordStoreOptions.getMaximumTableSize()) {
                truncate();
            }
        } catch (SQLException e) {
            logger.warn("Exception while trying to clean db");
        }

        for (WireRecord wireRecord : records) {
            store(wireRecord);
        }

        // emit the list of Wire Records to the downstream components
        this.wireSupport.emit(records);
    }

    /**
     * Stores the provided {@link WireRecord} in the database
     *
     * @param wireRecord
     *            the {@link WireRecord} to be stored
     * @throws NullPointerException
     *             if the provided argument is null
     */
    protected void store(final WireRecord wireRecord) {
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);
        int retryCount = 0;
        final String tableName = this.wireRecordStoreOptions.getTableName();
        do {
            try {
                insertDataRecord(tableName, wireRecord);
                break;
            } catch (final SQLException e) {
                logger.error("Insertion failed. Reconciling Table and Columns...", e);
                reconcileDB(wireRecord, tableName);
                retryCount++;
            }
        } while (retryCount < 2);
    }

    /**
     * Tries to reconcile the database.
     *
     * @param wireRecord
     *            against which the database columns have to be reconciled.
     * @param tableName
     *            the table name in the database that needs to be reconciled.
     */
    protected void reconcileDB(final WireRecord wireRecord, final String tableName) {
        try {
            if (nonNull(tableName) && !tableName.isEmpty()) {
                reconcileTable(tableName);
                reconcileColumns(tableName, wireRecord);
            }
        } catch (final SQLException ee) {
            logger.error("Error while storing Wire Records...", ee);
        }
    }

    /**
     * Tries to reconcile the database.
     *
     * @param tableName
     *            the table name in the database that needs to be reconciled.
     */
    protected synchronized void reconcileDB(final String tableName) {
        try {
            if (nonNull(this.dbHelper) && nonNull(tableName) && !tableName.isEmpty()) {
                reconcileTable(tableName);
            }
        } catch (final SQLException ee) {
            logger.error("Error while storing Wire Records...", ee);
        }
    }

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
    protected void reconcileTable(final String tableName) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);

        try (Connection c = this.dbHelper.getConnection()) {
            // check for the table that would collect the data of this emitter
            final String catalog = c.getCatalog();
            final DatabaseMetaData dbMetaData = c.getMetaData();
            try (final ResultSet rsTbls = dbMetaData.getTables(catalog, null,
                    this.wireRecordStoreOptions.getTableName(), this.dbQueries.getTableType())) {
                if (!rsTbls.next()) {
                    // table does not exist, create it
                    logger.info("Creating table {}...", sqlTableName);
                    this.dbHelper.execute(c, format(this.dbQueries.getCreateTableQuery(), sqlTableName));
                    createIndex(this.dbHelper.sanitizeSqlTableAndColumnName(tableName + "_TIMESTAMP"), sqlTableName,
                            "(TIMESTAMP DESC)");
                }
            }
        }
    }

    protected void createIndex(String indexname, String table, String order) throws SQLException {
        try (Connection c = this.dbHelper.getConnection()) {
            this.dbHelper.execute(c, format(this.dbQueries.getCreateTableIndexQuery(), indexname, table, order));
        }
        logger.info("Index {} created, order is {}", indexname, order);
    }

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
    protected void reconcileColumns(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);

        final Map<String, Integer> columns = CollectionUtil.newHashMap();

        try (Connection c = this.dbHelper.getConnection()) {
            final String catalog = c.getCatalog();
            final DatabaseMetaData dbMetaData = c.getMetaData();
            try (final ResultSet rsColumns = dbMetaData.getColumns(catalog, null, tableName, null)) {
                // map the columns
                while (rsColumns.next()) {
                    final String colName = rsColumns.getString(this.dbQueries.getColumnNameString());
                    final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(colName);
                    final int colType = rsColumns.getInt(this.dbQueries.getDataTypeString());
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
                    this.dbHelper.execute(c, format(this.dbQueries.getAddColumnQuery(), sqlTableName, sqlColName,
                            jdbcType.getTypeString()));
                } else if (sqlColType != jdbcType.getType()) {
                    // drop old column and add new one
                    this.dbHelper.execute(c, format(this.dbQueries.getDropColumnQuery(), sqlTableName, sqlColName));
                    this.dbHelper.execute(c, format(this.dbQueries.getAddColumnQuery(), sqlTableName, sqlColName,
                            jdbcType.getTypeString()));
                }
            }

        }
    }

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
    protected void insertDataRecord(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, NULL_TABLE_NAME_ERROR_MSG);
        requireNonNull(wireRecord, NULL_WIRE_RECORD_ERROR_MSG);

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

    protected PreparedStatement prepareStatement(Connection connection, String tableName,
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
        final String sqlInsert = format(this.dbQueries.getInsertRecordQuery(), sqlTableName, sbCols.toString(),
                sbVals.toString());
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

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    protected String format(String message, Object... arguments) {
        return MessageFormat.format(message, arguments);
    }

}
