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

import java.util.Map;

import org.eclipse.kura.wire.internal.AbstractConfigurationOptions;

/**
 * The Class DbWireRecordFilterOptions is responsible to contain all the Db Wire
 * Record related filter options
 */
public final class DbWireRecordFilterOptions extends AbstractConfigurationOptions {

	/** The Constant denotes the cache update interval. */
	private static final String CONF_CACHE_INTERVAL = "cache.update.interval";

	/** The Constant denotes the refresh rate. */
	private static final String CONF_REFRESH_RATE = "refresh.rate";

	/** The Constant denotes SQL view. */
	private static final String CONF_SQL_VIEW = "sql.view";

	/**
	 * Instantiates a new DB wire record filter options.
	 *
	 * @param properties
	 *            the provided properties
	 */
	public DbWireRecordFilterOptions(final Map<String, Object> properties) {
		super(properties);
	}

	/**
	 * Returns the cache interval as configured.
	 *
	 * @return the configured cache interval
	 */
	public int getCacheInterval() {
		int cacheInterval = 0;
		if ((this.m_properties != null) && (this.m_properties.containsKey(CONF_CACHE_INTERVAL))
				&& (this.m_properties.get(CONF_CACHE_INTERVAL) instanceof String)) {
			cacheInterval = (Integer) this.m_properties.get(CONF_CACHE_INTERVAL);
		}
		return cacheInterval;
	}

	/**
	 * Returns the rate of refresh for this view.
	 *
	 * @return the refresh rate
	 */
	public int getRefreshRate() {
		int refreshRate = 0;
		if ((this.m_properties != null) && (this.m_properties.containsKey(CONF_REFRESH_RATE))
				&& (this.m_properties.get(CONF_REFRESH_RATE) instanceof Integer)) {
			refreshRate = (Integer) this.m_properties.get(CONF_REFRESH_RATE);
		}
		return refreshRate;
	}

	/**
	 * Returns the SQL to be executed for this view.
	 *
	 * @return the configured SQL view
	 */
	public String getSqlView() {
		String sqlView = null;
		if ((this.m_properties != null) && (this.m_properties.containsKey(CONF_SQL_VIEW))
				&& (this.m_properties.get(CONF_SQL_VIEW) instanceof String)) {
			sqlView = String.valueOf(this.m_properties.get(CONF_SQL_VIEW));
		}
		return sqlView;
	}

}
