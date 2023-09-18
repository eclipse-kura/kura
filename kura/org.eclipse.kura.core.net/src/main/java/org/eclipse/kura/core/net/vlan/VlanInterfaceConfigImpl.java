/*******************************************************************************
 * Copyright (c) 2023 Areti and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Areti
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.net.vlan;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.vlan.VlanInterface;

public class VlanInterfaceConfigImpl extends VlanInterfaceImpl<NetInterfaceAddressConfig>
        implements NetInterfaceConfig<NetInterfaceAddressConfig> {

    public VlanInterfaceConfigImpl(String name) {
        super(name);
    }

    public VlanInterfaceConfigImpl(VlanInterface<? extends NetInterfaceAddress> other) {
        super(other);

        // Copy the NetInterfaceAddresses into NetInterfaceAddressesConfig instances
        List<? extends NetInterfaceAddress> otherNetInterfaceAddresses = other.getNetInterfaceAddresses();
        ArrayList<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        if (otherNetInterfaceAddresses != null) {
            for (NetInterfaceAddress netInterfaceAddress : otherNetInterfaceAddresses) {
                NetInterfaceAddressConfigImpl copiedInterfaceAddressImpl = new NetInterfaceAddressConfigImpl(
                        netInterfaceAddress);
                interfaceAddresses.add(copiedInterfaceAddressImpl);
            }
        }
        if (interfaceAddresses.size() == 0) {
            // add at least one empty interface implementation.
            // It is needed as a container for the NetConfig objects
            interfaceAddresses.add(new NetInterfaceAddressConfigImpl());
        }
        setNetInterfaceAddresses(interfaceAddresses);
    }
}
