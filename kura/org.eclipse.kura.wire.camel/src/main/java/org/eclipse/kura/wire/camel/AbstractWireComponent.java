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

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for creating wire components.
 */
public abstract class AbstractWireComponent implements WireComponent, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWireComponent.class);

    private WireHelperService wireHelperService;

    protected WireSupport wireSupport;

    public void setWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    @SuppressWarnings("unchecked")
    protected synchronized void activate(final ComponentContext componentContext, final Map<String, ?> properties)
            throws Exception {
        if (this.wireSupport == null) {
            this.wireSupport = this.wireHelperService.newWireSupport(this,
                    (ServiceReference<WireComponent>) componentContext.getServiceReference());
        }
    }

    protected void modified(final ComponentContext componentContext, final Map<String, ?> properties) throws Exception {
    }

    protected synchronized void deactivate() {
        /*
         * We must not close the wireSupport instance here as we do implement
         * modify by calls to deactivate/activate and thus we would loose all
         * information of exiting wirings.
         */
    }

    /*
     * For subclasses implementing WireReceiver
     */
    public synchronized void updated(final Wire wire, final Object value) {
        logger.debug("Updated: {}", wire);

        this.wireSupport.updated(wire, value);
    }

    /*
     * For subclasses implementing WireReceiver
     */
    public synchronized void producersConnected(final Wire[] wires) {
        logger.info("Producers connected - {}", (Object) wires);
        this.wireSupport.producersConnected(wires);
    }

    /*
     * For subclasses implementing WireEmitter
     */
    public synchronized Object polled(final Wire wire) {
        logger.debug("Polled: {}", wire);
        return this.wireSupport.polled(wire);
    }

    /*
     * For subclasses implementing WireEmitter
     */
    public synchronized void consumersConnected(final Wire[] wires) {
        logger.info("Consumers connected - {}", (Object) wires);
        this.wireSupport.consumersConnected(wires);
    }

    /*
     * For subclasses implementing WireReceiver
     */
    public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {
        logger.debug("onWireReceive: {}", wireEnvelope);
    }
}