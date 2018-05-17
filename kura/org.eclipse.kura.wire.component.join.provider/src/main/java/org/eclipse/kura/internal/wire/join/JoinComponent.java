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
package org.eclipse.kura.internal.wire.join;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JoinComponent implements MultiportWireReceiver, WireEmitter, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(JoinComponent.class);

    private volatile WireHelperService wireHelperService;

    private MultiportWireSupport wireSupport;
    private ComponentContext context;
    private JoinComponentOptions joinComponentOptions;

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
        logger.debug("Activating Join Wire Component...");
        this.context = componentContext;
        this.wireSupport = (MultiportWireSupport) this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        updated(properties);

        logger.debug("Activating Join Wire Component... Done");
    }

    public void updated(final Map<String, Object> properties) {
        logger.debug("Updating Join Wire Component...");
        this.joinComponentOptions = new JoinComponentOptions(properties, context.getBundleContext());

        this.joinComponentOptions.getPortAggregatorFactory().build(wireSupport.getReceiverPorts())
                .onWireReceive(this::onWireReceive);

        logger.debug("Updating Join Wire Component... Done");
    }

    private void onWireReceive(List<WireEnvelope> envelopes) {
        final WireEnvelope firstEnvelope = envelopes.get(0);
        final WireEnvelope secondEnvelope = envelopes.get(1);
        final List<WireRecord> firstRecords = firstEnvelope != null ? firstEnvelope.getRecords()
                : Collections.emptyList();
        final List<WireRecord> secondRecords = firstEnvelope != null ? secondEnvelope.getRecords()
                : Collections.emptyList();
        final List<WireRecord> result = new ArrayList<>();
        forEachPair(firstRecords.iterator(), secondRecords.iterator(), (first, second) -> {
            if (first == null) {
                result.add(new WireRecord(second.getProperties()));
                return;
            }
            if (second == null) {
                result.add(new WireRecord(first.getProperties()));
                return;
            }
            final Map<String, TypedValue<?>> resultProperties = new HashMap<>(first.getProperties());
            resultProperties.putAll(second.getProperties());
            result.add(new WireRecord(resultProperties));
        });
        this.wireSupport.emit(result);
    }

    private <T, U> void forEachPair(Iterator<T> first, Iterator<U> second, BiConsumer<T, U> consumer) {
        while (first.hasNext() || second.hasNext()) {
            final T firstValue = first.hasNext() ? first.next() : null;
            final U secondValue = second.hasNext() ? second.next() : null;
            consumer.accept(firstValue, secondValue);
        }
    }

    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("Deactivating Join Wire Component...");
        // remained for debugging purposes
        logger.debug("Deactivating Join Wire Component... Done");
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
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
