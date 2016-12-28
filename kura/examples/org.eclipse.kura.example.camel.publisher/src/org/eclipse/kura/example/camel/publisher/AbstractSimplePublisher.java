/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.example.camel.publisher;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.camel.component.Configuration;
import org.eclipse.kura.camel.runner.CamelRunner;
import org.eclipse.kura.camel.runner.CamelRunner.Builder;
import org.eclipse.kura.camel.runner.ServiceConsumer;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example publisher based on the Camel Java DSL
 */
public abstract class AbstractSimplePublisher implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSimplePublisher.class);

    /**
     * A RouterBuilder instance which has no routes
     */
    protected static final RouteBuilder NO_ROUTES = new RouteBuilder() {

        @Override
        public void configure() throws Exception {
        }
    };

    private CamelRunner camel;

    private String cloudServiceFilter;

    /**
     * Component activation
     *
     * @param properties
     *            initial properties
     * @throws Exception
     *             if something goes wrong
     */
    public void start(final Map<String, Object> properties) throws Exception {
        logger.info("Start: {}", properties);

        // create new filter and instance

        final String cloudServiceFilter = makeCloudServiceFilter(properties);
        this.camel = createCamelRunner(cloudServiceFilter);

        // set routes

        this.camel.setRoutes(fromProperties(properties));

        // start

        this.camel.start();
    }

    /**
     * Component update
     *
     * @param properties
     *            updated properties
     * @throws Exception
     *             if something goes wrong
     */
    public void updated(final Map<String, Object> properties) throws Exception {
        logger.info("Updating: {}", properties);

        final String cloudServiceFilter = makeCloudServiceFilter(properties);
        if (!this.cloudServiceFilter.equals(cloudServiceFilter)) {
            // update the routes and the filter

            // stop the camel context first
            this.camel.stop();

            // create a new camel runner, with new dependencies
            this.camel = createCamelRunner(cloudServiceFilter);

            // set the routes
            this.camel.setRoutes(fromProperties(properties));

            // and restart again
            this.camel.start();
        } else {
            // only update the routes, this is done without restarting the context

            this.camel.setRoutes(fromProperties(properties));
        }
    }

    /**
     * Component de-activation
     *
     * @throws Exception
     *             if something goes wrong
     */
    public void stop() throws Exception {
        if (this.camel != null) {
            this.camel.stop();
            this.camel = null;
        }
    }

    /**
     * Create a new camel runner with required dependencies
     *
     * @param cloudServiceFilter
     *            the filter for locating a cloud service filter
     * @return the new camel runner
     * @throws InvalidSyntaxException
     *             in case the cloud service filter had an illegal syntax
     */
    private CamelRunner createCamelRunner(final String cloudServiceFilter) throws InvalidSyntaxException {
        final BundleContext ctx = FrameworkUtil.getBundle(AbstractSimplePublisher.class).getBundleContext();

        this.cloudServiceFilter = cloudServiceFilter;

        // create a new camel CamelRunner.Builder

        final Builder builder = new CamelRunner.Builder();

        // add service dependency

        builder.dependOn(ctx, FrameworkUtil.createFilter(cloudServiceFilter),
                new ServiceConsumer<CloudService, CamelContext>() {

                    @Override
                    public void consume(final CamelContext context, final CloudService service) {
                        context.addComponent("cloud", new KuraCloudComponent(context, service));
                    }
                });

        // return un-started instance

        return builder.build();
    }

    /**
     * Construct an OSGi filter for a cloud service instance
     *
     * @param properties
     *            the properties to read from
     * @return the OSGi filter selecting the cloud service instance
     */
    private static String makeCloudServiceFilter(final Map<String, Object> properties) {
        final String filterPid = Configuration.asStringNotEmpty(properties, "cloudService",
                "org.eclipse.kura.cloud.CloudService");
        final String fullFilter = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudService.class.getName(), filterPid);
        return fullFilter;
    }

    /**
     * Create a new RouteBuilder instance from the properties
     *
     * @param properties
     *            the properties to read from
     * @return the new instance of RouteBuilder
     */
    protected abstract RouteBuilder fromProperties(Map<String, Object> properties);
}