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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.cloud.CloudService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base camel router for the use inside of Kura
 */
public abstract class AbstractCamelRouter extends RouteBuilder {

    public static final String COMPONENT_NAME_KURA_CLOUD = "kura-cloud";

    private static final Logger logger = LoggerFactory.getLogger(AbstractCamelRouter.class);

    private CamelContext camelContext;

    protected void start() throws Exception {
        logger.info("Starting camel router");

        // create and configure

        final CamelContext camelContext = createCamelContext();

        registerFeatures(camelContext);
        configure(camelContext);
        beforeStart(camelContext);

        // assign an start

        this.camelContext = camelContext;
        this.camelContext.start();
    }

    protected void stop() throws Exception {
        logger.info("Stopping camel router");

        // stopping

        if (this.camelContext != null) {
            this.camelContext.stop();
            this.camelContext = null;
        }
    }

    protected BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(AbstractCamelRouter.class).getBundleContext();
    }

    protected CamelContext getCamelContext() {
        return this.camelContext;
    }

    /**
     * Create a new camel context
     * <p>
     * This defaults to creating an {@link OsgiDefaultCamelContext} with the
     * {@link BundleContext} provided by a call to {@link #getBundleContext()}.
     * </p>
     * 
     * @return a new {@link CamelContext} instead which is neither configured nor started
     */
    protected CamelContext createCamelContext() {
        return new OsgiDefaultCamelContext(getBundleContext());
    }

    /**
     * A method which can register additional features (like components)
     * 
     * @param camelContext
     *            the camel context to use
     */
    protected void registerFeatures(final CamelContext camelContext) {
        logger.debug("Registering features - context: {}", camelContext);
    }

    /**
     * Called to configure the camel context
     * <p>
     * This method is called after the context is created.
     * It defaults to a call to {@link CamelContext#addRoutes(org.apache.camel.RoutesBuilder)} with {@code this} as
     * argument.
     * </p>
     * 
     * @param camelContext
     * @throws Exception
     */
    protected void configure(final CamelContext camelContext) throws Exception {
        logger.debug("Configuring - context: {}", camelContext);
        camelContext.addRoutes(this);
    }

    /**
     * Called before the context is started
     * <p>
     * The default implementation sets a timeout for the shutdown strategy and disables JMX.
     * </p>
     * 
     * @param camelContext
     *            the context to configure
     */
    protected void beforeStart(final CamelContext camelContext) {
        logger.debug("Before start - context: {}", camelContext);
        camelContext.getShutdownStrategy().setTimeout(5);
        camelContext.disableJMX();
    }

    /**
     * Register a custom cloud service for the component name "kura-cloud"
     * <p>
     * <strong>Note: </strong> This method should be called from {@link #registerFeatures(CamelContext)} method
     * </p>
     * 
     * @param camelContext
     *            the camel context
     * @param cloudService
     *            the cloud service
     */
    protected void registerCloudService(final CamelContext camelContext, final CloudService cloudService) {
        logger.debug("Registering cloud service - context: {}, service: {}", camelContext, cloudService);

        Objects.requireNonNull(camelContext);
        Objects.requireNonNull(cloudService);

        final KuraCloudComponent component = new KuraCloudComponent(camelContext);
        component.setCloudService(cloudService);

        camelContext.addComponent(COMPONENT_NAME_KURA_CLOUD, component);
    }
}
