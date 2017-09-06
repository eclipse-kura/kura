/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;


public class JavaNtpClockSyncProviderTest {

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
            lock.wait(500); // wait a bit so that synch runs once
        }

        Thread.sleep(20); // wait just a bit longer

        assertTrue((boolean) TestUtil.getFieldValue(provider, "isSynced"));

        // now wait for the second run
        Thread.sleep(1100);

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

        provider.init(properties, null);
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
            lock.wait(500); // wait a bit so that synch runs once
        }

        Thread.sleep(20); // wait just a bit longer

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
            lock.wait(500); // wait a bit so that synch runs once
        }

        Thread.sleep(100); // wait just a bit longer, so that the exception propagates

        assertEquals(0, TestUtil.getFieldValue(provider, "syncCount"));
        assertEquals(1, TestUtil.getFieldValue(provider, "numRetry"));
        assertTrue((boolean) TestUtil.getFieldValue(provider, "isSynced"));
    }
}
