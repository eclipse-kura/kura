/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.rest.provider;

import java.lang.reflect.Field;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletContainerBridgeFix implements ServiceTrackerCustomizer<Object, Thread> {

    private static final Logger logger = LoggerFactory.getLogger(ServletContainerBridgeFix.class);

    private final BundleContext bundleContext;

    public ServletContainerBridgeFix(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public Thread addingService(ServiceReference<Object> reference) {
        final Object service = this.bundleContext.getService(reference);

        logger.info("found service: {}", service.getClass());

        final Thread worker = new Worker(service);
        worker.start();

        return worker;
    }

    @Override
    public void modifiedService(ServiceReference<Object> reference, Thread worker) {
        // do nothing
    }

    @Override
    public void removedService(ServiceReference<Object> reference, Thread worker) {
        worker.interrupt();

        try {
            worker.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        this.bundleContext.ungetService(reference);

    }

    private static class Worker extends Thread {

        private final Object servlet;

        public Worker(final Object servlet) {
            this.servlet = servlet;
        }

        @Override
        public void run() {

            Thread.currentThread().setName("ServletContainerBridgeFixWorker");

            try {

                while (!isInterrupted()) {

                    final Field servletField = servlet.getClass().getDeclaredField("servlet");
                    servletField.setAccessible(true);

                    final Object bridge = servletField.get(servlet);

                    final Field servletConfigField = bridge.getClass().getDeclaredField("servletConfig");
                    servletConfigField.setAccessible(true);

                    final Object servletConfig = servletConfigField.get(bridge);

                    if (servletConfig == null) {
                        sleep(200);
                        continue;
                    }

                    final Field configField = bridge.getClass().getSuperclass().getSuperclass()
                            .getDeclaredField("config");
                    configField.setAccessible(true);

                    configField.set(bridge, servletConfig);

                    return;
                }

            } catch (final InterruptedException e) {
                interrupt();
            } catch (final Exception e) {
                logger.warn("failed to fix ServletContainerHolder", e);
            }
        }
    }

}
