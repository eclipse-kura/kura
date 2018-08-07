/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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
import org.osgi.annotation.versioning.ProviderType;

/**
 * Base class for configuration of network interfaces.
 * The two subclasses NetConfigIP4 and NetConfigIP6 represent
 * configurations of IPv4 and IPv6 addresses respectively.
 *
 * @param <T>
 *            IPv4 or IPv6 address
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public abstract class NetConfigIP<T extends IPAddress> implements NetConfig {

    private NetInterfaceStatus status;
    private boolean autoConnect;
    private boolean dhcp;
    private T address;
    private short networkPrefixLength;
    private T subnetMask;
    private T gateway;
    private List<T> dnsServers;
    private List<String> domains;
    private Map<String, Object> properties;

    NetConfigIP(NetInterfaceStatus status, boolean autoConnect) {
        this.status = status;
        this.autoConnect = autoConnect;
        this.dhcp = false;
        this.address = null;
        this.networkPrefixLength = -1;
        this.subnetMask = null;
        this.gateway = null;
        this.dnsServers = new ArrayList<>();
        this.domains = new ArrayList<>();
        this.properties = new HashMap<>();
    }

    NetConfigIP(NetInterfaceStatus status, boolean autoConnect, boolean dhcp) {
        this.status = status;
        this.autoConnect = autoConnect;
        this.dhcp = dhcp;
        this.address = null;
        this.networkPrefixLength = -1;
        this.subnetMask = null;
        this.gateway = null;
        this.dnsServers = new ArrayList<>();
        this.domains = new ArrayList<>();
        this.properties = new HashMap<>();
    }

    NetConfigIP(NetInterfaceStatus status, boolean autoConnect, T address, short networkPrefixLength, T gateway)
            throws KuraException {
        this.status = status;
        this.autoConnect = autoConnect;
        this.dhcp = false;
        this.address = address;
        this.networkPrefixLength = networkPrefixLength;
        this.subnetMask = calculateNetmaskFromNetworkPrefix(networkPrefixLength);
        this.gateway = gateway;
        this.dnsServers = new ArrayList<>();
        this.domains = new ArrayList<>();
        this.properties = new HashMap<>();
    }

    NetConfigIP(NetInterfaceStatus status, boolean autoConnect, T address, T subnetMask, T gateway)
            throws KuraException {
        this.status = status;
        this.autoConnect = autoConnect;
        this.dhcp = false;
        this.address = address;
        this.networkPrefixLength = calculateNetworkPrefixFromNetmask(subnetMask.getHostAddress());
        this.subnetMask = subnetMask;
        this.gateway = gateway;
        this.dnsServers = new ArrayList<>();
        this.domains = new ArrayList<>();
        this.properties = new HashMap<>();
    }

    /**
     * Return the NetInterfaceStatus of this configuration
     *
     * @return
     */
    public NetInterfaceStatus getStatus() {
        return this.status;
    }

    /**
     * Sets the NetInterfaceStatus to be used for the network interface
     *
     * @param status
     */
    public void setStatus(NetInterfaceStatus status) {
        this.status = status;
    }

    public boolean isAutoConnect() {
        return this.autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public boolean isDhcp() {
        return this.dhcp;
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
        this.dhcp = dhcp;
    }

    /**
     * Returns the address that should be statically assigned to the interface.
     * The returned address is IP4Address or IP6Address depending on
     * the NetConfigIP instance used. This is only used if dhcp is set to false.
     *
     * @return the static address for the interface
     */
    public T getAddress() {
        return this.address;
    }

    /**
     * Sets the static address to be assigned to the interface.
     * The address should IP4Address or IP6Address depending on
     * the NetConfigIP instance used. This is only used if dhcp is set to false.
     *
     * @param address
     *            - address to be statically assigned to the interface
     */
    public void setAddress(T address) {
        this.address = address;
    }

    /**
     * Return the prefix to be used for the network interface
     *
     * @return
     */
    public short getNetworkPrefixLength() {
        return this.networkPrefixLength;
    }

    /**
     * Sets the prefix length to be used for the network interface
     *
     * @param networkPrefixLength
     * @throws KuraException
     */
    public void setNetworkPrefixLength(short networkPrefixLength) throws KuraException {
        this.networkPrefixLength = networkPrefixLength;
        this.subnetMask = calculateNetmaskFromNetworkPrefix(networkPrefixLength);
    }

    /**
     * Return the prefix to be used for the network interface
     *
     * @return
     */
    public T getSubnetMask() {
        return this.subnetMask;
    }

    /**
     * Sets the subnet mask to be used for the network interface
     *
     * @param subnetMask
     * @throws KuraException
     */
    public void setSubnetMask(T subnetMask) throws KuraException {
        this.networkPrefixLength = calculateNetworkPrefixFromNetmask(subnetMask.getHostAddress());
        this.subnetMask = subnetMask;
    }

    /**
     * Returns the address of the gateway to be used for the interface
     *
     * @return
     */
    public T getGateway() {
        return this.gateway;
    }

    /**
     * Sets the gateway to be used for the interface
     *
     * @param gateway
     */
    public void setGateway(T gateway) {
        this.gateway = gateway;
    }

    /**
     * Returns the list of Name Servers to be associated to the interface.
     * The returned addresses are IP4Address or IP6Address depending on
     * the NetConfigIP instance used. This is only used if dhcp is set to false.
     *
     * @return list of address for the DNS Servers
     */
    public List<T> getDnsServers() {
        if (this.dnsServers != null) {
            return Collections.unmodifiableList(this.dnsServers);
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
        this.dnsServers = dnsServers;
    }

    /**
     * Returns the list of DNS domains to be associated to the interface.
     * This is only used if dhcp is set to false.
     *
     * @return - list of DNS domains
     */
    public List<String> getDomains() {
        if (this.domains != null) {
            return Collections.unmodifiableList(this.domains);
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
        this.domains = domains;
    }

    public Map<String, Object> getProperties() {
        if (this.properties != null) {
            return Collections.unmodifiableMap(this.properties);
        } else {
            return null;
        }
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.address == null ? 0 : this.address.hashCode());
        result = prime * result + (this.autoConnect ? 1231 : 1237);
        result = prime * result + (this.dhcp ? 1231 : 1237);
        result = prime * result + (this.dnsServers == null ? 0 : this.dnsServers.hashCode());
        result = prime * result + (this.domains == null ? 0 : this.domains.hashCode());
        result = prime * result + (this.gateway == null ? 0 : this.gateway.hashCode());
        result = prime * result + this.networkPrefixLength;
        result = prime * result + (this.properties == null ? 0 : this.properties.hashCode());
        result = prime * result + (this.status == null ? 0 : this.status.hashCode());
        result = prime * result + (this.subnetMask == null ? 0 : this.subnetMask.hashCode());
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
        if (this.address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!this.address.equals(other.address)) {
            return false;
        }
        if (this.autoConnect != other.autoConnect) {
            return false;
        }
        if (this.dhcp != other.dhcp) {
            return false;
        }
        if (this.dnsServers == null) {
            if (other.dnsServers != null) {
                return false;
            }
        } else if (!this.dnsServers.equals(other.dnsServers)) {
            return false;
        }
        if (this.domains == null) {
            if (other.domains != null) {
                return false;
            }
        } else if (!this.domains.equals(other.domains)) {
            return false;
        }
        if (this.gateway == null) {
            if (other.gateway != null) {
                return false;
            }
        } else if (!this.gateway.equals(other.gateway)) {
            return false;
        }
        if (this.networkPrefixLength != other.networkPrefixLength) {
            return false;
        }
        if (this.properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!this.properties.equals(other.properties)) {
            return false;
        }
        if (this.status != other.status) {
            return false;
        }
        if (this.subnetMask == null) {
            if (other.subnetMask != null) {
                return false;
            }
        } else if (!this.subnetMask.equals(other.subnetMask)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isValid() {
        // FIXME
        if (this.dhcp) {
            return true;
        } else {
            try {
                this.address.getHostAddress();
            } catch (Exception e) {
                return false;
            }

            for (IPAddress dns : this.dnsServers) {
                try {
                    dns.getHostAddress();
                } catch (Exception e) {
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
        builder.append("NetConfigIP [status=");
        builder.append(this.status);
        builder.append(", autoConnect=");
        builder.append(this.autoConnect);
        builder.append(", dhcp=");
        builder.append(this.dhcp);
        builder.append(", address=");
        builder.append(this.address);
        builder.append(", networkPrefixLength=");
        builder.append(this.networkPrefixLength);
        builder.append(", subnetMask=");
        builder.append(this.subnetMask);
        builder.append(", gateway=");
        builder.append(this.gateway);
        builder.append(", dnsServers=");
        builder.append(this.dnsServers);
        builder.append(", domains=");
        builder.append(this.domains);
        builder.append(", properties=");
        builder.append(this.properties);
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
        StringBuilder sb = new StringBuilder(15);
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
