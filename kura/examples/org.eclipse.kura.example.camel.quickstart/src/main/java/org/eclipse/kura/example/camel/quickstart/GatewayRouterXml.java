/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
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