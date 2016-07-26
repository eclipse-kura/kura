/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ServiceLocator {
    private static final ServiceLocator s_instance = new ServiceLocator();

    private ServiceLocator() {}


    public static ServiceLocator getInstance() {
        return s_instance;
    }

    @SuppressWarnings("rawtypes")
    public ServiceReference getServiceReference(Class<?> serviceClass) throws GwtKuraException {
        BundleContext bundleContext = Console.getBundleContext();
        ServiceReference sr = null;
        if (bundleContext != null) {
            sr = bundleContext.getServiceReference(serviceClass);
        }
        if (sr == null) {
            throw GwtKuraException.internalError(serviceClass.toString() + " not found.");
        }
        return sr;
    }

    @SuppressWarnings("rawtypes")
    public ServiceReference[] getServiceReferences(Class<?> serviceClass, String filter) throws GwtKuraException {
        BundleContext bundleContext = Console.getBundleContext();
        ServiceReference[] sr = null;
        if (bundleContext != null) {
            try {
                sr = bundleContext.getServiceReferences(serviceClass.getName(), filter);
            } catch (InvalidSyntaxException e) {
                throw GwtKuraException.internalError("Getting service references failed.");
            }
        }
        if (sr == null) {
            throw GwtKuraException.internalError(serviceClass.toString() + " not found.");
        }
        return sr;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T getService(Class<T> serviceClass) throws GwtKuraException {
        T service = null; 

        ServiceReference sr = getServiceReference(serviceClass);
        if (sr != null) {
            service = (T) getService(sr);
        }
        return service;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T getService(ServiceReference serviceReference) throws GwtKuraException {
        T service = null; 
        BundleContext bundleContext = Console.getBundleContext();
        if (bundleContext != null && serviceReference != null) {
            service = (T) bundleContext.getService(serviceReference);
        }
        if (service == null) {
            throw GwtKuraException.internalError("Service not found.");
        }
        return service;
    }

    public <T> List<T> getServices(Class<T> serviceClass) throws GwtKuraException {
        return getServices(serviceClass, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> List<T> getServices(Class<T> serviceClass, String filter) throws GwtKuraException {
        List<T> services = null;

        BundleContext bundleContext = Console.getBundleContext();
        if (bundleContext != null) {
            ServiceReference[] serviceReferences= getServiceReferences(serviceClass, filter);

            if (serviceReferences != null) {
                services= new ArrayList<T>(serviceReferences.length);
                for (ServiceReference sr : serviceReferences) {
                    services.add((T) getService(sr));
                }
            }
        }

        return services;
    }
    
    @SuppressWarnings("rawtypes")
    public boolean ungetService(ServiceReference serviceReference) {
        BundleContext bundleContext = Console.getBundleContext();
        if (bundleContext != null && serviceReference != null) {
            return bundleContext.ungetService(serviceReference);
        }
        return false;
    }
}
