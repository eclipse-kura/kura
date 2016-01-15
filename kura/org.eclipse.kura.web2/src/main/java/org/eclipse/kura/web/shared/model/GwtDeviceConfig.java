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
import java.util.Date;

import org.eclipse.kura.web.client.util.DateUtils;

import com.extjs.gxt.ui.client.data.BaseModel;

public class GwtDeviceConfig extends BaseModel implements Serializable {

    private static final long serialVersionUID = 1708831984640005284L;
    
	public GwtDeviceConfig() {}   
	
    @Override
    @SuppressWarnings({"unchecked"})
    public <X> X get(String property) {
    	if ("lastEventOnFormatted".equals(property)) {
    		return (X) (DateUtils.formatDateTime((Date) get("lastEventOn")));
    	}
    	else if ("uptimeFormatted".equals(property)) {
    	    if (getUptime() == -1) {
    	        return (X) "Unknown";
    	    }
    	    else {
    	        return (X) String.valueOf(getUptime());
    	    }
        }
    	else {
    		return super.get(property);
    	}
    }
    
    public String getAccountName() {
        return get("accountName");
    }

    public void setAccountName(String accountName) {
        set("accountName", accountName);
    }
    
    public String getClientId() {
        return (String) get("clientId");
    }


    public void setClientId(String clientId) {
        set("clientId", clientId);
    }
	
    public Long getUptime() {
        return (Long) get("uptime");
    }

    public String getUptimeFormatted() {
        return (String) get("uptimeFormatted");
    }

    public void setUptime(Long uptime) {
        set("uptime", uptime);
    }
    
    public String getGwtDeviceStatus() {
    	return (String) get("gwtDeviceStatus");
	}

	public void setGwtDeviceStatus(String gwtDeviceStatus) {
		set("gwtDeviceStatus", gwtDeviceStatus);
	}    
    
    public String getDisplayName() {
        return (String) get("displayName");
    }

    public void setDisplayName(String displayName) {
        set("displayName", displayName);
    }
    
    public String getModelName() {
        return (String) get("modelName");
    }

    public void setModelName(String modelName) {
        set("modelName", modelName);
    }
    
    public String getModelId() {
        return (String) get("modelId");
    }

    public void setModelId(String modelId) {
        set("modelId", modelId);
    }
    
    public String getPartNumber() {
        return (String) get("partNumber");
    }

    public void setPartNumber(String partNumber) {
        set("partNumber", partNumber);
    }    
    
    public String getSerialNumber() {
        return (String) get("serialNumber");
    }

    public void setSerialNumber(String serialNumber) {
        set("serialNumber", serialNumber);
    }
    
    public String getAvailableProcessors() {
        return (String) get("availableProcessors");
    }

    public void setAvailableProcessors(String availableProcessors) {
        set("availableProcessors", availableProcessors);
    }
    
    public String getTotalMemory() {
        return (String) get("totalMemory");
    }

    public void setTotalMemory(String totalMemory) {
        set("totalMemory", totalMemory);
    }
   
    public String getFirmwareVersion() {
        return (String) get("firmwareVersion");
    }

    public void setFirmwareVersion(String firmwareVersion) {
        set("firmwareVersion", firmwareVersion);
    }    
    
    public String getBiosVersion() {
        return (String) get("biosVersion");
    }

    public void setBiosVersion(String biosVersion) {
        set("biosVersion", biosVersion);
    }    
    
    public String getOs() {
        return (String) get("os");
    }

    public void setOs(String os) {
        set("os", os);
    }    
    
    public String getOsVersion() {
        return (String) get("osVersion");
    }

    public void setOsVersion(String osVersion) {
        set("osVersion", osVersion);
    }    
    
    public String getOsArch() {
        return (String) get("osArch");
    }

    public void setOsArch(String osArch) {
        set("osArch", osArch);
    }    
    
    public String getJvmName() {
        return (String) get("jvmName");
    }

    public void setJvmName(String jvmName) {
        set("jvmName", jvmName);
    }    
    
    public String getJvmVersion() {
        return (String) get("jvmVersion");
    }

    public void setJvmVersion(String jvmVersion) {
        set("jvmVersion", jvmVersion);
    }
    
    public String getJvmProfile() {
        return (String) get("jvmProfile");
    }

    public void setJvmProfile(String jvmProfile) {
        set("jvmProfile", jvmProfile);
    }
    
    public String getOsgiFramework() {
        return (String) get("osgiFramework");
    }

    public void setOsgiFramework(String osgiFramework) {
        set("osgiFramework", osgiFramework);
    }
    
    public String getOsgiFrameworkVersion() {
        return (String) get("osgiFrameworkVersion");
    }

    public void setOsgiFrameworkVersion(String osgiFrameworkVersion) {
        set("osgiFrameworkVersion", osgiFrameworkVersion);
    }
    
    public String getConnectionInterface() {
        return (String) get("connectionInterface");
    }

    public void setConnectionInterface(String connectionInterface) {
        set("connectionInterface", connectionInterface);
    }
    
    public String getConnectionIp() {
        return (String) get("connectionIp");
    }

    public void setConnectionIp(String connectionIp) {
        set("connectionIp", connectionIp);
    }
    
    public String getAcceptEncoding() {
    	return (String) get("acceptEncoding");
    }
    
    public void setAcceptEncoding(String acceptEncoding) {
    	set("acceptEncoding", acceptEncoding);
    }

    public String getApplicationIdentifiers() {
        return (String) get("applicationIdentifiers");
    }

    public void setApplicationIdentifiers(String applicationIdentifiers) {
        set("applicationIdentifiers", applicationIdentifiers);
    }
    
    public Double getGpsLatitude() {
        return (Double) get("gpsLatitude");
    }

    public void setGpsLatitude(Double gpsLatitude) {
        set("gpsLatitude", gpsLatitude);
    }

    public Double getGpsLongitude() {
        return (Double) get("gpsLongitude");
    }

    public void setGpsLongitude(Double gpsLongitude) {
        set("gpsLongitude", gpsLongitude);
    }

    public Double getGpsAltitude() {
        return (Double) get("gpsAltitude");
    }

    public void setGpsAltitude(Double gpsAltitude) {
        set("gpsAltitude", gpsAltitude);
    }    
    
    public String getGpsAddress() {
        return (String) get("gpsAddress");
    }

    public void setGpsAddress(String gpsAddress) {
        set("gpsAddress", gpsAddress);
    }

    public Date getLastEventOn() {
        return (Date) get("lastEventOn");
    }

    public String getLastEventOnFormatted() {
        return (String) get("lastEventOnFormatted");
    }

    public void setLastEventOn(Date lastEventDate) {
        set("lastEventOn", lastEventDate);
    }    
    
    public String getLastEventType() {
        return (String) get("lastEventType");
    }

    public void setLastEventType(String lastEventType) {
        set("lastEventType", lastEventType);
    }
    
    public boolean isOnline() {
    	return getGwtDeviceStatus().compareTo("CONNECTED") == 0;
    }
}
