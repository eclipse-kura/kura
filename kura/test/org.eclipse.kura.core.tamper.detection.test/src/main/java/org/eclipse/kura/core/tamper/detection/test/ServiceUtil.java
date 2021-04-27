/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.tamper.detection.test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
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
}
