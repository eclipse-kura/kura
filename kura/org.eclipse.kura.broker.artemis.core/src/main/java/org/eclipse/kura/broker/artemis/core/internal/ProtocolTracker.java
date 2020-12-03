/*******************************************************************************
 * Copyright (c) 2017, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.broker.artemis.core.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.artemis.spi.core.protocol.ProtocolManagerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolTracker {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolTracker.class);

    private final BundleContext context;

    private final Map<String, Collection<ProtocolManagerFactory<?>>> protocols = new HashMap<>();

    @SuppressWarnings({"rawtypes", "checkstyle:lineLength"})
    private final ServiceTrackerCustomizer<ProtocolManagerFactory, ProtocolManagerFactory> customizer = new ServiceTrackerCustomizer<ProtocolManagerFactory, ProtocolManagerFactory>() {

        @Override
        public ProtocolManagerFactory addingService(final ServiceReference<ProtocolManagerFactory> reference) {
            final ProtocolManagerFactory service = ProtocolTracker.this.context.getService(reference);
            addProtocols(service);
            return service;
        }

        @Override
        public void modifiedService(final ServiceReference<ProtocolManagerFactory> reference,
                final ProtocolManagerFactory service) {
        }

        @Override
        public void removedService(final ServiceReference<ProtocolManagerFactory> reference,
                final ProtocolManagerFactory service) {
            removeProtocols(service);
            ProtocolTracker.this.context.ungetService(reference);
        }

    };

    @SuppressWarnings("rawtypes")
    private final ServiceTracker<ProtocolManagerFactory, ProtocolManagerFactory> tracker;

    private final ProtocolTrackerListener listener;

    public ProtocolTracker(final BundleContext context, final ProtocolTrackerListener listener) {
        this.context = context;
        this.listener = listener;
        this.tracker = new ServiceTracker<>(context, ProtocolManagerFactory.class, this.customizer);
    }

    public void start() {
        this.tracker.open();
    }

    public void stop() {
        this.tracker.close();
    }

    protected synchronized void addProtocols(final ProtocolManagerFactory<?> factory) {
        final Set<String> referencedProtocols = new HashSet<>(Arrays.asList(factory.getProtocols()));

        for (final String protocol : referencedProtocols) {
            logger.info("Adding protocol - {} -> {}", protocol, factory);
            addProtocol(protocol, factory);
        }

        if (this.listener != null) {
            this.listener.protocolsAdded(referencedProtocols);
        }
    }

    protected synchronized void removeProtocols(final ProtocolManagerFactory<?> factory) {
        final Set<String> referencedProtocols = new HashSet<>(Arrays.asList(factory.getProtocols()));

        for (final String protocol : referencedProtocols) {
            logger.info("Removing protocol - {} -> {}", protocol, factory);
            removeProtocol(protocol, factory);
        }

        if (this.listener != null) {
            this.listener.protocolsRemoved(referencedProtocols);
        }
    }

    public synchronized Collection<ProtocolManagerFactory<?>> resolveProtocols(final Set<String> requiredProtocols) {
        final Map<String, ProtocolManagerFactory<?>> result = new HashMap<>();

        for (final String required : requiredProtocols) {
            final Collection<ProtocolManagerFactory<?>> factories = this.protocols.getOrDefault(required,
                    Collections.emptyList());
            if (factories.isEmpty()) {
                // return "unresolved"
                return null;
            }

            // just get the first one
            result.put(required, factories.iterator().next());
        }

        // we are resolved now ... add all others

        for (final Map.Entry<String, Collection<ProtocolManagerFactory<?>>> entry : this.protocols.entrySet()) {

            final String protocol = entry.getKey();
            final Collection<ProtocolManagerFactory<?>> factories = entry.getValue();

            for (final ProtocolManagerFactory<?> factory : factories) {
                if (!result.containsKey(protocol)) {
                    result.put(protocol, factory);
                    break;
                }
            }
        }

        // return the result

        return result.values();
    }

    private void addProtocol(final String protocol, final ProtocolManagerFactory<?> factory) {
        this.protocols.computeIfAbsent(protocol, p -> new ArrayList<>()).add(factory);
    }

    private void removeProtocol(final String protocol, final ProtocolManagerFactory<?> factory) {

        this.protocols.compute(protocol, (p, factories) -> {
            if (factories == null) {
                return null;
            }
            factories.remove(factory);
            return factories.isEmpty() ? null : factories;
        });

    }
}
