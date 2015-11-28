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
package org.eclipse.kura.linux.test.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.eclipse.kura.linux.net.wifi.WpaSupplicant;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WpaSupplicantTest extends TestCase {

	private static final Logger s_logger = LoggerFactory.getLogger(WpaSupplicantTest.class);
	
	private static CountDownLatch dependencyLatch = new CountDownLatch(0);	// initialize with number of dependencies
	
	private static WpaSupplicant s_wpaSupplicant;

	private static final String TMPDIR = "/tmp/" + WpaSupplicantTest.class.getName();
	private static String oldConfigBackup = TMPDIR + "/wpasupplicant.conf.backup";
	
	private static final String m_iface = "wlan0";
	//private static final String m_driver = WifiOptions.WIFI_MANAGED_DRIVER_WEXT;
//	private static final String m_driver = WifiOptions.WIFI_MANAGED_DRIVER_NL80211;
	private static final String m_ssid = "WpaSupplicantTest";
	private static final String m_password = "123WpaSupplicantTestPassword";

	
	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@BeforeClass
	public void setUp() {
		File tmpDir = new File(TMPDIR);
		tmpDir.mkdirs();
		
		// Wait for OSGi dependencies
		try {
			dependencyLatch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail("OSGi dependencies unfulfilled");
			System.exit(1);
		}
		
				
		
		// Backup current wpa_supplicant config
		try {
			s_logger.info("Backing up current wpa_supplicant config to " + oldConfigBackup);
						
			// Read current config from file
			File oldConfig = new File(WpaSupplicant.getConfigFilename());
			StringBuffer data = new StringBuffer();

			if(oldConfig.exists()) {
				FileReader fr = new FileReader(oldConfig);
				
				int in;
				while( (in = fr.read()) != -1) {
					data.append((char)in);
				}
				fr.close();
			}

			// Write current config to file
			FileOutputStream fos = new FileOutputStream(oldConfigBackup);
			PrintWriter pw = new PrintWriter(fos);
			pw.write(data.toString());
			pw.flush();
			fos.getFD().sync();
			pw.close();
			fos.close();
		} catch (Exception e) {
			fail("Error backing up current wpa_supplicant config");
			System.exit(1);
		}
		
		// Initialize
		try {
			s_wpaSupplicant = WpaSupplicant.getWpaSupplicant(m_iface,
					WifiMode.INFRA, null, m_ssid, WifiSecurity.SECURITY_WPA,
					WifiCiphers.CCMP_TKIP, WifiCiphers.CCMP_TKIP, null, 
					m_password, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testDisable() {
		s_logger.info("Test disable wpa_supplicant");

		try {
			s_wpaSupplicant.disable();
			assertFalse("wpa_supplicant is disabled", s_wpaSupplicant.isEnabled());
		} catch (Exception e) {
			fail("testDisable failed: " + e);
		}
	}
	
	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testConstructor1() {
		s_logger.info("Test wpa_supplicant - Infra WPA");
		
		try {
			s_wpaSupplicant = WpaSupplicant.getWpaSupplicant(m_iface,
					WifiMode.INFRA, null, m_ssid, WifiSecurity.SECURITY_WPA,
					WifiCiphers.CCMP_TKIP, WifiCiphers.CCMP_TKIP, null, 
					m_password, null);
			assertNotNull(s_wpaSupplicant);
			
			s_wpaSupplicant.saveConfig();
			s_logger.debug("config:\n{}", getConfig());
			
			s_wpaSupplicant.enable();
			assertTrue("wpa_supplicant is enabled", s_wpaSupplicant.isEnabled());
			
			String config = this.getConfig();
			assertTrue("config specifies ssid", config.contains("ssid=\"" + m_ssid + "\""));
			assertTrue("config specifies password", config.contains(m_password));
			
			s_logger.info("Disabling wpa_supplicant");
			Thread.sleep(1000);
			s_wpaSupplicant.disable();
			assertFalse("wpa_supplicant is disabled", s_wpaSupplicant.isEnabled());
		} catch (Exception e) {
			e.printStackTrace();
			fail("testEnable failed: " + e);
		}
	}
	
	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testConstructor2() {
		s_logger.info("Test wpa_supplicant - Infra WEP");
		
		try {
			String password = "1234567890";
			String ssid = "TestSSID#2";
			s_wpaSupplicant = WpaSupplicant.getWpaSupplicant(m_iface, WifiMode.INFRA,
					null, ssid, WifiSecurity.SECURITY_WEP, null, null,
					null, password, null);
			assertNotNull(s_wpaSupplicant);
			
			s_wpaSupplicant.saveConfig();
			s_logger.debug("config:\n{}", getConfig());
			
			s_wpaSupplicant.enable();
			assertTrue("wpa_supplicant is enabled",
					s_wpaSupplicant.isEnabled());

			String config = this.getConfig();
			assertTrue("config specifies ssid",
					config.contains("ssid=\"" + ssid + "\""));
			assertTrue("config specifies password",
					config.contains(password));

			s_logger.info("Disabling wpa_supplicant");
			s_wpaSupplicant.disable();
			assertFalse("wpa_supplicant is disabled",
					s_wpaSupplicant.isEnabled());
		} catch (Exception e) {
			e.printStackTrace();
			fail("testEnable failed: " + e);
		}
	}

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testConstructor3() {
		s_logger.info("Test wpa_supplicant - Adhoc none");
		
		try {
			String password = "1234567890Password1234567890";
			String ssid = "Another SSID";
			
			s_wpaSupplicant = WpaSupplicant.getWpaSupplicant(m_iface,
					WifiMode.ADHOC, null, ssid, WifiSecurity.SECURITY_NONE,
					null, null, WpaSupplicant.ALL_CHANNELS, password, null);
			assertNotNull(s_wpaSupplicant);
			
			s_wpaSupplicant.saveConfig();
			s_logger.debug("config:\n{}", getConfig());
			
			s_wpaSupplicant.enable();
			assertTrue("wpa_supplicant is enabled", s_wpaSupplicant.isEnabled());
			
			String config = this.getConfig();
			assertTrue("config specifies ssid", config.contains("ssid=\"" + ssid + "\""));
			
			s_logger.info("Disabling wpa_supplicant");
			s_wpaSupplicant.disable();
			
			Thread.sleep(1000);
			assertFalse("wpa_supplicant is disabled", s_wpaSupplicant.isEnabled());
		} catch (Exception e) {
			e.printStackTrace();
			fail("testEnable failed: " + e);
		}
	}
	

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@AfterClass()
	public void tearDown() {
		
		if (s_wpaSupplicant != null) {
			try {
				s_wpaSupplicant.disable();
			} catch (Exception e) {
				// continue anyway
			}
		}
		
		// Restore old wpa_supplicant config
		try {
			s_logger.info("Restoring wpa_supplicant config from " + oldConfigBackup);
			
			// Read current config from file
			File backupFile = new File(oldConfigBackup);
			StringBuffer data = new StringBuffer();
			
			if(backupFile.exists()) {
				FileReader fr = new FileReader(backupFile);
				
				int in;
				while( (in = fr.read()) != -1) {
					data.append((char)in);
				}
				fr.close();
			}
			
			// Write backup config to file
			FileOutputStream fos = new FileOutputStream(WpaSupplicant.getConfigFilename());
			PrintWriter pw = new PrintWriter(fos);
			pw.write(data.toString());
			pw.flush();
			fos.getFD().sync();
			pw.close();
			fos.close();	
		} catch (Exception e) {
			fail("Error restoring wpa_supplicant config");
		}
	}
	
	private String getConfig() {
		// Read current config from file
		File configFile = new File(WpaSupplicant.getConfigFilename());
		StringBuffer data = new StringBuffer();
		
		try {
			if(configFile.exists()) {
				FileReader fr = new FileReader(configFile);
				
				int in;
				while( (in = fr.read()) != -1) {
					data.append((char)in);
				}
				fr.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return data.toString();
	}

}
