/*******************************************************************************
 * Copyright (c) 2020 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.trigonometric.functions.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        logger.info("activated, properties: {}", properties);
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
        return wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        wireSupport.consumersConnected(wires);
    }

    @Override
    public void updated(Wire wire, Object value) {
        wireSupport.updated(wire, value);
    }

    @Override
    public void producersConnected(Wire[] wires) {
        wireSupport.producersConnected(wires);
    }

    @Override
    public void onWireReceive(WireEnvelope wireEnvelope) {
        final Optional<Double> operand = extractOperand(wireEnvelope, this.options.getOperandName());
        final Optional<Double> result = performTrigonometricOperation(operand);
        final Map<String, TypedValue<?>> properties = wireEnvelope.getRecords().get(0).getProperties();
        if (result.isPresent()) {
            if (this.options.shouldEmitReceivedProperties()) {
                final Map<String, TypedValue<?>> resultProperties = new HashMap<>(properties);
                resultProperties.put(this.options.getResultName(), new DoubleValue(result.get()));
                this.wireSupport.emit(Collections.singletonList(new WireRecord(resultProperties)));
            } else {
                this.wireSupport.emit(Collections.singletonList(new WireRecord(
                        Collections.singletonMap(this.options.getResultName(), new DoubleValue(result.get())))));
            }

            WireRecord toBeEmitted = new WireRecord(
                    Collections.singletonMap(this.options.getResultName(), TypedValues.newDoubleValue(result.get())));
            this.wireSupport.emit(Collections.singletonList(toBeEmitted));
        }
    }

    private Optional<Double> extractOperand(WireEnvelope wireEnvelope, String operandName) {
        if (wireEnvelope == null) {
            return Optional.empty();
        }
        final List<WireRecord> records = wireEnvelope.getRecords();
        if (records.isEmpty()) {
            return Optional.empty();
        }
        final Map<String, TypedValue<?>> properties = records.get(0).getProperties();
        if (properties.get(operandName) != null) {
            return Optional.of(Double.valueOf(properties.get(operandName).getValue().toString()));
        }
        return Optional.empty();
    }

    private Optional<Double> performTrigonometricOperation(Optional<Double> operand) {
        if (operand.isPresent()) {
            switch (this.options.getTrigonometricOperation()) {
            case SIN:
                return Optional.of((double) Math.round(Math.sin(operand.get().doubleValue())));
            case COS:
                return Optional.of((double) Math.round(Math.cos(operand.get().doubleValue())));
            case TAN:
                return Optional.of((double) Math.round(Math.tan(operand.get().doubleValue())));
            case ASIN:
                return Optional.of(Math.asin(operand.get().doubleValue()));
            case ACOS:
                return Optional.of(Math.acos(operand.get().doubleValue()));
            case ATAN:
                return Optional.of(Math.atan(operand.get().doubleValue()));
            default:
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

}
