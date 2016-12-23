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

/**
 * The abstract representation of a DhcpServerConfig object.
 *
 * @author eurotech
 *
 * @param <T>
 *            is the an appropriate subclass of IPAddress
 */
public abstract class DhcpServerConfigIP<T extends IPAddress> implements DhcpServerConfig {

    private String m_interfaceName;
    private boolean m_enabled;
    private T m_subnet;
    private T m_routerAddress;
    private T m_subnetMask;
    private int m_defaultLeaseTime;
    private int m_maximumLeaseTime;
    private short m_prefix;
    private T m_rangeStart;
    private T m_rangeEnd;
    private boolean m_passDns;
    private List<T> m_dnsServers;

    /**
     * The basic Constructor for a DhcpServerConfigIP
     *
     * @param interfaceName
     *            the interface name associated with the DhcpServerConfig
     * @param enabled
     *            the status of the DhcpServer as a boolean
     * @param subnet
     *            the subnet of the DhcpServerConfig
     * @param routerAddress
     *            the router IPAddress
     * @param subnetMask
     *            the subnet mask of the DhcpServerConfig
     * @param defaultLeaseTime
     *            the default lease time to issue to DHCP clients
     * @param maximumLeaseTime
     *            the maximum lease time to issue to DHCP clients
     * @param prefix
     *            the network prefix associated with the DhcpServerConfig
     * @param rangeStart
     *            the network starting address to issue to DHCP clients
     * @param rangeEnd
     *            the network ending address to issue to DHCP clients
     * @param passDns
     *            whether or not to pass DNS to DHCP clients
     * @param dnsServers
     *            the DNS servers that will get passed to DHCP clients if passDns is true
     */
    public DhcpServerConfigIP(String interfaceName, boolean enabled, T subnet, T routerAddress, T subnetMask,
            int defaultLeaseTime, int maximumLeaseTime, short prefix, T rangeStart, T rangeEnd, boolean passDns,
            List<T> dnsServers) {
        super();

        this.m_interfaceName = interfaceName;
        this.m_enabled = enabled;
        this.m_subnet = subnet;
        this.m_routerAddress = routerAddress;
        this.m_subnetMask = subnetMask;
        this.m_defaultLeaseTime = defaultLeaseTime;
        this.m_maximumLeaseTime = maximumLeaseTime;
        this.m_prefix = prefix;
        this.m_rangeStart = rangeStart;
        this.m_rangeEnd = rangeEnd;
        this.m_passDns = passDns;
        this.m_dnsServers = dnsServers;
    }

    @Override
    public String getInterfaceName() {
        return this.m_interfaceName;
    }

    /**
     * sets the interface name for the DhcpServerConfig
     *
     * @param interfaceName
     *            the interface name in the form of a {@link String}
     */
    public void setInterfaceName(String interfaceName) {
        this.m_interfaceName = interfaceName;
    }

    @Override
    public boolean isEnabled() {
        return this.m_enabled;
    }

    /**
     * sets the status for the DhcpServerConfig
     *
     * @param enabled
     *            the Dhcp Server status in the form of a {@link boolean}
     */
    public void setEnabledRouterMode(boolean enabled) {
        this.m_enabled = enabled;
    }

    @Override
    public T getSubnet() {
        return this.m_subnet;
    }

    /**
     * sets the subnet for the DhcpServerConfig
     *
     * @param subnet
     *            the subnet in the form of a {@link IPAddress}
     */
    public void setSubnet(T subnet) {
        this.m_subnet = subnet;
    }

    @Override
    public T getRouterAddress() {
        return this.m_routerAddress;
    }

    /**
     * sets the router IPAddress for the DhcpServerConfig
     *
     * @param routerAddress
     *            the router IPAddress in the form of a {@link IPAddress}
     */
    public void setRouterAddress(T routerAddress) {
        this.m_routerAddress = routerAddress;
    }

    @Override
    public T getSubnetMask() {
        return this.m_subnetMask;
    }

    /**
     * sets the subnet mask for the DhcpServerConfig
     *
     * @param subnetMask
     *            the subnet mask in the form of a {@link IPAddress}
     */
    public void setSubnetMask(T subnetMask) {
        this.m_subnetMask = subnetMask;
    }

    @Override
    public int getDefaultLeaseTime() {
        return this.m_defaultLeaseTime;
    }

    /**
     * sets the default lease time for DHCP clients
     *
     * @param defaultLeaseTime
     *            the default lease time
     */
    public void setDefaultLeaseTime(int defaultLeaseTime) {
        this.m_defaultLeaseTime = defaultLeaseTime;
    }

    @Override
    public int getMaximumLeaseTime() {
        return this.m_maximumLeaseTime;
    }

    /**
     * sets the maximum lease time for DHCP clients
     *
     * @param maximumLeaseTime
     *            the maximum lease time
     */
    public void setMaximumLeaseTime(int maximumLeaseTime) {
        this.m_maximumLeaseTime = maximumLeaseTime;
    }

    @Override
    public short getPrefix() {
        return this.m_prefix;
    }

    /**
     * sets the network prefix for the DhcpServerConfig
     *
     * @param prefix
     *            the prefix
     */
    public void setPrefix(short prefix) {
        this.m_prefix = prefix;
    }

