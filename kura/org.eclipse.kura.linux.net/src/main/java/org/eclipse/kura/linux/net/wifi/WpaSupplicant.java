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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.linux.net.route.RouteService;
import org.eclipse.kura.linux.net.route.RouteServiceImpl;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.route.RouteConfig;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines WiFi wpa_supplicant tool
 * 
 * @author ilya.binshtok
 * 
 */
public class WpaSupplicant {
	
	public static final int[] ALL_CHANNELS = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
	
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	
	private static final int MODE_INFRA = 0;
	private static final int MODE_IBSS = 1;
	private static final int MODE_AP = 2;

	private static final int CONN_NONE = 0;
	private static final int CONN_L2 = 2;
	private static final int CONN_L3 = 3;

	private static final String HEXES = "0123456789ABCDEF";

	private static Logger s_logger = LoggerFactory
			.getLogger(WpaSupplicant.class);

	private static WpaSupplicant m_wpaSupplicant = null;

	private boolean m_isConfigured = false;
	private String m_configFilename = null;

	private WifiMode m_mode = WifiMode.INFRA;
	private String m_iface = null;
	private String m_driver = null;
	private String m_essid = null;
	private WifiSecurity m_security = WifiSecurity.SECURITY_NONE;
	private WifiCiphers m_pairwiseCiphers = WifiCiphers.CCMP_TKIP;
	private WifiCiphers m_groupCiphers = WifiCiphers.CCMP_TKIP;
	private int[] m_scanChannels = ALL_CHANNELS;
	private String m_passwd = null;
	private WifiBgscan m_bgscan = null;

	private ScheduledThreadPoolExecutor m_worker = null;
	private ScheduledFuture<?> m_handle = null;
	private WpaSupplicantStatus m_status = null;
	private int m_ifaceDisabledCnt = 0;
	private int m_connState = CONN_NONE;
	
	private RouteService m_routeService = null;	

