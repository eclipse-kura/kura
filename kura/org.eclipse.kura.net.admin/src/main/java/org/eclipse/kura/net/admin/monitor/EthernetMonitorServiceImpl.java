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
package org.eclipse.kura.net.admin.monitor;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.route.RouteService;
import org.eclipse.kura.linux.net.route.RouteServiceImpl;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.EthernetMonitorService;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.route.RouteConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthernetMonitorServiceImpl implements EthernetMonitorService, EventHandler {
	
	private static final Logger s_logger = LoggerFactory.getLogger(EthernetMonitorServiceImpl.class);
	
	private final static String[] EVENT_TOPICS = new String[] {
        NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC,
    };
	
	private final static long THREAD_INTERVAL = 30000;
	private final static long THREAD_TERMINATION_TOUT = 1; // in seconds
	
	private static Map<String, ScheduledFuture<?>> tasks;
	private static Map<String, Boolean> stopThreads;
	
	private NetworkService m_networkService;
	private EventAdmin m_eventAdmin;
	private NetworkAdminService m_netAdminService;
	private NetworkConfigurationService m_netConfigService;
	private RouteService m_routeService;
	
	private Map<String, InterfaceState> m_interfaceState = new HashMap<String, InterfaceState>();
	private Map<String, EthernetInterfaceConfigImpl> m_networkConfiguration = new HashMap<String, EthernetInterfaceConfigImpl>();
	private Map<String, EthernetInterfaceConfigImpl> m_newNetworkConfiguration = new HashMap<String, EthernetInterfaceConfigImpl>();
	private ScheduledThreadPoolExecutor m_executor;
	
    // ----------------------------------------------------------------
    //
    //   Dependencies
    //
    // ----------------------------------------------------------------
	
    public void setNetworkService(NetworkService networkService) {
    	m_networkService = networkService;
    }
    
    public void unsetNetworkService(NetworkService networkService) {
    	m_networkService = null;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        m_eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        m_eventAdmin = null;
    }
    
    public void setNetworkAdminService(NetworkAdminService netAdminService) {
    	m_netAdminService = netAdminService;
    }
    
    public void unsetNetworkAdminService(NetworkAdminService netAdminService) {
    	m_netAdminService = null;
    }
    
    public void setNetworkConfigurationService(NetworkConfigurationService netConfigService) {
    	m_netConfigService = netConfigService;
    }
    
    public void unsetNetworkConfigurationService(NetworkConfigurationService netConfigService) {
    	m_netConfigService = null;
    }

    // ----------------------------------------------------------------
    //
    //   Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {

    	s_logger.debug("Activating EthernetMonitor Service...");
    	
    	Dictionary<String, String[]> d = new Hashtable<String, String[]>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, d);
        
        m_routeService = RouteServiceImpl.getInstance();
        
        m_executor = new ScheduledThreadPoolExecutor(1);
        m_executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        m_executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		
        // Get initial configurations
        try {
        	NetworkConfiguration netConfiguration = m_netConfigService.getNetworkConfiguration();
        	if (netConfiguration != null) {
				for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig: netConfiguration.getNetInterfaceConfigs()) {
					if (netInterfaceConfig instanceof EthernetInterfaceConfigImpl) {
						s_logger.debug("Adding initial ethernet config for " + netInterfaceConfig.getName());
						EthernetInterfaceConfigImpl newEthernetConfig = (EthernetInterfaceConfigImpl) netInterfaceConfig;
						m_newNetworkConfiguration.put(netInterfaceConfig.getName(), newEthernetConfig);
						m_networkConfiguration.put(netInterfaceConfig.getName(), newEthernetConfig);
					}
				}
			}
        } catch (KuraException e) {
        	s_logger.error("Could not update list of interfaces", e);
        }
        
        // Initialize monitors
        initializeMonitors();
        
    	s_logger.debug("Done Activating EthernetMonitor Service...");
    }

    protected void deactivate(ComponentContext componentContext) {
    	for (String key : tasks.keySet()) {
    		stopMonitor(key);
    	}
    	
    	if (m_executor != null) {
    		s_logger.debug("Terminating EthernetMonitor Thread ...");
    		m_executor.shutdownNow();
    		try {
				m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
    		s_logger.info("EthernetMonitor Thread terminated? - {}", m_executor.isTerminated());
    		m_executor = null;
    	}
    }
    
	private void monitor(String interfaceName) {
		while (!stopThreads.get(interfaceName)) {
			try {
				List <? extends NetInterfaceAddressConfig> new_niacs = null;
				List <? extends NetInterfaceAddressConfig> cur_niacs = null;
				InterfaceState currentInterfaceState = null;
	        	boolean interfaceEnabled = false;
	        	boolean isDhcpClient = false;
	        	boolean dhcpServerEnabled = false;
	        	IPAddress dhcpServerSubnet = null;
	        	short dhcpServerPrefix = -1;
	        	boolean postStatusChangeEvent = false;
	        	
	        	EthernetInterfaceConfigImpl currentInterfaceConfig = m_networkConfiguration.get(interfaceName);
	        	EthernetInterfaceConfigImpl newInterfaceConfig = m_newNetworkConfiguration.get(interfaceName);
	        	
	        	// Make sure the Ethernet Controllers are powered
	        	if(!LinuxNetworkUtil.isEthernetControllerPowered(interfaceName)) {
					LinuxNetworkUtil.powerOnEthernetController(interfaceName);
				}
	        	
	        	// If a new configuration exists, compare it to the existing configuration
	        	if (newInterfaceConfig != null) {
	        		// Get all configurations for the interface
	        		new_niacs = newInterfaceConfig.getNetInterfaceAddresses();
	        		if (currentInterfaceConfig != null) {
	        			cur_niacs = currentInterfaceConfig.getNetInterfaceAddresses();
	        		}
	        		
	        		if (isConfigChanged(new_niacs, cur_niacs)) {
	        			s_logger.debug("Found a new network configuration for " + interfaceName);
		        		
		        		// Disable the interface to be reconfigured below
						disableInterface(interfaceName);
	
		        		// Set the current config to the new config
		      			m_networkConfiguration.put(interfaceName, newInterfaceConfig);
						currentInterfaceConfig = newInterfaceConfig;
	
						postStatusChangeEvent = true;
	        		}
	        		
	        		m_newNetworkConfiguration.remove(interfaceName);
	        	}
	        		
	        	// Monitor for status changes and ensure dhcp server is running when enabled
	
	    		interfaceEnabled = isEthernetEnabled(currentInterfaceConfig);
	    		InterfaceState prevInterfaceState = m_interfaceState.get(interfaceName);
				currentInterfaceState = new InterfaceState(interfaceName);
				if(!currentInterfaceState.equals(prevInterfaceState)) {
					postStatusChangeEvent = true;
				}
				
				// Find if DHCP server or DHCP client mode is enabled
	            if (currentInterfaceConfig != null) {
	            	NetInterfaceStatus netInterfaceStatus = getStatus(currentInterfaceConfig);
	            	
	            	cur_niacs = currentInterfaceConfig.getNetInterfaceAddresses();
	            	
	            	if ((cur_niacs != null) && cur_niacs.size() > 0) {
	        			for (NetInterfaceAddressConfig niac : cur_niacs) {
	        				List<NetConfig> netConfigs = niac.getConfigs();
	        				if ((netConfigs != null) && netConfigs.size() > 0) {
	        					for (NetConfig netConfig : netConfigs) {
	        						if (netConfig instanceof DhcpServerConfig4) {
	        			            	// only enable if Enabled for LAN
	        			            	if(netInterfaceStatus.equals(NetInterfaceStatus.netIPv4StatusEnabledLAN)) {
	        			            		dhcpServerEnabled = ((DhcpServerConfig4) netConfig).isEnabled();
	        			            		dhcpServerSubnet = ((DhcpServerConfig4) netConfig).getSubnet();
	        			            		dhcpServerPrefix = ((DhcpServerConfig4) netConfig).getPrefix();
	        			            	} else {
	        			            		s_logger.trace("Not enabling DHCP server for " + interfaceName + " since it is set to " + netInterfaceStatus);
	        			            	}
	    	                        } else if (netConfig instanceof NetConfigIP4) {
	        							isDhcpClient = ((NetConfigIP4) netConfig).isDhcp();
	        						}
	        					}
	        				}
	        			}
	        		} else {
	        			s_logger.debug("No current net interface addresses for " + interfaceName);
	        		}
	            } else {
	            	s_logger.debug("Current interface config is null for " + interfaceName);
	            }
	            
	            // Enable/disable based on configuration and current status
	            if(interfaceEnabled) {
		            if(currentInterfaceState.isUp()) {
		            	if(!currentInterfaceState.isLinkUp()) {
		            		s_logger.debug("link is down - disabling " + interfaceName);
		            		disableInterface(interfaceName);
		            	}
		            } else {
		            	// State is currently down
		            	if(currentInterfaceState.isLinkUp()) {
		            		s_logger.debug("link is up - enabling " + interfaceName);
		            		m_netAdminService.enableInterface(interfaceName, isDhcpClient);
		            	}
		            }
	            } else {
	            	if(currentInterfaceState.isUp()) {
	                    s_logger.debug(interfaceName + " is currently up - disable interface");
	                    disableInterface(interfaceName);            		
	            	}
	            }
	            
	            // Get the status after all ifdowns and ifups
	            currentInterfaceState = new InterfaceState(interfaceName);
	
	            // Manage the DHCP server and validate routes
	            if (currentInterfaceState != null && currentInterfaceState.isUp() && currentInterfaceState.isLinkUp()) {
	            	NetInterfaceStatus netInterfaceStatus = getStatus(currentInterfaceConfig);
	            	if(netInterfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
	            		// This should be the default gateway - make sure it is
	            		boolean found = false;
	            		
	            		RouteConfig[] routes = m_routeService.getRoutes();
	            		if(routes != null && routes.length > 0) {
	            			for(RouteConfig route : routes) {
	            				if(route.getInterfaceName().equals(interfaceName) && 
	            						route.getDestination().equals(IPAddress.parseHostAddress("0.0.0.0")) &&
	            						!route.getGateway().equals(IPAddress.parseHostAddress("0.0.0.0"))) {
	            					found = true;
	            					break;
	            				}
	            			}
	            		}
	            		
	            		if(!found) {
	            			//disable the interface and reenable - something didn't happen at initialization as it was supposed to
	            			s_logger.error("WAN interface " + interfaceName + " did not have a route setting it as the default gateway, restarting it");
	            			m_netAdminService.disableInterface(interfaceName);
	            			m_netAdminService.enableInterface(interfaceName, isDhcpClient);
	            		}
	            	} else if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledLAN) {
	            		if (isDhcpClient) {
	            			RouteService rs = RouteServiceImpl.getInstance();
	            			RouteConfig rconf = rs.getDefaultRoute(interfaceName);
	            			if (rconf != null) {
	            				s_logger.debug("{} is configured for LAN/DHCP - removing GATEWAY route ...", rconf.getInterfaceName());
	            				rs.removeStaticRoute(rconf.getDestination(), rconf.getGateway(), rconf.getNetmask(), rconf.getInterfaceName());
	            			}
	            		}
	            	}
	            	
	            	if(dhcpServerEnabled && !DhcpServerManager.isRunning(interfaceName)) {
	            		s_logger.debug("Starting DHCP server for " + interfaceName);
	            		m_netAdminService.manageDhcpServer(interfaceName, true, new NetworkPair(dhcpServerSubnet, dhcpServerPrefix));
	            	}
				} else if(DhcpServerManager.isRunning(interfaceName)) {
					s_logger.debug("Stopping DHCP server for " + interfaceName);
					m_netAdminService.manageDhcpServer(interfaceName, false, null);
				}
	            
	            // post event if there were any changes
	            if(postStatusChangeEvent) {            	
	            	s_logger.debug("Posting NetworkStatusChangeEvent for " + interfaceName + ": " + currentInterfaceState);
					m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, currentInterfaceState, null));
		            m_interfaceState.put(interfaceName, currentInterfaceState);
	            }
	
	            // If the interface is disabled in Denali, stop the monitor
	            if(!interfaceEnabled) {
	            	s_logger.debug(interfaceName + " is disabled - stopping monitor");
	                stopMonitor(interfaceName);
	            }
	            Thread.sleep(30000);
			} catch (KuraException kuraException) {
				s_logger.debug(kuraException.getMessage());
				
			} catch (Exception e) {
				s_logger.debug(e.getMessage());
			}
		}
	}
	
	// On a network config change event, verify the change was for ethernet and add a new ethernet config
	@Override
	public void handleEvent(Event event) {
		String topic = event.getTopic();
		s_logger.debug("handleEvent - topic: " + topic);
        
		if (topic.equals(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC)) {
			NetworkConfigurationChangeEvent netConfigChangedEvent = (NetworkConfigurationChangeEvent)event;
            String [] propNames = netConfigChangedEvent.getPropertyNames();
            if ((propNames != null) && (propNames.length > 0)) {
                Map<String, Object> props = new HashMap<String, Object>();
                for (String propName : propNames) {
                    Object prop = netConfigChangedEvent.getProperty(propName);
                    if (prop != null) {
                        props.put(propName, prop);
                    }
                    else {
                    }
                }
                try {
                    NetworkConfiguration newNetworkConfig = new NetworkConfiguration(props);
                    if (newNetworkConfig != null) {
						for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig: newNetworkConfig.getNetInterfaceConfigs()) {
							if (netInterfaceConfig instanceof EthernetInterfaceConfigImpl) {
								s_logger.debug("Adding new ethernet config for " + netInterfaceConfig.getName());
								EthernetInterfaceConfigImpl newEthernetConfig = (EthernetInterfaceConfigImpl) netInterfaceConfig;
								m_newNetworkConfiguration.put(netInterfaceConfig.getName(), newEthernetConfig);
								if (isEthernetEnabled(newEthernetConfig)) {
									startMonitor(netInterfaceConfig.getName());
								}
							}
						}
					}
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
		}
		
	}
	
	// Compare configurations
	private boolean isConfigChanged(List <? extends NetInterfaceAddressConfig> newConfig, List <? extends NetInterfaceAddressConfig> currentConfig) {
		if(newConfig == null && currentConfig == null) {
			return false;
		}
		
		if ((newConfig == null || currentConfig == null) || (newConfig.size() != currentConfig.size())) {
			return true;
		}
		
		for (int i = 0; i < newConfig.size(); i++) {
			List<NetConfig> newNetConfigs = newConfig.get(i).getConfigs();
			List<NetConfig> currentNetConfigs = currentConfig.get(i).getConfigs();
				
			if(newNetConfigs == null && currentNetConfigs == null) {
				continue;
			}
			
			if((newNetConfigs == null || currentNetConfigs == null) || (newNetConfigs.size() != currentNetConfigs.size())) {
				s_logger.debug("Config changed current - " + currentNetConfigs);
				s_logger.debug("Config changed new     - " + newNetConfigs);
				return true;
			}
			
			for(int j = 0; j < newNetConfigs.size(); j++) {
				boolean foundMatch = false;
				NetConfig newNetConfig = newNetConfigs.get(j);
				for(int k = 0; k < currentNetConfigs.size(); k++) {
					NetConfig currentNetConfig = currentNetConfigs.get(k);
					
					if(newNetConfig.getClass() == currentNetConfig.getClass()) {
						foundMatch = true;
						if(!newNetConfig.equals(currentNetConfig) && newNetConfig.getClass() != FirewallNatConfig.class) {
							s_logger.debug("\tConfig changed - Current config: " + currentNetConfig.toString());
							s_logger.debug("\tConfig changed - New config: " + newNetConfig.toString());
							return true;
						}
						break;
					}
				}
				
				if(!foundMatch) {
					return true;
				}
			}
			
			/*
			if (!newConfig.get(i).equals(currentConfig.get(i))) {
				s_logger.debug("\tConfig changed - Old config: " + currentConfig.get(i).toString());
				s_logger.debug("\tConfig changed - New config: " + newConfig.get(i).toString());
				return true;
			}*/
		}
		return false;
	}
	
	// Very the interface is enabled in Denali
	private boolean isEthernetEnabled(EthernetInterfaceConfigImpl ethernetInterfaceConfig) {
		NetInterfaceStatus status = getStatus(ethernetInterfaceConfig);
		
		return status.equals(NetInterfaceStatus.netIPv4StatusEnabledLAN) || status.equals(NetInterfaceStatus.netIPv4StatusEnabledWAN);
	}
	
	private NetInterfaceStatus getStatus(EthernetInterfaceConfigImpl ethernetInterfaceConfig) {
		NetInterfaceStatus status = NetInterfaceStatus.netIPv4StatusUnknown;
		
		if (ethernetInterfaceConfig != null) {
			for (NetInterfaceAddressConfig addresses : ethernetInterfaceConfig.getNetInterfaceAddresses()) {
				for (NetConfig netConfig : addresses.getConfigs()) {
					if (netConfig instanceof NetConfigIP4) {
						status = ((NetConfigIP4)netConfig).getStatus();
					}
				}
			}
		}
		
		return status;
	}
	
		// Initialize a monitor thread for each ethernet interface
	private void initializeMonitors() {
		 List<String> currentInterfaceNames;
		try {
			currentInterfaceNames = m_networkService.getAllNetworkInterfaceNames();
			if(currentInterfaceNames != null && currentInterfaceNames.size() > 0) {
				for (String interfaceName : currentInterfaceNames) {
			        // skip non-ethernet interfaces
			        if(LinuxNetworkUtil.getType(interfaceName) != NetInterfaceType.ETHERNET) {
			            continue;
			        }
			        startMonitor(interfaceName);
				}
			         
			}
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Start a interface specific monitor thread
	private void startMonitor(final String interfaceName) {
		if (tasks == null) {
			tasks = new HashMap<String, ScheduledFuture<?>>();
		}
		if (stopThreads == null) {
			stopThreads = new HashMap<String, Boolean>();
		}
		
		stopThreads.put(interfaceName, false);
		
		// Ensure monitor doesn't already exist for this interface
		if (tasks.get(interfaceName) == null) {
			s_logger.debug("Starting monitor for " + interfaceName);
			ScheduledFuture<?> task = m_executor.schedule(new Runnable() {
	    		@Override
	    		public void run() {
		    			Thread.currentThread().setName("EthernetMonitor_" + interfaceName);
		    			while (!stopThreads.get(interfaceName)) {
		    				monitor(interfaceName);
		    				try {
		    					Thread.sleep(THREAD_INTERVAL);
		    				} catch (InterruptedException interruptedException) {
		    					Thread.currentThread().interrupt();
		    					s_logger.debug(interruptedException.getMessage());
		    				}
		    			}
	    	}}, 0, TimeUnit.SECONDS);
			
			tasks.put(interfaceName, task);
		}
	}
	
	// Stop a interface specific monitor thread
	private void stopMonitor(String interfaceName) {
		m_interfaceState.remove(interfaceName);
		
		ScheduledFuture<?> task = tasks.get(interfaceName);
		if ((task != null) && (!task.isDone())) {
			stopThreads.put(interfaceName, true);
			s_logger.debug("Stopping monitor for {} ...", interfaceName);
			task.cancel(true);
			s_logger.info("Monitor for {} cancelled? = {}", interfaceName, task.isDone());
			tasks.put(interfaceName, null);
		}
	}
	
	private void disableInterface(String interfaceName) throws Exception {
		LinuxNetworkUtil.disableInterface(interfaceName);
		LinuxNetworkUtil.powerOnEthernetController(interfaceName);
		m_netAdminService.manageDhcpServer(interfaceName, false, null);
	}
}
