/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *
 ******************************************************************************/
package org.eclipse.kura.net.status;

import java.util.List;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service API for getting the network interfaces status.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetworkStatusService {

    /**
     * Return the list of the {@link NetworkInterfaceStatus} of all network
     * interfaces detected in the system.
     * 
     * @return a list containing the status of all the network interfaces
     */
    public List<NetworkInterfaceStatus> getNetworkStatus();

    /**
     * Return an optional {@link NetworkInterfaceStatus} of the given network
     * interface. If the interface doesn't exist, an Empty value is returned.
     * 
     * @param interfaceName the name of the network interface
     * @return the {@link NetworkInterfaceStatus}
     */
    public Optional<NetworkInterfaceStatus> getNetworkStatus(String interfaceName);

    /**
     * Return the names of the network interfaces detected in the system.
     * 
     * @return a list containing the network interface names
     */
    public List<String> getInterfaceNames();

}