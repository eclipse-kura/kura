/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.multiport;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.eclipse.kura.wire.multiport.MultiportWireReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDualportMathComponent implements WireEmitter, MultiportWireReceiver,
        ConfigurableComponent, BiFunction<TypedValue<?>, TypedValue<?>, TypedValue<?>> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDualportMathComponentOptions.class);

    private WireHelperService wireHelperService;
    private MultiportWireSupport wireSupport;
    protected AbstractDualportMathComponentOptions options;
    protected BundleContext context;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = null;
    }

    public void activate(final Map<String, Object> properties, ComponentContext context) {
        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) context.getServiceReference());
        this.context = context.getBundleContext();
        updated(properties);
    }

    public void updated(final Map<String, Object> properties) {
        this.options = getOptions(properties);
        this.options.getPortAggregatorFactory().build(this.wireSupport.getReceiverPorts())
                .onWireReceive(this::onWireReceive);
        init();
    }

    public void deactivate() {
    }

    protected void init() {
    }

    protected AbstractDualportMathComponentOptions getOptions(final Map<String, Object> properties) {
        return new AbstractDualportMathComponentOptions(properties, this.context);
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

    private TypedValue<?> extractOperand(WireEnvelope wireEnvelope, String operandName) {
        if (wireEnvelope == null) {
            return null;
        }
        final List<WireRecord> records = wireEnvelope.getRecords();
        if (records.isEmpty()) {
            logger.warn("Received empty envelope");
            return null;
        }
        final Map<String, TypedValue<?>> properties = records.get(0).getProperties();
        final TypedValue<?> operand = properties.get(operandName);
        if (operand == null) {
            logger.warn("Missing operand");
            return null;
        }
        if (!(operand.getValue() instanceof Number)) {
            logger.warn("Not a number: {}", operand);
            return null;
        }
        return operand;
    }

    public void onWireReceive(List<WireEnvelope> wireEnvelopes) {
        final TypedValue<?> firstOperand = extractOperand(wireEnvelopes.get(0), this.options.getFirstOperandName());
        final TypedValue<?> secondOperand = extractOperand(wireEnvelopes.get(1), this.options.getSecondOperandName());
        if (firstOperand == null || secondOperand == null) {
            return;
        }
        final TypedValue<?> result = this.apply(firstOperand, secondOperand);
        this.wireSupport.emit(Collections
                .singletonList(new WireRecord(Collections.singletonMap(this.options.getResultName(), result))));
    }

}
