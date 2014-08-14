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

import org.eclipse.kura.linux.net.wifi.Hostapd;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HostapdTest extends TestCase {

	private static final Logger s_logger = LoggerFactory.getLogger(HostapdTest.class);
	
	private static CountDownLatch dependencyLatch = new CountDownLatch(0);	// initialize with number of dependencies
	
	private static Hostapd s_hostapd;

	private static final String TMPDIR = "/tmp/" + HostapdTest.class.getName();
	private static String oldConfigBackup = TMPDIR + "/hostapd.conf.backup";
	
	private static final String m_iface = "wlan0";
	private static final String m_ssid = "HostapdTest";
	private static final int m_channel = 10;
	private static final String m_password = "123HostapdTestPassword";

	
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
				
		
		// Backup current hostapd config
		try {
			s_logger.info("Backing up current hostapd config to " + oldConfigBackup);
						
			// Read current config from file
			File oldConfig = new File(Hostapd.getConfigFilename());
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
			fail("Error backing up current hostapd config");
			System.exit(1);
		}
		
		// Initialize
		try {
			s_hostapd = Hostapd.getHostapd(m_iface, null, m_ssid,
					WifiRadioMode.RADIO_MODE_80211g, m_channel,
					WifiSecurity.SECURITY_WPA, m_password);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testDisable() {
		s_logger.info("Test disable hostapd");

		try {
			s_hostapd.disable();
			assertFalse("hostapd is disabled", s_hostapd.isEnabled());
		} catch (Exception e) {
			fail("testDisable failed: " + e);
		}
	}
	
	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testEnable() {
		s_logger.info("Test enable hostapd");
		
		try {
			s_hostapd.enable();
			assertTrue("hostapd is enabled", s_hostapd.isEnabled());
			
			String config = this.getConfig();
			assertTrue("config specifies interface", config.contains("interface=" + m_iface));
			assertTrue("config specifies ssid", config.contains("ssid=" + m_ssid));
			assertTrue("config specifies channel", config.contains("channel=" + m_channel));
			assertTrue("config specifies password", config.contains(m_password));
			
			s_logger.info("Disabling hostapd");
			s_hostapd.disable();
			assertFalse("hostapd is disabled", s_hostapd.isEnabled());
		} catch (Exception e) {
			fail("testEnable failed: " + e);
		}
	}
	
	

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@AfterClass()
	public void tearDown() {
		
		if (s_hostapd != null) {
			try {
				s_hostapd.disable();
			} catch (Exception e) {
				// continue anyway
			}
		}
		
		// Restore old hostapd config
		try {
			s_logger.info("Restoring hostapd config from " + oldConfigBackup);
			
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
			FileOutputStream fos = new FileOutputStream(Hostapd.getConfigFilename());
			PrintWriter pw = new PrintWriter(fos);
			pw.write(data.toString());
			pw.flush();
			fos.getFD().sync();
			pw.close();
			fos.close();
		} catch (Exception e) {
			fail("Error restoring hostapd config");
		}
	}
	
	private String getConfig() {
		// Read current config from file
		File configFile = new File(Hostapd.getConfigFilename());
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
