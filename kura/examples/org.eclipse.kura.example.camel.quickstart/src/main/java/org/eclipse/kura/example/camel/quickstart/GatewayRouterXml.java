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

import static org.eclipse.kura.camel.cloud.KuraCloudComponent.DEFAULT_NAME;

import org.apache.camel.CamelContext;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.camel.component.AbstractXmlCamelComponent;
import org.eclipse.kura.cloud.CloudService;

/**
 * Example of a Kura Camel application based on the Camel XML schema
 * <p>
 * This example shows the use of the XML based camel component. Camel routes will
 * be provided externally using the Kura configuration mechanism. The XML data is ready
 * from the property "camel.route.xml".
 * </p>
 */
public class GatewayRouterXml extends AbstractXmlCamelComponent {

    private CloudService cloudService;

    public GatewayRouterXml() {
        super("camel.route.xml");
    }

    public void setCloudService(final CloudService cloudService) {
        this.cloudService = cloudService;
    }

    @Override
    protected void beforeStart(final CamelContext camelContext) {
        camelContext.addComponent(DEFAULT_NAME, new KuraCloudComponent(camelContext, this.cloudService));
    }

}