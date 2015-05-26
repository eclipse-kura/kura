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
			if (LinuxNetworkUtil.toolExists("dhclient")) {
				dhcpClientTool = DhcpClientTool.DHCLIENT;
			} else if (LinuxNetworkUtil.toolExists("udhcpc")) {
				dhcpClientTool = DhcpClientTool.UDHCPC;
			}
		}
		return dhcpClientTool;
	}
	
	
	public static boolean isRunning(String interfaceName) throws KuraException {
		
		try {
			String [] tokens = {interfaceName};
			int pid = LinuxProcessUtil.getPid("dhclient", tokens);
			return (pid > -1);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public static boolean enable(String interfaceName) throws KuraException {
		try {
			int pid = -1;
			if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
				pid = LinuxProcessUtil.getPid("dhclient", new String[]{interfaceName});
			} else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
				pid = LinuxProcessUtil.getPid("udhcpc", new String[]{interfaceName});
			}
			
			if (pid < 0) {
				LinuxProcessUtil.start(formCommand(interfaceName, true, true), true);
			}
			
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return false;
	}
	
	public static boolean disable(String interfaceName) throws KuraException {
		
		String [] tokens = {interfaceName};
		int pid = -1;
		try {
			if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
				pid = LinuxProcessUtil.getPid("dhclient", tokens);
			} else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
				pid = LinuxProcessUtil.getPid("udhcpc", tokens);
			}
			if (pid > -1) {
				s_logger.debug("manageDhcpClient() :: killing DHCP client for {}", interfaceName);
				if(LinuxProcessUtil.kill(pid)) {
					removePidFile(interfaceName);
				} else {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "error killing process, pid={}", pid);
				}
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		
		return false;
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
        	sb.append("/dhclient.");
        } else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
        	sb.append("/udhcpc-");
        }
        sb.append(interfaceName);
        sb.append(".pid");

        return sb.toString();
    }
	
	private static String formCommand(String interfaceName, boolean useLeasesFile, boolean usePidFile) {
		StringBuilder sb = new StringBuilder();
		
		if (dhcpClientTool == DhcpClientTool.DHCLIENT) {
			sb.append("dhclient ");
			if (useLeasesFile) {
				sb.append(formLeasesOption(interfaceName));
				sb.append(' ');
			}
			if (usePidFile) {
				sb.append(getPidFilename(interfaceName));
				sb.append(' ');
			} 
			sb.append(interfaceName);
		} else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
			sb.append("udhcpc ");
			sb.append("-i ");
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
			sb.append("dhclient -r ");
			sb.append(interfaceName);
		} else if (dhcpClientTool == DhcpClientTool.UDHCPC) {
			sb.append("udhcpc -R ");
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
		sb.append("/dhclient.");
		sb.append(interfaceName);
		sb.append(".leases");
		return sb.toString();
	}

}
