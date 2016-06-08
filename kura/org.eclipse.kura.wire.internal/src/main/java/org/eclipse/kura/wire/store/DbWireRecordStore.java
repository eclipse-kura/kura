/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.wire.store;

import static org.eclipse.kura.Preconditions.checkNull;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.ByteValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.ShortValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireRecordStore;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.store.DbDataTypeMapper.JdbcType;
import org.eclipse.kura.wire.util.Wires;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * The Class DbWireRecordStore is a wire component which is responsible to store
 * the received Wire Record.
 */
public final class DbWireRecordStore implements WireEmitter, WireReceiver, WireRecordStore, ConfigurableComponent {

	/** The Constant denoting name of the column. */
	private static final String COLUMN_NAME = "COLUMN_NAME";

	/** The constant data type */
	private static final String DATA_TYPE = "DATA_TYPE";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordStore.class);

	/** The Constant denoting query to add column. */
	private static final String SQL_ADD_COLUMN = "ALTER TABLE DR_{0} ADD COLUMN {1} {2};";

	/** The Constant denoting denoting query to create table. */
	private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS DR_{0} (timestamp TIMESTAMP NOT NULL PRIMARY KEY);";

	/** The Constant denoting denoting query to drop column. */
	private static final String SQL_DROP_COLUMN = "ALTER TABLE DR_{0} DROP COLUMN {1};";

	/** The Constant denoting denoting query to insert record. */
	private static final String SQL_INSERT_RECORD = "INSERT INTO DR_{0} ({1}) VALUES ({2});";

	/** The Constant denoting denoting query to truncate table. */
	private static final String SQL_TRUNCATE_TABLE = "TRUNCATE DR_{0};";

	/** The Component Context. */
	private ComponentContext m_ctx;

	/** DB Utility Helper */
	private DbServiceHelper m_dbHelper;

	/** The DB Service. */
	private volatile DbService m_dbService;

	/** Scheduled Executor Service */
	private final ScheduledExecutorService m_executorService;

	/** The wire record options. */
	private DbWireRecordStoreOptions m_options;

	/** The list of table names in which the wire records are stored */
	private final Set<String> m_tableNames;

	/** The future handle of the thread pool executor service. */
	private ScheduledFuture<?> m_tickHandle;

	/** The Wire Supporter Component. */
	private final WireSupport m_wireSupport;

	/** Constructor */
	public DbWireRecordStore() {
		this.m_executorService = Executors.newSingleThreadScheduledExecutor();
		this.m_wireSupport = Wires.newWireSupport(this);
		this.m_tableNames = Sets.newHashSet();
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
		s_logger.info("Activating DB Wire Record Store...");
		this.m_ctx = componentContext;
		this.m_options = new DbWireRecordStoreOptions(properties);
		this.m_dbHelper = DbServiceHelper.getInstance(this.m_dbService);
		this.scheduleTruncation();
		s_logger.info("Activating DB Wire Record Store...Done");
	}

	/** {@inheritDoc} */
	@Override
	public void consumersConnected(final Wire[] wires) {
		this.m_wireSupport.consumersConnected(wires);
	}

	/**
	 * OSGi Service Component callback for deactivation.
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.info("Deactivating DB Wire Record Store...");
		// no need to release the cloud clients as the updated application
		// certificate is already published due the missing dependency
		// we only need to empty our CloudClient list
		this.m_dbService = null;
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		this.m_executorService.shutdown();
		s_logger.info("Deactivating DB Wire Record Store...Done");
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return (String) this.m_ctx.getProperties().get("service.pid");
	}

	/**
	 * Insert data record.
	 *
	 * @param tableName
	 *            the table name
	 * @param wireRecord
	 *            the wire record
	 * @throws SQLException
	 *             the SQL exception
	 * @throws KuraRuntimeException
	 *             if any of the provided argument is null
	 */
	private void insertDataRecord(final String tableName, final WireRecord wireRecord) throws SQLException {
		checkNull(tableName, "Table name cannot be null");
		checkNull(wireRecord, "Wire Record cannot be null");

		final String sqlTableName = this.m_dbHelper.sanitizeSqlTableAndColumnName(tableName);
		final StringBuilder sbCols = new StringBuilder();
		final StringBuilder sbVals = new StringBuilder();

		// add the timestamp
		sbCols.append("TIMESTAMP");
		sbVals.append("?");

		final List<WireField> dataFields = wireRecord.getFields();
		for (final WireField dataField : dataFields) {
			final String sqlColName = this.m_dbHelper.sanitizeSqlTableAndColumnName(dataField.getName());
			sbCols.append(", " + sqlColName);
			sbVals.append(", ?");
		}

		s_logger.info("Storing data record from emitter {} into table {}...", tableName, sqlTableName);
		final String sqlInsert = MessageFormat.format(SQL_INSERT_RECORD, sqlTableName, sbCols.toString(),
				sbVals.toString());
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = this.m_dbHelper.getConnection();
			stmt = conn.prepareStatement(sqlInsert);
			stmt.setTimestamp(1, new Timestamp(wireRecord.getTimestamp().getTime()));
			for (int i = 0; i < dataFields.size(); i++) {
				final WireField dataField = dataFields.get(i);
				final DataType dataType = dataField.getValue().getType();
				final Object value = dataField.getValue();
				switch (dataType) {
				case BOOLEAN:
					s_logger.info("Storing boolean of value {}", ((BooleanValue) value).getValue());
					stmt.setBoolean(2 + i, ((BooleanValue) value).getValue());
					break;
				case BYTE:
					s_logger.info("Storing byte of value {}", ((ByteValue) value).getValue());
					stmt.setByte(2 + i, ((ByteValue) value).getValue());
					break;
				case DOUBLE:
					s_logger.info("Storing double of value {}", ((DoubleValue) value).getValue());
					stmt.setDouble(2 + i, ((DoubleValue) value).getValue());
					break;
				case INTEGER:
					s_logger.info("Storing integer of value {}", ((IntegerValue) value).getValue());
					stmt.setInt(2 + i, ((IntegerValue) value).getValue());
					break;
				case LONG:
					s_logger.info("Storing long of value {}", ((LongValue) value).getValue());
					stmt.setLong(2 + i, ((LongValue) value).getValue());
					break;
				case BYTE_ARRAY:
					s_logger.info("Storing byte array of value {}", ((ByteArrayValue) value).getValue());
					stmt.setBytes(2 + i, ((ByteArrayValue) value).getValue());
					break;
				case SHORT:
					s_logger.info("Storing short of value {}", ((ShortValue) value).getValue());
					stmt.setShort(2 + i, ((ShortValue) value).getValue());
					break;
				case STRING:
					s_logger.info("Storing string of value {}", ((StringValue) value).getValue());
					stmt.setString(2 + i, ((StringValue) value).getValue());
					break;
				default:
					break;
				}
			}
			stmt.execute();
			conn.commit();
			s_logger.info("Stored typed value");
		} catch (final SQLException e) {
			this.m_dbHelper.rollback(conn);
			Throwables.propagate(e);
		} finally {
			this.m_dbHelper.close(stmt);
			this.m_dbHelper.close(conn);
		}
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void onWireReceive(final WireEnvelope wireEvelope) {
		checkNull(wireEvelope, "Wire Envelope cannot be null");
		s_logger.debug("Wire Enveloped received..." + this.m_wireSupport);
		final List<WireRecord> dataRecords = wireEvelope.getRecords();
		final String tableName = wireEvelope.getEmitterName();
		this.m_tableNames.add(tableName);
		for (final WireRecord dataRecord : dataRecords) {
			this.store(tableName, dataRecord);
		}
		// emit the storage event
		this.m_wireSupport.emit(dataRecords);
	}

	/** {@inheritDoc} */
	@Override
	public Object polled(final Wire wire) {
		return this.m_wireSupport.polled(wire);
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		this.m_wireSupport.producersConnected(wires);
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
		checkNull(tableName, "Table name cannot be null");
		checkNull(wireRecord, "Wire Record cannot be null");

		final String sqlTableName = this.m_dbHelper.sanitizeSqlTableAndColumnName(tableName);
		Connection conn = null;
		ResultSet rsColumns = null;
		final Map<String, Integer> columns = Maps.newHashMap();
		try {
			// check for the table that would collect the data of this emitter
			conn = this.m_dbHelper.getConnection();
			final String catalog = conn.getCatalog();
			final DatabaseMetaData dbMetaData = conn.getMetaData();
			rsColumns = dbMetaData.getColumns(catalog, null, "DR_" + sqlTableName, null);
			// map the columns
			while (rsColumns.next()) {
				final String colName = rsColumns.getString(COLUMN_NAME);
				final int colType = rsColumns.getInt(DATA_TYPE);
				columns.put(colName, colType);
			}
		} finally {
			this.m_dbHelper.close(rsColumns);
			this.m_dbHelper.close(conn);
		}
		// reconcile columns
		final List<WireField> dataFields = wireRecord.getFields();
		for (final WireField dataField : dataFields) {
			final String sqlColName = this.m_dbHelper.sanitizeSqlTableAndColumnName(dataField.getName());
			final Integer sqlColType = columns.get(sqlColName);
			final JdbcType jdbcType = DbDataTypeMapper.getJdbcType(dataField.getValue().getType());
			if (sqlColType == null) {
				// add column
				this.m_dbHelper.execute(
						MessageFormat.format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString()));
			} else if (sqlColType != jdbcType.getType()) {
				// drop old column and add new one
				this.m_dbHelper.execute(MessageFormat.format(SQL_DROP_COLUMN, sqlTableName, sqlColName));
				this.m_dbHelper.execute(
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
		checkNull(tableName, "Table name cannot be null");
		final String sqlTableName = this.m_dbHelper.sanitizeSqlTableAndColumnName(tableName);
		final Connection conn = this.m_dbHelper.getConnection();
		try {
			// check for the table that would collect the data of this emitter
			final String catalog = conn.getCatalog();
			final DatabaseMetaData dbMetaData = conn.getMetaData();
			final ResultSet rsTbls = dbMetaData.getTables(catalog, null, sqlTableName, null);
			if (!rsTbls.next()) {
				// table does not exist, create it
				s_logger.info("Creating table DR_{}...", sqlTableName);
				this.m_dbHelper.execute(MessageFormat.format(SQL_CREATE_TABLE, sqlTableName));
			}
		} finally {
			this.m_dbHelper.close(conn);
		}
	}

	/**
	 * Schedule truncation of tables containing wire records
	 */
	private void scheduleTruncation() {
		final int cleanUpRate = this.m_options.getPeriodicCleanupRate();
		// Cancel the current refresh view handle
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		// schedule the truncation of collected wire records
		if (cleanUpRate != 0) {
			this.m_tickHandle = this.m_executorService.schedule(new Runnable() {
				/** {@inheritDoc} */
				@Override
				public void run() {
					for (final String tableName : m_tableNames) {
						clear(tableName);
					}
				}
			}, cleanUpRate, TimeUnit.SECONDS);
		}
	}

	/**
	 * Set the DB service.
	 *
	 * @param dbService
	 *            the new DB service
	 */
	public synchronized void setDbService(final DbService dbService) {
		if (this.m_dbService == null) {
			this.m_dbService = dbService;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void store(final String tableName, final WireRecord wireRecord) {
		checkNull(tableName, "Table name cannot be null");
		checkNull(wireRecord, "Wire Record cannot be null");

		boolean inserted = false;
		int retryCount = 0;
		do {
			try {
				this.insertDataRecord(tableName, wireRecord);
				inserted = true;
			} catch (final SQLException e) {
				try {
					this.reconcileTable(tableName);
					this.reconcileColumns(tableName, wireRecord);
					retryCount++;
				} catch (final SQLException ee) {
					Throwables.propagate(ee);
				}
			}
		} while (!inserted && (retryCount < 2));
	}

	/** {@inheritDoc} */
	@Override
	public void clear(final String tableName) throws KuraRuntimeException {
		checkNull(tableName, "Table name cannot be null");
		final String sqlTableName = this.m_dbHelper.sanitizeSqlTableAndColumnName(tableName);
		Connection conn = null;
		try {
			conn = this.m_dbHelper.getConnection();
			// check for the table that collects the data
			final String catalog = conn.getCatalog();
			final DatabaseMetaData dbMetaData = conn.getMetaData();
			final ResultSet rsTbls = dbMetaData.getTables(catalog, null, sqlTableName, null);
			if (rsTbls.next()) {
				// table does exist, truncate it
				s_logger.info("Truncating table DR_{}...", sqlTableName);
				this.m_dbHelper.execute(MessageFormat.format(SQL_TRUNCATE_TABLE, sqlTableName));
			}
		} catch (final SQLException sqlException) {
			s_logger.error("Error in truncating the table " + sqlTableName + "..."
					+ Throwables.getStackTraceAsString(sqlException));
		} finally {
			if (conn != null) {
				this.m_dbHelper.close(conn);
			}
		}
	}

	/**
	 * Unset the DB service.
	 *
	 * @param dbService
	 *            the DB service
	 */
	public synchronized void unsetDbService(final DbService dbService) {
		if (this.m_dbService == dbService) {
			this.m_dbService = null;
		}
	}

	/**
	 * OSGi Service Component callback for updating.
	 *
	 * @param properties
	 *            the updated service component properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.info("Updating DB Wire Record Store with..." + properties);
		this.m_options = new DbWireRecordStoreOptions(properties);
		this.scheduleTruncation();
		s_logger.info("Updating DB Wire Record Store...Done");
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}
}
