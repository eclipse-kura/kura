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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpConfigReader implements NetworkConfigurationVisitor {
	
	private static final Logger s_logger = LoggerFactory.getLogger(DhcpConfigReader.class);
	
	//private static final String FILE_DIR = "/etc/";
	
	private static DhcpConfigReader s_instance;
	
	public static DhcpConfigReader getInstance() {
		if (s_instance == null) {
			s_instance = new DhcpConfigReader();
		}
		
		return s_instance;
	}
	
	@Override
	public void visit(NetworkConfiguration config) throws KuraException {
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getNetInterfaceConfigs();
        
		Properties kuraExtendedProps = KuranetConfig.getProperties();
		
        for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            getConfig(netInterfaceConfig, kuraExtendedProps);
        }
	}
	
	private void getConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig, Properties kuraExtendedProps) throws KuraException{
        String interfaceName = netInterfaceConfig.getName();
        s_logger.debug("Getting DHCP server config for " + interfaceName);
        
        NetInterfaceType type = netInterfaceConfig.getType(); 
        if (type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI) {
            //StringBuffer configFilename = new StringBuffer(FILE_DIR).append("dhcpd-").append(interfaceName).append(".conf");
        	String configFilename = DhcpServerManager.getConfigFilename(interfaceName);
            File dhcpConfigFile = new File(configFilename);
            
            if (dhcpConfigFile.exists()) {
                DhcpServerConfig4 dhcpServerConfig4 = populateConfig(interfaceName, dhcpConfigFile, kuraExtendedProps);

                if(dhcpServerConfig4 != null) {
            	    List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
                    
                    if(netInterfaceAddressConfigs == null) { 
                        throw KuraException.internalError("NetInterfaceAddress list is null for interface " + interfaceName);
                    } else if(netInterfaceAddressConfigs.size() == 0) {
                        throw KuraException.internalError("NetInterfaceAddress list is empty for interface " + interfaceName);
                    }
                    
                    for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
                        List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
                        
                        if(netConfigs == null) {
                            netConfigs = new ArrayList<NetConfig>();
                            if(netInterfaceAddressConfig instanceof NetInterfaceAddressConfigImpl) {
                                ((NetInterfaceAddressConfigImpl)netInterfaceAddressConfig).setNetConfigs(netConfigs);
                            } else if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
                                ((WifiInterfaceAddressConfigImpl)netInterfaceAddressConfig).setNetConfigs(netConfigs);
                            }
                        }
                        
                        netConfigs.add(dhcpServerConfig4);
                    }
                }
            } else {
                s_logger.debug("There is no current DHCP server configuration for " + interfaceName);
            }
        }
	}
	
	private DhcpServerConfig4 populateConfig(String interfaceName, File dhcpConfigFile, Properties kuraExtendedProps) throws KuraException {
		DhcpServerConfig4 dhcpServerConfig4 = null;
		DhcpServerTool dhcpServerTool = DhcpServerManager.getTool();
		if (dhcpServerTool == DhcpServerTool.DHCPD) {
			dhcpServerConfig4 = populateDhcpdConfig(interfaceName, dhcpConfigFile, kuraExtendedProps);
		} else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
			dhcpServerConfig4 = populateUdhcpdConfig(interfaceName, dhcpConfigFile, kuraExtendedProps);
		}
		return dhcpServerConfig4;
	}
	
	private DhcpServerConfig4 populateDhcpdConfig(String interfaceName, File dhcpConfigFile, Properties kuraExtendedProps) throws KuraException {
        DhcpServerConfigIP4 dhcpServerConfigIP4 = null;
	    BufferedReader br = null;
	    
		try {
			boolean enabled = false;
			IP4Address subnet = null;
			IP4Address netmask = null;
			IP4Address router = null;
			int defaultLeaseTime = -1;
			int maxLeaseTime = -1;
			IP4Address rangeStart = null;
			IP4Address rangeEnd = null;
			boolean passDns = true;
			ArrayList<IP4Address> dnsList = new ArrayList<IP4Address>();
			
			br = new BufferedReader(new FileReader(dhcpConfigFile));
			
			String line = null;
			while((line = br.readLine()) != null) {
				//TODO - really simple for now
				StringTokenizer st = new StringTokenizer(line);
				while(st.hasMoreTokens()) {
					String token = st.nextToken();
					if(token.equals("#")) {
						break;
					} else if(token.equals("subnet")) {
						subnet = (IP4Address) IPAddress.parseHostAddress(st.nextToken());
						if(!st.nextToken().equals("netmask")) {
							throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "invalid dhcp config file: " + dhcpConfigFile.getAbsolutePath());
						}
						netmask = (IP4Address) IPAddress.parseHostAddress(st.nextToken());
					} else if(token.equals("interface")) {
						interfaceName = st.nextToken();
						interfaceName = interfaceName.substring(0, interfaceName.indexOf(';'));
					} else if(token.equals("ddns-update-style")) {
						if(st.nextToken().equals("none;")) {
							passDns = false;
						}
					} else if(token.equals("ddns-updates")) {
						if(st.nextToken().equals("off;")) {
							passDns = false;
						}
					} else if(token.equals("default-lease-time")) {
						String leaseTime = st.nextToken();
						defaultLeaseTime = Integer.parseInt(leaseTime.substring(0, leaseTime.indexOf(';')));
					} else if(token.equals("max-lease-time")) {
						String leaseTime = st.nextToken();
						maxLeaseTime = Integer.parseInt(leaseTime.substring(0, leaseTime.indexOf(';')));
					} else if(token.equals("range")) {
						rangeStart = (IP4Address) IPAddress.parseHostAddress(st.nextToken());
						String rangeEndString = st.nextToken();
						rangeEndString = rangeEndString.substring(0, rangeEndString.indexOf(';'));
						rangeEnd = (IP4Address) IPAddress.parseHostAddress(rangeEndString);
					} else if(token.equals("option")) {
						String option = st.nextToken();
						if(option.equals("routers")) {
							String routerString = st.nextToken();
							routerString = routerString.substring(0, routerString.indexOf(';'));
							router = (IP4Address) IPAddress.parseHostAddress(routerString);
						} else if(option.equals("domain-name-servers")) {
							String dnsString = st.nextToken();
							dnsString = dnsString.substring(0, dnsString.indexOf(';'));
							dnsList.add((IP4Address) IPAddress.parseHostAddress(dnsString));
						}
					}
				}
			}

			StringBuilder sb = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.dhcpServer4.enabled");
			if(kuraExtendedProps != null && kuraExtendedProps.getProperty(sb.toString()) != null) {
				enabled = Boolean.parseBoolean(kuraExtendedProps.getProperty(sb.toString()));
			} else {
				//the file is present and the flag is not - so assume enabled is true
				enabled = true;
			}
			sb = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.dhcpServer4.passDns");
			if(kuraExtendedProps != null && kuraExtendedProps.getProperty(sb.toString()) != null) {
				passDns = Boolean.parseBoolean(kuraExtendedProps.getProperty(sb.toString()));
			} 
			
			short prefix = NetworkUtil.getNetmaskShortForm(netmask.getHostAddress());
			
			s_logger.debug("instantiating DHCP server configuration during init with " + 
					" | interfaceName: " + interfaceName +
					" | enabled: " + enabled +
					" | subnet: " + subnet.getHostAddress() +
					" | router: " + router.getHostAddress() +
					" | netmask: " + netmask.getHostAddress() +
					" | prefix: " + prefix +
					" | defaultLeaseTime: " + defaultLeaseTime +
					" | maxLeaseTime: " + maxLeaseTime +
					" | rangeStart: " + rangeStart.getHostAddress() +
					" | rangeEnd: " + rangeEnd.getHostAddress() +
					" | passDns: " + passDns +
					" | dnsList: " + dnsList.toString());
			
			dhcpServerConfigIP4 = new DhcpServerConfigIP4(interfaceName, enabled, subnet, router, netmask, defaultLeaseTime, maxLeaseTime,
					prefix, rangeStart, rangeEnd, passDns, dnsList);
			
		} catch (FileNotFoundException e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
		} finally{
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}	
		}
        
        return dhcpServerConfigIP4;
	}
	
	private DhcpServerConfig4 populateUdhcpdConfig(String interfaceName, File dhcpConfigFile, Properties kuraExtendedProps) throws KuraException {
		
		DhcpServerConfigIP4 dhcpServerConfigIP4 = null;
		BufferedReader br = null;

		try {
			boolean enabled = false;
			IP4Address subnet = null;
			IP4Address netmask = null;
			IP4Address router = null;
			int defaultLeaseTime = -1;
			int maxLeaseTime = -1;
			IP4Address rangeStart = null;
			IP4Address rangeEnd = null;
			boolean passDns = true;
			ArrayList<IP4Address> dnsList = new ArrayList<IP4Address>();

			br = new BufferedReader(new FileReader(dhcpConfigFile));

			String line = null;
			while ((line = br.readLine()) != null) {
				line.trim();
				if (line.startsWith("start")) {
					rangeStart = (IP4Address) IPAddress.parseHostAddress(line.substring("start".length()).trim());
				} else if (line.startsWith("end")) {
					rangeEnd = (IP4Address) IPAddress.parseHostAddress(line.substring("end".length()).trim());
				} else if (line.startsWith("opt router")) {
					router = (IP4Address) IPAddress.parseHostAddress(line.substring("opt router".length()).trim());
				} else if (line.startsWith("opt subnet")) {
					netmask = (IP4Address) IPAddress.parseHostAddress(line.substring("opt subnet".length()).trim());
				} else if (line.startsWith("opt lease")) {
					defaultLeaseTime = Integer.parseInt(line.substring("opt lease".length()).trim());
					maxLeaseTime = defaultLeaseTime;
				} else if (line.startsWith("opt dns")) {
					line = line.substring("opt dns".length()).trim();
					StringTokenizer st = new StringTokenizer(line);
					while (st.hasMoreTokens()) {
						dnsList.add((IP4Address) IPAddress.parseHostAddress(st.nextToken()));
					}
				}
			}
				
			subnet = (IP4Address) IPAddress.parseHostAddress(NetworkUtil.calculateNetwork(router.getHostAddress(), netmask.getHostAddress()));
				
			StringBuilder sb = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.dhcpServer4.enabled");
			if(kuraExtendedProps != null && kuraExtendedProps.getProperty(sb.toString()) != null) {
				enabled = Boolean.parseBoolean(kuraExtendedProps.getProperty(sb.toString()));
			} else {
				//the file is present and the flag is not - so assume enabled is true
				enabled = true;
			}
			sb = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.dhcpServer4.passDns");
			if(kuraExtendedProps != null && kuraExtendedProps.getProperty(sb.toString()) != null) {
				passDns = Boolean.parseBoolean(kuraExtendedProps.getProperty(sb.toString()));
			} 
				
			short prefix = NetworkUtil.getNetmaskShortForm(netmask.getHostAddress());
				
			s_logger.info("instantiating DHCP server configuration during init with " + 
						" | interfaceName: " + interfaceName +
						" | enabled: " + enabled +
						" | subnet: " + subnet.getHostAddress() +
						" | router: " + router.getHostAddress() +
						" | netmask: " + netmask.getHostAddress() +
						" | prefix: " + prefix +
						" | defaultLeaseTime: " + defaultLeaseTime +
						" | maxLeaseTime: " + maxLeaseTime +
						" | rangeStart: " + rangeStart.getHostAddress() +
						" | rangeEnd: " + rangeEnd.getHostAddress() +
						" | passDns: " + passDns +
						" | dnsList: " + dnsList.toString());
				
			dhcpServerConfigIP4 = new DhcpServerConfigIP4(interfaceName,
					enabled, subnet, router, netmask, defaultLeaseTime,
					maxLeaseTime, prefix, rangeStart, rangeEnd, passDns,
					dnsList);
			
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
		}
		
		return dhcpServerConfigIP4;
	}
}
