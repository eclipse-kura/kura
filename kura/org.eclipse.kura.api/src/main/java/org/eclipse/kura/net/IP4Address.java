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
 * This class represents an Internet Protocol version 4 (IPv4) address.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class IP4Address extends IPAddress {

    IP4Address(byte[] addr, java.net.InetAddress jnAddress) {
        super(addr, jnAddress);
    }

    @Override
    public String toString() {
        return getHostAddress();
    }
}
