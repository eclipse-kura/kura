/*******************************************************************************
 * Copyright (c) 2017 Amit Kumar Mondal
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.util.service;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.UtilMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ServiceSupplier} is used to safely retrieve target service instances. The release of
 * target service objects can be performed automatically if used with try-with-resources block. Otherwise
 * {@link #close()} must be invoked to release the service objects manually
 *
 * @param <T>
 *            the target service object
 */
public final class ServiceSupplier<T> implements Supplier<List<T>>, AutoCloseable {

    /** Logger instance */
    private static final Logger logger = LoggerFactory.getLogger(ServiceSupplier.class);

    /** Localization instance */
    private static final UtilMessages message = LocalizationAdapter.adapt(UtilMessages.class);

    /** Associated Bundle Context */
    private final BundleContext bundleContext;

    /** Associated Service References */
    private final Collection<ServiceReference<T>> serviceReferences;

    /** Acquired Service Instances */
    private final Map<T, ServiceReference<T>> acquiredServices;

    /** Constructor */
    private ServiceSupplier(final Class<T> target, @Nullable final String filter) {
        requireNonNull(target, message.targetNonNull());
        this.bundleContext = ServiceUtil.getBundleContext();
        this.serviceReferences = ServiceUtil.getServiceReferences(target, filter);
        this.acquiredServices = CollectionUtil.newHashMap();
    }

    /** Constructor */
    private ServiceSupplier(final ServiceReference<T> reference) {
        requireNonNull(reference, message.referenceNonNull());
        this.bundleContext = ServiceUtil.getBundleContext();
        this.serviceReferences = CollectionUtil.newArrayList();
        this.serviceReferences.add(reference);
        this.acquiredServices = CollectionUtil.newHashMap();
    }

    /**
     * Supplies the instance of {@link ServiceSupplier} from which the target service instance can be
     * retrieved
     *
     * @param target
     *            the service instance to retrieve
     * @param filter
     *            the valid OSGi service filter (can be {@code null})
     * @return the {@link ServiceSupplier} instance
     * @throws NullPointerException
     *             if any of the arguments is {@code null} (except {@code filter})
     * @throws IllegalArgumentException
     *             if {@link BundleContext} could not be retrieved
     */
    public static <T> ServiceSupplier<T> supply(final Class<T> target, @Nullable final String filter) {
        return new ServiceSupplier<>(target, filter);
    }

    /**
     * Supplies the instance of {@link ServiceSupplier} from which the target service instance can be
     * retrieved
     *
     * @param reference
     *            the {@link ServiceReference} instance to get the target service
     * @return the {@link ServiceSupplier} instance
     * @throws NullPointerException
     *             if any of the arguments is {@code null}
     */
    public static <T> ServiceSupplier<T> supply(final ServiceReference<T> reference) {
        return new ServiceSupplier<>(reference);
    }

    /**
     * Retrieves the list of target service instances
     *
     * @return the list of service instances or empty list
     */
    @Override
    public List<T> get() {
        try {
            for (final ServiceReference<T> ref : this.serviceReferences) {
                final T service = this.bundleContext.getService(ref);
                if (service != null) {
                    this.acquiredServices.put(service, ref);
                }
            }
        } catch (final Exception ex) {
            logger.warn(message.errorRetrievingService(), ex);
        }
        return unmodifiableList(new ArrayList<>(this.acquiredServices.keySet()));
    }

    /**
     * Releases the service objects for the service referenced by the specified
     * {@link ServiceReference} objects
     */
    @Override
    public void close() {
        for (final Map.Entry<T, ServiceReference<T>> service : this.acquiredServices.entrySet()) {
            final ServiceReference<T> ref = service.getValue();
            this.bundleContext.ungetService(ref);
        }
        this.acquiredServices.clear();
        this.serviceReferences.clear();
    }

}