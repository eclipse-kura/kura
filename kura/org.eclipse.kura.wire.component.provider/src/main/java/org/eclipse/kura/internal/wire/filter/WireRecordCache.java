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

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireRecord;

/**
 * The Class WireRecordCache is responsible to contain the Wire Record cached
 * values.
 */
final class WireRecordCache {

	/** Map that is the cache. */
	private static final Map<Long, List<WireRecord>> m_map = CollectionUtil.newConcurrentHashMap();

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** Last refreshed time. */
	private Calendar m_lastRefreshedTime = null;

	/** DB Wire Record Filter instance. */
	private final DbWireRecordFilter m_recordFilter;

	/** Refresh duration in seconds */
	private int m_refreshDuration;

	/**
	 * Instantiates a new wire record cache.
	 *
	 * @param filter
	 *            the DB Wire Record filter
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	WireRecordCache(final DbWireRecordFilter filter) {
		checkNull(filter, s_message.dbFilterNonNull());
		this.m_recordFilter = filter;
	}

	/**
	 * Returns the object in the map based on input key.
	 *
	 * @param key
	 *            - key to get from cache map
	 * @return object for the particular key
	 */
	List<WireRecord> get(final long key) {
		if (this.refreshCache()) {
			m_map.put(this.m_lastRefreshedTime.getTimeInMillis(), this.m_recordFilter.filter());
		}
		return m_map.get(key);
	}

	/**
	 * Gets the last refreshed time.
	 *
	 * @return the last refreshed time
	 */
	Calendar getLastRefreshedTime() {
		return this.m_lastRefreshedTime;
	}

	/**
	 * Gets the refresh duration.
	 *
	 * @return the refresh duration
	 */
	int getRefreshDuration() {
		return this.m_refreshDuration;
	}

	/**
	 * Puts the object to the key provided in the cache map.
	 *
	 * @param key
	 *            - key to put in cache map
	 * @param value
	 *            - object for the key
	 */
	void put(final long key, final List<WireRecord> value) {
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
		final Calendar lastRefreshedTime = Calendar.getInstance(this.m_lastRefreshedTime.getTimeZone());
		lastRefreshedTime.setTime(this.m_lastRefreshedTime.getTime());
		lastRefreshedTime.add(Calendar.SECOND, this.m_refreshDuration);

		if (lastRefreshedTime.after(now)) {
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
	void setLastRefreshedTime(final Calendar lastRefreshedTime) {
		this.m_lastRefreshedTime = lastRefreshedTime;
	}

	/**
	 * Sets the refresh duration.
	 *
	 * @param refreshDuration
	 *            the new refresh duration
	 */
	void setRefreshDuration(final int refreshDuration) {
		this.m_refreshDuration = refreshDuration;
	}

}