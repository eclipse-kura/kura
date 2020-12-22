/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.trig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrigonometricComponent implements WireEmitter, ConfigurableComponent, WireReceiver {

    private static final Logger logger = LoggerFactory.getLogger(TrigonometricComponent.class);

    private WireHelperService wireHelperService;

    protected TrigonometricComponentOptions options;
    private WireSupport wireSupport;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    @SuppressWarnings("unchecked")
    public void activate(final Map<String, Object> properties, ComponentContext componentContext) {
        logger.info("activating...");
        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());
        updated(properties, componentContext);
        logger.info("activating...done");
    }

    public void updated(final Map<String, Object> properties, ComponentContext componentContext) {
        logger.info("updating...");
        this.options = new TrigonometricComponentOptions(properties);
        logger.info("updated, properties: {}", properties);
        logger.info("updating...done");
    }

    public synchronized void deactivate() {
        logger.info("deactivating...");
        logger.info("deactivating...done");
    }

    @Override
    public Object polled(Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    @Override
    public void updated(Wire wire, Object value) {
        this.wireSupport.updated(wire, value);
    }

    @Override
    public void producersConnected(Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    @Override
    public void onWireReceive(WireEnvelope wireEnvelope) {
        final Map<String, TypedValue<?>> properties = wireEnvelope.getRecords().get(0).getProperties();
        final Double parameter = ((Number) properties.get(this.options.getParameterName()).getValue()).doubleValue();
        final Double result = this.options.getTrigonometricFunction().apply(parameter);
        if (result != null) {
            if (this.options.shouldEmitReceivedProperties()) {
                final Map<String, TypedValue<?>> resultProperties = new HashMap<>(properties);
                resultProperties.put(this.options.getResultName(), new DoubleValue(result));
                this.wireSupport.emit(Collections.singletonList(new WireRecord(resultProperties)));
            } else {
                this.wireSupport.emit(Collections.singletonList(new WireRecord(
                        Collections.singletonMap(this.options.getResultName(), new DoubleValue(result)))));
            }
            final WireRecord toBeEmitted = new WireRecord(
                    Collections.singletonMap(this.options.getResultName(), TypedValues.newDoubleValue(result)));
            this.wireSupport.emit(Collections.singletonList(toBeEmitted));
        }
    }
}
