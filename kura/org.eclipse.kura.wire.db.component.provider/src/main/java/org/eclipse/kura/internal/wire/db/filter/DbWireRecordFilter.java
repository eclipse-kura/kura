/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.db.filter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.BaseDbService;
import org.eclipse.kura.db.H2DbService;
import org.eclipse.kura.internal.wire.db.common.BaseDbServiceProviderImpl;
import org.eclipse.kura.internal.wire.db.common.DbServiceProvider;
import org.eclipse.kura.internal.wire.db.common.H2DbServiceProviderImpl;
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
public class DbWireRecordFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LogManager.getLogger(DbWireRecordFilter.class);

    private DbServiceProvider dbServiceProvider;
    private List<WireRecord> lastRecords;
    private BaseDbService dbService;
    private DbWireRecordFilterOptions options;
    private WireHelperService wireHelperService;
    private WireSupport wireSupport;
    private Calendar lastRefreshedTime;
    private int cacheExpirationInterval;

    public synchronized void bindDbService(BaseDbService dbService) {
        this.dbService = dbService;
        if (this.dbService instanceof H2DbService) {
            this.dbServiceProvider = new H2DbServiceProviderImpl((H2DbService) this.dbService);
        } else {
            this.dbServiceProvider = new BaseDbServiceProviderImpl(this.dbService);
        }
    }

    public synchronized void unbindDbService(BaseDbService dbService) {
        if (this.dbService == dbService) {
            this.dbServiceProvider = null;
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
        this.options = new DbWireRecordFilterOptions(properties);

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

        this.options = new DbWireRecordFilterOptions(properties);

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
        this.dbServiceProvider = null;
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
        return this.dbServiceProvider.performSQLQuery(sqlView);
    }

    /**
     * Trigger data emit as soon as new {@link WireEnvelope} is received. The component caches the last database
     * read and provides, as output, this value until the cache validity is not expired or the query is changed.
     * Otherwise, a new database read is performed, and the value is kept in the {@link #lastRecords} field.
     * The cache validity is determined by the {@link DbWireRecordFilterOptions#CONF_CACHE_EXPIRATION_INTERVAL}
     * property
     * provided by the user in the component configuration.
     */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");

        if (this.dbServiceProvider == null) {
            logger.warn("DbService instance not attached");
            return;
        }

        if (isCacheExpired()) {
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

        return !nextRefreshTime.after(now);
    }
}
