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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

/**
 * Base class for configuration of network interfaces.
 * The two subclasses NetConfigIP4 and NetConfigIP6 represent
 * configurations of IPv4 and IPv6 addresses respectively.
 *
 * @param <T>
 *            IPv4 or IPv6 address
 */
public abstract class NetConfigIP<T extends IPAddress> implements NetConfig {

    private NetInterfaceStatus m_status;
    private boolean m_autoConnect;
    private boolean m_dhcp;
    private T m_address;
    private short m_networkPrefixLength;
    private T m_subnetMask;
    private T m_gateway;
    private List<T> m_dnsServers;
    private List<String> m_domains;
    private Map<String, Object> m_properties;

    NetConfigIP(NetInterfaceStatus status, boolean autoConnect) {
        this.m_status = status;
        this.m_autoConnect = autoConnect;
        this.m_dhcp = false;
        this.m_address = null;
        this.m_networkPrefixLength = -1;
        this.m_subnetMask = null;
        this.m_gateway = null;
        this.m_dnsServers = new ArrayList<T>();
        this.m_domains = new ArrayList<String>();
        this.m_properties = new HashMap<String, Object>();
    }

    NetConfigIP(NetInterfaceStatus status, boolean autoConnect, boolean dhcp) {
        this.m_status = status;
        this.m_autoConnect = autoConnect;
        this.m_dhcp = dhcp;
        this.m_address = null;
        this.m_networkPrefixLength = -1;
        this.m_subnetMask = null;
        this.m_gateway = null;
        this.m_dnsServers = new ArrayList<T>();
        this.m_domains = new ArrayList<String>();
        this.m_properties = new HashMap<String, Object>();
    }

    NetConfigIP(NetInterfaceStatus status, boolean autoConnect, T address, short networkPrefixLength, T gateway)
            throws KuraException {
        this.m_status = status;
        this.m_autoConnect = autoConnect;
        this.m_dhcp = false;
        this.m_address = address;
        this.m_networkPrefixLength = networkPrefixLength;
        this.m_subnetMask = calculateNetmaskFromNetworkPrefix(networkPrefixLength);
        this.m_gateway = gateway;
        this.m_dnsServers = new ArrayList<T>();
        this.m_domains = new ArrayList<String>();
        this.m_properties = new HashMap<String, Object>();
    }

    NetConfigIP(NetInterfaceStatus status, boolean autoConnect, T address, T subnetMask, T gateway)
            throws KuraException {
        this.m_status = status;
        this.m_autoConnect = autoConnect;
        this.m_dhcp = false;
        this.m_address = address;
        this.m_networkPrefixLength = calculateNetworkPrefixFromNetmask(subnetMask.getHostAddress());
        this.m_subnetMask = subnetMask;
        this.m_gateway = gateway;
        this.m_dnsServers = new ArrayList<T>();
        this.m_domains = new ArrayList<String>();
        this.m_properties = new HashMap<String, Object>();
    }

    /**
     * Return the NetInterfaceStatus of this configuration
     *
     * @return
     */
    public NetInterfaceStatus getStatus() {
        return this.m_status;
    }

    /**
     * Sets the NetInterfaceStatus to be used for the network interface
     *
     * @param status
     */
    public void setStatus(NetInterfaceStatus status) {
        this.m_status = status;
    }

    public boolean isAutoConnect() {
        return this.m_autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.m_autoConnect = autoConnect;
    }

    public boolean isDhcp() {
        return this.m_dhcp;
    }

    /**
     * Sets whether of not this configuration should be a dhcp client. If dhcp
     * is set to true it overrides and static configuration that is present in
     * the configuration.
     *
     * @param dhcp
     *            whether or not dhcp client mode should be used
     */
    public void setDhcp(boolean dhcp) {
        this.m_dhcp = dhcp;
    }

    /**
     * Returns the address that should be statically assigned to the interface.
     * The returned address is IP4Address or IP6Address depending on
     * the NetConfigIP instance used. This is only used if dhcp is set to false.
     *
     * @return the static address for the interface
     */
    public T getAddress() {
        return this.m_address;
    }