    @Override
    public T getRangeStart() {
        return this.m_rangeStart;
    }

    /**
     * sets the starting IPAddress in the pool for the DHCP clients
     *
     * @param m_rangeStart
     *            the starting IPAddress
     */
    public void setRangeStart(T m_rangeStart) {
        this.m_rangeStart = m_rangeStart;
    }

    @Override
    public T getRangeEnd() {
        return this.m_rangeEnd;
    }

    /**
     * sets the ending IPAddress in the pool for the DHCP clients
     *
     * @param rangeEnd
     *            the ending IPAddress
     */
    public void setRangeEnd(T rangeEnd) {
        this.m_rangeEnd = rangeEnd;
    }

    @Override
    public boolean isPassDns() {
        return this.m_passDns;
    }

    /**
     * whether or not to pass DNS to DHCP clients
     *
     * @param passDns
     *            true to pass, false to not
     */
    public void setPassDns(boolean passDns) {
        this.m_passDns = passDns;
    }

    @Override
    public List<T> getDnsServers() {
        return this.m_dnsServers;
    }

    /**
     * the DNS servers to pass to DHCP clients if passDns is set to true
     *
     * @param m_dnsServers
     *            the DNS servers to pass
     */
    public void setDnsServers(List<T> m_dnsServers) {
        this.m_dnsServers = m_dnsServers;
    }

    @Override
    public boolean isValid() {
        // TODO - implement
        return true;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("# enabled? ").append(this.m_enabled).append("\n");
        sb.append("# prefix: ").append(this.m_prefix).append("\n");
        sb.append("# pass DNS? ").append(this.m_passDns).append("\n\n");

        sb.append(
                "subnet " + this.m_subnet.getHostAddress() + " netmask " + this.m_subnetMask.getHostAddress() + " {\n");

        // DNS servers
        if (this.m_passDns && this.m_dnsServers != null && this.m_dnsServers.size() > 0) {
            sb.append("    option domain-name-servers ");
            for (int i = 0; i < this.m_dnsServers.size(); i++) {
                if (this.m_dnsServers.get(i) != null) {
                    sb.append(this.m_dnsServers.get(i).getHostAddress());
                }

                if (i + 1 == this.m_dnsServers.size()) {
                    sb.append(";\n\n");
                } else {
                    sb.append(",");
                }
            }
        }
        // interface
        if (this.m_interfaceName != null) {
            sb.append("    interface " + this.m_interfaceName + ";\n");
        }
        // router address
        if (this.m_routerAddress != null) {
            sb.append("    option routers " + this.m_routerAddress.getHostAddress() + ";\n");
        }
        // if DNS should not be forwarded, add the following lines
        if (!this.m_passDns) {
            sb.append("    ddns-update-style none;\n");
            sb.append("    ddns-updates off;\n");
        }
        // Lease times
        sb.append("    default-lease-time " + this.m_defaultLeaseTime + ";\n");
        if (this.m_maximumLeaseTime > -1) {
            sb.append("    max-lease-time " + this.m_maximumLeaseTime + ";\n");
        }

        // Add the pool and range
        sb.append("    pool {\n");
        sb.append(
                "        range " + this.m_rangeStart.getHostAddress() + " " + this.m_rangeEnd.getHostAddress() + ";\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        @SuppressWarnings("rawtypes")
        DhcpServerConfigIP other = (DhcpServerConfigIP) obj;

        if (this.m_enabled != other.m_enabled) {
            return false;
        }

        if (this.m_interfaceName == null) {
            if (other.m_interfaceName != null) {
                return false;
            }
        } else if (!this.m_interfaceName.equals(other.m_interfaceName)) {
            return false;
        }

        if (this.m_subnet == null) {
            if (other.m_subnet != null) {
                return false;
            }
        } else if (!this.m_subnet.equals(other.m_subnet)) {
            return false;
        }

        if (this.m_routerAddress == null) {
            if (other.m_routerAddress != null) {
                return false;
            }
        } else if (!this.m_routerAddress.equals(other.m_routerAddress)) {
            return false;
        }

        if (this.m_subnetMask == null) {
            if (other.m_subnetMask != null) {
                return false;
            }
        } else if (!this.m_subnetMask.equals(other.m_subnetMask)) {
            return false;
        }

        if (this.m_defaultLeaseTime != other.m_defaultLeaseTime) {
            return false;
        }

        if (this.m_maximumLeaseTime != other.m_maximumLeaseTime) {
            return false;
        }

        if (this.m_prefix != other.m_prefix) {
            return false;
        }

        if (this.m_rangeStart == null) {
            if (other.m_rangeStart != null) {
                return false;
            }
        } else if (!this.m_rangeStart.equals(other.m_rangeStart)) {
            return false;
        }

        if (this.m_rangeEnd == null) {
            if (other.m_rangeEnd != null) {
                return false;
            }
        } else if (!this.m_rangeEnd.equals(other.m_rangeEnd)) {
            return false;
        }

        if (this.m_passDns != other.m_passDns) {
            return false;
        }

        if (this.m_dnsServers == null) {
            if (other.m_dnsServers != null) {
                return false;
            }
        } else if (!this.m_dnsServers.equals(other.m_dnsServers)) {
            return false;
        }

        return true;
    }
}
