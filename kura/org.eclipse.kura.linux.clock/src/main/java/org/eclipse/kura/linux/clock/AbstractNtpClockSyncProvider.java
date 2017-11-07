/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.clock;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNtpClockSyncProvider implements ClockSyncProvider {

    private static final Logger s_logger = LoggerFactory.getLogger(AbstractNtpClockSyncProvider.class);

    protected Map<String, Object> properties;
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
    public void init(Map<String, Object> properties, ClockSyncListener listener) throws KuraException {
        this.properties = properties;
        this.listener = listener;

        readProperties();
    }

    @Override
    public void start() throws KuraException {
        this.isSynced = false;
        this.numRetry = 0;
        if (this.refreshInterval < 0) {
            // Never do any update. So Nothing to do.
            s_logger.info("No clock update required");
            if (this.scheduler != null) {
                this.scheduler.shutdown();
                this.scheduler = null;
            }
        } else if (this.refreshInterval == 0) {
            // Perform one clock update - but in a thread.
            s_logger.info("Perform clock update just once");
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
            s_logger.info("Perform periodic clock updates every {} sec", this.refreshInterval);
            if (this.scheduler != null) {
                this.scheduler.shutdown();
                this.scheduler = null;
            }
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.scheduler.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("AbstractNtpClockSyncProvider:schedule");
                    if (!AbstractNtpClockSyncProvider.this.isSynced) {
                        AbstractNtpClockSyncProvider.this.syncCount = 0;
                        try {
                            s_logger.info("Try to sync clock ({})", AbstractNtpClockSyncProvider.this.numRetry);
                            if (syncClock()) {
                                s_logger.info("Clock synced");
                                AbstractNtpClockSyncProvider.this.isSynced = true;
                                AbstractNtpClockSyncProvider.this.numRetry = 0;
                            } else {
                                AbstractNtpClockSyncProvider.this.numRetry++;
                                if (AbstractNtpClockSyncProvider.this.maxRetry > 0
                                        && AbstractNtpClockSyncProvider.this.numRetry >= AbstractNtpClockSyncProvider.this.maxRetry) {
                                    s_logger.error(
                                            "Failed to synchronize System Clock. Exhausted retry attempts, giving up");
                                    AbstractNtpClockSyncProvider.this.isSynced = true;
                                }
                            }
                        } catch (KuraException e) {
                            AbstractNtpClockSyncProvider.this.numRetry++;
                            s_logger.error("Error Synchronizing Clock", e);
                            if (AbstractNtpClockSyncProvider.this.maxRetry > 0
                                    && AbstractNtpClockSyncProvider.this.numRetry >= AbstractNtpClockSyncProvider.this.maxRetry) {
                                s_logger.error(
                                        "Failed to synchronize System Clock. Exhausted retry attempts, giving up");
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
                }
            }, 0, retryInt, TimeUnit.SECONDS);
        }
    }

    private void scheduleOnce() {
        if (this.scheduler != null) {
            this.scheduler.schedule(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("AbstractNtpClockSyncProvider:scheduleOnce");
                    try {
                        syncClock();
                    } catch (KuraException e) {
                        s_logger.error("Error Synchronizing Clock - retrying", e);
                        scheduleOnce();
                    }
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

    private void readProperties() throws KuraException {
        this.ntpHost = (String) this.properties.get("clock.ntp.host");
        if (this.ntpHost == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, "clock.ntp.host");
        }

        this.ntpPort = 123;
        if (this.properties.containsKey("clock.ntp.port")) {
            this.ntpPort = (Integer) this.properties.get("clock.ntp.port");
        }

        this.ntpTimeout = 10000;
        if (this.properties.containsKey("clock.ntp.timeout")) {
            this.ntpTimeout = (Integer) this.properties.get("clock.ntp.timeout");
        }

        this.retryInterval = 0;
        if (this.properties.containsKey("clock.ntp.retry.interval")) {
            this.retryInterval = (Integer) this.properties.get("clock.ntp.retry.interval");
        }

        this.refreshInterval = 0;
        if (this.properties.containsKey("clock.ntp.refresh-interval")) {
            this.refreshInterval = (Integer) this.properties.get("clock.ntp.refresh-interval");
        }

        this.maxRetry = 0;
        if (this.properties.containsKey("clock.ntp.max-retry")) {
            this.maxRetry = (Integer) this.properties.get("clock.ntp.max-retry");
        }
    }

    protected abstract boolean syncClock() throws KuraException;
}
