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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.wifi.WifiPassword;
import org.eclipse.kura.net.wifi.WifiSecurity;
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

	public static void start(String ifaceName, Password passkey, WifiSecurity wifiSecurity) throws KuraException {
		SafeProcess proc = null;
		try {
			if(HostapdManager.isRunning(ifaceName)) {
				stop(ifaceName);
			}
			
			//start hostapd
			String launchHostapdCommand = generateStartCommand(ifaceName, passkey, wifiSecurity);
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
			if (!s_isIntelEdison) {
				// delete temporary hostapd.conf that contains passkey
				File tmpHostapdConfigFile = new File(getHostapdConfigFileName(ifaceName, "/tmp"));
				if (tmpHostapdConfigFile.exists()) {
					tmpHostapdConfigFile.delete();
				}
			}
		}
	}

	public static void stop(String ifaceName) throws KuraException {
		SafeProcess proc = null;
		try {
			//kill hostapd
			s_logger.debug("stopping hostapd");
			String cmd = generateStopCommand(ifaceName);
			if ((cmd != null) && !cmd.isEmpty()) {
				proc = ProcessUtil.exec(cmd);
				proc.waitFor();
				Thread.sleep(1000);
			}
		} catch(Exception e) {
			throw KuraException.internalError(e);
		}
		finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}
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
			String [] tokens = {getHostapdConfigFileName(ifaceName, "/tmp")};
			int pid = LinuxProcessUtil.getPid("hostapd", tokens);
			s_logger.trace("getPid() :: pid={}", pid);
			return pid;
		} catch (Exception e) {
			throw KuraException.internalError(e);
		}
	}

	private static String generateStartCommand(String ifaceName,
			Password passkey, WifiSecurity wifiSecurity)
			throws KuraException {
		StringBuilder cmd = new StringBuilder();
		if (s_isIntelEdison) {
			cmd.append("systemctl start hostapd");
		} else {
			File configFile = generateHostapdConfigFile(ifaceName, passkey, wifiSecurity);
			cmd.append(HOSTAPD_EXEC).append(" -B ").append(configFile.getAbsolutePath());
		}
		return cmd.toString();
	}

	private static String generateStopCommand(String ifaceName) throws KuraException {
		StringBuilder cmd = new StringBuilder();
		if (s_isIntelEdison) {
			cmd.append("systemctl stop hostapd");
		} else {
			int pid;
			try {
				pid = getPid(ifaceName);
				//cmd.append("killall hostapd");
				if (pid > 0) {
					cmd.append("kill -9 ").append(pid);
				}
			} catch (KuraException e) {
				throw KuraException.internalError(e);
			}
		}
		return cmd.toString();
	}
	
	public static String getHostapdConfigFileName(String ifaceName) {
		return getHostapdConfigFileName(ifaceName, "/etc");
	}
	
	private static String getHostapdConfigFileName(String ifaceName, String folder) {
		StringBuilder sb = new StringBuilder();
		if (s_isIntelEdison) {
			sb.append("/etc/hostapd/hostapd.conf");
		} else {
			sb.append(folder).append('/').append("hostapd-").append(ifaceName).append(".conf");
		}
		return sb.toString();
	}
	
	private static File generateHostapdConfigFile(String ifaceName, Password passkey, WifiSecurity wifiSecurity) throws KuraException {
		File retConfigFile = new File(getHostapdConfigFileName(ifaceName, "/tmp"));
		File configFile = new File(getHostapdConfigFileName(ifaceName));
		if(!configFile.exists()) {
			throw KuraException.internalError("Config file does not exist: " + configFile.getAbsolutePath());
		}
		FileReader fr = null;
		FileWriter fw = null;
		BufferedReader br = null;
		PrintWriter pw = null;	
		try {
			fr = new FileReader(configFile);
			br = new BufferedReader(fr);
			fw = new FileWriter(retConfigFile);
			pw = new PrintWriter(fw);
			String line = null;
            while ((line = br.readLine()) != null) {
               line = line.trim();
               if (!(line.startsWith("#") || line.isEmpty())) {
            	   if (line.startsWith("wep_key") || line.startsWith("wpa_passphrase")) {
            		   int ind = line.indexOf('=');
            		   if (ind > 0) {
            			   WifiPassword wifiPassword = new WifiPassword(passkey.toString());
            			   wifiPassword.validate(wifiSecurity);
            			   pw.println(line.substring(0, ind+1).concat(passkey.toString()));
            		   }
            	   } else {
            		   pw.println(line);
            	   }
               }
            }
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (fr != null) {
					fr.close();
				}
				if (pw != null) {
					pw.close();
				}
				if (fw != null) {
					fw.close();
				}
			} catch (IOException e) {
				s_logger.error("Failed to close file stream - {}", e);
			}
		}
		return retConfigFile;
	}
}
