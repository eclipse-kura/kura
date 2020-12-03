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

import java.util.List;

import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;

public class NetInterfaceAddressConfigImpl extends NetInterfaceAddressImpl implements NetInterfaceAddressConfig {

    private List<NetConfig> configs;

    public NetInterfaceAddressConfigImpl() {
        super();
    }

    public NetInterfaceAddressConfigImpl(NetInterfaceAddress other) {
        super(other);
    }

    @Override
    public List<NetConfig> getConfigs() {
        return this.configs;
    }

    public void setNetConfigs(List<NetConfig> configs) {
        this.configs = configs;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof NetInterfaceAddressConfigImpl)) {
            return false;
        }

        NetInterfaceAddressConfigImpl other = (NetInterfaceAddressConfigImpl) obj;

        List<NetConfig> thisNetConfigs = getConfigs();
        List<NetConfig> otherNetConfigs = other.getConfigs();

        if (thisNetConfigs == null && otherNetConfigs == null) {
            // Both configurations are null
            return true;
        } else if (thisNetConfigs == null || otherNetConfigs == null) {
            // One configuration is null but the other one is not, so a null pointer exception would be thrown below!
            return false;
        }

        return thisNetConfigs.size() == otherNetConfigs.size() && thisNetConfigs.containsAll(otherNetConfigs)
                && otherNetConfigs.containsAll(thisNetConfigs);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.configs == null ? 0 : this.configs.hashCode());
        return result;
    }

    @Override
    public String toString() {
        if (this.configs != null) {
            StringBuilder sb = new StringBuilder();
            for (NetConfig netConfig : this.configs) {
                sb.append("NetConfig: ");
                if (netConfig != null) {
                    sb.append(netConfig.toString());
                } else {
                    sb.append("null");
                }
                sb.append(" - ");
            }

            return sb.toString();
        } else {
            return "NetConfig: no configurations";
        }
    }
}
