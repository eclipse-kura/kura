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

import java.util.List;


public class GwtBSModemInterfaceConfig extends GwtBSNetInterfaceConfig
{
	private static final long serialVersionUID = -2586979604841994110L;

	public GwtBSModemInterfaceConfig() {
		super();
	}
	
	public String getManufacturer() {
	    return (String) get("manufacturer");
	}
	
	public void setManufacturer(String manufacturer) {
	    set("manufacturer", manufacturer);
	}
	
	public String getModel() {
	    return (String) get("model");
	}
	
	public void setModel(String model) {
	    set("model", model);
	}
	
	public int getPppNum() {
	    return ((Integer) get("pppNum")).intValue();
	}
	
	public void setPppNum(int pppNum) {
	    set("pppNum", String.valueOf(pppNum));
	}
	
	public String getModemId() {
	    return (String) get("modemId");
	}
	
	public void setModemId(String modemId) {
	    set("modemId", modemId);
	}
	
	public String getDialString() {
		return (String) get("dialString");
	}
	
	public void setDialString(String dialString) {
		set("dialString", dialString);
	}
	
	public GwtBSModemAuthType getAuthType() {
	    if(get("authType") != null) {
            return GwtBSModemAuthType.valueOf((String)get("authType"));
	    } else {
            return null;
	    }
	}
	
	public void setAuthType(GwtBSModemAuthType authType) {
		set("authType", authType.name());
	}
	
	public String getUsername() {
		return (String) get("username");
	}
	
	public void setUsername(String username) {
		set("username", username);
	}
    
    public String getPassword() {
        return (String) get("password");
    }
    
    public void setPassword(String password) {
        set("password", password);
    }
    
    public boolean isPersist() {
    	if (get("persist") != null) {
    		return Boolean.valueOf(String.valueOf( get("persist")));
    	}
    	return false;
    }

    public void setPersist(boolean persist) {
        set("persist", String.valueOf(persist));
    }
    
    public int getMaxFail() {
    	 return ((Integer)get("maxFail")).intValue();
    }
    
    public void setMaxFail(int maxFail) {
	    set("maxFail", String.valueOf(maxFail));
	}
    
    public int getIdle() {
    	return ((Integer)get("idle")).intValue();
    }
   
    public void setIdle(int idle) {
	    set("idle", String.valueOf(idle));
	}
    
    public String getActiveFilter() {
		return (String) get("activeFilter");
	}
	
	public void setActiveFilter(String activeFilter) {
		set("activeFilter", activeFilter);
	}
    
    public int getLcpEchoInterval() {
	    return (Integer) get("lcpEchoInterval");
	}
	
	public void setLcpEchoInterval(int lcpEchoInterval) {
	    set("lcpEchoInterval", String.valueOf(lcpEchoInterval));
	}
	
	public int getLcpEchoFailure() {
	    return (Integer)get("lcpEchoFailure");
	}
	
	public void setLcpEchoFailure(int lcpEchoFailure) {
	    set("lcpEchoFailure", String.valueOf(lcpEchoFailure));
	}
    
	public int getProfileID() {
    	if (get("profileID") != null) {
    		return (Integer)(get("profileID"));
    	}
    	else {
    		return 0;
    	}
	}

	public void setProfileID(int id) {
		set("profileID", String.valueOf(id));
	}
	
	public GwtBSModemPdpType getPdpType() {
        if(get("pdpType") != null) {
            return GwtBSModemPdpType.valueOf((String)get("pdpType"));
        } else {
            return null;
        }
	}
	
	public void setPdpType(GwtBSModemPdpType pdpType) {
		set("pdpType", pdpType.name());
	}
	
	public String getApn() {
		return (String) get("apn");
	}
	
	public void setApn(String apn) {
		set("apn", apn);
	}
	
	public int getDataCompression() {
    	if (get("dataCompression") != null) {
    		return (Integer)( get("dataCompression"));
    	}
    	else {
    		return 0;
    	}
	}
	
	public void setDataCompression(int dataCompression) {
		set("dataCompression", String.valueOf(dataCompression));
	}
	
	public int getHeaderCompression() {
    	if (get("headerCompression") != null) {
    		return (Integer)(get("headerCompression"));
    	}
    	else {
    		return 0;
    	}
	}
	
	public void setHeaderCompression(int headerCompression) {
		set("headerCompression", String.valueOf(headerCompression));
	}
    
    public boolean isDataCompression() {
        return (this.getDataCompression() == 0)? false : true;
    }
    
    public boolean isHeaderCompression() {
        return (this.getHeaderCompression() == 0)? false : true;
    }
    
    public void setNetworkTechnology(List<String> networkTechnology) {
        set("networkTechnology", networkTechnology);
    }
    
    public List<String> getNetworkTechnology() {
        return (List<String>) get("networkTechnology");
    }
	
	public void setConnectionType(String connectionType) {
	    set("connectionType", connectionType);
	}
	
	public String getConnectionType() {
		return (String) get("connectionType");
	}

	public int getResetTimeout() {
		return (Integer)(get("resetTimeout"));
	}

	public void setResetTimeout(int resetTimeout) {
		set("resetTimeout", String.valueOf(resetTimeout));
	}
	
	public boolean isGpsEnabled() {
    	if (get("gpsEnabled") != null) {
    		return (Boolean)(get("gpsEnabled"));
    	}
    	return false;
    }

    public void setGpsEnabled(boolean gpsEnabled) {
        set("gpsEnabled", String.valueOf(gpsEnabled));
    }
    
    public boolean isGpsSupported() {
    	if (get("gpsSupported") != null) {
    		return (Boolean)get("gpsSupported");
    	}
    	return false;
    }

    public void setGpsSupported(boolean gpsSupported) {
        set("gpsSupported", String.valueOf(gpsSupported));
    }
}
