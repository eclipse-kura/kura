/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.runner;

import java.util.Objects;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class DefaultServiceDependency<T, C> implements ServiceDependency<T, C> {

    private final class HandleImpl implements Handle<C> {

        private final BundleContext bundleContext;
        private final ServiceConsumer<T, C> consumer;

        private final TreeMap<ServiceReference<T>, T> services = new TreeMap<>();

        private final ServiceTracker<T, T> tracker;

        private Runnable runnable;

        private final ServiceTrackerCustomizer<T, T> customizer = new ServiceTrackerCustomizer<T, T>() {

            @Override
            public T addingService(ServiceReference<T> reference) {
                return adding(reference);
            }

            @Override
            public void modifiedService(ServiceReference<T> reference, T service) {
            }

            @Override
            public void removedService(ServiceReference<T> reference, T service) {
                removed(reference, service);
            }
        };

        public HandleImpl(BundleContext bundleContext, Filter filter, Runnable runnable,
                final ServiceConsumer<T, C> consumer) {
            this.bundleContext = bundleContext;
            this.consumer = consumer;
            this.runnable = runnable;

            this.tracker = new ServiceTracker<>(bundleContext, filter, HandleImpl.this.customizer);
            this.tracker.open();
        }

        protected T adding(final ServiceReference<T> reference) {
            final T service = this.bundleContext.getService(reference);
            this.services.put(reference, service);
            triggerUpdate();
            return service;
        }

        protected void removed(final ServiceReference<T> reference, final T service) {
            this.bundleContext.ungetService(reference);
            this.services.remove(reference);
            triggerUpdate();
        }

        private void triggerUpdate() {
            final Runnable runnable = this.runnable;
            if (runnable != null) {
                runnable.run();
            }
        }

        @Override
        public void stop() {
            this.runnable = null;
            this.tracker.close();
        }

        @Override
        public boolean isSatisfied() {
            return !this.services.isEmpty();
        }

        @Override
        public void consume(C context) {
            this.consumer.consume(context, this.services.firstEntry().getValue());
        }

        @Override
        public String toString() {
            return String.format("[Service - filter: %s]", DefaultServiceDependency.this.filter);
        }

    }

    private final BundleContext bundleContext;

    private final Filter filter;

    private final ServiceConsumer<T, C> consumer;

    public DefaultServiceDependency(final BundleContext bundleContext, final Filter filter,
            final ServiceConsumer<T, C> consumer) {
        Objects.requireNonNull(bundleContext);
        Objects.requireNonNull(filter);

        this.bundleContext = bundleContext;
        this.filter = filter;
        this.consumer = consumer;
    }

    @Override
    public Handle<C> start(final Runnable runnable) {
        Objects.requireNonNull(runnable);

        return new HandleImpl(this.bundleContext, this.filter, runnable, this.consumer);

    }

}