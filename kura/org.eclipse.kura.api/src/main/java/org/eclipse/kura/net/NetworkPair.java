/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net;

/**
 * Model class for a 'network' that is specified by an IP and a mask.  For example in the network
 * represented by 192.168.1.0/24 the IpAddress would be 192.168.1.0 and the mask is 24 bits or 
 * 255.255.255.0.  NetworkPairs are used in various components such as DHCP server configurations
 * where a network must be specified to provide addresses on.
 * 
 * @author eurotech
 *
 * @param <T>
 */
public class NetworkPair<T extends IPAddress> {

	/** The IP Address portion of the NetworkPair **/
	public T 		m_ipAddress; 
	
	/** The prefix portion of the NetworkPair **/
	public short	m_prefix;
	
	public NetworkPair(T ipAddress, short prefix) {
		m_ipAddress = ipAddress;
		m_prefix = prefix;
	}

	public T getIpAddress() {
		return m_ipAddress;
	}

	public void setIpAddress(T ipAddress) {
		m_ipAddress = ipAddress;
	}

	public short getPrefix() {
		return m_prefix;
	}

	public void setPrefix(short prefix) {
		m_prefix = prefix;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(m_ipAddress.getHostAddress())
		.append("/")
		.append(m_prefix);

		return sb.toString();
	}
	
	@Override
	public boolean equals(Object o) {
	    if(!(o instanceof NetworkPair<?>)) {
	        return false;
	    }
	    
	    NetworkPair<?> other = (NetworkPair<?>) o;
	    
	    if(!(this.m_ipAddress.equals(other.m_ipAddress))) {
	        return false;
	    } else if(this.m_prefix != other.m_prefix) {
	        return false;
	    }
	    
	    return true;
	}
	
    @Override
    public int hashCode() {
        final int prime = 67;
        int result = 1;
        result = prime * result + m_prefix;
        result = prime * result
                + ((m_ipAddress == null) ? 0 : m_ipAddress.hashCode());

        return result;
    }
}