    /**
     * Sets the static address to be assigned to the interface.
     * The address should IP4Address or IP6Address depending on
     * the NetConfigIP instance used. This is only used if dhcp is set to false.
     *
     * @param address
     *            - address to be statically assigned to the interface
     */
    public void setAddress(T address) throws KuraException {
        this.m_address = address;
    }

    /**
     * Return the prefix to be used for the network interface
     *
     * @return
     */
    public short getNetworkPrefixLength() {
        return this.m_networkPrefixLength;
    }

    /**
     * Sets the prefix length to be used for the network interface
     *
     * @param networkPrefixLength
     * @throws KuraException
     */
    public void setNetworkPrefixLength(short networkPrefixLength) throws KuraException {
        this.m_networkPrefixLength = networkPrefixLength;
        this.m_subnetMask = calculateNetmaskFromNetworkPrefix(networkPrefixLength);
    }

    /**
     * Return the prefix to be used for the network interface
     *
     * @return
     */
    public T getSubnetMask() {
        return this.m_subnetMask;
    }

    /**
     * Sets the subnet mask to be used for the network interface
     *
     * @param subnetMask
     * @throws KuraException
     */
    public void setSubnetMask(T subnetMask) throws KuraException {
        this.m_networkPrefixLength = calculateNetworkPrefixFromNetmask(subnetMask.getHostAddress());
        this.m_subnetMask = subnetMask;
    }

    /**
     * Returns the address of the gateway to be used for the interface
     *
     * @return
     */
    public T getGateway() {
        return this.m_gateway;
    }

    /**
     * Sets the gateway to be used for the interface
     *
     * @param gateway
     */
    public void setGateway(T gateway) {
        this.m_gateway = gateway;
    }

    /**
     * Returns the list of Name Servers to be associated to the interface.
     * The returned addresses are IP4Address or IP6Address depending on
     * the NetConfigIP instance used. This is only used if dhcp is set to false.
     *
     * @return list of address for the DNS Servers
     */
    public List<T> getDnsServers() {
        if (this.m_dnsServers != null) {
            return Collections.unmodifiableList(this.m_dnsServers);
        } else {
            return null;
        }
    }

    /**
     * Sets the list of Name Servers to be associated to the interface.
     * The addresses are IP4Address or IP6Address depending on
     * the NetConfigIP instance used. This is only used if dhcp is set to false.
     */
    public void setDnsServers(List<T> dnsServers) {
        this.m_dnsServers = dnsServers;
    }

    /**
     * Returns the list of DNS domains to be associated to the interface.
     * This is only used if dhcp is set to false.
     *
     * @return - list of DNS domains
     */
    public List<String> getDomains() {
        if (this.m_domains != null) {
            return Collections.unmodifiableList(this.m_domains);
        } else {
            return null;
        }
    }

    /**
     * Sets the list of DNS domains to be associated to the interface.
     * This is only used if dhcp is set to false.
     *
     * @param domains
     */
    public void setDomains(List<String> domains) {
        this.m_domains = domains;
    }

    public Map<String, Object> getProperties() {
        if (this.m_properties != null) {
            return Collections.unmodifiableMap(this.m_properties);
        } else {
            return null;
        }
    }

