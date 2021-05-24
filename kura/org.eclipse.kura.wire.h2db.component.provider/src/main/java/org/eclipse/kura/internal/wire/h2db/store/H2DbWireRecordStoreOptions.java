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
package org.eclipse.kura.internal.wire.h2db.store;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class DbWireRecordStoreOptions is responsible to contain all the DB Wire
 * Record Store related options
 */
final class H2DbWireRecordStoreOptions {

    private static final int DEFAULT_MAXIMUM_TABLE_SIZE = 10000;

    private static final String MAXIMUM_TABLE_SIZE = "maximum.table.size";

    private static final String CLEANUP_RECORDS_KEEP = "cleanup.records.keep";

    private static final String TABLE_NAME = "table.name";

    private final Map<String, Object> properties;

    /**
     * Instantiates a new DB wire record store options.
     *
     * @param properties
     *            the configured properties
     */
    H2DbWireRecordStoreOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
    }

    /**
     * Returns the number of records to keep as configured .
     *
     * @return the number of records
     */
    int getNoOfRecordsToKeep() {
        int noOfRecords = 5000;
        final Object cleanUp = this.properties.get(CLEANUP_RECORDS_KEEP);
        if (nonNull(cleanUp) && cleanUp instanceof Integer) {
            noOfRecords = (Integer) cleanUp;
        }
        return noOfRecords;
    }

    int getMaximumTableSize() {
        int maximumSize = DEFAULT_MAXIMUM_TABLE_SIZE;
        final Object propertiesMaximumSize = this.properties.get(MAXIMUM_TABLE_SIZE);
        if (nonNull(propertiesMaximumSize) && propertiesMaximumSize instanceof Integer) {
            maximumSize = (Integer) propertiesMaximumSize;
        }
        return maximumSize;
    }

    /**
     * Returns the name of the table as configured.
     *
     * @return the name of the table
     */
    String getTableName() {
        String tableName = null;
        final Object name = this.properties.get(TABLE_NAME);
        if (nonNull(name) && name instanceof String) {
            tableName = name.toString();
        }
        return tableName;
    }
}