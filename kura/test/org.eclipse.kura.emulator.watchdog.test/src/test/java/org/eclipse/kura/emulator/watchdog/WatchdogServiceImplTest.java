/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.emulator.watchdog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.watchdog.CriticalComponent;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class WatchdogServiceImplTest {

    @Test
    public void testActivateDeactivate() throws NoSuchFieldException {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        TestUtil.setFieldValue(svc, "executor", executor);

        ComponentContext ccMock = mock(ComponentContext.class);
        Map<String, Object> properties = new HashMap<>();

        svc.activate(ccMock, properties);

        assertTrue(executor.isTerminated());

        ScheduledExecutorService ex = (ScheduledExecutorService) TestUtil.getFieldValue(svc, "executor");
        assertNotEquals(executor, ex);
        assertFalse(ex.isShutdown());

        try {
            Thread.sleep(10); // wait for the executor to start the thread
        } catch (InterruptedException e) {
        }

        svc.deactivate(ccMock);

        assertTrue(ex.isTerminated());

        assertNull(TestUtil.getFieldValue(svc, "executor"));
    }

    @Test
    public void testUpdate() throws NoSuchFieldException {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        ScheduledExecutorService executorMock = Executors.newSingleThreadScheduledExecutor();
        TestUtil.setFieldValue(svc, "executor", executorMock);

        ScheduledFuture futureMock = mock(ScheduledFuture.class);
        TestUtil.setFieldValue(svc, "future", futureMock);

        when(futureMock.isDone()).thenReturn(false).thenReturn(true);

        Map<String, Object> properties = new HashMap<>();
        properties.put("pingInterval", 5000);
        properties.put("enabled", true);

        svc.updated(properties);

        try {
            Thread.sleep(10); // wait for the executor to start the thread
        } catch (InterruptedException e) {
        }

        assertFalse(executorMock.isTerminated());
        assertTrue((boolean) TestUtil.getFieldValue(svc, "configEnabled"));
        assertEquals(5000, TestUtil.getFieldValue(svc, "pingInterval"));
    }

    @Test
    public void testRegistrationUnregistration() throws NoSuchFieldException {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        List<CriticalServiceImpl> services = new ArrayList<>();
        TestUtil.setFieldValue(svc, "criticalServiceList", services);

        int timeout = 1500;
        String name = "ccName";

        CriticalComponent criticalComponent = mock(CriticalComponent.class);
        when(criticalComponent.getCriticalComponentTimeout()).thenReturn(timeout);
        when(criticalComponent.getCriticalComponentName()).thenReturn(name);

        svc.registerCriticalComponent(criticalComponent);

        assertEquals(1, services.size());
        assertEquals(timeout, services.get(0).getTimeout());

        // test that it won't be added the second time
        int timeout2 = 400;
        CriticalComponent criticalComponent2 = mock(CriticalComponent.class);
        when(criticalComponent2.getCriticalComponentTimeout()).thenReturn(timeout2);
        when(criticalComponent2.getCriticalComponentName()).thenReturn(name);

        svc.registerCriticalComponent(criticalComponent2);

        assertEquals(1, services.size());
        assertEquals(timeout, services.get(0).getTimeout()); // still the same timeout

        // remove the first one
        svc.unregisterCriticalComponent(criticalComponent2);

        assertTrue(services.isEmpty());
    }

    @Test
    public void testCheckin() throws NoSuchFieldException, InterruptedException {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        List<CriticalServiceImpl> services = new ArrayList<>();
        TestUtil.setFieldValue(svc, "criticalServiceList", services);

        int timeout = 1500;
        String name = "ccName";

        CriticalComponent criticalComponent = mock(CriticalComponent.class);
        when(criticalComponent.getCriticalComponentTimeout()).thenReturn(timeout);
        when(criticalComponent.getCriticalComponentName()).thenReturn(name);

        svc.registerCriticalComponent(criticalComponent);

        long updated = (long) TestUtil.getFieldValue(services.get(0), "updated");

        Thread.sleep(2);

        svc.checkin(criticalComponent);

        long updated2 = (long) TestUtil.getFieldValue(services.get(0), "updated");

        assertTrue(updated < updated2);
    }

    @Test
    public void testWatchdogLoopReboot() throws Throwable {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        List<CriticalServiceImpl> services = new ArrayList<>();
        TestUtil.setFieldValue(svc, "criticalServiceList", services);

        TestUtil.setFieldValue(svc, "enabled", true);

        int timeout = 1500;
        String name = "ccTestName";

        CriticalComponent criticalComponent = mock(CriticalComponent.class);
        when(criticalComponent.getCriticalComponentTimeout()).thenReturn(timeout);
        when(criticalComponent.getCriticalComponentName()).thenReturn(name);

        svc.registerCriticalComponent(criticalComponent);

        TestUtil.invokePrivate(svc, "doWatchdogLoop");
    }

    @Test
    public void testWatchdogLoop() throws Throwable {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        List<CriticalServiceImpl> services = new ArrayList<>();
        TestUtil.setFieldValue(svc, "criticalServiceList", services);

        TestUtil.setFieldValue(svc, "enabled", true);

        int timeout = 1;
        String name = "ccName";

        CriticalComponent criticalComponent = mock(CriticalComponent.class);
        when(criticalComponent.getCriticalComponentTimeout()).thenReturn(timeout);
        when(criticalComponent.getCriticalComponentName()).thenReturn(name);

        svc.registerCriticalComponent(criticalComponent);

        Thread.sleep(2);

        TestUtil.invokePrivate(svc, "doWatchdogLoop");
    }

    @Test
    public void testEnableDisable() throws NoSuchFieldException {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        svc.setConfigEnabled(true);
        assertTrue(svc.isConfigEnabled());

        svc.setConfigEnabled(false);
        assertFalse(svc.isConfigEnabled());

        svc.startWatchdog();
        assertTrue((boolean) TestUtil.getFieldValue(svc, "enabled"));

        svc.stopWatchdog();
        assertFalse((boolean) TestUtil.getFieldValue(svc, "enabled"));
    }

}
