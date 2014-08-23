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

import java.io.Serializable;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.data.BaseModelData;

public class GwtNetInterfaceConfig extends BaseModelData implements Serializable
{
	private static final long serialVersionUID = 7079533925979145804L;
	
	public GwtNetInterfaceConfig() {	
		setStatus(GwtNetIfStatus.netIPv4StatusDisabled.name());
		setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
		setRouterMode(GwtNetRouterMode.netRouterOff.name());
	}

    public String getName() {
        return get("name");
    }

    public void setName(String name) {
        set("name", name);
    }
    
    public GwtNetIfStatus getStatusEnum() {
        return GwtNetIfStatus.valueOf(getStatus());
    }

    public String getStatus() {
        return get("status");
    }

    public void setStatus(String status) {
        set("status", status);
    }
    
    public GwtNetIfConfigMode getConfigModeEnum() {
        return GwtNetIfConfigMode.valueOf(getConfigMode());
    }

    public String getConfigMode() {
        return get("configMode");
    }

    public void setConfigMode(String configMode) {
        set("configMode", configMode);
    }
 
    public String getIpAddress() {
        return get("ipAddress");
    }

    public void setIpAddress(String ipAddress) {
        set("ipAddress", ipAddress);
    }

    public String getSubnetMask() {
        return get("subnetMask");
    }

    public void setSubnetMask(String subnetMask) {
        set("subnetMask", subnetMask);
    }

    public String getGateway() {
        return get("gateway");
    }

    public void setGateway(String gateway) {
        set("gateway", gateway);
    }

    public String getDnsServers() {
        return get("dnsServers");
    }

    public void setDnsServers(String dnsServers) {
        set("dnsServers", dnsServers);
    }

    public String getReadOnlyDnsServers() {
        return get("dnsReadOnlyServers");
    }

    public void setReadOnlyDnsServers(String dnsServers) {
        set("dnsReadOnlyServers", dnsServers);
    }
    
    public String getSearchDomains() {
        return get("searchDomains");
    }

    public void setSearchDomains(String searchDomains) {
        set("searchDomains", searchDomains);
    }
    
    public void setHwState(String hwState) {
    	set("hwState", hwState);
    }

    public String getHwState() {
    	return get("hwState");
    }

    public void setHwName(String hwName) {
    	set("hwName", hwName);
    }

    public String getHwName() {
    	return get("hwName");
    }
    
    public GwtNetIfType getHwTypeEnum() {
        GwtNetIfType typeEnum = GwtNetIfType.UNKNOWN;
        
        try {
            typeEnum = GwtNetIfType.valueOf(getHwType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return typeEnum;
    }

    public void setHwType(String hwType) {
    	set("hwType", hwType);
    }

    public String getHwType() {
    	return get("hwType");
    }

    public void setHwAddress(String hwAddress) {
    	set("hwAddress", hwAddress);
    }

    public String getHwAddress() {
    	return get("hwAddress");
    }
    
    public void setHwSerial(String hwSerial) {
        set("hwSerial", hwSerial);
    }
    
    public String getHwSerial() {
        return get("hwSerial");
    }

    public void setHwDriver(String hwDriver) {
    	set("hwDriver", hwDriver);
    }

    public String getHwDriver() {
    	return get("hwDriver");
    }

    public void setHwDriverVersion(String hwDriverVersion) {
    	set("hwDriverVersion", hwDriverVersion);
    }

    public String getHwDriverVersion() {
    	return get("hwDriverVersion");
    }

    public void setHwFirmware(String hwFirmware) {
    	set("hwFirmware", hwFirmware);
    }

    public String getHwFirmware() {
    	return get("hwFirmware");
    }

    public void setHwMTU(int mtu) {
    	set("hwMTU", mtu);
    }

    public int getHwMTU() {
    	if (get("hwMTU") != null) {
    		return (Integer) get("hwMTU");
    	}
    	else {
    		return 0;
    	}
    }

    public void setHwUsbDevice(String hwUsbDevice) {
    	set("hwUsbDevice", hwUsbDevice);
    }

    public String getHwUsbDevice() {
    	return get("hwUsbDevice");
    }
    
    public String getHwRssi() {
    	return get("hwRssi");
    }
    
    public void setHwRssi(String rssi) {
    	set("hwRssi", rssi);
    }
    
    public GwtNetRouterMode getRouterModeEnum() {
        return GwtNetRouterMode.valueOf(getRouterMode());
}

    public String getRouterMode() {
        return get("routerMode");
    }

    public void setRouterMode(String routerMode) {
        set("routerMode", routerMode);
    }

    public String getRouterDhcpBeginAddress() {
        return get("routerDhcpBeginAddress");
    }

    public void setRouterDhcpBeginAddress(String routerDhcpBeginAddress) {
        set("routerDhcpBeginAddress", routerDhcpBeginAddress);
    }

    public String getRouterDhcpEndAddress() {
        return get("routerDhcpEndAddress");
    }

    public void setRouterDhcpEndAddress(String routerDhcpEndAddress) {
        set("routerDhcpEndAddress", routerDhcpEndAddress);
    }

    public int getRouterDhcpDefaultLease() {
    	if (get("routerDhcpDefaultLease") != null) {
    		return (Integer) get("routerDhcpDefaultLease");
    	}
		return 0;
    }

    public void setRouterDhcpDefaultLease(int routerDhcpDefaultLease) {
        set("routerDhcpDefaultLease", routerDhcpDefaultLease);
    }
    
    public int getRouterDhcpMaxLease() {
    	if (get("routerDhcpMaxLease") != null) {
    		return (Integer) get("routerDhcpMaxLease");
    	}
		return 0;
    }

    public void setRouterDhcpMaxLease(int routerDhcpMaxLease) {
        set("routerDhcpMaxLease", routerDhcpMaxLease);
    }

    public String getRouterDhcpSubnetMask() {
		return (String) get("routerDhcpSubnetMask");
    }
    
    public void setRouterDhcpSubnetMask(String routerDhcpSubnetMask) {
    	set("routerDhcpSubnetMask", routerDhcpSubnetMask);
    }
    
    public boolean getRouterDnsPass() {
    	if (get("routerDnsPass") != null) {
    		return (Boolean) get("routerDnsPass");
    	}
    	return false;
    }

    public void setRouterDnsPass(boolean routerDnsPass) {
        set("routerDnsPass", routerDnsPass);
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof GwtNetInterfaceConfig)) {
            return false;
        }
        
        Map<String, Object> properties = this.getProperties();
        Map<String, Object> otherProps = ((GwtNetInterfaceConfig)o).getProperties();
        
        if(properties != null) {
            if(otherProps == null) {
                return false;
            }            
            if(properties.size() != otherProps.size()) {
                Log.debug("Sizes differ");
                return false;
            }
            
            Object oldVal, newVal;
            for(String key : properties.keySet()) {
                oldVal = properties.get(key);
                newVal = otherProps.get(key);                
                if(oldVal != null) {
                    if(!oldVal.equals(newVal)) {
                        Log.debug("Values differ - Key: " + key + " oldVal: " + oldVal + ", newVal: " + newVal);
                        return false;
                    }
                } else if(newVal != null) {
                    return false;
                }
            }
        } else if(otherProps != null) {
            return false;
        }
        
        return true;
    }
}



