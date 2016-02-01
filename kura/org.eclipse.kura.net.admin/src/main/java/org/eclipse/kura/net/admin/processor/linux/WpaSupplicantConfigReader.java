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
package org.eclipse.kura.net.admin.processor.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationReader;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.processor.linux.util.KuranetConfig;
import org.eclipse.kura.net.admin.processor.linux.util.WpaSupplicantUtil;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WpaSupplicantConfigReader implements NetworkConfigurationReader {

    private static final Logger s_logger = LoggerFactory.getLogger(WpaSupplicantConfigReader.class);
    
    private static String WPA_CONFIG_FILE = null;
    
    private static final String OS_VERSION = System.getProperty("kura.os.version");
    
    static {
		if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			WPA_CONFIG_FILE = "/etc/wpa_supplicant/wpa_supplicant.conf";
		} else {
			WPA_CONFIG_FILE = "/etc/wpa_supplicant.conf";
		}
	}

    private static WpaSupplicantConfigReader s_instance;
    
    public static WpaSupplicantConfigReader getInstance() {
        if(s_instance == null) {
            s_instance = new WpaSupplicantConfigReader();
        }
        
        return s_instance;
    }
    
    @Override
    public void read(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getNetInterfaceConfigs();
        
        for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if(netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                getConfig((WifiInterfaceConfigImpl)netInterfaceConfig);
            }
        }
    }
    
    private void getConfig(WifiInterfaceConfigImpl wifiInterfaceConfig) throws KuraException {
        String interfaceName = wifiInterfaceConfig.getName();
        s_logger.debug("Getting wpa_supplicant config for " + interfaceName);
        
        List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = wifiInterfaceConfig.getNetInterfaceAddresses();
        
        if(wifiInterfaceAddressConfigs == null || wifiInterfaceAddressConfigs.size() == 0) { 
        	wifiInterfaceAddressConfigs = new ArrayList<WifiInterfaceAddressConfig>();
        	wifiInterfaceAddressConfigs.add(new WifiInterfaceAddressConfigImpl());
        	wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);
        }
        
        for(WifiInterfaceAddressConfig wifiInterfaceAddressConfig : wifiInterfaceAddressConfigs) {
            if(wifiInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
                List<NetConfig> netConfigs = wifiInterfaceAddressConfig.getConfigs();
                
                if(netConfigs == null) {
                    netConfigs = new ArrayList<NetConfig>();
                    ((WifiInterfaceAddressConfigImpl)wifiInterfaceAddressConfig).setNetConfigs(netConfigs);
                }
                
                // Get infrastructure config
            	netConfigs.add(getWifiClientConfig(interfaceName, WifiMode.INFRA));
                
		/*
                // Get adhoc config
                WifiConfig adhocConfig = new WifiConfig();                
                setWifiClientConfig(adhocConfig, interfaceName, WifiMode.ADHOC);                
                netConfigs.add(adhocConfig);
		*/
            }
        }
    }
    
    private static WifiConfig getWifiClientConfig(String ifaceName, WifiMode wifiMode) throws KuraException {
        
    	WifiConfig wifiConfig = new WifiConfig();
        
        String ssid = "";
        WifiSecurity wifiSecurity = WifiSecurity.NONE;
        String password = "";
        int [] channels = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 };
        WifiBgscan bgscan = new WifiBgscan("");
        WifiCiphers pairwiseCiphers = null;
        WifiCiphers groupCiphers = null;

    	// Get properties from config file
        Properties props = parseConfigFile();
        
        if (props == null) {
            s_logger.warn("WPA in client mode is not configured");
        } else {
	        ssid = props.getProperty("ssid");
	        if(ssid == null) {
	            s_logger.warn("WPA in client mode is not configured");
	        } else {
		        s_logger.debug("curent wpa_supplicant.conf: ssid=" + ssid);
		        
		        // wifi mode
		        int currentMode = (props.getProperty("mode") != null) ? Integer.parseInt(props.getProperty("mode")) : WpaSupplicantUtil.MODE_INFRA;
		        s_logger.debug("current wpa_supplicant.conf: mode=" + currentMode);
		
		        switch (wifiMode) {
		        case INFRA:
		            String scan_freq = props.getProperty("scan_freq");
		            if (scan_freq != null) {
		                s_logger.debug("current wpa_supplicant.conf: scan_freq=" + scan_freq);
		                String [] saScanFreq = scan_freq.split(" ");
		                channels = new int [saScanFreq.length];
		                for (int i = 0; i < channels.length; i++) {
		                    try {
		                        channels[i] = WpaSupplicantUtil.convFrequencyToChannel(Integer.parseInt(saScanFreq[i]));
		                    } catch (NumberFormatException e) {
		                    }
		                }
		            }
		            break;
			/*
		        case ADHOC:
		            channels = new int [1];
		            String frequency = props.getProperty("frequency");
		            s_logger.debug("current wpa_supplicant.conf: frequency=" + frequency);
		            int freq = 2412;
		            if (frequency != null) { 
		                try {
		                    freq = Integer.parseInt(frequency);
		                } catch (NumberFormatException e) {
		                }
		            }
		            channels[0] = WpaSupplicantUtil.convFrequencyToChannel(freq);
		            wifiDriver = KuranetConfig.getProperty(adhocDriverKey.toString());
		            if(wifiDriver == null) {
		                wifiDriver = KuranetConfig.getProperty(infraDriverKey.toString());
		            }
		            break;
			*/
		        case MASTER:
		            throw KuraException
		                    .internalError("failed to get wpa_supplicant configuration: MASTER mode is invalid");
		        default:
		            throw KuraException
		                .internalError("failed to get wpa_supplicant configuration: invalid mode: " + wifiMode);
		        }
		        
		        String proto = props.getProperty("proto");
		        if (proto != null) {
		            s_logger.debug("current wpa_supplicant.conf: proto=" + proto);
		        }
		        
		        String pairwise = props.getProperty("pairwise");
		        if (pairwise != null) {
		            s_logger.debug("current wpa_supplicant.conf: pairwise=" + pairwise);
		            if(pairwise.contains(WifiCiphers.toString(WifiCiphers.CCMP_TKIP))) {
		                pairwiseCiphers = WifiCiphers.CCMP_TKIP;
		            } else if(pairwise.contains(WifiCiphers.toString(WifiCiphers.TKIP))) {
		                pairwiseCiphers = WifiCiphers.TKIP;
		            } else if(pairwise.contains(WifiCiphers.toString(WifiCiphers.CCMP))) {
		                pairwiseCiphers = WifiCiphers.CCMP;
		            } 
		        }
		        
		        String group = props.getProperty("group");
		        if (group != null) {
		            s_logger.debug("current wpa_supplicant.conf: group=" + group);
		            if(group.contains(WifiCiphers.toString(WifiCiphers.CCMP_TKIP))) {
		                groupCiphers = WifiCiphers.CCMP_TKIP;
		            } else if(group.contains(WifiCiphers.toString(WifiCiphers.TKIP))) {
		                groupCiphers = WifiCiphers.TKIP;
		            } else if(group.contains(WifiCiphers.toString(WifiCiphers.CCMP))) {
		                groupCiphers = WifiCiphers.CCMP;
		            } 
		        }
		        
		        // security
		        String keyMgmt = props.getProperty("key_mgmt");
		        s_logger.debug("current wpa_supplicant.conf: key_mgmt=" + keyMgmt);
		        if (keyMgmt != null && keyMgmt.equalsIgnoreCase("WPA-PSK")) {
		            password = props.getProperty("psk");
		            if (proto != null) {
						if(proto.trim().equals("WPA")) {
							wifiSecurity = WifiSecurity.SECURITY_WPA;
						} else if(proto.trim().equals("RSN")) {
							wifiSecurity = WifiSecurity.SECURITY_WPA2;
						} else if(proto.trim().equals("WPA RSN")) {
							wifiSecurity = WifiSecurity.SECURITY_WPA_WPA2;
						}
					} else {
						wifiSecurity = WifiSecurity.SECURITY_WPA2;
					}
		        } else {
		            password = props.getProperty("wep_key0");
		            if (password != null) {
		                wifiSecurity = WifiSecurity.SECURITY_WEP;
		            } else {
		                wifiSecurity = WifiSecurity.SECURITY_NONE;
		            }
		            pairwiseCiphers = null;
		            groupCiphers = null;
		        }
		        if(password == null) {
		        	password = "";
		        }
		        
		        String sBgscan = props.getProperty("bgscan");
		        if (sBgscan != null) {
		            s_logger.debug("current wpa_supplicant.conf: bgscan=" + sBgscan);
		            bgscan = new WifiBgscan(sBgscan);
		        }
	        }
        }
         
        // Populate the config
        wifiConfig.setMode(wifiMode);
        wifiConfig.setSSID(ssid);
        wifiConfig.setSecurity(wifiSecurity);
        wifiConfig.setPasskey(password);
        wifiConfig.setHardwareMode("");
        wifiConfig.setPairwiseCiphers(pairwiseCiphers);
        wifiConfig.setGroupCiphers(groupCiphers);
        wifiConfig.setChannels(channels);
        wifiConfig.setBgscan(bgscan);
        
        // Get self-stored properties
        
		boolean pingAP = false;
        StringBuilder key = new StringBuilder().append("net.interface.").append(ifaceName).append(".config.wifi.infra.pingAccessPoint");
        String statusString = KuranetConfig.getProperty(key.toString());
        if(statusString != null && !statusString.isEmpty()) {
        	pingAP = Boolean.parseBoolean(statusString);
        }
		wifiConfig.setPingAccessPoint(pingAP);
		
		boolean ignoreSSID = false;
		key = new StringBuilder().append("net.interface.").append(ifaceName).append(".config.wifi.infra.ignoreSSID");
        statusString = KuranetConfig.getProperty(key.toString());
        if(statusString != null && !statusString.isEmpty()) {
        	ignoreSSID = Boolean.parseBoolean(statusString);
        }
        wifiConfig.setIgnoreSSID(ignoreSSID);
        
        StringBuilder infraDriverKey = new StringBuilder("net.interface.").append(ifaceName).append(".config.wifi.infra.driver");
        //StringBuilder adhocDriverKey = new StringBuilder("net.interface.").append(ifaceName).append(".config.wifi.adhoc.driver");
        String wifiDriver = KuranetConfig.getProperty(infraDriverKey.toString());
        if(wifiDriver == null || wifiDriver.isEmpty()) {
        	wifiDriver = "nl80211";
        }
        wifiConfig.setDriver(wifiDriver);

        return wifiConfig;
    }
    
    private static Properties parseConfigFile () throws KuraException {
        
        Properties props = null;
        
        BufferedReader br = null;
        try {
            File wpaConfigFile = new File(WPA_CONFIG_FILE);
            if (wpaConfigFile.exists()) {
    
                // Read into a string
                br = new BufferedReader(new FileReader(wpaConfigFile));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
    
                String newConfig = null;
                int beginIndex = sb.toString().indexOf("network");
                int endIndex = sb.toString().indexOf('}');
                if ((beginIndex >= 0) && (endIndex > beginIndex)) {
                    newConfig = sb.toString().substring(beginIndex, endIndex);
                    beginIndex = newConfig.indexOf('{');
                    if (beginIndex >= 0) {
                        newConfig = newConfig.substring(beginIndex + 1);
                        props = new Properties();
                        props.load(new StringReader(newConfig));
                        Enumeration<Object> keys = props.keys();
                        while (keys.hasMoreElements()) {
                            String key = keys.nextElement().toString();
                            String val = props.getProperty(key);
                            if (val != null) {
                                if(val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
                                    props.setProperty(key, val.substring(1, val.length()-1));
                                }
                            }
                        }       
                    }
                }
            }
        } catch (Exception e) {
            throw KuraException
                    .internalError("wpa_supplicant failed to parse its configuration file");
        }
        finally{
        	if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
        }

        return props;
    }
}
