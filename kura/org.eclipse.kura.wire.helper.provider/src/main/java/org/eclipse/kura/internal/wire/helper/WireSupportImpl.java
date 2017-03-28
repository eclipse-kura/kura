/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.helper;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class WireSupportImpl implements {@link WireSupport}
 */
final class WireSupportImpl implements WireSupport {

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private final EventAdmin eventAdmin;

    private List<Wire> incomingWires;

    private List<Wire> outgoingWires;

    private final WireComponent wireSupporter;

    private String emitterPid;

    private String pid;

    /**
     * Instantiates a new wire support implementation.
     *
     * @param wireSupporter
     *            the wire supporter
     * @param wireHelperService
     *            the Wire Helper service
     * @param eventAdmin
     *            the Event Admin service
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    WireSupportImpl(final WireComponent wireSupporter, final WireHelperService wireHelperService,
            final EventAdmin eventAdmin) {
        requireNonNull(wireSupporter, message.wireSupportedComponentNonNull());
        requireNonNull(wireHelperService, message.wireHelperServiceNonNull());
        requireNonNull(eventAdmin, message.eventAdminNonNull());

        this.outgoingWires = CollectionUtil.newArrayList();
        this.incomingWires = CollectionUtil.newArrayList();
        this.emitterPid = wireHelperService.getServicePid(wireSupporter);
        this.pid = wireHelperService.getPid(wireSupporter);
        this.wireSupporter = wireSupporter;
        this.eventAdmin = eventAdmin;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void consumersConnected(final Wire[] wires) {
        this.outgoingWires = Arrays.asList(wires);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void emit(final List<WireRecord> wireRecords) {
        requireNonNull(wireRecords, message.wireRecordsNonNull());
        if (this.wireSupporter instanceof WireEmitter) {
            final WireEnvelope wei = new WireEnvelope(emitterPid, wireRecords);
            for (final Wire wire : this.outgoingWires) {
                wire.update(wei);
            }
            final Map<String, Object> properties = CollectionUtil.newHashMap();
            properties.put("emitter", pid);
            this.eventAdmin.postEvent(new Event(WireSupport.EMIT_EVENT_TOPIC, properties));
        }
    }

    /**
     * Gets the incoming wires.
     *
     * @return the incoming wires
     */
    List<Wire> getIncomingWires() {
        return Collections.unmodifiableList(this.incomingWires);
    }

    /**
     * Gets the outgoing wires.
     *
     * @return the outgoing wires
     */
    List<Wire> getOutgoingWires() {
        return Collections.unmodifiableList(this.outgoingWires);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Object polled(final Wire wire) {
        return wire.getLastValue();
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.incomingWires = Arrays.asList(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        requireNonNull(wire, message.wireNonNull());
        if (value instanceof WireEnvelope && this.wireSupporter instanceof WireReceiver) {
            ((WireReceiver) this.wireSupporter).onWireReceive((WireEnvelope) value);
        }
    }
}
