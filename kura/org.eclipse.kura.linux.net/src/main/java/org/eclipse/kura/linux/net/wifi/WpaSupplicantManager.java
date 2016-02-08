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

import java.io.File;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpaSupplicantManager {

	private static Logger s_logger = LoggerFactory.getLogger(WpaSupplicantManager.class);

	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");

	private static String WPA_CONFIG_FILE_NAME = null;
	static {
		if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			WPA_CONFIG_FILE_NAME = "/etc/wpa_supplicant/wpa_supplicant.conf";
		} else {
			WPA_CONFIG_FILE_NAME = "/etc/wpa_supplicant.conf";
		}
	}

	private static final File CONFIG_FILE = new File(WPA_CONFIG_FILE_NAME);
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

		try {
			if(WpaSupplicantManager.isRunning()) {
				stop();
			}

			m_interfaceName = interfaceName;
			String drv = WpaSupplicant.getDriver(interfaceName);
			if (drv != null) {
				if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
					m_driver = driver;
				} else {
					m_driver =  drv;
				}
			} else {
				m_driver = driver;
			}

			loadKernelModules();

			// start wpa_supplicant
			String wpaSupplicantCommand = formSupplicantStartCommand(configFile);
			s_logger.debug("starting wpa_supplicant -> {}", wpaSupplicantCommand);
			LinuxProcessUtil.start(wpaSupplicantCommand);
		} catch (Exception e) {
			s_logger.error("Exception while enabling WPA Supplicant!", e);
			throw KuraException.internalError(e);
		}
	}


	/*
	 * This method forms wpa_supplicant start command
	 */
	private static String formSupplicantStartCommand(File configFile) {
		StringBuilder sb = new StringBuilder();
		if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			sb.append("systemctl start wpa_supplicant");
		} else {
			sb.append("wpa_supplicant -B -D ");
			sb.append(m_driver);
			sb.append(" -i ");
			sb.append(m_interfaceName);
			sb.append(" -c ");
			sb.append(configFile);
		}

		return sb.toString();
	}

	/*
	 * This method forms wpa_supplicant start command
	 */
	private static String formSupplicantStopCommand() {

		StringBuilder sb = new StringBuilder();
		if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			sb.append("systemctl stop wpa_supplicant");
		} else {
			sb.append("killall wpa_supplicant");
		}

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
			//int pid = LinuxProcessUtil.getPid(formSupplicantCommand(CONFIG_FILE));
			String [] tokens = {"-c " + CONFIG_FILE};
			int pid = LinuxProcessUtil.getPid("wpa_supplicant", tokens);
			return (pid > -1);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}

	public static boolean isTempRunning() throws KuraException {
		try {
			// Check if wpa_supplicant is running
			//int pid = LinuxProcessUtil.getPid(formSupplicantCommand(TEMP_CONFIG_FILE));
			String [] tokens = {"-c " + TEMP_CONFIG_FILE};
			int pid = LinuxProcessUtil.getPid("wpa_supplicant", tokens);
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
			LinuxProcessUtil.start(formSupplicantStopCommand());
			if(m_interfaceName != null) {
				LinuxNetworkUtil.disableInterface(m_interfaceName);
			}
			Thread.sleep(1000);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}

	public static void loadKernelModules() throws KuraException {
		SafeProcess proc = null;

		try {

			if (TARGET_NAME.equals(KuraConstants.ReliaGATE_10_05.getTargetName())) {
				s_logger.debug("--> executing wpa_s rmmod");
				proc = ProcessUtil.exec("rmmod bcmdhd");
				proc.waitFor();

				s_logger.debug("--> executing wpa_s modprobe");
				proc = ProcessUtil.exec("modprobe -S 3.12.6 bcmdhd");
				if(proc.waitFor() != 0) {
					s_logger.error("failed modprobe");
					throw KuraException.internalError("failed modprobe"); 
				}
				Thread.sleep(1000);

				s_logger.debug("--> executing wpa_s ifconfig");
				proc = ProcessUtil.exec("ifconfig wlan0 up");
				if(proc.waitFor() != 0) {
					s_logger.error("failed ifconfig wlan0 up");
					throw KuraException.internalError("failed ifconfig wlan0 up"); 
				}
			}
		} catch (Exception e) {
			s_logger.error("Exception while preparing WPA Supplicant!", e);
			throw KuraException.internalError(e);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}
}
