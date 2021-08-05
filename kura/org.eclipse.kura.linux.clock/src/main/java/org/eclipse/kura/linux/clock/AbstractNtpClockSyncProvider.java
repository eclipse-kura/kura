/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.clock;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNtpClockSyncProvider implements ClockSyncProvider {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNtpClockSyncProvider.class);

    protected ClockSyncListener listener;

    protected String ntpHost;
    protected int ntpPort;
    protected int ntpTimeout;
    protected int retryInterval;
    protected int refreshInterval;
    protected Date lastSync;
    protected ScheduledExecutorService scheduler;
    protected int maxRetry;
    protected int numRetry;
    protected boolean isSynced;
    protected int syncCount;

    @Override
    public void init(ClockServiceConfig clockServiceConfig, ClockSyncListener listener) throws KuraException {
        this.listener = listener;
        this.ntpHost = clockServiceConfig.getNtpHost();
        this.ntpPort = clockServiceConfig.getNtpPort();
        this.ntpTimeout = clockServiceConfig.getNtpTimeout();
        this.retryInterval = clockServiceConfig.getNtpRetryInterval();
        this.refreshInterval = clockServiceConfig.getNtpRefreshInterval();
        this.maxRetry = clockServiceConfig.getNtpMaxRetries();
    }

    @Override
    public void start() throws KuraException {
        this.isSynced = false;
        this.numRetry = 0;
        if (this.refreshInterval < 0) {
            // Never do any update. So Nothing to do.
            logger.info("No clock update required");
            if (this.scheduler != null) {
                this.scheduler.shutdown();
                this.scheduler = null;
            }
        } else if (this.refreshInterval == 0) {
            // Perform one clock update - but in a thread.
            logger.info("Perform clock update just once");
            if (this.scheduler != null) {
                this.scheduler.shutdown();
                this.scheduler = null;
            }
            this.scheduler = Executors.newSingleThreadScheduledExecutor();

            // call recursive retry method for setting the clock
            scheduleOnce();
        } else {
            final int retryInt;
            if (this.retryInterval <= 0) {
                retryInt = 1;
            } else {
                retryInt = this.retryInterval;
            }
            // Perform periodic clock updates.
            logger.info("Perform periodic clock updates every {} sec", this.refreshInterval);
            if (this.scheduler != null) {
                this.scheduler.shutdown();
                this.scheduler = null;
            }
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.scheduler.scheduleAtFixedRate(() -> {
                Thread.currentThread().setName("AbstractNtpClockSyncProvider:schedule");
                if (!AbstractNtpClockSyncProvider.this.isSynced) {
                    AbstractNtpClockSyncProvider.this.syncCount = 0;
                    try {
                        logger.info("Try to sync clock ({})", AbstractNtpClockSyncProvider.this.numRetry);
                        if (syncClock()) {
                            logger.info("Clock synced");
                            AbstractNtpClockSyncProvider.this.isSynced = true;
                            AbstractNtpClockSyncProvider.this.numRetry = 0;
                        } else {
                            AbstractNtpClockSyncProvider.this.numRetry++;
                            if (AbstractNtpClockSyncProvider.this.maxRetry > 0
                                    && AbstractNtpClockSyncProvider.this.numRetry >= AbstractNtpClockSyncProvider.this.maxRetry) {
                                logger.error(
                                        "Failed to synchronize System Clock. Exhausted retry attempts, giving up");
                                AbstractNtpClockSyncProvider.this.isSynced = true;
                            }
                        }
                    } catch (KuraException e) {
                        AbstractNtpClockSyncProvider.this.numRetry++;
                        logger.error("Error Synchronizing Clock", e);
                        if (AbstractNtpClockSyncProvider.this.maxRetry > 0
                                && AbstractNtpClockSyncProvider.this.numRetry >= AbstractNtpClockSyncProvider.this.maxRetry) {
                            logger.error("Failed to synchronize System Clock. Exhausted retry attempts, giving up");
                            AbstractNtpClockSyncProvider.this.isSynced = true;
                        }
                    }
                } else {
                    AbstractNtpClockSyncProvider.this.syncCount++;
                    if (AbstractNtpClockSyncProvider.this.syncCount
                            * retryInt >= AbstractNtpClockSyncProvider.this.refreshInterval - 1) {
                        AbstractNtpClockSyncProvider.this.isSynced = false;
                        AbstractNtpClockSyncProvider.this.numRetry = 0;
                    }
                }
            }, 0, retryInt, TimeUnit.SECONDS);
        }
    }

    private void scheduleOnce() {
        if (this.scheduler != null) {
            this.scheduler.schedule(() -> {
                Thread.currentThread().setName("AbstractNtpClockSyncProvider:scheduleOnce");
                try {
                    syncClock();
                } catch (KuraException e) {
                    logger.error("Error Synchronizing Clock - retrying", e);
                    scheduleOnce();
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop() throws KuraException {
        if (this.scheduler != null) {
            this.scheduler.shutdown();
            this.scheduler = null;
        }
    }

    @Override
    public Date getLastSync() {
        return this.lastSync;
    }

    // ----------------------------------------------------------------
    //
    // Private/Protected Methods
    //
    // ----------------------------------------------------------------
    protected abstract boolean syncClock() throws KuraException;
}
