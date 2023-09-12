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
import java.util.Objects;

import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.vlan.VlanInterface;

public class VlanInterfaceImpl<T extends NetInterfaceAddress> extends AbstractNetInterface<T>
        implements VlanInterface<T> {

    private int flags;
    private String parentInterface;
    private int vlanId;
    private List<String> ingressMap;
    private List<String> egressMap;

    public VlanInterfaceImpl(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public VlanInterfaceImpl(VlanInterface<? extends NetInterfaceAddress> other) {
        super(other);

        this.flags = other.getFlags();
        this.parentInterface = other.getParentInterface();
        this.vlanId = other.getVlanId();

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
        return NetInterfaceType.VLAN;
    }
    
    @Override
    public int getFlags() {
        return this.flags;
    }
    
    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Override
    public String getParentInterface() {
        return this.parentInterface;
    }

    public void setParentInterface(String parentInterface) {
        this.parentInterface = parentInterface;
    }
    
    @Override
    public int getVlanId() {
        return this.vlanId;
    }

    public void setVlanId(int vlanId) {
        this.vlanId = vlanId;
    }
    
    public List<String> getIngressMap() {
        return this.ingressMap;
    }
    
    public void setIngressMap(List<String> ingressMap) {
        this.ingressMap = ingressMap;
    }
    
    public List<String> getEgressMap() {
        return this.egressMap;
    }
    
    public void setEgressMap(List<String> egressMap) {
        this.egressMap = egressMap;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" :: flags=")
            .append(this.flags).append(" :: parentInterface=")
            .append(this.parentInterface).append(" :: vlanId=").append(this.vlanId)
            .append(" :: ingressMap=").append(this.ingressMap)
            .append(" :: egressMap=").append(this.egressMap);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(this.flags, this.parentInterface, 
                this.vlanId, this.ingressMap, this.egressMap);
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
        if (!(obj instanceof VlanInterfaceImpl)) {
            return false;
        }
        VlanInterfaceImpl other = (VlanInterfaceImpl) obj;
        return this.vlanId == other.vlanId && this.flags == other.flags
                && Objects.equals(this.parentInterface, other.getParentInterface())
                && Objects.equals(this.ingressMap, other.getIngressMap())
                && Objects.equals(this.egressMap, other.getEgressMap());
    }
}
