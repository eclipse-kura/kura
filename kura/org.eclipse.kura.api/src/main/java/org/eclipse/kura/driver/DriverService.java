/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.driver;

import java.util.List;

/**
 * The interface DriverService is an utility service API to provide useful
 * methods for drivers.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface DriverService {

    /**
     * Gets the driver instance by the provided driver PID
     * ({@code kura.service.pid}).
     *
     * @param driverPid
     *            the driver PID to check
     * @return the driver instance
     */
    public Driver getDriver(String driverPid);

    /**
     * Gets the driver PID. ({@code kura.service.pid}) by the provided driver
     * instance
     *
     * @param driver
     *            the driver instance to check
     * @return the driver PID
     */
    public String getDriverPid(Driver driver);

    /**
     * Returns the list containing all the available driver instances
     *
     * @return the list of drivers available in service registry
     */
    public List<Driver> listDrivers();

}
