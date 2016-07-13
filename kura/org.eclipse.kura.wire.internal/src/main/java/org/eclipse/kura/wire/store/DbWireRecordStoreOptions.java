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
package org.eclipse.kura.wire.store;

import static org.eclipse.kura.wire.store.DbWireRecordStore.PREFIX;

import java.util.Map;

/**
 * The Class DbWireRecordStoreOptions is responsible to contain all the DB Wire
 * Record Store related options
 */
final class DbWireRecordStoreOptions {

	/** The Constant denotes the period as configured for periodic cleanup. */
	private static final String PERIODIC_CLEANUP_ID = "periodic.cleanup";

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
