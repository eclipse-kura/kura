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
package org.eclipse.kura.stress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stress implements ConfigurableComponent {

    private static final Logger s_logger = LoggerFactory.getLogger(Stress.class);

    private static final String HEAP_ENABLE_PROP_NAME = "heap.enable";
    private static final String HEAP_THREADS_PROP_NAME = "heap.threads";
    private static final String HEAP_KBYTES_PROP_NAME = "heap.size";
    private static final String HEAP_STRIDE_KBYTES_PROP_NAME = "heap.stride";
    private static final String HEAP_HANG_MS_PROP_NAME = "heap.keep";
    private static final String HEAP_INTERVAL_MS_PROP_NAME = "heap.interval";
    private static final String HEAP_LOG_PROP_NAME = "heap.log";
    private static final String HEAP_DELAY_MS_PROP_NAME = "heap.delay";

    private final ScheduledExecutorService m_worker;
    private final List<ScheduledFuture<?>> m_handle;

    private Map<String, Object> m_properties;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public Stress() {
        super();
        this.m_worker = Executors.newScheduledThreadPool(5);
        this.m_handle = new ArrayList<ScheduledFuture<?>>();
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Activating Stress...");

        this.m_properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Activate - " + s + ": " + properties.get(s));
        }

        doUpdate();

        s_logger.info("Activating Stress... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.debug("Deactivating Stress...");

        // shutting down the worker and cleaning up the properties
        this.m_worker.shutdown();

        s_logger.debug("Deactivating Stress... Done.");
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated Stress...");

        // store the properties received
        this.m_properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Update - " + s + ": " + properties.get(s));
        }

        // try to kick off a new job
        doUpdate();
        s_logger.info("Updated Stress... Done.");
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate() {
        // cancel a current worker handle if one if active
        for (ScheduledFuture<?> handle : this.m_handle) {
            if (handle != null) {
                handle.cancel(true);
            }
        }

        final boolean enable = (Boolean) this.m_properties.get(HEAP_ENABLE_PROP_NAME);
        final int threads = (Integer) this.m_properties.get(HEAP_THREADS_PROP_NAME);
        final int size = 1024 * (Integer) this.m_properties.get(HEAP_KBYTES_PROP_NAME);
        final int stride = 1024 * (Integer) this.m_properties.get(HEAP_STRIDE_KBYTES_PROP_NAME);
        final int hang = (Integer) this.m_properties.get(HEAP_HANG_MS_PROP_NAME);
        final int interval = (Integer) this.m_properties.get(HEAP_INTERVAL_MS_PROP_NAME);
        final boolean log = (Boolean) this.m_properties.get(HEAP_LOG_PROP_NAME);
        final int delay = (Integer) this.m_properties.get(HEAP_DELAY_MS_PROP_NAME);

        if (enable) {
            for (int i = 0; i < threads; i++) {
                final int tid = i;
                ScheduledFuture<?> handle = this.m_worker.scheduleAtFixedRate(new Runnable() {

                    @Override
                    public void run() {
                        String name = Thread.currentThread().getName();
                        Thread.currentThread().setName("Heap stress thread #" + tid);

                        if (log) {
                            s_logger.info("Heap stress thread #{} allocating...", tid);
                        }

                        stressHeap(size, stride, hang);

                        if (log) {
                            s_logger.info("Heap stress thread #{} allocating... done", tid);
                        }

                        Thread.currentThread().setName(name);
                    }
                }, delay * i, interval, TimeUnit.MILLISECONDS);
                this.m_handle.add(handle);
            }
        }
    }

    private void stressHeap(int size, int stride, long hang) {
        byte[] b = new byte[size];

        // touch one byte every stride bytes
        for (int i = 0; i < b.length; i += stride) {
            b[i] = 'k';
        }

        try {
            if (hang > 0) {
                Thread.sleep(hang);
            } else {
                while (true) {
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException e) {
            s_logger.warn("Interrupted", e);
        }
    }
}
