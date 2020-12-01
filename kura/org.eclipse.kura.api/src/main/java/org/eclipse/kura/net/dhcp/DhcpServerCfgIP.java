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
package org.eclipse.kura.net.dhcp;

import java.util.List;

import org.eclipse.kura.net.IPAddress;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The abstract representation of a 'networking' portion of DhcpServerConfig object.
 *
 * @param <T>
 *            is the an appropriate subclass of IPAddress
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.2
 */
@ProviderType
public abstract class DhcpServerCfgIP<T extends IPAddress> {

    private T subnet;
    private T subnetMask;
    private short prefix;
    private T routerAddress;
    private T rangeStart;
    private T rangeEnd;
    private List<T> dnsServers;

    /**
     * The basic Constructor for a DhcpServerConfigIP
     *
     * @param subnet
     *            the subnet of the DhcpServerConfig
     * @param subnetMask
     *            the subnet mask of the DhcpServerConfig
     * @param prefix
     *            the network prefix associated with the DhcpServerConfig
     * @param routerAddress
     *            the router IPAddress
     * @param rangeStart
     *            the network starting address to issue to DHCP clients
     * @param rangeEnd
     *            the network ending address to issue to DHCP clients
     * @param dnsServers
     *            the DNS servers that will get passed to DHCP clients if passDns is true
     */
    public DhcpServerCfgIP(T subnet, T subnetMask, short prefix, T routerAddress, T rangeStart, T rangeEnd,
            List<T> dnsServers) {
        super();
        this.subnet = subnet;
        this.subnetMask = subnetMask;
        this.prefix = prefix;
        this.routerAddress = routerAddress;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.dnsServers = dnsServers;
    }

    public T getSubnet() {
        return this.subnet;
    }

    public void setSubnet(T subnet) {
        this.subnet = subnet;
    }

    public T getSubnetMask() {
        return this.subnetMask;
    }

    public void setSubnetMask(T subnetMask) {
        this.subnetMask = subnetMask;
    }

    public short getPrefix() {
        return this.prefix;
    }

    public void setPrefix(short prefix) {
        this.prefix = prefix;
    }

    public T getRouterAddress() {
        return this.routerAddress;
    }

    public void setRouterAddress(T routerAddress) {
        this.routerAddress = routerAddress;
    }

    public T getRangeStart() {
        return this.rangeStart;
    }

    public void setRangeStart(T rangeStart) {
        this.rangeStart = rangeStart;
    }

    public T getRangeEnd() {
        return this.rangeEnd;
    }

    public void setRangeEnd(T rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public List<T> getDnsServers() {
        return this.dnsServers;
    }

    public void setDnsServers(List<T> dnsServers) {
        this.dnsServers = dnsServers;
    }
}
