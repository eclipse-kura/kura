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
/*
* Copyright (c) 2013 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.linux.net.dhcp;

import java.io.File;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpServerManager {

	private static final Logger s_logger = LoggerFactory.getLogger(DhcpServerManager.class);
	
	private static final String FILE_DIR = "/etc/";
	private static final String PID_FILE_DIR = "/var/run/";
	
	
	public static boolean isRunning(String interfaceName) throws KuraException {
		try {
			// Check if dhcpd is running
			int pid = LinuxProcessUtil.getPid(DhcpServerManager.formDhcpdCommand(interfaceName));
			return (pid > -1);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}	
	
	public static boolean enable(String interfaceName) throws KuraException {
		try {
			// Check if dhcpd is running
			if(DhcpServerManager.isRunning(interfaceName)) {
				// If so, disable it
				s_logger.error("DHCP server is already running for " + interfaceName + ", bringing it down...");
				DhcpServerManager.disable(interfaceName);
			}
			// Start dhcpd
			File configFile = new File(DhcpServerManager.getConfigFilename(interfaceName));
			if(configFile.exists()) {
			    // FIXME:MC This leads to a process leak
    			if (LinuxProcessUtil.startBackground(DhcpServerManager.formDhcpdCommand(interfaceName), false) == 0) {
    				s_logger.debug("DHCP server started.");
    				return true;
    			}
			} else {
			    s_logger.debug("Can't start DHCP server, config file does not exist: " + configFile.getAbsolutePath());
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return false;
	}

	public static boolean disable(String interfaceName) throws KuraException {
        s_logger.debug("Disable DHCP server for " + interfaceName);

		try {
			// Check if dhcpd is running
			int pid = LinuxProcessUtil.getPid(DhcpServerManager.formDhcpdCommand(interfaceName));
			if(pid > -1) {
				// If so, kill it.
				if (LinuxProcessUtil.stop(pid)) {
				    DhcpServerManager.removePidFile(interfaceName);
				} else {
					s_logger.debug("Failed to stop process...try to kill");
	                if(LinuxProcessUtil.kill(pid)) {
	                    DhcpServerManager.removePidFile(interfaceName);
	                } else {
	                	throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "error killing process, pid=" + pid);
	                }	
				}
			} else {
				s_logger.debug("tried to kill DHCP server for interface but it is not running");
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return true;
	}

	public static String getConfigFilename(String interfaceName) {
	    StringBuffer sb = new StringBuffer(FILE_DIR);
	    sb.append("dhcpd-").append(interfaceName).append(".conf");

		return sb.toString();
	}
	
	private static void removePidFile(String interfaceName) {
		File pidFile = new File(DhcpServerManager.getPidFilename(interfaceName));
		if (pidFile.exists()) {
			pidFile.delete();
		}
	}

    private static String getPidFilename(String interfaceName) {
        StringBuffer sb = new StringBuffer(PID_FILE_DIR);
        sb.append("dhcpd-").append(interfaceName).append(".pid");

        return sb.toString();
    }
	
	private static String formDhcpdCommand(String interfaceName) {
		StringBuffer sb = new StringBuffer("dhcpd");
		sb.append(" -cf ").append(DhcpServerManager.getConfigFilename(interfaceName));
		sb.append(" -pf ").append(DhcpServerManager.getPidFilename(interfaceName));
		
		return sb.toString();
	}
}
