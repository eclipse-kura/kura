/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.watchdog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.watchdog.CriticalComponent;
import org.junit.Test;

public class WatchdogServiceImplTest {

    private static final String WATCHDOG_TEST_DEVICE = "target/watchdogTestDevice";

    private class WatchdogTestWriter extends StringWriter {

        private int lastLength = 0;

        @Override
        public synchronized void write(String str) {
            super.write(str);
            notify();
        }

        public synchronized boolean waitForData(int delayMs) { // waits for some data to be written since the last call
                                                               // to this method
            if (toString().length() == this.lastLength) {
                try {
                    this.wait(delayMs);
                } catch (InterruptedException e) {
                }
            }
            final int currentLength = toString().length();
            final boolean result = currentLength != this.lastLength;
            this.lastLength = currentLength;
            return result;
        }
    }

    public class TestWatchdogServiceImpl extends WatchdogServiceImpl {

        private final Writer testWriter;
        private volatile boolean hasCheckedCriticalComponents;

        public TestWatchdogServiceImpl(Writer fileWriter) {
            this.testWriter = fileWriter;
        }

        protected Writer getWatchdogFileWriter() throws IOException {
            return this.testWriter;
        }

        @Override
        protected synchronized void checkCriticalComponents() {
            super.checkCriticalComponents();
            this.hasCheckedCriticalComponents = true;
            notify();
        }

        @Override
        protected Writer getWatchdogDeviceWriter(String watchdogDevice) throws IOException {
            return testWriter;
        }

        @Override
        protected boolean isWatchdogDeviceAvailable(String watchdogDevice) {
            return true;
        }

        public synchronized boolean waitForCriticalComponentCheck(int delayMs) {
            if (!this.hasCheckedCriticalComponents) {
                try {
                    this.wait(delayMs);
                } catch (InterruptedException e) {
                }
            }
            return this.hasCheckedCriticalComponents;
        }
    }

    @Test
    public void testActivateDeactivate() throws NoSuchFieldException, IOException, InterruptedException {
        // activate and deactivate

        final WatchdogTestWriter watchdogWriter = new WatchdogTestWriter();
        TestWatchdogServiceImpl svc = new TestWatchdogServiceImpl(watchdogWriter);

        Map<String, Object> properties = getProperties(true);
        svc.activate(properties);

        assertTrue(svc.waitForCriticalComponentCheck(10000));
        assertTrue(watchdogWriter.toString().startsWith("w")); // check that the watchdog has been kicked at least once

        assertNotNull(TestUtil.getFieldValue(svc, "pollExecutor"));

        Future task = (Future) TestUtil.getFieldValue(svc, "pollTask");
        assertNotNull(task);

        assertFalse(task.isCancelled());

        svc.deactivate();

        assertNull(TestUtil.getFieldValue(svc, "pollTask"));
        assertTrue(task.isCancelled());
        assertNull(TestUtil.getFieldValue(svc, "pollExecutor"));

        assertTrue(watchdogWriter.waitForData(10000));
        assertTrue(watchdogWriter.toString().endsWith("ww")); // check that the watchdog has been kicked at least twice
                                                              // and it has not been disabled
    }

    private Map<String, Object> getProperties(boolean enabled) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("watchdogDevice", WATCHDOG_TEST_DEVICE);
        properties.put("rebootCauseFilePath", "target/watchdogTestCauses");
        properties.put("enabled", enabled);

