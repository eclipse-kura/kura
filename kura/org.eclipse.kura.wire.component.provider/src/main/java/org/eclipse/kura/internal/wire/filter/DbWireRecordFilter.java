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
package org.eclipse.kura.internal.wire.filter;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.wire.SeverityLevel.INFO;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.internal.wire.common.DbServiceHelper;
import org.eclipse.kura.internal.wire.store.DbDataTypeMapper;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
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
 * The Class DbWireRecordFilter is responsible for representing a wire component
 * which is mainly used to filter records as received from the wire record
 */
public final class DbWireRecordFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordFilter.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** Cache container to store values of SQL view wire records */
	private final WireRecordCache m_cache;

	/** DB Utility Helper */
	private DbServiceHelper m_dbHelper;

	/** The DB Service dependency. */
	private volatile DbService m_dbService;

	/** Scheduled Executor Service */
	private final ScheduledExecutorService m_executorService;

	/** The DB Filter Options. */
	private DbWireRecordFilterOptions m_options;

	/** The future handle of the thread pool executor service. */
	private ScheduledFuture<?> m_tickHandle;

	/** The Wire Helper Service. */
	private volatile WireHelperService m_wireHelperService;

	/** The Wire Supporter component. */
	private WireSupport m_wireSupport;

	/** Constructor */
	public DbWireRecordFilter() {
		this.m_executorService = Executors.newSingleThreadScheduledExecutor();
		this.m_cache = new WireRecordCache(this);
	}

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
		s_logger.debug(s_message.activatingFilter());
		this.m_options = new DbWireRecordFilterOptions(properties);
		this.m_dbHelper = DbServiceHelper.getInstance(this.m_dbService);
		this.m_wireSupport = this.m_wireHelperService.newWireSupport(this);
		this.scheduleRefresh();
		s_logger.debug(s_message.activatingFilterDone());
	}

	/**
	 * Binds the DB service.
	 *
	 * @param dbService
	 *            the new DB service
	 */
	public synchronized void bindDbService(final DbService dbService) {
		if (this.m_dbService == null) {
			this.m_dbService = dbService;
		}
	}

	/**
	 * Binds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == null) {
			this.m_wireHelperService = wireHelperService;
		}
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
		s_logger.debug(s_message.deactivatingFilter());
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		this.m_executorService.shutdown();
		s_logger.debug(s_message.deactivatingFilterDone());
	}

	/**
	 * Filters the database records based on the provided query
	 *
	 * @return the filtered records
	 */
	synchronized List<WireRecord> filter() {
		s_logger.debug(s_message.filteringStarted());
		try {
			return this.refreshSQLView();
		} catch (final SQLException e) {
			s_logger.error(s_message.errorFiltering() + ThrowableUtil.stackTraceAsString(e));
		}
		return Collections.emptyList();
	}

	/**
	 * Trigger emitting data as soon as new wire envelope is received. This
	 * retrieves the last updated value from the cache if the time difference
	 * between the current time and the last cache updated time is less than the
	 * configured cache interval. If it is more than the aforementioned time
	 * difference, then retrieve the value from the cache using current time as
	 * a key. This will actually result in a cache miss. Every cache miss will
	 * internally be handled by {@link WireRecordCache} in such a way that
	 * whenever a cache miss occurs it will load the value from the DB.
	 */
	@Override
	public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {
		checkNull(wireEnvelope, s_message.wireEnvelopeNonNull());
		s_logger.debug(s_message.wireEnvelopeReceived() + wireEnvelope);
		this.m_wireSupport.emit(this.m_cache.get(this.m_cache.getLastRefreshedTime().getTimeInMillis()));
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
	 * Refreshes the SQL view
	 */
	private List<WireRecord> refreshSQLView() throws SQLException {
		final Date now = new Date();
		final List<WireRecord> dataRecords = CollectionUtil.newArrayList();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		final String sqlView = this.m_options.getSqlView();
		try {
			conn = this.m_dbHelper.getConnection();
			stmt = conn.createStatement();
			rset = stmt.executeQuery(sqlView);
			if (rset != null) {
				while (rset.next()) {
					final List<WireField> dataFields = CollectionUtil.newArrayList();
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
							s_logger.info(s_message.refreshBoolean(boolValue));
							dataField = new WireField(fieldName, TypedValues.newBooleanValue(boolValue), INFO);
							break;
						case BYTE:
							final byte byteValue = rset.getByte(i);
							s_logger.info(s_message.refreshByte(byteValue));
							dataField = new WireField(fieldName, TypedValues.newByteValue(byteValue), INFO);
							break;
						case DOUBLE:
							final double doubleValue = rset.getDouble(i);
							s_logger.info(s_message.refreshDouble(doubleValue));
							dataField = new WireField(fieldName, TypedValues.newDoubleValue(doubleValue), INFO);
							break;
						case INTEGER:
							final int intValue = rset.getInt(i);
							s_logger.info(s_message.refreshInteger(intValue));
							dataField = new WireField(fieldName, TypedValues.newIntegerValue(intValue), INFO);
							break;
						case LONG:
							final long longValue = rset.getLong(i);
							s_logger.info(s_message.refreshLong(longValue));
							dataField = new WireField(fieldName, TypedValues.newLongValue(longValue), INFO);
							break;
						case BYTE_ARRAY:
							final byte[] bytesValue = rset.getBytes(i);
							s_logger.info(s_message.refreshByteArray(Arrays.toString(bytesValue)));
							dataField = new WireField(fieldName, TypedValues.newByteArrayValue(bytesValue), INFO);
							break;
						case SHORT:
							final short shortValue = rset.getShort(i);
							s_logger.info(s_message.refreshShort(shortValue));
							dataField = new WireField(fieldName, TypedValues.newShortValue(shortValue), INFO);
							break;
						case STRING:
							final String stringValue = rset.getString(i);
							s_logger.info(s_message.refreshString(stringValue));
							dataField = new WireField(fieldName, TypedValues.newStringValue(stringValue), INFO);
							break;
						default:
							break;
						}
						dataFields.add(dataField);
					}
					dataRecords.add(new WireRecord(new Timestamp(now.getTime()), dataFields));
				}
			}
			s_logger.info(s_message.refreshed());
		} catch (final SQLException e) {
			throw e;
		} finally {
			this.m_dbHelper.close(rset);
			this.m_dbHelper.close(stmt);
			this.m_dbHelper.close(conn);
		}
		return dataRecords;
	}

	/**
	 * Schedule refresh of SQL view operation
	 */
	private void scheduleRefresh() {
		final int refreshRate = this.m_options.getRefreshRate();
		this.m_cache.setRefreshDuration(refreshRate);
		// Cancel the current refresh view handle
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		// schedule the new refresh view
		if (refreshRate != 0) {
			this.m_tickHandle = this.m_executorService.schedule(new Runnable() {
				/** {@inheritDoc} */
				@Override
				public void run() {
					m_cache.put(System.currentTimeMillis(), filter());
				}
			}, refreshRate, TimeUnit.SECONDS);
		}
	}

	/**
	 * Unbinds DB service.
	 *
	 * @param dbService
	 *            the DB service
	 */
	public synchronized void unbindDbService(final DbService dbService) {
		if (this.m_dbService == dbService) {
			this.m_dbService = null;
		}
	}

	/**
	 * Unbinds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == wireHelperService) {
			this.m_wireHelperService = null;
		}
	}

	/**
	 * OSGi service component callback for updating
	 *
	 * @param properties
	 *            the updated properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updatingFilter() + properties);
		this.m_options = new DbWireRecordFilterOptions(properties);
		this.scheduleRefresh();
		s_logger.debug(s_message.updatingFilterDone());
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}
}
