/*******************************************************************************
 * Copyright (c) 2018, 2020 Red Hat Inc and others
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

import java.util.Arrays;

import org.apache.camel.CamelContext;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelProcess extends AbstractReceiverWireComponent implements WireEmitter {

    private static final Logger logger = LoggerFactory.getLogger(CamelProcess.class);

    @Override
    protected void processReceive(final CamelContext context, final String endpointUri, final WireEnvelope envelope)
            throws Exception {

        final WireRecord[] result = on(context) //
                .withBody(envelope) //
                .to(endpointUri) //
                .request(WireRecord[].class);

        logger.debug("Result: {}", (Object) result);

        if (result != null) {
            this.wireSupport.emit(Arrays.asList(result));
        }
    }

}
