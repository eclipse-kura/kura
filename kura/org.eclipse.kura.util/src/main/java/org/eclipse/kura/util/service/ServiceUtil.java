/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
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
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.UtilMessages;
import org.osgi.framework.Bundle;
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

    /** Localization instance */
    private static final UtilMessages message = LocalizationAdapter.adapt(UtilMessages.class);

    /** Constructor */
    private ServiceUtil() {
        // Static Factory Methods container. No need to instantiate.
    }

    /**
     * Returns references to <em>all</em> services matching the target class name
     * and OSGi filter
     *
     * @param target
     *            the service instance to retrieve
     * @param filter
     *            valid OSGi filter (can be {@code null})
     * @return {@code non-null} array of references to matching services
     * @throws NullPointerException
     *             if {@code target} is {@code null}
     * @throws IllegalArgumentException
     *             if the specified {@code filter} contains an invalid filter expression that cannot
     *             be parsed or the {@link BundleContext} instance for this bundle cannot be acquired
     */
    public static <T> Collection<ServiceReference<T>> getServiceReferences(final Class<T> target,
            @Nullable final String filter) {
        requireNonNull(target, message.targetNonNull());
        final BundleContext context = getBundleContext();
        try {
            return context.getServiceReferences(target, filter);
        } catch (final InvalidSyntaxException ise) {
            throw new IllegalArgumentException(ise);
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
        requireNonNull(filter, message.filterNonNull());
        requireNonNull(timeunit, message.timeunitNonNull());
        if (timeout <= 0) {
            throw new IllegalArgumentException(message.timeoutError());
        }
        final long timeoutInMillis = timeunit.toMillis(timeout);
        final BundleContext bundleContext = getBundleContext();
        final Filter filterRef = bundleContext.createFilter(filter);
        final ServiceTracker<Object, Object> serviceTracker = new ServiceTracker<>(bundleContext, filterRef, null);
        serviceTracker.open();
        final Object service = serviceTracker.waitForService(timeoutInMillis);
        serviceTracker.close();
        return Optional.ofNullable(service);
    }

    /**
     * Returns {@link BundleContext} instance
     *
     * @throws IllegalArgumentException
     *             if {@link BundleContext} instance cannot be acquired
     * @return {@link BundleContext} instance
     */
    static BundleContext getBundleContext() {
        final Bundle bundle = FrameworkUtil.getBundle(ServiceUtil.class);
        final BundleContext context = bundle == null ? null : bundle.getBundleContext();
        if (context == null) {
            throw new IllegalArgumentException(message.noBundleContext(ServiceUtil.class.getCanonicalName()));
        }
        return context;
    }
}
