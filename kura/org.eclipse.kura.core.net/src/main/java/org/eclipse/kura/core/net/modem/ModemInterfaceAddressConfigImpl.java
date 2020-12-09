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

import java.util.List;

import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;

public class ModemInterfaceAddressConfigImpl extends ModemInterfaceAddressImpl implements ModemInterfaceAddressConfig {

    private List<NetConfig> configs;

    public ModemInterfaceAddressConfigImpl() {
        super();
    }

    public ModemInterfaceAddressConfigImpl(ModemInterfaceAddress other) {
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.configs == null ? 0 : this.configs.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ModemInterfaceAddressConfigImpl)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        ModemInterfaceAddressConfigImpl other = (ModemInterfaceAddressConfigImpl) obj;
        if (this.configs == null) {
            if (other.configs != null) {
                return false;
            }
        } else if (!this.configs.equals(other.configs)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (this.configs != null) {
            StringBuffer sb = new StringBuffer();
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
