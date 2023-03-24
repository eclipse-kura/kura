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

import java.util.Collections;
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
    private final Optional<T> gateway;
    private final List<T> dnsServerAddresses;

    private NetworkInterfaceIpAddressStatus(Builder<T> builder) {
        this.addresses = builder.addresses;
        this.gateway = builder.gateway;
        this.dnsServerAddresses = builder.dnsServerAddresses;
    }

    public List<NetworkInterfaceIpAddress<T>> getAddresses() {
        return this.addresses;
    }

    public Optional<T> getGateway() {
        return this.gateway;
    }

    public List<T> getDnsServerAddresses() {
        return this.dnsServerAddresses;
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
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NetworkInterfaceIpAddressStatus<T> other = (NetworkInterfaceIpAddressStatus<T>) obj;
        return Objects.equals(this.addresses, other.addresses)
                && Objects.equals(this.dnsServerAddresses, other.dnsServerAddresses)
                && Objects.equals(this.gateway, other.gateway);
    }

    public static <U extends IPAddress> Builder<U> builder() {
        return new Builder<>();
    }

    public static final class Builder<U extends IPAddress> {

        private List<NetworkInterfaceIpAddress<U>> addresses = Collections.emptyList();
        private Optional<U> gateway = Optional.empty();
        private List<U> dnsServerAddresses = Collections.emptyList();

        private Builder() {
        }

        public Builder<U> withAddresses(List<NetworkInterfaceIpAddress<U>> addresses) {
            this.addresses = addresses;
            return this;
        }

        public Builder<U> withGateway(Optional<U> gateway) {
            this.gateway = gateway;
            return this;
        }

        public Builder<U> withDnsServerAddresses(List<U> dnsServerAddresses) {
            this.dnsServerAddresses = dnsServerAddresses;
            return this;
        }

        public NetworkInterfaceIpAddressStatus<U> build() {
            return new NetworkInterfaceIpAddressStatus<>(this);
        }
    }

}
