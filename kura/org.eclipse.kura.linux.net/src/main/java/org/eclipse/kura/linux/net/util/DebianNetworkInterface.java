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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebianNetworkInterface extends GenericNetworkInterface {
	private static final Logger s_logger = LoggerFactory.getLogger(DebianNetworkInterface.class);
	
	public static NetInterfaceConfig getCurrentConfiguration(
			String interfaceName, NetInterfaceType type,
			NetInterfaceStatus status, boolean dhcpServerEnabled,
			boolean passDns)
			throws KuraException {
		NET_CONFIGURATION_DIRECTORY = "/etc/network/";
		
		try {
		    NetInterfaceConfig netInterfaceConfig = null;
		    
			//build up the configuration
			Properties kuraProps = new Properties();
			
			kuraFile = new File(NET_CONFIGURATION_DIRECTORY + "interfaces");
			if(kuraFile.exists()) {
				//found our match so load the properties
				Scanner scanner = null;

				s_logger.debug("getting args for {}", interfaceName);
				
				//Debian specific routine to create Properties object
	            kuraProps.setProperty("ONBOOT", "no");

	            try {
	            	scanner = new Scanner (new FileInputStream(kuraFile));
	                while (scanner.hasNextLine()) {
	                    String line = scanner.nextLine().trim();
	                    //ignore comments and blank lines
	                    if (!line.isEmpty() && !line.startsWith("#")) {
	                        String[] args = line.split("\\s+");
	                        try {
	                            //must be a line stating that interface starts on boot
	                        	if (args[0].equals("auto") && args[1].equals(interfaceName)) {
	                                kuraProps.setProperty("ONBOOT", "yes");
	                            }
	                        	//once the correct interface is found, read all configuration information
	                            else if (args[0].equals("iface") && args[1].equals(interfaceName)) {
	                                kuraProps.setProperty("BOOTPROTO", args[3]);
	                                while (!(line = scanner.nextLine()).isEmpty()) {
	                                    args = line.trim().split("\\s+");
	                                    if (args[0].equals("mtu"))
	                                        kuraProps.setProperty("mtu", args[1]);
	                                    else if (args[0].equals( "address"))
	                                        kuraProps.setProperty("IPADDR", args[1]);
	                                    else if (args[0].equals( "netmask"))
	                                        kuraProps.setProperty("NETMASK", args[1]);
	                                    else if (args[0].equals( "gateway"))
	                                        kuraProps.setProperty("GATEWAY", args[1]);
	                                    else if(args[0].equals("dns-nameservers")) {
	                                    	if (args.length == 2)
	                                    		kuraProps.setProperty("DNS1", args[1]);
	                                    	else if (args.length == 3) {
	                                    		kuraProps.setProperty("DNS1", args[1]);
	                                    		kuraProps.setProperty("DNS2", args[2]);
	                                    	} else {
	                                    		s_logger.warn("Possible malformed configuration file (dns) for " + interfaceName);
						}
	                                    }
	                                }
	                                //Debian makes assumptions about lo, handle those here
	                                if (interfaceName.equals("lo") && kuraProps.getProperty("IPADDR") == null && kuraProps.getProperty("NETMASK") == null) {
	                                	kuraProps.setProperty("IPADDR", "127.0.0.1");
	                                	kuraProps.setProperty("NETMASK", "255.0.0.0");
	                                }
	                                break;
	                            }
	                        } catch (Exception e) {
	                        	s_logger.warn("Possible malformed configuration file for " + interfaceName);
					e.printStackTrace();
	                        }
	                    }
	                }
	                
	                //if BOOTPROTO is null then interface was not found in the config file
	                if (kuraProps.getProperty("BOOTPROTO") != null)
	                	netInterfaceConfig = getCurrentConfig(interfaceName, type, status, dhcpServerEnabled, passDns, kuraProps);
	            } finally {
	                scanner.close();
	            }
            } else if(type == NetInterfaceType.MODEM) {
                s_logger.debug("getting args for {}", interfaceName);
                kuraProps.setProperty("BOOTPROTO", "dhcp");
                netInterfaceConfig = getCurrentConfig(interfaceName, type, status, dhcpServerEnabled, passDns, kuraProps);
            }
		
			return netInterfaceConfig;
		} catch (FileNotFoundException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public static void writeNewConfig(NetInterfaceConfig netInterfaceConfig) throws KuraException {
		Scanner scanner = null;
		try {
			StringBuffer sb = new StringBuffer();
			String outputFileName = NET_CONFIGURATION_DIRECTORY + "interfaces";
			kuraFile = new File(NET_CONFIGURATION_DIRECTORY + "interfaces");
			String iName = netInterfaceConfig.getName();
			
			if(kuraFile.exists()) {
				//found our match so load the properties
				scanner = new Scanner (new FileInputStream(kuraFile));
			
				//need to loop through the existing file and replace only the desired interface
				while (scanner.hasNextLine()) {
					String noTrimLine = scanner.nextLine();
					String line = noTrimLine.trim();
					
                    //ignore comments and blank lines
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String[] args = line.split("\\s+");
                        try {
                            //must be a line stating that interface starts on boot
                        	if (args[1].equals(iName)) {
                                
								List<? extends NetInterfaceAddressConfig> netInterfaceConfigs = netInterfaceConfig.getNetInterfaceAddresses();
								s_logger.debug("There are " + netInterfaceConfigs.size() + " NetInterfaceConfigs in this configuration");
								
								for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceConfigs) {
									List<NetConfig> netConfigs =  netInterfaceAddressConfig.getConfigs();
					
									if(netConfigs != null) {
										for(NetConfig netConfig : netConfigs) {
											if(netConfig instanceof NetConfigIP4) {
												//ONBOOT
												if(((NetConfigIP4) netConfig).isAutoConnect())
													sb.append("auto " + iName + "\n" );
												
												//BOOTPROTO
												sb.append("iface " + iName + " inet ");
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
					
													//DNS
													List<? extends IPAddress> dnsAddresses = ((NetConfigIP4) netConfig).getDnsServers();
													if (!dnsAddresses.isEmpty()) {
														sb.append("\tdns-nameservers ");
														for (int i = 0; i < dnsAddresses.size(); i++) {
															sb.append(dnsAddresses.get(i).getHostAddress() + " ");
														}
														sb.append("\n");
													}
												} else {
													// DEFROUTE
						                            if(((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN) {
						                            	sb.append("post-up route del default dev ");
						                            	sb.append(iName);
						                            	sb.append("\n");
						                            }
												}
											}
										}
									} else {
										s_logger.debug("netConfigs is null");
									}
									
									// WIFI
									if(netInterfaceAddressConfig instanceof WifiInterfaceAddressConfig) {
										s_logger.debug("new config is a WifiInterfaceAddressConfig");
										sb.append("\n#Wireless configuration\n");
										
										//TODO: Handle Wireless
									}
								}
								//remove old config lines from the scanner
								while (!(line = scanner.nextLine()).isEmpty()) {
								}
								sb.append("\n");
                        	}
                        	else
                        		sb.append(noTrimLine + "\n");
                        } catch(Exception e) {
                        	
                        }
                    }
                    else
                    	sb.append(noTrimLine + "\n");
				}
			}
			
			FileOutputStream fos = new FileOutputStream(outputFileName);
			PrintWriter pw = new PrintWriter(fos);
			pw.write(sb.toString());
			pw.flush();
			fos.getFD().sync();
			pw.close();
			fos.close();
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally{
			if(scanner != null){
				scanner.close();
			}
		}
	}
}
