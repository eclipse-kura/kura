/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.net;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.net.EthernetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceType;

public class EthernetInterfaceImpl<T extends NetInterfaceAddress> extends AbstractNetInterface<T> implements EthernetInterface<T>
{
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
        ArrayList<T> interfaceAddresses = new ArrayList<T>();

        if(otherNetInterfaceAddresses != null) {
            for(NetInterfaceAddress netInterfaceAddress : otherNetInterfaceAddresses) {
                NetInterfaceAddressImpl copiedInterfaceAddressImpl = new NetInterfaceAddressImpl(netInterfaceAddress);
                interfaceAddresses.add((T)copiedInterfaceAddressImpl);
            }
        }
        this.setNetInterfaceAddresses(interfaceAddresses);
    }

	public NetInterfaceType getType() {
		return NetInterfaceType.ETHERNET;
	}

	public boolean isLinkUp() {
		return linkUp;
	}

	public void setLinkUp(boolean linkUp) {
		this.linkUp = linkUp;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString())
		.append(" :: linkUp=")
		.append(linkUp);
		return sb.toString();
	}
}
