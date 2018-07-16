/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.camel.publisher;

import static org.eclipse.kura.camel.component.Configuration.asBoolean;

import java.util.Date;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.kura.message.KuraPayload;

public abstract class AbstractSimplePeriodicPublisher<T> extends AbstractSimplePublisher {

    private final String appId;

    protected abstract T parseConfiguration(Map<String, Object> properties);

    protected abstract Map<String, Object> getPayload(T configuration);

    public AbstractSimplePeriodicPublisher(final String appId) {
        this.appId = appId;
    }

    /**
     * Create a default route which periodically polls for data
     */
    @Override
    protected RouteBuilder fromProperties(final Map<String, Object> properties) {

        // we are disabled, to remove all routes

        if (!asBoolean(properties, "enabled")) {
            return NO_ROUTES;
        }

        // parse new configuration

        final T configuration = parseConfiguration(properties);

        // return new router builder

        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                from("timer://heartbeat").id("payload").process(new Processor() {

                    @Override
                    public void process(final Exchange exchange) throws Exception {
                        // get new payload for this run
                        final Map<String, Object> payload = getPayload(configuration);

                        // create new Kura payload structure
                        final KuraPayload kuraPayload = new KuraPayload();
                        kuraPayload.setTimestamp(new Date());

                        // set map of data
                        for (final Map.Entry<String, Object> entry : payload.entrySet()) {
                            kuraPayload.addMetric(entry.getKey(), entry.getValue());
                        }

                        // set camel exchange data
                        exchange.getIn().setBody(kuraPayload);
                    }
                }).to("cloud:" + AbstractSimplePeriodicPublisher.this.appId);
            }
        };
    }

}
