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
package org.eclipse.kura.internal.wire.filter;

import java.util.Map;

import org.eclipse.kura.wire.SeverityLevel;

/**
 * The Class DbWireRecordFilterOptions is responsible to contain all the Db Wire
 * Record related filter options
 */
final class DbWireRecordFilterOptions {

	/** The Constant denotes the cache update interval. */
	private static final String CONF_CACHE_INTERVAL = "cache.update.interval";

	/** The Constant denotes the refresh rate. */
	private static final String CONF_REFRESH_RATE = "refresh.rate";

	/** The Constant denotes SQL view. */
	private static final String CONF_SQL_VIEW = "sql.view";

	/** The Constant denoting severity level. */
	private static final String SEVERITY_LEVEL = "severity.level";

	/** The properties as associated */
	private final Map<String, Object> m_properties;

	/**
	 * Instantiates a new DB wire record filter options.
	 *
	 * @param properties
	 *            the provided properties
	 */
	DbWireRecordFilterOptions(final Map<String, Object> properties) {
		this.m_properties = properties;
	}

	/**
	 * Returns the cache interval as configured.
	 *
	 * @return the configured cache interval
	 */
	int getCacheInterval() {
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
	int getRefreshRate() {
		int refreshRate = 0;
		if ((this.m_properties != null) && (this.m_properties.containsKey(CONF_REFRESH_RATE))
				&& (this.m_properties.get(CONF_REFRESH_RATE) instanceof Integer)) {
			refreshRate = (Integer) this.m_properties.get(CONF_REFRESH_RATE);
		}
		return refreshRate;
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
	 * Returns the SQL to be executed for this view.
	 *
	 * @return the configured SQL view
	 */
	String getSqlView() {
		String sqlView = null;
		if ((this.m_properties != null) && (this.m_properties.containsKey(CONF_SQL_VIEW))
				&& (this.m_properties.get(CONF_SQL_VIEW) instanceof String)) {
			sqlView = String.valueOf(this.m_properties.get(CONF_SQL_VIEW));
		}
		return sqlView;
	}

}
