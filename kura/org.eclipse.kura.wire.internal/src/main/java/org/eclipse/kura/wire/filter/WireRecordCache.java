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
package org.eclipse.kura.wire.filter;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.kura.wire.WireRecord;

/**
 * The Class WireRecordCache is responsible to contain the Wire Record cached
 * values.
 */
public final class WireRecordCache {

	/** Map that is the cache. */
	private static final Map<Long, List<WireRecord>> m_map = new ConcurrentHashMap<Long, List<WireRecord>>();

	/** Last refreshed time. */
	private Calendar m_lastRefreshedTime = null;

	/** DB Wire Record Filter instance. */
	private final DbWireRecordFilter m_recordFilter;

	/** Refresh duration in seconds - 1 hour = 60 * 60 = 3600 seconds. */
	private int m_refreshDuration = 3600;

	/**
	 * Instantiates a new wire record cache.
	 *
	 * @param filter
	 *            the DB Wire Record filter
	 */
	public WireRecordCache(final DbWireRecordFilter filter) {
		this.m_recordFilter = filter;
	}

	/**
	 * Gets the last refreshed time.
	 *
	 * @return the last refreshed time
	 */
	public Calendar getLastRefreshedTime() {
		return this.m_lastRefreshedTime;
	}

	/**
	 * Gets the refresh duration.
	 *
	 * @return the refresh duration
	 */
	public int getRefreshDuration() {
		return this.m_refreshDuration;
	}

	/**
	 * Returns the object in the map based on input key.
	 *
	 * @param key
	 *            - key to put in cache map
	 * @return object for the particular key
	 */
	public List<WireRecord> getValue(final Long key) {
		if (this.refreshCache()) {
			m_map.put(this.m_lastRefreshedTime.getTimeInMillis(), this.m_recordFilter.filter());
		}
		return m_map.get(key);
	}

	/**
	 * Puts the object to the key provided in the cache map.
	 *
	 * @param key
	 *            - key to put in cache map
	 * @param value
	 *            - object for the key
	 */
	public void put(final Long key, final List<WireRecord> value) {
		m_map.put(key, value);
		this.m_lastRefreshedTime = Calendar.getInstance();
	}

	/**
	 * Refreshes the Cache as per the cache duration set.
	 *
	 * @return true or false if cache is expired or not
	 */
	private boolean refreshCache() {
		if (this.m_lastRefreshedTime == null) {
			this.m_lastRefreshedTime = Calendar.getInstance();
			return false;
		}
		final Calendar now = Calendar.getInstance();
		this.m_lastRefreshedTime.add(Calendar.SECOND, this.m_refreshDuration);

		if (this.m_lastRefreshedTime.after(now)) {
			return false;
		} else {
			// Cache expired hence refresh it
			this.m_lastRefreshedTime = Calendar.getInstance();
			return true;
		}
	}

	/**
	 * Sets the last refreshed time.
	 *
	 * @param lastRefreshedTime
	 *            the new last refreshed time
	 */
	public void setLastRefreshedTime(final Calendar lastRefreshedTime) {
		this.m_lastRefreshedTime = lastRefreshedTime;
	}

	/**
	 * Sets the refresh duration.
	 *
	 * @param refreshDuration
	 *            the new refresh duration
	 */
	public void setRefreshDuration(final int refreshDuration) {
		this.m_refreshDuration = refreshDuration;
	}

}