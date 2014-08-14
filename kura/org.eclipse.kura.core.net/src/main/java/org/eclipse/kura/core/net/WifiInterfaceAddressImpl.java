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
package org.eclipse.kura.core.net;

import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiMode;

public class WifiInterfaceAddressImpl extends NetInterfaceAddressImpl implements WifiInterfaceAddress 
{
	private WifiMode        m_mode;
	private long            m_bitrate;
	private WifiAccessPoint m_wifiAccessPoint;
	
	public WifiInterfaceAddressImpl()
	{
		super();
	}
	
	public WifiInterfaceAddressImpl(WifiInterfaceAddress other) {
	    super(other);
	    this.m_mode = other.getMode();
	    this.m_bitrate = other.getBitrate();
	    this.m_wifiAccessPoint = other.getWifiAccessPoint();
	}

	public WifiMode getMode() {
		return m_mode;
	}

	public void setMode(WifiMode mode) {
		this.m_mode = mode;
	}

	public long getBitrate() {
		return m_bitrate;
	}

	public void setBitrate(long bitrate) {
		this.m_bitrate = bitrate;
	}

	public WifiAccessPoint getWifiAccessPoint() {
		return m_wifiAccessPoint;
	}

	public void setWifiAccessPoint(WifiAccessPoint wifiAccessPoint) {
		this.m_wifiAccessPoint = wifiAccessPoint;
	}
	
	@Override
	public boolean equals(Object obj) {
        if(!super.equals(obj)) {
            return false;
        }
        
        if(!(obj instanceof WifiInterfaceAddress)) {
            return false;
        }
        
        WifiInterfaceAddress other = (WifiInterfaceAddress) obj;
        
        if(!compare(m_mode, other.getMode())) {
            return false;
        }
        if(!compare(m_bitrate, other.getBitrate())) {
            return false;
        }
        if(!compare(m_wifiAccessPoint, other.getWifiAccessPoint())) {
            return false;
        }
	    
	    return true;
	}
}
