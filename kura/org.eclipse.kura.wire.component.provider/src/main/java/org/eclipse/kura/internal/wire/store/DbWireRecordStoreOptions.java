/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.store;

import static org.eclipse.kura.internal.wire.store.DbWireRecordStore.PREFIX;

import java.util.Map;

/**
 * The Class DbWireRecordStoreOptions is responsible to contain all the DB Wire
 * Record Store related options
 */
final class DbWireRecordStoreOptions {

    /** The Constant denotes the period as configured for periodic cleanup. */
    private static final String PERIODIC_CLEANUP_ID = "periodic.cleanup";

    /** The Constant denotes the number of records in the table to keep. */
    private static final String PERIODIC_CLEANUP_RECORDS_ID = "periodic.cleanup.records.keep";

    /** The Constant denotes the name of the table to perform operations on. */
    private static final String TABLE_NAME = "table.name";

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new DB wire record store options.
     *
     * @param properties
     *            the configured properties
     */
    DbWireRecordStoreOptions(final Map<String, Object> properties) {
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
        if ((this.properties != null) && this.properties.containsKey(PERIODIC_CLEANUP_RECORDS_ID) && (cleanUp != null)
                && (cleanUp instanceof Integer)) {
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
        if ((this.properties != null) && this.properties.containsKey(PERIODIC_CLEANUP_ID) && (rate != null)
                && (rate instanceof Integer)) {
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
        if ((this.properties != null) && this.properties.containsKey(TABLE_NAME) && (name != null)
                && (name instanceof String)) {
            tableName = name.toString();
        }
        return PREFIX + tableName;
    }

}