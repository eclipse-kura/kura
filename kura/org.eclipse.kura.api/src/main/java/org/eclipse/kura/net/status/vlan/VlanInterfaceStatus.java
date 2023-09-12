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
 ******************************************************************************/
package org.eclipse.kura.net.status.vlan;

import java.util.Objects;

import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Class that contains specific properties to describe the status of a
 * Vlan.
 *
 */
@ProviderType
public class VlanInterfaceStatus extends NetworkInterfaceStatus {

    private final String parentInterface;
    private final int vlanId;
    
    private VlanInterfaceStatus(VlanInterfaceStatusBuilder builder) {
        super(builder);
        this.vlanId = builder.vlanId;
        this.parentInterface = builder.parentInterface;
    }

    public int getVlanId() {
        return this.vlanId;
    }

    public String getParentInterface() {
        return this.parentInterface;
    }
    
    public static VlanInterfaceStatusBuilder builder() {
        return new VlanInterfaceStatusBuilder();
    }

    public static class VlanInterfaceStatusBuilder
        extends NetworkInterfaceStatusBuilder<VlanInterfaceStatusBuilder> {
        
        private String parentInterface;
        private int vlanId;
        
        public VlanInterfaceStatusBuilder withParentInterface(String parentInterface) {
            this.parentInterface = parentInterface;
            return getThis();
        }
        
        public VlanInterfaceStatusBuilder withVlanId(int vlanId) {
            this.vlanId = vlanId;
            return getThis();
        }
        
        public VlanInterfaceStatus build() {
            withType(NetworkInterfaceType.VLAN);
            return new VlanInterfaceStatus(this);
        }
        
        public VlanInterfaceStatusBuilder getThis() {
            return this;
        }
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(this.vlanId, this.parentInterface);
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj) || getClass() != obj.getClass()) {
            return false;
        }
        VlanInterfaceStatus other = (VlanInterfaceStatus) obj;
        return this.vlanId == other.vlanId 
                && Objects.equals(this.parentInterface, other.getParentInterface());
    }
    
}
