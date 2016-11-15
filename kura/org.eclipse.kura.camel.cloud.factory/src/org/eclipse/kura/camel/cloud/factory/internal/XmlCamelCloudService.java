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
package org.eclipse.kura.camel.cloud.factory.internal;

import static org.apache.camel.ServiceStatus.Started;
import static org.eclipse.kura.camel.runner.ScriptRunner.create;

import java.io.ByteArrayInputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.camel.ServiceStatus;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.eclipse.kura.camel.camelcloud.DefaultCamelCloudService;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.camel.runner.ScriptRunner;
import org.eclipse.kura.cloud.CloudService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlCamelCloudService {

    private static final Logger logger = LoggerFactory.getLogger(XmlCamelCloudService.class);

    private final BundleContext context;

    private final String pid;

    private final ServiceConfiguration configuration;

    private DefaultCamelCloudService service;

    private OsgiDefaultCamelContext router;

    private ServiceRegistration<CloudService> handle;

    public XmlCamelCloudService(final BundleContext context, final String pid,
            final ServiceConfiguration configuration) {
        this.context = context;
        this.pid = pid;
        this.configuration = configuration;
    }

    public void start() throws Exception {

        // new router

        this.router = new OsgiDefaultCamelContext(this.context);

        // call init code

        callInitCode(this.router);

        // new cloud service

        this.service = new DefaultCamelCloudService(this.router);

        // set up

        final KuraCloudComponent cloudComponent = new KuraCloudComponent(this.router, this.service);
        this.router.addComponent("kura-cloud", cloudComponent);

        final RoutesDefinition routesDefinition = this.router
                .loadRoutesDefinition(new ByteArrayInputStream(this.configuration.getXml().getBytes()));
        this.router.addRouteDefinitions(routesDefinition.getRoutes());

        // start

        logger.debug("Starting router...");
        this.router.start();
        final ServiceStatus status = this.router.getStatus();
        logger.debug("Starting router... {} ({}, {})", status, status == Started, this.service.isConnected());

        // register

        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, this.pid);
        props.put("kura.service.pid", this.pid);

        if (this.configuration.getServiceRanking() != null) {
            props.put(Constants.SERVICE_RANKING, this.configuration.getServiceRanking());
        }

        this.handle = this.context.registerService(CloudService.class, this.service, props);
    }

    private void callInitCode(OsgiDefaultCamelContext router) throws ScriptException {
        if (this.configuration.getInitCode() == null || this.configuration.getInitCode().isEmpty()) {
            return;
        }

        try {

            // setup runner

            final ScriptRunner runner = create(XmlCamelCloudService.class.getClassLoader(), "JavaScript",
                    this.configuration.getInitCode());

            // setup arguments

            final SimpleBindings bindings = new SimpleBindings();
            bindings.put("camelContext", router);

            // perform call

            runner.run(bindings);
        } catch (final Exception e) {
            logger.warn("Failed to run init code", e);
            throw e;
        }
    }

    public void stop() throws Exception {
        if (this.handle != null) {
            this.handle.unregister();
            this.handle = null;
        }
        if (this.service != null) {
            this.service.dispose();
            this.service = null;
        }
        if (this.router != null) {
            this.router.stop();
            this.router = null;
        }

    }
}
