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
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
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

    private final String emitterPid;

    private final String pid;

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
            final String emitterPid = this.wireHelperService.getPid(this.wireSupporter);
            final Set<WireConfiguration> wireConfigurations = this.wireHelperService
                    .getWireConfigurationsByEmitterPid(emitterPid);
            final WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);

            for (final WireConfiguration wc : wireConfigurations) {
                final String filter = wc.getFilter();
                final Wire wire = wc.getWire();
                if (filter == null) {
                    wire.update(wireEnvelope);
                    continue;
                }
                final WireEnvelope filteredWireEnvelope = new WireEnvelope(emitterPid, filter(wireRecords, filter));
                wire.update(filteredWireEnvelope);
            }

            // fire OSGi event for every emit operation
            final Map<String, Object> properties = CollectionUtil.newHashMap();
            properties.put("emitter", emitterPid);
            this.eventAdmin.postEvent(new Event(WireSupport.EMIT_EVENT_TOPIC, properties));
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<WireRecord> filter(final List<WireRecord> wireRecords, final String filter) {
        requireNonNull(wireRecords, message.wireRecordsNonNull());
        requireNonNull(filter, message.filterNonNull());

        final List<WireRecord> filteredWireRecords = CollectionUtil.newArrayList();
        for (final WireRecord wireRecord : wireRecords) {
            final Map<String, TypedValue<?>> previousProperties = wireRecord.getProperties();
            final Map<String, TypedValue<?>> filteredProperties = lookup(filter, previousProperties);

            // If both the maps' references refer to the same map instance, there is no need
            // to create a new WireRecord. This is an optimization functionality, in which
            // the regular expression filter matches all the provided keys of the properties
            if (previousProperties == filteredProperties) {
                filteredWireRecords.add(wireRecord);
                continue;
            }
            final WireRecord newWireRecord = new WireRecord(filteredProperties);
            filteredWireRecords.add(newWireRecord);
        }
        return filteredWireRecords;
    }

    /**
     * Filters out the keys from the provided {@link Map} instance
     *
     * @param regularExpression
     *            the regular expression to match
     * @param map
     *            the {@link Map} instance to filter
     * @return the {@link Map} instance comprising the keys
     *         that match the provided regular expression
     */
    private static <V> Map<String, V> lookup(final String regularExpression, final Map<String, V> map) {
        final Pattern pattern = Pattern.compile(regularExpression);
        final Set<Entry<String, V>> entrySet = map.entrySet();
        final Supplier<Stream<Entry<String, V>>> streamSupplier = () -> entrySet.stream();
        // If the provided regular expression matches all the keys of the provided map,
        // there is no need to create a new map instance
        final boolean allMatch = streamSupplier.get().allMatch(entry -> pattern.matcher(entry.getKey()).matches());
        if (allMatch) {
            return map;
        }
        return streamSupplier.get().filter(entry -> pattern.matcher(entry.getKey()).matches())
                .collect(toMap(Entry::getKey, Entry::getValue));
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
