/*******************************************************************************
(Object) * Copyright (c) 2018 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.wire.camel;

import static org.apache.camel.builder.DefaultFluentProducerTemplate.on;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.eclipse.kura.wire.WireEnvelope;

public class CamelProduce extends AbstractReceiverWireComponent {

    private FluentProducerTemplate template = null;

    @Override
    protected void processReceive(final CamelContext context, final String endpointUri, final WireEnvelope envelope)
            throws Exception {
        if (this.template == null) {
            return;
        }
        this.template //
                .withBody(envelope) //
                .to(endpointUri) //
                .asyncSend();
    }

    @Override
    protected void bindContext(final CamelContext context) {

        this.template = on(context);

    }
}
