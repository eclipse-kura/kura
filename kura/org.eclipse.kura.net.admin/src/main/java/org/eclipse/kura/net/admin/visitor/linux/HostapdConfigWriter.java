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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.linux.net.wifi.Hostapd;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostapdConfigWriter implements NetworkConfigurationVisitor {
	
	private static final Logger s_logger = LoggerFactory.getLogger(HostapdConfigWriter.class);
	
	private static final String HEXES = "0123456789ABCDEF";
	
	private static final String HOSTAPD_CONFIG_FILE = "/etc/hostapd.conf";
	
	private static final String HOSTAPD_TMP_CONFIG_FILE = "/etc/hostapd.conf.tmp";
	
	private static HostapdConfigWriter s_instance;
	
	public static HostapdConfigWriter getInstance() {
		if(s_instance == null) {
			s_instance = new HostapdConfigWriter();
		}
		
		return s_instance;
	}

	@Override
	public void visit(NetworkConfiguration config) throws KuraException {
		
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getModifiedNetInterfaceConfigs();
		
		for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig: netInterfaceConfigs) {
			if(netInterfaceConfig.getType() == NetInterfaceType.WIFI) {
                // ignore 'mon' interface
                if(netInterfaceConfig.getName().startsWith("mon.")) {
                    continue;
                }

			    writeConfig(netInterfaceConfig);
			}
		}
	}
	
	private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
		
		String interfaceName = netInterfaceConfig.getName();
		
		List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();

		if (netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
			for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
				if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
					List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
					NetInterfaceStatus netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
					WifiConfig apConfig = null;
					String interfaceDriver = null;
					if (netConfigs != null) {
						for (NetConfig netConfig : netConfigs) {
							try {
								if(netConfig instanceof WifiConfig) {
								    if(((WifiConfig)netConfig).getMode() == WifiMode.MASTER) {
								        s_logger.debug("Found wifiConfig with mode set to master");
								        interfaceDriver = ((WifiConfig)netConfig).getDriver();
								        if(interfaceDriver != null) {
    								        s_logger.debug("Writing wifiConfig: " + netConfig);
    								        apConfig = (WifiConfig)netConfig;
								        } else {
								            s_logger.error("Can't generate hostapd config - no driver specified");
								        }
								    }
								} else  if(netConfig instanceof NetConfigIP4) {
		    						netInterfaceStatus = ((NetConfigIP4) netConfig).getStatus();
		    					}
							} catch (Exception e) {
								s_logger.error("Failed to configure Hostapd");
					        	throw KuraException.internalError(e);
							}
						}
						
						if(netInterfaceStatus == NetInterfaceStatus.netIPv4StatusDisabled) {
		        			s_logger.info("Network interface status for " + interfaceName + " is disabled - not overwriting hostapd configuration file");
		        			return;
		        		}
						
						if (apConfig != null) {
							try {
								generateHostapdConf(apConfig, interfaceName, interfaceDriver);
							} catch (Exception e) {
								s_logger.error("Failed to generate hostapd configuration file");
								throw KuraException.internalError(e);
							}
						}
					}
				}
			}
		}
	}
	
	/*
	 * This method generates hostapd configuration file
	 */
	private void generateHostapdConf(WifiConfig wifiConfig, String interfaceName, String interfaceDriver) throws Exception {
		
		s_logger.debug("Generating Hostapd Config");
		
		if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_NONE) {
			
			File outputFile = new File(HOSTAPD_TMP_CONFIG_FILE);
			
			//replace the necessary components
			String fileAsString = IOUtil.readResource("/src/main/resources/wifi/hostapd.conf_no_security");
			if(interfaceName != null) {
				fileAsString = fileAsString.replaceFirst("KURA_INTERFACE", interfaceName);
			} else {
				throw KuraException.internalError("the interface name can not be null");
			}
			if((interfaceDriver != null) && (interfaceDriver.length() > 0)) {
				fileAsString = fileAsString.replaceFirst("KURA_DRIVER", interfaceDriver);
			} else {
				String drv = Hostapd.getDriver(interfaceName);
				s_logger.warn("The 'driver' parameter must be set: setting to: " + drv);
				fileAsString = fileAsString.replaceFirst("KURA_DRIVER", drv);
				//throw KuraException.internalError("the driver name can not be null");
			}
			if(wifiConfig.getSSID() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_ESSID", wifiConfig.getSSID());
			} else {
				throw KuraException.internalError("the essid can not be null");
			}
			
			WifiRadioMode radioMode = wifiConfig.getRadioMode();
			if (radioMode == WifiRadioMode.RADIO_MODE_80211a) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "a");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				fileAsString = fileAsString.replaceFirst("ht_capab=KURA_HTCAPAB", "");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211b) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "b");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				fileAsString = fileAsString.replaceFirst("ht_capab=KURA_HTCAPAB", "");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211g) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				fileAsString = fileAsString.replaceFirst("ht_capab=KURA_HTCAPAB", "");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211nHT20) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
				fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[SHORT-GI-20]");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211nHT40above) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
				fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40+][SHORT-GI-20][SHORT-GI-40]");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211nHT40below) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
				fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40-][SHORT-GI-20][SHORT-GI-40]");
			} else {
				throw KuraException.internalError("invalid hardware mode");
			}
			
			if(wifiConfig.getChannels()[0] > 0 && wifiConfig.getChannels()[0] < 14) {
				fileAsString = fileAsString.replaceFirst("KURA_CHANNEL", Integer.toString(wifiConfig.getChannels()[0]));
			} else {
				throw KuraException.internalError("the channel " + wifiConfig.getChannels()[0] + " must be between 1 (inclusive) and 11 (inclusive) or 1 (inclusive) and 13 (inclusive) depending on your locale");
			}
			
			if (wifiConfig.ignoreSSID()) {
				fileAsString = fileAsString.replaceFirst("KURA_IGNORE_BROADCAST_SSID", "2");
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_IGNORE_BROADCAST_SSID", "0");
			}
			
			//everything is set and we haven't failed - write the file
			this.copyFile(fileAsString, outputFile);
			
			//move the file if we made it this far
			this.moveFile();
			
			return;
		} else if(wifiConfig.getSecurity() == WifiSecurity.SECURITY_WEP) {
			File outputFile = new File(HOSTAPD_TMP_CONFIG_FILE);

			//replace the necessary components
			String fileAsString = IOUtil.readResource("/src/main/resources/wifi/hostapd.conf_wep");
			if(interfaceName != null) {
				fileAsString = fileAsString.replaceFirst("KURA_INTERFACE", interfaceName);
			} else {
				throw KuraException.internalError("the interface name can not be null");
			}
			if((interfaceDriver != null) && (interfaceDriver.length() > 0)) {
				fileAsString = fileAsString.replaceFirst("KURA_DRIVER", interfaceDriver);
			} else {
				String drv = Hostapd.getDriver(interfaceName);
				s_logger.warn("The 'driver' parameter must be set: setting to: " + drv);
				fileAsString = fileAsString.replaceFirst("KURA_DRIVER", drv);
				//throw KuraException.internalError("the driver name can not be null");
			}
			if(wifiConfig.getSSID() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_ESSID", wifiConfig.getSSID());
			} else {
				throw KuraException.internalError("the essid can not be null");
			}
			
			WifiRadioMode radioMode = wifiConfig.getRadioMode();
			if (radioMode == WifiRadioMode.RADIO_MODE_80211a) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "a");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				fileAsString = fileAsString.replaceFirst("ht_capab=KURA_HTCAPAB", "");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211b) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "b");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				fileAsString = fileAsString.replaceFirst("ht_capab=KURA_HTCAPAB", "");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211g) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				fileAsString = fileAsString.replaceFirst("ht_capab=KURA_HTCAPAB", "");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211nHT20) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
				fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[SHORT-GI-20]");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211nHT40above) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
				fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40+][SHORT-GI-20][SHORT-GI-40]");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211nHT40below) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
				fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40-][SHORT-GI-20][SHORT-GI-40]");
			} else {
				throw KuraException.internalError("invalid hardware mode");
			}
			
			if(wifiConfig.getChannels()[0] > 0 && wifiConfig.getChannels()[0] < 14) {
				fileAsString = fileAsString.replaceFirst("KURA_CHANNEL", Integer.toString(wifiConfig.getChannels()[0]));
			} else {
				throw KuraException.internalError("the channel must be between 1 (inclusive) and 11 (inclusive) or 1 (inclusive) and 13 (inclusive) depending on your locale");
			}
			String passKey = wifiConfig.getPasskey();
			if(passKey != null) {
				if(passKey.length() == 10) {
					//check to make sure it is all hex
					try {
						Long.parseLong(passKey, 16);
					} catch(Exception e) {
						throw KuraException.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
					}
					
					//since we're here - save the password
					fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passKey);
				} else if(passKey.length() == 26) {
					String part1 = passKey.substring(0, 13);
					String part2 = passKey.substring(13);
					
					try {
						Long.parseLong(part1, 16);
						Long.parseLong(part2, 16);
					} catch(Exception e) {
						throw KuraException.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
					}
					
					//since we're here - save the password
					fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passKey);
				} else if(passKey.length() == 32) {
					String part1 = passKey.substring(0, 10);
					String part2 = passKey.substring(10, 20);
					String part3 = passKey.substring(20);
					try {
						Long.parseLong(part1, 16);
						Long.parseLong(part2, 16);
						Long.parseLong(part3, 16);
					} catch(Exception e) {
						throw KuraException.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
					}
					
					//since we're here - save the password
					fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passKey);
				} else if ((passKey.length() == 5)
						|| (passKey.length() == 13)
						|| (passKey.length() == 16)) {
					
					// 5, 13, or 16 ASCII characters
					passKey = toHex(passKey);
					
					//since we're here - save the password
					fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passKey);
				} else {
					throw KuraException.internalError("the WEP key (passwd) must be 10, 26, or 32 HEX characters in length");
				}
				
			} else {
				throw KuraException.internalError("the passwd can not be null");
			}
			
			if (wifiConfig.ignoreSSID()) {
				fileAsString = fileAsString.replaceFirst("KURA_IGNORE_BROADCAST_SSID", "2");
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_IGNORE_BROADCAST_SSID", "0");
			}

			//everything is set and we haven't failed - write the file
			this.copyFile(fileAsString, outputFile);
			
			//move the file if we made it this far
			this.moveFile();
			
			return;
		} else if ((wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA)
				|| (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA2)) {

		    File tmpOutputFile = new File(HOSTAPD_TMP_CONFIG_FILE);
			
			String resName = null;
			if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA) {
				resName = "/src/main/resources/wifi/hostapd.conf_master_wpa_psk";
			} else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA2) {
				resName = "/src/main/resources/wifi/hostapd.conf_master_wpa2_psk";
			}
			
			//replace the necessary components
			String fileAsString = IOUtil.readResource(resName);
			
			if(interfaceName != null) {
				fileAsString = fileAsString.replaceFirst("KURA_INTERFACE", interfaceName);
			} else {
				throw KuraException.internalError("the interface name can not be null");
			}
			if((interfaceDriver != null) && (interfaceDriver.length() > 0)) {
				fileAsString = fileAsString.replaceFirst("KURA_DRIVER", interfaceDriver);
			} else {
				String drv = Hostapd.getDriver(interfaceName);
				s_logger.warn("The 'driver' parameter must be set: setting to: " + drv);
				fileAsString = fileAsString.replaceFirst("KURA_DRIVER", drv);
				//throw KuraException.internalError("the driver name can not be null");
			}
			if(wifiConfig.getSSID() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_ESSID", wifiConfig.getSSID());
			} else {
				throw KuraException.internalError("the essid can not be null");
			}
			
			WifiRadioMode radioMode = wifiConfig.getRadioMode();
			if (radioMode == WifiRadioMode.RADIO_MODE_80211a) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "a");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				fileAsString = fileAsString.replaceFirst("ht_capab=KURA_HTCAPAB", "");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211b) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "b");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				fileAsString = fileAsString.replaceFirst("ht_capab=KURA_HTCAPAB", "");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211g) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				fileAsString = fileAsString.replaceFirst("ht_capab=KURA_HTCAPAB", "");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211nHT20) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
				fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[SHORT-GI-20]");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211nHT40above) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
				fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40+][SHORT-GI-20][SHORT-GI-40]");
			} else if (radioMode == WifiRadioMode.RADIO_MODE_80211nHT40below) {
				fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
				fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
				fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
				fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40-][SHORT-GI-20][SHORT-GI-40]");
			} else {
				throw KuraException.internalError("invalid hardware mode");
			}
			
			if((wifiConfig.getChannels()[0] > 0) && (wifiConfig.getChannels()[0] < 14)) {
				fileAsString = fileAsString.replaceFirst("KURA_CHANNEL", Integer.toString(wifiConfig.getChannels()[0]));
			} else {
				throw KuraException.internalError("the channel must be between 1 (inclusive) and 11 (inclusive) or 1 (inclusive) and 13 (inclusive) depending on your locale");
			}
			if(wifiConfig.getPasskey() != null && wifiConfig.getPasskey().trim().length() > 0) {
				if((wifiConfig.getPasskey().length() < 8) || (wifiConfig.getPasskey().length() > 63)) {
					throw KuraException.internalError("the WPA passphrase (passwd) must be between 8 (inclusive) and 63 (inclusive) characters in length: " + wifiConfig.getPasskey());
				} else {
					fileAsString = fileAsString.replaceFirst("KURA_PASSPHRASE", wifiConfig.getPasskey().trim());
				}
			} else {
				throw KuraException.internalError("the passwd can not be null");
			}
			
			if (wifiConfig.ignoreSSID()) {
				fileAsString = fileAsString.replaceFirst("KURA_IGNORE_BROADCAST_SSID", "2");
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_IGNORE_BROADCAST_SSID", "0");
			}

			//everything is set and we haven't failed - write the file
			this.copyFile(fileAsString, tmpOutputFile);
			
			//move the file if we made it this far
			this.moveFile();
			
			return;
		} else {
			s_logger.error("unsupported security type: " + wifiConfig.getSecurity() +
					" It must be WifiSecurity.SECURITY_NONE, WifiSecurity.SECURITY_WEP, WifiSecurity.SECURITY_WPA, or WifiSecurity.SECURITY_WPA2");
			throw KuraException.internalError("unsupported security type: " + wifiConfig.getSecurity());
		}
	}
	
	
	/*
	 * This method copies supplied String to a file
	 */
	private void copyFile(String data, File destination) throws KuraException {
		FileOutputStream fos = null;
		PrintWriter pw = null;
		try {
			fos = new FileOutputStream(destination);
			pw = new PrintWriter(fos);
			pw.write(data);
			pw.flush();
			fos.getFD().sync();
			
			setPermissions(destination.toString());
		} catch (IOException e) {
			throw KuraException.internalError(e);
		}
		finally{
			if(fos != null){
				try{
					fos.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}	
			if(pw != null) pw.close();
		}
	}
	
	private void moveFile() throws Exception {
		File tmpFile = new File(HOSTAPD_TMP_CONFIG_FILE);
		File file = new File(HOSTAPD_CONFIG_FILE);
		if(!FileUtils.contentEquals(tmpFile, file)) {
			if(tmpFile.renameTo(file)){
				s_logger.trace("Successfully wrote hostapd.conf file");
			}else{
				s_logger.error("Failed to write hostapd.conf file");
				throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for hostapd");
			}
		} else {
			s_logger.info("Not rewriting hostapd.conf file because it is the same");
		}
	}
	
	/*
	 * This method sets permissions to hostapd configuration file 
	 */
	private void setPermissions(String fileName) throws KuraException {
		Process procDos = null;
		Process procChmod = null;
		try {
			procChmod = ProcessUtil.exec("chmod 600 " + fileName);
			procChmod.waitFor();
			
			procDos = ProcessUtil.exec("dos2unix " + fileName);
			procDos.waitFor();
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
		finally {
			ProcessUtil.destroy(procChmod);
			ProcessUtil.destroy(procDos);			
		}
	}
	
	/*
	 * This method converts supplied string to hex
	 */
	private String toHex(String s) {
		if (s == null) {
			return null;
		}
		byte[] raw = s.getBytes();

		StringBuffer hex = new StringBuffer(2 * raw.length);
		for (int i = 0; i < raw.length; i++) {
			hex.append(HEXES.charAt((raw[i] & 0xF0) >> 4)).append(HEXES.charAt((raw[i] & 0x0F)));
		}
		return hex.toString();
	}

}
