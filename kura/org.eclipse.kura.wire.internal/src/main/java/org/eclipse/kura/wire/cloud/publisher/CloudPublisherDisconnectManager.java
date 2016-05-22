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

import static org.eclipse.kura.device.internal.Preconditions.checkCondition;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.data.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

/**
 * The Class CloudPublisherDisconnectManager manages the disconnection with
 * Cloud Publisher
 */
public final class CloudPublisherDisconnectManager {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherDisconnectManager.class);

	/** The data service dependency. */
	private final DataService m_dataService;

	/** Schedule Executor Service **/
	private final ScheduledExecutorService m_executorService;

	/** The next execution time. */
	private long m_nextExecutionTime;

	/** The quiece timeout. */
	private long m_quieceTimeout;

	/**
	 * Instantiates a new cloud publisher disconnect manager.
	 *
	 * @param dataService
	 *            the data service
	 * @param quieceTimeout
	 *            the quiece timeout
	 * @throws KuraRuntimeException
	 *             if data service dependency is null
	 */
	public CloudPublisherDisconnectManager(final DataService dataService, final long quieceTimeout) {
		checkCondition(dataService == null, "Data Service cannot be null");

		this.m_dataService = dataService;
		this.m_quieceTimeout = quieceTimeout;
		this.m_nextExecutionTime = 0;
		this.m_executorService = Executors.newScheduledThreadPool(5);
	}

	/**
	 * Disconnect in minutes.
	 *
	 * @param minutes
	 *            the minutes
	 * @throws KuraRuntimeException
	 *             if argument passed is negative
	 */
	public synchronized void disconnectInMinutes(final int minutes) {
		checkCondition(minutes < 0, "Minutes cannot be negative");
		// check if the required timeout is longer than the scheduled one
		final long remainingDelay = this.m_nextExecutionTime - System.currentTimeMillis();
		final long requiredDelay = (long) minutes * 60 * 1000;
		if (requiredDelay > remainingDelay) {
			this.schedule(requiredDelay);
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
	 * Schedule new schedule thread pool executor with the specified delay
	 *
	 * @param delay
	 *            the delay
	 * @throws KuraRuntimeException
	 *             if delay provided is negative
	 */
	private void schedule(final long delay) {
		checkCondition(delay < 0, "Delay cannot be negative");

		// cancel existing timer
		if (this.m_executorService != null) {
			this.m_executorService.shutdown();
		}

		this.m_executorService.schedule(new Runnable() {
			@Override
			public void run() {
				// disconnect
				try {
					m_dataService.disconnect(CloudPublisherDisconnectManager.this.m_quieceTimeout);
				} catch (final Exception e) {
					s_logger.warn("Error while disconnecting cloud publisher..." + Throwables.getRootCause(e));
				}
				// cleaning up
				m_nextExecutionTime = 0;
			}
		}, delay, TimeUnit.MILLISECONDS);

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
	 * Stops the scheduler thread pool
	 */
	public synchronized void stop() {
		s_logger.info("Scheduler stopping in Cloud Publisher Dosconnect Manager....");
		if (this.m_executorService != null) {
			this.m_executorService.shutdown();
		}
		s_logger.info("Scheduler stopping in Cloud Publisher Dosconnect Manager....Done");
	}
}
