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

import java.io.File;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostapdManager {

	private static Logger s_logger = LoggerFactory.getLogger(HostapdManager.class);

	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");

	private static String HOSTAPD_CONFIG_FILE_NAME = null; 
	static {
		if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			HOSTAPD_CONFIG_FILE_NAME = "/etc/hostapd/hostapd.conf";
		} else {
			HOSTAPD_CONFIG_FILE_NAME = "/etc/hostapd.conf";
		}
	}

	private static final File CONFIG_FILE = new File(HOSTAPD_CONFIG_FILE_NAME);
	private static final String HOSTAPD_EXEC = "hostapd";

	public static void start() throws KuraException {
		SafeProcess proc = null;

		if(!CONFIG_FILE.exists()) {
			throw KuraException.internalError("Config file does not exist: " + CONFIG_FILE.getAbsolutePath());
		}

		try {
			if(HostapdManager.isRunning()) {
				stop();
			}

			loadKernelModules();
			
			//start hostapd
			String launchHostapdCommand = generateStartCommand();
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
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}

	public static void stop() throws KuraException {
		SafeProcess proc = null;
		try {
			//kill hostapd
			s_logger.debug("stopping hostapd");
			proc = ProcessUtil.exec(generateStopCommand());
			proc.waitFor();
			Thread.sleep(1000);
		} catch(Exception e) {
			throw KuraException.internalError(e);
		}
		finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}
	}

	public static boolean isRunning() throws KuraException {
		try {
			// Check if hostapd is running
			//int pid = LinuxProcessUtil.getPid(generateCommand());
			String [] tokens = {HOSTAPD_CONFIG_FILE_NAME};
			int pid = LinuxProcessUtil.getPid("hostapd", tokens);
			s_logger.trace("isRunning() :: pid={}", pid);
			return (pid > -1);
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}

	private static String generateStartCommand() {
		StringBuilder cmd = new StringBuilder();
		if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			cmd.append("systemctl start hostapd");
		} else {
			cmd.append(HOSTAPD_EXEC).append(" -B ").append(CONFIG_FILE.getAbsolutePath());
		}
		return cmd.toString();
	}

	private static String generateStopCommand() {
		StringBuilder cmd = new StringBuilder();
		if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			cmd.append("systemctl stop hostapd");
		} else {
			cmd.append("killall hostapd");
		}
		return cmd.toString();
	}

	public static void loadKernelModules() throws KuraException {
		SafeProcess proc = null;
		try{
			if (TARGET_NAME.equals(KuraConstants.ReliaGATE_10_05.getTargetName())) {
				s_logger.debug("--> executing rmmod bcmdhd");
				proc = ProcessUtil.exec("rmmod bcmdhd");
				proc.waitFor();

				s_logger.debug("--> executing modprobe");
				proc = ProcessUtil.exec("modprobe -S 3.12.6 bcmdhd firmware_path=\"/system/etc/firmware/fw_bcm43438a0_apsta.bin\" op_mode=2");
				if(proc.waitFor() != 0) {
					s_logger.error("failed modprobe");
					throw KuraException.internalError("failed modprobe"); 
				}
				Thread.sleep(1000);
			}
		} catch(Exception e) {
			throw KuraException.internalError(e);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}
}
