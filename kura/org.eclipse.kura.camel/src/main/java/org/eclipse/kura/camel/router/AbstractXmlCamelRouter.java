/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - Refactor XML component
 *******************************************************************************/
package org.eclipse.kura.camel.router;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for implementing a custom XML based Camel router
 */
public abstract class AbstractXmlCamelRouter extends AbstractCamelRouter {
    private final static Logger logger = LoggerFactory.getLogger(AbstractXmlCamelRouter.class);

    private String currentRouteXml;

    private List<RouteDefinition> currentRoutes;

    /**
     * Update the route XML
     * 
     * @param routeXml
     *            the new route XML, may be {@code null}
     */
    protected void updateRouteXml(String routeXml) {
        if (!isChange(routeXml)) {
            logger.debug("This is not a change");
            return;
        }

        logger.debug("Change from:\n{}\n to:\n{}", this.currentRouteXml, routeXml);

        try {

            if (clearRoutesOnUpdate()) {
                // clear first
                logger.debug("Removing old routes first");
                clearExistingRoutes();
            }

            if (routeXml != null && !routeXml.isEmpty()) {
                logger.debug("Adding new routes");

                final InputStream bais = new ByteArrayInputStream(routeXml.getBytes());
                final RoutesDefinition routesDefinition = this.camelContext.loadRoutesDefinition(bais);

                // now add
                addRoutes(routesDefinition.getRoutes());
            }

            this.currentRouteXml = routeXml;
        } catch (Exception e) {
            logger.warn("Cannot load routes definitions: {}", routeXml);
            logger.warn("Error reason", e);
        }
    }

    private void addRoutes(final List<RouteDefinition> routes) throws Exception {
        this.currentRoutes = new ArrayList<>(routes);
        this.camelContext.addRouteDefinitions(currentRoutes);
    }

    private void clearExistingRoutes() throws Exception {
        final List<RouteDefinition> oldRoutes = this.currentRoutes;
        this.currentRoutes = null;
        if (oldRoutes != null) {
            if (logger.isDebugEnabled()) {
                for (RouteDefinition route : oldRoutes) {
                    logger.debug("\tRoute: {}", route.getId());
                }
            }
            this.camelContext.removeRouteDefinitions(oldRoutes);
        }
    }

    /**
     * Returns if existing routes should be cleared during an update
     * <p>
     * This method may be overridden by implementors if they want to the change
     * the update behavior
     * </p>
     * <p>
     * During the update process of XML routes this will influence if already
     * registered routes will be removed from the context before the new ones
     * will be registered
     * </p>
     * 
     * @return {@code true} if existing routes should be removed before adding
     *         the new ones, {@code false} otherwise
     */
    protected boolean clearRoutesOnUpdate() {
        return true;
    }

    /**
     * Test if the new route XML is a change to the current state
     * 
     * @param routeXml
     *            the new route XML
     * @return {@code true} if this would mean a change, {@code false} otherwise
     */
    private boolean isChange(String routeXml) {
        if (this.currentRouteXml == routeXml) {
            // also covers null == null
            return false;
        }

        if (this.currentRouteXml == null) {
            // routeXml is non null here
            return true;
        }

        return !this.currentRouteXml.equals(routeXml);
    }
}
