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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.kura.camel.CamelRunner;
import org.eclipse.kura.camel.CamelRunner.BeforeStart;
import org.eclipse.kura.camel.CamelRunner.Builder;
import org.eclipse.kura.camel.ContextFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJavaCamelComponent extends RouteBuilder {

    private final static Logger logger = LoggerFactory.getLogger(AbstractJavaCamelComponent.class);

    protected CamelRunner runner;

    protected void start() throws Exception {
        logger.info("Starting camel router");

        // create and configure

        final Builder builder = new CamelRunner.Builder();
        builder.contextFactory(getContextFactory());
        builder.addBeforeStart(new BeforeStart() {

            @Override
            public void beforeStart(final CamelContext camelContext) throws Exception {
                AbstractJavaCamelComponent.this.beforeStart(camelContext);
                camelContext.addRoutes(AbstractJavaCamelComponent.this);
            }
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

    protected void beforeStart(final CamelContext camelContext) {
    }

    protected ContextFactory getContextFactory() {
        return CamelRunner.createOsgiFactory(getBundleContext());
    }

    protected BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(AbstractCamelComponent.class).getBundleContext();
    }
}
