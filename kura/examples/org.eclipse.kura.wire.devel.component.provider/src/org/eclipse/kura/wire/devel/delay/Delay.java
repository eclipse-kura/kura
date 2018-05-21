/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.devel.delay;

import java.util.Map;
import java.util.Random;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Delay implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(Delay.class);

    private volatile WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private final Random random = new Random();
    private int delayAverage;
    private int delayStdDev;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = null;
    }

    @SuppressWarnings("unchecked")
    public void activate(final ComponentContext context, final Map<String, Object> properties) {
        logger.info("acitvating..");

        wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) context.getServiceReference());

        updated(properties);

        logger.info("activating...done");
    }

    public void deactivate() {
        logger.info("deactivating..");
        logger.info("deactivating...done");
    }

    public void updated(final Map<String, Object> properties) {
        logger.info("updating..");

        final DelayOptions options = new DelayOptions(properties);

        this.delayAverage = options.getAverageDelay();
        this.delayStdDev = options.getDelayStdDev();

        logger.info("updating...done");
    }

    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        final long delayMs = (long) (random.nextGaussian() * delayStdDev + delayAverage);

        if (delayMs > 0) {

            logger.info("sleeping for {} milliseconds", delayMs);

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        wireSupport.emit(wireEnvelope.getRecords());
    }

    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }
}
