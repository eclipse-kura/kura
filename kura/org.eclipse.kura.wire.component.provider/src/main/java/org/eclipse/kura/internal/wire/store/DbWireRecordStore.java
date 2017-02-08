/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.store;

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
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.internal.wire.common.DbServiceHelper;
import org.eclipse.kura.internal.wire.store.DbDataTypeMapper.JdbcType;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.ByteValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.ShortValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DbWireRecordStore is a wire component which is responsible to store
 * the received {@link WireRecord}. <br/>
 * <br/>
 * Also note that, every table name provided by DB Wire Record Store will be
 * prepended by {@code WR_}
 */
public final class DbWireRecordStore implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final String COLUMN_NAME = "COLUMN_NAME";

    private static final String DATA_TYPE = "DATA_TYPE";

    private static final Logger logger = LoggerFactory.getLogger(DbWireRecordStore.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private static final String SQL_ADD_COLUMN = "ALTER TABLE {0} ADD COLUMN {1} {2};";

    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS {0} (TIMESTAMP BIGINT NOT NULL PRIMARY KEY);";

    private static final String SQL_DELETE_RANGE_TABLE = "DELETE FROM {0} LIMIT {1};";

    private static final String SQL_DROP_COLUMN = "ALTER TABLE {0} DROP COLUMN {1};";

    private static final String SQL_INSERT_RECORD = "INSERT INTO {0} ({1}) VALUES ({2});";

    private static final String SQL_TRUNCATE_TABLE = "TRUNCATE TABLE {0};";

    private static final String[] TABLE_TYPE = new String[] { "TABLE" };

    private DbServiceHelper dbHelper;

    private volatile DbService dbService;

    private final ScheduledExecutorService executorService;

    private DbWireRecordStoreOptions wireRecordStoreOptions;

    /** The future handle of the thread pool executor service. */
    private ScheduledFuture<?> tickHandle;

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    public DbWireRecordStore() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Binds the DB service.
     *
     * @param dbService
     *            the new DB service
     */
    public void bindDbService(final DbService dbService) {
        if (isNull(this.dbService)) {
            this.dbService = dbService;
        }
    }

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Unbinds the DB service.
     *
     * @param dbService
     *            the DB service
     */
    public void unbindDbService(final DbService dbService) {
        if (this.dbService == dbService) {
            this.dbService = null;
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
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
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug(message.activatingStore());
        this.wireRecordStoreOptions = new DbWireRecordStoreOptions(properties);
        this.dbHelper = DbServiceHelper.getInstance(this.dbService);
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        scheduleTruncation();
        logger.debug(message.activatingStoreDone());
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated service component properties
     */
    public void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingStore() + properties);
        this.wireRecordStoreOptions = new DbWireRecordStoreOptions(properties);
        scheduleTruncation();
        logger.debug(message.updatingStoreDone());
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingStore());
        if (nonNull(this.tickHandle)) {
            this.tickHandle.cancel(true);
        }
        this.executorService.shutdown();
        logger.debug(message.deactivatingStoreDone());
    }

    /**
     * Schedule truncation of tables containing {@link WireRecord}s
     */
    private void scheduleTruncation() {
        final int cleanUpRate = this.wireRecordStoreOptions.getPeriodicCleanupRate();
        final int noOfRecordsToKeep = this.wireRecordStoreOptions.getNoOfRecordsToKeep();
        // Cancel the current refresh view handle
        if (nonNull(this.tickHandle)) {
            this.tickHandle.cancel(true);
        }
        // schedule the truncation of collected wire records
        if (cleanUpRate != 0) {
            this.tickHandle = this.executorService.scheduleWithFixedDelay(new Runnable() {

                /** {@inheritDoc} */
                @Override
                public void run() {
                    DbWireRecordStore.this.clear(noOfRecordsToKeep);
                }
            }, cleanUpRate, cleanUpRate, TimeUnit.SECONDS);
        }
    }

    /**
     * Truncates the records in the table
     *
     * @param noOfRecordsToKeep
     *            the no of records to keep in the table
     */
    private void clear(final int noOfRecordsToKeep) {
        final String tableName = this.wireRecordStoreOptions.getTableName();
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        Connection conn = null;
        try {
            conn = this.dbHelper.getConnection();

            final String catalog = conn.getCatalog();
            final DatabaseMetaData dbMetaData = conn.getMetaData();
            final ResultSet rsTbls = dbMetaData.getTables(catalog, null, tableName, TABLE_TYPE);
            if (rsTbls.next()) {
                // table does exist, truncate it
                if (noOfRecordsToKeep == 0) {
                    logger.info(message.truncatingTable(sqlTableName));
                    this.dbHelper.execute(MessageFormat.format(SQL_TRUNCATE_TABLE, sqlTableName));
                } else {
                    this.dbHelper
                            .execute(MessageFormat.format(SQL_DELETE_RANGE_TABLE, sqlTableName, noOfRecordsToKeep));
                }
            }
        } catch (final SQLException sqlException) {
            logger.error(message.errorTruncatingTable(sqlTableName), sqlException);
        } finally {
            if (nonNull(conn)) {
                this.dbHelper.close(conn);
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
        requireNonNull(wireEvelope, message.wireEnvelopeNonNull());
        logger.debug(message.wireEnvelopeReceived() + this.wireSupport);

        final List<WireRecord> records = wireEvelope.getRecords();
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
    private void store(final WireRecord wireRecord) {
        requireNonNull(wireRecord, message.wireRecordNonNull());
        int retryCount = 0;
        final String tableName = this.wireRecordStoreOptions.getTableName();
        do {
            try {
                insertDataRecord(tableName, wireRecord);
                break;
            } catch (final SQLException e) {
                logger.error(message.insertionFailed(), e);
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
    private void reconcileDB(final WireRecord wireRecord, final String tableName) {
        try {
            if (nonNull(tableName) && !tableName.isEmpty()) {
                reconcileTable(tableName);
                reconcileColumns(tableName, wireRecord);
            }
        } catch (final SQLException ee) {
            logger.error(message.errorStoring() + ee);
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
    private void reconcileTable(final String tableName) throws SQLException {
        requireNonNull(tableName, message.tableNameNonNull());
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        final Connection conn = this.dbHelper.getConnection();
        try {
            // check for the table that would collect the data of this emitter
            final String catalog = conn.getCatalog();
            final DatabaseMetaData dbMetaData = conn.getMetaData();
            final ResultSet rsTbls = dbMetaData.getTables(catalog, null, this.wireRecordStoreOptions.getTableName(),
                    TABLE_TYPE);
            if (!rsTbls.next()) {
                // table does not exist, create it
                logger.info(message.creatingTable(sqlTableName));
                this.dbHelper.execute(MessageFormat.format(SQL_CREATE_TABLE, sqlTableName));
            }
        } finally {
            this.dbHelper.close(conn);
        }
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
    private void reconcileColumns(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, message.tableNameNonNull());
        requireNonNull(wireRecord, message.wireRecordNonNull());

        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        Connection conn = null;
        ResultSet rsColumns = null;
        final Map<String, Integer> columns = CollectionUtil.newHashMap();
        try {
            // check for the table that would collect the data of this emitter
            conn = this.dbHelper.getConnection();
            final String catalog = conn.getCatalog();
            final DatabaseMetaData dbMetaData = conn.getMetaData();
            rsColumns = dbMetaData.getColumns(catalog, null, sqlTableName, null);
            // map the columns
            while (rsColumns.next()) {
                final String colName = rsColumns.getString(COLUMN_NAME);
                final int colType = rsColumns.getInt(DATA_TYPE);
                columns.put(colName, colType);
            }
        } finally {
            this.dbHelper.close(rsColumns);
            this.dbHelper.close(conn);
        }
        // reconcile columns
        for (Entry<String, TypedValue<?>> entry : wireRecord.getProperties().entrySet()) {
            final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(entry.getKey());
            final Integer sqlColType = columns.get(sqlColName);
            final JdbcType jdbcType = DbDataTypeMapper.getJdbcType(entry.getValue().getType());
            if (isNull(sqlColType)) {
                // add column
                this.dbHelper.execute(
                        MessageFormat.format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString()));
            } else if (sqlColType != jdbcType.getType()) {
                // drop old column and add new one
                this.dbHelper.execute(MessageFormat.format(SQL_DROP_COLUMN, sqlTableName, sqlColName));
                this.dbHelper.execute(
                        MessageFormat.format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString()));
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
    private void insertDataRecord(final String tableName, final WireRecord wireRecord) throws SQLException {
        requireNonNull(tableName, message.tableNameNonNull());
        requireNonNull(wireRecord, message.wireRecordNonNull());

        final Map<String, TypedValue<?>> wireRecordProperties = wireRecord.getProperties();

        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = this.dbHelper.getConnection();
            stmt = prepareStatement(connection, tableName, wireRecordProperties, new Date().getTime());
            stmt.execute();
            connection.commit();
            logger.info(message.stored());
        } catch (final SQLException e) {
            this.dbHelper.rollback(connection);
            throw e;
        } finally {
            this.dbHelper.close(stmt);
            this.dbHelper.close(connection);
        }
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
            sbCols.append(", " + sqlColName);
            sbVals.append(", ?");
        }

        logger.debug(message.storingRecord(sqlTableName));
        final String sqlInsert = MessageFormat.format(SQL_INSERT_RECORD, sqlTableName, sbCols.toString(),
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
            case BYTE:
                stmt.setByte(i, ((ByteValue) value).getValue());
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
            case SHORT:
                stmt.setShort(i, ((ShortValue) value).getValue());
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
}
