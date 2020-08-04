/*******************************************************************************
 * Copyright (c) 2020 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.example.wire.bool.multiport.provider;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.example.wire.bool.multiport.provider.LogicalComponentOptions.OperatorOption;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.eclipse.kura.wire.multiport.MultiportWireReceiver;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalComponent implements WireEmitter, ConfigurableComponent, MultiportWireReceiver {

    private static final Logger logger = LoggerFactory.getLogger(LogicalComponent.class);

    private WireHelperService wireHelperService;
    private MultiportWireSupport wireSupport;

    protected LogicalComponentOptions options;
    protected BundleContext context;

    public void bindWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    @SuppressWarnings("unchecked")
    public void activate(final Map<String, Object> properties, ComponentContext componentContext) {
        logger.info("activating...");
        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());
        logger.info("activated, properties: {}", properties);
        context = componentContext.getBundleContext();
        updated(properties, componentContext);
        logger.info("activating...done");
    }

    public void updated(final Map<String, Object> properties, ComponentContext componentContext) {
        logger.info("updating...");
        this.options = new LogicalComponentOptions(properties, this.context);
        logger.info("updated, properties: {}", properties);
        this.options.getPortAggregatorFactory().build(this.wireSupport.getReceiverPorts())
                .onWireReceive(this::onWireReceive);

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

    private Boolean extractOperand(WireEnvelope wireEnvelope, String operandName) {
        if (wireEnvelope == null) {
            throw new NullPointerException();
        }
        final List<WireRecord> records = wireEnvelope.getRecords();
        if (records.isEmpty()) {
            throw new NullPointerException();
        }
        final Map<String, TypedValue<?>> properties = records.get(0).getProperties();
        return (Boolean) properties.get(operandName).getValue();
    }

    public void onWireReceive(List<WireEnvelope> wireEnvelopes) {
        final Boolean firstOperand = extractOperand(wireEnvelopes.get(0), this.options.getFirstOperandName());
        final Boolean secondOperand;
        if (OperatorOption.NOT.equals(this.options.getBooleanOperation())) {
            secondOperand = firstOperand;
        } else {
            secondOperand = extractOperand(wireEnvelopes.get(1), this.options.getSecondOperandName());
        }
        final TypedValue<Boolean> result = TypedValues
                .newBooleanValue(performBooleanOperation(firstOperand, secondOperand));
        this.wireSupport.emit(Collections
                .singletonList(new WireRecord(Collections.singletonMap(this.options.getResultName(), result))));
    }

    private boolean performBooleanOperation(Boolean firstOperand, Boolean secondOperand) {
        switch (this.options.getBooleanOperation()) {
        case NOT:
            return !(firstOperand.booleanValue());
        case AND:
            return firstOperand.booleanValue() && secondOperand.booleanValue();
        case OR:
            return firstOperand.booleanValue() || secondOperand.booleanValue();
        case NOR:
            return !(firstOperand.booleanValue() || secondOperand.booleanValue());
        case NAND:
            return !(firstOperand.booleanValue() && secondOperand.booleanValue());
        case XOR:
            return firstOperand.booleanValue() ^ secondOperand.booleanValue();
        default:
            return false;
        }

    }

}
