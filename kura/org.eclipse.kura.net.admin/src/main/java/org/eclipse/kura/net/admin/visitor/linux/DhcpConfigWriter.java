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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpConfigWriter implements NetworkConfigurationVisitor {
	
private static final Logger s_logger = LoggerFactory.getLogger(DhcpConfigWriter.class);
	
	//private static final String FILE_DIR = "/etc/";
	//private static final String PID_FILE_DIR = "/var/run/";
	
	private static DhcpConfigWriter s_instance;
	
	public static DhcpConfigWriter getInstance() {
		if (s_instance == null) {
			s_instance = new DhcpConfigWriter();
		}
		
		return s_instance;
	}

	@Override
	public void visit(NetworkConfiguration config) throws KuraException {
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getModifiedNetInterfaceConfigs();
		 
		for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
			if (netInterfaceConfig.getType() == NetInterfaceType.ETHERNET || netInterfaceConfig.getType() == NetInterfaceType.WIFI) {
				writeConfig(netInterfaceConfig);
				writeKuraExtendedConfig(netInterfaceConfig, KuranetConfig.getProperties());
			}
		}
	}
	
	/*
	private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
		DhcpServerTool dhcpServerTool = DhcpServerManager.getTool();
		if (dhcpServerTool == DhcpServerTool.DHCPD) {
			 writeDhcpdConfig(netInterfaceConfig);
		} else if (dhcpServerTool == DhcpServerTool.DHCPD) {
			writeUdhcpdConfig(netInterfaceConfig);
		}
	}
	*/
	
	private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
		String interfaceName = netInterfaceConfig.getName();
		
		/*
		String dhcpConfigFileName = new StringBuffer().append(FILE_DIR).append("dhcpd-").append(interfaceName).append(".conf").toString();
		String tmpDhcpConfigFileName = new StringBuffer().append(FILE_DIR).append("dhcpd-").append(interfaceName).append(".conf").append(".tmp").toString();		
		*/
		String dhcpConfigFileName = DhcpServerManager.getConfigFilename(interfaceName);
		String tmpDhcpConfigFileName = new StringBuilder(dhcpConfigFileName).append(".tmp").toString();
		
		s_logger.debug("Writing DHCP config for " + interfaceName);
		
		List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
		
		if (netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
			for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
				List<NetConfig> netConfigs =  netInterfaceAddressConfig.getConfigs();
				
				if (netConfigs != null) {
					for (NetConfig netConfig : netConfigs) {
						if(netConfig instanceof DhcpServerConfig4) {
							DhcpServerConfig4 dhcpServerConfig = (DhcpServerConfig4) netConfig;
							writeConfigFile(tmpDhcpConfigFileName, interfaceName, dhcpServerConfig);
							//move the file if we made it this far and they are different
							File tmpDhcpConfigFile = new File(tmpDhcpConfigFileName);
							File dhcpConfigFile = new File(dhcpConfigFileName);
							try {
								if(!FileUtils.contentEquals(tmpDhcpConfigFile, dhcpConfigFile)) {
									if(tmpDhcpConfigFile.renameTo(dhcpConfigFile)) {
										s_logger.trace("Successfully wrote DHCP config file");
									} else {
										s_logger.error("Failed to write DHCP config file for " + interfaceName);
										throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration files for dhcp server: " + interfaceName);
									}
								} else {
									s_logger.info("Not rewriting DHCP config file for " + interfaceName + " because it is the same");
								}
							} catch (IOException e) {
								throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration files for dhcp servers", e);
							}
						}
					}
				}
			}
		}
	}
	
	private void writeConfigFile(String configFileName, String ifaceName, DhcpServerConfig4 dhcpServerConfig) throws KuraException {
		FileOutputStream fos = null;
		PrintWriter pw = null;
		try {
			fos = new FileOutputStream(configFileName);
			pw = new PrintWriter(fos);
			s_logger.trace("writing to {} with: {}", configFileName, dhcpServerConfig.toString());	
			DhcpServerTool dhcpServerTool = DhcpServerManager.getTool();
			if (dhcpServerTool == DhcpServerTool.DHCPD) {
				pw.print(dhcpServerConfig.toString());
			} else if (dhcpServerTool == DhcpServerTool.UDHCPD) {
				pw.println("start " + dhcpServerConfig.getRangeStart().getHostAddress());
				pw.println("end " + dhcpServerConfig.getRangeEnd().getHostAddress());
				pw.println("interface " + ifaceName);
				pw.println("pidfile " + DhcpServerManager.getPidFilename(ifaceName));
				pw.println("max_leases " + (ip2int(dhcpServerConfig.getRangeEnd()) - ip2int(dhcpServerConfig.getRangeStart())));
				pw.println("auto_time 0");
				pw.println("decline_time "	+ dhcpServerConfig.getDefaultLeaseTime());
				pw.println("conflict_time " + dhcpServerConfig.getDefaultLeaseTime());
				pw.println("offer_time " + dhcpServerConfig.getDefaultLeaseTime());
				pw.println("min_lease " + dhcpServerConfig.getDefaultLeaseTime());
				pw.println("opt subnet " + dhcpServerConfig.getSubnetMask().getHostAddress());
				pw.println("opt router " + dhcpServerConfig.getRouterAddress().getHostAddress());
				pw.println("opt lease " + dhcpServerConfig.getDefaultLeaseTime());
			}
			pw.flush();
			fos.getFD().sync();
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "error while building up new configuration files for dhcp servers", e);
		}
		finally{
			if(fos != null){
				try{
					fos.close();
				}catch(IOException ex){
					s_logger.warn("Error while closing FileOutputStream");
				}
			}
			if(pw != null){
				pw.close();
			}
		}
	}
	
	private void writeKuraExtendedConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig, Properties kuraExtendedProps) throws KuraException {
		boolean enabled = false;
		boolean passDns = false;
		
		List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = null;
		if(netInterfaceConfig instanceof EthernetInterfaceConfigImpl) {
    		netInterfaceAddressConfigs = ((EthernetInterfaceConfigImpl)netInterfaceConfig).getNetInterfaceAddresses();
    	} else if(netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
    		netInterfaceAddressConfigs = ((WifiInterfaceConfigImpl)netInterfaceConfig).getNetInterfaceAddresses();
    	} else {
    		s_logger.error("not adding config for " + netInterfaceConfig.getName());
    	}
		
		if(netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
    		for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
    			List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
    			if(netConfigs != null && netConfigs.size() > 0) {
    				for(int i=0; i<netConfigs.size(); i++) {
    					NetConfig netConfig = netConfigs.get(i);
    					if(netConfig instanceof DhcpServerConfig4) {
    						enabled = ((DhcpServerConfig4) netConfig).isEnabled();
    						passDns = ((DhcpServerConfig4) netConfig).isPassDns();
    					}
    				}
    			}
    		}
    	}

    	//set it all
    	if(kuraExtendedProps == null) {
    		s_logger.debug("kuraExtendedProps was null");
    		kuraExtendedProps = new Properties();
    	}
    	StringBuilder sb = new StringBuilder().append("net.interface.").append(netInterfaceConfig.getName()).append(".config.dhcpServer4.enabled");
    	kuraExtendedProps.put(sb.toString(), Boolean.toString(enabled));
    	sb = new StringBuilder().append("net.interface.").append(netInterfaceConfig.getName()).append(".config.dhcpServer4.passDns");
    	kuraExtendedProps.put(sb.toString(), Boolean.toString(passDns));
    	
    	//write it
    	if(kuraExtendedProps != null && !kuraExtendedProps.isEmpty()) {
			try {
			    KuranetConfig.storeProperties(kuraExtendedProps);
			} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
	}
	
	private int ip2int(IPAddress ip) {
		int result = 0;
		for (byte b: ip.getAddress())
		{
		    result = result << 8 | (b & 0xFF);
		}
		return result;
	}

}
