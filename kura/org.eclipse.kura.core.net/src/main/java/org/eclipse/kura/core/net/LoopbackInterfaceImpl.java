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
import java.util.List;

import org.eclipse.kura.net.LoopbackInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceType;

public class LoopbackInterfaceImpl<T extends NetInterfaceAddress> extends AbstractNetInterface<T>
        implements LoopbackInterface<T> {

    public LoopbackInterfaceImpl(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public LoopbackInterfaceImpl(LoopbackInterface<? extends NetInterfaceAddress> other) {
        super(other);

        // Copy the NetInterfaceAddresses
        List<? extends NetInterfaceAddress> otherNetInterfaceAddresses = other.getNetInterfaceAddresses();
        ArrayList<T> interfaceAddresses = new ArrayList<>();

        if (otherNetInterfaceAddresses != null) {
            for (NetInterfaceAddress netInterfaceAddress : otherNetInterfaceAddresses) {
                NetInterfaceAddressImpl copiedInterfaceAddressImpl = new NetInterfaceAddressImpl(netInterfaceAddress);
                interfaceAddresses.add((T) copiedInterfaceAddressImpl);
            }
        }
        setNetInterfaceAddresses(interfaceAddresses);
    }

    @Override
    public NetInterfaceType getType() {
        return NetInterfaceType.LOOPBACK;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
