/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.driver.descriptor;

import java.util.List;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The DriverDescriptorService interface is an utility service API to get descriptors for drivers
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.4
 */
@ProviderType
public interface DriverDescriptorService {

    /**
     * Returns the {@link DriverDescriptor} corresponding to the Driver instance
     * identified by the provided Driver {@code kura.service.pid}.
     *
     * @param driverPid
     *            the Driver {@code kura.service.pid} that identifies a Driver
     *            Instance
     * @return the {@link DriverDescriptor} corresponding to the provided method
     *         argument. Or an empty Optional is the provided argument is not a Driver {@code kura.service.pid}
     * @throws NullPointerException
     *             if the provided driver PID is null
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
