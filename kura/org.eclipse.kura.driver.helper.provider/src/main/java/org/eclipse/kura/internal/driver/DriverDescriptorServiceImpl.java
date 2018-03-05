/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.driver;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.descriptor.DriverDescriptor;
import org.eclipse.kura.driver.descriptor.DriverDescriptorService;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverDescriptorServiceImpl implements DriverDescriptorService {

    private static final Logger logger = LoggerFactory.getLogger(DriverDescriptorServiceImpl.class);

    private BundleContext bundleContext;

    public void activate(ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
    }

    @Override
    public Optional<DriverDescriptor> getDriverDescriptor(String driverPid) {
        requireNonNull(driverPid, "Driver PID cannot be null");
        DriverDescriptor driverDescriptor = null;

        String filterString = String.format("(&(kura.service.pid=%s))", driverPid);

        final ServiceReference<Driver>[] refs = getDriverServiceReferences(filterString);
        try {
            for (final ServiceReference<Driver> driverServiceReference : refs) {
                String factoryPid = driverServiceReference.getProperty(SERVICE_FACTORYPID).toString();
                Driver driver = this.bundleContext.getService(driverServiceReference);
                driverDescriptor = newDriverDescriptor(driverPid, factoryPid, driver);
            }
        } finally {
            ungetDriverServiceReferences(refs);
        }

        return Optional.ofNullable(driverDescriptor);
    }

    @Override
    public List<DriverDescriptor> listDriverDescriptors() {
        List<DriverDescriptor> driverDescriptors = new ArrayList<>();

        final ServiceReference<Driver>[] refs = getDriverServiceReferences(null);
        try {
            for (final ServiceReference<Driver> driverServiceReference : refs) {
                String driverPid = driverServiceReference.getProperty(KURA_SERVICE_PID).toString();
                String factoryPid = driverServiceReference.getProperty(SERVICE_FACTORYPID).toString();
                Driver driver = this.bundleContext.getService(driverServiceReference);
                driverDescriptors.add(newDriverDescriptor(driverPid, factoryPid, driver));
            }
        } finally {
            ungetDriverServiceReferences(refs);
        }

        return driverDescriptors;
    }

    private DriverDescriptor newDriverDescriptor(String driverPid, String factoryPid, Driver driver) {
        Object channelDescriptorObj = null;
        try {
            channelDescriptorObj = driver.getChannelDescriptor().getDescriptor();
        } catch (Exception e) {
            logger.warn("Failed to get driver descriptor for pid: {}", driverPid, e);
        }
        return new DriverDescriptor(driverPid, factoryPid, channelDescriptorObj);
    }

    protected ServiceReference<Driver>[] getDriverServiceReferences(final String filter) {
        return ServiceUtil.getServiceReferences(this.bundleContext, Driver.class, filter);
    }

    protected void ungetDriverServiceReferences(final ServiceReference<Driver>[] refs) {
        ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
    }
}
