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
package org.eclipse.kura.internal.wire.publisher;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CloudPublisherDisconnectManager manages the disconnection with
 * Cloud Publisher
 */
final class CloudPublisherDisconnectManager {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherDisconnectManager.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The data service dependency. */
	private final DataService m_dataService;

	/** Schedule Executor Service **/
	private ScheduledExecutorService m_executorService;

	/** The next execution time. */
	private long m_nextExecutionTime;

	/** The quiesce timeout. */
	private long m_quiesceTimeout;

	/** The future handle of the thread pool executor service. */
	private ScheduledFuture<?> m_tickHandle;

	/**
	 * Instantiates a new cloud publisher disconnect manager.
	 *
	 * @param dataService
	 *            the data service
	 * @param quiesceTimeout
	 *            the quiesce timeout
	 * @throws KuraRuntimeException
	 *             if data service dependency is null
	 */
	CloudPublisherDisconnectManager(final DataService dataService, final long quiesceTimeout) {
		checkNull(dataService, s_message.dataServiceNonNull());
		this.m_dataService = dataService;
		this.m_quiesceTimeout = quiesceTimeout;
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
	synchronized void disconnectInMinutes(final int minutes) {
		checkCondition(minutes < 0, s_message.minutesNonNegative());
		// check if the required timeout is longer than the scheduled one
		final long remainingDelay = this.m_nextExecutionTime - System.currentTimeMillis();
		final long requiredDelay = (long) minutes * 60 * 1000;
		if (requiredDelay > remainingDelay) {
			this.schedule(requiredDelay);
		}
	}

	/**
	 * Gets the quiesce timeout.
	 *
	 * @return the quiesce timeout
	 */
	long getQuiesceTimeout() {
		return this.m_quiesceTimeout;
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
		checkCondition(delay < 0, s_message.delayNonNegative());
		// cancel existing timer
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		this.m_tickHandle = this.m_executorService.scheduleAtFixedRate(new Runnable() {
			/** {@inheritDoc} */
			@Override
			public void run() {
				try {
					m_dataService.disconnect(m_quiesceTimeout);
				} catch (final Exception exception) {
					s_logger.error(
							s_message.errorDisconnectingCloudPublisher() + ThrowableUtil.stackTraceAsString(exception));
				}
				// cleaning up
				m_nextExecutionTime = 0;
			}
		}, 0, delay, TimeUnit.MILLISECONDS);

	}

	/**
	 * Sets the quiesce timeout.
	 *
	 * @param quiesceTimeout
	 *            the new quiesce timeout
	 */
	void setQuiesceTimeout(final long quiesceTimeout) {
		this.m_quiesceTimeout = quiesceTimeout;
	}

	/**
	 * Stops the scheduler thread pool
	 */
	synchronized void stop() {
		s_logger.info(s_message.schedulerStopping());
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		if (this.m_executorService != null) {
			this.m_executorService.shutdown();
		}
		this.m_executorService = null;
		s_logger.info(s_message.schedulerStopped());
	}
}
