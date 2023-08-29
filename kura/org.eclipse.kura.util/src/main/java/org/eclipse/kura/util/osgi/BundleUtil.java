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
package org.eclipse.kura.util.osgi;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * The Class BundleUtil contains all necessary static factory methods to deal
 * with OSGi bundles.
 */
public class BundleUtil {

    private BundleUtil() {
        // Static Factory Methods container. No need to instantiate.
    }

    /**
     * Returns the set of OSGi bundles in which registered services match one of the classes and all specified
     * properties.
     * 
     * @param bundleContext
     *            the context of bundle.
     * @param classes
     *            Array of possible classes.
     * @param properties
     *            Map of required properties.
     * @return A set of OSGi bundles.
     */

    public static Set<Bundle> getBundles(final BundleContext bundleContext, final Class<?>[] classes,
            final Map<String, String> properties) {
        requireNonNull(bundleContext, "Bundle context cannot be null.");
        requireNonNull(properties, "Filter cannot be null.");
        requireNonNull(classes, "Class array cannot be null.");

        String classFilter = Arrays.stream(classes).map(FilterUtil::objectClass).reduce("", FilterUtil::or);

        String propertiesFilter = properties.entrySet().stream().map(p -> FilterUtil.equal(p.getKey(), p.getValue()))
                .reduce("", FilterUtil::and);

        String filterAsString = FilterUtil.and(classFilter, propertiesFilter);

        try {
            Filter filter = bundleContext.createFilter(filterAsString);
            ServiceReference<?>[] services = bundleContext.getAllServiceReferences(null, filter.toString());

            if (services == null || services.length == 0) {
                return Collections.emptySet();
            }

            return Arrays.stream(services).map(ServiceReference::getBundle).collect(Collectors.toSet());

        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the set of OSGi bundles containing the list of required headers key/value in the MANIFEST.MF file.
     * 
     * @param bundleContext
     *            the context of the bundle.
     * @param headers
     *            the list of key/value required headers
     * @return the set of bundles
     */

    public static Set<Bundle> getBundles(final BundleContext bundleContext, final Map<String, String> headers) {
        requireNonNull(bundleContext, "Bundle context cannot be null.");
        requireNonNull(headers, "Properties cannot be null.");

        return Stream.of(bundleContext.getBundles()).filter(contains(headers)).collect(Collectors.toSet());
    }

    private static Predicate<? super Bundle> contains(Map<String, String> properties) {
        return b -> {
            Dictionary<String, String> headers = b.getHeaders();

            Map<String, String> headersMap = Collections.list(headers.keys()).stream()
                    .collect(Collectors.toMap(Function.identity(), headers::get));

            return headersMap.entrySet().containsAll(properties.entrySet());
        };
    }

}
