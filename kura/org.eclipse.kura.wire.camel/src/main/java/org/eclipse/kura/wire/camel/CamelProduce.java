/*******************************************************************************
 * Copyright (c) 2018, 2022 Red Hat Inc and others
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
package org.eclipse.kura.wire.camel;

import static org.apache.camel.builder.DefaultFluentProducerTemplate.on;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.eclipse.kura.wire.WireEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelProduce extends AbstractReceiverWireComponent {

    private static final Logger logger = LoggerFactory.getLogger(CamelProduce.class);

    private FluentProducerTemplate template = null;

    @Override
    protected void processReceive(final CamelContext context, final String endpointUri, final WireEnvelope envelope)
            throws Exception {
        if (template == null) {
            template = on(context);
        } else {
            if (template.getCamelContext() != context) {
                template.stop();
                template = on(context);
            }
        }
        template //
                .withBody(envelope) //
                .to(endpointUri) //
                .asyncSend();
    }

    @Override
    protected void deactivate() {
        super.deactivate();
        if (template != null) {
            try {
                template.stop();
            } catch (Exception e) {
                logger.warn("Failed to stop ProducerTemplate", e);
            } finally {
                template = null;
            }
        }
    }

}
