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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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

    private Set<WireConfiguration> wireConfigurations;
    private final WireHelperService wireHelperService;
    private final WireComponent wireComponent;
    private final EventAdmin eventAdmin;
    private final String componentPid;
    private final AtomicBoolean canRetrieveConfiguration;

    /**
     * Instantiates a new {@link WireSupportImpl}
     *
     * @param wireComponent
     *            the {@link WireComponent} instance
     * @param wireHelperService
     *            the {@link WireHelperService} instance
     * @param eventAdmin
     *            the {@link EventAdmin} instance
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    WireSupportImpl(final WireComponent wireComponent, final WireHelperService wireHelperService,
            final EventAdmin eventAdmin) {
        requireNonNull(wireComponent, message.wireSupportedComponentNonNull());
        requireNonNull(wireHelperService, message.wireHelperServiceNonNull());
        requireNonNull(eventAdmin, message.eventAdminNonNull());

        this.wireComponent = wireComponent;
        this.wireHelperService = wireHelperService;
        this.eventAdmin = eventAdmin;
        this.componentPid = this.wireHelperService.getPid(this.wireComponent);
        this.canRetrieveConfiguration = new AtomicBoolean(false);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void emit(final List<WireRecord> wireRecords) {
        requireNonNull(wireRecords, message.wireRecordsNonNull());
        if (!(this.wireComponent instanceof WireEmitter)) {
            return;
        }
        retrieveWireConfigurationsIfNeeded();
        final WireEnvelope wireEnvelope = new WireEnvelope(this.componentPid, wireRecords);
        for (final WireConfiguration wc : this.wireConfigurations) {
            final Wire wire = wc.getWire();
            if (wire == null) {
                continue;
            }
            final String filter = wc.getFilter();
            if (filter == null) {
                wire.update(wireEnvelope);
                continue;
            }
            // invoke filter mechanism
            final WireEnvelope filteredWireEnvelope = getFilteredWireEnvelope(wireRecords, wireEnvelope, filter);
            wire.update(filteredWireEnvelope);
        }
        fireEmitEvent();
    }

    /**
     * fires OSGi event for every emit operation
     */
    private void fireEmitEvent() {
        final Map<String, Object> properties = CollectionUtil.newHashMap();
        properties.put("emitter", this.componentPid);
        this.eventAdmin.postEvent(new Event(WireSupport.EMIT_EVENT_TOPIC, properties));
    }

    /**
     * For the first time when Wire Graph is first created, Wire Configurations
     * are not persisted. The WireAdmin push and pull-based callback methods
     * producerConnected(..) and consumersConnected(..) are called before WireService
     * creates any Wire Configurations. Hence, for the first time, the collection of
     * Wire Configurations is empty
     */
    private void retrieveWireConfigurationsIfNeeded() {
        if (this.canRetrieveConfiguration.get()) {
            this.wireConfigurations = this.wireHelperService.getWireConfigurationsByEmitterPid(this.componentPid);
            this.canRetrieveConfiguration.set(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<WireRecord> filter(final List<WireRecord> wireRecords, final String filter) {
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
     * Tries to filter the provided list of {@link WireRecord}s that matches the provided filter.
     * In case of exceptional conditions, it returns the provided non-filtered {@link WireEnvelope}
     * instance
     *
     * @param wireRecords
     *            list of non-filtered {@link WireRecord}s
     * @param nonFilteredWireEnvelope
     *            non-filtered {@link WireEnvelope} instance
     * @param filter
     *            filter to match
     * @return returns A {@link WireEnvelope} instance associating the list of filtered
     *         {@link WireRecord}s in case of success, otherwise the provided non-filtered
     *         {@link WireEnvelope} instance
     */
    private WireEnvelope getFilteredWireEnvelope(final List<WireRecord> wireRecords,
            final WireEnvelope nonFilteredWireEnvelope, final String filter) {
        WireEnvelope filteredWireEnvelope;
        try {
            filteredWireEnvelope = new WireEnvelope(this.componentPid, filter(wireRecords, filter));
        } catch (final Exception ex) {
            // if any exception occurs while filtering, just emit the not filtered
            // WireRecords
            filteredWireEnvelope = nonFilteredWireEnvelope;
        }
        return filteredWireEnvelope;
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
     * @throws PatternSyntaxException
     *             If the regular expression's syntax is invalid
     */
    private static <V> Map<String, V> lookup(final String regularExpression, final Map<String, V> map) {
        final Pattern pattern = Pattern.compile(regularExpression);
        final Set<Entry<String, V>> entrySet = map.entrySet();
        final Supplier<Stream<Entry<String, V>>> streamSupplier = () -> entrySet.stream();
        // If the provided regular expression matches all the keys of the provided map,
        // there is no need to create a new map instance
        final boolean allMatch = streamSupplier.get().allMatch(matches(pattern));
        if (allMatch) {
            return map;
        }
        final Map<String, V> filteredMap = streamSupplier.get().filter(matches(pattern))
                .collect(toMap(Entry::getKey, Entry::getValue));
        return Collections.unmodifiableMap(filteredMap);
    }

    /**
     * The {@link Predicate} instance denoting the provided {@link Pattern} instance to match
     *
     * @param pattern
     *            the {@link Pattern} instance to match
     * @return a {@link Predicate} instance
     */
    public static <V> Predicate<Entry<String, V>> matches(final Pattern pattern) {
        return entry -> pattern.matcher(entry.getKey()).matches();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Object polled(final Wire wire) {
        return wire.getLastValue();
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        // not required
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.canRetrieveConfiguration.compareAndSet(false, true);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void updated(final Wire wire, final Object value) {
        requireNonNull(wire, message.wireNonNull());
        if (value instanceof WireEnvelope && this.wireComponent instanceof WireReceiver) {
            ((WireReceiver) this.wireComponent).onWireReceive((WireEnvelope) value);
        }
    }
}