        return properties;
    }

    @Test
    public void testUpdatedDisabled() throws IOException, NoSuchFieldException {
        // run activate/update

        final WatchdogTestWriter watchdogWriter = new WatchdogTestWriter();

        WatchdogServiceImpl svc = new TestWatchdogServiceImpl(watchdogWriter);

        Map<String, Object> properties = getProperties(false);

        svc.activate(properties);

        assertFalse(watchdogWriter.waitForData(1000));

        assertNull(TestUtil.getFieldValue(svc, "watchdogFileWriter"));
        assertNull(TestUtil.getFieldValue(svc, "timedOutOn"));

        svc.deactivate();

        assertFalse(watchdogWriter.waitForData(1000));
        assertTrue(watchdogWriter.toString().isEmpty());
    }

    @Test
    public void testDisable() throws Throwable {
        // disable the service and check that watchdog device is properly updated

        final WatchdogTestWriter watchdogWriter = new WatchdogTestWriter();

        WatchdogServiceImpl svc = new TestWatchdogServiceImpl(watchdogWriter);

        Map<String, Object> properties = getProperties(false);

        WatchdogServiceOptions options = new WatchdogServiceOptions(properties);
        TestUtil.setFieldValue(svc, "options", options);

        TestUtil.setFieldValue(svc, "watchdogFileWriter", watchdogWriter);

        TestUtil.invokePrivate(svc, "disableWatchdog");

        assertTrue(watchdogWriter.waitForData(10000));
        assertEquals("V", watchdogWriter.toString());

        svc.deactivate();
    }

    @Test
    public void testRegisterUnregisterGetCriticalComponent() throws NoSuchFieldException {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        List<CriticalComponentRegistration> criticalComponentRegistrations = new CopyOnWriteArrayList<>();
        TestUtil.setFieldValue(svc, "criticalComponentRegistrations", criticalComponentRegistrations);

        CriticalComponent cc1 = new TestCC("1", 0);
        svc.registerCriticalComponent(cc1);

        List<CriticalComponent> components = svc.getCriticalComponents();
        assertEquals(1, components.size());

        assertEquals("1", components.get(0).getCriticalComponentName());
        assertEquals(0, components.get(0).getCriticalComponentTimeout());

        // add the same component
        svc.registerCriticalComponent(cc1);

        components = svc.getCriticalComponents();
        assertEquals(1, components.size());

        // add a new component with the same name and timeout
        CriticalComponent cc2 = new TestCC("1", 0);
        svc.registerCriticalComponent(cc2);

        components = svc.getCriticalComponents();
        assertEquals(2, components.size());

        assertEquals(criticalComponentRegistrations.size(), components.size());

        // the deprecated method also does the same
        CriticalComponent cc3 = new TestCC("3", 1);
        svc.registerCriticalService(cc3);

        components = svc.getCriticalComponents();
        assertEquals(3, components.size());

        assertEquals(criticalComponentRegistrations.size(), components.size());

        assertEquals("1", components.get(0).getCriticalComponentName());
        assertEquals(0, components.get(0).getCriticalComponentTimeout());

        assertEquals("1", components.get(1).getCriticalComponentName());
        assertEquals(0, components.get(1).getCriticalComponentTimeout());

        assertEquals("3", components.get(2).getCriticalComponentName());
        assertEquals(1, components.get(2).getCriticalComponentTimeout());

        // add the first component, again - won't work
        svc.registerCriticalComponent(cc1);

        components = svc.getCriticalComponents();
        assertEquals(3, components.size());

        // unregister first one
        svc.unregisterCriticalComponent(cc1);

        components = svc.getCriticalComponents();
        assertEquals(2, components.size());

        // re-register first one
        svc.registerCriticalComponent(cc1);

        components = svc.getCriticalComponents();
        assertEquals(3, components.size());

        // unregister the now second one
        svc.unregisterCriticalComponent(cc1);

        components = svc.getCriticalComponents();
        assertEquals(2, components.size());

        // the deprecated method also works
        svc.unregisterCriticalService(cc3);

        components = svc.getCriticalComponents();
        assertEquals(1, components.size());

        assertEquals("1", components.get(0).getCriticalComponentName());
        assertEquals(0, components.get(0).getCriticalComponentTimeout());
    }

    @Test
    public void testCheckin() throws NoSuchFieldException {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        List<CriticalComponentRegistration> criticalComponentRegistrations = new CopyOnWriteArrayList<>();
        TestUtil.setFieldValue(svc, "criticalComponentRegistrations", criticalComponentRegistrations);

        CriticalComponent cc1 = new TestCC("1", 1);
        svc.registerCriticalComponent(cc1);

        CriticalComponent cc2 = new TestCC("2", 2);
        svc.registerCriticalComponent(cc2);

        CriticalComponentRegistration cci2 = criticalComponentRegistrations.get(1);
        TestUtil.setFieldValue(cci2, "updated", 12345);

        svc.checkin(cc2);

        assertTrue((long) TestUtil.getFieldValue(cci2, "updated") > 12345);
        assertTrue((long) TestUtil.getFieldValue(cci2, "updated") <= System.nanoTime());
    }

    public void testWatchdogFileDoesNotExist() throws Throwable {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        Map<String, Object> properties = getProperties(true);

        svc.activate(properties);

        assertNull(TestUtil.getFieldValue(svc, "pollTask"));
    }

    public void testShouldNotCreateWatchdogFile() throws InterruptedException {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        Map<String, Object> properties = getProperties(true);

        svc.activate(properties);

        Thread.sleep(2000);

        assertFalse(new File(WATCHDOG_TEST_DEVICE).exists());

        svc.deactivate();

        assertFalse(new File(WATCHDOG_TEST_DEVICE).exists());
    }

}

class TestCC implements CriticalComponent {

    private final String name;
    private final int timeout;

    public TestCC(String name, int timeout) {
        this.name = name;
        this.timeout = timeout;
    }

    @Override
    public String getCriticalComponentName() {
        return this.name;
    }

    @Override
    public int getCriticalComponentTimeout() {
        return this.timeout;
    }

}
