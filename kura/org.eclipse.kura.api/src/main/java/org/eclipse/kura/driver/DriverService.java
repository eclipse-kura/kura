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
 * The interface {@link DriverService} is an utility service API to provide useful
 * methods for {@link Driver}s
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface DriverService {

    /**
     * Gets the {@link Driver} instance by the provided {@link Driver} PID
     * ({@code kura.service.pid}).
     *
     * @param driverPid
     *            the {@link Driver} PID ({@code kura.service.pid}) to check
     * @return the {@link Driver} instance wrapped in {@link Optional}
     *         instance or an empty {@link Optional} instance
     * @throws NullPointerException
     *             if the provided {@link Driver} PID ({@code kura.service.pid})
     *             is {@code null}
     */
    public Optional<Driver> getDriver(String driverPid);

    /**
     * Gets the {@link Driver} PID ({@code kura.service.pid}) by the provided
     * {@link Driver} instance
     *
     * @param driver
     *            the {@link Driver} instance to check
     * @return the {@link Driver} PID ({@code kura.service.pid}) wrapped in
     *         {@link Optional} instance or an empty {@link Optional} instance
     * @throws NullPointerException
     *             if the provided {@link Driver} instance is {@code null}
     */
    public Optional<String> getDriverPid(Driver driver);

    /**
     * Returns the list containing all the available {@link Driver} instances
     *
     * @return the list of {@link Driver} instances available in service registry
     *         or empty list if no {@link Driver} instance is available
     */
    public List<Driver> listDrivers();

}
