/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represents a Network Interface address as currently running on the system
 * In short it's an IP address, a subnet mask and a broadcast address when the address is an IPv4 one.
 * An IP address and a network prefix length in the case of IPv6 address.
 * Both IPv4 and IPv6 addresses will have a gateway and one or more DNS addresses.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetInterfaceAddress {

    /**
     * Returns an InetAddress for this address.
     *
     * @return the address
     */
    public IPAddress getAddress();

    /**
     * Returns the network prefix length for this address.
     * This is also known as the subnet mask in the context of IPv4 addresses.
     * Typical IPv4 values would be 8 (255.0.0.0), 16 (255.255.0.0) or 24 (255.255.255.0).
     * Typical IPv6 values would be 128 (::1/128) or 10 (fe80::203:baff:fe27:1243/10)
     *
     * @return a short representing the prefix length for the subnet of that address.
     */
    public short getNetworkPrefixLength();

    /**
     * Returns the network mask for this address
     * Typical IPv4 values would be 255.0.0.0, 255.255.0.0 or 255.255.255.0.
     * Typical IPv6 values would be ::1/128 or fe80::203:baff:fe27:1243/10
     *
     * @return an IPaddress representing the subnet mask of that address
     */
    public IPAddress getNetmask();

    /**
     * Returns the InetAddress for the gateway.
     *
     * @return the gateway address
     */
    public IPAddress getGateway();

    /**
     * Returns an InetAddress for the broadcast address for this InterfaceAddress.
     *
     * @return the broadcast address
     */
    public IPAddress getBroadcast();

    /**
     * Gets the list of DNS servers associated with this interface
     *
     * @return the list of DNS servers
     */
    public List<? extends IPAddress> getDnsServers();
}
