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

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Nullable;

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

	/**
	 * Prepares new driver event.
	 *
	 * @param driverRecord
	 *            the associated driver record
	 * @return the newly created driver event
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public DriverEvent newDriverEvent(final DriverRecord driverRecord);

	/**
	 * Prepares new driver record.
	 *
	 * @return the newly created driver record
	 */
	public DriverRecord newDriverRecord();

	/**
	 * Prepares the driver specific operation status.
	 *
	 * @param driverFlag
	 *            the driver flag
	 * @return the newly created status instance
	 * @throws KuraRuntimeException
	 *             if the driver flag is null
	 */
	public DriverStatus newDriverStatus(final DriverFlag driverFlag);

	/**
	 * Prepares the driver specific operation status.
	 *
	 * @param driverFlag
	 *            the driver flag
	 * @param exceptionMessage
	 *            the exception message
	 * @param exception
	 *            the exception
	 * @return the newly created status instance
	 * @throws KuraRuntimeException
	 *             if the driver flag is null
	 */
	public DriverStatus newDriverStatus(final DriverFlag driverFlag, @Nullable final String exceptionMessage,
			@Nullable final Exception exception);

}
