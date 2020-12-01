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

import org.osgi.annotation.versioning.ProviderType;

/**
 * Model class for a 'network' that is specified by an IP and a mask. For example in the network
 * represented by 192.168.1.0/24 the IpAddress would be 192.168.1.0 and the mask is 24 bits or
 * 255.255.255.0. NetworkPairs are used in various components such as DHCP server configurations
 * where a network must be specified to provide addresses on.
 *
 * @param <T>
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class NetworkPair<T extends IPAddress> {

    /** The IP Address portion of the NetworkPair **/
    @SuppressWarnings({"checkstyle:memberName", "checkstyle:visibilityModifier"})
    public T m_ipAddress;

    /** The prefix portion of the NetworkPair **/
    @SuppressWarnings({"checkstyle:memberName", "checkstyle:visibilityModifier"})
    public short m_prefix;

    public NetworkPair(T ipAddress, short prefix) {
        this.m_ipAddress = ipAddress;
        this.m_prefix = prefix;
    }

    public T getIpAddress() {
        return this.m_ipAddress;
    }

    public void setIpAddress(T ipAddress) {
        this.m_ipAddress = ipAddress;
    }

    public short getPrefix() {
        return this.m_prefix;
    }

    public void setPrefix(short prefix) {
        this.m_prefix = prefix;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.m_ipAddress.getHostAddress()).append("/").append(this.m_prefix);

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NetworkPair<?>)) {
            return false;
        }

        NetworkPair<?> other = (NetworkPair<?>) o;

        if (!this.m_ipAddress.equals(other.m_ipAddress)) {
            return false;
        } else if (this.m_prefix != other.m_prefix) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 67;
        int result = 1;
        result = prime * result + this.m_prefix;
        result = prime * result + (this.m_ipAddress == null ? 0 : this.m_ipAddress.hashCode());

        return result;
    }
}
