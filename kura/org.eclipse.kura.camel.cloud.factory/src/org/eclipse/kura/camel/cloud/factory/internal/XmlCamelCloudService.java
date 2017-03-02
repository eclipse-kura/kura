/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.cloud.factory.internal;

import static org.apache.camel.ServiceStatus.Started;
import static org.eclipse.kura.camel.cloud.factory.internal.CamelCloudServiceFactory.PID;
import static org.eclipse.kura.camel.cloud.factory.internal.CamelFactory.FACTORY_ID;
import static org.eclipse.kura.camel.utils.CamelContexts.scriptInitCamelContext;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.io.ByteArrayInputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.script.ScriptException;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiServiceRegistry;
import org.apache.camel.impl.CompositeRegistry;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.model.RoutesDefinition;
import org.eclipse.kura.camel.bean.PayloadFactory;
import org.eclipse.kura.camel.camelcloud.DefaultCamelCloudService;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.cloud.CloudService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An service managing a single Camel context as {@link CloudService}
 * <p>
 * This service component does manage the lifecycle of a single {@link DefaultCamelCloudService}
 * instance. It will instantiate the Camel context and register the {@link CloudService} instance
 * with OSGi.
 * </p>
 */
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

        // new registry

        final SimpleRegistry simpleRegistry = new SimpleRegistry();
        simpleRegistry.put("payloadFactory", new PayloadFactory());

        final CompositeRegistry registry = new CompositeRegistry();
        registry.addRegistry(new OsgiServiceRegistry(this.context));
        registry.addRegistry(simpleRegistry);

        // new router

        this.router = new OsgiDefaultCamelContext(this.context, registry);
        if (!configuration.isEnableJmx()) {
            this.router.disableJMX();
        }

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
        props.put(SERVICE_PID, this.pid);
        props.put("service.factoryPid", FACTORY_ID);
        props.put(KURA_SERVICE_PID, this.pid);
        props.put("kura.cloud.service.factory.pid", PID);

        this.handle = this.context.registerService(CloudService.class, this.service, props);
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

    private void callInitCode(final CamelContext router) throws ScriptException {
        scriptInitCamelContext(router, configuration.getInitCode(), XmlCamelCloudService.class.getClassLoader());
    }

}
