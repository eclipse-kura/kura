/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Configuration for a network interface based on IPv4 addresses.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class NetConfigIP4 extends NetConfigIP<IP4Address> implements NetConfig4 {

    private List<IP4Address> winsServers;

    public NetConfigIP4(NetInterfaceStatus status, boolean autoConnect) {
        super(status, autoConnect);
        this.winsServers = new ArrayList<>();
    }

    public NetConfigIP4(NetInterfaceStatus status, boolean autoConnect, boolean dhcp) {
        super(status, autoConnect, dhcp);
        this.winsServers = new ArrayList<>();
    }

    public NetConfigIP4(NetInterfaceStatus status, boolean autoConnect, IP4Address address, short networkPrefixLength,
            IP4Address gateway) throws KuraException {
        super(status, autoConnect, address, networkPrefixLength, gateway);
        this.winsServers = new ArrayList<>();
    }

    public NetConfigIP4(NetInterfaceStatus status, boolean autoConnect, IP4Address address, IP4Address subnetMask,
            IP4Address gateway) throws KuraException {
        super(status, autoConnect, address, subnetMask, gateway);
        this.winsServers = new ArrayList<>();
    }

    /**
     * Returns the list of Windows Servers to be associated with the interface
     *
     * @return
     */
    public List<IP4Address> getWinsServers() {
        if (this.winsServers != null) {
            return Collections.unmodifiableList(this.winsServers);
        } else {
            return null;
        }
    }

    /**
     * Sets the list of Windows Servers to be associated with the interface
     *
     * @param winsServers
     */
    public void setWinsServers(List<IP4Address> winsServers) {
        this.winsServers = winsServers;
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NetConfigIP4 [winsServers=");
        builder.append(this.winsServers);
        builder.append(", super.toString()=");
        builder.append(super.toString());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.winsServers == null ? 0 : this.winsServers.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NetConfigIP4 other = (NetConfigIP4) obj;
        if (this.winsServers == null) {
            if (other.winsServers != null) {
                return false;
            }
        } else if (!this.winsServers.equals(other.winsServers)) {
            return false;
        }
        return true;
    }
}
