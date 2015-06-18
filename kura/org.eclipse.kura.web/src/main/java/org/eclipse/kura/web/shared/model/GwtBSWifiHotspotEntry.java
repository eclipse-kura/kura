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

public class GwtBSWifiHotspotEntry extends GwtBSBaseModel implements Serializable {
	
	private static final long serialVersionUID = -7818472380334612955L;

	public String getSSID() {
        return get("ssid");
    }

    public void setSSID(String ssid) {
        set("ssid", ssid);
    }
    
    public String getMacAddress() {
    	return get("macAddress");
    }
    
    public void setMacAddress(String macAddress) {
    	set("macAddress", macAddress);
    }
    
    public int getSignalStrength() {
    	if (get("signalStrength") != null) {
    		return ((Integer) get("signalStrength"));
    	}
    	else {
    		return 0;
    	}
    }
    
    public void setsignalStrength(int signalStrength) {
    	set("signalStrength", String.valueOf(signalStrength));
    }
    
    public int getChannel() {
    	if (get("channel") != null) {
    		return ((Integer) get("channel")).intValue();
    	}
    	else {
    		return 0;
    	}
    }

    public void setChannel(int channel) {
        set("channel", String.valueOf(channel));
    }
    
    public int getFrequency() {
    	if (get("frequency") != null) {
    		return ((Integer) get("frequency")).intValue();
    	}
    	else {
    		return 0;
    	}
    }

    public void setFrequency(int frequency) {
        set("frequency", String.valueOf(frequency));
    }
    
    
    public String getSecurity() {
    	return get("security");
    }
    
    public void setSecurity(String security) {
    	set("security", security);
    }

}
