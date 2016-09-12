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
package org.eclipse.kura.example.camel.quickstart;

import org.apache.camel.CamelContext;
import org.eclipse.kura.camel.router.AbstractXmlCamelComponent;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;

/**
 * Example of a Kura Camel application based on the Camel XML schema
 */
public class GatewayRouterXml extends AbstractXmlCamelComponent implements ConfigurableComponent {

    private CloudService cloudService;

    public void setCloudService(final CloudService cloudService) {
        this.cloudService = cloudService;
    }

    @Override
    protected void beforeStart(CamelContext camelContext) {
        super.beforeStart(camelContext);
        
        // registering a custom CloudService instance
        registerCloudService(camelContext, this.cloudService);
    }
    
    public GatewayRouterXml() {
        super ("camel.route.xml");
    }
}