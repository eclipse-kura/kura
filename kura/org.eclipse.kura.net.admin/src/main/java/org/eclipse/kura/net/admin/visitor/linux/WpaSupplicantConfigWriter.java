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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantManager;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.WifiVisitorUtil;
import org.eclipse.kura.net.admin.visitor.linux.util.WpaSupplicantUtil;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiPassword;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpaSupplicantConfigWriter implements NetworkConfigurationVisitor {
	private static final Logger s_logger = LoggerFactory.getLogger(WpaSupplicantConfigWriter.class);
	
	private static final String WPA_TMP_CONFIG_FILE = "/etc/wpa_supplicant.conf.tmp";
	private static final String TMP_WPA_CONFIG_FILE = "/tmp/wpa_supplicant.conf";
	
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	
	private static WpaSupplicantConfigWriter s_instance;
	
	public static WpaSupplicantConfigWriter getInstance() {
		if (s_instance == null) {
			s_instance = new WpaSupplicantConfigWriter();
		}
		
		return s_instance;
	}

	@Override
	public void visit(NetworkConfiguration config) throws KuraException {
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getModifiedNetInterfaceConfigs();
        
        for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if(netInterfaceConfig.getType() == NetInterfaceType.WIFI) {
                // ignore 'mon' interface
                if(netInterfaceConfig.getName().startsWith("mon.")) {
                    continue;
                }
                
                writeConfig(netInterfaceConfig);
            }
        }

	}
	
	public void generateTempWpaSupplicantConf(WifiConfig wifiConfig, String interfaceName) throws KuraException {

		try {
			this.generateWpaSupplicantConf(wifiConfig, interfaceName, TMP_WPA_CONFIG_FILE);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public void generateTempWpaSupplicantConf() throws KuraException {
		
		try {
			String fileAsString = IOUtil.readResource(FrameworkUtil.getBundle(getClass()), "/src/main/resources/wifi/wpasupplicant.conf");
			File outputFile = new File(TMP_WPA_CONFIG_FILE);
			copyFile(fileAsString, outputFile);
		} catch (Exception e) {
			throw KuraException.internalError("Failed to generate wpa_supplicant.conf");
		}
	}
	
	private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException{
		String interfaceName = netInterfaceConfig.getName();
        s_logger.debug("Writing wpa_supplicant config for {}", interfaceName);
        
        List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
        
        if(netInterfaceAddressConfigs.size() > 0) {
        
	        for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
	        	if(netInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
	        		List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
	        		NetInterfaceStatus netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
	        		WifiMode wifiMode = ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).getMode();
	        		WifiConfig infraConfig = null;
	        		WifiConfig adhocConfig = null;
	        		WifiConfig wpaSupplicantConfig = null;
	        		
	        		// Get the wifi configs
	        		if(netConfigs != null) {
	        			for(NetConfig netConfig : netConfigs){
				        	if(netConfig instanceof WifiConfig) {
				        	    if(((WifiConfig) netConfig).getMode() == WifiMode.ADHOC) {
				        	        adhocConfig = (WifiConfig) netConfig;
				        	    } else if (((WifiConfig) netConfig).getMode() == WifiMode.INFRA) {
				        	        infraConfig = (WifiConfig) netConfig;
				        	    }
				        	} else  if(netConfig instanceof NetConfigIP4) {
	    						netInterfaceStatus = ((NetConfigIP4) netConfig).getStatus();
	    					}
	        			}
	        		}
	        		
	        		if(netInterfaceStatus == NetInterfaceStatus.netIPv4StatusDisabled) {
	        			s_logger.info("Network interface status for " + interfaceName + " is disabled - not overwriting wpaconfig file");
	        			return;
	        		}

                    // Choose which config to write
                    if(wifiMode == WifiMode.INFRA) {
                        if(infraConfig != null) {
                        	StringBuilder key = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.wifi.infra.pingAccessPoint");
        					try {
    							KuranetConfig.setProperty(key.toString(), Boolean.toString(infraConfig.pingAccessPoint()));
    						} catch (IOException e) {
    							s_logger.warn("Error setting KuranetConfig property", e);
    						}
        					
        					key = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.wifi.infra.ignoreSSID");
        					try {
    							KuranetConfig.setProperty(key.toString(), Boolean.toString(infraConfig.ignoreSSID()));
    						} catch (IOException e) {
    							s_logger.warn("Error setting KuranetConfig property", e);
    						}
                            wpaSupplicantConfig = infraConfig;
                        } else {
                            s_logger.debug("Not updating wpa_supplicant config - wifi mode is " + wifiMode + " but the infra config is null");
                        }
                    } else if (wifiMode == WifiMode.ADHOC) {
                        if(adhocConfig != null) {
                            wpaSupplicantConfig = adhocConfig;
                        } else {
                            s_logger.debug("Not updating wpa_supplicant config - wifi mode is " + wifiMode + " but the adhoc config is null");
                        }
                    } else if (wifiMode == WifiMode.MASTER) {
                        if(infraConfig != null && adhocConfig != null) {
                            wpaSupplicantConfig = infraConfig;      // Choose the infra config if both are present?
                        } else if (infraConfig != null) {
                            wpaSupplicantConfig = infraConfig;
                        } else if (adhocConfig != null) {
                            wpaSupplicantConfig = adhocConfig;
                        } else {
                            s_logger.debug("Not updating wpa_supplicant config - wifi mode is " + wifiMode + " and the infra and adhoc configs are null");
                        }
                    }

                    // Write the config
	        		try {
    	        		if(wpaSupplicantConfig != null) {
                            s_logger.debug("Writing wifiConfig: {}", wpaSupplicantConfig);
                            generateWpaSupplicantConf(wpaSupplicantConfig, interfaceName, WPA_TMP_CONFIG_FILE);
                            moveWpaSupplicantConf(interfaceName, WPA_TMP_CONFIG_FILE);
    	        		}
                    } catch (Exception e) {
                        s_logger.error("Failed to configure WPA Supplicant");
                        throw KuraException.internalError(e);
                    }
	        	}
	        }
        }
	}
	
	/*
	 * This method generates the wpa_supplicant configuration file
	 */
	private void generateWpaSupplicantConf(WifiConfig wifiConfig, String interfaceName, String configFile) throws Exception {
		s_logger.debug("Generating WPA Supplicant Config");
		s_logger.debug("Store wifiMode driver: {}", wifiConfig.getDriver());
        StringBuilder key = new StringBuilder("net.interface." +  interfaceName + ".config.wifi." + wifiConfig.getMode().toString().toLowerCase() + ".driver");
		try {
            KuranetConfig.setProperty(key.toString(), wifiConfig.getDriver());
        } catch (Exception e) {
            s_logger.error("Failed to save kuranet config", e);
            throw KuraException.internalError(e);
        }
		if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WEP) {
			File outputFile = new File(configFile);
			
			String fileAsString = null;
			
			if (wifiConfig.getMode() == WifiMode.INFRA) {
				fileAsString = IOUtil.readResource(FrameworkUtil.getBundle(getClass()), "/src/main/resources/wifi/wpasupplicant.conf_wep");
			} else if (wifiConfig.getMode() == WifiMode.ADHOC) { 
				fileAsString = IOUtil.readResource(FrameworkUtil.getBundle(getClass()), "/src/main/resources/wifi/wpasupplicant.conf_adhoc_wep");
				fileAsString = fileAsString.replaceFirst("KURA_FREQUENCY", Integer.toString(
						WpaSupplicantUtil.convChannelToFrequency(wifiConfig.getChannels()[0])));
			} else {
				throw KuraException.internalError("Failed to generate wpa_supplicant.conf -- Inavlid mode: " + wifiConfig.getMode());
			}
			
			// Remove the 'wheel' group assignment for Yocto image on Raspberry_Pi
			if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())) {
				fileAsString = fileAsString.replaceFirst("ctrl_interface_group=wheel", "#ctrl_interface_group=wheel");
			}
			// Replace the necessary components
			fileAsString = fileAsString.replaceFirst("KURA_MODE", Integer.toString(getSupplicantMode(wifiConfig.getMode())));
			if (wifiConfig.getSSID() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_ESSID", wifiConfig.getSSID());
			} else {
				throw KuraException.internalError("the essid can not be null");
			}
			
			WifiPassword wifiPassword = new WifiPassword(wifiConfig.getPasskey().toString());
			wifiPassword.validate(wifiConfig.getSecurity());
			CryptoService cryptoService = WifiVisitorUtil.getCryptoService();
			String passphrase = "";
			if (cryptoService != null) {
				// encrypt password before putting it to the /etc/wpa_supplicant.conf
				passphrase = new String(cryptoService.encryptAes(wifiConfig.getPasskey().getPassword()));
			}
			fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passphrase);
			
			if (wifiConfig.getBgscan() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN",
						wifiConfig.getBgscan().toString());
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN", "");
			}
			
			fileAsString = fileAsString.replaceFirst("KURA_SCANFREQ",
					getScanFrequenciesMHz(wifiConfig.getChannels()));

			// everything is set and we haven't failed - write the file
			copyFile(fileAsString, outputFile);
			return;
		} else if ((wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA)
				|| (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA2)
				|| (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA_WPA2)) {

			File outputFile = new File(configFile);
			String fileAsString = null;

			if (wifiConfig.getMode() == WifiMode.INFRA) {
				fileAsString = IOUtil.readResource(FrameworkUtil.getBundle(getClass()), "/src/main/resources/wifi/wpasupplicant.conf_wpa");
			} else if (wifiConfig.getMode() == WifiMode.ADHOC) {
				fileAsString = IOUtil.readResource(FrameworkUtil.getBundle(getClass()), "/src/main/resources/wifi/wpasupplicant.conf_adhoc_wpa");
				fileAsString = fileAsString.replaceFirst("KURA_FREQUENCY", Integer.toString(
								WpaSupplicantUtil.convChannelToFrequency(wifiConfig.getChannels()[0])));
				fileAsString = fileAsString.replaceFirst("KURA_PAIRWISE", "NONE");
			} else {
				throw KuraException.internalError("Failed to generate wpa_supplicant.conf -- Invalid mode: " + wifiConfig.getMode());
			}
			
			// Remove the 'wheel' group assignment for Yocto image on Raspberry_Pi
			if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())) {
				fileAsString = fileAsString.replaceFirst("ctrl_interface_group=wheel", "#ctrl_interface_group=wheel");
			}
			// replace the necessary components
			fileAsString = fileAsString.replaceFirst("KURA_MODE",
					Integer.toString(getSupplicantMode(wifiConfig.getMode())));

			if (wifiConfig.getSSID() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_ESSID", wifiConfig.getSSID());
			} else {
				throw KuraException.internalError("the essid can not be null");
			}
			
			WifiPassword wifiPassword = new WifiPassword(wifiConfig.getPasskey().toString());
			wifiPassword.validate(wifiConfig.getSecurity());
			CryptoService cryptoService = WifiVisitorUtil.getCryptoService();
			String passphrase = "";
			if (cryptoService != null) {
				// encrypt password before putting it to the /etc/wpa_supplicant.conf
				passphrase = new String(cryptoService.encryptAes(wifiConfig.getPasskey().getPassword()));
			}
			fileAsString = fileAsString.replaceFirst("KURA_PASSPHRASE", passphrase);
						
			if(wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA) {
				fileAsString = fileAsString.replaceFirst("KURA_PROTO", "WPA");
			} else if(wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA2) {
				fileAsString = fileAsString.replaceFirst("KURA_PROTO", "RSN");
			} else if(wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA_WPA2) {
				fileAsString = fileAsString.replaceFirst("KURA_PROTO", "WPA RSN");
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_PROTO", "WPA RSN");
			}

			if (wifiConfig.getPairwiseCiphers() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_PAIRWISE", WifiCiphers.toString(wifiConfig.getPairwiseCiphers()));
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_PAIRWISE", "CCMP TKIP");
			}

			if (wifiConfig.getGroupCiphers() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_GROUP", WifiCiphers.toString(wifiConfig.getGroupCiphers()));
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_GROUP", "CCMP TKIP");
			}
			
			if (wifiConfig.getBgscan() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN", wifiConfig.getBgscan().toString());
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN", "");
			}

			fileAsString = fileAsString.replaceFirst("KURA_SCANFREQ", getScanFrequenciesMHz(wifiConfig.getChannels()));
			
			// everything is set and we haven't failed - write the file
			this.copyFile(fileAsString, outputFile);
			return;
		} else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_NONE || wifiConfig.getSecurity() == WifiSecurity.NONE) {
			File outputFile = new File(configFile);
			String fileAsString = null;

			if (wifiConfig.getMode() == WifiMode.INFRA) {
				fileAsString = IOUtil.readResource(FrameworkUtil.getBundle(getClass()), "/src/main/resources/wifi/wpasupplicant.conf_open");
			} else if (wifiConfig.getMode() == WifiMode.ADHOC) {
				fileAsString = IOUtil.readResource(FrameworkUtil.getBundle(getClass()), "/src/main/resources/wifi/wpasupplicant.conf_adhoc_open");
				fileAsString = fileAsString
						.replaceFirst("KURA_FREQUENCY", Integer.toString(
								WpaSupplicantUtil.convChannelToFrequency(wifiConfig.getChannels()[0])));
			} else {
				throw KuraException
						.internalError("Failed to generate wpa_supplicant.conf -- Invalid mode: " + wifiConfig.getMode());
			}

			// Remove the 'wheel' group assignment for Yocto image on Raspberry_Pi
			if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())) {
				fileAsString = fileAsString.replaceFirst("ctrl_interface_group=wheel", "#ctrl_interface_group=wheel");
			}
			// replace the necessary components
			fileAsString = fileAsString.replaceFirst("KURA_MODE",
					Integer.toString(getSupplicantMode(wifiConfig.getMode())));

			if (wifiConfig.getSSID() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_ESSID", wifiConfig.getSSID());
			} else {
				throw KuraException.internalError("the essid can not be null");
			}
			
			if (wifiConfig.getBgscan() != null) {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN", wifiConfig.getBgscan().toString());
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN", "");
			}

			fileAsString = fileAsString.replaceFirst("KURA_SCANFREQ", getScanFrequenciesMHz(wifiConfig.getChannels()));

			// everything is set and we haven't failed - write the file
			this.copyFile(fileAsString, outputFile);
			return;
		} else {
			throw KuraException.internalError("unsupported security type: " + wifiConfig.getSecurity());
		}
	}
	
	private void moveWpaSupplicantConf(String ifaceName, String configFile) throws KuraException {
		
		File outputFile = new File(configFile);
		File wpaConfigFile = new File(WpaSupplicantManager.getWpaSupplicantConfigFilename(ifaceName));
		try {
			if(!FileUtils.contentEquals(outputFile, wpaConfigFile)) {
			    if(outputFile.renameTo(wpaConfigFile)){
			    	s_logger.trace("Successfully wrote wpa_supplicant config file");
			    }else{
			    	s_logger.error("Failed to write wpa_supplicant config file");
			    	throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for wpa_supplicant config");
			    }
			} else {
				s_logger.info("Not rewriting wpa_supplicant.conf file because it is the same");
			}
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for wpa_supplicant config");
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
					s_logger.error("I/O Exception while closing FileOutputStream!");
				}
			}	
			if(pw != null) pw.close();
		}
	}
	
	/*
	 * This method sets permissions to the wpa_supplicant configuration file
	 */
	private void setPermissions(String fileName) throws KuraException {
		SafeProcess procChmod = null;
		SafeProcess procDos = null;
		try {
			procChmod = ProcessUtil.exec("chmod 600 " + fileName);
			procChmod.waitFor();
			procDos = ProcessUtil.exec("dos2unix " + fileName);
			procDos.waitFor();
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
		finally {
			if (procChmod != null) ProcessUtil.destroy(procChmod);
			if (procDos != null) ProcessUtil.destroy(procDos);
		}
	}
	
	/*
	 * This method returns the supplicant mode
	 */
	private static int getSupplicantMode(WifiMode mode) {
		if (mode == WifiMode.ADHOC) {
			return WpaSupplicantUtil.MODE_IBSS;
		} else if (mode == WifiMode.MASTER) {
			return WpaSupplicantUtil.MODE_AP;
		} else {
			return WpaSupplicantUtil.MODE_INFRA;
		}
	}
	
	/*
	 * This method returns a list of frequencies based on a list of channels
	 */
	private String getScanFrequenciesMHz(int[] channels) {
		StringBuffer sbFrequencies = new StringBuffer();
		if (channels != null && channels.length > 0) {
			for (int i = 0; i < channels.length; i++) {
				sbFrequencies.append(WpaSupplicantUtil.convChannelToFrequency(channels[i]));
				if (i < (channels.length - 1)) {
					sbFrequencies.append(' ');
				}
			}
		} else {
			for (int i = 1; i <= 13; i++) {
				sbFrequencies.append(WpaSupplicantUtil.convChannelToFrequency(i));
				if (i < 13) {
					sbFrequencies.append(' ');
				}
			}
		}
		
		return sbFrequencies.toString();
	}

}
