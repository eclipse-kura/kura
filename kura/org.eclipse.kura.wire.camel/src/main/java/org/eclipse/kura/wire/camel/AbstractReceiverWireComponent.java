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

import org.apache.camel.CamelContext;
import org.eclipse.kura.util.base.StringUtil;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReceiverWireComponent extends AbstractEndpointWireComponent implements WireReceiver {

    private static final Logger logger = LoggerFactory.getLogger(AbstractReceiverWireComponent.class);

    @Override
    public void onWireReceive(final WireEnvelope envelope) {
        logger.debug("Received: {}", envelope);

        withContext(context -> {
            try {
                processReceive(context, envelope);
            } catch (final Exception e) {
                logger.warn("Failed to produce event", e);
            }
        });
    }

    private void processReceive(final CamelContext context, final WireEnvelope envelope) throws Exception {

        final String endpointUri = this.endpointUri;

        if (StringUtil.isNullOrEmpty(endpointUri)) {
            logger.debug("Endpoint missing. Component is disabled.");
            return;
        }

        processReceive(context, endpointUri, envelope);
    }

    protected abstract void processReceive(CamelContext context, String endpointUri, WireEnvelope envelope)
            throws Exception;
}
