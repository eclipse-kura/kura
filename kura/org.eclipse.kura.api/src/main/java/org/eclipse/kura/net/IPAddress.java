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
package org.eclipse.kura.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represents an Internet Protocol (IP) address.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public abstract class IPAddress {

    private static final int BIT_MASK = 0xff;
    private static final int BYTE_SIZE = 8;
    private static final int BYTE_3 = 24;
    private final byte[] address;
    protected java.net.InetAddress javaNetAddress;

    IPAddress(byte[] address, java.net.InetAddress jnAddress) {
        this.address = address;
        this.javaNetAddress = jnAddress;
    }

    /**
     * Returns an InetAddress object given the raw IP address.
     * The argument is in network byte order: the highest order byte of the address is in getAddress()[0].
     * This method doesn't block, i.e. no reverse name service lookup is performed.
     * <br>
     * IPv4 address byte array must be 4 bytes long and IPv6 byte array must be 16 bytes long
     *
     * @param addr
     *            - the raw IP address in network byte order
     * @return an InetAddress object created from the raw IP address.
     */
    public static IPAddress getByAddress(byte[] addr) throws UnknownHostException {
        IPAddress result = null;
        java.net.InetAddress jnetAddr = java.net.InetAddress.getByAddress(addr);
        if (jnetAddr instanceof java.net.Inet4Address) {
            result = new IP4Address(addr, jnetAddr);
        } else if (jnetAddr instanceof java.net.Inet6Address) {
            result = new IP6Address(addr, jnetAddr);
        }
        return result;
    }

    // TODO - only works on IPv4 now
    public static IPAddress getByAddress(int addr) throws UnknownHostException {

        StringBuffer sb = new StringBuffer();
        for (int shift = BYTE_3; shift > 0; shift -= BYTE_SIZE) {
            // process 3 bytes, from high order byte down
            sb.append(Integer.toString(addr >>> shift & BIT_MASK));
            sb.append('.');
        }
        sb.append(Integer.toString(addr & BIT_MASK));

        InetAddress jnetAddr = InetAddress.getByName(sb.toString());
        return getByAddress(jnetAddr.getAddress());
    }

    /**
     * Parse a literal representation of an IP address and returns the
     * corresponding IPAddress class.
     *
     * @param hostAddress
     * @return
     */
    public static IPAddress parseHostAddress(String hostAddress) throws UnknownHostException {
        InetAddress jnetAddr = InetAddress.getByName(hostAddress);
        return getByAddress(jnetAddr.getAddress());
    }

    /**
     * Returns the raw IP address of this InetAddress object.
     * The result is in network byte order: the highest order byte of the address is in getAddress()[0].
     *
     * @return the raw IP address of this object.
     */
    public byte[] getAddress() {
        return this.address;
    }

    /**
     * Returns the IP address string in textual presentation.
     *
     * @return the raw IP address in a string format.
     */
    public String getHostAddress() {
        return this.javaNetAddress.getHostAddress();
    }

    /**
     * Utility routine to check if the InetAddress in a wildcard address.
     *
     * @return a boolean indicating if the Inetaddress is a wildcard address.
     */
    public boolean isAnyLocalAddress() {
        return this.javaNetAddress.isAnyLocalAddress();
    }

    /**
     * Utility routine to check if the InetAddress is an link local address.
     *
     * @return a boolean indicating if the InetAddress is a link local address; or false if address is not a link local
     *         unicast address.
     */
    public boolean isLinkLocalAddress() {
        return this.javaNetAddress.isLinkLocalAddress();
    }

    /**
     * Utility routine to check if the InetAddress is a loopback address.
     *
     * @return a boolean indicating if the InetAddress is a loopback address; or false otherwise.
     */
    public boolean isLoopbackAddress() {
        return this.javaNetAddress.isLoopbackAddress();
    }

    /**
     * Utility routine to check if the multicast address has global scope.
     *
     * @return a boolean indicating if the address has is a multicast address of global scope, false if it is not of
     *         global scope or it is not a multicast address
     */
    public boolean isMCGlobal() {
        return this.javaNetAddress.isMCGlobal();
    }

    /**
     * Utility routine to check if the multicast address has link scope.
     *
     * @return a boolean indicating if the address has is a multicast address of link-local scope, false if it is not of
     *         link-local scope or it is not a multicast address
     */
    public boolean isMCLinkLocal() {
        return this.javaNetAddress.isMCLinkLocal();
    }

    /**
     * Utility routine to check if the multicast address has node scope.
     *
     * @return a boolean indicating if the address has is a multicast address of node-local scope, false if it is not of
     *         node-local scope or it is not a multicast address
     */
    public boolean isMCNodeLocal() {
        return this.javaNetAddress.isMCNodeLocal();
    }

    /**
     * Utility routine to check if the multicast address has organization scope.
     *
     * @return a boolean indicating if the address has is a multicast address of organization-local scope, false if it
     *         is not of organization-local scope or it is not a multicast address
     */
    public boolean isMCOrgLocal() {
        return this.javaNetAddress.isMCOrgLocal();
    }

    /**
     * Utility routine to check if the multicast address has site scope.
     *
     * @return a boolean indicating if the address has is a multicast address of site-local scope, false if it is not of
     *         site-local scope or it is not a multicast address
     */
    public boolean isMCSiteLocal() {
        return this.javaNetAddress.isMCSiteLocal();
    }

    /**
     * Utility routine to check if the InetAddress is an IP multicast address.
     *
     * @return
     */
    public boolean isMulticastAddress() {
        return this.javaNetAddress.isMulticastAddress();
    }

    /**
     * Utility routine to check if the InetAddress is a site local address.}
     *
     * @return a boolean indicating if the InetAddress is a site local address; or false if address is not a site local
     *         unicast address.
     */
    public boolean isSiteLocalAddress() {
        return this.javaNetAddress.isSiteLocalAddress();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.address);
        result = prime * result + (this.javaNetAddress == null ? 0 : this.javaNetAddress.hashCode());
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
        IPAddress other = (IPAddress) obj;
        if (!Arrays.equals(this.address, other.address)) {
            return false;
        }
        if (this.javaNetAddress == null) {
            if (other.javaNetAddress != null) {
                return false;
            }
        } else if (!this.javaNetAddress.equals(other.javaNetAddress)) {
            return false;
        }
        return true;
    }
}
