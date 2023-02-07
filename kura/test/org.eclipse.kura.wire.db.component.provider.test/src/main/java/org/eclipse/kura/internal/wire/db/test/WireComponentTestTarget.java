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
package org.eclipse.kura.internal.wire.db.test;

public interface WireComponentTestTarget {

    public static final WireComponentTestTarget DB_FILTER_AND_DB_STORE = new DbFilterStore();
    public static final WireComponentTestTarget WIRE_RECORD_QUERY_AND_WIRE_RECORD_STORE = new WireRecordQueryStore();

    public String storeFactoryPid();

    public String filterFactoryPid();

    public String filterQueryPropertyKey();

    public String filterCacheExpirationIntervalKey();

    public String filterReferenceKey();

    public String filterEmitOnEmptyResultKey();

    public String storeNameKey();

    public String storeMaximumSizeKey();

    public String storeCleanupRecordsKeepKey();

    public String storeReferenceKey();

    public class WireRecordQueryStore implements WireComponentTestTarget {

        @Override
        public String storeFactoryPid() {
            return "org.eclipse.kura.wire.WireRecordStore";
        }

        @Override
        public String filterFactoryPid() {
            return "org.eclipse.kura.wire.WireRecordQuery";
        }

        @Override
        public String filterQueryPropertyKey() {
            return "query";
        }

        @Override
        public String filterCacheExpirationIntervalKey() {
            return "cache.expiration.interval";
        }

        @Override
        public String filterReferenceKey() {
            return "QueryableWireRecordStoreProvider.target";
        }

        @Override
        public String filterEmitOnEmptyResultKey() {
            return "emit.on.empty.result";
        }

        @Override
        public String storeNameKey() {
            return "store.name";
        }

        @Override
        public String storeMaximumSizeKey() {
            return "maximum.store.size";
        }

        @Override
        public String storeCleanupRecordsKeepKey() {
            return "cleanup.records.keep";
        }

        @Override
        public String storeReferenceKey() {
            return "WireRecordStoreProvider.target";
        }

        @Override
        public String toString() {
            return "Wire Record Query and Wire Record Store";
        }
    }

    public class DbFilterStore implements WireComponentTestTarget {

        @Override
        public String storeFactoryPid() {
            return "org.eclipse.kura.wire.DbWireRecordStore";
        }

        @Override
        public String filterFactoryPid() {
            return "org.eclipse.kura.wire.DbWireRecordFilter";
        }

        @Override
        public String filterQueryPropertyKey() {
            return "sql.view";
        }

        @Override
        public String filterCacheExpirationIntervalKey() {
            return "cache.expiration.interval";
        }

        @Override
        public String filterReferenceKey() {
            return "BaseDbService.target";
        }

        @Override
        public String filterEmitOnEmptyResultKey() {
            return "emit.on.empty.result";
        }

        @Override
        public String storeNameKey() {
            return "table.name";
        }

        @Override
        public String storeMaximumSizeKey() {
            return "maximum.table.size";
        }

        @Override
        public String storeCleanupRecordsKeepKey() {
            return "cleanup.records.keep";
        }

        @Override
        public String storeReferenceKey() {
            return "BaseDbService.target";
        }

        @Override
        public String toString() {
            return "Db Store and Db Filter";
        }

    }
}
