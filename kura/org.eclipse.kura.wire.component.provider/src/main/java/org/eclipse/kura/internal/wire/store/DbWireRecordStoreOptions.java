/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.wire.store;

import static org.eclipse.kura.internal.wire.store.DbWireRecordStore.PREFIX;

import java.util.Map;

import org.eclipse.kura.wire.SeverityLevel;

/**
 * The Class DbWireRecordStoreOptions is responsible to contain all the DB Wire
 * Record Store related options
 */
final class DbWireRecordStoreOptions {

	/** The Constant denotes the period as configured for periodic cleanup. */
	private static final String PERIODIC_CLEANUP_ID = "periodic.cleanup";

	/** The Constant denotes the number of records in the table to keep. */
	private static final String PERIODIC_CLEANUP_RECORDS_ID = "periodic.cleanup.records.keep";

	/** The Constant denoting severity level. */
	private static final String SEVERITY_LEVEL = "severity.level";

	/** The Constant denotes the name of the table to perform operations on. */
	private static final String TABLE_NAME = "table.name";

	/** The properties as associated */
	private final Map<String, Object> m_properties;

	/**
	 * Instantiates a new DB wire record store options.
	 *
	 * @param properties
	 *            the configured properties
	 */
	DbWireRecordStoreOptions(final Map<String, Object> properties) {
		this.m_properties = properties;
	}

	/**
	 * Returns the number of records to keep as configured .
	 *
	 * @return the number of records
	 */
	int getNoOfRecordsToKeep() {
		int noOfRecords = 0;
		if ((this.m_properties != null) && this.m_properties.containsKey(PERIODIC_CLEANUP_RECORDS_ID)
				&& (this.m_properties.get(PERIODIC_CLEANUP_RECORDS_ID) != null)
				&& (this.m_properties.get(PERIODIC_CLEANUP_RECORDS_ID) instanceof Integer)) {
			noOfRecords = (Integer) this.m_properties.get(PERIODIC_CLEANUP_RECORDS_ID);
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
		if ((this.m_properties != null) && this.m_properties.containsKey(PERIODIC_CLEANUP_ID)
				&& (this.m_properties.get(PERIODIC_CLEANUP_ID) != null)
				&& (this.m_properties.get(PERIODIC_CLEANUP_ID) instanceof Integer)) {
			period = (Integer) this.m_properties.get(PERIODIC_CLEANUP_ID);
		}
		return period;
	}

	/**
	 * Returns the severity level of accepted wire fields.
	 *
	 * @return the severity level
	 */
	SeverityLevel getSeverityLevel() {
		String severityLevel = "ERROR";
		if ((this.m_properties != null) && this.m_properties.containsKey(SEVERITY_LEVEL)
				&& (this.m_properties.get(SEVERITY_LEVEL) != null)
				&& (this.m_properties.get(SEVERITY_LEVEL) instanceof String)) {
			severityLevel = String.valueOf(this.m_properties.get(SEVERITY_LEVEL));
		}
		if ("ERROR".equalsIgnoreCase(severityLevel)) {
			return SeverityLevel.ERROR;
		}
		if ("INFO".equalsIgnoreCase(severityLevel)) {
			return SeverityLevel.INFO;
		}
		if ("CONFIG".equalsIgnoreCase(severityLevel)) {
			return SeverityLevel.CONFIG;
		}
		return SeverityLevel.ERROR;
	}

	/**
	 * Returns the name of the table as configured.
	 *
	 * @return the name of the table
	 */
	String getTableName() {
		String tableName = null;
		if ((this.m_properties != null) && this.m_properties.containsKey(TABLE_NAME)
				&& (this.m_properties.get(TABLE_NAME) != null)
				&& (this.m_properties.get(TABLE_NAME) instanceof String)) {
			tableName = this.m_properties.get(TABLE_NAME).toString();
		}
		return PREFIX + tableName;
	}

}
