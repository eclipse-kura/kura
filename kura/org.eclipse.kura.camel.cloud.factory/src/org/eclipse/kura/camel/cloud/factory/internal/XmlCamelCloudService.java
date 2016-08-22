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

import java.io.ByteArrayInputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.camel.CamelContext;
import org.apache.camel.component.kura.KuraRouter;
import org.apache.camel.model.RoutesDefinition;
import org.eclipse.kura.camel.camelcloud.DefaultCamelCloudService;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.cloud.CloudService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class XmlCamelCloudService {
    private BundleContext context;

    private String xml;

    private DefaultCamelCloudService service;

    private KuraRouter router;

    private String pid;

    public XmlCamelCloudService(final BundleContext context, final String xml, String pid) {
        this.context = context;
        this.xml = xml;
        this.pid = pid;
    }

    public void start() throws Exception {
        this.router = new KuraRouter() {
            protected void beforeStart(CamelContext camelContext) {
                camelContext.getShutdownStrategy().setTimeout(5);
                camelContext.disableJMX();
            };
        };

        this.service = new DefaultCamelCloudService(this.router.getContext());

        KuraCloudComponent cloudComponent = new KuraCloudComponent(this.router.getContext());
        cloudComponent.setCloudService(this.service);
        this.router.getContext().addComponent("kura-cloud", cloudComponent);

        final RoutesDefinition routesDefinition = this.router.getContext().loadRoutesDefinition(new ByteArrayInputStream(this.xml.getBytes()));
        this.router.getContext().addRouteDefinitions(routesDefinition.getRoutes());

        // register

        this.router.start(this.context);

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
            this.router.stop(context);
            this.router = null;
        }

    }
}
