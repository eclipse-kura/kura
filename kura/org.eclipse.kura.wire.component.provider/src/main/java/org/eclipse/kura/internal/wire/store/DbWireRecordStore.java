/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.wire.store;

import static org.eclipse.kura.Preconditions.checkNull;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraRuntimeException;
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
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
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
 * the received Wire Record. <br/>
 * <br/>
 * Also note that, every table name provided by DB Wire Record Store will be
 * prepended by {@code WR_}
 */
public final class DbWireRecordStore implements WireEmitter, WireReceiver, ConfigurableComponent {

    /** The Constant denoting name of the column. */
    private static final String COLUMN_NAME = "COLUMN_NAME";

    /** The constant data type */
    private static final String DATA_TYPE = "DATA_TYPE";

    /** The table name prefix to be used */
    public static final String PREFIX = "WR_";

    /** The Logger instance. */
    private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordStore.class);

    /** Localization Resource */
    private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

    /** The Constant denoting query to add column. */
    private static final String SQL_ADD_COLUMN = "ALTER TABLE {0} ADD COLUMN {1} {2};";

    /** The Constant denoting query to create table. */
    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS {0} (TIMESTAMP BIGINT NOT NULL PRIMARY KEY);";

    /** The Constant denoting query to delete records in a table. */
    private static final String SQL_DELETE_RANGE_TABLE = "DELETE FROM {0} LIMIT {1};";

    /** The Constant denoting query to drop column. */
    private static final String SQL_DROP_COLUMN = "ALTER TABLE {0} DROP COLUMN {1};";

    /** The Constant denoting query to insert record. */
    private static final String SQL_INSERT_RECORD = "INSERT INTO {0} ({1}) VALUES ({2});";

    /** The Constant denoting query to truncate table. */
    private static final String SQL_TRUNCATE_TABLE = "TRUNCATE TABLE {0};";

    /** DB Utility Helper */
    private DbServiceHelper dbHelper;

    /** The DB Service. */
    private volatile DbService dbService;

    /** Scheduled Executor Service */
    private final ScheduledExecutorService executorService;

    /** The wire record options. */
    private DbWireRecordStoreOptions options;

    /** The future handle of the thread pool executor service. */
    private ScheduledFuture<?> tickHandle;

    /** The Wire Helper Service. */
    private volatile WireHelperService wireHelperService;

    /** The Wire Supporter Component. */
    private WireSupport wireSupport;

    /** Constructor */
    public DbWireRecordStore() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * OSGi Service Component callback for activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        s_logger.debug(s_message.activatingStore());
        this.options = new DbWireRecordStoreOptions(properties);
        this.dbHelper = DbServiceHelper.getInstance(this.dbService);
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.scheduleTruncation();
        s_logger.debug(s_message.activatingStoreDone());
    }

    /**
     * Binds the DB service.
     *
     * @param dbService
     *            the new DB service
     */
    public synchronized void bindDbService(final DbService dbService) {
        if (this.dbService == null) {
            this.dbService = dbService;
        }
    }

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Truncates the records in the table
     *
     * @param noOfRecordsToKeep
     *            the no of records to keep in the table
     */
    private void clear(final int noOfRecordsToKeep) {
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(this.options.getTableName());
        Connection conn = null;
        try {
            conn = this.dbHelper.getConnection();
            // check for the table that collects the data
            final String catalog = conn.getCatalog();
            final DatabaseMetaData dbMetaData = conn.getMetaData();
            final ResultSet rsTbls = dbMetaData.getTables(catalog, null, sqlTableName, null);
            if (rsTbls.next()) {
                // table does exist, truncate it
                if (noOfRecordsToKeep == 0) {
                    s_logger.info(s_message.truncatingTable(sqlTableName));
                    this.dbHelper.execute(MessageFormat.format(SQL_TRUNCATE_TABLE, sqlTableName));
                } else {
                    this.dbHelper
                            .execute(MessageFormat.format(SQL_DELETE_RANGE_TABLE, sqlTableName, noOfRecordsToKeep));
                }
            }
        } catch (final SQLException sqlException) {
            s_logger.error(
                    s_message.errorTruncatingTable(sqlTableName) + ThrowableUtil.stackTraceAsString(sqlException));
        } finally {
            if (conn != null) {
                this.dbHelper.close(conn);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected synchronized void deactivate(final ComponentContext componentContext) {
        s_logger.debug(s_message.deactivatingStore());
        if (this.tickHandle != null) {
            this.tickHandle.cancel(true);
        }
        this.executorService.shutdown();
        s_logger.debug(s_message.deactivatingStoreDone());
    }

    /**
     * Insert the provided wire record to the specified table
     *
     * @param tableName
     *            the table name
     * @param wireRecord
     *            the wire record
     * @throws SQLException
     *             the SQL exception
     * @throws KuraRuntimeException
     *             if any of the provided arguments is null
     */
    private void insertDataRecord(final String tableName, final WireRecord wireRecord) throws SQLException {
        checkNull(tableName, s_message.tableNameNonNull());
        checkNull(wireRecord, s_message.wireRecordNonNull());

        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        final StringBuilder sbCols = new StringBuilder();
        final StringBuilder sbVals = new StringBuilder();

        // add the timestamp
        sbCols.append("TIMESTAMP");
        sbVals.append("?");

        final List<WireField> dataFields = wireRecord.getFields();
        for (final WireField dataField : dataFields) {
            final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(dataField.getName());
            sbCols.append(", " + sqlColName);
            sbVals.append(", ?");
        }

        s_logger.info(s_message.storingRecord(sqlTableName));
        final String sqlInsert = MessageFormat.format(SQL_INSERT_RECORD, sqlTableName, sbCols.toString(),
                sbVals.toString());
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = this.dbHelper.getConnection();
            stmt = conn.prepareStatement(sqlInsert);
            stmt.setLong(1, wireRecord.getTimestamp().getTime());
            for (int i = 0; i < dataFields.size(); i++) {
                final WireField dataField = dataFields.get(i);
                final DataType dataType = dataField.getValue().getType();
                final Object value = dataField.getValue();
                switch (dataType) {
                case BOOLEAN:
                    s_logger.info(s_message.storeBoolean(((BooleanValue) value).getValue()));
                    stmt.setBoolean(2 + i, ((BooleanValue) value).getValue());
                    break;
                case BYTE:
                    s_logger.info(s_message.storeByte(((ByteValue) value).getValue()));
                    stmt.setByte(2 + i, ((ByteValue) value).getValue());
                    break;
                case DOUBLE:
                    s_logger.info(s_message.storeDouble(((DoubleValue) value).getValue()));
                    stmt.setDouble(2 + i, ((DoubleValue) value).getValue());
                    break;
                case INTEGER:
                    s_logger.info(s_message.storeInteger(((IntegerValue) value).getValue()));
                    stmt.setInt(2 + i, ((IntegerValue) value).getValue());
                    break;
                case LONG:
                    s_logger.info(s_message.storelong(((LongValue) value).getValue()));
                    stmt.setLong(2 + i, ((LongValue) value).getValue());
                    break;
                case BYTE_ARRAY:
                    s_logger.info(s_message.storeByteArray(Arrays.toString(((ByteArrayValue) value).getValue())));
                    stmt.setBytes(2 + i, ((ByteArrayValue) value).getValue());
                    break;
                case SHORT:
                    s_logger.info(s_message.storeShort(((ShortValue) value).getValue()));
                    stmt.setShort(2 + i, ((ShortValue) value).getValue());
                    break;
                case STRING:
                    s_logger.info(s_message.storeString(((StringValue) value).getValue()));
                    stmt.setString(2 + i, ((StringValue) value).getValue());
                    break;
                default:
                    break;
                }
            }
            stmt.execute();
            conn.commit();
            s_logger.info(s_message.stored());
        } catch (final SQLException e) {
            this.dbHelper.rollback(conn);
            throw e;
        } finally {
            this.dbHelper.close(stmt);
            this.dbHelper.close(conn);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEvelope) {
        checkNull(wireEvelope, s_message.wireEnvelopeNonNull());
        s_logger.debug(s_message.wireEnvelopeReceived() + this.wireSupport);
        // filtering list of wire records based on the provided severity level
        final List<WireRecord> dataRecords = this.wireSupport.filter(wireEvelope.getRecords());
        for (final WireRecord dataRecord : dataRecords) {
            this.store(dataRecord);
        }
        // emit the list of Wire Records to the downstream components
        this.wireSupport.emit(dataRecords);
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

    /**
     * Reconcile columns.
     *
     * @param tableName
     *            the table name
     * @param wireRecord
     *            the data record
     * @throws SQLException
     *             the SQL exception
     * @throws KuraRuntimeException
     *             if any of the provided arguments is null
     */
    private void reconcileColumns(final String tableName, final WireRecord wireRecord) throws SQLException {
        checkNull(tableName, s_message.tableNameNonNull());
        checkNull(wireRecord, s_message.wireRecordNonNull());

        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        Connection conn = null;
        ResultSet rsColumns = null;
        final Map<String, Integer> columns = CollectionUtil.newHashMap();
        try {
            // check for the table that would collect the data of this emitter
            conn = this.dbHelper.getConnection();
            final String catalog = conn.getCatalog();
            final DatabaseMetaData dbMetaData = conn.getMetaData();
            rsColumns = dbMetaData.getColumns(catalog, null, PREFIX + sqlTableName, null);
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
        final List<WireField> dataFields = wireRecord.getFields();
        for (final WireField dataField : dataFields) {
            final String sqlColName = this.dbHelper.sanitizeSqlTableAndColumnName(dataField.getName());
            final Integer sqlColType = columns.get(sqlColName);
            final JdbcType jdbcType = DbDataTypeMapper.getJdbcType(dataField.getValue().getType());
            if (sqlColType == null) {
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
     * Reconcile table.
     *
     * @param tableName
     *            the table name
     * @throws SQLException
     *             the SQL exception
     * @throws KuraRuntimeException
     *             if the provided argument is null
     */
    private void reconcileTable(final String tableName) throws SQLException {
        checkNull(tableName, s_message.tableNameNonNull());
        final String sqlTableName = this.dbHelper.sanitizeSqlTableAndColumnName(tableName);
        final Connection conn = this.dbHelper.getConnection();
        try {
            // check for the table that would collect the data of this emitter
            final String catalog = conn.getCatalog();
            final DatabaseMetaData dbMetaData = conn.getMetaData();
            final ResultSet rsTbls = dbMetaData.getTables(catalog, null, sqlTableName, null);
            if (!rsTbls.next()) {
                // table does not exist, create it
                s_logger.info(s_message.creatingTable(sqlTableName));
                this.dbHelper.execute(MessageFormat.format(SQL_CREATE_TABLE, sqlTableName));
            }
        } finally {
            this.dbHelper.close(conn);
        }
    }

    /**
     * Schedule truncation of tables containing wire records
     */
    private void scheduleTruncation() {
        final int cleanUpRate = this.options.getPeriodicCleanupRate();
        final int noOfRecordsToKeep = this.options.getNoOfRecordsToKeep();
        // Cancel the current refresh view handle
        if (this.tickHandle != null) {
            this.tickHandle.cancel(true);
        }
        // schedule the truncation of collected wire records
        if (cleanUpRate != 0) {
            this.tickHandle = this.executorService.schedule(new Runnable() {

                /** {@inheritDoc} */
                @Override
                public void run() {
                    DbWireRecordStore.this.clear(noOfRecordsToKeep);
                }
            }, cleanUpRate, TimeUnit.SECONDS);
        }
    }

    /**
     * Stores the provided wire record in the database
     *
     * @param wireRecord
     *            the wire record to be stored
     * @throws KuraRuntimeException
     *             if the provided argument is null
     */
    private void store(final WireRecord wireRecord) {
        checkNull(wireRecord, s_message.wireRecordNonNull());
        boolean inserted = false;
        int retryCount = 0;
        final String tableName = this.options.getTableName();
        do {
            try {
                this.insertDataRecord(tableName, wireRecord);
                inserted = true;
            } catch (final SQLException e) {
                s_logger.error(s_message.insertionFailed() + ThrowableUtil.stackTraceAsString(e));
                try {
                    if ((tableName != null) && (!tableName.isEmpty())) {
                        retryCount++;
                        this.reconcileTable(tableName);
                        this.reconcileColumns(tableName, wireRecord);
                    }
                } catch (final SQLException ee) {
                    s_logger.error(s_message.errorStoring() + ee);
                }
            }
        } while (!inserted && (retryCount < 2));
    }

    /**
     * Unbinds the DB service.
     *
     * @param dbService
     *            the DB service
     */
    public synchronized void unbindDbService(final DbService dbService) {
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
    public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated service component properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        s_logger.debug(s_message.updatingStore() + properties);
        this.options = new DbWireRecordStoreOptions(properties);
        this.scheduleTruncation();
        s_logger.debug(s_message.updatingStoreDone());
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }
}
