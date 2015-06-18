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
package org.eclipse.kura.web.shared.model;

import java.util.Map;

public class GwtBSWifiNetInterfaceConfig extends GwtBSNetInterfaceConfig 
{
	private static final long serialVersionUID = -7509149975400963837L;
	
	private GwtBSWifiConfig m_adhocWifiConfig;
	private GwtBSWifiConfig m_stationWifiConfig;
	private GwtBSWifiConfig m_accessPointWifiConfig;
	
	public GwtBSWifiNetInterfaceConfig() {
		super();
		
		m_adhocWifiConfig = new GwtBSWifiConfig();
		m_stationWifiConfig = new GwtBSWifiConfig();
		m_accessPointWifiConfig = new GwtBSWifiConfig();
		
		setWirelessMode(GwtBSWifiWirelessMode.netWifiWirelessModeStation.name());
	}
	
	public GwtBSWifiNetInterfaceConfig(GwtBSWifiConfig adhocConfig,
			GwtBSWifiConfig stationConfig, GwtBSWifiConfig accessPointConfig) {
		super();

		m_adhocWifiConfig = adhocConfig;
		m_stationWifiConfig = stationConfig;
		m_accessPointWifiConfig = accessPointConfig;

		setWirelessMode(GwtBSWifiWirelessMode.netWifiWirelessModeStation.name());
	}
	
	public void setAdhocWifiConfig(GwtBSWifiConfig adhocConfig) {
		m_adhocWifiConfig = adhocConfig;
	}
	
	public void setAdhocWifiConfig(Map<String, Object> properties) {		
		m_adhocWifiConfig = new GwtBSWifiConfig();
		m_adhocWifiConfig.setProperties(properties);
	}
	
	public GwtBSWifiConfig getAdhocWifiConfig() {
		return m_adhocWifiConfig;
	}
	
	public Map<String, Object> getAdhocWifiConfigProps() {
		return m_adhocWifiConfig.getProperties();
	}
	
	public void setStationWifiConfig(GwtBSWifiConfig stationConfig) {
		m_stationWifiConfig = stationConfig;
	}
	
	public void setStationWifiConfig(Map<String, Object> properties) {
		m_stationWifiConfig = new GwtBSWifiConfig();
		m_stationWifiConfig.setProperties(properties);
	}
	
	public GwtBSWifiConfig getStationWifiConfig() {
		return m_stationWifiConfig;
	}
	
	public Map<String, Object> getStationWifiConfigProps() {
		
		return m_stationWifiConfig.getProperties();
	}
	
	public void setAccessPointWifiConfig(GwtBSWifiConfig accessPointConfig) {
		m_accessPointWifiConfig = accessPointConfig;
	}
	
	public void setAccessPointWifiConfig (Map<String, Object> properties) {
		m_accessPointWifiConfig = new GwtBSWifiConfig();
		m_accessPointWifiConfig.setProperties(properties);
	}
	
	public GwtBSWifiConfig getAccessPointWifiConfig() {
		return m_accessPointWifiConfig;
	}
	
	public Map<String, Object> getAccessPointWifiConfigProps() {
		
		return m_accessPointWifiConfig.getProperties();
	}
	
	public void setWifiConfig(GwtBSWifiConfig wifiConfig) {
		GwtBSWifiWirelessMode wifiMode = wifiConfig.getWirelessModeEnum();
		
	    if(wifiMode.equals(GwtBSWifiWirelessMode.netWifiWirelessModeAccessPoint)) {
	    	setAccessPointWifiConfig(wifiConfig);
	    } else if(wifiMode.equals(GwtBSWifiWirelessMode.netWifiWirelessModeAdHoc)) {
	    	setAdhocWifiConfig(wifiConfig);
	    } else if(wifiMode.equals(GwtBSWifiWirelessMode.netWifiWirelessModeStation)) {
	    	setStationWifiConfig(wifiConfig);
	    }
	}
	
	public GwtBSWifiConfig getActiveWifiConfig() {
		GwtBSWifiWirelessMode wifiMode = getWirelessModeEnum();
		GwtBSWifiConfig activeConfig = null;
		
	    if(wifiMode.equals(GwtBSWifiWirelessMode.netWifiWirelessModeAccessPoint)) {
	    	activeConfig = m_accessPointWifiConfig;
	    } else if(wifiMode.equals(GwtBSWifiWirelessMode.netWifiWirelessModeAdHoc)) {
	    	activeConfig = m_adhocWifiConfig;
	    } else if(wifiMode.equals(GwtBSWifiWirelessMode.netWifiWirelessModeStation)) {
	    	activeConfig = m_stationWifiConfig;
	    }
	    
	    return activeConfig;
	}
	
    public void setWirelessMode(String wirelessMode) {
        set("wirelessMode", wirelessMode);
    }
    
    public String getWirelessMode() {
        return get("wirelessMode");
    }

    public GwtBSWifiWirelessMode getWirelessModeEnum() {
        return GwtBSWifiWirelessMode.valueOf(getWirelessMode());
    }
    
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof GwtBSWifiNetInterfaceConfig)) {
            return false;
        }

        if(super.equals(o) == false) {
            return false;
        }
        
        GwtBSWifiNetInterfaceConfig other = (GwtBSWifiNetInterfaceConfig) o;
        
        if(compare(getActiveWifiConfig(), other.getActiveWifiConfig()) == false) {
            return false;
        }
        
        return true;
    }
    
    private boolean compare(Object obj1, Object obj2) {
        if(obj1 == null) {
            if(obj2 != null) {
                return false;
            }
        } else {
            return obj1.equals(obj2);
        }
        
        return true;
    }
}
