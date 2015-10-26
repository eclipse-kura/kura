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
package org.eclipse.kura.net.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.linux.net.dhcp.DhcpClientManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dns.LinuxNamed;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.LocalRule;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.eclipse.kura.linux.net.iptables.PortForwardRule;
import org.eclipse.kura.linux.net.util.IScanTool;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.util.ScanTool;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.linux.net.wifi.WpaSupplicant;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantManager;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantStatus;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfig6;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.visitor.linux.WpaSupplicantConfigWriter;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiHotspotInfo;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkAdminServiceImpl implements NetworkAdminService, EventHandler {

	private static final Logger s_logger = LoggerFactory.getLogger(NetworkAdminServiceImpl.class);
	
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	
	private static final String SSID_REGEXP = "[0-9A-Za-z/.@#:\\ \\_\\-]+";
	
    private ComponentContext                   m_ctx;
	private ConfigurationService               m_configurationService;
	private NetworkConfigurationService		   m_networkConfigurationService;
	private SystemService 					   m_systemService;
	
	private boolean m_pendingChange = false;
	
    private final static String[] EVENT_TOPICS = new String[] {
        NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC,
    };
    
    private class NetworkRollbackItem {
		String m_src; String m_dst;
		NetworkRollbackItem(String src, String dst) {
			m_src = src; m_dst = dst;
		}
	}

	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------
    public void setConfigurationService(ConfigurationService configurationService) {
        m_configurationService = configurationService;
    }
    
    public void unsetConfigurationService(ConfigurationService configurationService) {
        m_configurationService = null;
    }
    
    public void setNetworkConfigurationService(NetworkConfigurationService networkConfigurationService) {
        m_networkConfigurationService = networkConfigurationService;
    }
    
    public void unsetNetworkConfigurationService(NetworkConfigurationService networkConfigurationService) {
        m_networkConfigurationService = null;
    }
    
	public void setSystemService(SystemService systemService) {
		m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		m_systemService = null;
	}
    
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext) {
		
		s_logger.debug("Activating NetworkAdmin Service...");
        // save the bundle context
        m_ctx = componentContext;

		//since we are just starting up, start named if needed
		LinuxNamed linuxNamed;
		try {
			linuxNamed = LinuxNamed.getInstance();
			if(linuxNamed.isConfigured()) {
				linuxNamed.disable();
				linuxNamed.enable();
			}
		} catch (KuraException e) {
			e.printStackTrace();
		}
		
        Dictionary<String, String[]> d = new Hashtable<String, String[]>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        m_ctx.getBundleContext().registerService(EventHandler.class.getName(), this, d);
        s_logger.debug("Done Activating NetworkAdmin Service...");
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
	}

	@Override
	// FIME: This api should be deprecated in favor of the following signature:
	// List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getNetworkInterfaceConfigs()
	public List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getNetworkInterfaceConfigs() throws KuraException {
	    
		try {
			s_logger.debug("Getting all networkInterfaceConfigs");
			return m_networkConfigurationService.getNetworkConfiguration().getNetInterfaceConfigs();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	@Override
	public List<NetConfig> getNetworkInterfaceConfigs(String interfaceName)
			throws KuraException {
	    
	    ArrayList<NetConfig> netConfigs = new ArrayList<NetConfig>();
	    NetworkConfiguration networkConfig = m_networkConfigurationService.getNetworkConfiguration();
	    if ((interfaceName != null) && (networkConfig != null)) {
	    	try {
	    		s_logger.debug("Getting networkInterfaceConfigs for " + interfaceName);
				if(networkConfig != null && networkConfig.getNetInterfaceConfigs() != null && networkConfig.getNetInterfaceConfigs().size() > 0) {
		    	    for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : networkConfig.getNetInterfaceConfigs()) {
		    	        if(interfaceName.equals(netInterfaceConfig.getName())) {
		    	            List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
		    	            if(netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
		        	            for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
		        	                netConfigs.addAll(netInterfaceAddressConfig.getConfigs());
		        	            }
		    	            }
		    	            
		    	            break;
		    	        }
		    	    }
				}
	    	} catch (Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
	    }
	    
		return netConfigs;
	}

	@Override
	public void updateEthernetInterfaceConfig(String interfaceName,
			boolean autoConnect, int mtu, List<NetConfig> netConfigs)
			throws KuraException {
		
		NetConfigIP4 netConfig4 = null;
		NetConfigIP6 netConfig6 = null;
		DhcpServerConfigIP4 dhcpServerConfigIP4 = null;
		FirewallAutoNatConfig natConfig = null;
		boolean hadNetConfig4 = false, hadNetConfig6 = false, hadDhcpServerConfigIP4 = false, hadNatConfig = false;
		
		if(netConfigs != null && netConfigs.size() > 0) {
			for(NetConfig netConfig : netConfigs) {
				if(!netConfig.isValid()) {
					throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "NetConfig Configuration is invalid: " + netConfig.toString());
				}
				if(netConfig instanceof NetConfigIP4) {
					netConfig4 = (NetConfigIP4)netConfig;
				} else if(netConfig instanceof NetConfigIP6) {
					netConfig6 = (NetConfigIP6)netConfig;
				} else if(netConfig instanceof DhcpServerConfigIP4) {
					dhcpServerConfigIP4 = (DhcpServerConfigIP4) netConfig;
				} else if(netConfig instanceof FirewallAutoNatConfig) {
					natConfig = (FirewallAutoNatConfig) netConfig;
				}
			}
		}
		
		//validation
		if ((netConfig4 == null) && (netConfig6 == null)){
			throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, "Either IPv4 or IPv6 configuration must be defined");
		}
		
		List<String> modifiedInterfaceNames = new ArrayList<String>();
		boolean configurationChanged = false;
	
		ComponentConfiguration originalNetworkComponentConfiguration = ((SelfConfiguringComponent)m_networkConfigurationService).getConfiguration();
		if (originalNetworkComponentConfiguration == null) {
			s_logger.debug("Returning for some unknown reason - no existing config???");
			return;
		}
		try {
			NetworkConfiguration newNetworkConfiguration = new NetworkConfiguration(originalNetworkComponentConfiguration.getConfigurationProperties());
			List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = newNetworkConfiguration.getNetInterfaceConfigs();
			for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
				if (netInterfaceConfig.getName().equals(interfaceName)) {
					//handle MTU
					if(mtu != netInterfaceConfig.getMTU()) {
						AbstractNetInterface<?> absNetInterfaceConfig = (AbstractNetInterface<?>)netInterfaceConfig;
						s_logger.debug("updating MTU for " + interfaceName);
						absNetInterfaceConfig.setMTU(mtu);
						configurationChanged = true;
						if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
					}
					
					//handle autoconnect
					if(autoConnect != netInterfaceConfig.isAutoConnect()) {
						AbstractNetInterface<?> absNetInterfaceConfig = (AbstractNetInterface<?>)netInterfaceConfig;
						s_logger.debug("updating autoConnect for " + interfaceName + " to be " + autoConnect);
						absNetInterfaceConfig.setAutoConnect(autoConnect);
						configurationChanged = true;
						if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
					}
					
					//replace existing configs
					List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
					if(netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
						for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
							List<NetConfig> existingNetConfigs = netInterfaceAddressConfig.getConfigs();
							List<NetConfig> newNetConfigs = new ArrayList<NetConfig>();
							for(NetConfig netConfig : existingNetConfigs) {
								s_logger.debug("looking at existing NetConfig for " + interfaceName + " with value: " + netConfig.toString());			
								if(netConfig instanceof NetConfigIP4) {
									if(netConfig4 == null) {
										s_logger.debug("removing NetConfig4 for " + interfaceName);
									} else {
										hadNetConfig4 = true;
                                        newNetConfigs.add(netConfig4);
										if(!netConfig.equals(netConfig4)) {									
											s_logger.debug("updating NetConfig4 for " + interfaceName);
											s_logger.debug("Is new State DHCP? " + ((NetConfigIP4)netConfig4).isDhcp());
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating NetConfig4 for " + interfaceName + " because it is unchanged");
										}
									}
								} else if(netConfig instanceof NetConfig6) {
									if(netConfig6 == null) {
										s_logger.debug("removing NetConfig6 for " + interfaceName);
									} else {
										hadNetConfig6 = true;
                                        newNetConfigs.add(netConfig6);
										if(!netConfig.equals(netConfig6)) {
											s_logger.debug("updating NetConfig6 for " + interfaceName);
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating NetConfig6 for " + interfaceName + " because it is unchanged");
										}
									}
								} else if(netConfig instanceof DhcpServerConfigIP4) {
									if(dhcpServerConfigIP4 == null) {
										s_logger.debug("removing DhcpServerConfigIP4 for " + interfaceName);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									} else {
										hadDhcpServerConfigIP4 = true;
                                        newNetConfigs.add(dhcpServerConfigIP4);
										if(!netConfig.equals(dhcpServerConfigIP4)) {
											s_logger.debug("updating DhcpServerConfigIP4 for " + interfaceName);
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating DhcpServerConfigIP4 for " + interfaceName + " because it is unchanged");
										}
									}
								} else if(netConfig instanceof FirewallAutoNatConfig) {
									if(natConfig == null) {
										s_logger.debug("removing FirewallAutoNatConfig for " + interfaceName);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									} else {
										hadNatConfig = true;
                                        newNetConfigs.add(natConfig);
										if(!netConfig.equals(natConfig)) {
											s_logger.debug("updating FirewallAutoNatConfig for " + interfaceName);
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating FirewallAutoNatConfig for " + interfaceName + " because it is unchanged");
										}
									}
								} else {
									s_logger.debug("Found unsupported configuration: " + netConfig.toString());
								}
							}
							
							// add configs that did not match any in the current configuration
							if(netConfigs != null && netConfigs.size() > 0) {
								for(NetConfig netConfig : netConfigs) {
									if(netConfig instanceof NetConfigIP4 && !hadNetConfig4) {
										s_logger.debug("adding new NetConfig4 to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
									if(netConfig instanceof NetConfigIP6 && !hadNetConfig6) {
										s_logger.debug("adding new NetConfig6 to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
									if(netConfig instanceof DhcpServerConfigIP4 && !hadDhcpServerConfigIP4) {
										s_logger.debug("adding new DhcpServerConfigIP4 to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
									if(netConfig instanceof FirewallAutoNatConfig && !hadNatConfig) {
										s_logger.debug("adding new FirewallAutoNatConfig to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
								}
							}
							
							for(NetConfig netConfig : newNetConfigs) {
								s_logger.debug("New NetConfig: " + netConfig.getClass().toString() + " :: " + netConfig.toString());
							}
								
							// replace with new list
							((NetInterfaceAddressConfigImpl)netInterfaceAddressConfig).setNetConfigs(newNetConfigs);
						}
				    }
				}
			}
			
			if (configurationChanged) {
			    submitConfiguration(modifiedInterfaceNames, newNetworkConfiguration);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updateWifiInterfaceConfig(String interfaceName,
			boolean autoConnect, WifiAccessPoint accessPoint,
			List<NetConfig> netConfigs) throws KuraException {
		
		NetConfigIP4 netConfig4 = null;
		NetConfigIP6 netConfig6 = null;
		WifiConfig wifiConfig = null;
		DhcpServerConfigIP4 dhcpServerConfigIP4 = null;
		FirewallAutoNatConfig natConfig = null;
		boolean hadNetConfig4 = false, hadNetConfig6 = false, hadWifiConfig = false, hadDhcpServerConfigIP4 = false, hadNatConfig = false;
		
		if(netConfigs != null && netConfigs.size() > 0) {
			for(NetConfig netConfig : netConfigs) {
				if(!netConfig.isValid()) {
					throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "NetConfig Configuration is invalid: " + netConfig.toString());
				}

				if(netConfig instanceof NetConfigIP4) {
					s_logger.debug("got new NetConfigIP4");
					netConfig4 = (NetConfigIP4)netConfig;
				} else if(netConfig instanceof NetConfigIP6) {
					s_logger.debug("got new NetConfigIP6");
					netConfig6 = (NetConfigIP6)netConfig;
				} else if(netConfig instanceof WifiConfig) {
					s_logger.debug("got new WifiConfig");
					wifiConfig = (WifiConfig)netConfig;
				} else if(netConfig instanceof DhcpServerConfigIP4) {
					s_logger.debug("got new DhcpServerConfigIP4");
					dhcpServerConfigIP4 = (DhcpServerConfigIP4) netConfig;
				} else if(netConfig instanceof FirewallAutoNatConfig) {
					s_logger.debug("got new NatConfig");
					natConfig = (FirewallAutoNatConfig) netConfig;
				}
			}
		}
		
		//validation
		if(netConfig4 == null && netConfig6 == null) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, "Either IPv4 or IPv6 configuration must be defined");
		}
		if (wifiConfig == null) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, "WiFi configuration must be defined");
		}
		
		List<String> modifiedInterfaceNames = new ArrayList<String>();
		boolean configurationChanged = false;
		
		ComponentConfiguration originalNetworkComponentConfiguration = ((SelfConfiguringComponent)m_networkConfigurationService).getConfiguration();
		if (originalNetworkComponentConfiguration == null) {
			return;
		}
		try {
			NetworkConfiguration newNetworkConfiguration = new NetworkConfiguration(originalNetworkComponentConfiguration.getConfigurationProperties());
			List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = newNetworkConfiguration.getNetInterfaceConfigs();
			for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
				if (netInterfaceConfig.getName().equals(interfaceName)) {
					
					//replace existing configs
					List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
					if(netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
						for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
							List<NetConfig> existingNetConfigs = netInterfaceAddressConfig.getConfigs();
							List<NetConfig> newNetConfigs = new ArrayList<NetConfig>();
							WifiMode newWifiMode = (wifiConfig != null) ? wifiConfig.getMode() : null;
							for(NetConfig netConfig : existingNetConfigs) {
								s_logger.debug("looking at existing NetConfig for " + interfaceName + " with value: " + netConfig.toString());			
								if(netConfig instanceof NetConfigIP4) {
									if(netConfig4 == null) {
										s_logger.debug("removing NetConfig4 for " + interfaceName);
									} else {
										hadNetConfig4 = true;
										newNetConfigs.add(netConfig4);
										if(!netConfig.equals(netConfig4)) {									
											s_logger.debug("updating NetConfig4 for " + interfaceName);
											s_logger.debug("Is new State DHCP? " + ((NetConfigIP4)netConfig4).isDhcp());
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating NetConfig4 for " + interfaceName + " because it is unchanged");
										}
									}
								} else if(netConfig instanceof NetConfig6) {
									if(netConfig6 == null) {
										s_logger.debug("removing NetConfig6 for " + interfaceName);
									} else {
										hadNetConfig6 = true;
                                        newNetConfigs.add(netConfig6);
										if(!netConfig.equals(netConfig6)) {
											s_logger.debug("updating NetConfig6 for " + interfaceName);
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating NetConfig6 for " + interfaceName + " because it is unchanged");
										}
									}
								} else if(netConfig instanceof WifiConfig) {
									if(wifiConfig == null) {
										s_logger.debug("removing wifiConfig for " + interfaceName);
									} else {
                                        // There should be one new WifiConfig, which indicates the selected mode
										// but there may be multiple current wifi configs, one for each mode (infra, master, adhoc)
										// Check the one corresponding to the newly selected mode, and automatically the others
										if(newWifiMode.equals(((WifiConfig) netConfig).getMode())) {
											hadWifiConfig = true;
                                            newNetConfigs.add(wifiConfig);
										    s_logger.debug("checking WifiConfig for " + wifiConfig.getMode() + " mode");
		    								if(!netConfig.equals(wifiConfig)) {	
		    									s_logger.debug("updating WifiConfig for " + interfaceName);
		    									configurationChanged = true;
												if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
		    								} else {
		    									s_logger.debug("not updating WifiConfig for " + interfaceName + " because it is unchanged");
		    								}
		    							} else {
		    								// Keep the old WifiConfig for the non-selected wifi modes
		    								s_logger.debug("adding other WifiConfig: " + netConfig);
		    								newNetConfigs.add(netConfig);
		    							}
									}
								} else if(netConfig instanceof DhcpServerConfigIP4) {
									if(dhcpServerConfigIP4 == null) {
										s_logger.debug("removing DhcpServerConfigIP4 for " + interfaceName);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									} else {
										hadDhcpServerConfigIP4 = true;
                                        newNetConfigs.add(dhcpServerConfigIP4);
										if(!netConfig.equals(dhcpServerConfigIP4)) {
											s_logger.debug("updating DhcpServerConfigIP4 for " + interfaceName);
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating DhcpServerConfigIP4 for " + interfaceName + " because it is unchanged");
										}
									}
								} else if(netConfig instanceof FirewallAutoNatConfig) {
									if(natConfig == null) {
										s_logger.debug("removing FirewallAutoNatConfig for " + interfaceName);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									} else {
										hadNatConfig = true;
                                        newNetConfigs.add(natConfig);
										if(!netConfig.equals(natConfig)) {
											s_logger.debug("updating FirewallAutoNatConfig for " + interfaceName);
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating FirewallNatConfig for " + interfaceName + " because it is unchanged");
										}
									}
								} else {
									s_logger.debug("Found unsupported configuration: " + netConfig.toString());
								}
							}
	
							// add configs that did not match any in the current configuration
							if(netConfigs != null && netConfigs.size() > 0) {
								for(NetConfig netConfig : netConfigs) {
									if(netConfig instanceof NetConfigIP4 && !hadNetConfig4) {
										s_logger.debug("adding new NetConfig4 to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
									if(netConfig instanceof NetConfigIP6 && !hadNetConfig6) {
										s_logger.debug("adding new NetConfig6 to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
									if(netConfig instanceof WifiConfig && !hadWifiConfig) {
										s_logger.debug("adding new WifiConfig to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
									if(netConfig instanceof DhcpServerConfigIP4 && !hadDhcpServerConfigIP4) {
										s_logger.debug("adding new DhcpServerConfigIP4 to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
									if(netConfig instanceof FirewallAutoNatConfig && !hadNatConfig) {
										s_logger.debug("adding new FirewallAutoNatConfig to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
								}
							}
							
							// Update the wifi mode
							if(newWifiMode != null) {
	    						s_logger.debug("setting address config wifiMode to: " + newWifiMode);
	    						((WifiInterfaceAddressConfigImpl)netInterfaceAddressConfig).setMode(newWifiMode);
							}
	
	                        // replace with new list                        
	                        for(NetConfig netConfig : newNetConfigs) {
	                            s_logger.debug("Current NetConfig: " + netConfig.getClass().toString() + " :: " + netConfig.toString());
	                        }
							((WifiInterfaceAddressConfigImpl)netInterfaceAddressConfig).setNetConfigs(newNetConfigs);
						}
				    }
				}
			}
			
			if (configurationChanged) {
			    submitConfiguration(modifiedInterfaceNames, newNetworkConfiguration);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updateModemInterfaceConfig(String interfaceName,
			String serialNum, String modemId, int pppNumber,
			boolean autoConnect, int mtu, List<NetConfig> netConfigs)
			throws KuraException {
		
	    NetConfigIP4 netConfig4 = null;
		NetConfigIP6 netConfig6 = null;
		ModemConfig modemConfig = null;
		boolean hadNetConfig4 = false,  hadNetConfig6 = false, hadModemConfig = false;
		
		if(netConfigs != null && netConfigs.size() > 0) {
			for(NetConfig netConfig : netConfigs) {
				if(!netConfig.isValid()) {
					throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "NetConfig Configuration is invalid: " + netConfig.toString());
				}
				if(netConfig instanceof NetConfigIP4) {
					netConfig4 = (NetConfigIP4)netConfig;
				} else if(netConfig instanceof NetConfigIP6) {
					netConfig6 = (NetConfigIP6)netConfig;
				} else if(netConfig instanceof ModemConfig) {
					modemConfig = (ModemConfig)netConfig;
				}
			}
		}
		
		//validation
		if ((netConfig4 == null) && (netConfig6 == null)){
			throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, "Either IPv4 or IPv6 configuration must be defined");
		}
		if (modemConfig == null) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, "Modem configuration must be defined");
		}
		
		List<String> modifiedInterfaceNames = new ArrayList<String>();
		boolean configurationChanged = false;
		
		ComponentConfiguration originalNetworkComponentConfiguration = ((SelfConfiguringComponent)m_networkConfigurationService).getConfiguration();
		if (originalNetworkComponentConfiguration == null) {
			return;
		}
		try {
			NetworkConfiguration newNetworkConfiguration = new NetworkConfiguration(originalNetworkComponentConfiguration.getConfigurationProperties());
			List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = newNetworkConfiguration.getNetInterfaceConfigs();
			for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
				if (netInterfaceConfig.getName().equals(interfaceName)) {
					//handle MTU
					if(mtu != netInterfaceConfig.getMTU()) {
						AbstractNetInterface<?> absNetInterfaceConfig = (AbstractNetInterface<?>)netInterfaceConfig;
						s_logger.debug("updating MTU for " + interfaceName);
						absNetInterfaceConfig.setMTU(mtu);
						configurationChanged = true;
						if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
					}
					
					if(netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
					    ModemInterfaceConfigImpl modemInterfaceConfig = (ModemInterfaceConfigImpl)netInterfaceConfig;
					    if(modemId == null)
					        modemId = "";
					    				    
					    // handle modem id
					    if(!modemId.equals(modemInterfaceConfig.getModemIdentifier())) {
					        s_logger.debug("updating Modem identifier: " + modemId );
					        modemInterfaceConfig.setModemIdentifier(modemId);
		                    configurationChanged = true;
							if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
		                }
					    
	                    // handle ppp num
	                    if(pppNumber != modemInterfaceConfig.getPppNum()) {
	                        s_logger.debug("updating PPP number: " + pppNumber);
	                        modemInterfaceConfig.setPppNum(pppNumber);
	                        configurationChanged = true;
							if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
	                    }			    
					}
					
					//replace existing configs
					List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
					if(netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
						for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
							List<NetConfig> existingNetConfigs = netInterfaceAddressConfig.getConfigs();
							List<NetConfig> newNetConfigs = new ArrayList<NetConfig>();
							for(NetConfig netConfig : existingNetConfigs) {
								s_logger.debug("looking at existing NetConfig for " + interfaceName + " with value: " + netConfig.toString());			
								if(netConfig instanceof NetConfigIP4) {
									if(netConfig4 == null) {
										s_logger.debug("removing NetConfig4 for " + interfaceName);
									} else {
										hadNetConfig4 = true;
                                        newNetConfigs.add(netConfig4);
										if(!netConfig.equals(netConfig4)) {									
											s_logger.debug("updating NetConfig4 for " + interfaceName);
											s_logger.debug("Is new State DHCP? " + ((NetConfigIP4)netConfig4).isDhcp());
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating NetConfig4 for " + interfaceName + " because it is unchanged");
										}
									}
								} else if(netConfig instanceof NetConfig6) {
									if(netConfig6 == null) {
										s_logger.debug("removing NetConfig6 for " + interfaceName);
									} else {
										hadNetConfig6 = true;
                                        newNetConfigs.add(netConfig6);
										if(!netConfig.equals(netConfig6)) {
											s_logger.debug("updating NetConfig6 for " + interfaceName);
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating NetConfig6 for " + interfaceName + " because it is unchanged");
										}
									}
								} else if(netConfig instanceof ModemConfig) {
									if(modemConfig == null) {
										s_logger.debug("removing ModemConfig for " + interfaceName);
									} else {
										hadModemConfig = true;
                                        newNetConfigs.add(modemConfig);
										if(!netConfig.equals(modemConfig)) {	
											s_logger.debug("updating ModemConfig for " + interfaceName);
											configurationChanged = true;
											if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
										} else {
											s_logger.debug("not updating ModemConfig for " + interfaceName + " because it is unchanged");
										}
									}
								} else {
									s_logger.debug("Found unsupported configuration: " + netConfig.toString());
								}
							}
	
							// add configs that did not match any in the current configuration
							if(netConfigs != null && netConfigs.size() > 0) {
								for(NetConfig netConfig : netConfigs) {
									if(netConfig instanceof NetConfigIP4 && !hadNetConfig4) {
										s_logger.debug("adding new NetConfig4 to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
									if(netConfig instanceof NetConfigIP6 && !hadNetConfig6) {
										s_logger.debug("adding new NetConfig6 to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
									if(netConfig instanceof ModemConfig && !hadModemConfig) {
										s_logger.debug("adding new ModemConfig to existing config for " + interfaceName);
										newNetConfigs.add(netConfig);
										configurationChanged = true;
										if(!modifiedInterfaceNames.contains(interfaceName)) {modifiedInterfaceNames.add(interfaceName);}
									}
								}
							}
							
							for(NetConfig netConfig : newNetConfigs) {
								s_logger.debug("Current NetConfig: " + netConfig.getClass().toString() + " :: " + netConfig.toString());
							}
							
							// replace with new list
							((ModemInterfaceAddressConfigImpl)netInterfaceAddressConfig).setNetConfigs(newNetConfigs);
						}
				    }
				}
				
				newNetworkConfiguration.addNetInterfaceConfig(netInterfaceConfig);
			}
			
			if (configurationChanged) {
			    submitConfiguration(modifiedInterfaceNames, newNetworkConfiguration);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void enableInterface(String interfaceName, boolean dhcp) throws KuraException {
		
		try {
			NetInterfaceType type = LinuxNetworkUtil.getType(interfaceName);

			if(!LinuxNetworkUtil.isUp(interfaceName) ||
					(type == NetInterfaceType.WIFI && !LinuxNetworkUtil.isLinkUp(interfaceName))) {

				s_logger.info("bringing interface {} up", interfaceName);
				
				if (type == NetInterfaceType.WIFI) {
					enableWifiInterface(interfaceName);
				}
				if (dhcp) {
					renewDhcpLease(interfaceName);
				} else {
					LinuxNetworkUtil.enableInterface(interfaceName);
				}
				
				//if it isn't up - at least make sure the Ethernet controller is powered on
				if(!LinuxNetworkUtil.isUp(interfaceName)) {
					LinuxNetworkUtil.powerOnEthernetController(interfaceName);
				}
			} else {
				s_logger.info("not bringing interface {} up because it is already up", interfaceName);
				if (dhcp) {
					renewDhcpLease(interfaceName);
				}
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	@Override
	public void disableInterface(String interfaceName) throws KuraException {
		
		if(!interfaceName.equals("lo")) {
			try {
				if (LinuxNetworkUtil.isUp(interfaceName)) {
					s_logger.info("bringing interface {} down", interfaceName);
					manageDhcpClient(interfaceName, false);
					manageDhcpServer(interfaceName, false);

					// FIXME: can we avoid getting the interface type again and ask for the caller to pass it in?
					NetInterfaceType type = LinuxNetworkUtil.getType(interfaceName);
					if (type == NetInterfaceType.WIFI) {
						disableWifiInterface(interfaceName);
					}

					LinuxNetworkUtil.disableInterface(interfaceName);

				} else {
					s_logger.info("not bringing interface {} down because it is already down", interfaceName);
					manageDhcpClient(interfaceName, false);
					manageDhcpServer(interfaceName, false);
				}
			} catch(Exception e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
	}
	
	public void manageDhcpClient(String interfaceName, boolean enable) throws KuraException {
		
		try {
			/*
			int pid = LinuxProcessUtil.getPid(formDhclientCommand(interfaceName, false));
			if (pid > -1) {
				s_logger.debug("manageDhcpClient() :: killing {}", formDhclientCommand(interfaceName, false));
				LinuxProcessUtil.kill(pid);
			} else {
				pid = LinuxProcessUtil.getPid(formDhclientCommand(interfaceName, true));
				if (pid > -1) {
					s_logger.debug("manageDhcpClient() :: killing {}", formDhclientCommand(interfaceName, true));
					LinuxProcessUtil.kill(pid);
				}
			}
			*/
			DhcpClientManager.disable(interfaceName);
			if (enable) {
				this.renewDhcpLease(interfaceName);
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public void manageDhcpServer(String interfaceName, boolean enable) throws KuraException {
		
		DhcpServerManager.disable(interfaceName);
		if (enable) {
			DhcpServerManager.enable(interfaceName);			
		}
	}
	
	public void renewDhcpLease(String interfaceName) throws KuraException {
		
		DhcpClientManager.releaseCurrentLease(interfaceName);
		DhcpClientManager.enable(interfaceName);
	}
	
	public void manageFirewall (String gatewayIface) throws KuraException {
		// get desired NAT rules interfaces
		LinkedHashSet<NATRule> desiredNatRules = null; 
		ComponentConfiguration networkComponentConfiguration = ((SelfConfiguringComponent)m_networkConfigurationService).getConfiguration();
		if ((gatewayIface != null) && (networkComponentConfiguration != null)) {
			try {
				NetworkConfiguration netConfiguration = new NetworkConfiguration(networkComponentConfiguration.getConfigurationProperties());
				List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = netConfiguration.getNetInterfaceConfigs();
				for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
					String ifaceName = netInterfaceConfig.getName();
					List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
					if(netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
						for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
							List<NetConfig> existingNetConfigs = netInterfaceAddressConfig.getConfigs();
							if(existingNetConfigs != null && existingNetConfigs.size() > 0) {
								for(NetConfig netConfig : existingNetConfigs) {
									if (netConfig instanceof FirewallAutoNatConfig) {
										if (desiredNatRules == null) {
											desiredNatRules = new LinkedHashSet<NATRule>();
										}
										desiredNatRules.add(new NATRule(ifaceName, gatewayIface, true));
									}
								}
							}
						}
					}
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		
		LinuxFirewall firewall = LinuxFirewall.getInstance();
		if (desiredNatRules != null) {
			firewall.replaceAllNatRules(desiredNatRules); 
		} else {
			firewall.deleteAllAutoNatRules();
		}
		
		firewall.enable();
	}

	@Override
	public List<NetConfig> getFirewallConfiguration() throws KuraException {
		
		s_logger.debug("getting the firewall configuration");
		LinuxFirewall firewall = LinuxFirewall.getInstance();
		List<NetConfig> netConfigs = new ArrayList<NetConfig>();

		//convert the objects
		//FIXME - should change the firewall implementation so we use the API config objects rather than local ones
		Iterator<LocalRule> localRules = firewall.getLocalRules().iterator();
		while(localRules.hasNext()) {
		    LocalRule localRule = localRules.next();
			s_logger.debug("Adding local rule " + localRule.getPort());
			netConfigs.add(new FirewallOpenPortConfigIP4(localRule.getPort(), 
					NetProtocol.valueOf(localRule.getProtocol()), 
					localRule.getPermittedNetwork(),
					localRule.getPermittedInterfaceName(),
					localRule.getUnpermittedInterfaceName(),
					localRule.getPermittedMAC(), 
					localRule.getSourcePortRange()));
		}
		Iterator<PortForwardRule> portForwardRules = firewall.getPortForwardRules().iterator();
		while(portForwardRules.hasNext()) {
		    PortForwardRule portForwardRule = portForwardRules.next();
			try {
				s_logger.debug("Adding port forwarding - inbound iface is {}", portForwardRule.getInboundIface());
				netConfigs.add(new FirewallPortForwardConfigIP4(portForwardRule.getInboundIface(),
						portForwardRule.getOutboundIface(),
						(IP4Address) IPAddress.parseHostAddress(portForwardRule.getAddress()),
						NetProtocol.valueOf(portForwardRule.getProtocol()),
						portForwardRule.getInPort(),
						portForwardRule.getOutPort(),
						portForwardRule.isMasquerade(),
						new NetworkPair<IP4Address>((IP4Address) IPAddress.parseHostAddress(portForwardRule.getPermittedNetwork()), (short)portForwardRule.getPermittedNetworkMask()),
								portForwardRule.getPermittedMAC(),
								portForwardRule.getSourcePortRange()
								));
			} catch (UnknownHostException e) {
				e.printStackTrace();
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
		Iterator<NATRule> autoNatRules = firewall.getAutoNatRules().iterator();
		while(autoNatRules.hasNext()) {
		    NATRule autoNatRule = autoNatRules.next();
			s_logger.debug("Adding auto NAT rules " + autoNatRule.getSourceInterface() );
			netConfigs.add(new FirewallAutoNatConfig(autoNatRule.getSourceInterface(),
					autoNatRule.getDestinationInterface(),
					autoNatRule.isMasquerade()));
		}
		
		Iterator<NATRule> natRules = firewall.getNatRules().iterator();
		while (natRules.hasNext()) {
		    NATRule natRule = natRules.next();
			s_logger.debug("Adding NAT rules " + natRule.getSourceInterface());
			netConfigs.add(new FirewallNatConfig(natRule.getSourceInterface(),
					natRule.getDestinationInterface(), natRule.getProtocol(),
					natRule.getSource(), natRule.getDestination(), natRule.isMasquerade()));
		}

		return netConfigs;
	}

	@Override
	public void setFirewallOpenPortConfiguration(
			List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallConfiguration)
			throws KuraException {
		
		s_logger.debug("Deleting local rules");
		LinuxFirewall firewall = LinuxFirewall.getInstance();
		firewall.deleteAllLocalRules();
		
		for(FirewallOpenPortConfigIP<? extends IPAddress> openPortEntry : firewallConfiguration) {
			s_logger.debug("Adding local rule for: " + openPortEntry.getPort());
			
			if(openPortEntry.getPermittedNetwork() == null || openPortEntry.getPermittedNetwork().getIpAddress() == null) {
				try {
					openPortEntry.setPermittedNetwork(new NetworkPair(IPAddress.parseHostAddress("0.0.0.0"), (short) 0));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
			
			firewall.addLocalRule(openPortEntry.getPort(), 
					openPortEntry.getProtocol().name(), 
					openPortEntry.getPermittedNetwork().getIpAddress().getHostAddress(), 
					Short.toString(openPortEntry.getPermittedNetwork().getPrefix()), 
					openPortEntry.getPermittedInterfaceName(),
					openPortEntry.getUnpermittedInterfaceName(),
					openPortEntry.getPermittedMac(), 
					openPortEntry.getSourcePortRange());
		}
	}

	@Override
	public void setFirewallPortForwardingConfiguration(
			List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallConfiguration)
			throws KuraException {
		s_logger.debug("Deleting port forward rules");
		LinuxFirewall firewall = LinuxFirewall.getInstance();
		firewall.deleteAllPortForwardRules();
		
		for(FirewallPortForwardConfigIP<? extends IPAddress> portForwardEntry : firewallConfiguration) {
			s_logger.debug("Adding port forward rule for: " + portForwardEntry.getInPort());
			
			if(portForwardEntry.getPermittedNetwork() == null || portForwardEntry.getPermittedNetwork().getIpAddress() == null) {
				try {
					portForwardEntry.setPermittedNetwork(new NetworkPair(IPAddress.parseHostAddress("0.0.0.0"), (short) 0));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
			
			firewall.addPortForwardRule(portForwardEntry.getInboundInterface(), 
					portForwardEntry.getOutboundInterface(),
					portForwardEntry.getAddress().getHostAddress(), 
					portForwardEntry.getProtocol().name(), 
					portForwardEntry.getInPort(), 
					portForwardEntry.getOutPort(),
					portForwardEntry.isMasquerade(),
					portForwardEntry.getPermittedNetwork().getIpAddress().getHostAddress(), 
					Short.toString(portForwardEntry.getPermittedNetwork().getPrefix()), 
					portForwardEntry.getPermittedMac(), 
					portForwardEntry.getSourcePortRange());
		}
	}
	
	@Override
	public void setFirewallNatConfiguration(List<FirewallNatConfig> natConfigs) throws KuraException {
		
		LinuxFirewall firewall = LinuxFirewall.getInstance();
		firewall.deleteAllNatRules();
		for (FirewallNatConfig natConfig : natConfigs) {
			firewall.addNatRule(natConfig.getSourceInterface(),
					natConfig.getDestinationInterface(),
					natConfig.getProtocol(), natConfig.getSource(),
					natConfig.getDestination(), natConfig.isMasquerade());
		}
	}
	
	public Map<String, WifiHotspotInfo> getWifiHotspots(String ifaceName) throws KuraException {
		
		Map<String, WifiHotspotInfo> mWifiHotspotInfo = new HashMap<String, WifiHotspotInfo>();
		WifiMode wifiMode = WifiMode.UNKNOWN;
		List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = getNetworkInterfaceConfigs();
	    for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
	        if(netInterfaceConfig.getName().equals(ifaceName)) {
	            List<? extends NetInterfaceAddressConfig> netInterfaceAddresses = netInterfaceConfig.getNetInterfaceAddresses();
	            if(netInterfaceAddresses != null) {
	                for(NetInterfaceAddressConfig netInterfaceAddress : netInterfaceAddresses) {
	                    if(netInterfaceAddress instanceof WifiInterfaceAddressConfig) {
	                        wifiMode = ((WifiInterfaceAddressConfig) netInterfaceAddress).getMode();
	                    }
	                }
	            }
	            break;
	        }
	    }
	    
	    try {
		    if (wifiMode == WifiMode.MASTER) {
		    	WpaSupplicantConfigWriter wpaSupplicantConfigWriter = WpaSupplicantConfigWriter.getInstance();
		    	wpaSupplicantConfigWriter.generateTempWpaSupplicantConf();
		    	
		    	s_logger.debug("getWifiHotspots() :: Starting temporary instance of wpa_supplicant");
		    	StringBuilder key = new StringBuilder("net.interface." +  ifaceName + ".config.wifi.infra.driver");
		    	String driver = KuranetConfig.getProperty(key.toString());
		    	WpaSupplicantManager.startTemp(ifaceName, WifiMode.INFRA, driver);
		    	wifiModeWait(ifaceName, WifiMode.INFRA, 10);
		    }
		    
		    s_logger.info("getWifiHotspots() :: scanning for available access points ...");
		    IScanTool scanTool = ScanTool.get(ifaceName);
		    if (scanTool != null) {
			    List<WifiAccessPoint> wifiAccessPoints = scanTool.scan();
			    for(WifiAccessPoint wap : wifiAccessPoints) {
			    	
			    	if ((wap.getSSID() == null) || (wap.getSSID().length() == 0)) {
			    		s_logger.debug("Skipping hidden SSID");
			    		continue;
			    	}
			    	
			    	if (!wap.getSSID().matches(SSID_REGEXP)){
			    		s_logger.debug("Skipping undesired SSID");
			    		continue;
			    	}
			    	
			    	s_logger.trace("getWifiHotspots() :: SSID={}", wap.getSSID());
			    	s_logger.trace("getWifiHotspots() :: Signal={}", wap.getStrength());
			    	s_logger.trace("getWifiHotspots() :: Frequency={}", wap.getFrequency());
			    	
			    	byte [] baMacAddress = wap.getHardwareAddress();
			    	StringBuffer sbMacAddress = new StringBuffer();
			    	for (int i = 0; i < baMacAddress.length; i++) {
			    		sbMacAddress.append(String.format("%02x", baMacAddress[i]&0x0ff).toUpperCase());
			    		if (i < baMacAddress.length-1) {
			    			sbMacAddress.append(':');
			    		}
			    	}
			    	
			    	WifiSecurity wifiSecurity = WifiSecurity.NONE;
			    	
			    	EnumSet<WifiSecurity> esWpaSecurity = wap.getWpaSecurity();
			    	if ((esWpaSecurity != null) && (esWpaSecurity.size() > 0)) {
			    		
			    		wifiSecurity = WifiSecurity.SECURITY_WPA;
			    		
			    		Iterator<WifiSecurity> itWpaSecurity = esWpaSecurity.iterator();	
				    	while (itWpaSecurity.hasNext()) {
				    		s_logger.trace("getWifiHotspots() :: WPA Security={}", itWpaSecurity.next());
				    	}
			    	}
			    	
			    	EnumSet<WifiSecurity> esRsnSecurity = wap.getRsnSecurity();
			    	if ((esRsnSecurity != null) && (esRsnSecurity.size() > 0)) {
			    		if (wifiSecurity == WifiSecurity.SECURITY_WPA) {
			    			wifiSecurity = WifiSecurity.SECURITY_WPA_WPA2;
			    		} else {
			    			wifiSecurity = WifiSecurity.SECURITY_WPA2;
			    		}
			    		Iterator<WifiSecurity> itRsnSecurity = esRsnSecurity.iterator();
			    		while (itRsnSecurity.hasNext()) {
				    		s_logger.trace("getWifiHotspots() :: RSN Security={}", itRsnSecurity.next());
				    	}
			    	}
			    	
			    	if (wifiSecurity == WifiSecurity.NONE) {
			    		List<String> capabilities = wap.getCapabilities();
			    		if ((capabilities != null) && (capabilities.size() > 0)) {
				    		for (String capab : capabilities) {
				    			if (capab.equals("Privacy")) {
				    				wifiSecurity = WifiSecurity.SECURITY_WEP;
				    				break;
				    			}
				    		}
			    		}
			    	}
			    	
			    	int frequency = (int)wap.getFrequency();
			    	int channel = frequencyMhz2Channel(frequency);
			    	
			    	EnumSet<WifiSecurity>pairCiphers = EnumSet.noneOf(WifiSecurity.class);
			    	EnumSet<WifiSecurity>groupCiphers = EnumSet.noneOf(WifiSecurity.class);
			    	if (wifiSecurity == WifiSecurity.SECURITY_WPA_WPA2) {
			    		Iterator<WifiSecurity> itWpaSecurity = esWpaSecurity.iterator();
			    		while (itWpaSecurity.hasNext()) {
			    			WifiSecurity securityEntry = itWpaSecurity.next();
			    			if ((securityEntry == WifiSecurity.PAIR_CCMP) || 
					    	    (securityEntry == WifiSecurity.PAIR_TKIP)) {
			    				pairCiphers.add(securityEntry);
			    			} else if ((securityEntry == WifiSecurity.GROUP_CCMP) || 
			    					   (securityEntry == WifiSecurity.GROUP_TKIP)) {
			    				groupCiphers.add(securityEntry);
			    			}
			    		}
			    		Iterator<WifiSecurity> itRsnSecurity = esRsnSecurity.iterator();
			    		while (itRsnSecurity.hasNext()) {
			    			WifiSecurity securityEntry = itRsnSecurity.next();
			    			if ((securityEntry == WifiSecurity.PAIR_CCMP) || 
				    			(securityEntry == WifiSecurity.PAIR_TKIP)) {
			    				if (!pairCiphers.contains(securityEntry))
			    					pairCiphers.add(securityEntry);
			    			} else if ((securityEntry == WifiSecurity.GROUP_CCMP) || 
			    					   (securityEntry == WifiSecurity.GROUP_TKIP)) {
			    				if (!groupCiphers.contains(securityEntry))
			    					groupCiphers.add(securityEntry);
			    			}
			    		}
			    	} else if (wifiSecurity == WifiSecurity.SECURITY_WPA) {
			    		Iterator<WifiSecurity> itWpaSecurity = esWpaSecurity.iterator();
			    		while (itWpaSecurity.hasNext()) {
			    			WifiSecurity securityEntry = itWpaSecurity.next();
			    			if ((securityEntry == WifiSecurity.PAIR_CCMP) || 
			    				(securityEntry == WifiSecurity.PAIR_TKIP)) {
			    				pairCiphers.add(securityEntry);
			    			} else if ((securityEntry == WifiSecurity.GROUP_CCMP) || 
			    					   (securityEntry == WifiSecurity.GROUP_TKIP)) {
			    				groupCiphers.add(securityEntry);
			    			}
			    		}
			    	} else if (wifiSecurity == WifiSecurity.SECURITY_WPA2) {
			    		Iterator<WifiSecurity> itRsnSecurity = esRsnSecurity.iterator();
			    		while (itRsnSecurity.hasNext()) {
			    			WifiSecurity securityEntry = itRsnSecurity.next();
			    			if ((securityEntry == WifiSecurity.PAIR_CCMP) || 
				    			(securityEntry == WifiSecurity.PAIR_TKIP)) {
			    				pairCiphers.add(securityEntry);
			    			} else if ((securityEntry == WifiSecurity.GROUP_CCMP) || 
			    					   (securityEntry == WifiSecurity.GROUP_TKIP)) {
			    				groupCiphers.add(securityEntry);
			    			}
			    		}
			    	}
			    	
					WifiHotspotInfo wifiHotspotInfo = new WifiHotspotInfo(
							wap.getSSID(), sbMacAddress.toString(),
							0 - wap.getStrength(), channel, frequency,
							wifiSecurity, pairCiphers, groupCiphers);
			    	mWifiHotspotInfo.put(wap.getSSID(), wifiHotspotInfo);
			    }
	    	}
		    
		    if (wifiMode == WifiMode.MASTER) {
		    	if (WpaSupplicantManager.isTempRunning()) {
					s_logger.debug("getWifiHotspots() :: stoping temporary instance of wpa_supplicant");
					WpaSupplicantManager.stop();
				}
		    }
	    } catch(Throwable t) {
	    	throw new KuraException(KuraErrorCode.INTERNAL_ERROR, t, "scan operation has failed");
	    }
	    
	    return mWifiHotspotInfo;
	}
	
	@Override
	public boolean verifyWifiCredentials(String ifaceName, WifiConfig wifiConfig, int tout) {
		
		boolean ret = false;
		boolean restartSupplicant = false;
		WpaSupplicantConfigWriter wpaSupplicantConfigWriter = WpaSupplicantConfigWriter.getInstance();
		try {
			wpaSupplicantConfigWriter.generateTempWpaSupplicantConf(wifiConfig, ifaceName);

			if (WpaSupplicantManager.isRunning()) {
				s_logger.debug("verifyWifiCredentials() :: stoping wpa_supplicant");
				WpaSupplicantManager.stop();
				restartSupplicant = true;
			}
			s_logger.debug("verifyWifiCredentials() :: Restarting temporary instance of wpa_supplicant");
			WpaSupplicantManager.startTemp(ifaceName, WifiMode.INFRA, wifiConfig.getDriver());
			wifiModeWait(ifaceName, WifiMode.INFRA, 10);
			ret = isWifiConnectionCompleted(ifaceName, tout);
			
			if (WpaSupplicantManager.isTempRunning()) {
				s_logger.debug("verifyWifiCredentials() :: stoping temporary instance of wpa_supplicant");
				WpaSupplicantManager.stop();
			}
		} catch (KuraException e) {
			e.printStackTrace();
		}
		
		if (restartSupplicant) {
			try {
				s_logger.debug("verifyWifiCredentials() :: Restarting wpa_supplicant");
				WpaSupplicant wpaSupplicant = WpaSupplicant.getWpaSupplicant(ifaceName);
				if (wpaSupplicant != null) {
					WpaSupplicantManager.start(ifaceName, wpaSupplicant.getMode(), wpaSupplicant.getDriver());
					if (isWifiConnectionCompleted(ifaceName, tout)) {
						this.renewDhcpLease(ifaceName);
					}
				}
			} catch (KuraException e) {
				e.printStackTrace();
			}
		}
		
		return ret;
	}
	
	@Override
	public boolean rollbackDefaultConfiguration() throws KuraException {
		s_logger.debug("rollbackDefaultConfiguration() :: Recovering default configuration ...");
				
		ArrayList<NetworkRollbackItem> rollbackItems = new ArrayList<NetworkRollbackItem>();
				
		if (m_systemService == null) {
			return false;
		}
		
		String dstDataDirectory = m_systemService.getKuraDataDirectory();
		if (dstDataDirectory == null) {
			return false;
		}
		
		int ind = dstDataDirectory.lastIndexOf('/');
		String srcDataDirectory = null;
		if (ind >= 0) {
			srcDataDirectory = "".concat(dstDataDirectory.substring(0, ind+1).concat(".data"));
		}
		
		if (srcDataDirectory == null) {
			return false;
		}
		
		rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/kuranet.conf", dstDataDirectory + "/kuranet.conf"));
		//rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/firewall", "/etc/init.d/firewall"));
		if (OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/hostapd.conf", "/etc/hostapd/hostapd.conf"));
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/dhcpd-eth0.conf", "/etc/udhcpd-usb0.conf"));
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/dhcpd-wlan0.conf", "/etc/udhcpd-wlan0.conf"));
		} else {
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/hostapd.conf", "/etc/hostapd.conf"));
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/dhcpd-eth0.conf", "/etc/dhcpd-eth0.conf"));
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/dhcpd-wlan0.conf", "/etc/dhcpd-wlan0.conf"));
		}
			
		if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) ||
				OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName()) || 
				OS_VERSION.equals(KuraConstants.BeagleBone.getImageName()) ||
				OS_VERSION.equals(KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion() + "_" + KuraConstants.Intel_Edison.getTargetName())) {
			// restore Debian interface configuration
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/interfaces", "/etc/network/interfaces"));
		} else {
			// restore RedHat interface configuration
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/ifcfg-eth0", "/etc/sysconfig/network-scripts/ifcfg-eth0"));
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/ifcfg-eth1", "/etc/sysconfig/network-scripts/ifcfg-eth1"));
			rollbackItems.add(new NetworkRollbackItem(srcDataDirectory + "/ifcfg-wlan0", "/etc/sysconfig/network-scripts/ifcfg-wlan0"));
		}
		
		for (NetworkRollbackItem rollbackItem : rollbackItems) {
			rollbackItem(rollbackItem);
		}
		
		s_logger.debug("rollbackDefaultConfiguration() :: setting network configuration ...");
		m_networkConfigurationService.setNetworkConfiguration(m_networkConfigurationService.getNetworkConfiguration());
		
		return true;
	}
	
	@Override
	public boolean rollbackDefaultFirewallConfiguration() throws KuraException {
		s_logger.debug("rollbackDefaultFirewallConfiguration() :: initializing firewall ...");
		if (m_systemService == null) {
			return false;
		}
		
		String dstDataDirectory = m_systemService.getKuraDataDirectory();
		if (dstDataDirectory == null) {
			return false;
		}
		
		int ind = dstDataDirectory.lastIndexOf('/');
		String srcDataDirectory = null;
		if (ind >= 0) {
			srcDataDirectory = "".concat(dstDataDirectory.substring(0, ind+1).concat(".data"));
		}
		
		if (srcDataDirectory == null) {
			return false;
		}
		
		NetworkRollbackItem firewallRollbackItem = new NetworkRollbackItem(srcDataDirectory + "/firewall", "/etc/init.d/firewall");
		rollbackItem(firewallRollbackItem);
		LinuxFirewall.getInstance().initialize();
		LinuxFirewall.getInstance().enable();
		return true;
	}
	
	private void rollbackItem (NetworkRollbackItem rollbackItem) {
		File srcFile = new File (rollbackItem.m_src);
		File dstFile = new File (rollbackItem.m_dst);
		if (srcFile.exists()) {
			try {
				s_logger.debug("rollbackItem() :: copying {} to {} ...", srcFile, dstFile);
				copyFile(srcFile, dstFile);
			} catch (IOException e) {
				s_logger.error("rollbackItem() :: Failed to recover {} file - {}", dstFile, e);
			}
		}
	}
	
	private void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
    @Override
    public void handleEvent(Event event) {
        s_logger.debug("handleEvent - topic: " + event.getTopic());
        String topic = event.getTopic();
        if(topic.equals(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC)) {
            m_pendingChange = false;
        }
    }
    
    private boolean isWifiConnectionCompleted (String ifaceName, int tout) throws KuraException {
    	
    	boolean ret = false;
    	long start = System.currentTimeMillis();
		do {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			WpaSupplicantStatus wpaSupplicantStatus = new WpaSupplicantStatus(ifaceName);
			String wpaState = wpaSupplicantStatus.getWpaState();
			if ((wpaState != null) && (wpaState.equals("COMPLETED"))) {
				ret = true;
				break;
			}
		} while (System.currentTimeMillis()-start < tout*1000);
		
		return ret;
    }
    
    private void wifiModeWait(String ifaceName, WifiMode mode, int tout) {
    	long startTimer = System.currentTimeMillis();
    	do {
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
    		try {
				if (LinuxNetworkUtil.getWifiMode(ifaceName) == mode) {
					break;
				}
			} catch (KuraException e) {
				s_logger.error("wifiModeWait() :: Failed to obtain WiFi mode - {}", e);
			}
    	} while((System.currentTimeMillis()-startTimer) < 1000L*tout);
    }
	
	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------	
	private void enableWifiInterface (String ifaceName) throws KuraException {
		
	    // ignore mon.* interface
	    if(ifaceName.startsWith("mon.")) {
	        return;
	    }
	    // ignore redpine vlan interface 
        if (ifaceName.startsWith("rpine")) {
        	return;
        }
	    
        NetInterfaceStatus status = NetInterfaceStatus.netIPv4StatusUnknown;
	    WifiMode wifiMode = WifiMode.UNKNOWN;
	    WifiConfig wifiConfig = null;
	    
	    List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = getNetworkInterfaceConfigs();
	    for(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
	        if(netInterfaceConfig.getName().equals(ifaceName)) {
	            List<? extends NetInterfaceAddressConfig> netInterfaceAddresses = netInterfaceConfig.getNetInterfaceAddresses();
	            if(netInterfaceAddresses != null) {
	                for(NetInterfaceAddressConfig netInterfaceAddress : netInterfaceAddresses) {
	                    if(netInterfaceAddress instanceof WifiInterfaceAddressConfig) {
	                        wifiMode = ((WifiInterfaceAddressConfig) netInterfaceAddress).getMode();
	                        
	                        for(NetConfig netConfig : netInterfaceAddress.getConfigs()) {
	                            if(netConfig instanceof NetConfigIP4) {
	                                status = ((NetConfigIP4) netConfig).getStatus();
	                                s_logger.debug("Interface status is set to " + status);
	                            } else if (netConfig instanceof WifiConfig) {
	                                if(((WifiConfig)netConfig).getMode() == wifiMode) {
	                                    wifiConfig = (WifiConfig) netConfig;
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	            break;
	        }
	    }
	    
        s_logger.debug("Configuring " + ifaceName + " for " + wifiMode + " mode");
        
        s_logger.debug("Stopping hostapd and wpa_supplicant");
        HostapdManager.stop();
        WpaSupplicantManager.stop();
        
        if (status == NetInterfaceStatus.netIPv4StatusEnabledLAN
                && wifiMode.equals(WifiMode.MASTER)) {
            
            s_logger.debug("Starting hostapd");
            HostapdManager.start();
            
        } else if((status == NetInterfaceStatus.netIPv4StatusEnabledLAN || status == NetInterfaceStatus.netIPv4StatusEnabledWAN)
                && (wifiMode.equals(WifiMode.INFRA) || wifiMode.equals(WifiMode.ADHOC))) {

            if(wifiConfig != null) {
                s_logger.debug("Starting wpa_supplicant");
                WpaSupplicantManager.start(ifaceName, wifiMode, wifiConfig.getDriver());
            } else {
                s_logger.warn("No WifiConfig configured for mode " + wifiMode);
            }
        } else {
            s_logger.debug("Invalid wifi configuration - NetInterfaceStatus:" + status + ", WifiMode:" + wifiMode);
        }
	}
	
	private void disableWifiInterface (String ifaceName) throws KuraException {
	    s_logger.debug("Stopping hostapd and wpa_supplicant");
		HostapdManager.stop();
		WpaSupplicantManager.stop();
	}
	
	
	// Submit new configuration, waiting for network configuration change event before returning
	private void submitConfiguration(List<String> modifiedInterfaceNames, NetworkConfiguration networkConfiguration) throws KuraException {
		short timeout = 30;		// in seconds
	    
	    m_pendingChange = true;
	    if(modifiedInterfaceNames != null && !modifiedInterfaceNames.isEmpty()) {
	    	networkConfiguration.setModifiedInterfaceNames(modifiedInterfaceNames);
	    	s_logger.debug("Set modified interface names: " + modifiedInterfaceNames.toString());
	    }
	    m_networkConfigurationService.setNetworkConfiguration(networkConfiguration);
	    m_configurationService.snapshot();
	    
        while(m_pendingChange && timeout > 0) {
            timeout -= 0.5;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        
        if(m_pendingChange) {
            s_logger.warn("Did not receive a network configuration change event");
            m_pendingChange = false;
        }
	}
	
	private int frequencyMhz2Channel(int frequency) {
		
		int channel = (frequency - 2407)/5;
		return channel;
	}
}
