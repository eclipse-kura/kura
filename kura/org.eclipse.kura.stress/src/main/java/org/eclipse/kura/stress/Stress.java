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

    private static final Logger logger = LoggerFactory.getLogger(Stress.class);

    private static final String HEAP_ENABLE_PROP_NAME = "heap.enable";
    private static final String HEAP_THREADS_PROP_NAME = "heap.threads";
    private static final String HEAP_KBYTES_PROP_NAME = "heap.size";
    private static final String HEAP_STRIDE_KBYTES_PROP_NAME = "heap.stride";
    private static final String HEAP_HANG_MS_PROP_NAME = "heap.keep";
    private static final String HEAP_INTERVAL_MS_PROP_NAME = "heap.interval";
    private static final String HEAP_LOG_PROP_NAME = "heap.log";
    private static final String HEAP_DELAY_MS_PROP_NAME = "heap.delay";

    private final ScheduledExecutorService worker;
    private final List<ScheduledFuture<?>> handle;

    private Map<String, Object> properties;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public Stress() {
        super();
        this.worker = Executors.newScheduledThreadPool(5);
        this.handle = new ArrayList<ScheduledFuture<?>>();
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Activating Stress...");

        this.properties = properties;
        for (String s : properties.keySet()) {
            logger.info("Activate - " + s + ": " + properties.get(s));
        }

        doUpdate();

        logger.info("Activating Stress... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating Stress...");

        // shutting down the worker and cleaning up the properties
        this.worker.shutdown();

        logger.debug("Deactivating Stress... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated Stress...");

        // store the properties received
        this.properties = properties;
        for (String s : properties.keySet()) {
            logger.info("Update - " + s + ": " + properties.get(s));
        }

        // try to kick off a new job
        doUpdate();
        logger.info("Updated Stress... Done.");
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
        for (ScheduledFuture<?> handleItem : this.handle) {
            if (handleItem != null) {
                handleItem.cancel(true);
            }
        }

        final boolean enable = (Boolean) this.properties.get(HEAP_ENABLE_PROP_NAME);
        final int threads = (Integer) this.properties.get(HEAP_THREADS_PROP_NAME);
        final int size = 1024 * (Integer) this.properties.get(HEAP_KBYTES_PROP_NAME);
        final int stride = 1024 * (Integer) this.properties.get(HEAP_STRIDE_KBYTES_PROP_NAME);
        final int hang = (Integer) this.properties.get(HEAP_HANG_MS_PROP_NAME);
        final int interval = (Integer) this.properties.get(HEAP_INTERVAL_MS_PROP_NAME);
        final boolean log = (Boolean) this.properties.get(HEAP_LOG_PROP_NAME);
        final int delay = (Integer) this.properties.get(HEAP_DELAY_MS_PROP_NAME);

        if (enable) {
            for (int i = 0; i < threads; i++) {
                final int tid = i;
                ScheduledFuture<?> futureHandle = this.worker.scheduleAtFixedRate(new Runnable() {

                    @Override
                    public void run() {
                        String name = Thread.currentThread().getName();
                        Thread.currentThread().setName("Heap stress thread #" + tid);

                        if (log) {
                            logger.info("Heap stress thread #{} allocating...", tid);
                        }

                        stressHeap(size, stride, hang);

                        if (log) {
                            logger.info("Heap stress thread #{} allocating... done", tid);
                        }

                        Thread.currentThread().setName(name);
                    }
                }, delay * i, interval, TimeUnit.MILLISECONDS);
                this.handle.add(futureHandle);
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
            logger.warn("Interrupted", e);
        }
    }
}
