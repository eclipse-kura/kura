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

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service API for getting the network interfaces status.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetworkStatusService {

    /**
     * Return an optional {@link NetworkInterfaceStatus} of the given network
     * interface selected by its id. For Ethernet and WiFi interfaces, the
     * identifier is typically the interface name. For the modems, instead,
     * it is the usb or pci path.
     * If the interface doesn't exist, an Empty value is returned.
     *
     * @param interfaceId
     *            the identifier of the network interface
     * @return the {@link NetworkInterfaceStatus}
     * @throws KuraException
     *             when an error occurs while retrieving the status of the
     *             given interface
     */
    public Optional<NetworkInterfaceStatus> getNetworkStatus(String interfaceId) throws KuraException;

    /**
     * Return an optional {@link NetworkInterfaceStatus} of the given network
     * interface selected by its id.For Ethernet and WiFi interfaces, the
     * identifier is typically the interface name. For the modems, instead,
     * it is the usb or pci path.
     * If the interface doesn't exist, an Empty value is returned.
     * 
     * @param interfaceId
     *            the identifier of the network interface
     * @param recompute
     *            If set to true, the NetworkStatusService will perform some additional expensive operations (like AP
     *            scanning) to ensure the status returned by the method is up to date. If set to false, it will return
     *            the status as returned by the network configuration provider.
     * @return the {@link NetworkInterfaceStatus}
     * @throws KuraException
     *             when an error occurs while retrieving the status of the
     *             given interface
     */

    public Optional<NetworkInterfaceStatus> getNetworkStatus(String interfaceId, boolean recompute)
            throws KuraException;

    /**
     * Return the identifiers of the network interfaces detected in the
     * system. For Ethernet and WiFi interfaces, the identifier is typically
     * the interface name. For the modems, instead, it is the usb or pci path.
     *
     * @return a list containing the network interface identifiers
     */
    public List<String> getInterfaceIds() throws KuraException;

}