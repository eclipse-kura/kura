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

package org.eclipse.kura.camel.component;

import static org.eclipse.kura.camel.component.Configuration.asString;

import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for implementing a {@link ConfigurableComponent} using
 * configured XML
 * <p>
 * This class intended to be subclasses and customized according to needs.
 * </p>
 * <p>
 * <strong>Note:</strong> This class is intended to be used as <em>OSGi Service
 * Component</em>. There the methods {@link #activate(BundleContext, Map)},
 * {@link #modified(Map)} and {@link #deactivate(BundleContext)} need to be
 * configured accordingly.
 * </p>
 * <p>
 * The lifecycle methods of this class declare annotations based on {@link org.osgi.service.component.annotations}.
 * However those annotations are only discovered during build time. They are declared in order
 * to provide proper support when annotation based tooling is used. Otherwise those methods must be
 * mapped manually in the DS declaration.
 * </p>
 */
public abstract class AbstractXmlCamelComponent extends AbstractCamelComponent implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(AbstractXmlCamelComponent.class);
    private final String xmlDataProperty;

    public AbstractXmlCamelComponent(final String xmlDataProperty) {
        Objects.requireNonNull(xmlDataProperty);

        this.xmlDataProperty = xmlDataProperty;
    }

    protected void activate(final BundleContext context, final Map<String, Object> properties) throws Exception {
        try {
            start(properties);

            // apply current routes
            applyRoutes(properties);
        } catch (Exception e) {
            logger.warn("Problem activating component", e);
            // we need to suppress exceptions during start
            // otherwise Kura cannot configure us anymore
        }
    }

    protected void deactivate(final BundleContext context) throws Exception {
        try {
            stop();
        } catch (Exception e) {
            logger.warn("Problem deactivating component", e);
            throw e;
        }
    }

    protected void modified(final Map<String, Object> properties) throws Exception {
        logger.debug("Updating properties: {}", properties);
        try {
            if (isRestartNeeded(properties)) {
                logger.info("Need restart");
                stop();
                start(properties);
            }

            // apply current routes

            applyRoutes(properties);
        } catch (Exception e) {
            logger.warn("Problem updating component", e);
            throw e;
        }
    }

    private void applyRoutes(final Map<String, Object> properties) throws Exception {
        this.runner.setRoutes(asString(properties, this.xmlDataProperty));
    }

    protected boolean isRestartNeeded(final Map<String, Object> properties) {
        return false;
    }

}
