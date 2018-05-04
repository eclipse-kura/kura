/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc and others
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

import static java.lang.Boolean.getBoolean;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.kura.camel.runner.CamelRunner;
import org.eclipse.kura.camel.runner.CamelRunner.Builder;
import org.eclipse.kura.camel.runner.ContextFactory;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for implementing a {@link ConfigurableComponent} using
 * the Java DSL
 * <p>
 * This class intended to be subclasses and customized according to needs.
 * </p>
 * <p>
 * <strong>Note:</strong> This class is intended to be used as <em>OSGi Service
 * Component</em>. The methods {@link #start()} and {@link #stop()} have to be configured
 * accordingly.
 * </p>
 * <p>
 * The lifecycle methods of this class declare annotations based on {@link org.osgi.service.component.annotations}.
 * However those annotations are only discovered during build time. They are declared in order
 * to provide proper support when annotation based tooling is used. Otherwise those methods must be
 * mapped manually in the DS declaration.
 * </p>
 */
public abstract class AbstractJavaCamelComponent extends RouteBuilder implements ConfigurableComponent {

    private static final String PROP_DISABLE_JMX = AbstractCamelComponent.PROP_DISABLE_JMX;

    private final static Logger logger = LoggerFactory.getLogger(AbstractJavaCamelComponent.class);

    protected CamelRunner runner;

    protected void start() throws Exception {
        logger.info("Starting camel router");

        // create and configure

        final Builder builder = new CamelRunner.Builder();
        builder.contextFactory(getContextFactory());
        builder.disableJmx(getBoolean(PROP_DISABLE_JMX));
        builder.addBeforeStart(camelContext -> {
            beforeStart(camelContext);
            camelContext.addRoutes(AbstractJavaCamelComponent.this);
        });

        this.runner = builder.build();

        // start

        this.runner.start();
    }

    protected void stop() throws Exception {
        logger.info("Stopping camel router");

        // stopping

        if (this.runner != null) {
            this.runner.stop();
            this.runner = null;
        }
    }

    /**
     * Called before the context is started
     *
     * @param camelContext
     *            the Camel context which is being prepared for starting
     */
    protected void beforeStart(final CamelContext camelContext) {
    }

    protected ContextFactory getContextFactory() {
        return CamelRunner.createOsgiFactory(getBundleContext());
    }

    protected BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(AbstractCamelComponent.class).getBundleContext();
    }
}
