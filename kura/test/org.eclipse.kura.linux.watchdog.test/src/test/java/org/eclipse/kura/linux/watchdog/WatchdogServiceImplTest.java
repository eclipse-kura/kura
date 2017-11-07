/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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
            this.notify();
        }

        public synchronized boolean waitForData(int delayMs) { // waits for some data to be written since the last call
                                                               // to this method
            if (this.toString().length() == lastLength) {
                try {
                    this.wait(delayMs);
                } catch (InterruptedException e) {
                }
            }
            final int currentLength = this.toString().length();
            final boolean result = currentLength != lastLength;
            this.lastLength = currentLength;
            return result;
        }
    }

    public class TestWatchdogServiceImpl extends WatchdogServiceImpl {

        private Writer testWriter;
        private volatile boolean hasCheckedCriticalComponents;

        public TestWatchdogServiceImpl(Writer fileWriter) {
            this.testWriter = fileWriter;
        }

        @Override
        protected Writer getWatchdogFileWriter() throws IOException {
            return testWriter;
        }

        @Override
        protected synchronized void checkCriticalComponents() {
            // TODO Auto-generated method stub
            super.checkCriticalComponents();
            this.hasCheckedCriticalComponents = true;
            this.notify();
        }

        public synchronized boolean waitForCriticalComponentCheck(int delayMs) {
            if (!hasCheckedCriticalComponents) {
                try {
                    this.wait(delayMs);
                } catch (InterruptedException e) {
                }
            }
            return hasCheckedCriticalComponents;
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

        assertTrue((boolean) TestUtil.getFieldValue(svc, "enabled"));
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

        assertFalse((boolean) TestUtil.getFieldValue(svc, "enabled"));

        svc.deactivate();

        assertFalse(watchdogWriter.waitForData(1000));
        assertTrue(watchdogWriter.toString().isEmpty());
    }

    @Test
    public void testDisable() throws Throwable {
        // disable the service and check that watchdog device is properly updated

        final WatchdogTestWriter watchdogWriter = new WatchdogTestWriter();

        WatchdogServiceImpl svc = new TestWatchdogServiceImpl(watchdogWriter);

        TestUtil.setFieldValue(svc, "enabled", true);

        Map<String, Object> properties = getProperties(false);

        WatchdogServiceOptions options = new WatchdogServiceOptions(properties);
        TestUtil.setFieldValue(svc, "options", options);

        TestUtil.invokePrivate(svc, "disableWatchdog");

        assertTrue(watchdogWriter.waitForData(10000));
        assertEquals("V", watchdogWriter.toString());

        assertFalse((boolean) TestUtil.getFieldValue(svc, "enabled"));

        svc.deactivate();
    }

    @Test
    public void testRegisterUnregisterGetCriticalComponent() throws NoSuchFieldException {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        List<CriticalComponentImpl> criticalComponentList = new CopyOnWriteArrayList<>();
        TestUtil.setFieldValue(svc, "criticalComponentList", criticalComponentList);

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

        assertEquals(criticalComponentList.size(), components.size());

        // the deprecated method also does the same
        CriticalComponent cc3 = new TestCC("3", 1);
        svc.registerCriticalService(cc3);

        components = svc.getCriticalComponents();
        assertEquals(3, components.size());

        assertEquals(criticalComponentList.size(), components.size());

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

        List<CriticalComponentImpl> criticalComponentList = new CopyOnWriteArrayList<>();
        TestUtil.setFieldValue(svc, "criticalComponentList", criticalComponentList);

        CriticalComponent cc1 = new TestCC("1", 1);
        svc.registerCriticalComponent(cc1);

        CriticalComponent cc2 = new TestCC("2", 2);
        svc.registerCriticalComponent(cc2);

        CriticalComponentImpl cci2 = criticalComponentList.get(1);
        TestUtil.setFieldValue(cci2, "updated", 12345);

        svc.checkin(cc2);

        assertTrue((long) TestUtil.getFieldValue(cci2, "updated") > 12345);
        assertTrue((long) TestUtil.getFieldValue(cci2, "updated") <= System.currentTimeMillis());
    }

    @Test
    public void testCheck() throws Throwable {
        final WatchdogTestWriter watchdogWriter = new WatchdogTestWriter();

        WatchdogServiceImpl svc = new TestWatchdogServiceImpl(watchdogWriter);

        RebootCauseFileWriter causeWriterMock = mock(RebootCauseFileWriter.class);
        TestUtil.setFieldValue(svc, "rebootCauseWriter", causeWriterMock);

        List<CriticalComponentImpl> criticalComponentList = new CopyOnWriteArrayList<>();
        TestUtil.setFieldValue(svc, "criticalComponentList", criticalComponentList);

        TestUtil.setFieldValue(svc, "enabled", true);

        TestUtil.setFieldValue(svc, "watchdogToStop", true); // prevent reboot

        Map<String, Object> properties = getProperties(false);
        WatchdogServiceOptions options = new WatchdogServiceOptions(properties);
        TestUtil.setFieldValue(svc, "options", options);

        CriticalComponent cc1 = new TestCC("1", 1);
        svc.registerCriticalComponent(cc1);

        CriticalComponent cc2 = new TestCC("2", 2000);
        svc.registerCriticalComponent(cc2);

        Thread.sleep(2); // wait for cc1 to time out

        TestUtil.invokePrivate(svc, "checkCriticalComponents");

        verify(causeWriterMock, times(1)).writeRebootCause("failure in 1");

        // verify that reboot path was skipped
        assertTrue(watchdogWriter.waitForData(10000));
        assertTrue(watchdogWriter.toString().equals("w"));
    }

    @Test(expected = IOException.class)
    public void testShouldThrowIfWatchdogFileDoesNotExist() throws Throwable {
        WatchdogServiceImpl svc = new WatchdogServiceImpl();

        Map<String, Object> properties = getProperties(true);

        svc.activate(properties);

        TestUtil.invokePrivate(svc, "getWatchdogFileWriter");
    }

    public void testShouldNotCreateWatchdogFile() throws InterruptedException {
        final AtomicBoolean called = new AtomicBoolean(false);
        WatchdogServiceImpl svc = new WatchdogServiceImpl() {

            protected Writer getWatchdogFileWriter() throws IOException {
                called.set(true);
                called.notify();
                return super.getWatchdogFileWriter();
            }
        };

        Map<String, Object> properties = getProperties(true);

        svc.activate(properties);

        synchronized (called) {
            called.wait(2000);
        }
        assertTrue(called.get());
        assertFalse(new File(WATCHDOG_TEST_DEVICE).exists());

        svc.deactivate();

        assertFalse(new File(WATCHDOG_TEST_DEVICE).exists());
    }

}

class TestCC implements CriticalComponent {

    private String name;
    private int timeout;

    public TestCC(String name, int timeout) {
        this.name = name;
        this.timeout = timeout;
    }

    @Override
    public String getCriticalComponentName() {
        return name;
    }

    @Override
    public int getCriticalComponentTimeout() {
        return timeout;
    }

}
