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
 *******************************************************************************/
package org.eclipse.kura.core.net;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiInterface;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;

public class WifiInterfaceImpl<T extends WifiInterfaceAddress> extends AbstractNetInterface<T>
        implements WifiInterface<T> {

    private Set<Capability> capabilities = null;

    public WifiInterfaceImpl(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public WifiInterfaceImpl(WifiInterface<? extends WifiInterfaceAddress> other) {
        super(other);
        this.capabilities = other.getCapabilities();

        // Copy the NetInterfaceAddresses
        List<? extends WifiInterfaceAddress> otherNetInterfaceAddresses = other.getNetInterfaceAddresses();
        ArrayList<T> interfaceAddresses = new ArrayList<>();

        if (otherNetInterfaceAddresses != null) {
            for (WifiInterfaceAddress wifiInterfaceAddress : otherNetInterfaceAddresses) {
                WifiInterfaceAddressImpl copiedInterfaceAddressImpl = new WifiInterfaceAddressImpl(
                        wifiInterfaceAddress);
                interfaceAddresses.add((T) copiedInterfaceAddressImpl);
            }
        }
        setNetInterfaceAddresses(interfaceAddresses);
    }

    @Override
    public NetInterfaceType getType() {
        return NetInterfaceType.WIFI;
    }

    @Override
    public Set<Capability> getCapabilities() {
        if (this.capabilities != null) {
            return EnumSet.copyOf(this.capabilities);
        }

        return null;
    }

    public void setCapabilities(Set<Capability> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        if (this.capabilities != null && !this.capabilities.isEmpty()) {
            sb.append(" :: capabilities=");
            for (Capability capability : this.capabilities) {
                sb.append(capability).append(" ");
            }
        } else {
            sb.append(" :: capabilities=null");
        }
        return sb.toString();
    }
}
