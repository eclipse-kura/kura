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
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpServerManager {

	private static final Logger s_logger = LoggerFactory.getLogger(DhcpServerManager.class);
	
	private static final String FILE_DIR = "/etc/";
	private static final String PID_FILE_DIR = "/var/run/";
	private static DhcpServerTool dhcpServerTool = DhcpServerTool.NONE;
	
	static {
		dhcpServerTool = getTool();
	}
	
	public static DhcpServerTool getTool() {
		if (dhcpServerTool == DhcpServerTool.NONE) {
			if (LinuxNetworkUtil.toolExists(DhcpServerTool.DHCPD.getValue())) {
				dhcpServerTool = DhcpServerTool.DHCPD;
			} else if (LinuxNetworkUtil.toolExists(DhcpServerTool.UDHCPD.getValue())) {
				dhcpServerTool = DhcpServerTool.UDHCPD;
			}
		}
		return dhcpServerTool;
	}
	
	public static boolean isRunning(String interfaceName) throws KuraException {
		try {
			// Check if DHCP server is running
			int pid = LinuxProcessUtil.getPid(DhcpServerManager.formDhcpdCommand(interfaceName));
			return (pid > -1);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}	
	
	public static boolean enable(String interfaceName) throws KuraException {
		try {
			// Check if DHCP server is running
			if(DhcpServerManager.isRunning(interfaceName)) {
				// If so, disable it
				s_logger.error("DHCP server is already running for " + interfaceName + ", bringing it down...");
				DhcpServerManager.disable(interfaceName);
			}
			// Start DHCP server
			File configFile = new File(DhcpServerManager.getConfigFilename(interfaceName));
			if(configFile.exists()) {
			    // FIXME:MC This leads to a process leak
    			if (LinuxProcessUtil.startBackground(DhcpServerManager.formDhcpdCommand(interfaceName), false) == 0) {
    				s_logger.debug("DHCP server started.");
    				return true;
    			}
			} else {
			    s_logger.debug("Can't start DHCP server, config file does not exist: {}", configFile.getAbsolutePath());
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return false;
	}

	public static boolean disable(String interfaceName) throws KuraException {
        s_logger.debug("Disable DHCP server for {}", interfaceName);

		try {
			// Check if DHCP server is running
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
	    StringBuilder sb = new StringBuilder(FILE_DIR);
	    if (dhcpServerTool == DhcpServerTool.DHCPD) {
	    	sb.append(DhcpServerTool.DHCPD.getValue());
	    	sb.append('-');
	    	sb.append(interfaceName);
	    	sb.append(".conf");
	    } else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
	    	sb.append(DhcpServerTool.UDHCPD.getValue());
	    	sb.append('-');
	    	sb.append(interfaceName);
	    	sb.append(".conf");
	    }
		return sb.toString();
	}
	
	private static void removePidFile(String interfaceName) {
		File pidFile = new File(DhcpServerManager.getPidFilename(interfaceName));
		if (pidFile.exists()) {
			pidFile.delete();
		}
	}

    public static String getPidFilename(String interfaceName) {
        StringBuilder sb = new StringBuilder(PID_FILE_DIR);
        if (dhcpServerTool == DhcpServerTool.DHCPD) {
        	sb.append(DhcpServerTool.DHCPD.getValue());
        	sb.append('-');
        	sb.append(interfaceName);
        	sb.append(".pid");
        } else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
        	sb.append(DhcpServerTool.UDHCPD.getValue());
        	sb.append('-');
        	sb.append(interfaceName);
        	sb.append(".pid");
        }
        return sb.toString();
    }
	
	private static String formDhcpdCommand(String interfaceName) {
		StringBuilder sb = new StringBuilder();
		if (dhcpServerTool == DhcpServerTool.DHCPD) {
			sb.append(DhcpServerTool.DHCPD.getValue());
			sb.append(" -cf ").append(DhcpServerManager.getConfigFilename(interfaceName));
			sb.append(" -pf ").append(DhcpServerManager.getPidFilename(interfaceName));
		} else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
			sb.append(DhcpServerTool.UDHCPD.getValue());
			sb.append(" -f -S ");
			sb.append(DhcpServerManager.getConfigFilename(interfaceName));
		}
		return sb.toString();
	}
}
