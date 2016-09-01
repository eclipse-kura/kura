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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.eclipse.kura.camel.camelcloud.DefaultCamelCloudService;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.camel.router.CamelRouter;
import org.eclipse.kura.message.KuraPayload;

import java.util.Random;

/**
 * Example of the Kura Camel application.
 */
public class GatewayRouter extends CamelRouter {

    @Override
    public void configure() throws Exception {
        KuraCloudComponent cloudComponent = new KuraCloudComponent();
        cloudComponent.setCloudService(new DefaultCamelCloudService(camelContext));
        camelContext.addComponent("kura-cloud", cloudComponent);

        from("timer://heartbeat").
                process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        KuraPayload payload = new KuraPayload();
                        payload.addMetric("temperature", new Random().nextInt(20));
                        exchange.getIn().setBody(payload);
                    }
                }).to("kura-cloud:myapp/topic");

        from("kura-cloud:myapp/topic").
                choice().
                  when(simple("${body.metrics()[temperature]} < 10"))
                .to("log:lessThanTen")
                  .when(simple("${body.metrics()[temperature]} == 10"))
                  .to("log:equalToTen")
                .otherwise()
                .to("log:greaterThanTen");

        from("timer://xmltopic").
                process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        KuraPayload payload = new KuraPayload();
                        payload.addMetric("temperature", new Random().nextInt(20));
                        exchange.getIn().setBody(payload);
                    }
                }).to("kura-cloud:myapp/xmltopic");
    }

}