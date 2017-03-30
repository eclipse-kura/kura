/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.stress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentContext;

public class StressTest {

    private static final String HEAP_ENABLE_PROP_NAME = "heap.enable";
    private static final String HEAP_THREADS_PROP_NAME = "heap.threads";
    private static final String HEAP_KBYTES_PROP_NAME = "heap.size";
    private static final String HEAP_STRIDE_KBYTES_PROP_NAME = "heap.stride";
    private static final String HEAP_HANG_MS_PROP_NAME = "heap.keep";
    private static final String HEAP_INTERVAL_MS_PROP_NAME = "heap.interval";
    private static final String HEAP_LOG_PROP_NAME = "heap.log";
    private static final String HEAP_DELAY_MS_PROP_NAME = "heap.delay";

    @Test
    public void testActivate() throws NoSuchFieldException, InterruptedException {
        ComponentContext componentContext = Mockito.mock(ComponentContext.class);
        // initialize stress parameters
        Map<String, Object> properties = setupStressConfig();

        Stress stress = new Stress();
        stress.activate(componentContext, properties);

        // check Stress initialized correctly
        assertNotNull(TestUtil.getFieldValue(stress, "m_properties"));
        assertEquals(properties, TestUtil.getFieldValue(stress, "m_properties"));

        ScheduledExecutorService worker = (ScheduledExecutorService) TestUtil.getFieldValue(stress, "m_worker");
        @SuppressWarnings("unchecked")
        List<ScheduledFuture<?>> taskList = (List<ScheduledFuture<?>>) TestUtil.getFieldValue(stress, "m_handle");
        assertEquals(properties.get(HEAP_THREADS_PROP_NAME), taskList.size());

        // let the stress run for a while
        Thread.sleep(3000);
        // confirm stress is running
        Assert.assertFalse(taskList.get(0).isDone());
        Assert.assertFalse(taskList.get(1).isDone());

        // stop stress
        // taskList.get(0).cancel(true);
        // taskList.get(1).cancel(true);
        worker.shutdown();
        worker.awaitTermination(1, TimeUnit.SECONDS);

        // check stress completed successfully
        assertTrue("Stress tasks not completed", worker.isTerminated());
        assertTrue("Stress not shutdown properly", worker.isShutdown());
    }

    @Test
    public void testDeactivate() throws NoSuchFieldException, InterruptedException {
        ComponentContext componentContext = Mockito.mock(ComponentContext.class);
        // initialize stress parameters
        Map<String, Object> properties = setupStressConfig();

        Stress stress = new Stress();
        stress.activate(componentContext, properties);

        // let the stress run for a while
        Thread.sleep(1000);

        stress.deactivate(componentContext);

        ScheduledExecutorService worker = (ScheduledExecutorService) TestUtil.getFieldValue(stress, "m_worker");
        worker.awaitTermination(1, TimeUnit.SECONDS);
        assertTrue("Stress not shutdown properly", worker.isShutdown());
    }

    @Test
    public void testUpdated() throws NoSuchFieldException, InterruptedException {
        ComponentContext componentContext = Mockito.mock(ComponentContext.class);
        // initialize stress parameters
        Map<String, Object> properties = setupStressConfig();

        Stress stress = new Stress();
        stress.activate(componentContext, properties);

        // let the stress run for a while then update config
        Thread.sleep(3000);
        Map<String, Object> propertiesUpdated = setupStressConfig();
        propertiesUpdated.put(HEAP_KBYTES_PROP_NAME, 10);
        stress.updated(propertiesUpdated);

        // check Stress updated correctly
        assertEquals(propertiesUpdated, TestUtil.getFieldValue(stress, "m_properties"));

        ScheduledExecutorService worker = (ScheduledExecutorService) TestUtil.getFieldValue(stress, "m_worker");
        @SuppressWarnings("unchecked")
        List<ScheduledFuture<?>> taskList = (List<ScheduledFuture<?>>) TestUtil.getFieldValue(stress, "m_handle");

        // let the stress run for a while
        Thread.sleep(1000);
        // confirm updated stress is running
        Assert.assertFalse(taskList.get(2).isDone());
        Assert.assertFalse(taskList.get(3).isDone());

        // stop stress
        worker.shutdown();
        worker.awaitTermination(1, TimeUnit.SECONDS);

        // check stress completed successfully
        Assert.assertTrue("Stress tasks not completed", worker.isTerminated());
        Assert.assertTrue("Stress not shutdown properly", worker.isShutdown());
    }

    private Map<String, Object> setupStressConfig() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(HEAP_ENABLE_PROP_NAME, Boolean.TRUE);
        properties.put(HEAP_THREADS_PROP_NAME, 2);
        properties.put(HEAP_KBYTES_PROP_NAME, 100);
        properties.put(HEAP_STRIDE_KBYTES_PROP_NAME, 100);
        properties.put(HEAP_HANG_MS_PROP_NAME, 100);
        properties.put(HEAP_INTERVAL_MS_PROP_NAME, 100);
        properties.put(HEAP_LOG_PROP_NAME, Boolean.TRUE);
        properties.put(HEAP_DELAY_MS_PROP_NAME, 500);
        return properties;
    }

}
