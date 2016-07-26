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
package org.eclipse.kura.net.wifi;

import java.util.EnumSet;

public class WifiHotspotInfo {

	private String m_ssid;
	private String m_macAddress;
	private int m_signalLevel;
	private int m_channel;
	private int m_frequency;
	private WifiSecurity m_security;
	private EnumSet<WifiSecurity> m_pairCiphers;
	private EnumSet<WifiSecurity> m_groupCiphers;
	
	public WifiHotspotInfo(String ssid, String macAddress,
			int signalLevel, int channel, int frequency, WifiSecurity security) {
		super();
		m_ssid = ssid;
		m_macAddress = macAddress;
		m_signalLevel = signalLevel;
		m_channel = channel;
		m_frequency = frequency;
		m_security = security;
	}
	
	public WifiHotspotInfo(String ssid, String macAddress,
			int signalLevel, int channel, int frequency, WifiSecurity security,
			EnumSet<WifiSecurity> pairCiphers, EnumSet<WifiSecurity> groupCiphers) {
		this(ssid, macAddress, signalLevel, channel, frequency, security);
		m_pairCiphers = pairCiphers;
		m_groupCiphers = groupCiphers;
	}

	public String getSsid() {
		return m_ssid;
	}

	public String getMacAddress() {
		return m_macAddress;
	}

	public int getSignalLevel() {
		return m_signalLevel;
	}

	public int getChannel() {
		return m_channel;
	}

	public int getFrequency() {
		return m_frequency;
	}

	public WifiSecurity getSecurity() {
		return m_security;
	}
	
	public EnumSet<WifiSecurity> getPairCiphers() {
		return m_pairCiphers;
	}
	
	public EnumSet<WifiSecurity> getGroupCiphers() {
		return m_groupCiphers;
	}
	
	public String toString() {
		
		StringBuffer sb = new StringBuffer();
		sb.append(m_macAddress);
		sb.append(" :: ");
		sb.append(m_ssid);
		sb.append(" :: ");
		sb.append(m_signalLevel);
		sb.append(" :: ");
		sb.append(m_channel);
		sb.append(" :: ");
		sb.append(m_frequency);
		sb.append(" :: ");
		sb.append(m_security);
		
		return sb.toString();
	}
}
