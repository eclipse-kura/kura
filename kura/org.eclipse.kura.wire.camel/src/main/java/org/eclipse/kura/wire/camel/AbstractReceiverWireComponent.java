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
