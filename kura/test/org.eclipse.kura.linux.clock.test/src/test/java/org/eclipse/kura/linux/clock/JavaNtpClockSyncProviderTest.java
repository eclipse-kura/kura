/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.linux.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;

public class JavaNtpClockSyncProviderTest {

    private static final long WAIT_CLOCK_SYNC_MS = 1000;
    private static final long SYNC_TIMEOUT_MS = 100;
    private static final long SLACK_WAIT_TIME_MS = 10;

    @Test
    public void testStartFail() throws Throwable {
        // test running updates through java NTP - first run doesn't sync

        Object lock = new Object();

        JavaNtpClockSyncProvider provider = new JavaNtpClockSyncProvider() {

            @Override
            protected boolean syncClock() throws KuraException {
                synchronized (lock) {
                    lock.notifyAll();
                }

                return false;
            }
        };

        initProviderWithProperties(provider);
        provider.start();

        synchronized (lock) {
            lock.wait(WAIT_CLOCK_SYNC_MS); // wait a bit so that synch runs once
        }

        waitProviderToSync(provider);

        assertTrue((boolean) TestUtil.getFieldValue(provider, "isSynced"));

        // now wait for the second run
        Thread.sleep(WAIT_CLOCK_SYNC_MS);

        assertEquals(1, TestUtil.getFieldValue(provider, "syncCount"));
        assertEquals(0, TestUtil.getFieldValue(provider, "numRetry"));
        assertFalse((boolean) TestUtil.getFieldValue(provider, "isSynced"));
    }

    private void initProviderWithProperties(JavaNtpClockSyncProvider provider) throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.provider", "java-ntp");
        properties.put("clock.ntp.host", "localhost");
        properties.put("clock.ntp.port", 123);
        properties.put("clock.ntp.timeout", 100);
        properties.put("clock.ntp.retry.interval", 1);
        properties.put("clock.ntp.refresh-interval", 1);
        properties.put("clock.ntp.max-retry", 1);

        ClockServiceConfig clockServiceConfig = new ClockServiceConfig(properties);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        provider.init(clockServiceConfig, scheduler, null);
    }

    @Test
    public void testStart() throws Throwable {
        // test running updates through java NTP - synch immediately

        Object lock = new Object();

        JavaNtpClockSyncProvider provider = new JavaNtpClockSyncProvider() {

            @Override
            protected boolean syncClock() throws KuraException {
                synchronized (lock) {
                    lock.notifyAll();
                }

                return true;
            }
        };

        initProviderWithProperties(provider);
        provider.start();

        synchronized (lock) {
            lock.wait(WAIT_CLOCK_SYNC_MS); // wait a bit so that synch runs once
        }

        waitProviderToSync(provider);

        assertTrue((boolean) TestUtil.getFieldValue(provider, "isSynced"));
        assertEquals(0, TestUtil.getFieldValue(provider, "syncCount"));
        assertEquals(0, TestUtil.getFieldValue(provider, "numRetry"));
    }

    @Test
    public void testStartSynchException() throws Throwable {
        // test running updates through java NTP - exception during synch

        Object lock = new Object();

        JavaNtpClockSyncProvider provider = new JavaNtpClockSyncProvider() {

            @Override
            protected boolean syncClock() throws KuraException {
                synchronized (lock) {
                    lock.notifyAll();
                }

                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "test");
            }
        };

        initProviderWithProperties(provider);
        provider.start();

        synchronized (lock) {
            lock.wait(WAIT_CLOCK_SYNC_MS); // wait a bit so that synch runs once
        }

        waitProviderToSync(provider);

        assertEquals(0, TestUtil.getFieldValue(provider, "syncCount"));
        assertEquals(1, TestUtil.getFieldValue(provider, "numRetry"));
        assertTrue((boolean) TestUtil.getFieldValue(provider, "isSynced"));
    }

    private void waitProviderToSync(JavaNtpClockSyncProvider provider) throws InterruptedException {
        long elapsedTime = 0;
        while (!provider.isSynced && SYNC_TIMEOUT_MS > elapsedTime) {
            Thread.sleep(SLACK_WAIT_TIME_MS);
            elapsedTime += SLACK_WAIT_TIME_MS;
        }
    }
}
