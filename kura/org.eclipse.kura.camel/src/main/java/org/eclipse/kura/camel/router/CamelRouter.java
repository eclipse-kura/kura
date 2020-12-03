/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.camel.router;

import java.util.Map;
import java.util.Objects;

import org.apache.camel.CamelContext;
import org.eclipse.kura.camel.component.AbstractXmlCamelComponent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * The class provides a compatibility layer for the older API
 *
 * @deprecated Use the {@link org.eclipse.kura.camel.runner.CamelRunner}, one of the abstract implementations from
 *             {@link org.eclipse.kura.camel.component} or the native
 *             {@link org.apache.camel.core.osgi.OsgiDefaultCamelContext}
 */
@Deprecated
public abstract class CamelRouter extends AbstractXmlCamelComponent implements BundleActivator {

    protected CamelContext camelContext;

    public CamelRouter() {
        super("camel.route.xml");
    }

    @Override
    protected void activate(BundleContext context, Map<String, Object> properties) throws Exception {
        super.activate(context, properties);
        this.camelContext = getCamelContext();
    }

    @Override
    protected void deactivate(BundleContext context) throws Exception {
        this.camelContext = null;
        super.deactivate(context);
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
