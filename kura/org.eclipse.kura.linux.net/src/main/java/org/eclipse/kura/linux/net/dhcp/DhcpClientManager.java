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
package org.eclipse.kura.linux.net.dhcp;

import java.io.File;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpClientManager {
	
	private static final Logger s_logger = LoggerFactory.getLogger(DhcpClientManager.class);
	
	private static DhcpClientTool dhcpClientTool = DhcpClientTool.NONE;
	private static final String PID_FILE_DIR = "/var/run";
	private static final String LEASES_DIR = "/var/lib/dhclient";
		
	static {
		File leasesDirectory = new File(LEASES_DIR);
		if (!leasesDirectory.exists()) {
			leasesDirectory.mkdirs();
		}
		dhcpClientTool = getTool();
	}
	
	public static DhcpClientTool getTool() {
		if (dhcpClientTool == DhcpClientTool.NONE) {
			if (LinuxNetworkUtil.toolExists(DhcpClientTool.DHCLIENT.getValue())) {
				dhcpClientTool = DhcpClientTool.DHCLIENT;
			} else if (LinuxNetworkUtil.toolExists(DhcpClientTool.UDHCPC.getValue())) {
				dhcpClientTool = DhcpClientTool.UDHCPC;
			}
		}
		return dhcpClientTool;
	}
	
	
	public static boolean isRunning(String interfaceName) throws KuraException {
		
		int pid = -1;
		try {
			if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
				pid = LinuxProcessUtil.getPid(DhcpClientTool.DHCLIENT.getValue(), new String[]{interfaceName});
			}  else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
				pid = LinuxProcessUtil.getPid(DhcpClientTool.UDHCPC.getValue(), new String[]{interfaceName});
			}
			return (pid > -1);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public static void enable(String interfaceName) throws KuraException {
		try {
			int pid = -1;
			if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
				pid = LinuxProcessUtil.getPid(DhcpClientTool.DHCLIENT.getValue(), new String[]{interfaceName});
			} else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
				pid = LinuxProcessUtil.getPid(DhcpClientTool.UDHCPC.getValue(), new String[]{interfaceName});
			}
			
			if (pid >= 0) {
				s_logger.info("enable() :: disabling DHCP client for {}", interfaceName);
				disable(interfaceName);
			}
			s_logger.info("enable() :: Starting DHCP client for {}", interfaceName);
			LinuxProcessUtil.start(formCommand(interfaceName, true, true), true);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public static void disable(String interfaceName) throws KuraException {
		int pid = -1;
		try {
			if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
				pid = LinuxProcessUtil.getPid(DhcpClientTool.DHCLIENT.getValue(), new String[]{interfaceName});
			} else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
				pid = LinuxProcessUtil.getPid(DhcpClientTool.UDHCPC.getValue(), new String[]{interfaceName});
			}
			if (pid > -1) {
				s_logger.info("disable() :: killing DHCP client for {}", interfaceName);
				if(LinuxProcessUtil.kill(pid)) {
					removePidFile(interfaceName);
				} else {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "error killing process, pid={}", pid);
				}
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public static void releaseCurrentLease(String interfaceName) throws KuraException {
		try {
			LinuxProcessUtil.start(formReleaseCurrentLeaseCommand(interfaceName), true);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	private static void removePidFile(String interfaceName) {
		File pidFile = new File(getPidFilename(interfaceName));
		if (pidFile.exists()) {
			pidFile.delete();
		}
	}
	
	private static String getPidFilename(String interfaceName) {
        StringBuilder sb = new StringBuilder(PID_FILE_DIR);
        if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
        	sb.append('/');
        	sb.append(DhcpClientTool.DHCLIENT.getValue());
        	sb.append('.');
        } else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
        	sb.append('/');
        	sb.append(DhcpClientTool.UDHCPC.getValue());
        	sb.append('-');
        }
        sb.append(interfaceName);
        sb.append(".pid");

        return sb.toString();
    }
	
	private static String formCommand(String interfaceName, boolean useLeasesFile, boolean usePidFile) {
		StringBuilder sb = new StringBuilder();
		
		if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
			sb.append(DhcpClientTool.DHCLIENT.getValue());
			sb.append(' ');
			if (useLeasesFile) {
				sb.append(formLeasesOption(interfaceName));
				sb.append(' ');
			}
			if (usePidFile) {
				sb.append(" -pf ");
				sb.append(getPidFilename(interfaceName));
				sb.append(' ');
			} 
			sb.append(interfaceName);
		} else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
			sb.append(DhcpClientTool.UDHCPC.getValue());
			sb.append(" -i ");
			sb.append(interfaceName);
			sb.append(' ');
			if (usePidFile) {
				sb.append(getPidFilename(interfaceName));
				sb.append(' ');
			}
			sb.append(" -S");
		}
		sb.append("\n");
		return sb.toString();
	}
	
	private static String formReleaseCurrentLeaseCommand(String interfaceName) {
		
		StringBuilder sb = new StringBuilder();
		if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
			sb.append(DhcpClientTool.DHCLIENT.getValue());
			sb.append(" -r ");
			sb.append(interfaceName);
		} else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
			sb.append(DhcpClientTool.UDHCPC.getValue());
			sb.append(" -R ");
			sb.append("-i ");
			sb.append(interfaceName);
		}
		sb.append("\n");
		return sb.toString();
	}
	
	private static String formLeasesOption(String interfaceName) {
		
		StringBuffer sb = new StringBuffer();
		sb.append("-lf ");
		sb.append(LEASES_DIR);
		sb.append('/');
		sb.append(DhcpClientTool.DHCLIENT.getValue());
		sb.append('.');
		sb.append(interfaceName);
		sb.append(".leases");
		return sb.toString();
	}
}
