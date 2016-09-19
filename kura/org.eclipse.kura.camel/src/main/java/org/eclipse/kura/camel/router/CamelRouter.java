/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - Initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.router;

import java.util.Objects;

import org.apache.camel.CamelContext;
import org.eclipse.kura.camel.RouterConstants;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * @deprecated use either {@link AbstractXmlCamelComponent} or {@link AbstractCamelRouter} as a base class
 */
@Deprecated
public abstract class CamelRouter extends AbstractXmlCamelComponent implements ConfigurableComponent, BundleActivator {
    protected CamelContext camelContext;

    public CamelRouter() {
        super(RouterConstants.XML_ROUTE_PROPERTY);
    }
    
    @Override
    protected CamelContext createCamelContext() {
        this.camelContext = super.createCamelContext();
        return this.camelContext;
    }

    protected <T> T service(Class<T> serviceType) {
        Objects.requireNonNull(serviceType);
        
        final ServiceReference<T> reference = getBundleContext().getServiceReference(serviceType);
        return reference == null ? null : getBundleContext().getService(reference);
    }

    protected <T> T requiredService(final Class<T> serviceType) {
        Objects.requireNonNull(serviceType);
        
        final ServiceReference<T> reference = getBundleContext().getServiceReference(serviceType);
        if (reference == null) {
            throw new IllegalStateException("Cannot find service: " + serviceType.getName());
        }
        
        return getBundleContext().getService(reference);
    }

    protected String camelXmlRoutesPid() {
        return "kura.camel";
    }

    protected String camelXmlRoutesProperty() {
        return "kura.camel." + FrameworkUtil.getBundle(this.getClass()).getSymbolicName() + ".route";
    }
}
