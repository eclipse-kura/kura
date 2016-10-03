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

import org.eclipse.kura.net.LoopbackInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;

public class LoopbackInterfaceConfigImpl extends LoopbackInterfaceImpl<NetInterfaceAddressConfig>
        implements NetInterfaceConfig<NetInterfaceAddressConfig> {

    public LoopbackInterfaceConfigImpl(String name) {
        super(name);
    }

    public LoopbackInterfaceConfigImpl(LoopbackInterface<? extends NetInterfaceAddress> other) {
        super(other);

        // Copy the NetInterfaceAddresses
        List<? extends NetInterfaceAddress> otherNetInterfaceAddresses = other.getNetInterfaceAddresses();
        ArrayList<NetInterfaceAddressConfig> interfaceAddresses = new ArrayList<NetInterfaceAddressConfig>();

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
