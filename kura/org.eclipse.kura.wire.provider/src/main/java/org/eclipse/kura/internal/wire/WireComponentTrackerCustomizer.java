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

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class WireComponentTrackerCustomizer represents an OSGi service tracker
 * to track Wire Components (Wire Receiver and Wire Emitter)
 */
final class WireComponentTrackerCustomizer implements ServiceTrackerCustomizer<WireComponent, WireComponent> {

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(WireComponentTrackerCustomizer.class);

    /** Localization Resource */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Bundle Context */
    private final BundleContext context;

    /** The wire emitter PIDs. */
    private final List<String> wireEmitters;

    /** The wire receiver PIDs. */
    private final List<String> wireReceivers;

    /** The wire service. */
    private final WireServiceImpl wireService;

    /**
     * Instantiates a new wire service tracker.
     *
     * @param context
     *            the bundle context
     * @param wireService
     *            the wire service
     * @throws InvalidSyntaxException
     *             the invalid syntax exception
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    WireComponentTrackerCustomizer(final BundleContext context, final WireServiceImpl wireService)
            throws InvalidSyntaxException {
        requireNonNull(context, message.bundleContextNonNull());
        requireNonNull(wireService, message.wireServiceNonNull());

        this.wireEmitters = CollectionUtil.newArrayList();
        this.wireReceivers = CollectionUtil.newArrayList();
        this.wireService = wireService;
        this.context = context;
        this.searchPreExistingWireComponents();
    }

    /** {@inheritDoc} */
    @Override
    public WireComponent addingService(final ServiceReference<WireComponent> reference) {
        final WireComponent service = this.context.getService(reference);
        logger.debug(message.addingWireComponent());
        final String property = String.valueOf(reference.getProperty(KURA_SERVICE_PID));
        if (service instanceof WireEmitter) {
            this.wireEmitters.add(property);
            logger.debug(message.registeringEmitter(property));
        }
        if (service instanceof WireReceiver) {
            this.wireReceivers.add(property);
            logger.debug(message.registeringReceiver(property));
        }
        try {
            this.wireService.createWires();
        } catch (final KuraException e) {
            logger.error(message.errorCreatingWires() + ThrowableUtil.stackTraceAsString(e));
        }
        logger.debug(message.addingWireComponentDone());
        return service;
    }

    /**
     * Gets the wire emitter PIDs.
     *
     * @return the wire emitter PIDs
     */
    List<String> getWireEmitters() {
        return this.wireEmitters;
    }

    /**
     * Gets the wire receiver PIDs.
     *
     * @return the wire receiver PIDs
     */
    List<String> getWireReceivers() {
        return this.wireReceivers;
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
            this.removeWireComponent(property);
            if (service instanceof WireEmitter) {
                this.wireEmitters.remove(property);
                logger.debug(message.deregisteringEmitter(property));
            }
            if (service instanceof WireReceiver) {
                this.wireReceivers.remove(property);
                logger.debug(message.deregisteringReceiver(property));
            }
        }
        this.context.ungetService(reference);
        logger.debug(message.removingWireComponentDone());
    }

    /**
     * Removes all the Wire Configurations related to the provided PID
     * (kura.service.pid)
     *
     * @param pid
     *            the wire component PID
     * @throws NullPointerException
     *             if the argument is null
     */
    private void removeWireComponent(final String pid) {
        requireNonNull(pid, message.pidNonNull());
        for (final WireConfiguration wireConfiguration : this.wireService.getWireConfigurations()) {
            if ((wireConfiguration.getWire() != null) && (pid.equals(wireConfiguration.getEmitterPid())
                    || (pid.equals(wireConfiguration.getReceiverPid())))) {
                this.wireService.deleteWireConfiguration(wireConfiguration);
            }
        }
    }

    /**
     * Searches for the service instances of type Wire Components
     */
    private void searchPreExistingWireComponents() {
        final ServiceReference<WireComponent>[] refs = ServiceUtil.getServiceReferences(this.context,
                WireComponent.class, null);
        for (final ServiceReference<?> ref : refs) {
            final WireComponent wc = (WireComponent) this.context.getService(ref);
            final Object pidProperty = ref.getProperty(KURA_SERVICE_PID);
            if (wc instanceof WireEmitter) {
                this.wireEmitters.add(String.valueOf(pidProperty));
            }
            if (wc instanceof WireReceiver) {
                this.wireReceivers.add(String.valueOf(pidProperty));
            }
        }
        ServiceUtil.ungetServiceReferences(this.context, refs);
    }

}
