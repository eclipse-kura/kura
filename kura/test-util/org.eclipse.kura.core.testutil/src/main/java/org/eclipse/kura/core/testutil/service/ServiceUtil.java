/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.testutil.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServiceUtil {

    private ServiceUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> trackService(final Class<T> classz, final Optional<String> filter) {
        return trackService(classz.getName(), filter).thenApply(c -> (T) c);
    }

    public static CompletableFuture<Object> trackService(final String serviceInterfaceName,
            final Optional<String> filter) {
        try {
            final BundleContext bundleContext = FrameworkUtil.getBundle(ServiceUtil.class).getBundleContext();

            final CompletableFuture<Object> result = new CompletableFuture<>();

            final Filter osgiFilter;

            if (filter.isPresent()) {
                osgiFilter = FrameworkUtil
                        .createFilter("(&(objectClass=" + serviceInterfaceName + ")" + filter.get() + ")");
            } else {
                osgiFilter = FrameworkUtil.createFilter("(objectClass=" + serviceInterfaceName + ")");
            }

            final ServiceTracker<Object, Object> tracker = new ServiceTracker<>(bundleContext, osgiFilter,
                    new ServiceTrackerCustomizer<Object, Object>() {

                        @Override
                        public Object addingService(final ServiceReference<Object> ref) {
                            final Object obj = bundleContext.getService(ref);

                            result.complete(obj);

                            return obj;

                        }

                        @Override
                        public void modifiedService(final ServiceReference<Object> ref, final Object comp) {
                            // nothing to do
                        }

                        @Override
                        public void removedService(final ServiceReference<Object> ref, final Object comp) {
                            bundleContext.ungetService(ref);
                        }
                    });

            tracker.open();

            return result.whenComplete((ok, ex) -> tracker.close());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> CompletableFuture<T> createFactoryConfiguration(final ConfigurationService configurationService,
            final Class<T> classz, final String pid, final String factoryPid, final Map<String, Object> properties) {
        try {
            final BundleContext bundleContext = FrameworkUtil.getBundle(ServiceUtil.class).getBundleContext();

            final CompletableFuture<T> result = new CompletableFuture<>();

            final Filter filter = FrameworkUtil
                    .createFilter("(&(objectClass=" + classz.getName() + ")(kura.service.pid=" + pid + "))");

            final ServiceTracker<T, T> tracker = new ServiceTracker<>(bundleContext, filter,
                    new ServiceTrackerCustomizer<T, T>() {

                        @Override
                        public T addingService(final ServiceReference<T> ref) {
                            final T obj = bundleContext.getService(ref);

                            result.complete(obj);

                            return obj;

                        }

                        @Override
                        public void modifiedService(final ServiceReference<T> ref, final T comp) {
                            // nothing to do
                        }

                        @Override
                        public void removedService(final ServiceReference<T> ref, final T comp) {
                            bundleContext.ungetService(ref);
                        }
                    });

            tracker.open();

            configurationService.createFactoryConfiguration(factoryPid, pid, properties, true);

            return result.whenComplete((ok, ex) -> tracker.close());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<Void> deleteFactoryConfiguration(final ConfigurationService configurationService,
            final String pid) {

        try {
            final BundleContext bundleContext = FrameworkUtil.getBundle(ServiceUtil.class).getBundleContext();

            final CompletableFuture<Void> tracked = new CompletableFuture<>();
            final CompletableFuture<Void> removed = new CompletableFuture<>();

            final Filter filter = FrameworkUtil.createFilter("(kura.service.pid=" + pid + ")");

            final ServiceTracker<Object, Object> tracker = new ServiceTracker<>(bundleContext, filter,
                    new ServiceTrackerCustomizer<Object, Object>() {

                        @Override
                        public Object addingService(final ServiceReference<Object> ref) {
                            tracked.complete(null);
                            return bundleContext.getService(ref);
                        }

                        @Override
                        public void modifiedService(final ServiceReference<Object> ref, final Object comp) {
                            // nothing to do
                        }

                        @Override
                        public void removedService(final ServiceReference<Object> ref, final Object comp) {
                            removed.complete(null);
                            bundleContext.ungetService(ref);
                        }
                    });

            tracker.open();

            try {
                tracked.get(30, TimeUnit.SECONDS);
            } catch (final Exception e) {
                tracker.close();
                throw e;
            }

            configurationService.deleteFactoryConfiguration(pid, true);

            return removed.whenComplete((ok, ex) -> tracker.close());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<Void> updateComponentConfiguration(final ConfigurationService configurationService,
            final String pid, final Map<String, Object> properties) throws KuraException, InvalidSyntaxException {

        final CompletableFuture<Void> result = modified("(kura.service.pid=" + pid + ")");

        configurationService.updateConfiguration(pid, properties);

        return result;
    }

    public static CompletableFuture<Void> modified(final String filter) throws InvalidSyntaxException {

        final CompletableFuture<Void> result = new CompletableFuture<>();
        final BundleContext context = FrameworkUtil.getBundle(ServiceUtil.class).getBundleContext();

        final ServiceTracker<?, ?> tracker = new ServiceTracker<>(context, FrameworkUtil.createFilter(filter),
                new ServiceTrackerCustomizer<Object, Object>() {

                    @Override
                    public Object addingService(ServiceReference<Object> reference) {
                        return context.getService(reference);
                    }

                    @Override
                    public void modifiedService(ServiceReference<Object> reference, Object service) {
                        result.complete(null);
                    }

                    @Override
                    public void removedService(ServiceReference<Object> reference, Object service) {
                        context.ungetService(reference);
                    }
                });

        tracker.open();

        return result.whenComplete((ok, ex) -> tracker.close());
    }

    public static CompletableFuture<Void> removed(final String filter) throws InvalidSyntaxException {

        final CompletableFuture<Void> result = new CompletableFuture<>();
        final BundleContext context = FrameworkUtil.getBundle(ServiceUtil.class).getBundleContext();

        final ServiceTracker<?, ?> tracker = new ServiceTracker<>(context, FrameworkUtil.createFilter(filter),
                new ServiceTrackerCustomizer<Object, Object>() {

                    @Override
                    public Object addingService(ServiceReference<Object> reference) {
                        return context.getService(reference);
                    }

                    @Override
                    public void modifiedService(ServiceReference<Object> reference, Object service) {
                        // no need
                    }

                    @Override
                    public void removedService(ServiceReference<Object> reference, Object service) {
                        context.ungetService(reference);
                        result.complete(null);
                    }
                });

        tracker.open();

        return result.whenComplete((ok, ex) -> tracker.close());
    }
}
