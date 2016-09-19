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

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for implementing a {@link ConfigurableComponent} using
 * configured XML
 * <p>
 * <strong>Note:</strong> If you wish your service to be picked up by Kura's configuration
 * service, then you implementation must also implement {@link ConfigurableComponent} in addition.
 * </p>
 * <p>
 * <strong>Note:</strong> This class is intended to be used as <em>OSGi Service
 * Component</em>. There the methods {@link #activate(BundleContext, Map)},
 * {@link #modified(Map)} and {@link #deactivate(BundleContext)} need to be
 * configured accordingly.
 * </p>
 */
public abstract class AbstractXmlCamelComponent extends AbstractXmlCamelRouter {
    private final static Logger logger = LoggerFactory.getLogger(AbstractXmlCamelComponent.class);
    private final String xmlDataProperty;

    public AbstractXmlCamelComponent(final String xmlDataProperty) {
        this.xmlDataProperty = xmlDataProperty;
    }

    protected void activate(final BundleContext context, final Map<String, Object> properties) throws Exception {
        try {
            start();
            modified(properties);
        } catch (Exception e) {
            logger.warn("Problem activating component", e);
            throw e;
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

    protected void modified(final Map<String, Object> properties) {
        logger.debug("Updating properties: {}", properties);

        Object value = properties.get(this.xmlDataProperty);

        logger.debug("XML value - before: {}", value);

        if (value instanceof String[]) {
            String[] values = (String[]) value;
            value = values.length > 0 ? values[0] : null;
        } else if (!(value instanceof String)) {
            value = null;
        }

        logger.debug("XML value - after: {}", value);

        updateRouteXml((String) value);
    }

}
