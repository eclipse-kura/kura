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
package org.eclipse.kura.linux.net.wifi;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines hostapd AP mode tool
 * 
 * @author ilya.binshtok
 *
 */
public class Hostapd {

	private static final String HEXES = "0123456789ABCDEF";
	
	private static Logger s_logger = LoggerFactory.getLogger(Hostapd.class);
	
	private static Hostapd m_hostapd = null;
	
	private boolean m_isConfigured = false;
	private String m_configFilename = null;
	
	private String m_iface = null;
	private String m_driver = null;
	private String m_essid = null;
	private WifiRadioMode m_radioMode = WifiRadioMode.RADIO_MODE_80211g;

	private int m_channel = 0;
	private WifiSecurity m_security = WifiSecurity.SECURITY_NONE;
	private String m_passwd = null;
	
	private Hostapd() {
		m_configFilename = formHostapdConfigFilename();
	}
	
	public static Hostapd getHostapd() throws KuraException {
		if(m_hostapd == null) {
		    m_hostapd = parseHostapdConf(getConfigFilename());
		}
		return m_hostapd;
	}
	
	/**
	 * Hostapd constructor
	 * 
	 * @param iface - interface name as {@link String}
	 * @param driver - driver as {@link String}
	 * @param essid - SSID as {@link String}
	 * @param hwMode - hardware mode as {@link}
	 * @param channel - channel as {@link}
	 * @param securityType - security type as {@link}
	 * @param password - password as {@link}
	 */
	public static Hostapd getHostapd(String iface, String driver, String essid,
			WifiRadioMode radioMode, int channel, WifiSecurity security, String password) {

		try {
            if (driver == null) {
                driver = getDriver(iface);
            }

            if (m_hostapd == null) {
				m_hostapd = new Hostapd(iface, driver, essid, radioMode, channel,
						security, password);
			} else {
			    // update the current instance
			    m_hostapd.setInterface(iface);
			    m_hostapd.setDriver(driver);
			    m_hostapd.setSSID(essid);
			    m_hostapd.setRadioMode(radioMode);
			    m_hostapd.setChannel(channel);
			    m_hostapd.setSecurity(security);
			    m_hostapd.setPassword(password);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return m_hostapd;
	}

	private Hostapd(String iface, String driver, String essid, WifiRadioMode radioMode,
			int channel, WifiSecurity security, String passwd) throws KuraException {
		
		this.m_configFilename = formHostapdConfigFilename();
		
		this.m_iface = iface;
		this.m_driver = driver;
		this.m_essid = essid;
		this.m_radioMode = radioMode;
		this.m_channel = channel;
		this.m_security = security;
		this.m_passwd = passwd;
		
		String hostapdConfDirectory = formHostapdConfigDirectory();
		
		//make sure our directory exists
		File fHostapdConfigDirectory = new File(hostapdConfDirectory);
		if (!fHostapdConfigDirectory.exists()) {
			if (!fHostapdConfigDirectory.mkdirs()) {
				s_logger.error("failed to create the temporary storage directory in {}", hostapdConfDirectory);
			} else {
				s_logger.debug("created temporary storage directory in {}", hostapdConfDirectory);
			}

			if (!fHostapdConfigDirectory.isDirectory()) {
				s_logger.error("{} is not a directory as it should be", hostapdConfDirectory);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals (Object obj) {
		
		if (!(obj instanceof Hostapd)) {
			return false;
		}
		
		Hostapd hostapd = (Hostapd)obj;
		
		if (!this.m_iface.equals(hostapd.m_iface)) {
			return false;
		}
		if (!this.m_essid.equals(hostapd.m_essid)) {
			return false;
		}
		if (!this.m_driver.equals(hostapd.m_driver)) {
			return false;
		}
		if (this.m_channel != hostapd.m_channel) {
			return false;
		}
		if (!this.m_passwd.equals(hostapd.m_passwd)) {
			return false;
		}
		if (this.m_radioMode != hostapd.m_radioMode) {
			return false;
		}
		if (this.m_security != hostapd.m_security) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Get the interface name
	 */
	public String getInterface() {
		return this.m_iface;
	}
	
	/**
	 * Set the interface name
	 */
	public void setInterface(String iface) {
	    this.m_iface = iface;
	}
	
	/**
	 * Get the driver
	 */
	public String getDriver() {
		return this.m_driver;
	}
	   
    /**
     * Set the driver
     */
    public void setDriver(String driver) {
        this.m_driver = driver;
    }

	/**
	 * Get the SSID
	 */
	public String getSSID() {
		return this.m_essid;
	}
	   
    /**
     * Set the SSID
     */
    public void setSSID(String essid) {
        this.m_essid = essid;
    }	
	
	/**
	 * Get the radio mode
	 * 
	 * @return WifiOptions
	 */
	public WifiRadioMode getRadioMode() {
		return this.m_radioMode;
	}
	
    /**
     * Set the radio mode
     */
    public void setRadioMode(WifiRadioMode radioMode) {
        this.m_radioMode = radioMode;
    }
	
	/**
	 * Get the channel 
	 */
	public int getChannel() {
		return this.m_channel;
	}
	
	/**
	 * Set the channel
	 */
	public void setChannel(int channel) {
	    this.m_channel = channel;
	}
	
	/**
	 * Get the security type
	 * 
	 * @return WifiOptions
	 */
	public WifiSecurity getSecurity() {
		return this.m_security;
	}
	
	/**
	 * Set the security type
	 */
	public void setSecurity(WifiSecurity security) {
	    this.m_security = security;
	}
	
	/**
	 * Get the password
	 */
	public String getPassword() {
		return this.m_passwd;
	}
	
	/**
	 * Set the password
	 */
	public void setPassword(String password) {
	    this.m_passwd = password;
	}

	/**
	 * Save the current values to the config file
	 */
	public void saveConfig() throws KuraException {

		try {
			Hostapd hostapd = null;
			try {
				hostapd = getHostapd();
			} catch (Exception e) {
				e.printStackTrace();
				this.generateHostapdConf();
			}
			
			if ((hostapd == null) || (!this.equals(hostapd))) {
				this.generateHostapdConf();
			}
			this.m_isConfigured = true;
		} catch (Exception e) {
			this.m_isConfigured = false;
			s_logger.error("failed to configure hostapd");
			throw KuraException.internalError(e);
		}
	}
	
	/**
	 * Launches hostapd
	 * 
	 * @throws Exception
	 */
	public void enable() throws KuraException {
		
		this.saveConfig();
		
		if(this.m_isConfigured) {
			SafeProcess proc = null;
			try {
				if(this.isEnabled()) {
					this.disable();
				}
				
				//start hostapd
				String launchHostapdCommand = this.formHostapdCommand();
				s_logger.debug("starting hostapd --> {}", launchHostapdCommand);
				proc = ProcessUtil.exec(launchHostapdCommand);
				if(proc.waitFor() != 0) {
					s_logger.error("failed to start hostapd for unknown reason");
					throw KuraException.internalError("failed to start hostapd for unknown reason");
				}
				Thread.sleep(1000);
			} catch(Exception e) {
				throw KuraException.internalError(e);
			}
			finally {
				if (proc != null) ProcessUtil.destroy(proc);
			}
		} else {
			s_logger.error("Hostapd failed to configure - so can not start");
			throw KuraException.internalError("Hostapd failed to configure - so can not start");
		}
	}

	/**
	 * Stops hostapd
	 * 
	 * @throws Exception
	 */
	public void disable() throws KuraException {
		killAll();
	}
	
	/**
	 * Stops all instances of hostapd
	 * 
	 * @throws Exception
	 */
	public static void killAll() throws KuraException {
		SafeProcess proc = null;
		try {
			//kill hostapd
			s_logger.debug("stopping hostapd");
			proc = ProcessUtil.exec("killall hostapd");
			proc.waitFor();
			Thread.sleep(1000);
		} catch(Exception e) {
			throw KuraException.internalError(e);
		}
		finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}
	}
	
	/**
	 * Reports if hostapd is running
	 * 
	 * @return {@link boolean}
	 */
	public boolean isEnabled() throws KuraException {
		try {
			// Check if hostapd is running
			int pid = LinuxProcessUtil.getPid(this.formHostapdCommand());
			return (pid > -1);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}
	
	/**
	 * Reports if there is an instance of hostapd running
	 * 
	 * @return {@link boolean}
	 */
	public static boolean hasInstanceRunning() throws KuraException {
		try {
			// Check if hostapd is running
			int pid = LinuxProcessUtil.getPid("hostapd");
			return (pid > -1);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}
	
	/*
	 * This method generates hostapd configuration file
	 */
	private void generateHostapdConf() throws Exception {
		if(this.m_security == WifiSecurity.SECURITY_NONE) {
			File outputFile = new File(this.m_configFilename);
			InputStream is = this.getClass().getResourceAsStream("/src/main/resources/wifi/hostapd.conf_no_security");
			String fileAsString = null;
			if (is != null) {
				fileAsString = readInputStreamAsString(is);
				is.close();
				is = null;
			}
			if (fileAsString != null) {
				//relace the necessary components
				if(this.m_iface != null) {
					fileAsString = fileAsString.replaceFirst("KURA_INTERFACE", this.m_iface);
				} else {
					throw KuraException.internalError("the interface name can not be null");
				}
				if(this.m_driver != null) {
					fileAsString = fileAsString.replaceFirst("KURA_DRIVER", this.m_driver);
				} else {
					throw KuraException.internalError("the driver name can not be null");
				}
				if(this.m_essid != null) {
					fileAsString = fileAsString.replaceFirst("KURA_ESSID", this.m_essid);
				} else {
					throw KuraException.internalError("the essid can not be null");
				}
				
				if (m_radioMode == WifiRadioMode.RADIO_MODE_80211a) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "a");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211b) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "b");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211g) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211nHT20) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
					fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[SHORT-GI-20]");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211nHT40above) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
					fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40+][SHORT-GI-20][SHORT-GI-40]");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211nHT40below) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
					fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40-][SHORT-GI-20][SHORT-GI-40]");
				} else {
					throw KuraException.internalError("invalid hardware mode");
				}
				
				if(this.m_channel > 0 && this.m_channel < 14) {
					fileAsString = fileAsString.replaceFirst("KURA_CHANNEL", Integer.toString(this.m_channel));
				} else {
					throw KuraException.internalError("the channel " + this.m_channel + " must be between 1 (inclusive) and 11 (inclusive) or 1 (inclusive) and 13 (inclusive) depending on your locale");
				}
	
				//everything is set and we haven't failed - write the file
				this.copyFile(fileAsString, outputFile);
			}
			return;
		} else if(this.m_security == WifiSecurity.SECURITY_WEP) {
			File outputFile = new File(this.m_configFilename);
			InputStream is = this.getClass().getResourceAsStream("/src/main/resources/wifi/hostapd.conf_wep");
			String fileAsString = null;
			if (is != null) {
				fileAsString = readInputStreamAsString(is);
				is.close();
				is = null;
			}
			if (fileAsString != null) {
				//relace the necessary components
				if(this.m_iface != null) {
					fileAsString = fileAsString.replaceFirst("KURA_INTERFACE", this.m_iface);
				} else {
					throw KuraException.internalError("the interface name can not be null");
				}
				if(this.m_driver != null) {
					fileAsString = fileAsString.replaceFirst("KURA_DRIVER", this.m_driver);
				} else {
					throw KuraException.internalError("the driver name can not be null");
				}
				if(this.m_essid != null) {
					fileAsString = fileAsString.replaceFirst("KURA_ESSID", this.m_essid);
				} else {
					throw KuraException.internalError("the essid can not be null");
				}
				
				if (m_radioMode == WifiRadioMode.RADIO_MODE_80211a) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "a");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211b) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "b");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211g) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211nHT20) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
					fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[SHORT-GI-20]");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211nHT40above) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
					fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40+][SHORT-GI-20][SHORT-GI-40]");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211nHT40below) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
					fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40-][SHORT-GI-20][SHORT-GI-40]");
				} else {
					throw KuraException.internalError("invalid hardware mode");
				}
				
				if(this.m_channel > 0 && this.m_channel < 14) {
					fileAsString = fileAsString.replaceFirst("KURA_CHANNEL", Integer.toString(this.m_channel));
				} else {
					throw KuraException.internalError("the channel must be between 1 (inclusive) and 11 (inclusive) or 1 (inclusive) and 13 (inclusive) depending on your locale");
				}
				if(this.m_passwd != null) {
					if(this.m_passwd.length() == 10) {
						//check to make sure it is all hex
						try {
							Long.parseLong(this.m_passwd, 16);
						} catch(Exception e) {
							throw KuraException.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
						}
						
						//since we're here - save the password
						fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", m_passwd);
					} else if(this.m_passwd.length() == 26) {
						String part1 = this.m_passwd.substring(0, 13);
						String part2 = this.m_passwd.substring(13);
						
						try {
							Long.parseLong(part1, 16);
							Long.parseLong(part2, 16);
						} catch(Exception e) {
							throw KuraException.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
						}
						
						//since we're here - save the password
						fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", this.m_passwd);
					} else if(this.m_passwd.length() == 32) {
						String part1 = this.m_passwd.substring(0, 10);
						String part2 = this.m_passwd.substring(10, 20);
						String part3 = this.m_passwd.substring(20);
						try {
							Long.parseLong(part1, 16);
							Long.parseLong(part2, 16);
							Long.parseLong(part3, 16);
						} catch(Exception e) {
							throw KuraException.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
						}
						
						//since we're here - save the password
						fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", this.m_passwd);
					} else if ((this.m_passwd.length() == 5)
							|| (this.m_passwd.length() == 13)
							|| (this.m_passwd.length() == 16)) {
						
						// 5, 13, or 16 ASCII characters
						this.m_passwd = this.toHex(this.m_passwd);
						
						//since we're here - save the password
						fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", this.m_passwd);
					} else {
						throw KuraException.internalError("the WEP key (passwd) must be 10, 26, or 32 HEX characters in length");
					}
					
				} else {
					throw KuraException.internalError("the passwd can not be null");
				}
	
				//everything is set and we haven't failed - write the file
				this.copyFile(fileAsString, outputFile);
			}
			return;
		} else if ((this.m_security == WifiSecurity.SECURITY_WPA)
				|| (this.m_security == WifiSecurity.SECURITY_WPA2)) {

		    File outputFile = new File(this.m_configFilename);
			
			InputStream is = null;
			String fileAsString = null;
			if (this.m_security == WifiSecurity.SECURITY_WPA) {
				is = this.getClass().getResourceAsStream("/src/main/resources/wifi/hostapd.conf_master_wpa_psk");
				if (is != null) {
					fileAsString = readInputStreamAsString(is);
					is.close();
					is = null;
				}
			} else if (this.m_security == WifiSecurity.SECURITY_WPA2) {
				is = this.getClass().getResourceAsStream("/src/main/resources/wifi/hostapd.conf_master_wpa2_psk");
				if (is != null) {
					fileAsString = readInputStreamAsString(is);
					is.close();
					is = null;
				}
			}
			
			if (fileAsString != null) {
				//replace the necessary components
				if(this.m_iface != null) {
					fileAsString = fileAsString.replaceFirst("KURA_INTERFACE", this.m_iface);
				} else {
					throw KuraException.internalError("the interface name can not be null");
				}
				if(this.m_driver != null) {
					fileAsString = fileAsString.replaceFirst("KURA_DRIVER", this.m_driver);
				} else {
					throw KuraException.internalError("the driver name can not be null");
				}
				if(this.m_essid != null) {
					fileAsString = fileAsString.replaceFirst("KURA_ESSID", this.m_essid);
				} else {
					throw KuraException.internalError("the essid can not be null");
				}
				
				if (m_radioMode == WifiRadioMode.RADIO_MODE_80211a) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "a");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211b) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "b");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211g) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "0");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "0");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211nHT20) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
					fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[SHORT-GI-20]");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211nHT40above) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
					fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40+][SHORT-GI-20][SHORT-GI-40]");
				} else if (m_radioMode == WifiRadioMode.RADIO_MODE_80211nHT40below) {
					fileAsString = fileAsString.replaceFirst("KURA_HW_MODE", "g");
					fileAsString = fileAsString.replaceFirst("KURA_WME_ENABLED", "1");
					fileAsString = fileAsString.replaceFirst("KURA_IEEE80211N", "1");
					fileAsString = fileAsString.replaceFirst("KURA_HTCAPAB", "[HT40-][SHORT-GI-20][SHORT-GI-40]");
				} else {
					throw KuraException.internalError("invalid hardware mode");
				}
				
				if((this.m_channel > 0) && (this.m_channel < 14)) {
					fileAsString = fileAsString.replaceFirst("KURA_CHANNEL", Integer.toString(this.m_channel));
				} else {
					throw KuraException.internalError("the channel must be between 1 (inclusive) and 11 (inclusive) or 1 (inclusive) and 13 (inclusive) depending on your locale");
				}
				if(this.m_passwd != null && m_passwd.trim().length() > 0) {
					if((this.m_passwd.length() < 8) || (this.m_passwd.length() > 63)) {
						throw KuraException.internalError("the WPA passphrase (passwd) must be between 8 (inclusive) and 63 (inclusive) characters in length: " + m_passwd);
					} else {
						fileAsString = fileAsString.replaceFirst("KURA_PASSPHRASE", this.m_passwd.trim());
					}
				} else {
					throw KuraException.internalError("the passwd can not be null");
				}
	
				//everything is set and we haven't failed - write the file
				this.copyFile(fileAsString, outputFile);
			}
			return;
		} else {
			s_logger.error("unsupported security type: {} It must be WifiSecurity.SECURITY_NONE, WifiSecurity.SECURITY_WEP, WifiSecurity.SECURITY_WPA, or WifiSecurity.SECURITY_WPA2", m_security);
			throw KuraException.internalError("unsupported security type: " + m_security);
		}
	}
	
	
	/*
	 * Return a Hostapd instance from a given config file
	 */
	private static Hostapd parseHostapdConf(String filename) throws KuraException {
		FileInputStream fis = null;
		try {
			Hostapd hostapd = null;
	
			File configFile = new File(filename);
			Properties hostapdProps = new Properties();
			
			s_logger.debug("parsing hostapd config file: {}", configFile.getAbsolutePath());
			if(configFile.exists()) {
				fis = new FileInputStream(configFile);
				hostapdProps.load(fis);

				// remove any quotes around the values
				Enumeration<Object> keys = hostapdProps.keys();
				while(keys.hasMoreElements()) {
					String key = keys.nextElement().toString();
					String val = hostapdProps.getProperty(key);
					if(val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
						hostapdProps.setProperty(key, val.substring(1, val.length()-1));
					}
				}
				
				String iface = hostapdProps.getProperty("interface");
				String driver = hostapdProps.getProperty("driver");
				String essid = hostapdProps.getProperty("ssid");
				int channel = Integer.parseInt(hostapdProps.getProperty("channel"));
				
				// Determine radio mode
				WifiRadioMode wifiRadioMode = null;
				String hwModeStr = hostapdProps.getProperty("hw_mode");
				if("a".equals(hwModeStr)) {
					wifiRadioMode = WifiRadioMode.RADIO_MODE_80211a;
				} else if("b".equals(hwModeStr)) {
					wifiRadioMode = WifiRadioMode.RADIO_MODE_80211b;
				} else if("g".equals(hwModeStr)) {
					wifiRadioMode = WifiRadioMode.RADIO_MODE_80211g;
					if("1".equals(hostapdProps.getProperty("ieee80211n"))) {
						wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT20;
						String ht_capab = hostapdProps.getProperty("ht_capab");
						if(ht_capab != null) {
							if(ht_capab.contains("HT40+")) {
								wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT40above;
							} else if(ht_capab.contains("HT40-")) {
								wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT40below;
							}
						}
					}
				} else {
					throw KuraException.internalError("malformatted config file, unexpected hw_mode: " + configFile.getAbsolutePath());
				}
				
				// Determine security and pass
				WifiSecurity security = WifiSecurity.SECURITY_NONE;
				String passwd = "";			
				
				if(hostapdProps.containsKey("wpa")) {
					if("1".equals(hostapdProps.getProperty("wpa"))) {
						security = WifiSecurity.SECURITY_WPA;
					} else if("2".equals(hostapdProps.getProperty("wpa"))) {
						security = WifiSecurity.SECURITY_WPA2;
					} else {
						throw KuraException.internalError("malformatted config file: " + configFile.getAbsolutePath());
					}
					
					if(hostapdProps.containsKey("wpa_passphrase")) {
						passwd = hostapdProps.getProperty("wpa_passphrase");
					} else if(hostapdProps.containsKey("wpa_psk")) {
						passwd = hostapdProps.getProperty("wpa_psk");
					} else {
						throw KuraException.internalError("malformatted config file, no wpa passphrase: " + configFile.getAbsolutePath());
					}
				} else if(hostapdProps.containsKey("wep_key0")) {
					security = WifiSecurity.SECURITY_WEP;
					passwd = hostapdProps.getProperty("wep_key0");
				}
				
				hostapd = new Hostapd(iface, driver, essid, wifiRadioMode, channel, security, passwd);
			} else {
				hostapd = new Hostapd();
			}
			
			return hostapd;
		} catch (Exception e) {
			e.printStackTrace();
			throw KuraException.internalError(e);
		}
		finally{
			if(fis != null){
				try{
					fis.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}			
		}
	}
	
	/*
	 * This method copies supplied String to a file
	 */
	private void copyFile(String data, File destination) throws KuraException {
		try {
			FileOutputStream fos = new FileOutputStream(destination);
			PrintWriter pw = new PrintWriter(fos);
			pw.write(data);
			pw.flush();
			fos.getFD().sync();
			pw.close();
			fos.close();
			
			setPermissions(destination.toString());
		} catch (IOException e) {
			throw KuraException.internalError(e);
		}
	}
	
	/*
	 * This method sets permissions to hostapd configuration file 
	 */
	private void setPermissions(String fileName) throws KuraException {
		SafeProcess procDos = null;
		SafeProcess procChmod = null;
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
	 * This method reads supplied input stream into a string
	 */
	private static String readInputStreamAsString(InputStream is) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while(result != -1) {
			byte b = (byte)result;
			buf.write(b);
			result = bis.read();
		}
		return buf.toString();
	}
	
	/*
	 * This method converts supplied string to hex
	 */
	private String toHex(String s) {
		if (s == null) {
			return null;
		}
		byte[] raw = s.getBytes();

		StringBuilder hex = new StringBuilder(2 * raw.length);
		for (int i = 0; i < raw.length; i++) {
			hex.append(HEXES.charAt((raw[i] & 0xF0) >> 4)).append(HEXES.charAt((raw[i] & 0x0F)));
		}
		return hex.toString();
	}
	
	/*
	 * This method forms hostapd command
	 */
	private String formHostapdCommand () {
		
		StringBuilder sb = new StringBuilder();
		sb.append("hostapd -B ");
		sb.append(this.m_configFilename);
		
		return sb.toString();
	}
	
	/*
	 * This method forms hostapd configuration directory string
	 */
	private static String formHostapdConfigDirectory () {
		
		StringBuilder sb = new StringBuilder();
//		sb.append("/tmp/.kura/");
//		sb.append(Hostapd.class.getPackage().getName());
		sb.append("/etc/");
		
		return sb.toString();
	}
	
	/*
	 * This method forms hostapd configuration filename
	 */
	private static String formHostapdConfigFilename () {
		
		StringBuilder sb = new StringBuilder ();
		sb.append(Hostapd.formHostapdConfigDirectory());
		sb.append("/hostapd.conf");
		
		return sb.toString();
	}
	
	public static String getConfigFilename() {
		return formHostapdConfigFilename();
	}

    public static String getDriver(String iface) throws KuraException {
        String driver = null;
        Collection<String> supportedWifiOptions = null;
        try {
            supportedWifiOptions = WifiOptions.getSupportedOptions(iface);
        } catch (Exception e) {
            throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
        }
        
        if ((supportedWifiOptions != null) && (supportedWifiOptions.size() > 0)) {
            if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
                driver = WifiOptions.WIFI_MANAGED_DRIVER_NL80211;
            } else {
                driver = "hostap";
            }
        } else {
        	//make a guess
        	driver = WifiOptions.WIFI_MANAGED_DRIVER_NL80211;
        }
        return driver;
    }
}
