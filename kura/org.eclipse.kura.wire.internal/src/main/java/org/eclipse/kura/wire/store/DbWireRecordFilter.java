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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DbWireRecordFilter is responsible for representing a wire component
 * which is mainly used to filter records as received by wire record
 */
public final class DbWireRecordFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

	/** The Constant s_logger. */
	private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordFilter.class);

	// FIXME: Remove refresh rate parameter and add a new DataEventTimer service
	// FIXME: Add support for Cloudlet

	/** The component context. */
	private ComponentContext m_ctx;

	/** The DB Service dependency. */
	private volatile DbService m_dbService;

	/** The DB Filter Options. */
	private DbWireRecordFilterOptions m_options;

	/** The Wire Supporter component. */
	private WireSupport m_wireSupport;

	/**
	 * OSGi service component callback for deactivation
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.info("Activating DB Wire Record Filter...");
		this.m_ctx = componentContext;
		this.m_wireSupport = WireSupport.of(this);

		this.m_options = new DbWireRecordFilterOptions(properties);
		s_logger.info("Activating DB Wire Record Filter...Done");
	}

	/**
	 * Closes the connection
	 *
	 * @param connection
	 *            the connection instance to close
	 */
	private void close(final Connection connection) {
		this.m_dbService.close(connection);
	}

	/**
	 * Closes the connection
	 *
	 * @param resultSet
	 *            the result set
	 */
	private void close(final ResultSet... resultSet) {
		this.m_dbService.close(resultSet);
	}

	/**
	 * Closes the DB connection
	 *
	 * @param statements
	 *            the the statements
	 */
	private void close(final Statement... statements) {
		this.m_dbService.close(statements);
	}

	/** {@inheritDoc} */
	@Override
	public void consumersConnected(final Wire[] wires) {
		this.m_wireSupport.consumersConnected(wires);
	}

	/**
	 * OSGi service component callback for deactivation
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.info("deactivate...");
		// no need to release the cloud clients as the updated app
		// certificate is already published due the missing dependency
		// we only need to empty our CloudClient list
		this.m_dbService = null;
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

	/** {@inheritDoc} */
	@Override
	public synchronized void onWireReceive(final WireEnvelope wireEvelope) {
		s_logger.error("wireEnvelope received!");
		// FIXME: add implementation for onWireReceive
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
	 * Refresh data view.
	 *
	 * @return the list
	 * @throws SQLException
	 *             the SQL exception
	 */
	private List<WireRecord> refreshDataView() throws SQLException {
		final Date now = new Date();
		final List<WireRecord> dataRecords = new ArrayList<WireRecord>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		final String sqlView = this.m_options.getSqlView();
		try {

			conn = this.getConnection();
			stmt = conn.createStatement();
			rset = stmt.executeQuery(sqlView);
			if (rset != null) {
				while (rset.next()) {

					final List<WireField> dataFields = new ArrayList<WireField>();
					final ResultSetMetaData rmet = rset.getMetaData();
					for (int i = 1; i <= rmet.getColumnCount(); i++) {

						String fieldName = rmet.getColumnLabel(i);
						if (fieldName == null) {
							fieldName = rmet.getColumnName(i);
						}

						WireField dataField = null;

						final int jdbcType = rmet.getColumnType(i);
						final DataType dataType = DbDataTypeMapper.getDataType(jdbcType);
						switch (dataType) {
						case BOOLEAN:
							final boolean boolValue = rset.getBoolean(i);
							dataField = new WireField(fieldName, new BooleanValue(boolValue));
							break;
						case BYTE:
							final byte byteValue = rset.getByte(i);
							dataField = new WireField(fieldName, new ByteValue(byteValue));
							break;
						case DOUBLE:
							final double doubleValue = rset.getDouble(i);
							dataField = new WireField(fieldName, new DoubleValue(doubleValue));
							break;
						case INTEGER:
							final int intValue = rset.getInt(i);
							dataField = new WireField(fieldName, new IntegerValue(intValue));
							break;
						case LONG:
							final long longValue = rset.getLong(i);
							dataField = new WireField(fieldName, new LongValue(longValue));
							break;
						case BYTE_ARRAY:
							final byte[] bytesValue = rset.getBytes(i);
							dataField = new WireField(fieldName, new ByteArrayValue(bytesValue));
							break;
						case SHORT:
							final short shortValue = rset.getShort(i);
							dataField = new WireField(fieldName, new ShortValue(shortValue));
							break;
						case STRING:
							final String stringValue = rset.getString(i);
							dataField = new WireField(fieldName, new StringValue(stringValue));
							break;
						}
						dataFields.add(dataField);
					}
					dataRecords.add(new WireRecord(now, dataFields));
				}
			}
		} finally {
			this.close(rset);
			this.close(stmt);
			this.close(conn);
		}

		return dataRecords;
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
	 * Unset db service.
	 *
	 * @param dataService
	 *            the data service
	 */
	public synchronized void unsetDbService(final DbService dataService) {
		this.m_dbService = null;
	}

	/**
	 * OSGi service component callback for updating
	 *
	 * @param properties
	 *            the properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.info("Updating DBWireRecordFilter..." + properties);
		this.m_options = new DbWireRecordFilterOptions(properties);
		s_logger.info("Updating DBWireRecordFilter...Done " + properties);
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}
}
