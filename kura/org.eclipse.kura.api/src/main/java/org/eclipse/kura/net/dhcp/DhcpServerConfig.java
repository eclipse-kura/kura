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
package org.eclipse.kura.net.dhcp;

import java.util.List;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Marker interface for all DHCP server configuration classes
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface DhcpServerConfig extends NetConfig {

    /**
     * Returns the interface name associated with this DhcpServerConfig
     *
     * @return a {@link String} representing the interface name
     */
    public String getInterfaceName();

    /**
     * Returns the {@link boolean} status associated with this DhcpServerConfig
     *
     * @return a {@link boolean} representing the status
     */
    public boolean isEnabled();

    /**
     * Returns the subnet associated with this DhcpServerConfig
     *
     * @return a {@link IPAddress } representing the subnet
     */
    public IPAddress getSubnet();

    /**
     * Returns the router IP address associated with this DhcpServerConfig
     *
     * @return a {@link IPAddress } representing the router IP address
     */
    public IPAddress getRouterAddress();

    /**
     * Returns the subnet mask associated with this DhcpServerConfig
     *
     * @return a {@link IPAddress } representing the subnet mask
     */
    public IPAddress getSubnetMask();

    /**
     * Returns the default lease time offered by the DHCP server
     *
     * @return the default lease time (in seconds) offered by the DHCP server
     */
    public int getDefaultLeaseTime();

    /**
     * Returns the maximum lease time offered by the DHCP server
     *
     * @return the maximum lease time (in seconds) offered by the DHCP server
     */
    public int getMaximumLeaseTime();

    /**
     * Returns the network prefix length for this DHCP server's address range
     * This is also known as the subnet mask in the context of IPv4 addresses.
     * Typical IPv4 values would be 8 (255.0.0.0), 16 (255.255.0.0) or 24 (255.255.255.0).
     * Typical IPv6 values would be 128 (::1/128) or 10 (fe80::203:baff:fe27:1243/10)
     *
     * @return a short representing the prefix length for the subnet of the DHCP server address range.
     */
    public short getPrefix();

    /**
     * Returns the starting DHCP server InetAddress to provide to DHCP clients.
     *
     * @return the starting address to provide to DHCP clients
     */
    public IPAddress getRangeStart();

    /**
     * Returns the ending DHCP server InetAddress to provide to DHCP clients.
     *
     * @return the ending address to provide to DHCP clients
     */
    public IPAddress getRangeEnd();

    /**
     * Returns whether or not DHCP clients should get DNS services.
     *
     * @return a boolean representing whether or not DHCP clients should receive DNS services.
     */
    public boolean isPassDns();

    /**
     * Returns the DNS servers associated with this DhcpServerConfig that will be passed to DHCP clients
     *
     * @return a {@link List } of IPAddresses that represent the DNS servers
     */
    public List<? extends IPAddress> getDnsServers();
}
