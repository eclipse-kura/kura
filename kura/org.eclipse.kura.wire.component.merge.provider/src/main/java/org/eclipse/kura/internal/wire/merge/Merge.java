/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.merge;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.MultiportWireSupport;
import org.eclipse.kura.wire.multiport.MultiportWireReceiver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Conditional is a specific Wire Component to apply a condition
 * on the received {@link WireEnvelope}
 */
public final class Merge implements MultiportWireReceiver, WireEmitter, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(Merge.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private volatile WireHelperService wireHelperService;

    private MultiportWireSupport wireSupport;
    private ComponentContext context;
    private MergeOptions mergeOptions;

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

    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug(message.activatingLogger());
        this.context = componentContext;
        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this);
        updated(properties);

        logger.debug(message.activatingLoggerDone());
    }

    public void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingLogger());
        this.mergeOptions = new MergeOptions(properties, context.getBundleContext());

        this.mergeOptions.getPortAggregatorFactory().build(wireSupport.getReceiverPorts())
                .onWireReceive(this::onWireReceive);

        logger.debug(message.updatingLoggerDone());
    }

    private void onWireReceive(List<WireEnvelope> envelopes) {
        final Map<String, TypedValue<?>> result = new HashMap<>();
        for (WireEnvelope e : envelopes) {
            if (e == null || e.getRecords().isEmpty()) {
                continue;
            }
            result.putAll(e.getRecords().get(0).getProperties());
        }
        wireSupport.emit(Collections.singletonList(new WireRecord(result)));
    }

    protected void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingLogger());
        // remained for debugging purposes
        logger.debug(message.deactivatingLoggerDone());
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        requireNonNull(wires, message.wiresNonNull());
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    @Override
    public Object polled(Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }
}