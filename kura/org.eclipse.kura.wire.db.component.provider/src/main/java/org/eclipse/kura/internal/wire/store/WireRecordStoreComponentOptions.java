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
package org.eclipse.kura.internal.wire.store;

import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.util.configuration.Property;

public final class WireRecordStoreComponentOptions {

    private static final Property<String> STORE_NAME_PROPERTY = new Property<>("store.name", "WR_data");
    private static final Property<Integer> MAXIMUM_STORE_SIZE_PROPERTY = new Property<>("maximum.store.size", 10000);
    private static final Property<Integer> CLEANUP_RECORDS_KEEP_PROPERTY = new Property<>("cleanup.records.keep", 5000);

    private final String storeName;
    private final int maximumStoreSize;
    private final int cleanupRecordsKeep;

    public WireRecordStoreComponentOptions(final Map<String, Object> properties) {
        this.storeName = STORE_NAME_PROPERTY.get(properties);
        this.maximumStoreSize = MAXIMUM_STORE_SIZE_PROPERTY.get(properties);
        this.cleanupRecordsKeep = CLEANUP_RECORDS_KEEP_PROPERTY.get(properties);
    }

    public int getCleanupRecordsKeep() {
        return cleanupRecordsKeep;
    }

    public int getMaximumStoreSize() {
        return maximumStoreSize;
    }

    public String getStoreName() {
        return storeName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cleanupRecordsKeep, maximumStoreSize, storeName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WireRecordStoreComponentOptions)) {
            return false;
        }
        WireRecordStoreComponentOptions other = (WireRecordStoreComponentOptions) obj;
        return cleanupRecordsKeep == other.cleanupRecordsKeep && maximumStoreSize == other.maximumStoreSize
                && Objects.equals(storeName, other.storeName);
    }

}