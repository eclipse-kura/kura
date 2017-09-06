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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.position.PositionService;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class GpsClockSyncProviderTest {

    @Test
    public void testHandleEvent() throws NoSuchFieldException {
        // test handleEvent()

        AtomicBoolean visited = new AtomicBoolean(false);

        GpsClockSyncProvider provider = new GpsClockSyncProvider() {

            @Override
            protected void synchClock() throws KuraException {
                visited.set(true);
            }
        };

        Map<String, ?> properties = new HashMap<>();
        provider.handleEvent(new Event("topic", properties));

        assertFalse(visited.get());

        // improper event - test for handleEvent issue #1639
        TestUtil.setFieldValue(provider, "waitForLocked", true);

        provider.handleEvent(new Event("locked", properties));

        assertFalse(visited.get());

        // proper event
        provider.handleEvent(new Event("org/eclipse/kura/position/locked", properties));

        assertTrue(visited.get());

        // proper event, wrong other conditions - test for handleEvent issue #1639
        visited.set(false);
        TestUtil.setFieldValue(provider, "waitForLocked", false);

        provider.handleEvent(new Event("org/eclipse/kura/position/locked", properties));

        assertFalse(visited.get());
    }

    @Test
    public void testHandleEventException() throws NoSuchFieldException {
        // verify that the exception is handled

        AtomicBoolean visited = new AtomicBoolean(false);

        GpsClockSyncProvider provider = new GpsClockSyncProvider() {

            @Override
            protected void synchClock() throws KuraException {
                visited.set(true);

                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "test");
            }
        };

        Map<String, ?> properties = new HashMap<>();

        TestUtil.setFieldValue(provider, "waitForLocked", true);

        // no exception is to be thrown
        provider.handleEvent(new Event("org/eclipse/kura/position/locked", properties));

        assertTrue(visited.get());
    }

    @Test
    public void testInitExc() throws KuraException {
        // test that exception is wrapped in a KuraException

        GpsClockSyncProvider provider = new GpsClockSyncProvider() {

            @Override
            protected BundleContext getBundleContext() {
                throw new RuntimeException("test");
            }
        };

        Map<String, Object> properties = new HashMap<>();
        properties.put("clock.ntp.refresh-interval", 1);
        ClockSyncListener listener = mock(ClockSyncListener.class);

        try {
            provider.init(properties, listener);
            fail("Exception expected");
        } catch (KuraException e) {
            assertEquals("test", e.getCause().getMessage());
        }
    }

    @Test
    public void testInit() throws KuraException {
        // successful init

        BundleContext bcMock = mock(BundleContext.class);

        ServiceReference<PositionService> psrMock = mock(ServiceReference.class);
        when(bcMock.getServiceReference(PositionService.class)).thenReturn(psrMock);

        PositionService psMock = mock(PositionService.class);
        when(bcMock.getService(psrMock)).thenReturn(psMock);

        GpsClockSyncProvider provider = new GpsClockSyncProvider() {

            @Override
            protected BundleContext getBundleContext() {
                return bcMock;
            }
        };

        Map<String, Object> properties = new HashMap<>();
        properties.put("clock.ntp.refresh-interval", 1);
        ClockSyncListener listener = mock(ClockSyncListener.class);

        provider.init(properties, listener);

        verify(bcMock, times(1)).registerService(eq(EventHandler.class.getName()), eq(provider), anyObject());
    }

    @Test
    public void testStartNoUpdate() throws KuraException, NoSuchFieldException {
        // start service without actually doing anything

        AtomicBoolean visited = new AtomicBoolean(false);

        GpsClockSyncProvider provider = new GpsClockSyncProvider() {

            @Override
            protected void synchClock() throws KuraException {
                visited.set(true);
            }
        };

        TestUtil.setFieldValue(provider, "refreshInterval", -1);

        provider.start();

        assertFalse(visited.get());
        assertNull(TestUtil.getFieldValue(provider, "scheduler"));
    }

    @Test
    public void testStartSingleUpdate() throws KuraException, NoSuchFieldException {
        // start service and perform a single update

        AtomicInteger visited = new AtomicInteger(0);

        GpsClockSyncProvider provider = new GpsClockSyncProvider() {

            @Override
            protected void synchClock() throws KuraException {
                visited.incrementAndGet();
            }
        };

        TestUtil.setFieldValue(provider, "refreshInterval", 0);

        provider.start();

        assertEquals(1, visited.get());
        assertNull(TestUtil.getFieldValue(provider, "scheduler"));
    }

    @Test(timeout = 2400)
    public void testStartScheduledUpdates() throws KuraException, NoSuchFieldException, InterruptedException {
        // start the service and schedule multiple updates

        AtomicInteger visited = new AtomicInteger(0);
        Object lock = new Object();

        GpsClockSyncProvider provider = new GpsClockSyncProvider() {

            @Override
            protected void synchClock() throws KuraException {
                int count = visited.incrementAndGet();
                if (count == 2) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }
        };

        TestUtil.setFieldValue(provider, "refreshInterval", 1);

        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        TestUtil.setFieldValue(provider, "scheduler", scheduler);

        provider.start();

        synchronized (lock) {
            lock.wait(2500);
        }

        verify(scheduler, times(1)).shutdown();

        assertEquals(2, visited.get()); // 2 visits in this time
        assertNotEquals(scheduler, TestUtil.getFieldValue(provider, "scheduler")); // scheduler was replaced
    }

    @Test
    public void testStop() throws NoSuchFieldException, KuraException {
        // stop the service

        GpsClockSyncProvider provider = new GpsClockSyncProvider();

        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        TestUtil.setFieldValue(provider, "scheduler", scheduler);

        PositionService psMock = mock(PositionService.class);
        TestUtil.setFieldValue(provider, "positionService", psMock);

        provider.stop();

        verify(scheduler, times(1)).shutdown();
        assertNull(TestUtil.getFieldValue(provider, "scheduler"));
    }

    @Test
    public void testSynchNotLocked() throws KuraException, NoSuchFieldException {
        // position service does not have a locked position

        GpsClockSyncProvider provider = new GpsClockSyncProvider();

        PositionService psMock = mock(PositionService.class);
        TestUtil.setFieldValue(provider, "positionService", psMock);

        when(psMock.isLocked()).thenReturn(false);

        provider.synchClock();

        assertTrue((boolean) TestUtil.getFieldValue(provider, "waitForLocked"));
        assertNull(TestUtil.getFieldValue(provider, "lastSync"));
    }

    @Test
    public void testSynchNoGPSDate() throws KuraException, NoSuchFieldException {
        // position service is locked on, but date is invalid

        GpsClockSyncProvider provider = new GpsClockSyncProvider();

        PositionService psMock = mock(PositionService.class);
        TestUtil.setFieldValue(provider, "positionService", psMock);

        when(psMock.isLocked()).thenReturn(true);

        when(psMock.getNmeaDate()).thenReturn("");

        provider.synchClock();

        assertFalse((boolean) TestUtil.getFieldValue(provider, "waitForLocked"));
        assertNull(TestUtil.getFieldValue(provider, "lastSync"));
    }

    @Test
    public void testSynchDate() throws KuraException, NoSuchFieldException {
        // position service is locked on, date is OK

        String date = "050917";
        String date2 = "170905";
        String time = "123456";
        String time2 = "12:34:56";

        AtomicInteger visitCount = new AtomicInteger(0);
        GpsClockSyncProvider provider = new GpsClockSyncProvider() {

            @Override
            protected SafeProcess exec(String command) throws IOException {
                assertTrue(String.format("date +%%Y%%m%%d -s \"20%s\"", date2).equals(command)
                        || String.format("date +%%T -u -s \"%s\"", time2).equals(command));

                int count = visitCount.getAndIncrement();

                SafeProcess processMock = mock(SafeProcess.class);
                when(processMock.exitValue()).thenReturn(count);

                return processMock;
            }
        };

        PositionService psMock = mock(PositionService.class);
        TestUtil.setFieldValue(provider, "positionService", psMock);

        when(psMock.isLocked()).thenReturn(true);

        when(psMock.getNmeaDate()).thenReturn(date);
        when(psMock.getNmeaTime()).thenReturn(time);

        AtomicBoolean visited = new AtomicBoolean(false);
        ClockSyncListener listener = new ClockSyncListener() {

            @Override
            public void onClockUpdate(long offset) {
                visited.set(true);
            }
        };
        TestUtil.setFieldValue(provider, "listener", listener);

        provider.synchClock();

        assertFalse((boolean) TestUtil.getFieldValue(provider, "waitForLocked"));
        assertNotNull(TestUtil.getFieldValue(provider, "lastSync"));

        assertTrue(visited.get());
        assertEquals(2, visitCount.get());
    }

    @Test
    public void testSynchDateTime() throws KuraException, NoSuchFieldException {
        // position service is locked on, date is OK

        String date = "050917";
        String date2 = "170905";
        String time = "123456";
        String time2 = "12:34:56";

        SafeProcess processMock = mock(SafeProcess.class);
        when(processMock.exitValue()).thenReturn(0);

        AtomicInteger visitCount = new AtomicInteger(0);
        GpsClockSyncProvider provider = new GpsClockSyncProvider() {

            @Override
            protected SafeProcess exec(String command) throws IOException {
                assertTrue(String.format("date +%%Y%%m%%d -s \"20%s\"", date2).equals(command)
                        || String.format("date +%%T -u -s \"%s\"", time2).equals(command));

                visitCount.incrementAndGet();

                return processMock;
            }
        };

        PositionService psMock = mock(PositionService.class);
        TestUtil.setFieldValue(provider, "positionService", psMock);

        when(psMock.isLocked()).thenReturn(true);

        when(psMock.getNmeaDate()).thenReturn(date);
        when(psMock.getNmeaTime()).thenReturn(time);

        AtomicBoolean visited = new AtomicBoolean(false);
        ClockSyncListener listener = new ClockSyncListener() {

            @Override
            public void onClockUpdate(long offset) {
                visited.set(true);
            }
        };
        TestUtil.setFieldValue(provider, "listener", listener);

        provider.synchClock();

        assertFalse((boolean) TestUtil.getFieldValue(provider, "waitForLocked"));
        assertNotNull(TestUtil.getFieldValue(provider, "lastSync"));

        assertTrue(visited.get());
        assertEquals(2, visitCount.get());

        verify(processMock, times(2)).destroy();
    }
}
