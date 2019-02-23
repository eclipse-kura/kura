/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.net;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.wifi.WifiInterface;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;

public class WifiInterfaceConfigImpl extends WifiInterfaceImpl<WifiInterfaceAddressConfig>
        implements NetInterfaceConfig<WifiInterfaceAddressConfig> {

    public WifiInterfaceConfigImpl(String name) {
        super(name);
    }

    public WifiInterfaceConfigImpl(WifiInterface<? extends WifiInterfaceAddress> other) {
        super(other);

        // Copy the NetInterfaceAddresses
        List<? extends WifiInterfaceAddress> otherNetInterfaceAddresses = other.getNetInterfaceAddresses();
        ArrayList<WifiInterfaceAddressConfig> interfaceAddresses = new ArrayList<WifiInterfaceAddressConfig>();

        if (otherNetInterfaceAddresses != null) {
            for (WifiInterfaceAddress wifiInterfaceAddress : otherNetInterfaceAddresses) {
                WifiInterfaceAddressConfigImpl copiedInterfaceAddressImpl = new WifiInterfaceAddressConfigImpl(
                        wifiInterfaceAddress);
                interfaceAddresses.add(copiedInterfaceAddressImpl);
            }
        }
        if (interfaceAddresses.size() == 0) {
            // add at least one empty interface implementation.
            // It is needed as a container for the NetConfig objects
            interfaceAddresses.add(new WifiInterfaceAddressConfigImpl());
        }
        setNetInterfaceAddresses(interfaceAddresses);
    }
}
