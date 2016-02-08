/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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
		return m_configs;
	}

	public void setNetConfigs(List<NetConfig> configs) {
		m_configs = configs;
	}
	
    @Override
    public boolean equals(Object obj) {
        
        if (this == obj) {
            return true;
        }

        if(!super.equals(obj)) {
            return false;
        }

        if(!(obj instanceof ModemInterfaceAddressConfig)) {
            return false;
        }
        
        ModemInterfaceAddressConfig other = (ModemInterfaceAddressConfig) obj;
        
        if(!compare(this.getSignalStrength(), other.getSignalStrength())) {
            return false;
        }
        
        if(!compare(this.isRoaming(), other.isRoaming())) {
            return false;
        }
        
        if(!compare(this.getConnectionStatus(), other.getConnectionStatus())) {
            return false;
        }
        
        if(this.getBytesTransmitted() != other.getBytesTransmitted()) {
            return false;
        }
        
        if(this.getBytesReceived() != other.getBytesReceived()) {
            return false;
        }
        
        if(!compare(this.getConnectionType(), other.getConnectionType())) {
            return false;
        }
        
        List<NetConfig> thisNetConfigs = getConfigs();
        List<NetConfig> otherNetConfigs = other.getConfigs();
        
        if(thisNetConfigs.size() != otherNetConfigs.size()) {
            return false;
        }        
        if(!thisNetConfigs.containsAll(otherNetConfigs)) {
            return false;
        }
        if(!otherNetConfigs.containsAll(thisNetConfigs)) {
            return false;
        }
        
        return true;
    }
    
	@Override
	public String toString() {
		if(m_configs != null) {
			StringBuffer sb = new StringBuffer();
			for(NetConfig netConfig : m_configs) {
				sb.append("NetConfig: ");
				if(netConfig != null) {
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
