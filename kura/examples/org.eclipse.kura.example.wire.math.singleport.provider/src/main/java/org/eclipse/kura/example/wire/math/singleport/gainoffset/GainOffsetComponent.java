/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport.gainoffset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
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

public class GainOffsetComponent implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(GainOffsetComponent.class);

    private WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private GainOffsetComponentOptions options;

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
        try {
            this.options = new GainOffsetComponentOptions(properties);
        } catch (Exception e) {
            logger.warn("Invalid configuration, please review", e);
            this.options = null;
        }
    }

    public void deactivate() {
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
        if (options == null) {
            logger.warn("Invalid configuration, please review");
        }
        final List<WireRecord> inputRecords = wireEnvelope.getRecords();
        final List<WireRecord> records = new ArrayList<>(inputRecords.size());
        for (final WireRecord record : inputRecords) {
            records.add(processRecord(record));
        }
        this.wireSupport.emit(records);
    }

    private WireRecord processRecord(WireRecord record) {
        final Map<String, TypedValue<?>> inputProperties = record.getProperties();
        final Map<String, TypedValue<?>> outProperties = new HashMap<>();
        if (this.options.shouldEmitReceivedProperties()) {
            outProperties.putAll(inputProperties);
        }
        for (GainOffsetEntry e : this.options.getEntries()) {
            final String propertyName = e.getPropertyName();
            final TypedValue<?> typedValue = inputProperties.get(propertyName);
            if (typedValue == null) {
                continue;
            }
            final Object value = typedValue.getValue();
            if (value == null || !(value instanceof Number)) {
                logger.warn("Invalid property value: {}={}", propertyName, typedValue);
                continue;
            }
            outProperties.put(propertyName,
                    TypedValues.newDoubleValue(((Number) value).doubleValue() * e.getGain() + e.getOffset()));
        }
        return new WireRecord(outProperties);
    }
}
