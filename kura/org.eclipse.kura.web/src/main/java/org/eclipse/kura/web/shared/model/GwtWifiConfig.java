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
import java.util.ArrayList;

import com.extjs.gxt.ui.client.data.BaseModelData;


public class GwtWifiConfig extends BaseModelData implements Serializable {

	private static final long serialVersionUID = -7610506986073264800L;
	
	public GwtWifiConfig() {
		setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeStation.name());
		setRadioMode(GwtWifiRadioMode.netWifiRadioModeBGN.name());
		//setChannel(GwtWifiChannel.netWifiChannelAuto.name());
		setSecurity(GwtWifiSecurity.netWifiSecurityWPA2.name());
	}

    public String getWirelessMode() {
        return get("wirelessMode");
    }

    public void setWirelessMode(String wirelessMode) {
        set("wirelessMode", wirelessMode);
    }

    public GwtWifiWirelessMode getWirelessModeEnum() {
        return GwtWifiWirelessMode.valueOf(getWirelessMode());
    }
    
    public String getWirelessSsid() {
        return get("wirelessSsid");
    }

    public void setWirelessSsid(String wirelessSsid) {
        set("wirelessSsid", wirelessSsid);
    }
	
    public String getDriver() {
        return get("driver");
    }

    public void setDriver(String driver) {
        set("driver", driver);
    }
    
    public String getRadioMode() {
        return get("radioMode");
    }

    public void setRadioMode(String radioMode) {
        set("radioMode", radioMode);
    }

    public GwtWifiRadioMode getRadioModeEnum() {
        return GwtWifiRadioMode.valueOf(getRadioMode());
    }

    public ArrayList<Integer> getChannels () {
    	return get("channels");
    }
    
    public void setChannels(ArrayList<Integer> channels) {
    	set("channels", channels);
    }    
    
    public String getSecurity() {
        return get("security");
    }

    public void setSecurity(String security) {
        set("security", security);
    }
    
    public GwtWifiSecurity getSecurityEnum() {
        return GwtWifiSecurity.valueOf(getSecurity());
    }
    
    public String getPairwiseCiphers() {
    	return get("pairwiseCiphers");
    }
    
    public void setPairwiseCiphers(String ciphers) {
    	set("pairwiseCiphers", ciphers);
    }
    
    public GwtWifiCiphers getPairwiseCiphersEnum () {
    	return GwtWifiCiphers.valueOf(getPairwiseCiphers());
    }
    
    public String getGroupCiphers() {
    	return get("groupCiphers");
    }
    
    public void setGroupCiphers(String ciphers) {
    	set("groupCiphers", ciphers);
    }
    
    public GwtWifiCiphers getGroupCiphersEnum () {
    	return GwtWifiCiphers.valueOf(getGroupCiphers());
    }

    public String getPassword() {
        return get("password");
    }

    public void setPassword(String password) {
        set("password", password);
    }
    
    public String getBgscanModule () {
    	return get("bgscanModule");
    }
    
    public void setBgscanModule(String bgscanModule) {
    	set("bgscanModule", bgscanModule);
    }
    
    public GwtWifiBgscanModule getBgscanModuleEnum () {
    	return GwtWifiBgscanModule.valueOf(getBgscanModule());
    }
    
    public int getBgscanRssiThreshold () {
    	if (get("bgscanRssiThreshold") != null) {
    		return (Integer)get("bgscanRssiThreshold");
    	}
    	return 0;
    }
    
    public void setBgscanRssiThreshold (int bgscanRssiThreshold) {
    	set ("bgscanRssiThreshold", bgscanRssiThreshold);
    }
    
    public int getBgscanShortInterval () {
    	if (get("bgscanShortInterval") != null) {
    		return (Integer)get("bgscanShortInterval");
    	}
    	return 0;
    }
    
    public void setBgscanShortInterval (int bgscanShortInterval) {
    	set("bgscanShortInterval", bgscanShortInterval);
    }
    
    public int getBgscanLongInterval () {
    	
    	if (get("bgscanLongInterval") != null) {
    		return (Integer)get("bgscanLongInterval");
    	}
    	return 0;
    }
    
    public void setBgscanLongInterval (int bgscanLongInterval) {
    	set("bgscanLongInterval", bgscanLongInterval);
    } 
    
    public boolean pingAccessPoint() {
    	if (get("pingAccessPoint") != null) {
    		return (Boolean) get("pingAccessPoint");
    	}
    	return false;
    }

    public void setPingAccessPoint(boolean pingAccessPoint) {
        set("pingAccessPoint", pingAccessPoint);
    }
    
    public boolean ignoreSSID() {
    	if (get("ignoreSSID") != null) {
    		return (Boolean) get("ignoreSSID");
    	}
    	return false;
    }
    
    public void setIgnoreSSID(boolean ignoreSSID) {
    	set("ignoreSSID", ignoreSSID);
    }
}
