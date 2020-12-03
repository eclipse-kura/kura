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
package org.eclipse.kura.core.net.modem;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;

public class ModemInterfaceConfigImpl extends ModemInterfaceImpl<ModemInterfaceAddressConfig>
        implements NetInterfaceConfig<ModemInterfaceAddressConfig> {

    public ModemInterfaceConfigImpl(String name) {
        super(name);
    }

    public ModemInterfaceConfigImpl(ModemInterface<? extends ModemInterfaceAddress> other) {
        super(ModemInterfaceAddressConfig.class, other);

        // Copy the NetInterfaceAddresses
        List<? extends ModemInterfaceAddress> otherNetInterfaceAddresses = other.getNetInterfaceAddresses();
        ArrayList<ModemInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        if (otherNetInterfaceAddresses != null) {
            for (ModemInterfaceAddress modemInterfaceAddress : otherNetInterfaceAddresses) {
                ModemInterfaceAddressConfigImpl copiedInterfaceAddressImpl = new ModemInterfaceAddressConfigImpl(
                        modemInterfaceAddress);
                copiedInterfaceAddressImpl.setConnectionType(ModemConnectionType.PPP);
                if (other.isUp()) {
                    copiedInterfaceAddressImpl.setConnectionStatus(ModemConnectionStatus.CONNECTED);
                } else {
                    copiedInterfaceAddressImpl.setConnectionStatus(ModemConnectionStatus.DISCONNECTED);
                }
                interfaceAddresses.add(copiedInterfaceAddressImpl);
            }
        }
        if (interfaceAddresses.isEmpty()) {
            // add at least one empty interface implementation.
            // It is needed as a container for the NetConfig objects
            interfaceAddresses.add(new ModemInterfaceAddressConfigImpl());
        }
        setNetInterfaceAddresses(interfaceAddresses);
    }
}
