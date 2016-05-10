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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
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
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.store.DbDataTypeMapper.JdbcType;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.base.Throwables;

/**
 * The Class DbWireRecordStore is a wire component which is responsible to store
 * the received Wire Record
 */
@Beta
public final class DbWireRecordStore implements WireEmitter, WireReceiver, ConfigurableComponent {

	/** The Constant denoting name of the column. */
	private static final String COLUMN_NAME = "COLUMN_NAME";

	/**
	 * FIXME: Extract the DataRecordStore service interface in the API and
	 * implement a store and query method FIXME: Add primary key and index on
	 * timestamp FIXME: Verify timestamp resolution to milliseconds FIXME: Add
	 * support for period cleanup of the data records collected! FIXME: Add
	 * support for different table type - persisted vs in-memory. FIXME: SQL
	 * escaping of the names of the tables and columns. Be careful on the
	 * capitalization; it is lossy? FIXME: Add support for Cloudlet
	 */

	/** The constant data type */
	private static final String DATA_TYPE = "DATA_TYPE";

	/** The Logger. */
	private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordStore.class);

	/** The Constant denoting query to add column. */
	private static final String SQL_ADD_COLUMN = "ALTER TABLE DR_{0} ADD COLUMN {1} {2};";

	/** The Constant denoting denoting query to create table. */
	private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS DR_{0} (timestamp TIMESTAMP);";

	/** The Constant denoting denoting query to drop column. */
	private static final String SQL_DROP_COLUMN = "ALTER TABLE DR_{0} DROP COLUMN {1};";

	/** The Constant denoting denoting query to insert record. */
	private static final String SQL_INSERT_RECORD = "INSERT INTO DR_{0} ({1}) VALUES ({2});";

	/** The Component Context. */
	private ComponentContext m_ctx;

	/** The DB Service. */
	private volatile DbService m_dbService;

	/** The wire record options. */
	@SuppressWarnings("unused")
	private DbWireRecordStoreOptions m_options;

	/** The Wire Supporter Component. */
	private WireSupport m_wireSupport;

	/**
	 * OSGi Service Component callback for activation
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.info("Activating DB Wire Record Store...");

		// save the bundle context and the properties
		this.m_ctx = componentContext;
		this.m_wireSupport = WireSupport.of(this);

		// Update properties
		this.m_options = new DbWireRecordStoreOptions(properties);

		// FIXME: remove test table create and insert
		try {

			final List<WireField> fields = new ArrayList<WireField>();
			fields.add(new WireField("c_byte", new ByteValue((byte) 1)));
			fields.add(new WireField("c_short", new ShortValue((short) 1)));
			fields.add(new WireField("c_int", new IntegerValue(1)));
			fields.add(new WireField("c_long", new LongValue(1)));
			fields.add(new WireField("c_double", new DoubleValue(1)));
			fields.add(new WireField("c_bool", new BooleanValue(true)));
			fields.add(new WireField("c_raw", new ByteArrayValue(new byte[] {})));
			fields.add(new WireField("c_string", new StringValue("a")));

			final WireRecord dataRecord = new WireRecord(new Date(), fields);

			final String tableName = "testTable";
			this.reconcileTable(tableName);

			this.reconcileColumns(tableName, dataRecord);
			this.insertDataRecord(tableName, dataRecord);
		} catch (final SQLException e) {
			s_logger.error(Throwables.getStackTraceAsString(e));
		}
		s_logger.info("Activating DB Wire Record Store...Done");
	}

	/**
	 * Closes the connection
	 *
	 * @param conn
	 *            the connection instance
	 */
	private void close(final Connection conn) {
		this.m_dbService.close(conn);
	}

	/**
	 * Closes the connection
	 *
	 * @param rss
	 *            the result set
	 */
	private void close(final ResultSet... rss) {
		this.m_dbService.close(rss);
	}

	/**
	 * Closes the connection
	 *
	 * @param stmts
	 *            the statements
	 */
	private void close(final Statement... stmts) {
		this.m_dbService.close(stmts);
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

		// no need to release the cloud clients as the updated app
		// certificate is already published due the missing dependency
		// we only need to empty our CloudClient list
		this.m_dbService = null;
		s_logger.info("Deactivating DB Wire Record Store...Done");
	}

	/**
	 * Escapes sql string
	 *
	 * @param string
	 *            the string to be filtered
	 * @return the escaped string
	 */
	private String escapeSql(final String string) {
		// The escaping java mechanism is used as it conforms to the generic
		// standard that Java also follows
		return StringEscapeUtils.escapeJava(string);
	}

	/**
	 * Executes the provided sql query.
	 *
	 * @param sql
	 *            the sql query to execute
	 * @param params
	 *            the params extra parameters needed for the query
	 * @throws SQLException
	 *             the SQL exception
	 */
	private synchronized void execute(final String sql, final Integer... params) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = this.getConnection();
			stmt = conn.prepareStatement(sql);
			for (int i = 0; i < params.length; i++) {
				stmt.setInt(1 + i, params[i]);
			}
			stmt.execute();
			conn.commit();
		} catch (final SQLException e) {
			this.rollback(conn);
			throw e;
		} finally {
			this.close(stmt);
			this.close(conn);
		}
	}

	/**
	 * Gets the connection.
	 *
	 * @return the connection
	 * @throws SQLException
	 *             the SQL exception
	 */
	private Connection getConnection() throws SQLException {
		return this.m_dbService.getConnection();
	}

	/**
	 * Gets the db service.
	 *
	 * @return the db service
	 */
	public DbService getDbService() {
		return this.m_dbService;
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
	 * @param dataRecord
	 *            the data record
	 * @throws SQLException
	 *             the SQL exception
	 */
	private void insertDataRecord(final String tableName, final WireRecord dataRecord) throws SQLException {
		final String sqlTableName = this.escapeSql(tableName);
		final StringBuilder sbCols = new StringBuilder();
		final StringBuilder sbVals = new StringBuilder();

		// add the timestamp
		sbCols.append("TIMESTAMP");
		sbVals.append("?");

		final List<WireField> dataFields = dataRecord.getFields();
		for (final WireField dataField : dataFields) {
			final String sqlColName = this.escapeSql(dataField.getName());
			sbCols.append(", " + sqlColName);
			sbVals.append(", ?");
		}

		s_logger.info("Storing data record from emitter {} into table {}...", tableName, sqlTableName);
		final String sqlInsert = MessageFormat.format(SQL_INSERT_RECORD, sqlTableName, sbCols.toString(),
				sbVals.toString());
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = this.getConnection();
			stmt = conn.prepareStatement(sqlInsert);
			stmt.setTimestamp(1, new Timestamp(dataRecord.getTimestamp().getTime()));
			for (int i = 0; i < dataFields.size(); i++) {

				final WireField dataField = dataFields.get(i);
				final DataType dataType = dataField.getValue().getType();
				switch (dataType) {
				case BOOLEAN:
					stmt.setBoolean(2 + i, ((BooleanValue) dataField.getValue()).getValue());
					break;
				case BYTE:
					stmt.setByte(2 + i, ((ByteValue) dataField.getValue()).getValue());
					break;
				case DOUBLE:
					s_logger.info("Storing double of value {}", ((DoubleValue) dataField.getValue()).getValue());
					stmt.setDouble(2 + i, ((DoubleValue) dataField.getValue()).getValue());
					break;
				case INTEGER:
					stmt.setInt(2 + i, ((IntegerValue) dataField.getValue()).getValue());
					break;
				case LONG:
					stmt.setLong(2 + i, ((LongValue) dataField.getValue()).getValue());
					break;
				case BYTE_ARRAY:
					stmt.setBytes(2 + i, ((ByteArrayValue) dataField.getValue()).getValue());
					break;
				case SHORT:
					stmt.setShort(2 + i, ((ShortValue) dataField.getValue()).getValue());
					break;
				case STRING:
					stmt.setString(2 + i, ((StringValue) dataField.getValue()).getValue());
					break;
				default:
					break;
				}
			}
			stmt.execute();
			conn.commit();
			s_logger.info("Stored double of value");
		} catch (final SQLException e) {
			this.rollback(conn);
			throw e;
		} finally {
			this.close(stmt);
			this.close(conn);
		}
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void onWireReceive(final WireEnvelope wireEvelope) {
		final List<WireRecord> dataRecords = wireEvelope.getRecords();
		for (final WireRecord dataRecord : dataRecords) {
			this.storeDataRecord(wireEvelope.getEmitterName(), dataRecord);
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
	 * @param dataRecord
	 *            the data record
	 * @throws SQLException
	 *             the SQL exception
	 */
	private void reconcileColumns(final String tableName, final WireRecord dataRecord) throws SQLException {
		final String sqlTableName = this.escapeSql(tableName);
		Connection conn = null;
		ResultSet rsColumns = null;
		final Map<String, Integer> columns = new HashMap<String, Integer>();
		try {
			// check for the table that would collect the data of this emitter
			conn = this.getConnection();
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
			this.close(rsColumns);
			this.close(conn);
		}

		// reconcile columns
		final List<WireField> dataFields = dataRecord.getFields();
		for (final WireField dataField : dataFields) {

			final String sqlColName = this.escapeSql(dataField.getName());
			final Integer sqlColType = columns.get(sqlColName);
			final JdbcType jdbcType = DbDataTypeMapper.getJdbcType(dataField.getValue().getType());
			if (sqlColType == null) {

				// add column
				this.execute(MessageFormat.format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString()));
			} else if (sqlColType != jdbcType.getType()) {

				// drop old column and add new one
				this.execute(MessageFormat.format(SQL_DROP_COLUMN, sqlTableName, sqlColName));
				this.execute(MessageFormat.format(SQL_ADD_COLUMN, sqlTableName, sqlColName, jdbcType.getTypeString()));
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
	 */
	private void reconcileTable(final String tableName) throws SQLException {
		final String sqlTableName = this.escapeSql(tableName);
		final Connection conn = this.getConnection();
		try {

			// check for the table that would collect the data of this emitter
			final String catalog = conn.getCatalog();
			final DatabaseMetaData dbMetaData = conn.getMetaData();
			final ResultSet rsTbls = dbMetaData.getTables(catalog, null, sqlTableName, null);
			if (!rsTbls.next()) {
				// table does not exist, create it
				s_logger.info("Creating table DR_{}...", sqlTableName);
				this.execute(MessageFormat.format(SQL_CREATE_TABLE, sqlTableName));
			}
		} finally {
			this.close(conn);
		}
	}

	/**
	 * Rollback.
	 *
	 * @param conn
	 *            the conn
	 */
	private void rollback(final Connection conn) {
		this.m_dbService.rollback(conn);
	}

	/**
	 * Sets the db service.
	 *
	 * @param dbService
	 *            the new db service
	 */
	public synchronized void setDbService(final DbService dbService) {
		this.m_dbService = dbService;
	}

	/**
	 * Store data record.
	 *
	 * @param emitterId
	 *            the emitter id
	 * @param dataRecord
	 *            the data record
	 */
	private void storeDataRecord(final String emitterId, final WireRecord dataRecord) {
		boolean inserted = false;
		int retryCount = 0;
		do {
			try {
				// store the record
				this.insertDataRecord(emitterId, dataRecord);
				inserted = true;
			} catch (final SQLException e) {
				try {
					this.reconcileTable(emitterId);
					this.reconcileColumns(emitterId, dataRecord);
					retryCount++;
				} catch (final SQLException ee) {
					s_logger.error("Cannot reconcile the database", Throwables.getStackTraceAsString(ee));
				}
			}
		} while (!inserted && (retryCount < 2));
	}

	/**
	 * Unset db service.
	 *
	 * @param dataService
	 *            the data service
	 */
	public synchronized void unsetDbService(final DbService dataService) {
		this.m_dbService = null;
	}

	/**
	 * Updated.
	 *
	 * @param properties
	 *            the properties
	 */
	public void updated(final Map<String, Object> properties) {
		s_logger.info("updated...: " + properties);

		// Update properties
		this.m_options = new DbWireRecordStoreOptions(properties);
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}
}
