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

import java.util.Map;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;

/**
 * The Class DbWireRecordFilterOptions is responsible to contain all the Db Wire
 * Record related filter options
 */
final class DbWireRecordFilterOptions {

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private static final String CONF_CACHE_EXPIRATION_INTERVAL = "cache.expiration.interval";

    private static final String CONF_SQL_VIEW = "sql.view";

    private final Map<String, Object> properties;

    /**
     * Instantiates a new DB wire record filter options.
     *
     * @param properties
     *            the provided properties
     */
    DbWireRecordFilterOptions(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        this.properties = properties;
    }

    /**
     * Returns the cache interval as configured.
     *
     * @return the configured cache interval
     */
    int getCacheExpirationInterval() {
        int cacheInterval = 0;
        final Object cacheInt = this.properties.get(CONF_CACHE_EXPIRATION_INTERVAL);
        if (nonNull(cacheInt) && cacheInt instanceof Integer) {
            cacheInterval = (Integer) cacheInt;
        }
        return cacheInterval;
    }

    /**
     * Returns the SQL to be executed for this view.
     *
     * @return the configured SQL view
     */
    String getSqlView() {
        String sqlView = null;
        final Object view = this.properties.get(CONF_SQL_VIEW);
        if (nonNull(view) && view instanceof String) {
            sqlView = String.valueOf(view);
        }
        return sqlView;
    }
}