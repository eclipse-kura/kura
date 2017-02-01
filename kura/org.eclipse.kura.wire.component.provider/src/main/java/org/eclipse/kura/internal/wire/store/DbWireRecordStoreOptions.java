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
package org.eclipse.kura.internal.wire.store;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.internal.wire.store.DbWireRecordStore.TABLE_NAME_PREFIX;

import java.util.Map;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;

/**
 * The Class DbWireRecordStoreOptions is responsible to contain all the DB Wire
 * Record Store related options
 */
final class DbWireRecordStoreOptions {

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private static final String PERIODIC_CLEANUP_ID = "periodic.cleanup";

    private static final String PERIODIC_CLEANUP_RECORDS_ID = "periodic.cleanup.records.keep";

    private static final String TABLE_NAME = "table.name";

    private final Map<String, Object> properties;

    /**
     * Instantiates a new DB wire record store options.
     *
     * @param properties
     *            the configured properties
     */
    DbWireRecordStoreOptions(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        this.properties = properties;
    }

    /**
     * Returns the number of records to keep as configured .
     *
     * @return the number of records
     */
    int getNoOfRecordsToKeep() {
        int noOfRecords = 0;
        final Object cleanUp = this.properties.get(PERIODIC_CLEANUP_RECORDS_ID);
        if (cleanUp != null && cleanUp instanceof Integer) {
            noOfRecords = (Integer) cleanUp;
        }
        return noOfRecords;
    }

    /**
     * Returns the period as configured for the periodic cleanup.
     *
     * @return the period
     */
    int getPeriodicCleanupRate() {
        int period = 0;
        final Object rate = this.properties.get(PERIODIC_CLEANUP_ID);
        if (rate != null && rate instanceof Integer) {
            period = (Integer) rate;
        }
        return period;
    }

    /**
     * Returns the name of the table as configured.
     *
     * @return the name of the table
     */
    String getTableName() {
        String tableName = null;
        final Object name = this.properties.get(TABLE_NAME);
        if (name != null && name instanceof String) {
            tableName = name.toString();
        }
        return TABLE_NAME_PREFIX + tableName;
    }
}