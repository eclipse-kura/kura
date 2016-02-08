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
package org.eclipse.kura.linux.net.wifi;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.net.wifi.WifiHotspotInfo;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NL80211 {
	
	private final static String LABEL = NL80211.class.getName() + ": ";
	private static Logger s_logger = LoggerFactory.getLogger(NL80211.class);
	
	private static NL80211 s_nl80211;
	
	private String m_ifaceName;
	
	static {
		try {
			System.loadLibrary("nl80211");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private native boolean NL80211initialize(); 
	private native void NL80211cleanup();
	private native boolean NL80211setMode(String ifaceName, int mode);
	private native boolean NL80211triggerScan(String ifaceName);
	private native boolean NL80211triggerScanForSsid(String ifaceNa, String ssid);
	private native String[] NL80211getScanResults(String ifaceName);
	private native String NL80211getSSID(String bssid);
	private native int NL80211getChannel(String bssid);
	private native int NL80211getFrequency(String bssid);
	private native int NL80211getSignal(String bssid);
	private native int NL80211getSecurity(String bssid);
	
	private NL80211(String ifaceName) {
		m_ifaceName = ifaceName;
		initialize();
	}
	
	public static NL80211 getInstance(String ifaceName) {
		
		if (s_nl80211 == null) {
			s_nl80211 = new NL80211(ifaceName);
		}
		
		return s_nl80211;
	}
	
	public synchronized boolean initialize () {
		return NL80211initialize();
		
	}
	
	public synchronized void release() {
		NL80211cleanup();
		s_nl80211 = null;
	}
	
	public synchronized boolean setMode(WifiMode wifiMode) {
		
		boolean status = NL80211setMode(m_ifaceName, WifiMode.getCode(wifiMode));
		return status;
	}
	
	public synchronized boolean setMode(WifiMode wifiMode, int sleep) {
		
		boolean status = NL80211setMode(m_ifaceName, WifiMode.getCode(wifiMode));
		if (sleep > 0) {
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
			}
		}
		return status;
	}
	
	public synchronized boolean triggerScan() {
		
		boolean scanStatus = NL80211triggerScan(m_ifaceName);
		return scanStatus;
	}
	
	public synchronized boolean triggerScan(String ssid) {
		
		boolean scanStatus = NL80211triggerScanForSsid(m_ifaceName, ssid);
		return scanStatus;
	}
	
	public synchronized Map<String, WifiHotspotInfo> getScanResults() {
		
		Map<String, WifiHotspotInfo> scanResults = null;
		String[] BSSIDs = NL80211getScanResults(m_ifaceName);
		if ((BSSIDs != null) && (BSSIDs.length > 0)) {
			scanResults = new HashMap<String, WifiHotspotInfo>();
			for (String bssid : BSSIDs) {
				String ssid = NL80211getSSID(bssid);
				int channel = NL80211getChannel(bssid);
				int frequency = NL80211getFrequency(bssid);
				int signal = NL80211getSignal(bssid);
				int security = NL80211getSecurity(bssid);
				WifiSecurity wifiSecurity = WifiSecurity.NONE;
				switch (security) {
					case 0:
						wifiSecurity = WifiSecurity.NONE;
						break;
					case 1:
						wifiSecurity = WifiSecurity.SECURITY_WEP;
						break;
					case 2:
						wifiSecurity = WifiSecurity.SECURITY_WPA;
						break;
					case 4:
						wifiSecurity = WifiSecurity.SECURITY_WPA2;
						break;
					case 6:
						wifiSecurity = WifiSecurity.SECURITY_WPA_WPA2;
						break;
				}
				
				WifiHotspotInfo wifiHotspotInfo = new WifiHotspotInfo(ssid, bssid, signal, channel, frequency, wifiSecurity);
				scanResults.put(bssid, wifiHotspotInfo);
			}
			
		}
		return scanResults;
	}	
	
	public Map<String, WifiHotspotInfo> getScanResults(int noRetries, int delay) {
	
		Map<String, WifiHotspotInfo> scanResults = null;
		while ((noRetries > 0) && ((scanResults == null) || (scanResults.isEmpty()))) {
			try {
				Thread.sleep(delay*1000);
			} catch (InterruptedException e) {
			}
			
			scanResults = getScanResults();	
			noRetries--;
		}
		return scanResults;
	}
}
