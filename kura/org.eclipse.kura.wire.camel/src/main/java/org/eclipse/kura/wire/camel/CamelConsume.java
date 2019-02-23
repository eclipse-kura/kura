/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.wire.camel;

import static java.util.Arrays.asList;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelConsume extends AbstractEndpointWireComponent implements WireEmitter {

    private static final Logger logger = LoggerFactory.getLogger(CamelConsume.class);

    private Endpoint endpoint;
    private Consumer consumer;

    private CamelContext camelContext;

    @Override
    protected void bindContext(final CamelContext context) {

        try {
            stopConsumer();
            this.camelContext = context;
            startConsumer();
        } catch (Exception e) {
            logger.warn("Failed to bind Camel context", e);
        }

    }

    @Override
    public void setEndpointUri(final String endpointUri) {
        if (this.endpointUri == null || !this.endpointUri.equals(endpointUri))
            try {
                stopConsumer();
                super.setEndpointUri(endpointUri);
                startConsumer();
            } catch (final Exception e) {
                logger.warn("Failed to set endpoint URI", e);
            }
    }

    private void startConsumer() throws Exception {
        if (this.camelContext == null) {
            return;
        }

        if (this.endpoint == null) {
            logger.info("Starting endpoint");
            this.endpoint = this.camelContext.getEndpoint(this.endpointUri);
            this.endpoint.start();
        }

        if (this.consumer == null) {
            logger.info("Starting consumer");
            this.consumer = this.endpoint.createConsumer(new Processor() {

                @Override
                public void process(final Exchange exchange) throws Exception {
                    processMessage(exchange.getIn());
                }
            });
            this.consumer.start();
        }

    }

    private void stopConsumer() throws Exception {

        if (this.consumer != null) {
            logger.info("Stopping consumer");
            this.consumer.stop();
            this.consumer = null;
        }

        if (this.endpoint != null) {
            logger.info("Stopping endpoint");
            this.endpoint.stop();
            this.endpoint = null;
        }
    }

    private void processMessage(final Message message) {
        logger.debug("Process message: {}", message);

        final WireRecord[] records = message.getBody(WireRecord[].class);

        logger.debug("Consumed: {}", (Object) records);

        if (records != null) {
            wireSupport.emit(asList(records));
        }
    }
}
