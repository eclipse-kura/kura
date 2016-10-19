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
	private static final String HOSTAPD_EXEC = "hostapd";
	
	private static boolean s_isIntelEdison = false;
	static {
		StringBuilder sb = new StringBuilder(KuraConstants.Intel_Edison.getImageName());
		sb.append('_').append(KuraConstants.Intel_Edison.getImageVersion()).append('_').append(KuraConstants.Intel_Edison.getTargetName());
		if(OS_VERSION.equals(sb.toString())) {
			s_isIntelEdison = true;
		}
	}

	public static void start(String ifaceName) throws KuraException {
		SafeProcess proc = null;
		File configFile = new File(getHostapdConfigFileName(ifaceName));      
		if (!configFile.exists()) {      
		    throw KuraException.internalError("Config file does not exist: " + configFile.getAbsolutePath());       
		}
		try {
			if(HostapdManager.isRunning(ifaceName)) {
				stop(ifaceName);
			}
			//start hostapd
			String launchHostapdCommand = generateStartCommand(ifaceName);
			s_logger.info("starting hostapd for the {} interface --> {}", ifaceName, launchHostapdCommand);
			int stat = LinuxProcessUtil.start(launchHostapdCommand);
			if(stat != 0) {
				s_logger.error("failed to start hostapd for the {} interface for unknown reason - errorCode={}", ifaceName, stat);
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

	public static void stop(String ifaceName) throws KuraException {
		SafeProcess proc = null;
		try {
			if (!s_isIntelEdison) {
				int pid = getPid(ifaceName);
				if (pid >= 0) {
		    		s_logger.info("stopping hostapd for the {} interface, pid={}", ifaceName, pid);
		    		
		    		boolean exists = LinuxProcessUtil.stop(pid);
		    		if (!exists) {
		    			s_logger.warn("stopping hostapd for the {} inetrface, pid={} has failed", ifaceName, pid);
		    		} else {
		    			exists = waitProcess(pid, 500, 5000);
		    		}
		    		
		    		if (exists) {
		    			s_logger.info("stopping hostapd for the {} interface - killing pid={}", ifaceName, pid);
		    			exists = LinuxProcessUtil.kill(pid);
		    			if (!exists) {
		    				s_logger.warn("stopping hostapd for the {} interface - killing pid={} has failed", ifaceName, pid);
		    			} else {
		    				exists = waitProcess(pid, 500, 5000);
		    			}
		    		}
		    		
		    		if (exists) {
		    			s_logger.warn("Failed to stop hostapd for the {} interface", ifaceName);
		    		}
				}
			} else {
				proc = ProcessUtil.exec("systemctl stop hostapd");
				proc.waitFor();
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			throw KuraException.internalError(e);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}
	
	// Only call this method after a call to stop or kill.
	// FIXME: this is an utility method that should be moved in a suitable package.
	private static boolean waitProcess(int pid, long poll, long timeout) {
		boolean exists = true;
		try {
			final long startTime = System.currentTimeMillis();
			long now;
			do {
				Thread.sleep(poll);
				exists = LinuxProcessUtil.stop(pid);
				now = System.currentTimeMillis();
			} while (exists && (now - startTime) < timeout);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			s_logger.warn("Interrupted waiting for pid {} to exit", pid);
		}
			
		return exists;
	}

	public static boolean isRunning(String ifaceName) throws KuraException {
		try {
			boolean ret = false;
			if (getPid(ifaceName) > 0) {
				ret = true;
			}
			s_logger.trace("isRunning() :: --> {}", ret);
			return ret;
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}
	
	public static int getPid(String ifaceName) throws KuraException {
		try {
		    String [] tokens = {getHostapdConfigFileName(ifaceName)};
			int pid = LinuxProcessUtil.getPid("hostapd", tokens);
			s_logger.trace("getPid() :: pid={}", pid);
			return pid;
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}

	private static String generateStartCommand(String ifaceName) {
		StringBuilder cmd = new StringBuilder();
		if (s_isIntelEdison) {
			cmd.append("systemctl start hostapd");
		} else {
		    File configFile = new File(getHostapdConfigFileName(ifaceName));
			cmd.append(HOSTAPD_EXEC).append(" -B ").append(configFile.getAbsolutePath());
		}
		return cmd.toString();
	}

	public static String getHostapdConfigFileName(String ifaceName) {
		StringBuilder sb = new StringBuilder();
		if (s_isIntelEdison) {
			sb.append("/etc/hostapd/hostapd.conf");
		} else {
		    sb.append("/etc/hostapd-").append(ifaceName).append(".conf");
		}
		return sb.toString();
	}
}
