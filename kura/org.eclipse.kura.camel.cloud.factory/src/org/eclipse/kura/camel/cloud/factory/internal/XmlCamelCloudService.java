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

import java.io.ByteArrayInputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.camel.ServiceStatus;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.eclipse.kura.camel.camelcloud.DefaultCamelCloudService;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.cloud.CloudService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlCamelCloudService {
    private static final Logger logger = LoggerFactory.getLogger(XmlCamelCloudService.class);

    private BundleContext context;

    private String xml;

    private DefaultCamelCloudService service;

    private OsgiDefaultCamelContext router;

    private String pid;

    public XmlCamelCloudService(final BundleContext context, final String xml, String pid) {
        this.context = context;
        this.xml = xml;
        this.pid = pid;
    }

    public void start() throws Exception {

        // new router

        this.router = new OsgiDefaultCamelContext(this.context);

        // new cloud service
        
        this.service = new DefaultCamelCloudService(this.router);
        
        // set up

        final KuraCloudComponent cloudComponent = new KuraCloudComponent(this.router);
        cloudComponent.setCloudService(this.service);
        this.router.addComponent("kura-cloud", cloudComponent);
        
        final RoutesDefinition routesDefinition = this.router.loadRoutesDefinition(new ByteArrayInputStream(this.xml.getBytes()));
        this.router.addRouteDefinitions(routesDefinition.getRoutes());

        // start

        logger.debug("Starting router...");
        this.router.start();
        final ServiceStatus status = this.router.getStatus();
        logger.debug("Starting router... {} ({}, {})", status, status == Started, this.service.isConnected());

        // register

        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("cloud.service.pid", this.pid);
        props.put(Constants.SERVICE_PID, this.pid + "-CloudService");
        props.put("kura.service.pid", this.pid + "-CloudService");
        this.context.registerService(CloudService.class, this.service, props);
    }

    public void stop() throws Exception {
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
