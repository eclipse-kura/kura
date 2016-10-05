/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix generic types, Fix issue #599
 *******************************************************************************/
package org.eclipse.kura.web.server.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ServiceLocator {

    private static final ServiceLocator s_instance = new ServiceLocator();

    private ServiceLocator() {
    }

    public static ServiceLocator getInstance() {
        return s_instance;
    }

    public <T> ServiceReference<T> getServiceReference(Class<T> serviceClass) throws GwtKuraException {
        BundleContext bundleContext = Console.getBundleContext();
        ServiceReference<T> sr = null;
        if (bundleContext != null) {
            sr = bundleContext.getServiceReference(serviceClass);
        }
        if (sr == null) {
            throw GwtKuraException.internalError(serviceClass.toString() + " not found.");
        }
        return sr;
    }

    public <T> Collection<ServiceReference<T>> getServiceReferences(Class<T> serviceClass, String filter)
            throws GwtKuraException {
        final BundleContext bundleContext = Console.getBundleContext();
        Collection<ServiceReference<T>> sr = null;
        if (bundleContext != null) {
            try {
                sr = bundleContext.getServiceReferences(serviceClass, filter);
            } catch (InvalidSyntaxException e) {
                throw GwtKuraException.internalError("Getting service references failed.");
            }
        }
        if (sr == null) {
            throw GwtKuraException.internalError(serviceClass.toString() + " not found.");
        }
        return sr;
    }

    public <T> T getService(Class<T> serviceClass) throws GwtKuraException {
        T service = null;

        ServiceReference<T> sr = getServiceReference(serviceClass);
        if (sr != null) {
            service = getService(sr);
        }
        return service;
    }

    public interface ServiceFunction<T, R> {

        public R apply(T service);
    }

    /**
     * Locate a service and execute the provided function
     * <p>
     * The function will also be called if the service could not be found. It will be called with a {@code null}
     * argument in that case.
     * </p>
     *
     * @param serviceClass
     *            the service to locate
     * @param function
     *            the function to execute
     * @return the return value of the function
     */
    public static <T, R> R withOptionalService(final Class<T> serviceClass, final ServiceFunction<T, R> function) {
        final BundleContext ctx = FrameworkUtil.getBundle(ServiceLocator.class).getBundleContext();
        final ServiceReference<T> ref = ctx.getServiceReference(serviceClass);
        if (ref == null) {
            return function.apply(null);
        }

        final T service = ctx.getService(ref);
        try {
            return function.apply(service);
        } finally {
            ctx.ungetService(ref);
        }
    }

    public <T> T getService(ServiceReference<T> serviceReference) throws GwtKuraException {
        T service = null;
        BundleContext bundleContext = Console.getBundleContext();
        if (bundleContext != null && serviceReference != null) {
            service = bundleContext.getService(serviceReference);
        }
        if (service == null) {
            throw GwtKuraException.internalError("Service not found.");
        }
        return service;
    }

    public <T> List<T> getServices(Class<T> serviceClass) throws GwtKuraException {
        return getServices(serviceClass, null);
    }

    public <T> List<T> getServices(Class<T> serviceClass, String filter) throws GwtKuraException {
        List<T> services = null;

        BundleContext bundleContext = Console.getBundleContext();
        if (bundleContext != null) {
            Collection<ServiceReference<T>> serviceReferences = getServiceReferences(serviceClass, filter);

            if (serviceReferences != null) {
                services = new ArrayList<T>(serviceReferences.size());
                for (ServiceReference<T> sr : serviceReferences) {
                    services.add(getService(sr));
                }
            }
        }

        return services;
    }

    public boolean ungetService(ServiceReference<?> serviceReference) {
        BundleContext bundleContext = Console.getBundleContext();
        if (bundleContext != null && serviceReference != null) {
            return bundleContext.ungetService(serviceReference);
        }
        return false;
    }
}
