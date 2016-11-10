/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.internal.wire.publisher;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
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
    private final DataService dataService;

    /** Schedule Executor Service **/
    private ScheduledExecutorService executorService;

    /** The quiesce timeout. */
    private long quiesceTimeout;

    /** The future handle of the thread pool executor service. */
    private ScheduledFuture<?> tickHandle;

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
        this.dataService = dataService;
        this.quiesceTimeout = quiesceTimeout;
        this.executorService = Executors.newScheduledThreadPool(5);
    }

    /**
     * Disconnect in minutes.
     *
     * @param minutes
     *            the minutes
     * @param isForceUpdate
     *            checks if the scheduling needs to be updated forcefully
     * @throws KuraRuntimeException
     *             if minutes argument is negative
     */
    synchronized void disconnectInMinutes(final int minutes, final boolean isForceUpdate) {
        checkCondition(minutes < 0, s_message.minutesNonNegative());
        final long requiredDelay = (long) minutes * 60 * 1000;
        this.schedule(requiredDelay, isForceUpdate);
    }

    /**
     * Gets the quiesce timeout.
     *
     * @return the quiesce timeout
     */
    long getQuiesceTimeout() {
        return this.quiesceTimeout;
    }

    /**
     * Schedule new schedule thread pool executor with the specified delay
     *
     * @param delay
     *            the delay
     * @param isForceUpdate
     *            checks if the scheduling needs to be updated forcefully
     * @throws KuraRuntimeException
     *             if delay provided is negative
     */
    private void schedule(final long delay, final boolean isForceUpdate) {
        checkCondition(delay < 0, s_message.delayNonNegative());
        // cancel existing timer
        if (this.tickHandle != null) {
            // if it is a force update then cancel existing scheduler
            if (isForceUpdate) {
                this.tickHandle.cancel(true);
            } else {
                return;
            }
        }
        this.tickHandle = this.executorService.scheduleAtFixedRate(new Runnable() {

            /** {@inheritDoc} */
            @Override
            public void run() {
                try {
                    CloudPublisherDisconnectManager.this.dataService
                            .disconnect(CloudPublisherDisconnectManager.this.quiesceTimeout);
                } catch (final Exception exception) {
                    s_logger.error(
                            s_message.errorDisconnectingCloudPublisher() + ThrowableUtil.stackTraceAsString(exception));
                }
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
        this.quiesceTimeout = quiesceTimeout;
    }

    /**
     * Stops the scheduler thread pool
     */
    synchronized void stop() {
        s_logger.info(s_message.schedulerStopping());
        if (this.tickHandle != null) {
            this.tickHandle.cancel(true);
        }
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
        this.executorService = null;
        s_logger.info(s_message.schedulerStopped());
    }
}
