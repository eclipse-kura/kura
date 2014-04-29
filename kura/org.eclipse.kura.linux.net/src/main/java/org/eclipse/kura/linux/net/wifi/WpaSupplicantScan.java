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
package org.eclipse.kura.linux.net.wifi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.linux.util.ProcessStats;
import org.eclipse.kura.net.wifi.WifiHotspotInfo;
import org.eclipse.kura.net.wifi.WifiSecurity;

public class WpaSupplicantScan {
	
	private String m_iface = null;
	private List <WifiHotspotInfo> m_listWifiHotspotInfo = null;
	
	public WpaSupplicantScan (String iface) {
		m_iface = iface;
		m_listWifiHotspotInfo = new ArrayList<WifiHotspotInfo>();
	}

	public void scan() throws KuraException {

		String line = null;
		ProcessStats processStats = null;
		BufferedReader br = null;
		String sScanCommand = formSupplicantScanCommand(m_iface);
		
		// scan for wireless networks
		try { 	
			processStats = LinuxProcessUtil.startWithStats(sScanCommand);
		} catch (Exception e) {
			throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
		}
		try {
			br = new BufferedReader(new InputStreamReader(processStats.getInputStream()));
	    	line = br.readLine();
	    	if ((line == null) || !line.equals("OK")) {
	    		throw new KuraException (KuraErrorCode.INTERNAL_ERROR, sScanCommand + " command failed");
	    	}
		} catch (Exception e) {
			throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
		
		// get scan results
		String sScanResultsCommand = formSupplicantScanResultsCommand(m_iface);
		try { 	
			processStats = LinuxProcessUtil.startWithStats(sScanResultsCommand);
		} catch (Exception e) {
			throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
		}
		try {
			br = new BufferedReader(new InputStreamReader(processStats.getInputStream()));
	    	String [] aScanInfo = null; 
			while ((line = br.readLine()) != null) {
				aScanInfo = line.split("\\s+");
				if (aScanInfo.length > 0) {
					String macAddress = aScanInfo[0];
					int frequency = Integer.parseInt(aScanInfo[1]);
					int signalLevel = Integer.parseInt(aScanInfo[2]);
					
					int securityCode = 0;
					String sSecurity = aScanInfo[3].substring(aScanInfo[3].indexOf("[")+1, aScanInfo[3].lastIndexOf(']'));
					StringTokenizer st = new StringTokenizer(sSecurity, "][");
					while (st.hasMoreTokens()) {
						String token = st.nextToken();
						if (token.startsWith("WEP")) {
							securityCode |= 1;
						} else if (token.startsWith("WPA2")) {
							securityCode |= 4;
						} else if (token.startsWith("WPA")) {
							securityCode |= 2;
						}
					}
					WifiSecurity wifiSecurity = null;
					switch (securityCode) {
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
					default:
						wifiSecurity = WifiSecurity.NONE;
					}
					
					String ssid = aScanInfo[4];
					
					WifiHotspotInfo wifiHotspotInfo = new WifiHotspotInfo(ssid,
							macAddress, signalLevel,
							frequencyMhz2Channel(frequency), frequency,
							wifiSecurity);
					m_listWifiHotspotInfo.add(wifiHotspotInfo);
				}
	    	}
		} catch (Exception e) {
			throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
	}
	
	public List <WifiHotspotInfo> getWifiHotspotInfo() {
		return m_listWifiHotspotInfo;
	}
	
	private static String formSupplicantScanCommand (String iface) {
		
		StringBuffer sb = new StringBuffer ();
		sb.append("wpa_cli -i ");
		sb.append(iface);
		sb.append(" scan");
		return sb.toString();
	}
	
	private static String formSupplicantScanResultsCommand (String iface) {
		
		StringBuffer sb = new StringBuffer ();
		sb.append("wpa_cli -i ");
		sb.append(iface);
		sb.append(" scan_results");
		return sb.toString();
	}
	
	private static int frequencyMhz2Channel(int frequency) {
		int channel = (frequency - 2407)/5;
		return channel;
	}
}
