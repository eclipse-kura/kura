/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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
 * The abstract representation of a DhcpServerConfig object.
 *
 * @param <T>
 *            is the an appropriate subclass of IPAddress
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public abstract class DhcpServerConfigIP<T extends IPAddress> implements DhcpServerConfig {

    private String interfaceName;
    private boolean enabled;
    private T subnet;
    private T routerAddress;
    private T subnetMask;
    private int defaultLeaseTime;
    private int maximumLeaseTime;
    private short prefix;
    private T rangeStart;
    private T rangeEnd;
    private boolean passDns;
    private List<T> dnsServers;

    /**
     * The basic Constructor for a DhcpServerConfigIP
     *
     * @param interfaceName
     *                         the interface name associated with the
     *                         DhcpServerConfig
     * @param enabled
     *                         the status of the DhcpServer as a boolean
     * @param subnet
     *                         the subnet of the DhcpServerConfig
     * @param routerAddress
     *                         the router IPAddress
     * @param subnetMask
     *                         the subnet mask of the DhcpServerConfig
     * @param defaultLeaseTime
     *                         the default lease time to issue to DHCP clients
     * @param maximumLeaseTime
     *                         the maximum lease time to issue to DHCP clients
     * @param prefix
     *                         the network prefix associated with the
     *                         DhcpServerConfig
     * @param rangeStart
     *                         the network starting address to issue to DHCP clients
     * @param rangeEnd
     *                         the network ending address to issue to DHCP clients
     * @param passDns
     *                         whether or not to pass DNS to DHCP clients
     * @param dnsServers
     *                         the DNS servers that will get passed to DHCP clients
     *                         if passDns is true
     */
    @Deprecated
    @SuppressWarnings("checkstyle:parameterNumber")
    public DhcpServerConfigIP(String interfaceName, boolean enabled, T subnet, T routerAddress, T subnetMask,
            int defaultLeaseTime, int maximumLeaseTime, short prefix, T rangeStart, T rangeEnd, boolean passDns,
            List<T> dnsServers) {
        super();

        this.interfaceName = interfaceName;
        this.enabled = enabled;
        this.subnet = subnet;
        this.routerAddress = routerAddress;
        this.subnetMask = subnetMask;
        this.defaultLeaseTime = defaultLeaseTime;
        this.maximumLeaseTime = maximumLeaseTime;
        this.prefix = prefix;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.passDns = passDns;
        this.dnsServers = dnsServers;
    }

    /**
     * The basic Constructor for a DhcpServerConfigIP
     *
     * @param dhcpServerCfg
     *                        DHCP server configuration
     * @param dhcpServerCfgIP
     *                        'network' configuration
     * @since 1.2
     */
    public DhcpServerConfigIP(DhcpServerCfg dhcpServerCfg, DhcpServerCfgIP<T> dhcpServerCfgIP) {
        this.interfaceName = dhcpServerCfg.getInterfaceName();
        this.enabled = dhcpServerCfg.isEnabled();
        this.subnet = dhcpServerCfgIP.getSubnet();
        this.routerAddress = dhcpServerCfgIP.getRouterAddress();
        this.subnetMask = dhcpServerCfgIP.getSubnetMask();
        this.defaultLeaseTime = dhcpServerCfg.getDefaultLeaseTime();
        this.maximumLeaseTime = dhcpServerCfg.getMaximumLeaseTime();
        this.prefix = dhcpServerCfgIP.getPrefix();
        this.rangeStart = dhcpServerCfgIP.getRangeStart();
        this.rangeEnd = dhcpServerCfgIP.getRangeEnd();
        this.passDns = dhcpServerCfg.isPassDns();
        this.dnsServers = dhcpServerCfgIP.getDnsServers();
    }

    @Override
    public String getInterfaceName() {
        return this.interfaceName;
    }

    /**
     * sets the interface name for the DhcpServerConfig
     *
     * @param interfaceName
     *                      the interface name in the form of a {@link String}
     */
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * sets the status for the DhcpServerConfig
     *
     * @param enabled
     *                the Dhcp Server status in the form of a {@link boolean}
     */
    public void setEnabledRouterMode(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public T getSubnet() {
        return this.subnet;
    }

    /**
     * sets the subnet for the DhcpServerConfig
     *
     * @param subnet
     *               the subnet in the form of a {@link IPAddress}
     */
    public void setSubnet(T subnet) {
        this.subnet = subnet;
    }

    @Override
    public T getRouterAddress() {
        return this.routerAddress;
    }

    /**
     * sets the router IPAddress for the DhcpServerConfig
     *
     * @param routerAddress
     *                      the router IPAddress in the form of a {@link IPAddress}
     */
    public void setRouterAddress(T routerAddress) {
        this.routerAddress = routerAddress;
    }

    @Override
    public T getSubnetMask() {
        return this.subnetMask;
    }

    /**
     * sets the subnet mask for the DhcpServerConfig
     *
     * @param subnetMask
     *                   the subnet mask in the form of a {@link IPAddress}
     */
    public void setSubnetMask(T subnetMask) {
        this.subnetMask = subnetMask;
    }

    @Override
    public int getDefaultLeaseTime() {
        return this.defaultLeaseTime;
    }

    /**
     * sets the default lease time for DHCP clients
     *
     * @param defaultLeaseTime
     *                         the default lease time
     */
    public void setDefaultLeaseTime(int defaultLeaseTime) {
        this.defaultLeaseTime = defaultLeaseTime;
    }

    @Override
    public int getMaximumLeaseTime() {
        return this.maximumLeaseTime;
    }

    /**
     * sets the maximum lease time for DHCP clients
     *
     * @param maximumLeaseTime
     *                         the maximum lease time
     */
    public void setMaximumLeaseTime(int maximumLeaseTime) {
        this.maximumLeaseTime = maximumLeaseTime;
    }

    @Override
    public short getPrefix() {
        return this.prefix;
    }

    /**
     * sets the network prefix for the DhcpServerConfig
     *
     * @param prefix
     *               the prefix
     */
    public void setPrefix(short prefix) {
        this.prefix = prefix;
    }

    @Override
    public T getRangeStart() {
        return this.rangeStart;
    }

    /**
     * sets the starting IPAddress in the pool for the DHCP clients
     *
     * @param m_rangeStart
     *                     the starting IPAddress
     */
    public void setRangeStart(T rangeStart) {
        this.rangeStart = rangeStart;
    }

    @Override
    public T getRangeEnd() {
        return this.rangeEnd;
    }

    /**
     * sets the ending IPAddress in the pool for the DHCP clients
     *
     * @param rangeEnd
     *                 the ending IPAddress
     */
    public void setRangeEnd(T rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    @Override
    public boolean isPassDns() {
        return this.passDns;
    }

    /**
     * whether or not to pass DNS to DHCP clients
     *
     * @param passDns
     *                true to pass, false to not
     */
    public void setPassDns(boolean passDns) {
        this.passDns = passDns;
    }

    @Override
    public List<T> getDnsServers() {
        return this.dnsServers;
    }

    /**
     * the DNS servers to pass to DHCP clients if passDns is set to true
     *
     * @param m_dnsServers
     *                     the DNS servers to pass
     */
    public void setDnsServers(List<T> dnsServers) {
        this.dnsServers = dnsServers;
    }

    @Override
    public boolean isValid() {
        if (this.interfaceName == null || !isValidSubnet()) {
            return false;
        }
        if (!isValidPoolRange() || !isValidLeaseTime() || this.prefix <= 0) {
            return false;
        }
        return true;
    }

    private boolean isValidSubnet() {
        return this.subnet != null && this.subnetMask != null ? true : false;
    }

    private boolean isValidPoolRange() {
        return this.rangeStart != null && this.rangeEnd != null ? true : false;
    }

    private boolean isValidLeaseTime() {
        return this.defaultLeaseTime > 0 && this.maximumLeaseTime > 0 ? true : false;
    }

    /**
     * Returns a string representation of the Dhcp Server Configuration.
     * DO NOT use it to write the service configuration file in the filesystem.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("# enabled? ").append(this.enabled).append("\n");
        sb.append("# prefix: ").append(this.prefix).append("\n");
        sb.append("# pass DNS? ").append(this.passDns).append("\n\n");

        sb.append("subnet " + this.subnet.getHostAddress() + " netmask " + this.subnetMask.getHostAddress() + " {\n");

        // DNS servers
        if (this.passDns && this.dnsServers != null && !this.dnsServers.isEmpty()) {
            sb.append("    option domain-name-servers ");
            for (int i = 0; i < this.dnsServers.size(); i++) {
                if (this.dnsServers.get(i) != null) {
                    sb.append(this.dnsServers.get(i).getHostAddress());
                }

                if (i + 1 == this.dnsServers.size()) {
                    sb.append(";\n\n");
                } else {
                    sb.append(",");
                }
            }
        }
        // interface
        if (this.interfaceName != null) {
            sb.append("    interface " + this.interfaceName + ";\n");
        }
        // router address
        if (this.routerAddress != null) {
            sb.append("    option routers " + this.routerAddress.getHostAddress() + ";\n");
        }
        // if DNS should not be forwarded, add the following lines
        if (!this.passDns) {
            sb.append("    ddns-update-style none;\n");
            sb.append("    ddns-updates off;\n");
        }
        // Lease times
        sb.append("    default-lease-time " + this.defaultLeaseTime + ";\n");
        if (this.maximumLeaseTime > -1) {
            sb.append("    max-lease-time " + this.maximumLeaseTime + ";\n");
        }

        // Add the pool and range
        sb.append("    pool {\n");
        sb.append("        range " + this.rangeStart.getHostAddress() + " " + this.rangeEnd.getHostAddress() + ";\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 59;
        int result = super.hashCode();
        result = prime * result + (this.enabled ? 1 : 0);
        result = prime * result + (this.interfaceName == null ? 0 : this.interfaceName.hashCode());
        result = prime * result + (this.subnet == null ? 0 : this.subnet.hashCode());
        result = prime * result + (this.subnetMask == null ? 0 : this.subnetMask.hashCode());
        result = prime * result + (this.routerAddress == null ? 0 : this.routerAddress.hashCode());
        result = prime * result + (this.rangeStart == null ? 0 : this.rangeStart.hashCode());
        result = prime * result + (this.rangeEnd == null ? 0 : this.rangeEnd.hashCode());
        result = prime * result + (this.dnsServers == null ? 0 : this.dnsServers.hashCode());
        result = prime * result + this.defaultLeaseTime;
        result = prime * result + this.maximumLeaseTime;
        result = prime * result + this.prefix;
        result = prime * result + (this.passDns ? 1 : 0);
        return result;
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

        if (this.enabled != other.enabled) {
            return false;
        }

        if (this.interfaceName == null) {
            if (other.interfaceName != null) {
                return false;
            }
        } else if (!this.interfaceName.equals(other.interfaceName)) {
            return false;
        }

        if (this.subnet == null) {
            if (other.subnet != null) {
                return false;
            }
        } else if (!this.subnet.equals(other.subnet)) {
            return false;
        }

        if (this.routerAddress == null) {
            if (other.routerAddress != null) {
                return false;
            }
        } else if (!this.routerAddress.equals(other.routerAddress)) {
            return false;
        }

        if (this.subnetMask == null) {
            if (other.subnetMask != null) {
                return false;
            }
        } else if (!this.subnetMask.equals(other.subnetMask)) {
            return false;
        }

        if (this.defaultLeaseTime != other.defaultLeaseTime) {
            return false;
        }

        if (this.maximumLeaseTime != other.maximumLeaseTime) {
            return false;
        }

        if (this.prefix != other.prefix) {
            return false;
        }

        if (this.rangeStart == null) {
            if (other.rangeStart != null) {
                return false;
            }
        } else if (!this.rangeStart.equals(other.rangeStart)) {
            return false;
        }

        if (this.rangeEnd == null) {
            if (other.rangeEnd != null) {
                return false;
            }
        } else if (!this.rangeEnd.equals(other.rangeEnd)) {
            return false;
        }

        if (this.passDns != other.passDns) {
            return false;
        }

        if (this.dnsServers == null) {
            if (other.dnsServers != null) {
                return false;
            }
        } else if (!this.dnsServers.equals(other.dnsServers)) {
            return false;
        }

        return true;
    }
}
