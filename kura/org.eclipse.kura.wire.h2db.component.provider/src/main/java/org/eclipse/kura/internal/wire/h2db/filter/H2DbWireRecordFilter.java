/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.h2db.filter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.internal.wire.h2db.common.H2DbServiceHelper;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class DbWireRecordFilter is responsible for representing a wire component
 * which is focused on performing an user defined SQL query in a database table and emitting the result as a Wire
 * Envelope.
 */
public class H2DbWireRecordFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LogManager.getLogger(H2DbWireRecordFilter.class);

    private List<WireRecord> lastRecords;

    private H2DbServiceHelper dbHelper;

    private H2DbService dbService;

    private H2DbWireRecordFilterOptions options;

    private volatile WireHelperService wireHelperService;

    private WireSupport wireSupport;

    private Calendar lastRefreshedTime;

    private int cacheExpirationInterval;

    public synchronized void bindDbService(H2DbService dbService) {
        this.dbService = dbService;
        this.dbHelper = H2DbServiceHelper.of(dbService);
    }

    public synchronized void unbindDbService(H2DbService dbService) {
        if (this.dbService == dbService) {
            this.dbHelper = null;
            this.dbService = null;
            this.options = null;
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
        logger.debug("Activating DB Wire Record Filter...");
        this.options = new H2DbWireRecordFilterOptions(properties);

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        this.cacheExpirationInterval = this.options.getCacheExpirationInterval();

        // Initialize the lastRefreshTime and remove the cacheExpirationInterval in order to immediately have the cache
        // expired
        this.lastRefreshedTime = Calendar.getInstance();
        this.lastRefreshedTime.add(Calendar.SECOND, -this.cacheExpirationInterval);
        logger.debug("Activating DB Wire Record Filter... Done");
    }

    /**
     * OSGi service component callback for updating
     *
     * @param properties
     *            the updated properties
     */
    public void updated(final Map<String, Object> properties) {
        logger.debug("Updating DB Wire Record Filter... {}", properties);

        final String oldSqlView = this.options.getSqlView();

        this.options = new H2DbWireRecordFilterOptions(properties);

        this.cacheExpirationInterval = this.options.getCacheExpirationInterval();

        // Initialize the lastRefreshTime and remove the cacheExpirationInterval in order to immediately have the cache
        // expired
        this.lastRefreshedTime = Calendar.getInstance();
        this.lastRefreshedTime.add(Calendar.SECOND, -this.cacheExpirationInterval);

        // do not want the history related to other queries
        if (!oldSqlView.equals(this.options.getSqlView())) {
            this.lastRecords = null;
        }

        logger.debug("Updating DB Wire Record Filter... Done");
    }

    /**
     * OSGi service component callback for deactivation
     *
     * @param componentContext
     *            the component context
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("Dectivating DB Wire Record Filter...");
        this.dbHelper = null;
        this.dbService = null;
        this.options = null;
        logger.debug("Dectivating DB Wire Record Filter... Done");
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    private List<WireRecord> performSQLQuery() throws SQLException {

        final String sqlView = this.options.getSqlView();

        return this.dbHelper.withConnection(c -> {
            final List<WireRecord> dataRecords = new ArrayList<>();

            try (final Statement stmt = c.createStatement(); final ResultSet rset = stmt.executeQuery(sqlView)) {
                while (rset.next()) {
                    final WireRecord wireRecord = new WireRecord(convertSQLRowToWireRecord(rset));
                    dataRecords.add(wireRecord);
                }
            }
            logger.debug("Refreshed typed values");
            return dataRecords;
        });
    }

    private Map<String, TypedValue<?>> convertSQLRowToWireRecord(ResultSet rset) throws SQLException {
        final Map<String, TypedValue<?>> wireRecordProperties = new HashMap<>();
        final ResultSetMetaData rmet = rset.getMetaData();
        for (int i = 1; i <= rmet.getColumnCount(); i++) {
            String fieldName = rmet.getColumnLabel(i);
            Object dbExtractedData = rset.getObject(i);

            if (isNull(fieldName)) {
                fieldName = rmet.getColumnName(i);
            }

            if (isNull(dbExtractedData)) {
                continue;
            }

            if (dbExtractedData instanceof Blob) {
                final Blob dbExtractedBlob = (Blob) dbExtractedData;
                final int dbExtractedBlobLength = (int) dbExtractedBlob.length();
                dbExtractedData = dbExtractedBlob.getBytes(1, dbExtractedBlobLength);
            }

            final TypedValue<?> value = TypedValues.newTypedValue(dbExtractedData);
            wireRecordProperties.put(fieldName, value);
        }
        return wireRecordProperties;
    }

    /**
     * Trigger data emit as soon as new {@link WireEnvelope} is received. The component caches the last database
     * read and provides, as output, this value until the cache validity is not expired or the query is changed.
     * Otherwise, a new database read is performed, and the value is kept in the {@link #lastRecords} field.
     * The cache validity is determined by the {@link H2DbWireRecordFilterOptions#CONF_CACHE_EXPIRATION_INTERVAL}
     * property
     * provided by the user in the component configuration.
     */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");

        if (this.dbHelper == null) {
            logger.warn("H2DbService instance not attached");
            return;
        }

        if (isCacheExpired() && this.dbHelper != null) {
            refreshCachedRecords();
        }

        List<WireRecord> result;
        if (nonNull(this.lastRecords)) {
            result = Collections.unmodifiableList(this.lastRecords);
        } else {
            result = Collections.unmodifiableList(new ArrayList<WireRecord>());
        }

        if (!result.isEmpty() || this.options.emitOnEmptyResult()) {
            this.wireSupport.emit(result);
        }
    }

    private void refreshCachedRecords() {
        try {
            final List<WireRecord> tmpWireRecords = performSQLQuery();
            this.lastRecords = tmpWireRecords;
            this.lastRefreshedTime = Calendar.getInstance(this.lastRefreshedTime.getTimeZone());
        } catch (SQLException e) {
            logger.error("Error while filtering Wire Records...", e);
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
