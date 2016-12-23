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
package org.eclipse.kura.net.admin.monitor;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.ConnectionInfoImpl;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterfaceType;

public class InterfaceState {

    private final String m_name;
    private final boolean m_up;
    protected boolean m_link;
    private final IPAddress m_ipAddress;

    public InterfaceState(String interfaceName, boolean up, boolean link, IPAddress ipAddress) {
        this.m_name = interfaceName;
        this.m_up = up;
        this.m_link = link;
        this.m_ipAddress = ipAddress;
    }

    public InterfaceState(NetInterfaceType type, String interfaceName) throws KuraException {
        this.m_name = interfaceName;
        this.m_up = LinuxNetworkUtil.hasAddress(interfaceName);
        this.m_link = LinuxNetworkUtil.isLinkUp(type, interfaceName);

        ConnectionInfo connInfo = new ConnectionInfoImpl(interfaceName);
        this.m_ipAddress = connInfo.getIpAddress();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.m_name != null ? this.m_name.hashCode() : 0);
        result = prime * result + (this.m_link ? 1231 : 1237);
        result = prime * result + (this.m_up ? 1231 : 1237);
        result = prime * result + (this.m_ipAddress != null ? this.m_ipAddress.hashCode() : 0);
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
        InterfaceState other = (InterfaceState) obj;
        if (this.m_name != null) {
            if (!this.m_name.equals(other.m_name)) {
                return false;
            }
        } else if (other.m_name != null) {
            return false;
        }
        if (this.m_link != other.m_link) {
            return false;
        }
        if (this.m_up != other.m_up) {
            return false;
        }
        if (this.m_ipAddress != null) {
            if (!this.m_ipAddress.equals(other.m_ipAddress)) {
                return false;
            }
        } else if (other.m_ipAddress != null) {
            return false;
        }

        return true;
    }

    public String getName() {
        return this.m_name;
    }

    public boolean isUp() {
        return this.m_up;
    }

    public boolean isLinkUp() {
        return this.m_link;
    }

    public IPAddress getIpAddress() {
        return this.m_ipAddress;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.m_name);
        sb.append(" -- Link Up?: ");
        sb.append(this.m_link);
        sb.append(", Is Up?: ");
        sb.append(this.m_up);
        sb.append(", IP Address: ");
        sb.append(this.m_ipAddress);
        return sb.toString();
    }
}
