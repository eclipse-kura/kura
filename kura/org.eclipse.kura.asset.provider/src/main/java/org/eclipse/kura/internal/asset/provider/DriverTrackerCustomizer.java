/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.internal.asset.provider;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import org.eclipse.kura.asset.provider.BaseAsset;
import org.eclipse.kura.driver.Driver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DriverTrackerCustomizer is responsible for tracking the specific
 * driver as provided. It tracks for the service in the OSGi service registry
 * and it triggers the specific methods as soon as it is injected.
 */
public final class DriverTrackerCustomizer implements ServiceTrackerCustomizer<Driver, Driver> {

    private static final Logger logger = LoggerFactory.getLogger(DriverTrackerCustomizer.class);

    private final BaseAsset baseAsset;

    private final BundleContext context;

    private final String driverId;

    /**
     * Instantiates a new driver tracker.
     *
     * @param context
     *            the bundle context
     * @param baseAsset
     *            the asset
     * @param driverId
     *            the driver id
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public DriverTrackerCustomizer(final BundleContext context, final BaseAsset baseAsset, final String driverId) {
        requireNonNull(context, "Bundle context cannot be null");
        requireNonNull(baseAsset, "Asset cannot be null");
        requireNonNull(driverId, "Driver PID cannot be null");

        this.driverId = driverId;
        this.baseAsset = baseAsset;
        this.context = context;
    }

    /** {@inheritDoc} */
    @Override
    public Driver addingService(final ServiceReference<Driver> reference) {
        final Driver driver = this.context.getService(reference);
        if (reference.getProperty(KURA_SERVICE_PID).equals(this.driverId)) {
            logger.info("Driver has been found by the driver tracker... ==> adding service");
            this.baseAsset.setDriver(driver);
        }
        return driver;
    }

    /** {@inheritDoc} */
    @Override
    public void modifiedService(final ServiceReference<Driver> reference, final Driver service) {
        removedService(reference, service);
        addingService(reference);
    }

    /** {@inheritDoc} */
    @Override
    public void removedService(final ServiceReference<Driver> reference, final Driver service) {
        this.context.ungetService(reference);
        if (reference.getProperty(KURA_SERVICE_PID).equals(this.driverId)) {
            logger.info("Driver has been removed by the driver tracker... {}", service);
            this.baseAsset.unsetDriver();
        }
    }
}
