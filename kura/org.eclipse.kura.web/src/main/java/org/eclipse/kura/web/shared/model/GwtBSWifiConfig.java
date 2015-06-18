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



public class GwtBSWifiConfig extends GwtBSBaseModel implements Serializable {

	private static final long serialVersionUID = -7610506986073264800L;
	
	public GwtBSWifiConfig() {
		setWirelessMode(GwtBSWifiWirelessMode.netWifiWirelessModeStation.name());
		setRadioMode(GwtBSWifiRadioMode.netWifiRadioModeBGN.name());
		//setChannel(GwtBSWifiChannel.netWifiChannelAuto.name());
		setSecurity(GwtBSWifiSecurity.netWifiSecurityWPA2.name());
	}

    public String getWirelessMode() {
        return get("wirelessMode");
    }

    public void setWirelessMode(String wirelessMode) {
        set("wirelessMode", wirelessMode);
    }

    public GwtBSWifiWirelessMode getWirelessModeEnum() {
        return GwtBSWifiWirelessMode.valueOf(getWirelessMode());
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

    public GwtBSWifiRadioMode getRadioModeEnum() {
        return GwtBSWifiRadioMode.valueOf(getRadioMode());
    }

    public ArrayList<Integer> getChannels () {
    	return convertInts((int[])get("channels"));
    }
    
    public void setChannels(ArrayList<Integer> channels) {
    	set("channels", convertIntegers(channels));
    }    
    
    public  int[] convertIntegers(ArrayList<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }
    
    public ArrayList<Integer> convertInts(int[] ints){
    	ArrayList<Integer> ret = new ArrayList<Integer>();
    	for(int i=0;i< ints.length;i++){
    		ret.add(ints[i]);
    	}
    	return ret;
    }
    
    public String getSecurity() {
        return get("security");
    }

    public void setSecurity(String security) {
        set("security", security);
    }
    
    public GwtBSWifiSecurity getSecurityEnum() {
        return GwtBSWifiSecurity.valueOf(getSecurity());
    }
    
    public String getPairwiseCiphers() {
    	return get("pairwiseCiphers");
    }
    
    public void setPairwiseCiphers(String ciphers) {
    	set("pairwiseCiphers", ciphers);
    }
    
    public GwtBSWifiCiphers getPairwiseCiphersEnum () {
    	return GwtBSWifiCiphers.valueOf(getPairwiseCiphers());
    }
    
    public String getGroupCiphers() {
    	return get("groupCiphers");
    }
    
    public void setGroupCiphers(String ciphers) {
    	set("groupCiphers", ciphers);
    }
    
    public GwtBSWifiCiphers getGroupCiphersEnum () {
    	return GwtBSWifiCiphers.valueOf(getGroupCiphers());
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
    
    public GwtBSWifiBgscanModule getBgscanModuleEnum () {
    	return GwtBSWifiBgscanModule.valueOf(getBgscanModule());
    }
    
    public int getBgscanRssiThreshold () {
    	if (get("bgscanRssiThreshold") != null) {
    		return ((Integer)get("bgscanRssiThreshold")).intValue();
    	}
    	return 0;
    }
    
    public void setBgscanRssiThreshold (int bgscanRssiThreshold) {
    	set ("bgscanRssiThreshold", String.valueOf(bgscanRssiThreshold));
    }
    
    public int getBgscanShortInterval () {
    	if (get("bgscanShortInterval") != null) {
    		return ((Integer)get("bgscanShortInterval")).intValue();
    	}
    	return 0;
    }
    
    public void setBgscanShortInterval (int bgscanShortInterval) {
    	set("bgscanShortInterval", String.valueOf(bgscanShortInterval));
    }
    
    public int getBgscanLongInterval () {
    	
    	if (get("bgscanLongInterval") != null) {
    		return ((Integer)get("bgscanLongInterval")).intValue();
    	}
    	return 0;
    }
    
    public void setBgscanLongInterval (int bgscanLongInterval) {
    	set("bgscanLongInterval", String.valueOf(bgscanLongInterval));
    } 
    
    public boolean pingAccessPoint() {
    	if (get("pingAccessPoint") != null) {
    		return (Boolean) get("pingAccessPoint");
    	}
    	return false;
    }

    public void setPingAccessPoint(boolean pingAccessPoint) {
        set("pingAccessPoint", String.valueOf(pingAccessPoint));
    }
    
    public boolean ignoreSSID() {
    	if (get("ignoreSSID") != null) {
    		return (Boolean) get("ignoreSSID");
    	}
    	return false;
    }
    
    public void setIgnoreSSID(boolean ignoreSSID) {
    	set("ignoreSSID", String.valueOf(ignoreSSID));
    }
}
