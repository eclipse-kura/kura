/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.driver;

import java.util.List;

/**
 * The interface DriverService is an utility service API to provide useful
 * methods for drivers.
 */
public interface DriverService {

	/**
	 * Gets the driver instance by the provided driver ID
	 * ({@code kura.service.pid}).
	 *
	 * @param driverId
	 *            the driver ID to check
	 * @return the driver instance
	 */
	public Driver getDriver(String driverId);

	/**
	 * Gets the driver ID. ({@code kura.service.pid}) by the provided driver
	 * instance
	 *
	 * @param driver
	 *            the driver instance to check
	 * @return the driver ID
	 */
	public String getDriverId(Driver driver);

	/**
	 * Returns the list containing all the available driver instances
	 *
	 * @return the list of drivers available in service registry
	 */
	public List<Driver> listAvailableDrivers();

}
