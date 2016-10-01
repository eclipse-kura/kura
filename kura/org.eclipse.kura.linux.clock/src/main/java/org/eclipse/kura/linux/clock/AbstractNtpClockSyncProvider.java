/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

    protected Map<String, Object> m_properties;
    protected ClockSyncListener m_listener;

    protected String m_ntpHost;
    protected int m_ntpPort;
    protected int m_ntpTimeout;
    protected int m_retryInterval;
    protected int m_refreshInterval;
    protected Date m_lastSync;
    protected ScheduledExecutorService m_scheduler;
    protected int m_maxRetry;
    protected int m_numRetry;
    protected boolean m_isSynced;
    protected int m_syncCount;

    @Override
    public void init(Map<String, Object> properties, ClockSyncListener listener) throws KuraException {
        this.m_properties = properties;
        this.m_listener = listener;

        readProperties();
    }

    @Override
    public void start() throws KuraException {
        this.m_isSynced = false;
        this.m_numRetry = 0;
        if (this.m_refreshInterval < 0) {
            // Never do any update. So Nothing to do.
            s_logger.info("No clock update required");
            if (this.m_scheduler != null) {
                this.m_scheduler.shutdown();
                this.m_scheduler = null;
            }
        } else if (this.m_refreshInterval == 0) {
            // Perform one clock update - but in a thread.
            s_logger.info("Perform clock update just once");
            if (this.m_scheduler != null) {
                this.m_scheduler.shutdown();
                this.m_scheduler = null;
            }
            this.m_scheduler = Executors.newSingleThreadScheduledExecutor();

            // call recursive retry method for setting the clock
            scheduleOnce();
        } else {
            final int retryInt;
            if (this.m_retryInterval <= 0) {
                retryInt = 1;
            } else {
                retryInt = this.m_retryInterval;
            }
            // Perform periodic clock updates.
            s_logger.info("Perform periodic clock updates every {} sec", this.m_refreshInterval);
            if (this.m_scheduler != null) {
                this.m_scheduler.shutdown();
                this.m_scheduler = null;
            }
            this.m_scheduler = Executors.newSingleThreadScheduledExecutor();
            this.m_scheduler.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("AbstractNtpClockSyncProvider:schedule");
                    if (!AbstractNtpClockSyncProvider.this.m_isSynced) {
                        AbstractNtpClockSyncProvider.this.m_syncCount = 0;
                        try {
                            s_logger.info("Try to sync clock ({})", AbstractNtpClockSyncProvider.this.m_numRetry);
                            if (syncClock()) {
                                s_logger.info("Clock synced");
                                AbstractNtpClockSyncProvider.this.m_isSynced = true;
                                AbstractNtpClockSyncProvider.this.m_numRetry = 0;
                            } else {
                                AbstractNtpClockSyncProvider.this.m_numRetry++;
                                if (AbstractNtpClockSyncProvider.this.m_maxRetry > 0
                                        && AbstractNtpClockSyncProvider.this.m_numRetry >= AbstractNtpClockSyncProvider.this.m_maxRetry) {
                                    s_logger.error(
                                            "Failed to synchronize System Clock. Exhausted retry attempts, giving up");
                                    AbstractNtpClockSyncProvider.this.m_isSynced = true;
                                }
                            }
                        } catch (KuraException e) {
                            AbstractNtpClockSyncProvider.this.m_numRetry++;
                            s_logger.error("Error Synchronizing Clock", e);
                            if (AbstractNtpClockSyncProvider.this.m_maxRetry > 0
                                    && AbstractNtpClockSyncProvider.this.m_numRetry >= AbstractNtpClockSyncProvider.this.m_maxRetry) {
                                s_logger.error(
                                        "Failed to synchronize System Clock. Exhausted retry attempts, giving up");
                                AbstractNtpClockSyncProvider.this.m_isSynced = true;
                            }
                        }
                    } else {
                        AbstractNtpClockSyncProvider.this.m_syncCount++;
                        if (AbstractNtpClockSyncProvider.this.m_syncCount
                                * retryInt >= AbstractNtpClockSyncProvider.this.m_refreshInterval - 1) {
                            AbstractNtpClockSyncProvider.this.m_isSynced = false;
                            AbstractNtpClockSyncProvider.this.m_numRetry = 0;
                        }
                    }
                }
            }, 0, retryInt, TimeUnit.SECONDS);
        }
    }

    private void scheduleOnce() {
        if (this.m_scheduler != null) {
            this.m_scheduler.schedule(new Runnable() {

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
        if (this.m_scheduler != null) {
            this.m_scheduler.shutdown();
            this.m_scheduler = null;
        }
    }

    @Override
    public Date getLastSync() {
        return this.m_lastSync;
    }

    // ----------------------------------------------------------------
    //
    // Private/Protected Methods
    //
    // ----------------------------------------------------------------

    private void readProperties() throws KuraException {
        this.m_ntpHost = (String) this.m_properties.get("clock.ntp.host");
        if (this.m_ntpHost == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, "clock.ntp.host");
        }

        this.m_ntpPort = 123;
        if (this.m_properties.containsKey("clock.ntp.port")) {
            this.m_ntpPort = (Integer) this.m_properties.get("clock.ntp.port");
        }

        this.m_ntpTimeout = 10000;
        if (this.m_properties.containsKey("clock.ntp.timeout")) {
            this.m_ntpTimeout = (Integer) this.m_properties.get("clock.ntp.timeout");
        }

        this.m_retryInterval = 0;
        if (this.m_properties.containsKey("clock.ntp.retry.interval")) {
            this.m_retryInterval = (Integer) this.m_properties.get("clock.ntp.retry.interval");
        }

        this.m_refreshInterval = 0;
        if (this.m_properties.containsKey("clock.ntp.refresh-interval")) {
            this.m_refreshInterval = (Integer) this.m_properties.get("clock.ntp.refresh-interval");
        }

        this.m_maxRetry = 0;
        if (this.m_properties.containsKey("clock.ntp.max-retry")) {
            this.m_maxRetry = (Integer) this.m_properties.get("clock.ntp.max-retry");
        }
    }

    protected abstract boolean syncClock() throws KuraException;
}
