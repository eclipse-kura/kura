/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.wire.WireComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class WireComponentTrackerCustomizer represents an OSGi service tracker
 * to track Wire Components (Wire Receiver and Wire Emitter)
 */
final class WireComponentTrackerCustomizer implements ServiceTrackerCustomizer<WireComponent, WireComponent> {

    private static final Logger logger = LoggerFactory.getLogger(WireComponentTrackerCustomizer.class);

    private final BundleContext context;

    private final WireGraphServiceImpl wireGraphService;

    /**
     * Instantiates a new wire service tracker.
     *
     * @param context
     *            the bundle context
     * @param wireGraphService
     *            the wire graph service
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    WireComponentTrackerCustomizer(final BundleContext context, final WireGraphServiceImpl wireGraphService) {
        requireNonNull(context, "Bundle context cannot be null");
        requireNonNull(wireGraphService, "Wire Graph Service cannot be null");

        this.wireGraphService = wireGraphService;
        this.context = context;

    }

    /** {@inheritDoc} */
    @Override
    public WireComponent addingService(final ServiceReference<WireComponent> reference) {
        final WireComponent service = this.context.getService(reference);
        logger.debug("Adding Wire Components....");

        this.wireGraphService.createWires();

        logger.debug("Adding Wire Components....Done");
        return service;
    }

    /** {@inheritDoc} */
    @Override
    public void modifiedService(final ServiceReference<WireComponent> reference, final WireComponent service) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void removedService(final ServiceReference<WireComponent> reference, final WireComponent service) {
        this.context.ungetService(reference);
    }
}