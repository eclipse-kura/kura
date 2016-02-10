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

public class NetInterfaceAddressImpl implements NetInterfaceAddress 
{
	private IPAddress                  m_address;
	private short                      m_networkPrefixLength;
	private IPAddress		           m_netmask;
    private IPAddress                  m_gateway;
	private IPAddress                  m_broadcast;
	private List<? extends IPAddress>  m_dnsAddresses;
	
	public NetInterfaceAddressImpl() {
	}
	
	public NetInterfaceAddressImpl(NetInterfaceAddress other) {
	    super();
	    this.m_address = other.getAddress();
        this.m_networkPrefixLength = other.getNetworkPrefixLength();
        this.m_netmask = other.getNetmask();
        this.m_gateway = other.getGateway();
	    this.m_broadcast = other.getBroadcast();
	    this.m_dnsAddresses = other.getDnsServers();
	}

	public IPAddress getAddress() {
		return m_address;
	}

	public void setAddress(IPAddress address) {
		m_address = address;
	}

	public short getNetworkPrefixLength() {
		return m_networkPrefixLength;
	}

	public void setNetworkPrefixLength(short networkPrefixLength) {
		m_networkPrefixLength = networkPrefixLength;
	}

	public IPAddress getNetmask() {
		return m_netmask;
	}

	public void setNetmask(IPAddress netmask) {
		m_netmask = netmask;
	}
	
    public IPAddress getGateway() {
        return m_gateway;
    }
    
    public void setGateway(IPAddress gateway) {
        m_gateway = gateway;
    }

	public IPAddress getBroadcast() {
		return m_broadcast;
	}

	public void setBroadcast(IPAddress broadcast) {
		m_broadcast = broadcast;
	}
	
    @Override
    public List<? extends IPAddress> getDnsServers() {
        return m_dnsAddresses;
    }
    
    public void setDnsServers(List<? extends IPAddress> dnsAddresses) {
        m_dnsAddresses = dnsAddresses;
    }
	
	@Override
	public boolean equals(Object obj) {
	    if(!(obj instanceof NetInterfaceAddress)) {
	        return false;
	    }
	    
	    NetInterfaceAddress other = (NetInterfaceAddress) obj;
        
        if(m_networkPrefixLength != other.getNetworkPrefixLength()) {
            return false;
        }
        if(!compare(m_address, other.getAddress())) {
	        return false;
	    }
        if(!compare(m_netmask, other.getNetmask())) {
            return false;
        }	    
        if(!compare(m_gateway, other.getGateway())) {
            return false;
        }
        if(!compare(m_broadcast, other.getBroadcast())) {
            return false;
        }
        if(!compare(m_dnsAddresses, other.getDnsServers())) {
            return false;
        }
	    
	    return true;
	}
	
	protected boolean compare(Object obj1, Object obj2) {
        return (obj1 == null) ? (obj2 == null) : (obj1.equals(obj2));
	}
}
