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
package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

	public static List<String> getInterfaceNames() throws KuraException {
		SafeProcess proc = null;
		BufferedReader br = null;
		List<String> ifaces = new ArrayList<String>();
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
				if(line.indexOf("Link encap:") > -1) {
					StringTokenizer st = new StringTokenizer(line);
					ifaces.add(st.nextToken());
				}
			}
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}  catch(InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} 
		finally {
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (proc != null) ProcessUtil.destroy(proc);
		}
		
		return ifaces;
	}

	public static List<String> getAllInterfaceNames() throws KuraException {
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
	
	public static boolean isUp(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return false;
		}
		
		boolean ret = false;
		LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);
		if (ifconfig != null) {
			if ((ifconfig.getInetAddress() != null) && (ifconfig.getInetMask() != null)) {
				ret = true;
			}
		}
		
		return ret;	
	}
	
	public static boolean isDhclientRunning(String interfaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(interfaceName.charAt(0))) {
			return false;
		}
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ps ax");
			if (proc.waitFor() != 0) {
				s_logger.error("error executing command --- ps ax --- exit value = " + proc.exitValue());
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unable to check in dhclient is running for " + interfaceName);
			}

			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while((line = br.readLine()) != null) {
				if(line.indexOf(interfaceName) > -1 && line.indexOf("dhclient") > -1) {					
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
			
			if (proc != null) ProcessUtil.destroy(proc);
		}

		return false;
	}

	public static String getCurrentIpAddress(String ifaceName) throws KuraException {
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

	public static String getCurrentNetmask(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return null;
		}
		String netmask = null;
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig " + ifaceName);
			if (proc.waitFor() != 0) {
                s_logger.warn("getCurrentNetmask() :: error executing command --- ifconfig {} --- exit value = {}", ifaceName , proc.exitValue());
                return netmask;
			}
			
			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while ((line = br.readLine()) != null) {
				if (line.indexOf(ifaceName) > -1) {
					if ((line = br.readLine()) != null) {
						int i = line.indexOf("Mask:");
						if (i > -1) {
							netmask = line.substring(i + 5);
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

		return netmask;
	}
	
	public static int getCurrentMtu(String ifaceName) throws KuraException {
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

	public static String getCurrentBroadcastAddress(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return null;
		}
		String broadcast = null;
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig " + ifaceName);
			if (proc.waitFor() != 0) {
                s_logger.warn("getCurrentBroadcastAddress() :: error executing command --- ifconfig {} --- exit value = {}", ifaceName , proc.exitValue());
                return broadcast;
			}
			
			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while ((line = br.readLine()) != null) {
				if (line.indexOf(ifaceName) > -1) {
					if ((line = br.readLine()) != null) {
						int i = line.indexOf("Bcast:");
						if (i > -1) {
							broadcast = line.substring(i + 6, line.indexOf(' ', i + 6));
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

		return broadcast;
	}
	
	public static String getCurrentPtpAddress(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return null;
		}
		String ptp = null;
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig " + ifaceName);
			if (proc.waitFor() != 0) {
                s_logger.warn("getCurrentPtpAddress() :: error executing command --- ifconfig {} --- exit value = {}", ifaceName , proc.exitValue());
                return ptp;
			}
			
			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while ((line = br.readLine()) != null) {
				if (line.indexOf(ifaceName) > -1) {
					if ((line = br.readLine()) != null) {
						int i = line.indexOf("P-t-P:");
						if (i > -1) {
							ptp = line.substring(i + 6, line.indexOf(' ', i + 6));
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

		return ptp;
	}

	public static boolean isLinkUp(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return false;
		}
		return isLinkUp(getType(ifaceName), ifaceName);
	}
	
	public static boolean isLinkUp(NetInterfaceType ifaceType, String ifaceName) throws KuraException {
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
		String[] searchFolders = new String[]{"/sbin/", "/usr/sbin/", "/bin/"};
		
		for (String folder : searchFolders) {
			File fTool = new File(folder + tool);
			if (fTool.exists()) {
				ret = true;
				break;
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
	
	public static LinuxIfconfig getInterfaceConfiguration(String ifaceName) throws KuraException {
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
					
			if (proc != null) ProcessUtil.destroy(proc);
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

	public static String getMacAddress(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return null;
		}
		
		String mac = null;
		if (s_ifconfigs.containsKey(ifaceName)) {
			LinuxIfconfig ifconfig = s_ifconfigs.get(ifaceName);
			mac = ifconfig.getMacAddress();
			s_logger.trace("getMacAddress() :: interface={}, MAC={}", ifaceName, mac);
		} else {
			s_ifconfigs.put(ifaceName, new LinuxIfconfig(ifaceName));
		}
		
		if (mac != null) {
			return mac;
		}
		
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig " + ifaceName);
			if (proc.waitFor() != 0) {
                s_logger.warn("getMacAddress() :: error executing command --- ifconfig {} --- exit value = {}", ifaceName , proc.exitValue());
                return mac;
			}

			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while((line = br.readLine()) != null) {
				int index = line.indexOf("HWaddr ");
				if(index > -1) {
					mac = line.substring(index + 7, line.length()-2);
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
			
			if (proc != null) ProcessUtil.destroy(proc);
		}
		
		s_logger.trace("getMacAddress() :: interface={}, MAC Address={}", ifaceName, mac);
		LinuxIfconfig ifconfig = s_ifconfigs.get(ifaceName);
		ifconfig.setMacAddress(mac);
		
		return mac;
	}
	
	public static byte[] getMacAddressBytes(String interfaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(interfaceName.charAt(0))) {
			return new byte[]{0, 0, 0, 0, 0, 0};
		}

		String macAddress = LinuxNetworkUtil.getMacAddress(interfaceName);		

		if(macAddress == null) {
			return new byte[]{0, 0, 0, 0, 0, 0};
		}

		macAddress = macAddress.replaceAll(":","");

		byte[] mac = new byte[6];
        for(int i=0; i<6; i++) {
        	mac[i] = (byte) ((Character.digit(macAddress.charAt(i*2), 16) << 4)
        					+ Character.digit(macAddress.charAt(i*2+1), 16));
        }
        
        return mac;
	}
	
	public static boolean isSupportsMulticast(String ifaceName) throws KuraException {		
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return false;
		}
		
		Boolean suportsMulticast = null;
		if (s_ifconfigs.containsKey(ifaceName)) {
			LinuxIfconfig ifconfig = s_ifconfigs.get(ifaceName);
			suportsMulticast = ifconfig.isMulticast();
			s_logger.trace("isSupportsMulticast() :: interface={}, multicast?={}", ifaceName, suportsMulticast);
		} else {
			s_ifconfigs.put(ifaceName, new LinuxIfconfig(ifaceName));
		}
		
		if (suportsMulticast != null) {
			return suportsMulticast;
		}
		
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("ifconfig " + ifaceName);
			if (proc.waitFor() != 0) {
                s_logger.warn("isSupportsMulticast() :: error executing command --- ifconfig {} --- exit value = {}", ifaceName , proc.exitValue());
                return false;
			}

			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while ((line = br.readLine()) != null) {
				if (line.contains("MULTICAST")) {
					suportsMulticast = true;
					break;
				}
			}
		} catch(Exception e) {
		    s_logger.warn("Error reading multicast info", e);
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
		
		s_logger.trace("isSupportsMulticast() :: interface={}, multicast?={}", ifaceName, suportsMulticast);
		LinuxIfconfig ifconfig = s_ifconfigs.get(ifaceName);
		ifconfig.setMulticast(suportsMulticast);
		
		return suportsMulticast;
	}

	public static boolean canPing(String ipAddress, int count) throws KuraException {
		SafeProcess proc = null;
		try {
			proc = ProcessUtil.exec(new StringBuilder().append("ping -c ").append(count).append(" ").append(ipAddress).toString());
			if(proc.waitFor() == 0) {
				return true;
			} else {
				return false;
			}
		} catch(IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		} catch (InterruptedException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}
	}
	
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
		
		s_logger.trace("getType() :: interface={}, type={}", ifaceName, ifaceType);
		LinuxIfconfig ifconfig = s_ifconfigs.get(ifaceName);
		ifconfig.setType(ifaceType);
		
		return ifaceType;
	}
	
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
		} else {
			s_ifconfigs.put(interfaceName, new LinuxIfconfig(interfaceName));
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
					proc.waitFor();
				}
				procEthtool = ProcessUtil.exec("ethtool -i " + interfaceName);
				if (procEthtool.waitFor() != 0) {
	                s_logger.warn("getEthernetDriver() :: error executing command --- ethtool -i {}", interfaceName);
	                return driver;
				}
			}
			
			//get the output
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
			if (procEthtool != null) ProcessUtil.destroy(procEthtool);
		}
		return driver;
	}
	
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
			
			if (proc != null) ProcessUtil.destroy(proc);
		}
		return capabilities;
	}
	
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
					//return mode;
				}
				
				br1 = new BufferedReader(new InputStreamReader(procIw.getInputStream()));
				while((line = br1.readLine()) != null) {
					int index = line.indexOf("type ");
					if(index > -1) {
						s_logger.debug("line: " + line);
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
		                return mode;
					}
					
					//get the output
					br2 = new BufferedReader(new InputStreamReader(procIwConfig.getInputStream()));
					while((line = br2.readLine()) != null) {
						int index = line.indexOf("Mode:");
						if(index > -1) {
							s_logger.debug("line: " + line);
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
			
			if (procIw != null) ProcessUtil.destroy(procIw);
			if (procIwConfig != null) ProcessUtil.destroy(procIwConfig);
		}
		
		return mode;
	}
	
	public static long getWifiBitrate(String ifaceName) throws KuraException {
		long bitRate = 0;
		
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return bitRate;
		}
		
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("iwconfig " + ifaceName);
			if (proc.waitFor() != 0) {
				s_logger.warn("error executing command --- iwconfig --- exit value = {}", proc.exitValue());
				return bitRate;
			}

			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while((line = br.readLine()) != null) {
				int index = line.indexOf("Bit Rate=");
				if(index > -1) {
					s_logger.debug("line: " + line);
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
			
			if (proc != null) ProcessUtil.destroy(proc);
		}
		
		return bitRate;
	}
	
	public static String getSSID(String ifaceName) throws KuraException {
		//ignore logical interfaces like "1-1.2"
		if (Character.isDigit(ifaceName.charAt(0))) {
			return null;
		}
		String ssid = null;
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			//start the process
			proc = ProcessUtil.exec("iwconfig " + ifaceName);
			if (proc.waitFor() != 0) {
				s_logger.warn("error executing command --- ifconfig --- exit value = {}", proc.exitValue());
				return ssid;
			}

			//get the output
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while((line = br.readLine()) != null) {
				int index = line.indexOf("ESSID:");
				if(index > -1) {
					s_logger.debug("line: " + line);
					String lineSub = line.substring(index);
					StringTokenizer st = new StringTokenizer(lineSub);
					String ssidStr = st.nextToken();
					if(ssidStr.startsWith("\"") && ssidStr.endsWith("\"")) {
						ssid = ssidStr.substring(lineSub.indexOf('"') + 1, lineSub.lastIndexOf('"')); // get value between quotes
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
			
			if (proc != null) ProcessUtil.destroy(proc);
		}
		
		return ssid;
	}
		
	public static void disableInterface(String interfaceName) throws Exception {
		if(interfaceName != null) {
			//ignore logical interfaces like "1-1.2"
			if (Character.isDigit(interfaceName.charAt(0))) {
				return;
			}
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())) {
				LinuxProcessUtil.start("ifdown " + interfaceName + "\n");
				LinuxProcessUtil.start("ifconfig " + interfaceName + " down\n");
			} else {
				if (isUp(interfaceName)) {
					LinuxProcessUtil.start("ifdown " + interfaceName + "\n");		
				}
			}
			
			//always leave the Ethernet Controller powered
			powerOnEthernetController(interfaceName);
		}
	}
	
	public static void enableInterface(String interfaceName) throws Exception {
		if(interfaceName != null) {
			//ignore logical interfaces like "1-1.2"
			if (Character.isDigit(interfaceName.charAt(0))) {
				return;
			}
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())) {
				LinuxProcessUtil.start("ifconfig " + interfaceName + " up\n");
				LinuxProcessUtil.start("ifup -f " + interfaceName + "\n");
			} else {
				LinuxProcessUtil.start("ifup " + interfaceName + "\n");						
			}
		}
	}
	
	public static void powerOnEthernetController(String interfaceName) throws KuraException {
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
			
			if (proc != null) ProcessUtil.destroy(proc);
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
			if (proc != null) ProcessUtil.destroy(proc);
		}
	}
	
	public static boolean isEthernetControllerPowered(String interfaceName) throws KuraException {
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
			
			if (proc != null) ProcessUtil.destroy(proc);
		}

		return false;
	}
}
