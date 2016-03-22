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
package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.NetworkServiceImpl;
import org.eclipse.kura.linux.net.dhcp.DhcpClientTool;
import org.eclipse.kura.linux.net.wifi.WifiOptions;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxNetworkUtil {

	private static final Logger s_logger = LoggerFactory.getLogger(LinuxNetworkUtil.class);
	
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");
	
	private static Map<String, LinuxIfconfig> s_ifconfigs = new HashMap<String, LinuxIfconfig>();
	
	private static final String [] s_ignoreIfaces = {"can", "sit", "mon.wlan"};
	
	private static final ArrayList<String> s_tools = new ArrayList<String>(); 

	public static List<String> getAllInterfaceNames() throws KuraException {
		try {
			IpAddrShow ipAddrShow = new IpAddrShow();
			LinuxIfconfig[] configs = ipAddrShow.exec();
			List<String> ifaces = new ArrayList<String>();
			for (LinuxIfconfig config : configs) {
				ifaces.add(config.getName());
			}
			return ifaces;
		} catch (KuraException e) {
			s_logger.warn("FIXME: IpAddrShow failed. Falling back to old method", e);
			return getAllInterfaceNamesInternal();
		}
	}
	
	@Deprecated
	private static List<String> getAllInterfaceNamesInternal() throws KuraException {
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			List<String> ifaces = new ArrayList<String>();

			//start the process
			proc = ProcessUtil.exec("ifconfig -a");
			if (proc.waitFor() != 0) {
				s_logger.error("error executing command --- ifconfig -a --- exit value = " + proc.exitValue());
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
			}

			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while((line = br.readLine()) != null) {
				if(line.indexOf("Link encap:") > -1) {
					StringTokenizer st = new StringTokenizer(line);
					ifaces.add(st.nextToken());
				}
			}

			return ifaces;
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch(InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}

			if (proc != null) ProcessUtil.destroy(proc);
		}
	}
	
	/*
	 * Returns null if the interface is not found
	 */
	public static String getCurrentIpAddress(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return null;
		}

		LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);
		
		return ifconfig != null ? ifconfig.getInetAddress() : null;
	}
	
	@Deprecated
	private static String getCurrentIpAddressInternal(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return null;
		}
		
		String ipAddress = null;
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig " + ifaceName);
			if (proc.waitFor() != 0) {
				s_logger.warn("getCurrentIpAddress() :: error executing command --- ifconfig {} --- exit value = {}", ifaceName , proc.exitValue());
				return ipAddress;
			}

			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while ((line = br.readLine()) != null) {
				if (line.indexOf(ifaceName) > -1) {
					if ((line = br.readLine()) != null) {
						int i = line.indexOf("inet addr:");
						if (i > -1) {
							ipAddress = line.substring(i + 10, line.indexOf(' ', i + 10));
						}
					}
					break;
				}
			}			
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}

			if (proc != null) ProcessUtil.destroy(proc);
		}
		return ipAddress;
	}

	/*
	 * Returns -1 if the interface is not found
	 */
	public static int getCurrentMtu(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return -1;
		}
		
		LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);
		
		return ifconfig != null ? ifconfig.getMtu() : -1;
	}
	
	@Deprecated
	private static int getCurrentMtuInternal(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return -1;
		}
		
		int mtu = -1;
		String stringMtu = null;
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig " + ifaceName);
			if (proc.waitFor() != 0) {
				s_logger.warn("getCurrentMtu() :: error executing command --- ifconfig {} --- exit value = {}", ifaceName , proc.exitValue());
				return mtu;
			}

			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.indexOf("MTU:") > -1) {
					stringMtu = line.substring(line.indexOf("MTU:") + 4, line.indexOf("Metric:") - 2);
					break;
				}
			}

			mtu = Integer.parseInt(stringMtu);

		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}

			if (proc != null) ProcessUtil.destroy(proc);
		}
		return mtu;
	}

	public static boolean isLinkUp(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return false;
		}
		return isLinkUp(getType(ifaceName), ifaceName);
	}
	
	/*
	 * Returns false if the interface is not found
	 */
	public static boolean isLinkUp(NetInterfaceType ifaceType, String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return false;
		}
				
		try {
			LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);
			// FIXME: should we throw an exception if config is null?
			return ifconfig != null ? ifconfig.isLinkUp() : false;
		} catch (KuraException e) {
			s_logger.warn("FIXME: IpAddrShow failed. Falling back to old method", e);
			return isLinkUpInternal(ifaceType, ifaceName);
		}		
	}
	
	@Deprecated
	private static boolean isLinkUpInternal(NetInterfaceType ifaceType, String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return false;
		}
		
		try {
			if(ifaceType == NetInterfaceType.WIFI) {
				Collection<String> supportedWifiOptions = WifiOptions.getSupportedOptions(ifaceName);
				LinkTool linkTool = null;
				if ((supportedWifiOptions != null) && (supportedWifiOptions.size() > 0)) {
					if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
						linkTool = new IwLinkTool(ifaceName);
					} else if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_WEXT)) {
						linkTool = new iwconfigLinkTool(ifaceName);
					}
				}

				if((linkTool != null) && linkTool.get()) {
					return linkTool.isLinkDetected();
				} else {
					//throw new KuraException(Kura`ErrorCode.INTERNAL_ERROR, "link tool failed to detect the status of " + ifaceName);
					s_logger.error("link tool failed to detect the status of " + ifaceName);
					return false;
				}
			} else if(ifaceType == NetInterfaceType.ETHERNET) {
				LinkTool linkTool = null;
				if (toolExists("ethtool")) {
					linkTool = new EthTool (ifaceName);
				} else if (toolExists("mii-tool")) {
					linkTool = new MiiTool (ifaceName);
				}

				if (linkTool != null) {
					if(linkTool.get()) {
						return linkTool.isLinkDetected();
					} else {
						if (TARGET_NAME.equals(KuraConstants.ReliaGATE_15_10.getTargetName())) {
							SafeProcess proc = ProcessUtil.exec("ifconfig " + ifaceName + " up");
							if ((proc.waitFor() == 0) && linkTool.get()) {
								return linkTool.isLinkDetected();
							}
						}
						throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "link tool failed to detect the ethernet status of " + ifaceName);
					}
				} else {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "ethtool or mii-tool must be included with the Linux distro");
				}
			} else {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported NetInterfaceType: " + ifaceType);
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public static boolean toolExists(String tool) {
		boolean ret = false;
		final String[] searchFolders = new String[]{"/sbin/", "/usr/sbin/", "/bin/"};
		
		if (s_tools.contains(tool)) {
			ret = true;
		} else {
			for (String folder : searchFolders) {
				File fTool = new File(folder + tool);
				if (fTool.exists()) {
					s_tools.add(tool);
					ret = true;
					break;
				}
			}
		}
		return ret;
	}
	
	/**
	 * This method is meaningful only for interfaces of type: NetInterfaceType.ETHERNET, NetInterfaceType.WIFI, NetInterfaceType.LOOPBACK.
	 */
	public static boolean isAutoConnect(String interfaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(interfaceName.charAt(0))) {
			return false;
		}
		BufferedReader br = null;		
		try {
			File interfaceFile = new File("/etc/sysconfig/network-scripts/ifcfg-" + interfaceName);
			if(interfaceFile.exists()) {
				br = new BufferedReader(new FileReader(interfaceFile));
				if(br != null) {
					String line = null;
					while((line = br.readLine()) != null) {
						if(line.contains("ONBOOT=yes")) {
							return true;
						}
					}
				}
			}			
			return false;
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally{
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}			
		}
	}
	
	/*
	 * Returns null if the interface is not found
	 * Note: the returned configuration is also stored in the static cache
	 */
	public static LinuxIfconfig getInterfaceConfiguration(String ifaceName) throws KuraException {
		try {
			IpAddrShow ipAddrShow = new IpAddrShow(ifaceName);
			LinuxIfconfig[] configs = ipAddrShow.exec();
			if (configs.length == 0) {
				return null;
			}
			LinuxIfconfig config = configs[0];
			
			//determine if wifi
			if (config.getType() == NetInterfaceType.ETHERNET) {
				Collection<String> wifiOptions = WifiOptions.getSupportedOptions(ifaceName);
				if (wifiOptions != null) {
					for (String op : wifiOptions) {
						s_logger.trace("WiFi option supported on {} : {}", ifaceName, op);
					}
					config.setType(NetInterfaceType.WIFI);
				}
			}
			
			// determine driver
			if ((config.getType() == NetInterfaceType.ETHERNET) || (config.getType() == NetInterfaceType.WIFI)) {
				try {
					Map<String,String> driver = getEthernetDriver(ifaceName);
					config.setDriver(driver);
				} catch (KuraException e) {
					s_logger.error("getInterfaceConfiguration() :: failed to obtain driver information - {}", e);
				}
			}
			
			// cache information
			s_ifconfigs.put(ifaceName, config);
			return config;
		} catch (KuraException e) {
			if (e.getCode() == KuraErrorCode.OS_COMMAND_ERROR) {
				// Assuming ifconfig fails because a PPP link went down and its interface cannot be found
				if(ifaceName.matches("^ppp\\d+$")) {
					File pppFile = new File(NetworkServiceImpl.PPP_PEERS_DIR + ifaceName);
					if (pppFile.exists()) {
						LinuxIfconfig config = new LinuxIfconfig(ifaceName);
						config.setType(NetInterfaceType.valueOf("MODEM"));
						return config;
					}
				}
			} else {
				s_logger.warn("FIXME: IpAddrShow failed. Falling back to old ifconfig method", e);

				// FIXME: ifconfig is deprecated
				return getInterfaceConfigurationInternal(ifaceName);
			}
		}
		return null;
	}
	
	@Deprecated
	private static LinuxIfconfig getInterfaceConfigurationInternal(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return null;
		}
		for (String ignoreIface : s_ignoreIfaces) {
			if (ifaceName.startsWith(ignoreIface)) {
				return null;
			}
		}
		
		LinuxIfconfig linuxIfconfig = null;
		SafeProcess proc = null;
		BufferedReader br = null;
		linuxIfconfig = new LinuxIfconfig(ifaceName);
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig " + ifaceName);
			if (proc.waitFor() == 0) {
				//get the output
				br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String line = null;
				
				while ((line = br.readLine()) != null) {
					
					int i = line.indexOf("Link encap:");
					if(i > -1) {
						linuxIfconfig.setType(getInterfaceType(ifaceName, line));
						
						i = line.indexOf("HWaddr ");
						if(i > -1) {
							String mac = line.substring(i + 7, line.length()-2);
							linuxIfconfig.setMacAddress(mac);
						}
					}
					
					i = line.indexOf("inet addr:");
					if (i > -1) {
						String ipAddress = line.substring(i + 10, line.indexOf(' ', i + 10));
						linuxIfconfig.setInetAddress(ipAddress);
						
						i = line.indexOf("Mask:");
						if (i > -1) {
							String netmask = line.substring(i + 5);
							linuxIfconfig.setInetMask(netmask);
						}
							
						i = line.indexOf("Bcast:");
						if (i > -1) {
							String broadcast = line.substring(i + 6, line.indexOf(' ', i + 6));
							linuxIfconfig.setInetBcast(broadcast);
						}
					}
						
					i = line.indexOf("MTU:");
					if (i > -1) {
						String mtu = line.substring(i + 4, line.indexOf(' ', i + 4));
						linuxIfconfig.setMtu(Integer.parseInt(mtu));
					}	
					
					if (line.contains("MULTICAST")) {
						linuxIfconfig.setMulticast(true);
					}
				}
			} else {
				File pppFile = new File(NetworkServiceImpl.PPP_PEERS_DIR + ifaceName);
				if(pppFile.exists() || ifaceName.matches("^ppp\\d+$")) {
					linuxIfconfig.setType(NetInterfaceType.valueOf("MODEM"));
				}
			}			
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try	{
					br.close();
				} catch(IOException ex){
					s_logger.error("getInterfaceConfiguration() :: I/O Exception while closing BufferedReader!");
				}
			}
					
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
		
		if ((linuxIfconfig.getType() == NetInterfaceType.ETHERNET) || (linuxIfconfig.getType() == NetInterfaceType.WIFI)) {
			try {
				Map<String,String> driver = getEthernetDriver(ifaceName);
				if (driver != null) {
					linuxIfconfig.setDriver(driver);
				}
			} catch (KuraException e) {
				s_logger.error("getInterfaceConfiguration() :: failed to obtain driver information - {}", e);
			}
		}
		
		s_ifconfigs.put(ifaceName, linuxIfconfig);
		return linuxIfconfig;
	}
	
	/*
	 * Returns false on error
	 */
	public static boolean canPing(String ipAddress, int count) throws KuraException {
		SafeProcess proc = null;
		try {
			proc = ProcessUtil.exec(new StringBuilder().append("ping -c ").append(count).append(" ").append(ipAddress).toString());
			if(proc.waitFor() == 0) {
				return true;
			} else {
				// FIXME: throw an exception?
				return false;
			}
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}
	
	/*
	 * Returns NetInterfaceType.UNKNOWN for ignored interfaces or if the interface is not found
	 * Note: may return a cached information
	 */
	public static NetInterfaceType getType(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return NetInterfaceType.UNKNOWN;
		}
		for (String ignoreIface : s_ignoreIfaces) {
			if (ifaceName.startsWith(ignoreIface)) {
				return NetInterfaceType.UNKNOWN;
			}
		}
		
		NetInterfaceType ifaceType = null;

		if (s_ifconfigs.containsKey(ifaceName)) {
			LinuxIfconfig ifconfig = s_ifconfigs.get(ifaceName);
			ifaceType = ifconfig.getType();
		} else {
			LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);
			if (ifconfig != null) {
				ifaceType = ifconfig.getType();
			}
		}
		s_logger.trace("getType() :: interface={}, type={}", ifaceName, ifaceType);
		return ifaceType;
	}
	
	@Deprecated
	private static NetInterfaceType getTypeInternal(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return NetInterfaceType.UNKNOWN;
		}
		for (String ignoreIface : s_ignoreIfaces) {
			if (ifaceName.startsWith(ignoreIface)) {
				return NetInterfaceType.UNKNOWN;
			}
		}
		
		NetInterfaceType ifaceType = NetInterfaceType.UNKNOWN;

		if (s_ifconfigs.containsKey(ifaceName)) {
			LinuxIfconfig ifconfig = s_ifconfigs.get(ifaceName);
			ifaceType = ifconfig.getType();
			s_logger.trace("getType() :: interface={}, type={}", ifaceName, ifaceType);
		} else {
			s_ifconfigs.put(ifaceName, new LinuxIfconfig(ifaceName));
		}
		
		if (ifaceType != NetInterfaceType.UNKNOWN) {
			return ifaceType;
		}
		
		//String stringType = null;
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig " + ifaceName);
			if (proc.waitFor() == 0) {
				//get the output
				br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String line = null;

				while((line = br.readLine()) != null) {
					int index = line.indexOf("Link encap:");
					if(index > -1) {
						ifaceType = getInterfaceType(ifaceName, line);
						break;
					}
				}
			} else {
				File pppFile = new File(NetworkServiceImpl.PPP_PEERS_DIR + ifaceName);
				if(pppFile.exists() || ifaceName.matches("^ppp\\d+$")) {
				    ifaceType = NetInterfaceType.valueOf("MODEM");
				}
			}
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
			
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
		
		s_logger.trace("getType() :: interface={}, type={}", ifaceName, ifaceType);
		LinuxIfconfig ifconfig = s_ifconfigs.get(ifaceName);
		ifconfig.setType(ifaceType);
		
		return ifaceType;
	}

	@Deprecated
	private static NetInterfaceType getInterfaceType (String ifaceName, String line) throws KuraException {
		
		NetInterfaceType ifaceType = NetInterfaceType.UNKNOWN;
		
		String stringType = null;
		StringTokenizer st = new StringTokenizer(line);
		st.nextToken(); //skip iface name
		st.nextToken(); //skip Link
		stringType = st.nextToken();
		stringType = stringType.substring(6).toUpperCase();
		if(stringType.equals("LOCAL")) {
			stringType = "LOOPBACK";
		} else if (stringType.equals("ETHERNET")) { 
			stringType = "ETHERNET";
		} else if (stringType.equals("POINT-TO-POINT")) {
			stringType = "MODEM";
		}
		
		//determine if wifi
		if ("ETHERNET".equals(stringType)) {
			Collection<String> wifiOptions = WifiOptions.getSupportedOptions(ifaceName);
			if (wifiOptions.size() > 0) {
				for (String op : wifiOptions) {
					s_logger.trace("WiFi option supported on {} : {}", ifaceName, op);
				}
				stringType = "WIFI";
			}
		} 
		
		if (stringType != null) {
			try {
				ifaceType = NetInterfaceType.valueOf(stringType);
			} catch (Exception e) {
				// leave it UNKNOWN
			}
		}
		return ifaceType;
	}

	/*
	 * Return a dummy driver if the interface cannot be found or in case of an error
	 * Note: may return a cached information
	 */
	public static Map<String,String> getEthernetDriver(String interfaceName) throws KuraException
	{
		Map<String, String> driver = null;
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(interfaceName.charAt(0))) {
			driver = new HashMap<String, String>();
			driver.put("name", "unknown");
			driver.put("version", "unkown");
			driver.put("firmware", "unknown");
			return driver;
		}
		
		if (s_ifconfigs.containsKey(interfaceName)) {
			LinuxIfconfig ifconfig = s_ifconfigs.get(interfaceName);
			driver = ifconfig.getDriver();
		}
		
		if (driver != null) {
			return driver;
		}
		
		driver = new HashMap<String, String>();
		driver.put("name", "unknown");
		driver.put("version", "unkown");
		driver.put("firmware", "unknown");
		
		SafeProcess procEthtool = null;
		BufferedReader br = null;			
		try {
			//run ethtool
			if (toolExists("ethtool")) {
				if (TARGET_NAME.equals(KuraConstants.ReliaGATE_15_10.getTargetName())) {
					SafeProcess proc = ProcessUtil.exec("ifconfig " + interfaceName + " up");
					if (proc.waitFor() != 0) {
						s_logger.warn("getEthernetDriver() :: error executing command --- ifconfig {} up", interfaceName);
					}
				}
				procEthtool = ProcessUtil.exec("ethtool -i " + interfaceName);
				if (procEthtool.waitFor() != 0) {
	                s_logger.warn("getEthernetDriver() :: error executing command --- ethtool -i {}", interfaceName);
	                // FIXME: throw exception
	                return driver;
				}
			}
			
			//get the output
			if (procEthtool != null) {
				br = new BufferedReader(new InputStreamReader(procEthtool.getInputStream()));
				String line = null;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("driver: ")) {
						driver.put("name", line.substring(line.indexOf(": ") + 1));
					}
					else if (line.startsWith("version: ")) {
						driver.put("version", line.substring(line.indexOf(": ") + 1));
					}
					else if (line.startsWith("firmware-version: ")) {
						driver.put("firmware", line.substring(line.indexOf(": ") + 1));
					}
				}
			}
			
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}			
			if (procEthtool != null) {
				ProcessUtil.destroy(procEthtool);
			}
		}
		return driver;
	}
	
	/*
	 * Returns an empty capabilities set if the interface is not found or on error
	 */
	public static EnumSet<Capability> getWifiCapabilities(String ifaceName) throws KuraException {
		EnumSet<Capability> capabilities = EnumSet.noneOf(Capability.class);
		
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return capabilities;
		}
		
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("iwlist " + ifaceName + " auth");
			if (proc.waitFor() != 0) {
                s_logger.warn("error executing command --- iwlist --- exit value = {}", proc.exitValue());
                // FIXME: throw exception
                return capabilities;
			}
	
			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			while((line = br.readLine()) != null) {
				// Remove all whitespace
				String cleanLine = line.replaceAll("\\s", "");
				
				if ("WPA".equals(cleanLine)) {
					capabilities.add(Capability.WPA);
				} else if ("WPA2".equals(cleanLine)) {
					capabilities.add(Capability.RSN);
				} else if ("CIPHER-TKIP".equals(cleanLine)) {
					capabilities.add(Capability.CIPHER_TKIP);
				} else if ("CIPHER-CCMP".equals(cleanLine)) {
					capabilities.add(Capability.CIPHER_CCMP);
					
				// TODO: WEP options don't always seem to be displayed?
				} else if ("WEP-104".equals(cleanLine)) {
					capabilities.add(Capability.CIPHER_WEP104);
				} else if ("WEP-40".equals(cleanLine)) {
					capabilities.add(Capability.CIPHER_WEP40);
				}				
			}
			
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
			
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
		return capabilities;
	}
	
	/*
	 * Returns WifiMode.UNKNOWN if the interface is not found or on error
	 */
	public static WifiMode getWifiMode(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return WifiMode.UNKNOWN;
		}
		WifiMode mode = WifiMode.UNKNOWN;
		SafeProcess procIw = null;
		SafeProcess procIwConfig = null;
		BufferedReader br1 = null;
		BufferedReader br2 = null;
		String line = null;
		try {
			if (toolExists("iw")) {
				procIw = ProcessUtil.exec("iw dev " + ifaceName + " info");
				if (procIw.waitFor() != 0) {
					s_logger.warn("error executing command --- iw --- exit value = {}; will try iwconfig ...", procIw.exitValue());
					// fallback to iwconfig
				}
				
				br1 = new BufferedReader(new InputStreamReader(procIw.getInputStream()));
				while((line = br1.readLine()) != null) {
					int index = line.indexOf("type ");
					if(index > -1) {
						s_logger.debug("line: {}", line);
						String sMode = line.substring(index+"type ".length());
						if("AP".equals(sMode)) {
							mode = WifiMode.MASTER;
						} else if ("managed".equals(sMode)) {
							mode = WifiMode.INFRA;
						}
						break;
					}
				}
			}
						
			if (mode.equals(WifiMode.UNKNOWN)) {
				if(toolExists("iwconfig")) {
					procIwConfig = ProcessUtil.exec("iwconfig " + ifaceName);
					if (procIwConfig.waitFor() != 0) {
						s_logger.error("error executing command --- iwconfig --- exit value = {}", procIwConfig.exitValue());
						// FIXME: throw exception
		                return mode;
					}
					
					//get the output
					br2 = new BufferedReader(new InputStreamReader(procIwConfig.getInputStream()));
					while((line = br2.readLine()) != null) {
						int index = line.indexOf("Mode:");
						if(index > -1) {
							s_logger.debug("line: {}", line);
							StringTokenizer st = new StringTokenizer(line.substring(index));
							String modeStr = st.nextToken().substring(5);
							if("Managed".equals(modeStr)) {
								mode = WifiMode.INFRA;
							} else if ("Master".equals(modeStr)) {
								mode = WifiMode.MASTER;
							} else if ("Ad-Hoc".equals(modeStr)) {
								mode = WifiMode.ADHOC;
							}
							break;
						}
					}
				}
			}
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} finally {
			if(br1 != null){
				try{
					br1.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}	
			
			if(br2 != null){
				try{
					br2.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}	
			
			if (procIw != null) {
				ProcessUtil.destroy(procIw);
			}
			if (procIwConfig != null) {
				ProcessUtil.destroy(procIwConfig);
			}
		}
		
		return mode;
	}
	
	/*
	 * Returns 0 if the interface is not found or on error
	 */
	public static long getWifiBitrate(String ifaceName) throws KuraException {
		long bitRate = 0;
		
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return bitRate;
		}
		
		SafeProcess proc = null;
		BufferedReader br = null;
		String line = null;
		try {
			if (toolExists("iw")) {
				//start the process
				proc = ProcessUtil.exec("iw dev " + ifaceName + " link");
				if (proc.waitFor() != 0) {
					s_logger.warn("error executing command --- iw --- exit value = {}", proc.exitValue());
					// FIXME: why don't we fallback to iwconfig like in the case of getWifiMode()?
					return bitRate;
				}
				else {	
					//get the output
					br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					line = null;
		
					while((line = br.readLine()) != null) {
						int index = line.indexOf("tx bitrate: ");
						if(index > -1) {
							s_logger.debug("line: " + line);
							StringTokenizer st = new StringTokenizer(line.substring(index));
							st.nextToken();	// skip 'tx'
							st.nextToken(); // skip 'bitrate:'
							Double rate = Double.parseDouble(st.nextToken());
							int mult = 1;
							
							String unit = st.nextToken();
							if(unit.startsWith("kb")) {
								mult = 1000;
							} else if (unit.startsWith("Mb")) {
								mult = 1000000;
							} else if (unit.startsWith("Gb")) {
								mult = 1000000000;
							}
							
							bitRate = (long) (rate * mult);
							return bitRate;
						}
					}
				}
			}
			
			else if(toolExists("iwconfig")) {
				//start the process
				proc = ProcessUtil.exec("iwconfig " + ifaceName);
				if (proc.waitFor() != 0) {
					s_logger.warn("error executing command --- iwconfig --- exit value = {}", proc.exitValue());
					// FIXME: throw exception
					return bitRate;
				}
	
				//get the output
				br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				line = null;
	
				while((line = br.readLine()) != null) {
					int index = line.indexOf("Bit Rate=");
					if(index > -1) {
						s_logger.debug("line: {}", line);
						StringTokenizer st = new StringTokenizer(line.substring(index));
						st.nextToken();	// skip 'Bit'
						Double rate = Double.parseDouble(st.nextToken().substring(5));
						int mult = 1;
						
						String unit = st.nextToken();
						if(unit.startsWith("kb")) {
							mult = 1000;
						} else if (unit.startsWith("Mb")) {
							mult = 1000000;
						} else if (unit.startsWith("Gb")) {
							mult = 1000000000;
						}
						
						bitRate = (long) (rate * mult);
						return bitRate;
					}
				}
			}
			
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
			
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
		
		return bitRate;
	}
	
	/*
	 * Return null if the interface is not found or on error
	 */
	public static String getSSID(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return null;
		}
		
		String ssid = null;
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			if(toolExists("iw")) {
				//start the process
				proc = ProcessUtil.exec("iw dev " + ifaceName + " link");
				if (proc.waitFor() != 0) {
					s_logger.warn("error executing command --- iw --- exit value = {}", proc.exitValue());
					// FIXME: why don't we fallback to iwconfig like in the case of getWifiMode()?
					return ssid;
				}
	
				//get the output
				br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String line = null;
	
				while((line = br.readLine()) != null) {
					int index = line.indexOf("SSID:");
					if(index > -1) {
						s_logger.debug("line: {}", line);
						String lineSub = line.substring(index);
						StringTokenizer st = new StringTokenizer(lineSub);
						st.nextToken();
						ssid = st.nextToken();
						return ssid;
					}
				}
			}			
			
			else if(toolExists("iwconfig")) {
				//start the process
				proc = ProcessUtil.exec("iwconfig " + ifaceName);
				if (proc.waitFor() != 0) {
					s_logger.warn("error executing command --- iwconfig --- exit value = {}", proc.exitValue());
					// FIXME: throw exception
					return ssid;
				}
	
				//get the output
				br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String line = null;
	
				while((line = br.readLine()) != null) {
					int index = line.indexOf("ESSID:");
					if(index > -1) {
						s_logger.debug("line: {}", line);
						String lineSub = line.substring(index);
						StringTokenizer st = new StringTokenizer(lineSub);
						String ssidStr = st.nextToken();
						if(ssidStr.startsWith("\"") && ssidStr.endsWith("\"")) {
							ssid = ssidStr.substring(lineSub.indexOf('"') + 1, lineSub.lastIndexOf('"')); // get value between quotes
						}
					}
					return ssid;
				}
			}
			
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
			
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
		
		return ssid;
	}

	/*
	 * Note: this method DOES NOT bring down the interface.
	 * Instead, it just deletes the IP address leaving the interface up.
	 * The trick leaves the interface powered up allowing to detect a link state change.
	 * After a successful call to this method, a call to hasAddress() method returns false.
	 */
	public static void disableInterface(String interfaceName) throws Exception {
		if(interfaceName != null) {
			//ignore logical interfaces like "1-1.2"
			if (Character.isDigit(interfaceName.charAt(0))) {
				return;
			}
			// FIXME:
			// * Can we unify the below cases?
			// * Why do we need 'ifdown iface' followed by 'ifconfig iface down'?
			// * Do we really need to bring down the interface before deleting addresses?
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())) {
				// FIXME: check the exit code and throw an exception
				LinuxProcessUtil.start("ifdown " + interfaceName + "\n");
				LinuxProcessUtil.start("ifconfig " + interfaceName + " down\n");
			} else {
				if (hasAddress(interfaceName)) {
					// FIXME: check the exit code and throw an exception
					LinuxProcessUtil.start("ifdown " + interfaceName + "\n");
					// FIXME: this has been observed to fail (with exit code == 1).
					// Should we try an 'ifconfig iface down' like above?
				}
			}
			
			//always leave the Ethernet Controller powered
			bringUpDeletingAddress(interfaceName);
		}
	}
	
	public static void enableInterface(String interfaceName) throws Exception {
		if(interfaceName != null) {
			//ignore logical interfaces like "1-1.2"
			if (Character.isDigit(interfaceName.charAt(0))) {
				return;
			}
			// FIXME:
			// * Can we unify the below cases?
			// * Why is 'ifconfig iface' sometimes required before 'ifup iface'?
			// * Is '-f', '--force' used because the interface is or might be already up?
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())) {
				// FIXME: check the exit code and throw an exception
				LinuxProcessUtil.start("ifconfig " + interfaceName + " up\n");
				LinuxProcessUtil.start("ifup -f " + interfaceName + "\n");
			} else if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())) {
				// FIXME: check the exit code and throw an exception
				LinuxProcessUtil.start("ifconfig " + interfaceName + " up\n");
				LinuxProcessUtil.start("ifup --force " + interfaceName + "\n");
			} else {
				// FIXME: check the exit code and throw an exception
				LinuxProcessUtil.start("ifup " + interfaceName + "\n");						
			}
		}
	}
	
	/*
	 * Returns true if both the inet address and inet mask are non-null.
	 * Returns false if the interface is not found.
	 */
	public static boolean hasAddress(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return false;
		}
		
		LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);
	
		// FIXME: should we throw an exception if config is null?
		boolean ret = false;
		if (ifconfig != null &&
			ifconfig.getInetAddress() != null && (ifconfig.getInetMask() != null)) {
				ret = true;
		}

		return ret;
	}
	
	@Deprecated
	private static boolean isUpInternal(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return false;
		}
		
		boolean ret = false;
		LinuxIfconfig ifconfig = getInterfaceConfigurationInternal(ifaceName);
		if (ifconfig != null) {
			if ((ifconfig.getInetAddress() != null) && (ifconfig.getInetMask() != null)) {
				ret = true;
			}
		}

		return ret;
	}
	
	/*
	 * This method bring up the interface deleting its IP address.
	 * The trick powers the interface up allowing to detect a link state change.
	 * After a successful call to this method, a call to hasAddress() method returns false.
	 */
	public static void bringUpDeletingAddress(String interfaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(interfaceName.charAt(0))) {
			return;
		}
		
		//Power the controller.
		//This is implementing by setting the IPv4 unspecified address 0.0.0.0.
		//This is equivalent to e.g.:
		//ip addr del 172.16.0.1/32 dev eth0
		//or, to delete all the interface address:
		//ip addr flush dev eth0
		SafeProcess proc = null;
		try {
			//start the SafeProcess
			StringBuilder sb = new StringBuilder().append("ifconfig ").append(interfaceName).append(" 0.0.0.0");
			proc = ProcessUtil.exec(sb.toString());

			if (proc.waitFor() != 0) {
				// FIXME: throw an exception
				s_logger.error("error executing command --- " + sb.toString() + " --- exit value = " + proc.exitValue());
				return;
			}
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}
	
	@Deprecated
	private static void powerOnEthernetControllerInternal(String interfaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(interfaceName.charAt(0))) {
			return;
		}
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig");
			if (proc.waitFor() != 0) {
				s_logger.error("error executing command --- ifconfig --- exit value = " + proc.exitValue());
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
			}

			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while((line = br.readLine()) != null) {
				if(line.indexOf(interfaceName) > -1 && line.indexOf("mon." + interfaceName) < 0) {
					
					//so the interface is listed - power is already on
				    // WTF: should this be !=, or even better, can we remove the statement below?
					if(LinuxNetworkUtil.getCurrentIpAddress(interfaceName) == null) {
						return;
					}
				}
			}
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
			
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
		
		//power the controller since it is not on
		try {
			//start the SafeProcess
			StringBuilder sb = new StringBuilder().append("ifconfig ").append(interfaceName).append(" 0.0.0.0");
			proc = ProcessUtil.exec(sb.toString());

			if (proc.waitFor() != 0) {
				s_logger.error("error executing command --- " + sb.toString() + " --- exit value = " + proc.exitValue());
				return;
			}
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}
	
	/*
	 * Returns true if the interface is up (e.g. by 'ifup iface' or 'ifconfig iface up').
	 * Returns false if the interface is not found.
	 */
	public static boolean isUp(String interfaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(interfaceName.charAt(0))) {
			return false;
		}

		LinuxIfconfig config = getInterfaceConfiguration(interfaceName);
		
		return config != null ? config.isUp() : false;
	}
	
	@Deprecated
	private static boolean isEthernetControllerPoweredInternal(String interfaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(interfaceName.charAt(0))) {
			return false;
		}
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig");
			if (proc.waitFor() != 0) {
				s_logger.error("error executing command --- ifconfig --- exit value = " + proc.exitValue());
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
			}
			
			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while((line = br.readLine()) != null) {
				if(line.indexOf(interfaceName) > -1 && line.indexOf("mon." + interfaceName) < 0) {
					
					//so the interface is listed - power is already on
					return true;
				}
			}
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
			
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}

		return false;
	}
	
	public static boolean isKernelModuleLoaded(String interfaceName, WifiMode wifiMode) throws KuraException {
		boolean result = false;

		// FIXME: how to find the right kernel module by interface name?
		// Assume for now the interface name does not change
		// Note that WiFiConfig.getDriver() below usually returns the "nl80211", not the
		// the chipset kernel module (e.g. bcmdhd)
		// s_logger.info("{} driver: '{}'", interfaceName, wifiConfig.getDriver());

		if (KuraConstants.ReliaGATE_10_05.getTargetName().equals(TARGET_NAME) &&
				"wlan0".equals(interfaceName)) {
			SafeProcess proc = null;
			BufferedReader br = null;
			String cmd = "lsmod";
			try {
				s_logger.debug("Executing '{}'", cmd);
				proc = ProcessUtil.exec(cmd);
				if ((proc.waitFor()) != 0) {
					throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
				}

				//get the output
				br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String line = null;
				while ((line = br.readLine()) != null) {
					if (line.contains("bcmdhd")) {
						result = true;
						break;
					}
				}
			} catch (IOException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "'"+cmd+"' failed");
			} catch (InterruptedException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "'"+cmd+"' interrupted");
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						s_logger.warn("Failed to close process input stream", e);
					}
				}
				if (proc != null) {
					proc.destroy();
				}
			}
		}
		return result;
	}

	public static void unloadKernelModule(String interfaceName) throws KuraException {
		// FIXME: how to find the right kernel module by interface name?
		// Assume for now the interface name does not change
		if (KuraConstants.ReliaGATE_10_05.getTargetName().equals(TARGET_NAME) &&
				"wlan0".equals(interfaceName)) {
			SafeProcess proc = null;
			try {
				String cmd = "rmmod bcmdhd";
				s_logger.debug("Executing '{}'", cmd);
				proc = ProcessUtil.exec(cmd);
				if (proc.waitFor() != 0) {
					throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
				}
			} catch (IOException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e,"'rmmod bcmdhd' failed");
			} catch (InterruptedException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e,"'rmmod bcmdhd' interrupted");
			} finally {
				if (proc != null) {
					proc.destroy();
				}
			}
		} else {
			s_logger.debug("Kernel module unload not needed by platform '{}'", TARGET_NAME);
		}
	}

	public static void loadKernelModule(String interfaceName, WifiMode wifiMode) throws KuraException {
		// FIXME: how to find the right kernel module by interface name?
		// Assume for now the interface name does not change
		if (KuraConstants.ReliaGATE_10_05.getTargetName().equals(TARGET_NAME) &&
				"wlan0".equals(interfaceName)) {
			SafeProcess proc = null;
			String cmd = null;
			if (wifiMode == WifiMode.MASTER) {
				cmd = "modprobe -S 3.12.6 bcmdhd firmware_path=\"/system/etc/firmware/fw_bcm43438a0_apsta.bin\" op_mode=2";
			} else if (wifiMode == WifiMode.INFRA || wifiMode == WifiMode.ADHOC) {
				cmd = "modprobe -S 3.12.6 bcmdhd";
			} else {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Don't know what to load for WifiMode " + wifiMode);
			}

			try {
				s_logger.debug("Executing '{}'", cmd);
				proc = ProcessUtil.exec(cmd);
				if (proc.waitFor() != 0) {
					throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
				}
			} catch (IOException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "'"+cmd+"' failed");
			} catch (InterruptedException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, "'"+cmd+"' interrupted");
			} finally {
				if (proc != null) {
					proc.destroy();
				}
			}
		} else {
			s_logger.debug("Kernel module load not needed by platform '{}'", TARGET_NAME);
		}
	}
	
	public static boolean isKernelModuleLoadedForMode(String interfaceName, WifiMode wifiMode) throws KuraException {
		// FIXME: how to find the right kernel module by interface name?
		// Assume for now the interface name does not change.
		if (KuraConstants.ReliaGATE_10_05.getTargetName().equals(TARGET_NAME) &&
				"wlan0".equals(interfaceName)) {
			return false;
		} else {
			return true;
		}
	}
	
    public static boolean isWifiDeviceOn(String interfaceName) {
    	boolean deviceOn = false;
    	// FIXME Assume for now the interface name does not change
		if (KuraConstants.Reliagate_10_20.getTargetName().equals(TARGET_NAME) &&
				"wlan0".equals(interfaceName)) {
    		File fDevice = new File("/sys/bus/pci/devices/0000:01:00.0");
    		if (fDevice.exists()) {
    			deviceOn = true;
    		}
    	}
    	s_logger.debug("isWifiDeviceOn()? {}", deviceOn);
    	return deviceOn;
    }
    
    public static void turnWifiDeviceOn(String interfaceName) throws Exception {
    	// FIXME Assume for now the interface name does not change
		if (KuraConstants.Reliagate_10_20.getTargetName().equals(TARGET_NAME) &&
				"wlan0".equals(interfaceName)) {
    		s_logger.info("Turning Wifi device ON ...");
    		FileWriter fw = new FileWriter("/sys/bus/pci/rescan");
			fw.write("1");
			fw.close();
    	}
    }
    
    public static void turnWifiDeviceOff(String interfaceName) throws Exception {
    	// FIXME Assume for now the interface name does not change
		if (KuraConstants.Reliagate_10_20.getTargetName().equals(TARGET_NAME) &&
				"wlan0".equals(interfaceName)) {
    		s_logger.info("Turning Wifi device OFF ...");
			FileWriter fw = new FileWriter("/sys/bus/pci/devices/0000:01:00.0/remove");
			fw.write("1");
			fw.close();
    	}
    }
}
