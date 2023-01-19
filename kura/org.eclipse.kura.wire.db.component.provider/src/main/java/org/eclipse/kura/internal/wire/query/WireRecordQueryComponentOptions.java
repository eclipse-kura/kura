/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.query;

import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.util.configuration.Property;

public final class WireRecordQueryComponentOptions {

    private static final Property<String> QUERY_PROPERTY = new Property<>("query",
            "SELECT * FROM \"WR_data\" LIMIT 10;");
    private static final Property<Integer> CACHE_EXPIRATION_INTERVAL_PROPERTY = new Property<>(
            "cache.expiration.interval", 0);
    private static final Property<Boolean> EMIT_ON_EMPTY_RESULT_PROPERTY = new Property<>("emit.on.empty.result", true);

    private final String query;
    private final int cacheExpirationInterval;
    private final boolean emitOnEmptyResult;

    public WireRecordQueryComponentOptions(final Map<String, Object> properties) {
        this.cacheExpirationInterval = CACHE_EXPIRATION_INTERVAL_PROPERTY.get(properties);
        this.query = QUERY_PROPERTY.get(properties);
        this.emitOnEmptyResult = EMIT_ON_EMPTY_RESULT_PROPERTY.get(properties);
    }

    public String getQuery() {
        return query;
    }

    public int getCacheExpirationInterval() {
        return cacheExpirationInterval;
    }

    public boolean isEmitOnEmptyResult() {
        return emitOnEmptyResult;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cacheExpirationInterval, emitOnEmptyResult, query);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WireRecordQueryComponentOptions)) {
            return false;
        }
        WireRecordQueryComponentOptions other = (WireRecordQueryComponentOptions) obj;
        return cacheExpirationInterval == other.cacheExpirationInterval && emitOnEmptyResult == other.emitOnEmptyResult
                && Objects.equals(query, other.query);
    }

}