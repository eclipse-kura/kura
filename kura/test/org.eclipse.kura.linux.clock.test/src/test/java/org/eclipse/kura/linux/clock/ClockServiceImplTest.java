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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.clock.ClockEvent;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.junit.Test;
import org.osgi.service.event.EventAdmin;

public class ClockServiceImplTest {
    @Test
    public void testActivateException() {
        // error log output only

        ClockServiceImpl svc = new ClockServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", "true");

        svc.activate(properties);
    }

    @Test
    public void testActivateDeactivate() throws NoSuchFieldException {
        // too few properties, but enough to test activation

        ClockServiceImpl svc = new ClockServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.provider", "java-ntp");
        svc.activate(properties);

        Object provider = TestUtil.getFieldValue(svc, "provider");
        assertNotNull(provider);
        assertTrue(provider instanceof JavaNtpClockSyncProvider);

        svc.deactivate();

        provider = TestUtil.getFieldValue(svc, "provider");
        assertNull(provider);
    }

    @Test
    public void testUpdate() throws NoSuchFieldException {
        // a new provider is created, disabled when enabled = false

        ClockServiceImpl svc = new ClockServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.provider", "java-ntp");
        svc.activate(properties);

        Object provider = TestUtil.getFieldValue(svc, "provider");

        svc.updated(properties);

        assertNotEquals(provider, TestUtil.getFieldValue(svc, "provider"));

        // disable it
        properties.put("enabled", false);

        svc.updated(properties);

        assertNull(TestUtil.getFieldValue(svc, "provider"));
    }

    @Test
    public void testStartProviderMissingParams() throws Throwable {
        // exception due to missing mandatory parameter(s)

        ClockServiceImpl svc = new ClockServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.provider", "java-ntp");
        TestUtil.setFieldValue(svc, "properties", properties);

        try {
            TestUtil.invokePrivate(svc, "startClockSyncProvider");
            fail("Exception expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, e.getCode());
        }
    }

    @Test
    public void testJavaNTPScheduleOnce() throws Throwable {
        // test java NTP provider's scheduleOnce()

        Object lock = new Object();

        ClockServiceImpl svc = new ClockServiceImpl() {

            @Override
            public void onClockUpdate(long offset) {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        };

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.provider", "java-ntp");
        properties.put("clock.ntp.host", "localhost");
        properties.put("clock.ntp.port", 123);
        properties.put("clock.ntp.timeout", 100);
        properties.put("clock.ntp.retry.interval", 1);
        properties.put("clock.ntp.refresh-interval", 0);
        properties.put("clock.ntp.max-retry", 1);
        TestUtil.setFieldValue(svc, "properties", properties);

        TestUtil.invokePrivate(svc, "startClockSyncProvider");

        JavaNtpClockSyncProvider provider = (JavaNtpClockSyncProvider) TestUtil.getFieldValue(svc, "provider");

        synchronized (lock) {
            lock.wait(1500); // wait a bit so that synch runs once; it will wait the full time
        }

        ScheduledExecutorService scheduler = (ScheduledExecutorService) TestUtil.getFieldValue(provider, "scheduler");
        List<Runnable> list = scheduler.shutdownNow();
        assertTrue(list.isEmpty()); // task has not been re-scheduled
    }

    @Test
    public void testClockUpdateBasic() throws Throwable {
        // test the service's onClockUpdate(), without performing the actual updates

        ClockServiceImpl svc = new ClockServiceImpl();

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.set.hwclock", false);
        TestUtil.setFieldValue(svc, "properties", properties);

        svc.onClockUpdate(0); // 0 offset => don't perform sys clock update

        verify(eaMock, times(1)).postEvent(isA(ClockEvent.class));
    }

    @Test
    public void testClockUpdateLinux() throws Throwable {
        // test the service's onClockUpdate(), only run on Linux

        assumeTrue("Only run this test on Linux", System.getProperty("os.name").matches("[Ll]inux"));
        assumeTrue("Only run this test as root", "root".equals(System.getProperty("user.name")));

        ClockServiceImpl svc = new ClockServiceImpl();

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.set.hwclock", true);
        TestUtil.setFieldValue(svc, "properties", properties);

        svc.onClockUpdate(1);

        verify(eaMock, times(1)).postEvent(isA(ClockEvent.class)); // sys clock updated successfully
    }

    @Test
    public void testClockUpdateExceptions() throws Throwable {
        // test the service's onClockUpdate() with clock update exceptions; test of proper logging, mostly

        ClockServiceImpl svc = new ClockServiceImpl() {

            @Override
            protected SafeProcess exec(String command) throws IOException {
                throw new IOException("test");
            }
        };

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.set.hwclock", true);
        TestUtil.setFieldValue(svc, "properties", properties);

        svc.onClockUpdate(1);

        verify(eaMock, times(0)).postEvent(isA(ClockEvent.class)); // sys clock not updated
    }

    @Test
    public void testClockUpdateErrors() throws Throwable {
        // test the service's onClockUpdate() with clock update failures; test of proper logging, mostly

        ClockServiceImpl svc = new ClockServiceImpl() {

            @Override
            protected SafeProcess exec(String command) throws IOException {
                SafeProcess processMock = mock(SafeProcess.class);
                when(processMock.exitValue()).thenReturn(1);

                return processMock;
            }
        };

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.set.hwclock", true);
        TestUtil.setFieldValue(svc, "properties", properties);

        svc.onClockUpdate(1);

        verify(eaMock, times(0)).postEvent(isA(ClockEvent.class)); // sys clock not updated
    }

    @Test
    public void testClockUpdate() throws Throwable {
        // test the service's onClockUpdate()

        ClockServiceImpl svc = new ClockServiceImpl() {

            @Override
            protected SafeProcess exec(String command) throws IOException {
                SafeProcess processMock = mock(SafeProcess.class);
                when(processMock.exitValue()).thenReturn(0);

                return processMock;
            }
        };

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.set.hwclock", true);
        TestUtil.setFieldValue(svc, "properties", properties);

        svc.onClockUpdate(1);

        verify(eaMock, times(1)).postEvent(isA(ClockEvent.class)); // sys clock updated successfully
    }
}
