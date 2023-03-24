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
 *******************************************************************************/
package org.eclipse.kura.network.status.provider.api;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddress;

@SuppressWarnings("unused")
public class NetworkInterfaceIpAddressDTO {

    private final String address;
    private final short prefix;

    public NetworkInterfaceIpAddressDTO(final NetworkInterfaceIpAddress<? extends IPAddress> address) {
        this.address = address.getAddress().getHostAddress();
        this.prefix = address.getPrefix();
    }
}
