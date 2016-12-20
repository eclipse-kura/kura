/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.wire.SeverityLevel.CONFIG;
import static org.eclipse.kura.wire.SeverityLevel.ERROR;
import static org.eclipse.kura.wire.SeverityLevel.INFO;
import static org.eclipse.kura.wire.SeverityLevel.SEVERE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.SeverityLevel;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class WireSupportImpl implements {@link WireSupport}
 */
final class WireSupportImpl implements WireSupport {

    /** Localization Resource */
    private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

    /** Event Admin Service */
    private final EventAdmin eventAdmin;

    /** The incoming wires. */
    private List<Wire> incomingWires;

    /** The outgoing wires. */
    private List<Wire> outgoingWires;

    /** The Wire Helper Service. */
    private final WireHelperService wireHelperService;

    /** The supported Wire Component. */
    private final WireComponent wireSupporter;

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
     *             if any of the arguments is null
     */
    WireSupportImpl(final WireComponent wireSupporter, final WireHelperService wireHelperService,
            final EventAdmin eventAdmin) {
        requireNonNull(wireSupporter, s_message.wireSupportedComponentNonNull());
        requireNonNull(wireHelperService, s_message.wireHelperServiceNonNull());
        requireNonNull(eventAdmin, s_message.eventAdminNonNull());

        this.outgoingWires = CollectionUtil.newArrayList();
        this.incomingWires = CollectionUtil.newArrayList();
        this.wireSupporter = wireSupporter;
        this.wireHelperService = wireHelperService;
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
        requireNonNull(wireRecords, s_message.wireRecordsNonNull());
        if (this.wireSupporter instanceof WireEmitter) {
            final String emitterPid = this.wireHelperService.getServicePid(this.wireSupporter);
            final String pid = this.wireHelperService.getPid(this.wireSupporter);
            final WireEnvelope wei = new WireEnvelope(emitterPid, wireRecords);
            for (final Wire wire : this.outgoingWires) {
                wire.update(wei);
            }
            final Map<String, Object> properties = CollectionUtil.newHashMap();
            properties.put("emitter", pid);
            this.eventAdmin.postEvent(new Event(WireSupport.EMIT_EVENT_TOPIC, properties));
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<WireRecord> filter(final List<WireRecord> records) {
        requireNonNull(records, s_message.wireRecordsNonNull());
        final SeverityLevel level = this.getSeverityLevel();
        // If the severity level is SEVERE, then all wire fields remain
        if ((level == null) || (level == SEVERE)) {
            return records;
        }
        final List<WireRecord> newRecords = CollectionUtil.newArrayList();
        final List<WireField> newFields = CollectionUtil.newArrayList();
        for (final WireRecord wireRecord : records) {
            for (final WireField wireField : wireRecord.getFields()) {
                // If the severity level is INFO, then only info wire fields
                // will remain
                final SeverityLevel wireFieldLevel = wireField.getSeverityLevel();
                if ((wireFieldLevel == INFO) && (level == INFO)) {
                    newFields.add(wireField);
                }
                // If the severity level is ERROR, then only ERROR wire fields
                // will remain
                if ((wireFieldLevel == ERROR) && (level == ERROR)) {
                    newFields.add(wireField);
                }
                // If the severity level is CONFIG, then info and CONFIG wire
                // fields remain
                if (((wireFieldLevel == INFO) || (wireFieldLevel == CONFIG)) && (level == CONFIG)) {
                    newFields.add(wireField);
                }
                final WireRecord newWireRecord = new WireRecord(wireRecord.getTimestamp(), wireRecord.getPosition(),
                        newFields);
                newRecords.add(newWireRecord);
            }
        }
        return newRecords;
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

    /**
     * Returns the severity level of the wire component
     *
     * @return the severity level
     */
    private SeverityLevel getSeverityLevel() {
        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        final ServiceReference<WireComponent>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class,
                null);
        String property = null;
        for (final ServiceReference<WireComponent> ref : refs) {
            final WireComponent component = context.getService(ref);
            if (component == this.wireSupporter) {
                property = ref.getProperty("severity.level").toString();
                break;
            }
        }
        if ("INFO".equalsIgnoreCase(property)) {
            return INFO;
        }
        if ("ERROR".equalsIgnoreCase(property)) {
            return ERROR;
        }
        if ("CONFIG".equalsIgnoreCase(property)) {
            return CONFIG;
        }
        if ("SEVERE".equalsIgnoreCase(property)) {
            return SEVERE;
        }
        return null;
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
        requireNonNull(wire, s_message.wireNonNull());
        if ((value instanceof WireEnvelope) && (this.wireSupporter instanceof WireReceiver)) {
            ((WireReceiver) this.wireSupporter).onWireReceive((WireEnvelope) value);
        }
    }
}
