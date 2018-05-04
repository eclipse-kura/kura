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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.eclipse.kura.camel.runner.CamelRunner;
import org.eclipse.kura.camel.runner.CamelRunner.Builder;
import org.eclipse.kura.camel.runner.ContextFactory;
import org.eclipse.kura.camel.runner.ContextLifecycleListener;
import org.eclipse.kura.util.base.StringUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base Camel component for the use inside of Kura
 * <p>
 * This class intended to be subclasses and customized according to needs.
 * </p>
 */
public abstract class AbstractCamelComponent {

    static final String PROP_DISABLE_JMX = "org.eclipse.kura.camel.component.disableJmx";

    private static final Logger logger = LoggerFactory.getLogger(AbstractCamelComponent.class);

    protected CamelRunner runner;

    private ServiceRegistration<CamelContext> registration;

    protected void start(final Map<String, Object> properties) throws Exception {
        logger.info("Starting camel router");

        final String kuraServiceId = Configuration.asString(properties, "camel.context.id");
        final String contextId;

        if (kuraServiceId == null) { // allow disabling by setting an empty string
            contextId = Configuration.asString(properties, "kura.service.pid");
        } else {
            contextId = kuraServiceId;
        }

        // create and configure

        final Builder builder = new CamelRunner.Builder(getBundleContext());
        builder.contextFactory(getContextFactory());
        builder.disableJmx(Boolean.getBoolean(PROP_DISABLE_JMX));
        builder.addBeforeStart(this::beforeStart);

        if (!StringUtil.isNullOrEmpty(kuraServiceId) || !StringUtil.isNullOrEmpty(contextId)) {
            builder.addLifecycleListener(new ContextLifecycleListener() {

                @Override
                public void started(final CamelContext camelContext) throws Exception {
                    AbstractCamelComponent.this.started(camelContext, kuraServiceId, contextId);
                }

                @Override
                public void stopping(final CamelContext camelContext) throws Exception {
                    AbstractCamelComponent.this.stopping();
                }

            });
        }

        customizeBuilder(builder, properties);

        this.runner = builder.build();

        // start

        this.runner.start();
    }

    protected void started(final CamelContext camelContext, final String kuraServicePid, final String contextId) {

        // ensure we are reported stopped

        stopping();

        // now start

        final Dictionary<String, Object> properties = new Hashtable<>();

        if (!StringUtil.isNullOrEmpty(kuraServicePid)) {
            properties.put("kura.service.pid", kuraServicePid);
        }

        if (!StringUtil.isNullOrEmpty(contextId)) {
            properties.put(Constants.SERVICE_PID, contextId);
            properties.put("camel.context.id", contextId);
        }

        // register

        this.registration = getBundleContext().registerService(CamelContext.class, camelContext, properties);

        logger.info("Registered camel context: {}", this.registration);
    }

    protected void stopping() {

        if (this.registration != null) {

            logger.info("Unregister camel context: {}", this.registration);

            this.registration.unregister();
            this.registration = null;
        }

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
        final CamelRunner runner = this.runner;
        return runner != null ? runner.getCamelContext() : null;
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
