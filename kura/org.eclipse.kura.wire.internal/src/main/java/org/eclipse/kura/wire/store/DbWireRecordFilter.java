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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.util.TypedValues;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireRecordFilter;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.util.Wires;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * The Class DbWireRecordFilter is responsible for representing a wire component
 * which is mainly used to filter records as received from the wire record
 */
public final class DbWireRecordFilter implements WireEmitter, WireReceiver, WireRecordFilter, ConfigurableComponent {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordFilter.class);

	/** Container to store cache values of SQL view wire records */
	private final Cache<Long, List<WireRecord>> m_cache;

	/** Cache last updated timestamp */
	private long m_cacheLastUpdated;

	/** The component context. */
	private ComponentContext m_ctx;

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

	/** The Wire Supporter component. */
	private final WireSupport m_wireSupport;

	/** Constructor */
	public DbWireRecordFilter() {
		this.m_wireSupport = Wires.newWireSupport(this);
		this.m_executorService = Executors.newSingleThreadScheduledExecutor();
		this.m_cache = CacheBuilder.newBuilder().maximumSize(10000).expireAfterWrite(30, TimeUnit.MINUTES).build();
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
		s_logger.info("Activating DB Wire Record Filter...");
		this.m_ctx = componentContext;
		this.m_options = new DbWireRecordFilterOptions(properties);
		this.m_dbHelper = DbServiceHelper.getInstance(this.m_dbService);
		this.scheduleRefresh();
		s_logger.info("Activating DB Wire Record Filter...Done");
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
		s_logger.info("Dectivating DB Wire Record Filter...");
		// no need to release the cloud clients as the updated app
		// certificate is already published due the missing dependency
		// we only need to empty our CloudClient list
		this.m_dbService = null;
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		this.m_executorService.shutdown();
		s_logger.info("Activating DB Wire Record Filter...Done");
	}

	/** {@inheritDoc} */
	@Override
	public List<WireRecord> filter() {
		s_logger.debug("Wire record filtering started...");
		try {
			return this.refreshSQLView();
		} catch (final SQLException e) {
			s_logger.error("Error while filtering wire records.." + Throwables.getStackTraceAsString(e));
		}
		return ImmutableList.of();
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return (String) this.m_ctx.getProperties().get("service.pid");
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {
		checkNull(wireEnvelope, "Wire envelope cannot be null");
		s_logger.debug("Wire Enveloped received..." + wireEnvelope);
		// No need to look into the wire envelope as the filtered records are
		// prepared from the SQL view as configured by the user
		final Date currentTime = new Date(System.currentTimeMillis());
		// if current time is after the time for which the cache is last
		// updated due to scheduled refresh view operation, then this is
		// going to be new update operation. Hence, update the cache.
		if (currentTime.after(new Date(this.m_cacheLastUpdated))) {
			this.m_cacheLastUpdated = currentTime.getTime();
			this.m_cache.put(this.m_cacheLastUpdated, this.filter());
		}
		// simply read the last updated cache value. There is no need to update
		// the cache.
		this.m_wireSupport.emit(this.m_cache.getIfPresent(this.m_cacheLastUpdated));
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
	 * Refresh the SQL view
	 */
	private List<WireRecord> refreshSQLView() throws SQLException {
		final Date now = new Date();
		final List<WireRecord> dataRecords = Lists.newArrayList();
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
					final List<WireField> dataFields = Lists.newArrayList();
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
							dataField = Wires.newWireField(fieldName, TypedValues.newBooleanValue(boolValue));
							break;
						case BYTE:
							final byte byteValue = rset.getByte(i);
							dataField = Wires.newWireField(fieldName, TypedValues.newByteValue(byteValue));
							break;
						case DOUBLE:
							final double doubleValue = rset.getDouble(i);
							dataField = Wires.newWireField(fieldName, TypedValues.newDoubleValue(doubleValue));
							break;
						case INTEGER:
							final int intValue = rset.getInt(i);
							dataField = Wires.newWireField(fieldName, TypedValues.newIntegerValue(intValue));
							break;
						case LONG:
							final long longValue = rset.getLong(i);
							dataField = Wires.newWireField(fieldName, TypedValues.newLongValue(longValue));
							break;
						case BYTE_ARRAY:
							final byte[] bytesValue = rset.getBytes(i);
							dataField = Wires.newWireField(fieldName, TypedValues.newByteArrayValue(bytesValue));
							break;
						case SHORT:
							final short shortValue = rset.getShort(i);
							dataField = Wires.newWireField(fieldName, TypedValues.newShortValue(shortValue));
							break;
						case STRING:
							final String stringValue = rset.getString(i);
							dataField = Wires.newWireField(fieldName, TypedValues.newStringValue(stringValue));
							break;
						}
						dataFields.add(dataField);
					}
					dataRecords.add(Wires.newWireRecord(new Timestamp(now.getTime()), dataFields));
				}
			}
		} catch (final Exception e) {
			Throwables.propagateIfInstanceOf(e, SQLException.class);
			s_logger.error(Throwables.getStackTraceAsString(e));
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
		// Cancel the current refresh view handle
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		// schedule the new refresh view
		this.m_tickHandle = this.m_executorService.schedule(new Runnable() {
			/** {@inheritDoc} */
			@Override
			public void run() {
				m_cacheLastUpdated = System.currentTimeMillis();
				m_cache.put(m_cacheLastUpdated, filter());
			}
		}, this.m_options.getRefreshRate(), TimeUnit.SECONDS);
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

	/**
	 * Unset DB service.
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
	 * OSGi service component callback for updating
	 *
	 * @param properties
	 *            the updated properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.info("Updating DBWireRecordFilter..." + properties);
		this.m_options = new DbWireRecordFilterOptions(properties);
		this.scheduleRefresh();
		s_logger.info("Updating DBWireRecordFilter...Done ");
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}
}
