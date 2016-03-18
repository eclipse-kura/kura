/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.camel.router;

import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.RoutesDefinition;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * Base class for Camel routers deployable into Eclipse Kura. All
 * RhiotKuraRouters are configurable components managable from the Everyware
 * cloud.
 */
public abstract class KuraRouter extends org.apache.camel.component.kura.KuraRouter implements ConfigurableComponent {

    // Members

    /**
     * Camel route XML, usually configured using SCR property.
     */
    protected String camelRouteXml;
    private Map<String, Object> m_properties;

    protected void updated(Map<String, Object> properties) {
        log.debug("Refreshing SCR properties: " + properties);
        refreshCamelRouteXml(camelRouteXml, (String) properties.get(org.eclipse.kura.camel.KuraConstants.XML_ROUTE_PROPERTY));
    }

    public void refreshCamelRouteXml(String oldCamelRouteXml, String newCamelRouteXml) {
        if (newCamelRouteXml != null && !newCamelRouteXml.equals(oldCamelRouteXml)) {
            this.camelRouteXml = newCamelRouteXml;
            if (!camelRouteXml.isEmpty()) {
                try {
                    RoutesDefinition routesDefinition = camelContext
                            .loadRoutesDefinition(new ByteArrayInputStream(camelRouteXml.getBytes()));
                    camelContext.addRouteDefinitions(routesDefinition.getRoutes());
                } catch (Exception e) {
                    log.warn("Cannot load routes definitions: {}", camelRouteXml);
                }
            }
        }
    }

    // ASF Camel workarounds

    // TODO: Remove this overridden method as soon as Camel 2.17 is out (see
    // CAMEL-9357)
    @Override
    public void configure() throws Exception {
        log.debug("No programmatic routes configuration found.");
    }

    // TODO: Remove this overridden method as soon as Camel 2.17 is out (see
    // CAMEL-9314)
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        try {
            super.start(bundleContext);
        } catch (Throwable e) {
            String errorMessage = "Problem when starting Kura module " + getClass().getName() + ":";
            log.warn(errorMessage, e);

            // Print error to the Kura console.
            System.err.println(errorMessage);
            e.printStackTrace();

            throw new RuntimeException(e);
        }
    }

    // TODO: Remove these overridden methods as soon as Camel 2.17 is out (see
    // CAMEL-9351)
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) throws Exception {
        m_properties = properties;
        start(componentContext.getBundleContext());
        updated(properties); // TODO Keep this line even when Camel 2.17 is out
    }

    protected void deactivate(ComponentContext componentContext) throws Exception {
        stop(componentContext.getBundleContext());
    }

    @Override
    protected void beforeStart(CamelContext camelContext) {
        PropertiesComponent pc = camelContext.getComponent(org.eclipse.kura.camel.KuraConstants.PROPERTIES_COMPONENT,
                PropertiesComponent.class);
        pc.addFunction(new KuraMetatypePropertiesFunction(m_properties));
    }

    protected void modified(Map<String, Object> properties) {
        m_properties = properties;
        try {
            camelContext.stop();
            beforeStart(camelContext);
            updated(m_properties);
            camelContext.addRoutes(this);
            camelContext.start();
        } catch (Exception e) {
            log.error("Cannot restart Kura RHIOT Camel Context", e);
        }
    }

}