/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.filter;

import java.util.Map;

/**
 * The Class DbWireRecordFilterOptions is responsible to contain all the Db Wire
 * Record related filter options
 */
final class DbWireRecordFilterOptions {

    /** The Constant denotes the cache max capacity. */
    private static final String CONF_CACHE_CAPACITY = "cache.max.capacity";

    /** The Constant denotes the cache update interval. */
    private static final String CONF_CACHE_INTERVAL = "cache.update.interval";

    /** The Constant denotes the refresh rate. */
    private static final String CONF_REFRESH_RATE = "refresh.rate";

    /** The Constant denotes SQL view. */
    private static final String CONF_SQL_VIEW = "sql.view";

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new DB wire record filter options.
     *
     * @param properties
     *            the provided properties
     */
    DbWireRecordFilterOptions(final Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Returns the cache max capacity as configured.
     *
     * @return the configured cache max capacity
     */
    int getCacheCapacity() {
        int cacheSize = 0;
        final Object cacheCapacity = this.properties.get(CONF_CACHE_CAPACITY);
        if ((this.properties != null) && (this.properties.containsKey(CONF_CACHE_CAPACITY))
                && (cacheCapacity instanceof String)) {
            cacheSize = (Integer) cacheCapacity;
        }
        return cacheSize;
    }

    /**
     * Returns the cache interval as configured.
     *
     * @return the configured cache interval
     */
    int getCacheInterval() {
        int cacheInterval = 0;
        final Object cacheInt = this.properties.get(CONF_CACHE_INTERVAL);
        if ((this.properties != null) && (this.properties.containsKey(CONF_CACHE_INTERVAL))
                && (cacheInt instanceof String)) {
            cacheInterval = (Integer) cacheInt;
        }
        return cacheInterval;
    }

    /**
     * Returns the rate of refresh for this view.
     *
     * @return the refresh rate
     */
    int getRefreshRate() {
        int refreshRate = 0;
        final Object cacheRefreshRate = this.properties.get(CONF_REFRESH_RATE);
        if ((this.properties != null) && (this.properties.containsKey(CONF_REFRESH_RATE))
                && (cacheRefreshRate instanceof Integer)) {
            refreshRate = (Integer) cacheRefreshRate;
        }
        return refreshRate;
    }

    /**
     * Returns the SQL to be executed for this view.
     *
     * @return the configured SQL view
     */
    String getSqlView() {
        String sqlView = null;
        final Object view = this.properties.get(CONF_SQL_VIEW);
        if ((this.properties != null) && (this.properties.containsKey(CONF_SQL_VIEW)) && (view instanceof String)) {
            sqlView = String.valueOf(view);
        }
        return sqlView;
    }

}