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

import java.util.Map;

import org.apache.camel.CamelContext;
import org.eclipse.kura.camel.runner.BeforeStart;
import org.eclipse.kura.camel.runner.CamelRunner;
import org.eclipse.kura.camel.runner.CamelRunner.Builder;
import org.eclipse.kura.camel.runner.ContextFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base Camel component for the use inside of Kura
 * <p>
 * This class intended to be subclasses and customized according to needs.
 * </p>
 */
public abstract class AbstractCamelComponent {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCamelComponent.class);

    protected CamelRunner runner;

    protected void start(final Map<String, Object> properties) throws Exception {
        logger.info("Starting camel router");

        // create and configure

        final Builder builder = new CamelRunner.Builder();
        builder.contextFactory(getContextFactory());
        builder.addBeforeStart(new BeforeStart() {

            @Override
            public void beforeStart(final CamelContext camelContext) {
                AbstractCamelComponent.this.beforeStart(camelContext);
            }
        });

        customizeBuilder(builder, properties);

        this.runner = builder.build();

        // start

        this.runner.start();
    }

    /**
     * Customize the builder before it creates the runner
     * <br>
     * The default implementation is empty
     *
     * @param builder
     *            the builder
     * @param properties
     *            the properties provided to the {@link #start(Map)} method
     */
    protected void customizeBuilder(final Builder builder, final Map<String, Object> properties) {
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
     * Get the Camel context
     *
     * @return the camel context or {@code null} if the context is not started
     */
    public CamelContext getCamelContext() {
        return this.runner.getCamelContext();
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
