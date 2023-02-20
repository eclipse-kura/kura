/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.status;

import org.eclipse.kura.net.IPAddress;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This class describes an IP address with its prefix.
 * It can be used for IPv4 or IPv6 addresses.
 *
 */
@ProviderType
public class NetworkInterfaceIpAddress<T extends IPAddress> {

    private final T address;
    private final short prefix;

    public NetworkInterfaceIpAddress(T address, short prefix) {
        this.address = address;
        this.prefix = prefix;
    }

    public T getAddress() {
        return this.address;
    }

    public short getPrefix() {
        return this.prefix;
    }

}
