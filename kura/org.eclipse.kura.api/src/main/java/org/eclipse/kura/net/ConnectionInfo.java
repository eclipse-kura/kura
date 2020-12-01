/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for network interface 'connection info'. At runtime an interface may be associated with
 * gateways or DNS but the interface itself may not be active. If this is the case the ConnectionInfo
 * class is used to keep all relevant information in the event that this interface should become the
 * active one. This is necessary because many operating systems to not persist this information.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ConnectionInfo {

    /**
     * Gets the gateway address associated with this interface
     *
     * @return A IP4Address representing the gateway if it is not null
     */
    public IP4Address getGateway();

    /**
     * Gets the DNS addresses associated with this interface
     *
     * @return A List of IP4Address objects representing the DNS of this interface. If there are none it returns an
     *         empty list.
     */
    public List<IP4Address> getDnsServers();

    /**
     * Gets the interface name associated with this connection information
     *
     * @return The interface name associated with this connection information
     */
    public String getIfaceName();

    /**
     * Reports IP address
     *
     * @return IP address as {@link IP4Address}
     */
    public IP4Address getIpAddress();
}
