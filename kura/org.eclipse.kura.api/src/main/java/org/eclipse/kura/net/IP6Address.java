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
 * This class represents an Internet Protocol version 6 (IPv6) address.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class IP6Address extends IPAddress {

    IP6Address(byte[] addr, java.net.InetAddress jnAddress) {
        super(addr, jnAddress);
    }

    /**
     * Utility routine to check if the InetAddress is an IPv4 compatible IPv6 address.
     *
     * @return a boolean indicating if the InetAddress is an IPv4 compatible IPv6 address; or false if address is IPv4
     *         address.
     */
    public boolean isIPv4CompatibleAddress() {
        return ((java.net.Inet6Address) this.javaNetAddress).isIPv4CompatibleAddress();
    }
}
