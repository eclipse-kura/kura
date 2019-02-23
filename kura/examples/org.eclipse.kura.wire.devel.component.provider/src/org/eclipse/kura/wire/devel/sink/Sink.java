/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.devel.sink;

import static java.util.Objects.isNull;

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sink implements WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(Sink.class);

    private WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private boolean measureTimings;
    private long lastTimestamp;

    private String kuraServicePid;

    @SuppressWarnings("unchecked")
    public void activate(final ComponentContext context, final Map<String, Object> properties) {
        logger.info("activating...");

        this.kuraServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        this.wireSupport = wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) context.getServiceReference());
        updated(properties);

        logger.info("activating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");
        logger.info("deactivating...done");
    }

    public void updated(final Map<String, Object> properties) {
        logger.info("updating...");

        this.measureTimings = new SinkOptions(properties).shouldMeasureTimings();

        logger.info("updating...done");
    }

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        if (measureTimings) {
            final long currentTimestamp = System.currentTimeMillis();
            if (lastTimestamp != 0) {
                long diff = currentTimestamp - lastTimestamp;
                logger.info("{}: {} ms", this.kuraServicePid, diff);
            }
            lastTimestamp = currentTimestamp;
        }
    }
}
