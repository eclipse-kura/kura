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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddressStatus;

@SuppressWarnings("unused")
public class NetworkInterfaceIpAddressStatusDTO {

    private final List<NetworkInterfaceIpAddressDTO> addresses;
    private final String gateway;
    private final List<String> dnsServerAddresses;

    public NetworkInterfaceIpAddressStatusDTO(final NetworkInterfaceIpAddressStatus<? extends IPAddress> status) {
        this.addresses = status.getAddresses().stream().map(NetworkInterfaceIpAddressDTO::new)
                .collect(Collectors.toList());
        this.gateway = status.getGateway().map(IPAddress::getHostAddress).orElse(null);
        this.dnsServerAddresses = status.getDnsServerAddresses().stream().map(IPAddress::getHostAddress)
                .collect(Collectors.toList());
    }
}
