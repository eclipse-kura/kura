/*******************************************************************************
 * Copyright (c) 2016, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.util.service;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
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

    public static <T> Collection<ServiceReference<T>> getServiceReferencesAsCollection(
            final BundleContext bundleContext, Class<T> serviceClass, String filter) throws KuraException {

        try {
            final Collection<ServiceReference<T>> sr = bundleContext.getServiceReferences(serviceClass, filter);

            if (sr == null) {
                throw KuraException.internalError(serviceClass.toString() + " not found.");
            }

            return sr;
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }

    }

    public static <T> T getService(final BundleContext bundleContext, ServiceReference<T> serviceReference)
            throws KuraException {
        T service = null;

        if (serviceReference != null) {
            service = bundleContext.getService(serviceReference);
        }
        if (service == null) {
            throw KuraException.internalError("Service not found.");
        }
        return service;
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

    public static <T> T withService(final BundleContext bundleContext, final Function<Optional<Object>, T> func,
            final String filter) throws InvalidSyntaxException {
        final ServiceReference<?>[] refs = bundleContext.getServiceReferences((String) null, filter);

        if (refs == null || refs.length == 0) {
            return func.apply(Optional.empty());
        }

        final ServiceReference<?> ref = refs[0];

        try {
            final Object o = bundleContext.getService(ref);

            return func.apply(Optional.ofNullable(o));
        } finally {
            bundleContext.ungetService(ref);
        }
    }

    /**
     * Lookup services with a filter and iterate over their instances
     *
     * @param serviceClass
     *            the services to look for
     * @param consumer
     *            the consumer which will be called for each service instance
     * @throws GwtKuraException
     *             if any service consumer throws an exception
     * @throws InvalidSyntaxException
     *             if the filter was not {@code null} and had an invalid syntax
     */
    public static <T> void withAllServices(final BundleContext bundleContext, final Class<T> serviceClass,
            String filter, final ServiceConsumer<T> consumer) throws KuraException {

        withAllServiceReferences(bundleContext, serviceClass, filter, (ref, ctx) -> {
            final T service = ctx.getService(ref);

            try {
                consumer.consume(service);
            } finally {
                ctx.ungetService(ref);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> void withAllServiceReferences(final BundleContext bundleContext, final Class<T> serviceClass,
            String filter, final ServiceReferenceConsumer<T> consumer) throws KuraException {

        final ServiceReference<?>[] refs;
        try {
            refs = bundleContext.getAllServiceReferences(serviceClass != null ? serviceClass.getName() : null, filter);
        } catch (InvalidSyntaxException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    serviceClass != null ? serviceClass.getName() : "");
        }

        // no result ... do nothing
        if (refs == null) {
            return;
        }

        // iterate over results
        for (final ServiceReference<?> ref : refs) {
            consumer.consume((ServiceReference<T>) ref, bundleContext);
        }
    }

    @SuppressWarnings("unchecked")
    public static void withAllServices(final BundleContext bundleContext, String filter,
            final ServiceConsumer<Object> consumer, final Class<?>... classes) throws KuraException {

        if (classes == null || classes.length == 0) {
            withAllServices(bundleContext, null, filter, consumer);
        }

        for (Class<?> c : classes) {
            withAllServices(bundleContext, (Class<Object>) c, filter, consumer);
        }
    }

    @SuppressWarnings("unchecked")
    public static void withAllServiceReferences(final BundleContext bundleContext, String filter,
            final ServiceReferenceConsumer<Object> consumer, final Class<?>... classes) throws KuraException {

        if (classes == null || classes.length == 0) {
            withAllServiceReferences(bundleContext, null, filter, consumer);
        }

        for (Class<?> c : classes) {
            withAllServiceReferences(bundleContext, (Class<Object>) c, filter, consumer);
        }
    }

    public static boolean ungetService(final BundleContext bundleContext, ServiceReference<?> serviceReference) {

        if (serviceReference != null) {
            return bundleContext.ungetService(serviceReference);
        }

        return false;
    }

    public static <T, R> R applyToServiceOptionally(final BundleContext bundleContext, final Class<T> serviceClass,
            final ServiceFunction<T, R> function) throws KuraException {

        final ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);

        try {
            if (ref == null) {
                return function.apply(null);
            }

            final T service = bundleContext.getService(ref);
            try {
                return function.apply(service);
            } finally {
                bundleContext.ungetService(ref);
            }
        } catch (Exception e) {
            throw KuraException.internalError(e.getMessage());
        }
    }

    public static boolean providesService(final BundleContext bundleContext, final String kuraServicePid,
            final Class<?> serviceInterface) {
        return providesService(bundleContext, kuraServicePid, s -> s.contains(serviceInterface.getName()));
    }

    public static boolean providesService(final BundleContext bundleContext, final String kuraServicePid,
            final Predicate<Set<String>> filter) {

        final String pidFilter = "(kura.service.pid=" + kuraServicePid + ")";

        try {
            final ServiceReference<?>[] refs = bundleContext.getAllServiceReferences(null, pidFilter);

            if (refs == null) {
                return false;
            }

            for (final ServiceReference<?> ref : refs) {
                final Object rawProvidedInterfaces = ref.getProperty("objectClass");

                final Set<String> providedInterfaces;

                if (rawProvidedInterfaces instanceof String) {
                    providedInterfaces = Collections.singleton((String) rawProvidedInterfaces);
                } else if (rawProvidedInterfaces instanceof String[]) {
                    providedInterfaces = Arrays.asList((String[]) rawProvidedInterfaces).stream()
                            .collect(Collectors.toSet());
                } else {
                    providedInterfaces = Collections.emptySet();
                }

                if (filter.test(providedInterfaces)) {
                    return true;
                }
            }

            return false;
        } catch (InvalidSyntaxException e) {
            return false;
        }
    }

    public static boolean isFactoryOf(final BundleContext bundleContext, final String factoryPid,
            final Predicate<Set<String>> filter) {

        final ServiceReference<ServiceComponentRuntime>[] refs = getServiceReferences(bundleContext,
                ServiceComponentRuntime.class, null);

        if (refs == null || refs.length == 0) {
            return false;
        }

        final ServiceReference<ServiceComponentRuntime> ref = refs[0];

        try {
            final ServiceComponentRuntime scr = bundleContext.getService(ref);

            return scr.getComponentDescriptionDTOs().stream().anyMatch(c -> {
                if (!Objects.equals(factoryPid, c.name)) {
                    return false;
                }

                return providedInterfacesMatch(c.serviceInterfaces, filter);
            });
        } catch (final Exception e) {
            return false;
        } finally {
            bundleContext.ungetService(ref);
        }
    }

    private static boolean providedInterfacesMatch(final String[] providedInterfaces,
            final Predicate<Set<String>> filter) {
        if (providedInterfaces == null) {
            return filter.test(Collections.emptySet());
        }

        return filter.test(Arrays.stream(providedInterfaces).collect(Collectors.toSet()));
    }

    public static boolean isFactoryOfAnyService(final BundleContext bundleContext, final String factoryPid,
            final Class<?>... interfaces) {
        return isFactoryOf(bundleContext, factoryPid, s -> {
            for (final Class<?> intf : interfaces) {
                if (s.contains(intf.getName())) {
                    return true;
                }
            }

            return false;
        });
    }

    public interface ServiceFunction<T, R> {

        public R apply(T service) throws KuraException;
    }

    public interface ServiceConsumer<T> {

        public void consume(T service) throws KuraException;
    }

    public interface ServiceReferenceConsumer<T> {

        public void consume(ServiceReference<T> service, BundleContext context) throws KuraException;
    }

}
