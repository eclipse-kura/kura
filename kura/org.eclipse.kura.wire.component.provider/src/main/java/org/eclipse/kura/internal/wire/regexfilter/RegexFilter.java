/*******************************************************************************
 * Copyright (c) 2017, 2018 Amit Kumar Mondal and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Amit Kumar Mondal
 *      Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.regexfilter;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.eclipse.kura.internal.wire.regexfilter.FilterType.REMOVE;
import static org.eclipse.kura.internal.wire.regexfilter.FilterType.RETAIN;
import static org.eclipse.kura.util.collection.CollectionUtil.newArrayList;
import static org.eclipse.kura.util.collection.CollectionUtil.newHashMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

/**
 * The Class {@link RegexFilter} represents a {@link WireComponent} which filters the keys from
 * the associated properties in the incoming {@link WireEnvelope} that matches provided regular
 * expression
 */
public final class RegexFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

    /** Logger instance */
    private static final Logger logger = LogManager.getLogger(RegexFilter.class);

    /** Regular Expression Metatype Attribute Definition Property Key */
    private static final String REGEX_PROP = "regex.filter";

    /** Filter Type Attribute Definition Property Key */
    private static final String REGEX_TYPE_PROP = "filter.type";

    /** Emit and Receive support operation adapter */
    private WireSupport wireSupport;

    /** Associated Filter available in the component properties */
    private String filter;

    /** Associated Filter Type available in the component properties */
    private FilterType filterType;

    /** Wire Component PID */
    private String componentPid;

    /** Service Injection */
    private volatile WireHelperService wireHelperService;

    /**
     * Bind the {@link WireHelperService}.
     *
     * @param wireHelperService
     *            the new {@link WireHelperService}
     */
    protected synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Unbind the {@link WireHelperService}
     *
     * @param wireHelperService
     *            the new {@link WireHelperService}
     */
    protected synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi service component activation callback
     *
     * @param properties
     *            the configured properties
     * @param componentContext
     */
    protected synchronized void activate(final Map<String, Object> properties, ComponentContext componentContext) {
        logger.debug("Activating Regex Filter...");
        this.filter = String.valueOf(properties.getOrDefault(REGEX_PROP, ""));
        this.componentPid = String.valueOf(properties.get(KURA_SERVICE_PID));
        this.filterType = getType(properties);
        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());
        logger.debug("Activating Regex Filter... Done");
    }

    /**
     * OSGi service component modification callback
     *
     * @param properties
     *            the updated properties
     */
    protected synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating Regex Filter...");
        this.filter = String.valueOf(properties.getOrDefault(REGEX_PROP, ""));
        this.filterType = getType(properties);
        logger.debug("Updating Regex Filter... Done");
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {
        final List<WireRecord> receivedRecords = wireEnvelope.getRecords();
        if (isNull(this.filter) || this.filter.trim().isEmpty()) {
            // no filter has been associated
            this.wireSupport.emit(receivedRecords);
            return;
        }

        final WireEnvelope filteredWireEnvelope = getFilteredWireEnvelope(receivedRecords, wireEnvelope);
        this.wireSupport.emit(filteredWireEnvelope.getRecords());
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
     * @return returns A {@link WireEnvelope} instance associating the list of filtered
     *         {@link WireRecord}s in case of success, otherwise the provided non-filtered
     *         {@link WireEnvelope} instance
     */
    private WireEnvelope getFilteredWireEnvelope(final List<WireRecord> wireRecords,
            final WireEnvelope nonFilteredWireEnvelope) {
        WireEnvelope filteredWireEnvelope;
        try {
            filteredWireEnvelope = new WireEnvelope(this.componentPid,
                    filter(wireRecords, this.filter, this.filterType));
        } catch (final Exception ex) {
            // if any exception occurs while filtering, just emit the not filtered
            // Wire Records
            logger.warn("Error while filtering using provided Regular Expression...", ex);
            filteredWireEnvelope = nonFilteredWireEnvelope;
        }
        return filteredWireEnvelope;
    }

    /**
     * Filters out the keys from the associated properties of provided {@link WireRecord}s
     * that matches the provided filter
     *
     * @param wireRecords
     *            the list of {@link WireRecord}s
     * @param filter
     *            the filter to match
     * @param type
     *            the associated type that signifies either to retain matched keys or remove
     * @return the list of {@link WireRecord}s containing the filtered properties
     * @throws NullPointerException
     *             if any of the arguments is null
     * @throws PatternSyntaxException
     *             If the filter's syntax is invalid
     */
    private static List<WireRecord> filter(final List<WireRecord> wireRecords, final String filter,
            final FilterType type) {
        requireNonNull(wireRecords, "Wire Records cannot be null");
        requireNonNull(filter, "Filter cannot be null");

        final List<WireRecord> filteredWireRecords = newArrayList();
        for (final WireRecord wireRecord : wireRecords) {
            final Map<String, TypedValue<?>> previousProperties = wireRecord.getProperties();
            final Map<String, TypedValue<?>> filteredProperties = match(filter, previousProperties, type);

            // If both the maps' references refer to the same map instance, there is no need
            // to create a new Wire Record. This is an optimization functionality, in which
            // the regular expression filter matches all the provided keys of the properties
            // or the properties is empty
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
     * @param type
     *            the associated type that signifies either to retain matched keys or remove
     * @return the {@link Map} instance comprising the keys
     *         that match the provided regular expression
     * @throws PatternSyntaxException
     *             If the regular expression's syntax is invalid
     */
    private static <V> Map<String, V> match(final String regularExpression, final Map<String, V> map,
            final FilterType type) {
        // if the properties map is empty, no need to invoke filter mechanism
        if (map.isEmpty()) {
            return map;
        }
        final Pattern pattern = Pattern.compile(regularExpression);
        final Set<Entry<String, V>> entrySet = map.entrySet();
        final Supplier<Stream<Entry<String, V>>> streamSupplier = entrySet::stream;
        // If the provided regular expression matches all the keys of the provided map,
        // there is no need to create a new map instance (in case of RETAIN type)
        final boolean allMatch = streamSupplier.get().allMatch(matches(pattern, type));
        if (allMatch) {
            return type == RETAIN ? map : unmodifiableMap(newHashMap());
        }
        return streamSupplier.get().filter(matches(pattern, type))
                .collect(collectingAndThen(toMap(Entry::getKey, Entry::getValue), Collections::unmodifiableMap));
    }

    /**
     * The {@link Predicate} instance denoting the provided {@link Pattern} instance to match
     *
     * @param pattern
     *            the {@link Pattern} instance to match
     * @param type
     *            the type that signifies either to match entries or not
     * @return a {@link Predicate} instance
     */
    private static <V> Predicate<Entry<String, V>> matches(final Pattern pattern, final FilterType type) {
        final Predicate<Entry<String, V>> predicate = entry -> pattern.matcher(entry.getKey()).matches();
        return type == RETAIN ? predicate : predicate.negate();
    }

    /**
     * Returns the associated type of the filter operation
     *
     * @param props
     *            the properties to find the associated type
     * @return if type is equal to 2, then {@link FilterType#REMOVE} else {@link FilterType#RETAIN}
     */
    private static FilterType getType(final Map<String, Object> props) {
        final int type = (Integer) props.getOrDefault(REGEX_TYPE_PROP, 1);
        return type == 2 ? REMOVE : RETAIN;
    }

}
