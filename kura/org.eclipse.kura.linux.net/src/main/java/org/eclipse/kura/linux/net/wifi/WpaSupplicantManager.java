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

import java.io.File;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.util.LinuxProcessUtil;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpaSupplicantManager {
	
	private static Logger s_logger = LoggerFactory.getLogger(WpaSupplicantManager.class);
	
	private static final File CONFIG_FILE = new File("/etc/wpa_supplicant.conf");
	private static final File TEMP_CONFIG_FILE = new File("/tmp/wpa_supplicant.conf");

	private static String m_driver = null;
	private static String m_interfaceName = null;
	
	public static void start(String interfaceName, final WifiMode mode, String driver) throws KuraException {
		start (interfaceName, mode, driver, CONFIG_FILE);
	}
	
	public static void startTemp(String interfaceName, final WifiMode mode, String driver) throws KuraException {
		start (interfaceName, mode, driver, TEMP_CONFIG_FILE);
	}
	
	private static synchronized void start(String interfaceName, final WifiMode mode, String driver, File configFile) throws KuraException {
		
		s_logger.debug("enable WPA Supplicant");
		
		Process proc = null;
		try {
			if(WpaSupplicantManager.isRunning()) {
                stop();
            }
			
			m_interfaceName = interfaceName;
			m_driver = driver;

			// start wpa_supplicant
			String wpaSupplicantCommand = formSupplicantCommand(configFile);
			s_logger.debug("starting wpa_supplicant -> " + wpaSupplicantCommand);
			LinuxProcessUtil.start(wpaSupplicantCommand);
		} catch (Exception e) {
			e.printStackTrace();
			throw KuraException.internalError(e);
		}
		finally {
			ProcessUtil.destroy(proc);
		}

	}

	
	/*
	 * This method forms wpa_supplicant command
	 */
	private static String formSupplicantCommand(File configFile) {

		StringBuffer sb = new StringBuffer();
		sb.append("wpa_supplicant -B -D ");
		sb.append(m_driver);
		sb.append(" -i ");
		sb.append(m_interfaceName);
		sb.append(" -c ");
		sb.append(configFile);

		return sb.toString();
	}
	
	/**
	 * Reports if wpa_supplicant is running
	 * 
	 * @return {@link boolean}
	 */
	public static boolean isRunning() throws KuraException {
		try {
			// Check if wpa_supplicant is running
			int pid = LinuxProcessUtil.getPid(formSupplicantCommand(CONFIG_FILE));
			return (pid > -1);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}
	
	public static boolean isTempRunning() throws KuraException {
		try {
			// Check if wpa_supplicant is running
			int pid = LinuxProcessUtil.getPid(formSupplicantCommand(TEMP_CONFIG_FILE));
			return (pid > -1);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}
	
	/**
	 * Stops all instances of wpa_supplicant
	 * 
	 * @throws Exception
	 */
	public static void stop() throws KuraException {
		try {
			// kill wpa_supplicant
			s_logger.debug("stopping wpa_supplicant");
			
			LinuxProcessUtil.start("killall wpa_supplicant");
			if(m_interfaceName != null) {
				LinuxNetworkUtil.disableInterface(m_interfaceName);
			}
			Thread.sleep(1000);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}
}
