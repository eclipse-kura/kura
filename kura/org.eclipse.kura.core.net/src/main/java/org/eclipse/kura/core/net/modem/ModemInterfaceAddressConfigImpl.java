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
package org.eclipse.kura.core.net.modem;

import java.util.List;

import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;

public class ModemInterfaceAddressConfigImpl extends ModemInterfaceAddressImpl implements ModemInterfaceAddressConfig {

    private List<NetConfig> m_configs;

    public ModemInterfaceAddressConfigImpl() {
        super();
    }

    public ModemInterfaceAddressConfigImpl(ModemInterfaceAddress other) {
        super(other);
    }

    @Override
    public List<NetConfig> getConfigs() {
        return this.m_configs;
    }

    public void setNetConfigs(List<NetConfig> configs) {
        this.m_configs = configs;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_configs == null) ? 0 : m_configs.hashCode());
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
		if (m_configs == null) {
			if (other.m_configs != null) {
				return false;
			}
		} else if (!m_configs.equals(other.m_configs)) {
			return false;
		}
		return true;
	}

	@Override
    public String toString() {
        if (this.m_configs != null) {
            StringBuffer sb = new StringBuffer();
            for (NetConfig netConfig : this.m_configs) {
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
