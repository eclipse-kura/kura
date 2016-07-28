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

import org.eclipse.kura.KuraRuntimeException;

/**
 * The interface DriverService is an utility service API to provide useful
 * methods for drivers.
 */
public interface DriverService {

	/**
	 * Gets the actual driver instance by the provided driver identifier
	 * ({@code kura.service.pid})
	 *
	 * @param driverId
	 *            the unique identity of the driver to check
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
	 * Gets the driver PID. ({@code service.pid}) by the provided driver
	 * instance
	 *
	 * @param driver
	 *            the driver instance to check
	 * @return the driver PID
	 */
	public String getDriverPid(Driver driver);

	/**
	 * Gets the driver PID. ({@code service.pid}) by the provided driver
	 * identifier ({@code kura.service.pid})
	 *
	 * @param driverId
	 *            the unique identifier of the driver to check
	 * @return the driver PID
	 */
	public String getDriverPid(String driverId);

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
	 * @param channelId
	 *            the channel identifier
	 * @return the newly created driver record
	 * @throws KuraRuntimeException
	 *             if the argument is less than or equal to zero
	 */
	public DriverRecord newDriverRecord(final long channelId);

}