	/**
	 * Instantiates WpaSupplicant object
	 * 
	 * @param iface - interface name as {@link String}
	 * @param mode - wifi mode (master, managed, ad-hoc) as {@link WifiMode}
	 * @param driver - driver name as {@link String}
	 * @param essid - service set ID (ESSID) as {@link String}
	 * @param securityType - security type as {@link WifiSecurity}
	 * @param pairwiseCiphers - allowed pairwise ciphers as {@link WifiCiphers}
	 * @param groupCiphers - allowed group ciphers as {@link WifiCiphers}
	 * @param scanChannels- scan channels as {@link int[]}
	 * @param passwd - password as {@link String}
	 * @param bgscan - background scan as {@link WifiBgscan}
	 */
	public static WpaSupplicant getWpaSupplicant(String iface, WifiMode mode,
			String driver, String essid, WifiSecurity security,
			WifiCiphers pairwiseCiphers, WifiCiphers groupCiphers,
			int[] scanChannels, String passwd, WifiBgscan bgscan) {

		try {
		    if(m_wpaSupplicant == null) {
    			m_wpaSupplicant = new WpaSupplicant(iface, mode, driver, essid,
    					security, pairwiseCiphers,
    					groupCiphers, scanChannels, passwd, bgscan);
		    } else {
		        // update the current instance
		        m_wpaSupplicant.setInterface(iface);
		        m_wpaSupplicant.setMode(mode);
		        m_wpaSupplicant.setSSID(essid);
		        m_wpaSupplicant.setWifiSecurity(security);
		        m_wpaSupplicant.setPairwiseCiphers(pairwiseCiphers);
		        m_wpaSupplicant.setGroupCiphers(groupCiphers);
		        m_wpaSupplicant.setChannels(scanChannels);
		        m_wpaSupplicant.setPassword(passwd);
		        m_wpaSupplicant.setBgscan(bgscan);
		        
		        if(driver != null)
	                m_wpaSupplicant.setDriver(driver);
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}

		return m_wpaSupplicant;
	}

	public static WpaSupplicant getWpaSupplicant(String ifaceName) throws KuraException {

	    if (m_wpaSupplicant == null) {
	        m_wpaSupplicant = parseWpaConfig(ifaceName);
	    }
	    
		return m_wpaSupplicant;
	}
	
	// Constructor
    private WpaSupplicant(String iface, WifiMode mode, String driver,
            String essid, WifiSecurity security,
            WifiCiphers pairwiseCiphers, WifiCiphers groupCiphers,
            int[] scanChannels, String passwd, WifiBgscan bgscan) throws KuraException {
        
        this.setup(iface, mode, driver, essid, security, passwd);
        m_routeService = RouteServiceImpl.getInstance();
        m_pairwiseCiphers = pairwiseCiphers;
        m_groupCiphers = groupCiphers;
        m_scanChannels = scanChannels;
        m_bgscan = bgscan;
    }

    private static WpaSupplicant parseWpaConfig(String ifaceName)
            throws KuraException {
        Properties props = parseConfigFile();
		
		if (props == null) {
			s_logger.warn("WPA in client mode is not configured");
			return null;
		}
		
		String ssid = props.getProperty("ssid");
		if(ssid == null) {
			s_logger.warn("WPA in client mode is not configured");
			return null;
		}
		s_logger.debug("curent wpa_supplicant.conf: ssid=" + ssid);
		
		int [] channels = null;
		
		// wifi mode
		int mode = (props.getProperty("mode") != null) ? Integer.parseInt(props.getProperty("mode")) : MODE_INFRA;
		s_logger.debug("current wpa_supplicant.conf: mode=" + mode);
		WifiMode wifiMode = null;
		switch (mode) {
		case MODE_INFRA:
			wifiMode = WifiMode.INFRA;
			String scan_freq = props.getProperty("scan_freq");
			if (scan_freq != null && scan_freq.length() > 0) {
				s_logger.debug("current wpa_supplicant.conf: scan_freq=" + scan_freq);
				String [] saScanFreq = scan_freq.split(" ");
				channels = new int [saScanFreq.length];
				for (int i = 0; i < channels.length; i++) {
					try {
						channels[i] = frequencyMhz2Channel(Integer.parseInt(saScanFreq[i]));
					} catch (NumberFormatException e) {
						s_logger.warn("Invalid string in wpa_supplicant.conf for scan_freq: " + scan_freq);
					}
				}
			} else {
				channels = new int [11];
				for (int i = 0; i < channels.length; i++) {
					channels[i] = i+1;
				}
			}
			break;
		case MODE_IBSS:
			channels = new int [1];
			wifiMode = WifiMode.ADHOC;
			String frequency = props.getProperty("frequency");
			s_logger.debug("current wpa_supplicant.conf: frequency=" + frequency);
			int freq = 2412;
			if (frequency != null) { 
				try {
					freq = Integer.parseInt(frequency);
					channels[0] = frequencyMhz2Channel(freq);
				} catch (NumberFormatException e) {
					freq = 2412;
				}
			}
			break;
		case MODE_AP:
			throw KuraException
					.internalError("wpa_supplicant failed to parse its configuration file: MODE_AP is invalid");
		default:
			throw KuraException
				.internalError("wpa_supplicant failed to parse its configuration file: invalid mode: " + mode);
		}
		
		String proto = props.getProperty("proto");
		if (proto != null) {
			s_logger.debug("current wpa_supplicant.conf: proto=" + proto);
		}
		
		WifiCiphers pairwiseCiphers = null;
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
		
		WifiCiphers groupCiphers = null;
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
		WifiSecurity wifiSecurity = null;
		String password = null;
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
				wifiSecurity = WifiSecurity.SECURITY_WPA_WPA2;
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
		
		WifiBgscan bgscan = null;
		String sBgscan = props.getProperty("bgscan");
		if (sBgscan != null) {
			s_logger.debug("current wpa_supplicant.conf: bgscan=" + sBgscan);
			bgscan = new WifiBgscan(sBgscan);
		}
		
		return getWpaSupplicant(ifaceName, wifiMode, null, ssid, wifiSecurity,
				pairwiseCiphers, groupCiphers,
				channels, password, bgscan);
    }

	/**
	 * Setup and allocates resources
	 * 
	 * @param iface - interface name as {@link String}
	 * @param mode - wifi mode (master, managed, ad-hoc) as {@link WifiMode}
	 * @param driver - driver name as {@link String}
	 * @param essid - service set ID (ESSID) as {@link String}
	 * @param securityType - security type as {@link WifiSecurity}
	 * @param passwd - password as {@link String}
	 * @throws Exception
	 */
	private void setup(String iface, WifiMode mode, String driver,
			String essid, WifiSecurity security, String passwd)
			throws KuraException {

		this.m_mode = mode;
		this.m_worker = new ScheduledThreadPoolExecutor(1);
		this.m_configFilename = WpaSupplicant.formSupplicantConfigFilename();

		if ((security != WifiSecurity.SECURITY_WEP)
				&& (security != WifiSecurity.SECURITY_WPA)
				&& (security != WifiSecurity.SECURITY_WPA2)
				&& (security != WifiSecurity.SECURITY_WPA_WPA2)
				&& (security != WifiSecurity.SECURITY_NONE)) {
			throw KuraException
					.internalError("the security type must be either WifiSecurity.SECURITY_NONE, WifiSecurity.SECURITY_WEP, WifiSecurity.SECURITY_WPA, or WifiSecurity.SECURITY_WPA2 if you are using WpaSupplicant");
		}

		if (driver == null) {
			Collection<String> supportedDrivers = null;
			Collection<String> supportedWifiOptions = null;
			try {
				supportedDrivers = this.getSupportedDrivers();
				supportedWifiOptions = WifiOptions.getSupportedOptions(iface);
				supportedDrivers.retainAll(supportedWifiOptions);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}

			if ((supportedDrivers != null) && (supportedDrivers.size() > 0)) {
				if (supportedDrivers
						.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
					driver = WifiOptions.WIFI_MANAGED_DRIVER_NL80211;
				} else if (supportedDrivers
						.contains(WifiOptions.WIFI_MANAGED_DRIVER_WEXT)) {
					driver = WifiOptions.WIFI_MANAGED_DRIVER_WEXT;
				}
			}
		}

		if ((driver.compareTo(WifiOptions.WIFI_MANAGED_DRIVER_WEXT) != 0)
				&& (driver.compareTo(WifiOptions.WIFI_MANAGED_DRIVER_HOSTAP) != 0)
				&& (driver.compareTo(WifiOptions.WIFI_MANAGED_DRIVER_ATMEL) != 0)
				&& (driver.compareTo(WifiOptions.WIFI_MANAGED_DRIVER_WIRED) != 0)
				&& (driver.compareTo(WifiOptions.WIFI_MANAGED_DRIVER_NL80211) != 0)) {
			throw KuraException
					.internalError("the driver must be one of the following:\n"
							+ "\t\tIWifiDeviceService.WIFI_MANAGED_DRIVER_WEXT\n"
							+ "\t\tIWifiDeviceService.WIFI_MANAGED_DRIVER_HOSTAP\n"
							+ "\t\tIWifiDeviceService.WIFI_MANAGED_DRIVER_ATMEL\n"
							+ "\t\tIWifiDeviceService.WIFI_MANAGED_DRIVER_WIRED\n"
							+ "\t\tIWifiDeviceService.WIFI_MANAGED_DRIVER_NL80211\n");
		}

		// make sure our directory exists
		String supplicantConfigDirectory = WpaSupplicant
				.formSupplicantConfigDirectory();

		File fWpaSupplicantConfigDirectory = new File(supplicantConfigDirectory);
		if (!fWpaSupplicantConfigDirectory.exists()) {
			if (!fWpaSupplicantConfigDirectory.mkdirs()) {
				s_logger.error("failed to create directory for wpa_supplicant.conf "
						+ supplicantConfigDirectory);
			} else {
				s_logger.debug("created directory for wpa_supplicant.conf - "
						+ supplicantConfigDirectory);
			}

			if (!fWpaSupplicantConfigDirectory.isDirectory()) {
				s_logger.error(supplicantConfigDirectory
						+ " is not a directory as it should be.");
			}
		}

		this.m_iface = iface;
		this.m_driver = driver;
		this.m_essid = essid;
		this.m_security = security;
		this.m_passwd = passwd;
	}

	/**
	 * Launches wpa_supplicant
	 * 
	 * @throws Exception
	 */
	public synchronized void enable() throws KuraException {
		
		s_logger.debug("enable WPA Supplicant");
		
		//this.saveConfig();
		
		Process proc = null;
		try {
			if (this.isEnabled()) {
				this.disable();
			}
			// start wpa_supplicant
			String wpaSupplicantCommand = this.formSupplicantCommand();
			s_logger.debug("starting wpa_supplicant -> " + wpaSupplicantCommand);
			proc = ProcessUtil.exec(wpaSupplicantCommand);

			int stat = proc.waitFor();
			if (stat != 0) {
				s_logger.error("failed to start wpa_supplicant error code is "
						+ stat);
				s_logger.debug("STDOUT: " + LinuxProcessUtil.getInputStreamAsString(proc.getInputStream()));
				s_logger.debug("STDERR: " + LinuxProcessUtil.getInputStreamAsString(proc.getErrorStream()));
				throw KuraException
						.internalError("failed to start wpa_supplicant for unknown reason");
			}
			Thread.sleep(1000);
			s_logger.debug("Starting WpaSupplicant monitor thread ...");
			if (m_handle != null) {
				m_handle.cancel(true);
			}
			this.m_handle = this.m_worker.scheduleAtFixedRate(
					new Runnable() {
						public void run() {
							Thread.currentThread().setName("WpaSupplicant");
							monitor();
						}
					}, 0, 2, TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
			throw KuraException.internalError(e);
		}
		finally {
			ProcessUtil.destroy(proc);
		}

	}

	/**
	 * Stops wpa_supplicant
	 * 
	 * @throws Exception
	 */
	public synchronized void disable() throws KuraException {
		s_logger.debug("disable WPA Supplicant");
		killAll();
	}

	/**
	 * Stops all instances of wpa_supplicant
	 * 
	 * @throws Exception
	 */
	public static void killAll() throws KuraException {
		Process proc = null;
		try {
			// kill wpa_supplicant
			s_logger.debug("stopping wpa_supplicant");
			
			if (m_wpaSupplicant != null) {
				if ((m_wpaSupplicant.m_handle != null)
						&& !m_wpaSupplicant.m_handle.isDone()) {
					try {
						Thread.sleep(200);
					} catch (Exception e) {
					}
					m_wpaSupplicant.m_handle.cancel(true);
					m_wpaSupplicant.m_handle = null;
				}
			}
			
			proc = ProcessUtil.exec("killall wpa_supplicant");
			proc.waitFor();
			Thread.sleep(1000);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
		finally {
			ProcessUtil.destroy(proc);
		}
	}
	
	/**
	 * Reports wpa_supplicant configuration file
	 * 
	 * @return full path to wpa_supplicant configuration file {@link String}
	 */
	public static String getConfigFilename () {
		return formSupplicantConfigFilename();
	}

	/**
	 * Reports if wpa_supplicant is running
	 * 
	 * @return {@link boolean}
	 */
	public boolean isEnabled() throws KuraException {
		try {
			// Check if wpa_supplicant is running
			int pid = LinuxProcessUtil.getPid(this.formSupplicantCommand());
			return (pid > -1);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}

	/**
	 * Reports if there is an instance of wpa_supplicant running
	 * 
	 * @return {@link boolean}
	 */
	public static boolean hasInstanceRunning() throws KuraException {
		try {
			// Check if wpa_supplicant is running
			int pid = LinuxProcessUtil.getPid("wpa_supplicant");
			return (pid > -1);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals (Object obj) {
		
		if (!(obj instanceof WpaSupplicant)) {
			return false;
		}
		
		WpaSupplicant supplicant = (WpaSupplicant)obj;
		
		s_logger.debug("comparing " + m_wpaSupplicant.hashCode() + " with " + supplicant.hashCode());
			
		if (!m_wpaSupplicant.m_iface.equals(supplicant.m_iface)) {
			s_logger.debug("current supplicant doesn't match config file: ifaceName " + this.m_iface + ":" + supplicant.m_iface);
			return false;
		}
		if (!m_wpaSupplicant.m_essid.equals(supplicant.m_essid)) {
			s_logger.debug("current supplicant doesn't match config file: ssid");
			return false;
		}
		if (!m_wpaSupplicant.m_driver.equals(supplicant.m_driver)) {
			s_logger.debug("current supplicant doesn't match config file: driver");
			return false;
		}
		if (m_wpaSupplicant.m_pairwiseCiphers != supplicant.m_pairwiseCiphers) {
			s_logger.debug("current supplicant doesn't match config file: pairwiseCiphers");
			return false;
		}
		if (m_wpaSupplicant.m_groupCiphers != supplicant.m_groupCiphers) {
			s_logger.debug("current supplicant doesn't match config file: groupCiphers");
			return false;
		}
		if (m_wpaSupplicant.m_mode != supplicant.m_mode) {
			s_logger.debug("current supplicant doesn't match config file: mode");
			return false;
		}
		if (m_wpaSupplicant.m_security != supplicant.m_security) {
			s_logger.debug("current supplicant doesn't match config file: security");
			return false;
		}
		if (!m_wpaSupplicant.m_passwd.equals(supplicant.m_passwd)) {
			s_logger.debug("current supplicant doesn't match config file: password");
			return false;
		}
		if (m_wpaSupplicant.m_scanChannels.length == supplicant.m_scanChannels.length) {
			for (int i = 0; i < this.m_scanChannels.length; i++) {
				if (this.m_scanChannels[i] != supplicant.m_scanChannels[i]) {
					s_logger.debug("current supplicant doesn't match config file: channels");
					return false;
				}
			}
		} else {
			s_logger.debug("current supplicant doesn't match config file: channels");
			return false;
		}
		
		if (((m_wpaSupplicant.m_bgscan != null) && (supplicant.m_bgscan == null)) || 
		((m_wpaSupplicant.m_bgscan == null) && (supplicant.m_bgscan != null))) {
			return false;
		}
		
		if (((m_wpaSupplicant.m_bgscan != null) && (supplicant.m_bgscan != null))) {
			if(!m_wpaSupplicant.m_bgscan.equals(supplicant.m_bgscan)) {
				s_logger.debug("current supplicant doesn't match config file: bgscan");
				return false;
			}
		}
		
		s_logger.debug("current supplicant matches config file");
		return true;
	}
	
	public String getInterface() {
	    return m_iface;
	}
	
	public void setInterface(String iface) {
	    m_iface = iface;
	}
	
	public String getDriver() {
	    return m_driver;
	}
	
	public void setDriver(String driver) {
	    m_driver = driver;
	}
	
	public WifiMode getMode() {
	    return m_mode;
	}
	
	public void setMode(WifiMode mode) {
	    m_mode = mode;
	}
	
	public String getSSID() {
		return m_essid;
	}
	
	public void setSSID(String ssid) {
	    m_essid = ssid;
	}
	
	public String getPassword() {
		return m_passwd;
	}
	
	public void setPassword(String password) {
	    m_passwd = password;
	}
	
	public WifiSecurity getWifiSecurity () {
		return m_security;
	}
	
	public void setWifiSecurity(WifiSecurity security) {
	    m_security = security;
	}

	public WifiCiphers getPairwiseCiphers() {
		return m_pairwiseCiphers;
	}
	
	public void setPairwiseCiphers(WifiCiphers ciphers) {
	    m_pairwiseCiphers = ciphers;
	}
	
	public WifiCiphers getGroupCiphers() {
		return m_groupCiphers;
	}
	
	public void setGroupCiphers(WifiCiphers ciphers) {
	    m_groupCiphers = ciphers; 
	}
	
	public int [] getChannels() {
		return m_scanChannels;
	}
	
	public void setChannels(int[] channels) {
	    m_scanChannels = channels;
	}
	
	public WifiBgscan getBgscan () {
		return m_bgscan;
	}
	
	public void setBgscan(WifiBgscan bgscan) {
	    m_bgscan = bgscan;
	}

	private void monitor() {

		try {
			m_status = new WpaSupplicantStatus(m_iface);
			String state = m_status.getWpaState();
			if (state == null) {
				s_logger.warn("Failed to obtain 'state' info from wpa_supplicant");
				return;
			}
			
			if (m_mode == WifiMode.INFRA) {
				if (state.compareTo("INTERFACE_DISABLED") == 0) {
					if (m_ifaceDisabledCnt > 5) {
						s_logger.debug("WPA Supplicant reports 'INTERFACE_DISABLED' state .. Restarting ..");
						this.disable();
						this.enable();
						m_ifaceDisabledCnt = 0;
						m_connState = CONN_NONE;
					} else {
						s_logger.debug("WPA Supplicant reports 'INTERFACE_DISABLED' state");
						m_ifaceDisabledCnt++;
					}

					return;
				}

				m_ifaceDisabledCnt = 0;
				if (state.compareTo("COMPLETED") == 0) {
					if (m_connState < CONN_L2) {
						m_connState = CONN_L2;
						s_logger.info("WiFi (L2) is up");
					}

					if (m_status.getIpAddress() != null) {
						if (m_connState < CONN_L3) {
							s_logger.info("WiFi (L3) is up");
							
							if (m_routeService != null) {
								RouteConfig[] routes = m_routeService.getRoutes();
								boolean gwFound = false;
								for (RouteConfig route : routes) {
									if (route.getInterfaceName().equals(m_iface)
											&& route.getDestination().equals("0.0.0.0")) {
										gwFound = true;
										break;
									}
								}
								if (!gwFound) {
									s_logger.info("Default gateway is not installed, bringing interface "
											+ m_iface + " up");
									LinuxProcessUtil.start("ifup " + m_iface + "\n");
								}
							}
						}
						m_connState = CONN_L3;
					} else {
						m_connState = CONN_L2;
						s_logger.info("bringing interface " + m_iface + " up");
						LinuxProcessUtil.start("ifup " + m_iface + "\n");
					}
				} else {
					if (m_connState > CONN_NONE) {
						s_logger.info("WiFi (L2) is down");
					}
					m_connState = CONN_NONE;
				}
			} else if (m_mode == WifiMode.ADHOC) {
				if (state.compareTo("COMPLETED") == 0) {
					if (LinuxNetworkUtil.getCurrentIpAddress(m_iface) == null) {
						s_logger.info("bringing interface " + m_iface + " up");
						LinuxProcessUtil.start("ifup " + m_iface + "\n");
					}
					return;
				}
				if (this.m_ifaceDisabledCnt > 30) {
					this.m_ifaceDisabledCnt = 0;
					this.disable();
					Thread.sleep(1000);
					this.enable();
				} else {
					this.m_ifaceDisabledCnt++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Save the current values to the config file
	 */
	public void saveConfig() throws KuraException {
		
		s_logger.debug("saveConfig()");
		
		try {
			this.generateWpaSupplicantConf();
			this.m_isConfigured = true;
		} catch (Exception e) {
			this.m_isConfigured = false;
			s_logger.error("failed to configure wpa_supplicant");
			e.printStackTrace();
			throw KuraException.internalError(e);
		}
	}

	/*
	 * This method generates wpa_supplicant configuration file
	 */
	private void generateWpaSupplicantConf() throws Exception {

		s_logger.debug("generateWpaSupplicantConf()");
		
		File outputFile = new File(this.m_configFilename);
		InputStream is = null;
		String fileAsString = null;

		if (this.m_security == WifiSecurity.SECURITY_WEP) {
			
			if (m_mode == WifiMode.INFRA) {
				is = this.getClass().getResourceAsStream(
						"/src/main/resources/wifi/wpasupplicant.conf_wep");
				fileAsString = readInputStreamAsString(is);
			} else if (m_mode == WifiMode.ADHOC) {
				is = this.getClass().getResourceAsStream(
						"/src/main/resources/wifi/wpasupplicant.conf_adhoc_wep");
				fileAsString = readInputStreamAsString(is);
				fileAsString = fileAsString
						.replaceFirst("KURA_FREQUENCY", Integer.toString(this
								.getChannelFrequencyMHz(this.m_scanChannels[0])));
			} else {
				throw KuraException
						.internalError("Failed to generate wpa_supplicant.conf -- Invalid mode: "
								+ this.m_mode);
			}

			// replace the necessary components
			fileAsString = fileAsString.replaceFirst("KURA_MODE",
					Integer.toString(this.getSupplicantMode()));

			if (this.m_essid != null) {
				fileAsString = fileAsString.replaceFirst("KURA_ESSID",
						this.m_essid);
			} else {
				throw KuraException.internalError("the essid can not be null");
			}
			if (this.m_passwd != null) {
				if (this.m_passwd.length() == 10) {
					// check to make sure it is all hex
					try {
						Long.parseLong(this.m_passwd, 16);
					} catch (Exception e) {
						throw KuraException
								.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
					}

					// since we're here - save the password
					fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY",
							this.m_passwd);
				} else if (this.m_passwd.length() == 26) {
					String part1 = this.m_passwd.substring(0, 13);
					String part2 = this.m_passwd.substring(13);

					try {
						Long.parseLong(part1, 16);
						Long.parseLong(part2, 16);
					} catch (Exception e) {
						throw KuraException
								.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
					}

					// since we're here - save the password
					fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY",
							m_passwd);
				} else if (this.m_passwd.length() == 32) {
					String part1 = this.m_passwd.substring(0, 10);
					String part2 = this.m_passwd.substring(10, 20);
					String part3 = this.m_passwd.substring(20);
					try {
						Long.parseLong(part1, 16);
						Long.parseLong(part2, 16);
						Long.parseLong(part3, 16);
					} catch (Exception e) {
						throw KuraException
								.internalError("the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f");
					}

					// since we're here - save the password
					fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY",
							m_passwd);
				} else if ((this.m_passwd.length() == 5)
						|| (this.m_passwd.length() == 13)
						|| (this.m_passwd.length() == 16)) {

					// 5, 13, or 16 ASCII characters
					this.m_passwd = this.toHex(this.m_passwd);

					// since we're here - save the password
					fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY",
							m_passwd);
				} else {
					throw KuraException
							.internalError("the WEP key (passwd) must be 10, 26, or 32 HEX characters in length");
				}
			} else {
				throw KuraException.internalError("the passwd can not be null");
			}
			
			if (this.m_bgscan != null) {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN",
						this.m_bgscan.toString());
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN", "");
			}

			if(m_scanChannels != null && m_scanChannels.length > 0) {
				fileAsString = fileAsString.replaceFirst("KURA_SCANFREQ",
						this.getScanFrequenciesMHz(this.m_scanChannels));
			} else {
				fileAsString = fileAsString.replaceFirst("scan_freq=KURA_SCANFREQ",
						"");
			}

		} else if ((this.m_security == WifiSecurity.SECURITY_WPA)
				|| (this.m_security == WifiSecurity.SECURITY_WPA2)) {

			if (m_mode == WifiMode.INFRA) {
				is = this.getClass().getResourceAsStream(
						"/src/main/resources/wifi/wpasupplicant.conf_wpa");
				fileAsString = readInputStreamAsString(is);
			} else if (m_mode == WifiMode.ADHOC) {
				is = this.getClass().getResourceAsStream(
						"/src/main/resources/wifi/wpasupplicant.conf_adhoc_wpa");
				fileAsString = readInputStreamAsString(is);
				fileAsString = fileAsString
						.replaceFirst("KURA_FREQUENCY", Integer.toString(this
								.getChannelFrequencyMHz(this.m_scanChannels[0])));
				fileAsString = fileAsString
						.replaceFirst("KURA_PAIRWISE", "NONE");
			} else {
				throw KuraException
						.internalError("Failed to generate wpa_supplicant.conf -- Invalid mode: "
								+ this.m_mode);
			}

			// replace the necessary components
			fileAsString = fileAsString.replaceFirst("KURA_MODE",
					Integer.toString(this.getSupplicantMode()));

			if (this.m_essid != null) {
				fileAsString = fileAsString.replaceFirst("KURA_ESSID",
						this.m_essid);
			} else {
				throw KuraException.internalError("the essid can not be null");
			}
			if (this.m_passwd != null && m_passwd.trim().length() > 0) {
				if ((this.m_passwd.length() < 8)
						|| (this.m_passwd.length() > 63)) {
					throw KuraException
							.internalError("the WPA passphrase (passwd) must be between 8 (inclusive) and 63 (inclusive) characters in length: " + m_passwd);
				} else {
					fileAsString = fileAsString.replaceFirst("KURA_PASSPHRASE",
							this.m_passwd.trim());
				}
			} else {
				throw KuraException.internalError("the passwd can not be null");
			}
			
			if(m_security == WifiSecurity.SECURITY_WPA) {
				fileAsString = fileAsString.replaceFirst("KURA_PROTO", "WPA");
			} else if(m_security == WifiSecurity.SECURITY_WPA2) {
				fileAsString = fileAsString.replaceFirst("KURA_PROTO", "RSN");
			} else if(m_security == WifiSecurity.SECURITY_WPA_WPA2) {
				fileAsString = fileAsString.replaceFirst("KURA_PROTO", "WPA RSN");
			}

			if (this.m_pairwiseCiphers != null) {
				fileAsString = fileAsString.replaceFirst("KURA_PAIRWISE",
						WifiCiphers.toString(this.m_pairwiseCiphers));
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_PAIRWISE",
						"CCMP TKIP");
			}

			if (this.m_groupCiphers != null) {
				fileAsString = fileAsString.replaceFirst("KURA_GROUP",
						WifiCiphers.toString(this.m_groupCiphers));
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_GROUP",
						"CCMP TKIP");
			}
			
			if (this.m_bgscan != null) {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN",
						this.m_bgscan.toString());
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN", "");
			}

			if(m_scanChannels != null && m_scanChannels.length > 0) {
				fileAsString = fileAsString.replaceFirst("KURA_SCANFREQ",
						this.getScanFrequenciesMHz(this.m_scanChannels));
			} else {
				fileAsString = fileAsString.replaceFirst("scan_freq=KURA_SCANFREQ",
						"");
			}
			
		} else if (this.m_security == WifiSecurity.SECURITY_NONE) {

			if (m_mode == WifiMode.INFRA) {
				is = this.getClass().getResourceAsStream(
						"/src/main/resources/wifi/wpasupplicant.conf_open");
				fileAsString = readInputStreamAsString(is);
			} else if (m_mode == WifiMode.ADHOC) {
				is = this.getClass().getResourceAsStream(
						"/src/main/resources/wifi/wpasupplicant.conf_adhoc_open");
				fileAsString = readInputStreamAsString(is);
				fileAsString = fileAsString
						.replaceFirst("KURA_FREQUENCY", Integer.toString(this
								.getChannelFrequencyMHz(this.m_scanChannels[0])));
			} else {
				throw KuraException
						.internalError("Failed to generate wpa_supplicant.conf -- Invalid mode: "
								+ this.m_mode);
			}

			// replace the necessary components
			fileAsString = fileAsString.replaceFirst("KURA_MODE",
					Integer.toString(this.getSupplicantMode()));

			if (this.m_essid != null) {
				fileAsString = fileAsString.replaceFirst("KURA_ESSID",
						this.m_essid);
			} else {
				throw KuraException.internalError("the essid can not be null");
			}
			
			if (this.m_bgscan != null) {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN",
						this.m_bgscan.toString());
			} else {
				fileAsString = fileAsString.replaceFirst("KURA_BGSCAN", "");
			}

			if(m_scanChannels != null && m_scanChannels.length > 0) {
				fileAsString = fileAsString.replaceFirst("KURA_SCANFREQ",
						this.getScanFrequenciesMHz(this.m_scanChannels));
			} else {
				fileAsString = fileAsString.replaceFirst("scan_freq=KURA_SCANFREQ",
						"");
			}

		} else {
			s_logger.error("unsupported security type: " + this.m_security);
			throw KuraException.internalError("unsupported security type: "
					+ this.m_security);
		}
		
		// Remove the 'wheel' group assignment for Yocto image
		/*
		if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())) {
			fileAsString = fileAsString.replaceFirst("ctrl_interface_group=wheel", "#ctrl_interface_group=wheel");
		}
		*/
		
		// everything is set and we haven't failed - write the file
		this.copyFile(fileAsString, outputFile);
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
	 * This method sets permissions to wpa_supplicant configuration file
	 */
	private void setPermissions(String fileName) throws KuraException {
		Process procChmod = null;
		Process procDos = null;
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
	 * This method reads supplied input stream into a string
	 */
	private static String readInputStreamAsString(InputStream is)
			throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while (result != -1) {
			byte b = (byte) result;
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

		StringBuffer hex = new StringBuffer(2 * raw.length);
		for (int i = 0; i < raw.length; i++) {
			hex.append(HEXES.charAt((raw[i] & 0xF0) >> 4)).append(
					HEXES.charAt((raw[i] & 0x0F)));
		}
		return hex.toString();
	}

	/*
	 * This method forms wpa_supplicant command
	 */
	private String formSupplicantCommand() {

		StringBuffer sb = new StringBuffer();
		sb.append("wpa_supplicant -B -D ");
		sb.append(this.m_driver);
		sb.append(" -i ");
		sb.append(this.m_iface);
		sb.append(" -c ");
		sb.append(m_configFilename);

		return sb.toString();
	}

	/*
	 * This method forms wpa_supplicant configuration directory string
	 */
	private static String formSupplicantConfigDirectory() {

		StringBuffer sb = new StringBuffer();
		// sb.append("/tmp/.kura/");
		// sb.append(WpaSupplicant.class.getPackage().getName());
		sb.append("/etc/");

		return sb.toString();
	}

	/*
	 * This method forms wpa_supplicant configuration filename
	 */
	private static String formSupplicantConfigFilename() {

		StringBuffer sb = new StringBuffer();
		sb.append(WpaSupplicant.formSupplicantConfigDirectory());
		sb.append("/wpa_supplicant.conf");

		return sb.toString();
	}

	private Collection<String> getSupportedDrivers() throws Exception {

		s_logger.debug("getting drivers supported by wpa_supplicant ...");
		Collection<String> drivers = new HashSet<String>();
		BufferedReader br = null;
		Process proc = null;
		try {
			proc = ProcessUtil.exec("wpa_supplicant");
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			boolean fDrivers = false;

			while ((line = br.readLine()) != null) {
				if (fDrivers) {
					if (line.startsWith("options:")) {
						fDrivers = false;
						break;
					} else {
						String[] asDriver = line.split("=");
						drivers.add(asDriver[0].trim());
					}
				} else {
					if (line.startsWith("drivers:")) {
						fDrivers = true;
					}
				}
			}
		}
		finally {
			ProcessUtil.destroy(proc);
		}
		return drivers;
	}

	private String getScanFrequenciesMHz(int[] channels) {

		StringBuffer sbFrequencies = new StringBuffer();
		if (channels != null && channels.length > 0) {
			for (int i = 0; i < channels.length; i++) {
				int freq = getChannelFrequencyMHz(channels[i]);
				if(freq != -1) {
					sbFrequencies.append(freq);
					if (i < (channels.length - 1)) {
						sbFrequencies.append(' ');
					}
				}
			}
		} else {
			for (int i = 1; i <= 11; i++) {
				sbFrequencies.append(this.getChannelFrequencyMHz(i));
				if (i < 11) {
					sbFrequencies.append(' ');
				}
			}
		}

		return sbFrequencies.toString().trim();
	}
	
	private static Properties parseConfigFile () throws KuraException {
		
		Properties props = null;
		
		try {
			File wpaConfigFile = new File(formSupplicantConfigFilename());
			if (wpaConfigFile.exists()) {
	
				// Read into a string
				BufferedReader br = new BufferedReader(
						new FileReader(wpaConfigFile));
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line).append("\n");
				}
				br.close();
	
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

		return props;
	}

	private int getChannelFrequencyMHz(int channel) {

		int frequency = -1;
		if ((channel >= 1) && (channel <= 13)) {
			frequency = 2407 + channel * 5;
		} else {
			s_logger.error("Invalid channel specified.  Must be between 1 and 13 (inclusive) but was set to: " + channel);
		}
		return frequency;
	}
	
	private static int frequencyMhz2Channel(int frequency) {
		
		int channel = (frequency - 2407)/5;
		return channel;
	}

	private int getSupplicantMode() {

		int supplicantMode = MODE_INFRA;
		if (m_mode == WifiMode.INFRA) {
			supplicantMode = MODE_INFRA;
		} else if (m_mode == WifiMode.ADHOC) {
			supplicantMode = MODE_IBSS;
		} else if (m_mode == WifiMode.MASTER) {
			supplicantMode = MODE_AP;
		}
		return supplicantMode;
	}
}
