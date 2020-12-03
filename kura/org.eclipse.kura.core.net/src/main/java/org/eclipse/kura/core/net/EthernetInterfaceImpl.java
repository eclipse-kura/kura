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

import org.eclipse.kura.net.EthernetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceType;

public class EthernetInterfaceImpl<T extends NetInterfaceAddress> extends AbstractNetInterface<T>
        implements EthernetInterface<T> {

    private boolean linkUp;

    public EthernetInterfaceImpl(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public EthernetInterfaceImpl(EthernetInterface<? extends NetInterfaceAddress> other) {
        super(other);
        this.linkUp = other.isLinkUp();

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
        return NetInterfaceType.ETHERNET;
    }

    @Override
    public boolean isLinkUp() {
        return this.linkUp;
    }

    public void setLinkUp(boolean linkUp) {
        this.linkUp = linkUp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" :: linkUp=").append(this.linkUp);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.linkUp ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof EthernetInterfaceImpl)) {
            return false;
        }
        EthernetInterfaceImpl other = (EthernetInterfaceImpl) obj;
        if (this.linkUp != other.linkUp) {
            return false;
        }
        return true;
    }
}
