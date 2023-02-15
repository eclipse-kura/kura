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

public class NetworkInterfaceIpAddress<T extends IPAddress> {

    private final T address;
    private final short prefix;
    private final T broadcast;

    public NetworkInterfaceIpAddress(T address, short prefix, T broadcast) {
        this.address = address;
        this.prefix = prefix;
        this.broadcast = broadcast;
    }

    public T getAddress() {
        return this.address;
    }

    public short getPrefix() {
        return this.prefix;
    }

    public T getBroadcast() {
        return this.broadcast;
    }

}