    public void setProperties(Map<String, Object> properties) {
        this.m_properties = properties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.m_address == null ? 0 : this.m_address.hashCode());
        result = prime * result + (this.m_autoConnect ? 1231 : 1237);
        result = prime * result + (this.m_dhcp ? 1231 : 1237);
        result = prime * result + (this.m_dnsServers == null ? 0 : this.m_dnsServers.hashCode());
        result = prime * result + (this.m_domains == null ? 0 : this.m_domains.hashCode());
        result = prime * result + (this.m_gateway == null ? 0 : this.m_gateway.hashCode());
        result = prime * result + this.m_networkPrefixLength;
        result = prime * result + (this.m_properties == null ? 0 : this.m_properties.hashCode());
        result = prime * result + (this.m_status == null ? 0 : this.m_status.hashCode());
        result = prime * result + (this.m_subnetMask == null ? 0 : this.m_subnetMask.hashCode());
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
        NetConfigIP other = (NetConfigIP) obj;
        if (this.m_address == null) {
            if (other.m_address != null) {
                return false;
            }
        } else if (!this.m_address.equals(other.m_address)) {
            return false;
        }
        if (this.m_autoConnect != other.m_autoConnect) {
            return false;
        }
        if (this.m_dhcp != other.m_dhcp) {
            return false;
        }
        if (this.m_dnsServers == null) {
            if (other.m_dnsServers != null) {
                return false;
            }
        } else if (!this.m_dnsServers.equals(other.m_dnsServers)) {
            return false;
        }
        if (this.m_domains == null) {
            if (other.m_domains != null) {
                return false;
            }
        } else if (!this.m_domains.equals(other.m_domains)) {
            return false;
        }
        if (this.m_gateway == null) {
            if (other.m_gateway != null) {
                return false;
            }
        } else if (!this.m_gateway.equals(other.m_gateway)) {
            return false;
        }
        if (this.m_networkPrefixLength != other.m_networkPrefixLength) {
            return false;
        }
        if (this.m_properties == null) {
            if (other.m_properties != null) {
                return false;
            }
        } else if (!this.m_properties.equals(other.m_properties)) {
            return false;
        }
        if (this.m_status != other.m_status) {
            return false;
        }
        if (this.m_subnetMask == null) {
            if (other.m_subnetMask != null) {
                return false;
            }
        } else if (!this.m_subnetMask.equals(other.m_subnetMask)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isValid() {
        // FIXME
        if (this.m_dhcp) {
            return true;
        } else {
            try {
                this.m_address.getHostAddress();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            for (IPAddress dns : this.m_dnsServers) {
                try {
                    dns.getHostAddress();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            // if we got here...
            return true;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NetConfigIP [m_status=");
        builder.append(this.m_status);
        builder.append(", m_autoConnect=");
        builder.append(this.m_autoConnect);
        builder.append(", m_dhcp=");
        builder.append(this.m_dhcp);
        builder.append(", m_address=");
        builder.append(this.m_address);
        builder.append(", m_networkPrefixLength=");
        builder.append(this.m_networkPrefixLength);
        builder.append(", m_subnetMask=");
        builder.append(this.m_subnetMask);
        builder.append(", m_gateway=");
        builder.append(this.m_gateway);
        builder.append(", m_dnsServers=");
        builder.append(this.m_dnsServers);
        builder.append(", m_domains=");
        builder.append(this.m_domains);
        builder.append(", m_properties=");
        builder.append(this.m_properties);
        builder.append("]");
        return builder.toString();
    }

    // TODO - only works on IPv4 now
    private short calculateNetworkPrefixFromNetmask(String netmask) throws KuraException {
        if (netmask == null) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "netmask is null");
        }

        int netmaskValue = 0;
        StringTokenizer st = new StringTokenizer(netmask, ".");
        for (int i = 24; i >= 0; i -= 8) {
            netmaskValue = netmaskValue | Integer.parseInt(st.nextToken()) << i;
        }

        boolean hitZero = false;
        int displayMask = 1 << 31;
        int count = 0;

        for (int c = 1; c <= 32; c++) {
            if ((netmaskValue & displayMask) == 0) {
                hitZero = true;
            } else {
                if (hitZero) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "received invalid mask: " + netmask);
                }

                count++;
            }

            netmaskValue <<= 1;
        }

        return (short) count;
    }

    // TODO - only works on IPv4 now
    private T calculateNetmaskFromNetworkPrefix(int networkPrefixLength) throws KuraException {
        int mask = ~((1 << 32 - networkPrefixLength) - 1);
        StringBuffer sb = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {
            // process 3 bytes, from high order byte down.
            sb.append(Integer.toString(mask >>> shift & 0xff));
            sb.append('.');
        }

        sb.append(Integer.toString(mask & 0xff));

        try {
            @SuppressWarnings("unchecked")
            T netmask = (T) IPAddress.parseHostAddress(sb.toString());
            return netmask;
        } catch (UnknownHostException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }
}
