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
package org.eclipse.kura.core.net;

import java.util.List;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterfaceAddress;

public class NetInterfaceAddressImpl implements NetInterfaceAddress {

    private IPAddress address;
    private short networkPrefixLength;
    private IPAddress netmask;
    private IPAddress gateway;
    private IPAddress broadcast;
    private List<? extends IPAddress> dnsAddresses;

    public NetInterfaceAddressImpl() {
    }

    public NetInterfaceAddressImpl(NetInterfaceAddress other) {
        super();
        this.address = other.getAddress();
        this.networkPrefixLength = other.getNetworkPrefixLength();
        this.netmask = other.getNetmask();
        this.gateway = other.getGateway();
        this.broadcast = other.getBroadcast();
        this.dnsAddresses = other.getDnsServers();
    }

    @Override
    public IPAddress getAddress() {
        return this.address;
    }

    public void setAddress(IPAddress address) {
        this.address = address;
    }

    @Override
    public short getNetworkPrefixLength() {
        return this.networkPrefixLength;
    }

    public void setNetworkPrefixLength(short networkPrefixLength) {
        this.networkPrefixLength = networkPrefixLength;
    }

    @Override
    public IPAddress getNetmask() {
        return this.netmask;
    }

    public void setNetmask(IPAddress netmask) {
        this.netmask = netmask;
    }

    @Override
    public IPAddress getGateway() {
        return this.gateway;
    }

    public void setGateway(IPAddress gateway) {
        this.gateway = gateway;
    }

    @Override
    public IPAddress getBroadcast() {
        return this.broadcast;
    }

    public void setBroadcast(IPAddress broadcast) {
        this.broadcast = broadcast;
    }

    @Override
    public List<? extends IPAddress> getDnsServers() {
        return this.dnsAddresses;
    }

    public void setDnsServers(List<? extends IPAddress> dnsAddresses) {
        this.dnsAddresses = dnsAddresses;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NetInterfaceAddress)) {
            return false;
        }

        NetInterfaceAddress other = (NetInterfaceAddress) obj;

        return this.networkPrefixLength == other.getNetworkPrefixLength() && compare(this.address, other.getAddress())
                && compare(this.netmask, other.getNetmask()) && compare(this.gateway, other.getGateway())
                && compare(this.broadcast, other.getBroadcast()) && compare(this.dnsAddresses, other.getDnsServers());
    }

    protected boolean compare(Object obj1, Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.address == null ? 0 : this.address.hashCode());
        result = prime * result + (this.broadcast == null ? 0 : this.broadcast.hashCode());
        result = prime * result + (this.dnsAddresses == null ? 0 : this.dnsAddresses.hashCode());
        result = prime * result + (this.gateway == null ? 0 : this.gateway.hashCode());
        result = prime * result + (this.netmask == null ? 0 : this.netmask.hashCode());
        result = prime * result + this.networkPrefixLength;
        return result;
    }

}
