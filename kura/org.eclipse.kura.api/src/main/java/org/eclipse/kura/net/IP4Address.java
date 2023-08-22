/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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

import java.net.UnknownHostException;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represents an Internet Protocol version 4 (IPv4) address.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class IP4Address extends IPAddress {

    IP4Address(byte[] addr, java.net.InetAddress jnAddress) {
        super(addr, jnAddress);
    }

    /**
     * Returns the default IPv4 address (0.0.0.0/0).
     * 
     * @return the 0.0.0.0/0 IPv4 address
     * @throws UnknownHostException
     * @since 2.6
     */
    public static IP4Address getDefaultAddress() throws UnknownHostException {
        return (IP4Address) IPAddress.parseHostAddress("0.0.0.0");
    }

    @Override
    public String toString() {
        return getHostAddress();
    }
}
