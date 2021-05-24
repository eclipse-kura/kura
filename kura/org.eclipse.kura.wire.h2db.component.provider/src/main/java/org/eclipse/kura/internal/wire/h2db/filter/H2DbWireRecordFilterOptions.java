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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class DbWireRecordFilterOptions is responsible to contain all the Db Wire
 * Record related filter options
 */
final class H2DbWireRecordFilterOptions {

    private static final String CONF_CACHE_EXPIRATION_INTERVAL = "cache.expiration.interval";

    private static final String CONF_SQL_VIEW = "sql.view";

    private static final String EMIT_ON_EMPTY_RESULT = "emit.on.empty.result";

    private final Map<String, Object> properties;

    /**
     * Instantiates a new DB wire record filter options.
     *
     * @param properties
     *            the provided properties
     */
    H2DbWireRecordFilterOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
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

    boolean emitOnEmptyResult() {
        boolean result = true;
        final Object emitOnEmptyResult = this.properties.get(EMIT_ON_EMPTY_RESULT);
        if (nonNull(emitOnEmptyResult) && emitOnEmptyResult instanceof Boolean) {
            result = (Boolean) emitOnEmptyResult;
        }
        return result;
    }
}