/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.broker.artemis.core.internal;

import java.util.Arrays;
import java.util.Collection;
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ProtocolTracker {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolTracker.class);

    private final BundleContext context;

    private final Multimap<String, ProtocolManagerFactory<?>> protocols = HashMultimap.create();

    @SuppressWarnings("rawtypes")
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
        final Set<String> protocols = new HashSet<>(Arrays.asList(factory.getProtocols()));

        for (final String protocol : protocols) {
            logger.info("Adding protocol - {} -> {}", protocol, factory);
            this.protocols.put(protocol, factory);
        }

        if (this.listener != null) {
            this.listener.protocolsAdded(protocols);
        }
    }

    protected synchronized void removeProtocols(final ProtocolManagerFactory<?> factory) {
        final Set<String> protocols = new HashSet<>(Arrays.asList(factory.getProtocols()));

        for (final String protocol : protocols) {
            logger.info("Removing protocol - {} -> {}", protocol, factory);
            this.protocols.remove(protocol, factory);
        }

        if (this.listener != null) {
            this.listener.protocolsRemoved(protocols);
        }
    }

    public synchronized Collection<ProtocolManagerFactory<?>> resolveProtocols(final Set<String> requiredProtocols) {
        final Map<String, ProtocolManagerFactory<?>> result = new HashMap<>();

        for (final String required : requiredProtocols) {
            final Collection<ProtocolManagerFactory<?>> factories = this.protocols.get(required);
            if (factories.isEmpty()) {
                // return "unresolved"
                return null;
            }

            // just get the first one
            result.put(required, factories.iterator().next());
        }

        // we are resolved now ... add all others

        for (final Map.Entry<String, ProtocolManagerFactory<?>> entry : this.protocols.entries()) {
            if (!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        // return the result

        return result.values();
    }
}
