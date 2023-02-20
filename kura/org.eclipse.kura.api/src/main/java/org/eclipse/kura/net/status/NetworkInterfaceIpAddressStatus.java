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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.net.IPAddress;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This class describes the IP address status of a network interface:
 * a list of IP addresses, an optional gateway and a list of DNS servers
 * address. It can be used for IPv4 or IPv6 addresses.
 *
 */
@ProviderType
public class NetworkInterfaceIpAddressStatus<T extends IPAddress> {

    private final List<NetworkInterfaceIpAddress<T>> addresses;
    private Optional<T> gateway;
    private final List<T> dnsServerAddresses;

    public NetworkInterfaceIpAddressStatus() {
        this.addresses = new ArrayList<>();
        this.gateway = Optional.empty();
        this.dnsServerAddresses = new ArrayList<>();
    }

    public NetworkInterfaceIpAddressStatus(NetworkInterfaceIpAddress<T> address) {
        this.addresses = new ArrayList<>();
        this.addresses.add(address);
        this.gateway = Optional.empty();
        this.dnsServerAddresses = new ArrayList<>();
    }

    public List<NetworkInterfaceIpAddress<T>> getAddresses() {
        return this.addresses;
    }

    public void addAddress(NetworkInterfaceIpAddress<T> address) {
        this.addresses.add(address);
    }

    public Optional<T> getGateway() {
        return this.gateway;
    }

    public void setGateway(T gateway) {
        this.gateway = Optional.of(gateway);
    }

    public List<T> getDnsServerAddresses() {
        return this.dnsServerAddresses;
    }

    public void addDnsServerAddress(T dnsServerAddress) {
        this.dnsServerAddresses.add(dnsServerAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.addresses, this.dnsServerAddresses, this.gateway);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        NetworkInterfaceIpAddressStatus<T> other = (NetworkInterfaceIpAddressStatus<T>) obj;
        return Objects.equals(this.addresses, other.addresses)
                && Objects.equals(this.dnsServerAddresses, other.dnsServerAddresses)
                && Objects.equals(this.gateway, other.gateway);
    }

}
