/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.util.service;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The Class ServiceUtil contains all necessary static factory methods to deal
 * with OSGi services
 */
public final class ServiceUtil {

    private ServiceUtil() {
        // Static Factory Methods container. No need to instantiate.
    }

    /**
     * Returns references to <em>all</em> services matching the given class name
     * and OSGi filter.
     *
     * @param bundleContext
     *            OSGi bundle context
     * @param clazz
     *            qualified class type
     * @param filter
     *            valid OSGi filter (can be {@code null})
     * @return {@code non-null} array of references to matching services
     * @throws NullPointerException
     *             if {@code bundleContext} or {@code clazz} is {@code null}
     * @throws IllegalArgumentException
     *             if the specified {@code filter} contains an invalid filter expression that cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    public static <T> ServiceReference<T>[] getServiceReferences(final BundleContext bundleContext,
            final Class<T> clazz, @Nullable final String filter) {
        requireNonNull(bundleContext, "Bundle context cannot be null.");
        requireNonNull(clazz, "Class intance name cannot be null.");

        try {
            final Collection<ServiceReference<T>> refs = bundleContext.getServiceReferences(clazz, filter);
            return refs.toArray(new ServiceReference[0]);
        } catch (final InvalidSyntaxException ise) {
            throw new IllegalArgumentException(ise);
        }
    }

    /**
     * Resets all the provided service reference use counts. If the provided {@link BundleContext} bundle's use count
     * for the provided service references are already zero, this would not further decrement the counters to
     * negative. Otherwise, the provided {@link BundleContext} bundle's use counts for the provided service references
     * is decremented by one.
     *
     * @param bundleContext
     *            OSGi bundle context
     * @param refs
     *            {@code non-null} array of all service references
     * @throws NullPointerException
     *             if any of the arguments is {@code null}
     */
    public static void ungetServiceReferences(final BundleContext bundleContext, final ServiceReference<?>[] refs) {
        requireNonNull(bundleContext, "Bundle context cannot be null.");
        requireNonNull(refs, "Service References cannot be null.");

        for (final ServiceReference<?> ref : refs) {
            bundleContext.ungetService(ref);
        }
    }

    /**
     * Waits for the specified amount of time for the services that matches the provided OSGi filter
     *
     * @param filter
     *            valid OSGi filter to match
     * @param timeout
     *            the timeout period
     * @param timeunit
     *            the {@link TimeUnit} for the timeout
     * @throws NullPointerException
     *             if the provided filter or the {@link TimeUnit} is {@code null}
     * @throws IllegalArgumentException
     *             if the timeout period is {@code zero} or {@code negative}
     * @throws InterruptedException
     *             if another thread has interrupted the current worker thread
     * @throws InvalidSyntaxException
     *             if the provided filter syntax is erroneous
     * @return an {@link Optional} with the tracked service instance if the service instance
     *         is {@code non-null}, otherwise an empty {@link Optional}
     */
    public static Optional<Object> waitForService(final String filter, final long timeout, final TimeUnit timeunit)
            throws InterruptedException, InvalidSyntaxException {
        requireNonNull(filter, "Filter cannot be null.");
        requireNonNull(timeunit, "TimeUnit cannot be null");
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout period cannot be zero or negative");
        }

        final long timeoutInMillis = timeunit.toMillis(timeout);
        final BundleContext bundleContext = FrameworkUtil.getBundle(ServiceUtil.class).getBundleContext();
        final Filter filterRef = bundleContext.createFilter(filter);
        final ServiceTracker<Object, Object> serviceTracker = new ServiceTracker<>(bundleContext, filterRef, null);
        serviceTracker.open();
        final Object service = serviceTracker.waitForService(timeoutInMillis);
        serviceTracker.close();

        return Optional.ofNullable(service);
    }
}
