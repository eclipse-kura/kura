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
package org.eclipse.kura.internal.wire;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireConfiguration;

/**
 * Captures the configuration of the Wire Graph.
 */
final class WireServiceOptions {

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Regular Expression pattern used for checking wire configurations */
    private static final String PATTERN = "%s.";

    /**
     * The separator to be used for storing Wire Configuration properties in a
     * flat style
     */
    public static final String SEPARATOR = ".";

    private final List<WireConfiguration> wireConfigurations;

    /**
     * Instantiates a new wire service options.
     *
     * @param configurations
     *            the list of Wire Configurations
     * @throws NullPointerException
     *             if provided configurations is null
     */
    private WireServiceOptions(final List<WireConfiguration> configurations) {
        requireNonNull(configurations, message.configurationNonNull());
        this.wireConfigurations = configurations;
    }

    /**
     * Creates new instance of {@link WireServiceOptions}
     *
     * @param properties
     *            the properties
     * @param helperService
     *            the Wire Helper Service instance
     * @return the wire service options
     * @throws NullPointerException
     *             if provided properties is null
     */
    static WireServiceOptions getInstance(final Map<String, Object> properties) {
        requireNonNull(properties, message.wireServicePropNonNull());
        final List<WireConfiguration> wireConfs = CollectionUtil.newCopyOnWriteArrayList();
        final Set<Long> wireIds = CollectionUtil.newHashSet();
        for (final Map.Entry<String, Object> entry : properties.entrySet()) {
            final String key = entry.getKey();
            if (key.contains(SEPARATOR) && Character.isDigit(key.charAt(0))) {
                final Long wireConfId = Long.parseLong(key.substring(0, key.indexOf(SEPARATOR)));
                wireIds.add(wireConfId);
            }
        }
        final Iterator<Long> it = wireIds.iterator();
        while (it.hasNext()) {
            final String wireConfId = String.valueOf(it.next());
            String emitterPid = null;
            String receiverPid = null;
            String filter = null;
            for (final Map.Entry<String, Object> entry : properties.entrySet()) {
                final String key = entry.getKey();
                final String value = String.valueOf(entry.getValue());

                if (!key.contains(SEPARATOR)) {
                    continue;
                }
                if ((key.startsWith(String.format(PATTERN, wireConfId)))) {
                    if (key.contains(message.emitter())) {
                        emitterPid = value;
                    }
                    if (key.contains(message.receiver())) {
                        receiverPid = value;
                    }
                    if (key.contains(message.filter())) {
                        filter = value;
                    }
                }
            }
            final WireConfiguration configuration = new WireConfiguration(emitterPid, receiverPid);
            configuration.setFilter(filter);
            wireConfs.add(configuration);
        }
        return new WireServiceOptions(wireConfs);
    }

    /**
     * Gets the {@link WireConfiguration}s.
     *
     * @return the {@link WireConfiguration}s
     */
    List<WireConfiguration> getWireConfigurations() {
        return this.wireConfigurations;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "WireServiceOptions [wireConfigurations=" + this.wireConfigurations + "]";
    }
}
