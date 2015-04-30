/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.net.admin.monitor;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.ConnectionInfoImpl;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterfaceType;

public class InterfaceState {
	
	private String m_name;
    private boolean m_up;
    protected boolean m_link;
    private IPAddress m_ipAddress;

    public InterfaceState(String interfaceName, boolean up, boolean link, IPAddress ipAddress) {
    	m_name = interfaceName;
        m_up = up;
        m_link = link;
        m_ipAddress = ipAddress;
    }
    
    public InterfaceState(NetInterfaceType type, String interfaceName) throws KuraException {
        m_name = interfaceName;
        m_up = LinuxNetworkUtil.isUp(interfaceName);
        m_link = LinuxNetworkUtil.isLinkUp(type, interfaceName);
        
        ConnectionInfo connInfo = new ConnectionInfoImpl(interfaceName);
        m_ipAddress = connInfo.getIpAddress();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_name != null) ? m_name.hashCode() : 0);
        result = prime * result + (m_link ? 1231 : 1237);
        result = prime * result + (m_up ? 1231 : 1237);
        result = prime * result + ((m_ipAddress != null) ? m_ipAddress.hashCode() : 0);
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InterfaceState other = (InterfaceState) obj;
        if (m_name != null) {
        	if(!m_name.equals(other.m_name)) {
        		return false;
        	}
        } else if (other.m_name != null) {
        	return false;
        }
        if (m_link != other.m_link)
            return false;
        if (m_up != other.m_up)
            return false;
        if (m_ipAddress != null) {
        	if(!m_ipAddress.equals(other.m_ipAddress)) {
        		return false;
        	}
        } else if (other.m_ipAddress != null) {
        	return false;
        }
        
        return true;
    }

	public String getName() {
		return m_name;
	}

	public boolean isUp() {
		return m_up;
	}

	public boolean isLinkUp() {
		return m_link;
	}
	
	public IPAddress getIpAddress() {
		return m_ipAddress;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(m_name);
		sb.append(" -- Link Up?: ");
		sb.append(m_link);
		sb.append(", Is Up?: ");
		sb.append(m_up);
		sb.append(", IP Address: ");
		sb.append(m_ipAddress);
		return sb.toString();
	}
}
