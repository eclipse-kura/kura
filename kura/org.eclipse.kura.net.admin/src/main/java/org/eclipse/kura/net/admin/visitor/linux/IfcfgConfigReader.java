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
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigReader implements NetworkConfigurationVisitor {

	private static final Logger s_logger = LoggerFactory
			.getLogger(IfcfgConfigReader.class);

	private static final String OS_VERSION = System
			.getProperty("kura.os.version");
	private static final String REDHAT_NET_CONFIGURATION_DIRECTORY = "/etc/sysconfig/network-scripts/";
	private static final String DEBIAN_NET_CONFIGURATION_DIRECTORY = "/etc/network/";

	private static IfcfgConfigReader s_instance;

	public static IfcfgConfigReader getInstance() {
		if (s_instance == null) {
			s_instance = new IfcfgConfigReader();
		}

		return s_instance;
	}

	@Override
	public void visit(NetworkConfiguration config) throws KuraException {
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
				.getNetInterfaceConfigs();

		Properties kuraExtendedProps = KuranetConfig.getProperties();

		for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
			getConfig(netInterfaceConfig, kuraExtendedProps);
		}
	}

	private void getConfig(
			NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig,
			Properties kuraExtendedProps) throws KuraException {
		String interfaceName = netInterfaceConfig.getName();
		s_logger.debug("Getting config for " + interfaceName);

		NetInterfaceType type = netInterfaceConfig.getType();
		if (type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI
				|| type == NetInterfaceType.LOOPBACK) {

			NetInterfaceStatus netInterfaceStatus = null;

			StringBuilder sb = new StringBuilder().append("net.interface.")
					.append(netInterfaceConfig.getName())
					.append(".config.ip4.status");
			if (kuraExtendedProps != null
					&& kuraExtendedProps.getProperty(sb.toString()) != null) {
				netInterfaceStatus = NetInterfaceStatus
						.valueOf(kuraExtendedProps.getProperty(sb.toString()));
			} else {
				netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
			}
			s_logger.debug("Setting NetInterfaceStatus to "
					+ netInterfaceStatus + " for "
					+ netInterfaceConfig.getName());

			boolean autoConnect = false;
			// int mtu = -1; // MTU is not currently used
			boolean dhcp = false;
			IP4Address address = null;
			String ipAddress = null;
			String prefixString = null;
			String netmask = null;
			String broadcast = null;
			String gateway = null;

			File ifcfgFile = null;
			if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) ||
				OS_VERSION.equals(KuraConstants.Raspberry_Pi .getImageName()) || 
				OS_VERSION.equals(KuraConstants.BeagleBone.getImageName()) ||
				OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
				ifcfgFile = new File(DEBIAN_NET_CONFIGURATION_DIRECTORY
						+ "interfaces");
			} else {
				ifcfgFile = new File(REDHAT_NET_CONFIGURATION_DIRECTORY
						+ "ifcfg-" + interfaceName);
			}

			if (ifcfgFile.exists()) {
				Properties kuraProps;
				// found our match so load the properties
				if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) ||
					OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName()) ||
					OS_VERSION.equals(KuraConstants.BeagleBone.getImageName()) ||
					OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
					kuraProps = parseDebianConfigFile(ifcfgFile, interfaceName);
				} else {
					kuraProps = parseRedhatConfigFile(ifcfgFile, interfaceName);
				}

				if (kuraProps != null) {
					String onBoot = kuraProps.getProperty("ONBOOT");
					if ("yes".equals(onBoot)) {
						s_logger.debug("Setting autoConnect to true");
						autoConnect = true;
					} else {
						s_logger.debug("Setting autoConnect to false");
						autoConnect = false;
					}

					// override MTU with what is in config if it is present
					/* IAB: MTU is not currently used
					String stringMtu = kuraProps.getProperty("MTU");
					if (stringMtu == null) {
						try {
							mtu = LinuxNetworkUtil.getCurrentMtu(interfaceName);
						} catch (KuraException e) {
							// just assume ???
							if (interfaceName.equals("lo")) {
								mtu = 16436;
							} else {
								mtu = 1500;
							}
						}
					} else {
						mtu = Short.parseShort(stringMtu);
					}
					*/
					// get the bootproto
					String bootproto = kuraProps.getProperty("BOOTPROTO");
					if (bootproto == null) {
						bootproto = "static";
					}

					// get the defroute
					String defroute = kuraProps.getProperty("DEFROUTE");
					if (defroute == null) {
						defroute = "no";
					}

					// correct the status if needed by validating against the
					// actual properties
					if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusDisabled) {
						if (autoConnect) {
							if (defroute.equals("no")) {
								netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledLAN;
							} else {
								netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledWAN;
							}
						}
					}

					// check for dhcp or static configuration
					try {
						ipAddress = kuraProps.getProperty("IPADDR");
						prefixString = kuraProps.getProperty("PREFIX");
						netmask = kuraProps.getProperty("NETMASK");
						broadcast = kuraProps.getProperty("BROADCAST");
						try {
							gateway = kuraProps.getProperty("GATEWAY");
							s_logger.debug("got gateway for " + interfaceName
									+ ": " + gateway);
						} catch (Exception e) {
							s_logger.warn("missing gateway stanza for "
									+ interfaceName);
						}

						if (bootproto.equals("dhcp")) {
							s_logger.debug("currently set for DHCP");
							dhcp = true;
							ipAddress = null;
							netmask = null;
						} else {
							s_logger.debug("currently set for static address");
							dhcp = false;
						}
					} catch (Exception e) {
						throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
								"malformatted config file: "
										+ ifcfgFile.toString(), e);
					}

					if (ipAddress != null && !ipAddress.isEmpty()) {
						try {
							address = (IP4Address) IPAddress
									.parseHostAddress(ipAddress);
						} catch (UnknownHostException e) {
							s_logger.warn("Error parsing address: "
									+ ipAddress, e);
						}
					}

					// make sure at least prefix or netmask is present if static
					if (autoConnect && !dhcp && prefixString == null
							&& netmask == null) {
						throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
								"malformatted config file: "
										+ ifcfgFile.toString()
										+ " must contain NETMASK and/or PREFIX");
					}
				}

				List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
						.getNetInterfaceAddresses();

				if (netInterfaceAddressConfigs == null) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
							"InterfaceAddressConfig list is null");
				} else if (netInterfaceAddressConfigs.size() == 0) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
							"InterfaceAddressConfig list has no entries");
				}

				for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
					List<NetConfig> netConfigs = netInterfaceAddressConfig
							.getConfigs();

					if (netConfigs == null) {
						netConfigs = new ArrayList<NetConfig>();
						if (netInterfaceAddressConfig instanceof NetInterfaceAddressConfigImpl) {
							((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig)
									.setNetConfigs(netConfigs);
							if (dhcp) {
								// Replace with DNS provided by DHCP server
								// (displayed as read-only in Denali)
								List<? extends IPAddress> dhcpDnsServers = getDhcpDnsServers(
										interfaceName,
										netInterfaceAddressConfig.getAddress());
								if (dhcpDnsServers != null) {
									((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig)
											.setDnsServers(dhcpDnsServers);
								}
							}
						} else if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
							((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig)
									.setNetConfigs(netConfigs);
							if (dhcp) {
								// Replace with DNS provided by DHCP server
								// (displayed as read-only in Denali)
								List<? extends IPAddress> dhcpDnsServers = getDhcpDnsServers(
										interfaceName,
										netInterfaceAddressConfig.getAddress());
								if (dhcpDnsServers != null) {
									((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig)
											.setDnsServers(dhcpDnsServers);
								}
							}
						}
					}

					NetConfigIP4 netConfig = new NetConfigIP4(
							netInterfaceStatus, autoConnect);
					setNetConfigIP4(netConfig, autoConnect, dhcp, address,
							gateway, prefixString, netmask, kuraProps);
					s_logger.debug("NetConfig: " + netConfig.toString());
					netConfigs.add(netConfig);
				}
			}
		}
	}

	private Properties parseRedhatConfigFile(File ifcfgFile,
			String interfaceName) {
		Properties kuraProps = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(ifcfgFile);
			kuraProps.load(fis);
			
		} catch (Exception e) {
			s_logger.error("Could not get configuration for " + interfaceName,
					e);
		} 
		finally {
			if(fis != null){
				try{
					fis.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}	
		}
		return kuraProps;
	}

	static Properties parseDebianConfigFile(File ifcfgFile, String interfaceName)
			throws KuraException {
		Properties kuraProps = new Properties();
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileInputStream(ifcfgFile));

			// Debian specific routine to create Properties object
			kuraProps.setProperty("ONBOOT", "no");

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine().trim();
					// ignore comments and blank lines
					if (!line.isEmpty() && !line.startsWith("#")) {
						String[] args = line.split("\\s+");
						try {
							// must be a line stating that interface starts on
							// boot
							if (args[0].equals("auto")
									&& args[1].equals(interfaceName)) {
								s_logger.debug("Setting ONBOOT to yes for "
										+ interfaceName);
								kuraProps.setProperty("ONBOOT", "yes");
							}
							// once the correct interface is found, read all
							// configuration information
							else if (args[0].equals("iface")
									&& args[1].equals(interfaceName)) {
								kuraProps.setProperty("BOOTPROTO", args[3]);
								if (args[3].equals("dhcp")) {
									kuraProps.setProperty("DEFROUTE", "yes");
								}
								while (scanner.hasNextLine()) {
									line = scanner.nextLine().trim();
									if (line != null && !line.isEmpty()) {
										if (line.startsWith("auto")
												|| line.startsWith("iface")) {
											break;
										}

										args = line.trim().split("\\s+");
										if (args[0].equals("mtu")) {
											kuraProps.setProperty("mtu",
													args[1]);
										} else if (args[0].equals("address")) {
											kuraProps.setProperty("IPADDR",
													args[1]);
										} else if (args[0].equals("netmask")) {
											kuraProps.setProperty("NETMASK",
													args[1]);
										} else if (args[0].equals("gateway")) {
											kuraProps.setProperty("GATEWAY",
													args[1]);
											kuraProps.setProperty("DEFROUTE",
													"yes");
										} else if (args[0]
												.equals("#dns-nameservers")) {
											/*
											 * IAB: 
											 * If DNS servers are listed,
											 * those entries will be appended to
											 * the /etc/resolv.conf file on
											 * every ifdown/ifup sequence
											 * resulting in multiple entries for
											 * the same servers. (Tested on
											 * 10-20, 10-10, and Raspberry Pi).
											 * Commenting out dns-nameservers in
											 * the /etc/network interfaces file
											 * allows DNS servers to be picked
											 * up by the IfcfgConfigReader and
											 * be displayed on the Web UI but
											 * the /etc/resolv.conf file will
											 * only be updated by Kura.
											 */
											if (args.length > 1) {
												for (int i = 1; i < args.length; i++) {
													kuraProps
															.setProperty(
																	"DNS"
																			+ Integer
																					.toString(i),
																	args[i]);
												}
											}
										} else if (args[0].equals("post-up")) {
											StringBuffer sb = new StringBuffer();
											for (int i = 1; i < args.length; i++) {
												sb.append(args[i]);
												sb.append(' ');
											}
											if (sb.toString()
													.trim()
													.equals("route del default dev "
															+ interfaceName)) {
												kuraProps.setProperty(
														"DEFROUTE", "no");
											}
										}
									}
								}
								// Debian makes assumptions about lo, handle
								// those here
								if (interfaceName.equals("lo")
										&& kuraProps.getProperty("IPADDR") == null
										&& kuraProps.getProperty("NETMASK") == null) {
									kuraProps
											.setProperty("IPADDR", "127.0.0.1");
									kuraProps.setProperty("NETMASK",
											"255.0.0.0");
								}
								break;
							}
						} catch (Exception e) {
							s_logger.warn(
									"Possible malformed configuration file for "
											+ interfaceName, e);
						}
					}
				}
			
		} catch (FileNotFoundException err) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, err);
		} finally {
			if(scanner != null){
				scanner.close();
			}
		}
		return kuraProps;
	}

	private static void setNetConfigIP4(NetConfigIP4 netConfig,
			boolean autoConnect, boolean dhcp, IP4Address address,
			String gateway, String prefixString, String netmask,
			Properties kuraProps) throws KuraException {

		netConfig.setDhcp(dhcp);
		if (kuraProps != null) {
			// get the DNS
			List<IP4Address> dnsServers = new ArrayList<IP4Address>();
			int count = 1;
			while (true) {
				String dns = null;
				if ((dns = kuraProps.getProperty("DNS" + count)) != null) {
					try {
						dnsServers.add((IP4Address) IP4Address
								.parseHostAddress(dns));
					} catch (UnknownHostException e) {
						s_logger.error("Could not parse address: " + dns, e);
					}
					count++;
				} else {
					break;
				}
			}
			netConfig.setDnsServers(dnsServers);

			if (!dhcp) {
				netConfig.setAddress(address);
				// TODO ((NetConfigIP4)netConfig).setDomains(domains);
				if (gateway != null && !gateway.isEmpty()) {
					try {
						netConfig.setGateway((IP4Address) IP4Address
								.parseHostAddress(gateway));
					} catch (UnknownHostException e) {
						s_logger.error("Could not parse address: " + gateway, e);
					}
				}
				if (prefixString != null) {
					short prefix = Short.parseShort(prefixString);
					netConfig.setNetworkPrefixLength(prefix);
				}
				if (netmask != null) {
					netConfig.setNetworkPrefixLength(NetworkUtil
							.getNetmaskShortForm(netmask));
				}
				// TODO netConfig.setWinsServers(winsServers);
			}
		}
	}

	private static List<? extends IPAddress> getDhcpDnsServers(
			String interfaceName, IPAddress address) {
		List<IPAddress> dnsServers = null;

		if (address != null) {
			LinuxDns linuxDns = LinuxDns.getInstance();
			try {
				dnsServers = linuxDns.getDhcpDnsServers(interfaceName, address);
			} catch (KuraException e) {
				s_logger.error("Error getting DHCP DNS servers", e);
			}
		}

		return dnsServers;
	}
}
