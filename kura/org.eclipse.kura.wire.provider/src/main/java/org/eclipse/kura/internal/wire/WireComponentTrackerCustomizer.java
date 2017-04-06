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
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.Iterator;
import java.util.List;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class {@link WireComponentTrackerCustomizer} represents an OSGi service tracker
 * to track all {@link WireComponent}s
 */
final class WireComponentTrackerCustomizer implements ServiceTrackerCustomizer<WireComponent, WireComponent> {

    private static final Logger logger = LoggerFactory.getLogger(WireComponentTrackerCustomizer.class);
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private final BundleContext context;
    private final List<String> wireEmitterPids;
    private final List<String> wireReceiverPids;
    private final WireServiceImpl wireService;

    /**
     * Constructor
     *
     * @param context
     *            the {@link BundleContext}
     * @param wireService
     *            the {@link WireService}
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    WireComponentTrackerCustomizer(final BundleContext context, final WireServiceImpl wireService)
            throws InvalidSyntaxException {
        requireNonNull(context, message.bundleContextNonNull());
        requireNonNull(wireService, message.wireServiceNonNull());

        this.wireEmitterPids = CollectionUtil.newArrayList();
        this.wireReceiverPids = CollectionUtil.newArrayList();
        this.wireService = wireService;
        this.context = context;
    }

    /** {@inheritDoc} */
    @Override
    public WireComponent addingService(final ServiceReference<WireComponent> reference) {
        final WireComponent service = this.context.getService(reference);
        logger.debug(message.addingWireComponent());
        final String property = String.valueOf(reference.getProperty(KURA_SERVICE_PID));
        if (service instanceof WireEmitter) {
            this.wireEmitterPids.add(property);
            logger.debug(message.registeringEmitter(property));
        }
        if (service instanceof WireReceiver) {
            this.wireReceiverPids.add(property);
            logger.debug(message.registeringReceiver(property));
        }

        this.wireService.createWires();

        logger.debug(message.addingWireComponentDone());
        return service;
    }

    /**
     * Gets the {@link WireEmitter} PIDs
     *
     * @return the {@link WireEmitter} PIDs
     */
    List<String> getWireEmitters() {
        return this.wireEmitterPids;
    }

    /**
     * Gets the {@link WireReceiver} PIDs
     *
     * @return the {@link WireReceiver} PIDs
     */
    List<String> getWireReceivers() {
        return this.wireReceiverPids;
    }

    /** {@inheritDoc} */
    @Override
    public void modifiedService(final ServiceReference<WireComponent> reference, final WireComponent service) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void removedService(final ServiceReference<WireComponent> reference, final WireComponent service) {
        logger.debug(message.removingWireComponent());
        final String property = String.valueOf(reference.getProperty(KURA_SERVICE_PID));
        if (property != null) {
            removeWireComponent(property);
            if (service instanceof WireEmitter) {
                this.wireEmitterPids.remove(property);
                logger.debug(message.deregisteringEmitter(property));
            }
            if (service instanceof WireReceiver) {
                this.wireReceiverPids.remove(property);
                logger.debug(message.deregisteringReceiver(property));
            }
        }
        this.context.ungetService(reference);
        logger.debug(message.removingWireComponentDone());
    }

    /**
     * Removes all the {@link WireConfiguration}s related to the provided PID
     * (kura.service.pid)
     *
     * @param pid
     *            the {@link WireComponent} PID
     * @throws NullPointerException
     *             if the argument is null
     */
    private void removeWireComponent(final String pid) {
        requireNonNull(pid, message.pidNonNull());
        final Iterator<WireConfiguration> wireConfigsIterator = this.wireService.getWireConfigurations().iterator();
        while (wireConfigsIterator.hasNext()) {
            final WireConfiguration wireConfiguration = wireConfigsIterator.next();
            if (wireConfiguration.getWire() != null && (pid.equals(wireConfiguration.getEmitterPid())
                    || pid.equals(wireConfiguration.getReceiverPid()))) {
                this.wireService.deleteWireConfiguration(wireConfiguration);
            }
        }
    }
}
