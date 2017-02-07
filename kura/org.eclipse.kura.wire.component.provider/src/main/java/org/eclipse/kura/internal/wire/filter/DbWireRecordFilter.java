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
package org.eclipse.kura.internal.wire.filter;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.isNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.internal.wire.common.DbServiceHelper;
import org.eclipse.kura.internal.wire.store.DbDataTypeMapper;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
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
 * The Class DbWireRecordFilter is responsible for representing a wire component
 * which is focused on performing an user defined SQL query in a database table and emitting the result as a Wire
 * Envelope.
 */
public final class DbWireRecordFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(DbWireRecordFilter.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private List<WireRecord> lastRecords;

    private DbServiceHelper dbHelper;

    private volatile DbService dbService;

    private DbWireRecordFilterOptions options;

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    private Calendar lastRefreshedTime;

    private int cacheExpirationInterval;

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
     * Unbinds DB service.
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
     * OSGi service component callback for deactivation
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug(message.activatingFilter());
        this.options = new DbWireRecordFilterOptions(properties);
        this.dbHelper = DbServiceHelper.getInstance(this.dbService);
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.cacheExpirationInterval = this.options.getCacheExpirationInterval();

        // Initialize the lastRefreshTime and remove the cacheExpirationInterval in order to immediately have the cache
        // expired
        this.lastRefreshedTime = Calendar.getInstance();
        this.lastRefreshedTime.add(Calendar.SECOND, -this.cacheExpirationInterval);
        logger.debug(message.activatingFilterDone());
    }

    /**
     * OSGi service component callback for updating
     *
     * @param properties
     *            the updated properties
     */
    public void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingFilter() + properties);
        this.options = new DbWireRecordFilterOptions(properties);
        this.cacheExpirationInterval = this.options.getCacheExpirationInterval();

        // Initialize the lastRefreshTime and remove the cacheExpirationInterval in order to immediately have the cache
        // expired
        this.lastRefreshedTime = Calendar.getInstance();
        this.lastRefreshedTime.add(Calendar.SECOND, -this.cacheExpirationInterval);
        logger.debug(message.updatingFilterDone());
    }

    /**
     * OSGi service component callback for deactivation
     *
     * @param componentContext
     *            the component context
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingFilter());

        logger.debug(message.deactivatingFilterDone());
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    private List<WireRecord> refreshSQLView() throws SQLException {
        final List<WireRecord> dataRecords = new ArrayList<>();

        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        final String sqlView = this.options.getSqlView();
        try {
            conn = this.dbHelper.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(sqlView);
            if (nonNull(rset)) {
                while (rset.next()) {
                    final WireRecord wireRecord = new WireRecord();

                    final ResultSetMetaData rmet = rset.getMetaData();
                    for (int i = 1; i <= rmet.getColumnCount(); i++) {
                        String fieldName = rmet.getColumnLabel(i);
                        if (isNull(fieldName)) {
                            fieldName = rmet.getColumnName(i);
                        }

                        final int jdbcType = rmet.getColumnType(i);
                        final DataType dataType = DbDataTypeMapper.getDataType(jdbcType);
                        switch (dataType) {
                        case BOOLEAN:
                            final boolean boolValue = rset.getBoolean(i);
                            wireRecord.addProperty(fieldName, TypedValues.newBooleanValue(boolValue));
                            break;
                        case BYTE:
                            final byte byteValue = rset.getByte(i);
                            wireRecord.addProperty(fieldName, TypedValues.newByteValue(byteValue));
                            break;
                        case DOUBLE:
                            final double doubleValue = rset.getDouble(i);
                            wireRecord.addProperty(fieldName, TypedValues.newDoubleValue(doubleValue));
                            break;
                        case INTEGER:
                            final int intValue = rset.getInt(i);
                            wireRecord.addProperty(fieldName, TypedValues.newIntegerValue(intValue));
                            break;
                        case LONG:
                            final long longValue = rset.getLong(i);
                            wireRecord.addProperty(fieldName, TypedValues.newLongValue(longValue));
                            break;
                        case BYTE_ARRAY:
                            final byte[] bytesValue = rset.getBytes(i);
                            wireRecord.addProperty(fieldName, TypedValues.newByteArrayValue(bytesValue));
                            break;
                        case SHORT:
                            final short shortValue = rset.getShort(i);
                            wireRecord.addProperty(fieldName, TypedValues.newShortValue(shortValue));
                            break;
                        case STRING:
                            final String stringValue = rset.getString(i);
                            wireRecord.addProperty(fieldName, TypedValues.newStringValue(stringValue));
                            break;
                        default:
                            break;
                        }
                    }
                    dataRecords.add(wireRecord);
                }
            }
            logger.info(message.refreshed());
        } catch (final SQLException e) {
            throw e;
        } finally {
            this.dbHelper.close(rset);
            this.dbHelper.close(stmt);
            this.dbHelper.close(conn);
        }
        return dataRecords;
    }

    /**
     * Trigger data emit as soon as new {@link WireEnvelope} is received. The component caches the last database
     * read and
     * provides, as output, this value until the cache validity is not expired. Otherwise, a new database read is
     * performed, and the value is kept in the {@link #lastRecords} field.
     * The cache validity is determined by the {@link DbWireRecordFilterOptions#CONF_CACHE_EXPIRATION_INTERVAL} property
     * provided by the user in the component configuration.
     */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());
        logger.debug(message.wireEnvelopeReceived(), wireEnvelope);
        if (isCacheExpired()) {
            try {
                this.lastRecords = refreshSQLView();
                this.lastRefreshedTime = Calendar.getInstance(this.lastRefreshedTime.getTimeZone());
            } catch (SQLException e) {
                logger.error(message.errorFiltering(), e);
            }
        }

        if (nonNull(this.lastRecords)) {
            this.wireSupport.emit(this.lastRecords);
        }
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

    private boolean isCacheExpired() {
        final Calendar now = Calendar.getInstance();
        final Calendar nextRefreshTime = Calendar.getInstance(this.lastRefreshedTime.getTimeZone());
        nextRefreshTime.setTime(this.lastRefreshedTime.getTime());
        nextRefreshTime.add(Calendar.SECOND, this.cacheExpirationInterval);

        if (nextRefreshTime.after(now)) {
            return false;
        }
        return true;
    }
}
