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
package org.eclipse.kura.driver;

import java.util.List;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The interface DriverService is an utility service API to provide useful
 * methods for drivers
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface DriverService {

    /**
     * Gets the driver instance by the provided driver PID
     * ({@code kura.service.pid}).
     *
     * @param driverPid
     *            the driver PID to check
     * @return the driver instance
     * @throws NullPointerException
     *             if the provided driver PID is null
     */
    public Driver getDriver(String driverPid);

    /**
     * Gets the driver PID. ({@code kura.service.pid}) by the provided driver
     * instance
     *
     * @param driver
     *            the driver instance to check
     * @return the driver PID
     * @throws NullPointerException
     *             if the provided driver instance is null
     */
    public String getDriverPid(Driver driver);

    /**
     * Returns the list containing all the available driver instances
     *
     * @return the list of drivers available in service registry or empty list if no
     *         drivers are available
     */
    public List<Driver> listDrivers();

    /**
     * Returns the {@link DriverDescriptor} corresponding to the Driver instance
     * identified by the provided Driver {@code kura.service.pid}.
     *
     * @param driverPid
     *            the Driver {@code kura.service.pid} that identifies a Driver
     *            Instance
     * @return the {@link DriverDescriptor} corresponding to the provided method
     *         argument. Or an empty Optional is the provided argument is not a Driver {@code kura.service.pid}
     * @since 1.4
     */
    Optional<DriverDescriptor> getDriverDescriptor(String driverPid);

    /**
     * Returns a list of {@link DriverDescriptor} objects that correspond to the
     * entire list of Driver Instances in the Framework.
     *
     * @return a list of {@link DriverDescriptor}
     * @since 1.4
     */
    List<DriverDescriptor> listDriverDescriptors();

}
