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
package org.eclipse.kura.net.admin.monitor;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.ConnectionInfoImpl;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterfaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceState {

    private static final Logger logger = LoggerFactory.getLogger(InterfaceState.class);
    private final String name;
    private final boolean up;
    protected boolean link;
    private final IPAddress ipAddress;

    public InterfaceState(String interfaceName, boolean up, boolean link, IPAddress ipAddress) {
        this.name = interfaceName;
        this.up = up;
        this.link = link;
        this.ipAddress = ipAddress;
    }

    public InterfaceState(NetInterfaceType type, String interfaceName) throws KuraException {
        this.name = interfaceName;
        this.up = LinuxNetworkUtil.hasAddress(interfaceName);
        this.link = LinuxNetworkUtil.isLinkUp(type, interfaceName);
        logger.debug("InterfaceState() :: {} - link?={}", interfaceName, this.link);
        logger.debug("InterfaceState() :: {} - up?={}", interfaceName, this.up);
        ConnectionInfo connInfo = new ConnectionInfoImpl(interfaceName);
        this.ipAddress = connInfo.getIpAddress();
    }

    public InterfaceState(NetInterfaceType type, String interfaceName, boolean isL2OnlyInterface) throws KuraException {
        this.name = interfaceName;
        this.up = isL2OnlyInterface ? LinuxNetworkUtil.isUp(interfaceName) : LinuxNetworkUtil.hasAddress(interfaceName);
        this.link = LinuxNetworkUtil.isLinkUp(type, interfaceName);
        logger.debug("InterfaceState() :: {} - link?={}", interfaceName, this.link);
        logger.debug("InterfaceState() :: {} - up?={}", interfaceName, this.up);
        ConnectionInfo connInfo = new ConnectionInfoImpl(interfaceName);
        this.ipAddress = connInfo.getIpAddress();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.name != null ? this.name.hashCode() : 0);
        result = prime * result + (this.link ? 1231 : 1237);
        result = prime * result + (this.up ? 1231 : 1237);
        result = prime * result + (this.ipAddress != null ? this.ipAddress.hashCode() : 0);
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
        if (this.name != null) {
            if (!this.name.equals(other.name)) {
                return false;
            }
        } else if (other.name != null) {
            return false;
        }
        if (this.link != other.link) {
            return false;
        }
        if (this.up != other.up) {
            return false;
        }
        if (this.ipAddress != null) {
            if (!this.ipAddress.equals(other.ipAddress)) {
                return false;
            }
        } else if (other.ipAddress != null) {
            return false;
        }

        return true;
    }

    public String getName() {
        return this.name;
    }

    public boolean isUp() {
        return this.up;
    }

    public boolean isLinkUp() {
        return this.link;
    }

    public IPAddress getIpAddress() {
        return this.ipAddress;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.name);
        sb.append(" -- Link Up?: ");
        sb.append(this.link);
        sb.append(", Is Up?: ");
        sb.append(this.up);
        sb.append(", IP Address: ");
        sb.append(this.ipAddress);
        return sb.toString();
    }
}
