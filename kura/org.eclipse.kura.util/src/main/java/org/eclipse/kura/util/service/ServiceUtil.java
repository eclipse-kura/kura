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

import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.UtilMessages;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * The Class ServiceUtil contains all necessary static factory methods to deal
 * with OSGi services
 */
public final class ServiceUtil {

    /** Localization Resource. */
    private static final UtilMessages s_message = LocalizationAdapter.adapt(UtilMessages.class);

    /** Constructor */
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
     * @return non-<code>null</code> array of references to matching services
     * @throws NullPointerException
     *             if bundle syntax or class type is null
     * @throws IllegalArgumentException
     *             if the filter syntax is erroneous
     */
    @SuppressWarnings("unchecked")
    public static <T> ServiceReference<T>[] getServiceReferences(final BundleContext bundleContext,
            final Class<T> clazz, @Nullable final String filter) {
        requireNonNull(bundleContext, s_message.bundleContextNonNull());
        requireNonNull(clazz, s_message.clazzNonNull());

        try {
            final Collection<ServiceReference<T>> refs = bundleContext.getServiceReferences(clazz, filter);
            return refs.toArray(new ServiceReference[0]);
        } catch (final InvalidSyntaxException ise) {
            throw new IllegalArgumentException(ise);
        }
    }

    /**
     * Resets all the service reference counters
     *
     * @param bundleContext
     *            OSGi bundle context
     * @param refs
     *            the array of all service references
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public static void ungetServiceReferences(final BundleContext bundleContext, final ServiceReference<?>[] refs) {
        requireNonNull(bundleContext, s_message.bundleContextNonNull());
        requireNonNull(refs, s_message.referencesNonNull());

        for (final ServiceReference<?> ref : refs) {
            bundleContext.ungetService(ref);
        }

    }

}
