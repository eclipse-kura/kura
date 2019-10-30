/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
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

import org.eclipse.kura.net.IPAddress;

public class InterfaceState {

    private final String name;
    private final boolean up;
    protected boolean link;
    private final IPAddress ipAddress;

    /**
     *
     * @param interfaceName
     *            interface name as {@link String}
     * @param up
     *            if true the interface is up
     * @param link
     *            if true the interface has link
     * @param ipAddress
     *            the {@link IPAddress} assigned to the interface
     */
    public InterfaceState(String interfaceName, boolean up, boolean link, IPAddress ipAddress) {
        this.name = interfaceName;
        this.up = up;
        this.link = link;
        this.ipAddress = ipAddress;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.ipAddress == null ? 0 : this.ipAddress.hashCode());
        result = prime * result + (this.link ? 1231 : 1237);
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (this.up ? 1231 : 1237);
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
        if (this.ipAddress == null) {
            if (other.ipAddress != null) {
                return false;
            }
        } else if (!this.ipAddress.equals(other.ipAddress)) {
            return false;
        }
        if (this.link != other.link) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.up != other.up) {
            return false;
        }
        return true;
    }
}
