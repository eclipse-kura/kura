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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigWriter implements NetworkConfigurationVisitor {

	private static final Logger s_logger = LoggerFactory.getLogger(IfcfgConfigWriter.class);

	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String REDHAT_NET_CONFIGURATION_DIRECTORY = "/etc/sysconfig/network-scripts/";
	private static final String DEBIAN_NET_CONFIGURATION_FILE = "/etc/network/interfaces";
	private static final String DEBIAN_TMP_NET_CONFIGURATION_FILE = "/etc/network/interfaces.tmp";

	private static IfcfgConfigWriter s_instance;

	public static IfcfgConfigWriter getInstance() {
		if (s_instance == null) {
			s_instance = new IfcfgConfigWriter();
		}

		return s_instance;
	}

	@Override
	public void visit(NetworkConfiguration config) throws KuraException {
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getModifiedNetInterfaceConfigs();

		if(!netInterfaceConfigs.isEmpty()){
			for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
				writeConfig(netInterfaceConfig);
				writeKuraExtendedConfig(netInterfaceConfig);
			}
		}
	}

	private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
		if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) ||
			OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName()) ||
			OS_VERSION.equals(KuraConstants.BeagleBone.getImageName()) ||
			OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			NetInterfaceType type = netInterfaceConfig.getType();
			if(type == NetInterfaceType.LOOPBACK || type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI) {					
				if(configHasChanged(netInterfaceConfig)) {
					if(netInterfaceConfig.getType() != NetInterfaceType.LOOPBACK) {
						disableInterface(netInterfaceConfig.getName());
					}
					writeDebianConfig(netInterfaceConfig);
				}
			}
		}
		else {
			writeRedhatConfig(netInterfaceConfig);
		}
	}

	private void writeRedhatConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
		String interfaceName = netInterfaceConfig.getName();
		String outputFileName = new StringBuffer().append(REDHAT_NET_CONFIGURATION_DIRECTORY).append("ifcfg-").append(interfaceName).toString();
		String tmpOutputFileName = new StringBuffer().append(REDHAT_NET_CONFIGURATION_DIRECTORY).append("ifcfg-").append(interfaceName).append(".tmp").toString();
		s_logger.debug("Writing config for " + interfaceName);

		NetInterfaceType type = netInterfaceConfig.getType();
		if (type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI || type == NetInterfaceType.LOOPBACK) {
			StringBuffer sb = new StringBuffer();
			sb.append("# Networking Interface\n");

			//DEVICE
			sb.append("DEVICE=")
			.append(netInterfaceConfig.getName())
			.append("\n");

			//NAME
			sb.append("NAME=")
			.append(netInterfaceConfig.getName())
			.append("\n");

			//TYPE
			sb.append("TYPE=")
			.append(netInterfaceConfig.getType())
			.append("\n");

			List<?extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
			s_logger.debug("There are " + netInterfaceAddressConfigs.size() + " NetInterfaceConfigs in this configuration");

			boolean allowWrite = false;
			for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
				List<NetConfig> netConfigs =  netInterfaceAddressConfig.getConfigs();

				if(netConfigs != null) {
					for(NetConfig netConfig : netConfigs) {
						if(netConfig instanceof NetConfigIP4) {
							//ONBOOT
							sb.append("ONBOOT=");
							if(((NetConfigIP4) netConfig).isAutoConnect()) {
								sb.append("yes");
							} else {
								sb.append("no");
							}
							sb.append("\n");

							if(((NetConfigIP4) netConfig).isDhcp()) {
								//BOOTPROTO
								sb.append("BOOTPROTO=");
								s_logger.debug("new config is DHCP");
								sb.append("dhcp");
								sb.append("\n");
							} else {
								//BOOTPROTO
								sb.append("BOOTPROTO=");
								s_logger.debug("new config is STATIC");
								sb.append("static");
								sb.append("\n");

								//IPADDR
								sb.append("IPADDR=")
								.append(((NetConfigIP4) netConfig).getAddress().getHostAddress())
								.append("\n");

								//PREFIX
								sb.append("PREFIX=")
								.append(((NetConfigIP4) netConfig).getNetworkPrefixLength())
								.append("\n");

								//Gateway
								if(((NetConfigIP4) netConfig).getGateway() != null) {
									sb.append("GATEWAY=")
									.append(((NetConfigIP4) netConfig).getGateway().getHostAddress())
									.append("\n");
								}
							}

							//DEFROUTE
							if(((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
								sb.append("DEFROUTE=yes\n");
							} else {
								sb.append("DEFROUTE=no\n");
							}

							//DNS
							List<? extends IPAddress> dnsAddresses = ((NetConfigIP4) netConfig).getDnsServers();
							if(dnsAddresses != null && dnsAddresses.size() > 0) {
								for(int i=0; i<dnsAddresses.size(); i++) {
									IPAddress ipAddr = dnsAddresses.get(i);
									if (!(ipAddr.isLoopbackAddress()
											|| ipAddr.isLinkLocalAddress() 
											|| ipAddr.isMulticastAddress())) {
										sb.append("DNS")
										.append(i+1)
										.append("=")
										.append(ipAddr.getHostAddress())
										.append("\n");
									}
								}
							} else {
								s_logger.debug("no DNS entries");
							}

							allowWrite = true;
						}
					}
				} else {
					s_logger.debug("netConfigs is null");
				}

				// WIFI
				if(netInterfaceAddressConfig instanceof WifiInterfaceAddressConfig) {
					s_logger.debug("new config is a WifiInterfaceAddressConfig");
					sb.append("\n#Wireless configuration\n");

					// MODE
					String mode = null;
					WifiMode wifiMode = ((WifiInterfaceAddressConfig)netInterfaceAddressConfig).getMode(); 
					if (wifiMode == WifiMode.INFRA) {
						mode = "Managed";
					} else if (wifiMode == WifiMode.MASTER) {
						mode = "Master";
					} else if (wifiMode == WifiMode.ADHOC) {
						mode = "Ad-Hoc";
					} else if (wifiMode == null) {
						s_logger.error("WifiMode is null");
						mode = "null";
					} else {
						mode = wifiMode.toString();
					}
					sb.append("MODE=").append(mode).append("\n");
				}
			}

			if (allowWrite) {
				FileOutputStream fos = null;
				PrintWriter pw = null;
				try {
					fos = new FileOutputStream(tmpOutputFileName);
					pw = new PrintWriter(fos);
					pw.write(sb.toString());
					pw.flush();
					fos.getFD().sync();
				} catch (Exception e) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
				finally {
					if(fos != null){
						try{
							fos.close();
						}catch(IOException ex){
							s_logger.error("I/O Exception while closing BufferedReader!");
						}
					}
					if (pw != null) pw.close();
				}

				//move the file if we made it this far
				File tmpFile = new File(tmpOutputFileName);
				File outputFile = new File(outputFileName);
				try {
					if(!FileUtils.contentEquals(tmpFile, outputFile)) {
						if(tmpFile.renameTo(outputFile)){
							s_logger.trace("Successfully wrote network interface file for " + interfaceName);
						}else{
							s_logger.error("Failed to write network interface file");
							throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for network interface " + interfaceName);
						}
					} else {
						s_logger.info("Not rewriting network interfaces file for " + interfaceName + " because it is the same");
					}
				} catch(IOException e) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
			} else {
				s_logger.warn("writeNewConfig :: operation is not allowed");
			}
		}
	}

	private void writeDebianConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
		StringBuffer sb = new StringBuffer();
		File kuraFile = new File(DEBIAN_NET_CONFIGURATION_FILE);
		String iName = netInterfaceConfig.getName();
		boolean appendConfig = true;

		if(kuraFile.exists()) {
			//found our match so load the properties
			Scanner scanner = null;
			try {
				scanner = new Scanner (new FileInputStream(kuraFile));

				//need to loop through the existing file and replace only the desired interface
				while (scanner.hasNextLine()) {
					String noTrimLine = scanner.nextLine();
					String line = noTrimLine.trim();
					//ignore comments and blank lines
					if (!line.isEmpty()) {
						if (line.startsWith("#!kura!")) {
							line = line.substring("#!kura!".length());
						}
						
						if (!line.startsWith("#")) {
							String[] args = line.split("\\s+");
							//must be a line stating that interface starts on boot
							if(args.length > 1) {
								if (args[1].equals(iName)) {
									s_logger.debug("Found entry in interface file...");
									appendConfig = false;
									sb.append(debianWriteUtility(netInterfaceConfig, iName));
	
									//remove old config lines from the scanner
									while (scanner.hasNextLine() && !(line = scanner.nextLine()).isEmpty()) {
									}
									sb.append("\n");
								} else {
									sb.append(noTrimLine + "\n");
								}
							}
						} else {
							sb.append(noTrimLine + "\n");
						}
					} else {
						sb.append(noTrimLine + "\n");
					}
				}
			}
			catch (FileNotFoundException e1) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e1);
			}
			finally {
				scanner.close();
				scanner = null;				
			}

			// If config not present in file, append to end
			if (appendConfig) {
				s_logger.debug("Appending entry to interface file...");
				sb.append(debianWriteUtility(netInterfaceConfig, iName));
				sb.append("\n");
			}

			FileOutputStream fos = null;
			PrintWriter pw = null;
			try {
				fos = new FileOutputStream(DEBIAN_TMP_NET_CONFIGURATION_FILE);
				pw = new PrintWriter(fos);
				pw.write(sb.toString());
				pw.flush();
				fos.getFD().sync();
			} catch(Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			} finally {
				if(fos != null){
					try{
						fos.close();
					}catch(IOException ex){
						s_logger.error("I/O Exception while closing BufferedReader!");
					}
				}
				if (pw != null) pw.close();				
			}

			//move the file if we made it this far
			File tmpFile = new File(DEBIAN_TMP_NET_CONFIGURATION_FILE);
			File file = new File(DEBIAN_NET_CONFIGURATION_FILE);
			try {
				if(!FileUtils.contentEquals(tmpFile, file)) {
					if(tmpFile.renameTo(file)){
						s_logger.trace("Successfully wrote network interfaces file");
					}else{
						s_logger.error("Failed to write network interfaces file");
						throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration file for network interfaces");
					}
				} else {
					s_logger.info("Not rewriting network interfaces file because it is the same");
				}
			} catch(IOException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
	}

	private String debianWriteUtility(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig, String interfaceName) {
		
		List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
		StringBuffer sb = new StringBuffer();

		s_logger.debug("There are " + netInterfaceAddressConfigs.size() + " NetInterfaceAddressConfigs in this configuration");

		for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
			List<NetConfig> netConfigs =  netInterfaceAddressConfig.getConfigs();

			if(netConfigs != null) {
				for(NetConfig netConfig : netConfigs) {
					if(netConfig instanceof NetConfigIP4) {
						s_logger.debug("Writing netconfig " + netConfig.getClass().toString() + " for " + interfaceName);

						//ONBOOT
						if(((NetConfigIP4) netConfig).isAutoConnect()) {
							if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName()) && 
									(netInterfaceConfig.getType() == NetInterfaceType.WIFI) &&
									((NetConfigIP4) netConfig).isDhcp()) {
								sb.append("#!kura!auto " + interfaceName + "\n" );
							} else {
								sb.append("auto " + interfaceName + "\n" );
							}
						}

						//BOOTPROTO
						if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName()) && 
								(netInterfaceConfig.getType() == NetInterfaceType.WIFI) &&
								((NetConfigIP4) netConfig).isDhcp()) {
							sb.append("# Commented out to prevent wpa_supplicant from starting dhclient\n");
							sb.append("#!kura!iface " + interfaceName + " inet ");
						} else {
							sb.append("iface " + interfaceName + " inet ");
						}
						if(((NetConfigIP4) netConfig).isDhcp()) {
							s_logger.debug("new config is DHCP");
							sb.append("dhcp\n");
						} else {
							s_logger.debug("new config is STATIC");
							sb.append("static\n");
						}

						if(!((NetConfigIP4) netConfig).isDhcp()) {
							//IPADDR
							sb.append("\taddress ")
							.append(((NetConfigIP4) netConfig).getAddress().getHostAddress())
							.append("\n");

							//NETMASK
							sb.append("\tnetmask ")
							.append(((NetConfigIP4) netConfig).getSubnetMask().getHostAddress())
							.append("\n");

							//NETWORK
							//TODO: Handle Debian NETWORK value

							//Gateway
							if(((NetConfigIP4) netConfig).getGateway() != null) {
								sb.append("\tgateway ")
								.append(((NetConfigIP4) netConfig).getGateway().getHostAddress())
								.append("\n");
							}
						} else {
							// DEFROUTE
							if(((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN) {
								sb.append("post-up route del default dev ");
								sb.append(interfaceName);
								sb.append("\n");
							}
						}

						//DNS
						List<? extends IPAddress> dnsAddresses = ((NetConfigIP4) netConfig).getDnsServers();
						if (!dnsAddresses.isEmpty()) {
							boolean setDns = false;
							for (int i = 0; i < dnsAddresses.size(); i++) {
								if(!dnsAddresses.get(i).getHostAddress().equals("127.0.0.1")) {
									if(!setDns) {
										/* IAB:
										 * If DNS servers are listed, those entries will be appended to the /etc/resolv.conf
										 * file on every ifdown/ifup sequence resulting in multiple entries for the same servers.
										 * (Tested on 10-20, 10-10, and Raspberry Pi).
										 * Commenting out dns-nameservers in the /etc/network interfaces file allows DNS servers 
										 * to be picked up by the IfcfgConfigReader and be displayed on the Web UI but the 
										 * /etc/resolv.conf file will only be updated by Kura.    
										 */
										sb.append("\t#dns-nameservers ");
										setDns = true;
									}
									sb.append(dnsAddresses.get(i).getHostAddress() + " ");
								}
							}
							sb.append("\n");
						}
					}					
				}
			} else {
				s_logger.debug("netConfigs is null");
			}

			// WIFI
			if(netInterfaceAddressConfig instanceof WifiInterfaceAddressConfig) {
				s_logger.debug("new config is a WifiInterfaceAddressConfig");
			}
		}
		return sb.toString();
	}

	public static void writeKuraExtendedConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
		NetInterfaceStatus netInterfaceStatus = null;

		boolean gotNetConfigIP4 = false;

		List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();

		if(netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
			for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
				List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
				if(netConfigs != null && netConfigs.size() > 0) {
					for(int i=0; i<netConfigs.size(); i++) {
						NetConfig netConfig = netConfigs.get(i);
						if(netConfig instanceof NetConfigIP4) {
							netInterfaceStatus = ((NetConfigIP4) netConfig).getStatus();
							gotNetConfigIP4 = true;
						}
					}
				}
			}
		}

		if(!gotNetConfigIP4) {
			netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
		}

		s_logger.debug("Setting NetInterfaceStatus to " + netInterfaceStatus + " for " + netInterfaceConfig.getName());

		//set it all
		Properties kuraExtendedProps = KuranetConfig.getProperties();

		if(kuraExtendedProps == null) {
			s_logger.debug("kuraExtendedProps was null");
			kuraExtendedProps = new Properties();
		}
		StringBuilder sb = new StringBuilder().append("net.interface.").append(netInterfaceConfig.getName()).append(".config.ip4.status");
		kuraExtendedProps.put(sb.toString(), netInterfaceStatus.toString());

		//write it
		if(kuraExtendedProps != null && !kuraExtendedProps.isEmpty()) {
			try {
				KuranetConfig.storeProperties(kuraExtendedProps);
			} catch (IOException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
	}

	public static void removeKuraExtendedConfig(String interfaceName) throws KuraException {
		try {
			StringBuilder sb = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.ip4.status");
			KuranetConfig.deleteProperty(sb.toString());
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	private void disableInterface(String interfaceName) {
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		if(bundleContext != null) {
			ServiceReference<NetworkAdminService> sr = bundleContext.getServiceReference(NetworkAdminService.class);
			if (sr != null) {
				NetworkAdminService nas = bundleContext.getService(sr);
				try {
					nas.disableInterface(interfaceName);
				} catch (KuraException e) {
					s_logger.warn("Could not disable " + interfaceName, e);
				}
			}
		}
	}

	private Properties parseNetInterfaceAddressConfig(NetInterfaceAddressConfig netInterfaceAddressConfig) {
		Properties props = new Properties();

		List<NetConfig> netConfigs =  netInterfaceAddressConfig.getConfigs();

		if(netConfigs != null) {
			for(NetConfig netConfig : netConfigs) {
				if(netConfig instanceof NetConfigIP4) {
					NetConfigIP4 netConfigIP4 = (NetConfigIP4) netConfig;

					// ONBOOT
					props.setProperty("ONBOOT", netConfigIP4.isAutoConnect() ? "yes" : "no");

					//BOOTPROTO
					props.setProperty("BOOTPROTO", netConfigIP4.isDhcp() ? "dhcp" : "static");

					if(!netConfigIP4.isDhcp()) {
						//IPADDR
						if(netConfigIP4.getAddress() != null) {
							props.setProperty("IPADDR", netConfigIP4.getAddress().getHostAddress());
						}

						//NETMASK
						if(netConfigIP4.getSubnetMask() != null) {
							props.setProperty("NETMASK", netConfigIP4.getSubnetMask().getHostAddress());
						}

						//NETWORK
						//TODO: Handle Debian NETWORK value

						//GATEWAY
						if(netConfigIP4.getGateway() != null) {
							props.setProperty("GATEWAY", netConfigIP4.getGateway().getHostAddress());
							props.setProperty("DEFROUTE", "yes");
						} else {
							props.setProperty("DEFROUTE", "no");
						}
					} else {
						if(((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
							props.setProperty("DEFROUTE", "yes");
						} else {
							props.setProperty("DEFROUTE", "no");
						}
					}

					//DNS
					List<? extends IPAddress> dnsAddresses = ((NetConfigIP4) netConfig).getDnsServers();
					if (!dnsAddresses.isEmpty()) {
						for (int i = 0; i < dnsAddresses.size(); i++) {
							if(!dnsAddresses.get(i).getHostAddress().equals("127.0.0.1")) {
								props.setProperty("DNS" + Integer.toString(i+1), dnsAddresses.get(i).getHostAddress());
							}
						}
					}					
				}
			}
		} else {
			s_logger.debug("netConfigs is null");
		}

		return props;
	}

	private boolean configHasChanged(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
		Properties oldConfig = IfcfgConfigReader.parseDebianConfigFile(new File(DEBIAN_NET_CONFIGURATION_FILE), netInterfaceConfig.getName());
		Properties newConfig = parseNetInterfaceAddressConfig(netInterfaceConfig.getNetInterfaceAddresses().get(0));	// FIXME: assumes only one addressConfig

		s_logger.debug("Comparing configs for " + netInterfaceConfig.getName());
		s_logger.debug("oldProps: " + oldConfig);
		s_logger.debug("newProps: " + newConfig);

		if(!compare(oldConfig, newConfig, "ONBOOT")) {
			s_logger.debug("ONBOOT differs");
			return true;
		} else if(!compare(oldConfig, newConfig, "BOOTPROTO")) {
			s_logger.debug("BOOTPROTO differs");
			return true;
		} else if(!compare(oldConfig, newConfig, "IPADDR")) {
			s_logger.debug("IPADDR differs");
			return true;
		} else if(!compare(oldConfig, newConfig, "NETMASK")) {
			s_logger.debug("NETMASK differs");
			return true;
		} else if(!compare(oldConfig, newConfig, "GATEWAY")) {
			s_logger.debug("GATEWAY differs");
			return true;
		} else if(!compare(oldConfig, newConfig, "DNS1")) {
			s_logger.debug("DNS1 differs");
			return true;
		} else if(!compare(oldConfig, newConfig, "DNS2")) {
			s_logger.debug("DNS2 differs");
			return true;
		} else if(!compare(oldConfig, newConfig, "DNS3")) {
			s_logger.debug("DNS3 differs");
			return true;
		} else if (!compare(oldConfig, newConfig, "DEFROUTE")) {
			s_logger.debug("DEFROUTE differs");
			return true;
		}

		s_logger.debug("Configs match");
		return false;
	}

	private boolean compare(Properties prop1, Properties prop2, String key) {
		String val1 = prop1.getProperty(key);
		String val2 = prop2.getProperty(key);

		return (val1 != null) ? val1.equals(val2) : val2 == null;		
	}
}
