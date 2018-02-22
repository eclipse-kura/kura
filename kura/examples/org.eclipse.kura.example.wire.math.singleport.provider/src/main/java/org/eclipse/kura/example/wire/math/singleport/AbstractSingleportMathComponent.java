/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
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

public abstract class AbstractSingleportMathComponent
        implements WireEmitter, WireReceiver, ConfigurableComponent, Function<TypedValue<?>, TypedValue<?>> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSingleportMathComponentOptions.class);

    private WireHelperService wireHelperService;
    private WireSupport wireSupport;
    protected AbstractSingleportMathComponentOptions options;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = null;
    }

    public void activate(final Map<String, Object> properties, ComponentContext componentContext) {
        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());
        updated(properties);
    }

    public void updated(final Map<String, Object> properties) {
        this.options = getOptions(properties);
        init();
    }

    public void deactivate() {
    }

    protected void init() {
    }

    protected AbstractSingleportMathComponentOptions getOptions(final Map<String, Object> properties) {
        return new AbstractSingleportMathComponentOptions(properties);
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
        final List<WireRecord> records = wireEnvelope.getRecords();
        if (records.isEmpty()) {
            logger.warn("Received empty envelope");
            return;
        }
        final Map<String, TypedValue<?>> properties = records.get(0).getProperties();
        final TypedValue<?> operand = properties.get(this.options.getOperandName());
        if (operand == null) {
            logger.warn("Missing operand");
            return;
        }
        if (!(operand.getValue() instanceof Number)) {
            logger.warn("Not a number: {}", operand);
            return;
        }
        final TypedValue<?> result = this.apply(operand);
        if (this.options.shouldEmitReceivedProperties()) {
            final Map<String, TypedValue<?>> resultProperties = new HashMap<>(properties);
            resultProperties.put(this.options.getResultName(), result);
            this.wireSupport.emit(Collections.singletonList(new WireRecord(resultProperties)));
        } else {
            this.wireSupport.emit(Collections
                    .singletonList(new WireRecord(Collections.singletonMap(this.options.getResultName(), result))));
        }
    }

}
