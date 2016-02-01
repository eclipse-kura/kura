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
package org.eclipse.kura.net.admin.processor.linux;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationReader;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.processor.linux.util.KuranetConfig;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirewallAutoNatConfigReader implements NetworkConfigurationReader {
	
	private static final Logger s_logger = LoggerFactory.getLogger(FirewallAutoNatConfigReader.class);
	
	private static FirewallAutoNatConfigReader s_instance;
	
	public static FirewallAutoNatConfigReader getInstance () {
		
		if (s_instance == null) {
			s_instance = new FirewallAutoNatConfigReader();
		}
		return s_instance;
	}
	
	@Override
	public void read(NetworkConfiguration config) throws KuraException {
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config.getNetInterfaceConfigs();
		for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            getConfig(netInterfaceConfig, KuranetConfig.getProperties());
        }
	}
	
	private void getConfig(
			NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig,
			Properties kuraProps) throws KuraException {
		
		String interfaceName = netInterfaceConfig.getName();
		
		NetInterfaceType type = netInterfaceConfig.getType();
		if (type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI) {
			s_logger.debug("Getting NAT config for " + interfaceName);
			if(kuraProps != null) {
				s_logger.debug("Getting NAT config from kuraProps");
				boolean natEnabled = false;
				boolean useMasquerade = false;
				String prop = null;
				String srcIface = null;
				String dstIface = null;
				StringBuilder sb = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.nat.enabled");
				if((prop = kuraProps.getProperty(sb.toString())) != null) {
					natEnabled = Boolean.parseBoolean(prop);
				} 
				
				sb = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.nat.masquerade");
				if((prop = kuraProps.getProperty(sb.toString())) != null) {
					useMasquerade = Boolean.parseBoolean(prop);
				} 
				
				sb = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.nat.src.interface");
				if((prop = kuraProps.getProperty(sb.toString())) != null) {
					srcIface = prop;
				} 
				
				sb = new StringBuilder().append("net.interface.").append(interfaceName).append(".config.nat.dst.interface");
				if((prop = kuraProps.getProperty(sb.toString())) != null) {
					dstIface = prop;
				} 
				
				if (natEnabled) {
					FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig(srcIface, dstIface, useMasquerade);
					
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
                            
                        netConfigs.add(natConfig);
                    }
				}
			} else {
				//get it from the firewall file if possible
				LinuxFirewall firewall = LinuxFirewall.getInstance();
				Set<NATRule> natRules = firewall.getAutoNatRules();
				if(natRules != null && !natRules.isEmpty()) {
					Iterator<NATRule> it = natRules.iterator();
					while(it.hasNext()) {
						NATRule rule = it.next();
						if(rule.getSourceInterface().equals(interfaceName)) {
							s_logger.debug("found NAT rule: " + rule);
							
							//this is the one we care about
							FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig(rule.getSourceInterface(), rule.getDestinationInterface(), rule.isMasquerade());
							
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
		                            
		                        netConfigs.add(natConfig);
		                    }
						}
					}
				}
			}
		}
	}
}
