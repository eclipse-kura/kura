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
package org.eclipse.kura.wire.cloud.publisher;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.kura.data.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CloudPublisherDisconnectManager manages the disconnection with
 * Cloud Publisher
 */
public final class CloudPublisherDisconnectManager {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherDisconnectManager.class);

	/** The Constant denotes timer name. */
	private static final String TIMER_NAME = "CloudPublisherDisconnectManager";

	/** The data service dependency. */
	private final DataService m_dataService;

	/** The next execution time. */
	private long m_nextExecutionTime;

	/** The quiece timeout. */
	private long m_quieceTimeout;

	/** The timer instance. */
	private Timer m_timer;

	/**
	 * Instantiates a new cloud publisher disconnect manager.
	 *
	 * @param dataService
	 *            the data service
	 * @param quieceTimeout
	 *            the quiece timeout
	 */
	public CloudPublisherDisconnectManager(final DataService dataService, final long quieceTimeout) {
		this.m_dataService = dataService;
		this.m_quieceTimeout = quieceTimeout;
		this.m_nextExecutionTime = 0;
	}

	/**
	 * Disconnect in minutes.
	 *
	 * @param minutes
	 *            the minutes
	 */
	public synchronized void disconnectInMinutes(final int minutes) {
		// check if the required timeout is longer than the one already
		// scheduled
		final long remainingDelay = this.m_nextExecutionTime - System.currentTimeMillis();
		final long requiredDelay = (long) minutes * 60 * 1000;
		if (requiredDelay > remainingDelay) {
			this.scheduleNewTimer(requiredDelay);
		}
	}

	/**
	 * Gets the quiece timeout.
	 *
	 * @return the quiece timeout
	 */
	public long getQuieceTimeout() {
		return this.m_quieceTimeout;
	}

	/**
	 * Schedule new timer.
	 *
	 * @param delay
	 *            the delay
	 */
	private void scheduleNewTimer(final long delay) {
		// cancel existing timer
		if (this.m_timer != null) {
			this.m_timer.cancel();
		}

		// calculate next execution
		s_logger.info("Scheduling disconnect in {} msec...", delay);
		this.m_nextExecutionTime = System.currentTimeMillis() + delay;

		// start new timer
		this.m_timer = new Timer(TIMER_NAME);
		this.m_timer.schedule(new TimerTask() {

			@Override
			public void run() {
				// disconnect
				try {
					CloudPublisherDisconnectManager.this.m_dataService
							.disconnect(CloudPublisherDisconnectManager.this.m_quieceTimeout);
				} catch (final Exception e) {
					s_logger.warn("Error disconnecting", e);
				}

				// cleanup
				CloudPublisherDisconnectManager.this.m_timer = null;
				CloudPublisherDisconnectManager.this.m_nextExecutionTime = 0;
			}

		}, new Date(this.m_nextExecutionTime));
	}

	/**
	 * Sets the quiece timeout.
	 *
	 * @param quieceTimeout
	 *            the new quiece timeout
	 */
	public void setQuieceTimeout(final long quieceTimeout) {
		this.m_quieceTimeout = quieceTimeout;
	}

	/**
	 * Stops the timer
	 */
	public synchronized void stop() {
		// cancel existing timer
		if (this.m_timer != null) {
			this.m_timer.cancel();
		}

		s_logger.info("Stopped.");
	}
}
