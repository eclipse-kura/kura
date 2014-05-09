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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.dns.LinuxNamed;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;
import org.eclipse.kura.net.dns.DnsMonitorService;
import org.eclipse.kura.net.dns.DnsServerConfigIP4;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsMonitorServiceImpl implements DnsMonitorService, EventHandler {
    
    private static final Logger s_logger = LoggerFactory.getLogger(DnsMonitorServiceImpl.class);

    private final static String[] EVENT_TOPICS = new String[] {
        NetworkStatusChangeEvent.NETWORK_EVENT_STATUS_CHANGE_TOPIC,
        NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC,
    };
    
    private final static long THREAD_INTERVAL = 60000;
    private final static long THREAD_TERMINATION_TOUT = 1; // in seconds
    private static ScheduledFuture<?> s_monitorTask;
    private ScheduledThreadPoolExecutor m_executor;
    
    private boolean m_enabled;
    private static boolean stopThread;
    private NetworkConfigurationService m_netConfigService;
    private NetworkConfiguration m_networkConfiguration;
    private Set<NetworkPair<IP4Address>> m_allowedNetworks;
    private Set<IP4Address> m_forwarders;

    public void setNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        m_netConfigService = netConfigService;
    }
    
    public void unsetNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        m_netConfigService = null;
    }
    
    protected void activate(ComponentContext componentContext) {        
        s_logger.debug("Activating DnsProxyMonitor Service...");

        Dictionary<String, String[]> d = new Hashtable<String, String[]>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, d);
        
        try {
			m_networkConfiguration = m_netConfigService.getNetworkConfiguration();
		} catch (KuraException e) {
			s_logger.error("Could not get initial network configuration", e);
		}
        
        //FIXME - brute force handler for DNS updates
        // m_executorUtil = ExecutorUtil.getInstance(); <IAB>
        m_executor = new ScheduledThreadPoolExecutor(1);
        m_executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        m_executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        stopThread = false;
        s_monitorTask = m_executor.schedule(new Runnable() {
    		@Override
    		public void run() {
    			while (!stopThread) {
	    			Thread.currentThread().setName("DnsMonitorServiceImpl");
	    			Set<IPAddress> dnsServers = LinuxDns.getInstance().getDnServers();
	    			
	    			// Check that resolv.conf matches what is configured
	    			Set<IPAddress> configuredServers = getConfiguredDnsServers();
	    			if(!configuredServers.equals(dnsServers)) {
	    				setDnsServers(configuredServers);
	    				dnsServers = configuredServers;
	    			}
	    			
	    			Set<IP4Address> forwarders = new HashSet<IP4Address>();    			
	                if(dnsServers != null && !dnsServers.isEmpty()) {
	                	for(IPAddress dnsServer : dnsServers) {
	                		s_logger.debug("Found DNS Server: " + dnsServer.getHostAddress());
	                		forwarders.add((IP4Address) dnsServer);
	                	}
	                }
	                
	                if(forwarders != null && !forwarders.isEmpty()) {
	                	if(!forwarders.equals(m_forwarders)) {
	                		//there was a change - deal with it
	                		s_logger.info("Detected DNS resolv.conf change - restarting DNS proxy");
	                		m_forwarders = forwarders;
	                		
	                		try {
	                            LinuxNamed linuxNamed = LinuxNamed.getInstance();
	                            DnsServerConfigIP4 currentDnsServerConfig = linuxNamed.getDnsServerConfig();
	                            DnsServerConfigIP4 newDnsServerConfig = new DnsServerConfigIP4(m_forwarders, m_allowedNetworks); 
	                            
	                            if(currentDnsServerConfig.equals(newDnsServerConfig)) {
	                            	s_logger.debug("DNS server config has changed - updating from " + currentDnsServerConfig + " to " + newDnsServerConfig);
		                            s_logger.debug("Disabling DNS proxy");
		                            linuxNamed.disable();
		                            
		                            s_logger.debug("Writing config");
		                            linuxNamed.setConfig(newDnsServerConfig);
		
		                            if(m_enabled) {
		                                sleep(500);
		                                s_logger.debug("Starting DNS proxy");
		                                linuxNamed.enable();
		                            } else {
		                            	s_logger.debug("DNS proxy not enabled");
		                            }
	                            }
	                        } catch (KuraException e) {
	                            e.printStackTrace();
	                        }        
	                	}
	                }
	                try {
						Thread.sleep(THREAD_INTERVAL);
					} catch (InterruptedException e) {
						s_logger.debug(e.getMessage());
					}
    			}
    		}
    	}, 0, TimeUnit.MINUTES);
    }
    
    protected void deactivate(ComponentContext componentContext) {
    	
    	stopThread = true;
        if ((s_monitorTask != null) && (!s_monitorTask.isDone())) {
        	s_logger.debug("Cancelling DnsMonitorServiceImpl task ...");
        	s_monitorTask.cancel(true);
    		s_logger.info("DnsMonitorServiceImpl task cancelled? = {}", s_monitorTask.isDone());
    		s_monitorTask = null;
        }
            
        if (m_executor != null) {
        	s_logger.debug("Terminating DnsMonitorServiceImpl Thread ...");
        	m_executor.shutdownNow();
    		try {
				m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
    		s_logger.info("DnsMonitorServiceImpl Thread terminated? - {}", m_executor.isTerminated());
        	m_executor = null;
        }
    }
    
    @Override
    public void handleEvent(Event event) {
        s_logger.debug("handleEvent - topic: " + event.getTopic());
        String topic = event.getTopic();
        
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
                }
                try {
                    m_networkConfiguration = new NetworkConfiguration(props);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        	updateDnsResolverConfig();
        	updateDnsProxyConfig();
        } else if (topic.equals(NetworkStatusChangeEvent.NETWORK_EVENT_STATUS_CHANGE_TOPIC)) {
        	updateDnsResolverConfig();
        	updateDnsProxyConfig();
        }
    }
    
    private void updateDnsResolverConfig() {
    	s_logger.debug("Updating resolver config");
    	setDnsServers(getConfiguredDnsServers());
    }

	private void updateDnsProxyConfig() {
		m_enabled = false;

		m_allowedNetworks = new HashSet<NetworkPair<IP4Address>>();
		m_forwarders = new HashSet<IP4Address>();
		        
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = m_networkConfiguration.getNetInterfaceConfigs();
		 
		for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
		    if (netInterfaceConfig.getType() == NetInterfaceType.ETHERNET || 
		            netInterfaceConfig.getType() == NetInterfaceType.WIFI || 
		            netInterfaceConfig.getType() == NetInterfaceType.MODEM) {
		        try{
		            getAllowedNetworks(netInterfaceConfig);
		        } catch (KuraException e) {
		            s_logger.error("Error updating dns proxy", e);
		        }
		    }
		}
		
		Set<IPAddress> dnsServers = LinuxDns.getInstance().getDnServers();
		if(dnsServers != null && !dnsServers.isEmpty()) {
			for(IPAddress dnsServer : dnsServers) {
				s_logger.debug("Found DNS Server: " + dnsServer.getHostAddress());
				m_forwarders.add((IP4Address) dnsServer);
			}
		}
		
		try {
		    LinuxNamed linuxNamed = LinuxNamed.getInstance();

		    s_logger.debug("Disabling DNS proxy");
		    linuxNamed.disable();
		    
		    s_logger.debug("Writing config");
		    DnsServerConfigIP4 dnsServerConfigIP4 = new DnsServerConfigIP4(m_forwarders, m_allowedNetworks);            
		    linuxNamed.setConfig(dnsServerConfigIP4);

		    if(m_enabled) {
		        sleep(500);
		        s_logger.debug("Starting DNS proxy");
		        linuxNamed.enable();
		    } else {
		    	s_logger.debug("DNS proxy not enabled");
		    }
		} catch (KuraException e) {
		    e.printStackTrace();
		}
	}
    
    private void getAllowedNetworks(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
        
        String interfaceName = netInterfaceConfig.getName();
        s_logger.debug("Getting DNS proxy config for " + interfaceName);

        List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = null;
        netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();

        if(netInterfaceAddressConfigs != null && netInterfaceAddressConfigs.size() > 0) {
            for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
                List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
                if(netConfigs != null && netConfigs.size() > 0) {
                    for(int i=0; i<netConfigs.size(); i++) {
                        NetConfig netConfig = netConfigs.get(i);
                        if(netConfig instanceof DhcpServerConfig) {
                            if(((DhcpServerConfig) netConfig).isPassDns()) {
                                s_logger.debug("Found an allowed network: " + ((DhcpServerConfig) netConfig).getRouterAddress() + "/" + ((DhcpServerConfig) netConfig).getPrefix());
                                m_enabled = true;
                                
                                //this is an 'allowed network'
                                m_allowedNetworks.add(new NetworkPair<IP4Address>((IP4Address)((DhcpServerConfig) netConfig).getRouterAddress(), ((DhcpServerConfig) netConfig).getPrefix()));
                            }
                        }
                    }
                }
            }
        }
    }
    
    private boolean isEnabledForWan(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {
    	for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceConfig.getNetInterfaceAddresses()) {
    		for(NetConfig netConfig : netInterfaceAddressConfig.getConfigs()) {
    			if(netConfig instanceof NetConfigIP4) {
    				return NetInterfaceStatus.netIPv4StatusEnabledWAN.equals(((NetConfigIP4) netConfig).getStatus());
    			}
    		}
    	}
    	
    	return false;
    }
    
	private void setDnsServers(Set<IPAddress> newServers) {
    	LinuxDns linuxDns = LinuxDns.getInstance();
    	Set<IPAddress> currentServers = linuxDns.getDnServers();
    	
    	if(newServers == null || newServers.isEmpty()) {
    		s_logger.warn("Not Setting DNS servers to empty");
    		return;
    	}
    	
    	if(currentServers != null && !currentServers.isEmpty()) {
    		if(!currentServers.equals(newServers)) {
    			s_logger.info("Change to DNS - setting dns servers: " + newServers);
    			linuxDns.setDnServers(newServers);
    		} else {
    			s_logger.debug("No change to DNS servers - not updating");
    		}
    	} else {
    		s_logger.info("Current DNS servers are null - setting dns servers: " + newServers);
    		linuxDns.setDnServers(newServers);
    	}
	}

    // Get a list of dns servers for all WAN interfaces
	private Set<IPAddress> getConfiguredDnsServers() {
		LinkedHashSet<IPAddress> serverList = new LinkedHashSet<IPAddress>();
    	
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = m_networkConfiguration.getNetInterfaceConfigs();
		// If there are multiple WAN interfaces, their configured DNS servers are all included in no particular order
		for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {		
		    if (netInterfaceConfig.getType() == NetInterfaceType.ETHERNET || 
		            netInterfaceConfig.getType() == NetInterfaceType.WIFI || 
		            netInterfaceConfig.getType() == NetInterfaceType.MODEM) {
	        	if(isEnabledForWan(netInterfaceConfig)) {
	        		try {
	        			Set<IPAddress> servers = getConfiguredDnsServers(netInterfaceConfig);
	        			s_logger.trace(netInterfaceConfig.getName() + " is WAN, adding its dns servers: " + servers);
						serverList.addAll(servers);
					} catch (KuraException e) {
						s_logger.error("Error adding dns servers for " + netInterfaceConfig.getName(), e);
					}
		        }
		    }
		}
		return serverList;
	}

	// Get a list of dns servers for the specified NetInterfaceConfig
    private Set<IPAddress> getConfiguredDnsServers(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
    	String interfaceName = netInterfaceConfig.getName();
    	s_logger.trace("Getting dns servers for " + interfaceName);
    	LinuxDns linuxDns = LinuxDns.getInstance();
    	LinkedHashSet<IPAddress> serverList = new LinkedHashSet<IPAddress>();
    	
    	for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceConfig.getNetInterfaceAddresses()) {
    		for(NetConfig netConfig : netInterfaceAddressConfig.getConfigs()) {
    			if(netConfig instanceof NetConfigIP4) {
    				NetConfigIP4 netConfigIP4 = (NetConfigIP4) netConfig;
					List<IP4Address> userServers = netConfigIP4.getDnsServers();
    				if(netConfigIP4.isDhcp()) {
    					// If DHCP but there are user defined entries, use those instead
    					if(userServers != null && !userServers.isEmpty()) {
    						s_logger.debug("Configured for DHCP with user-defined servers - adding: " + userServers);
    						serverList.addAll(userServers);
    					} else {
    						if(netInterfaceConfig.getType().equals(NetInterfaceType.MODEM)) {
    							// FIXME - don't like this
    							// cannot use interfaceName here because it one config behind
    							int pppNo = ((ModemInterfaceConfigImpl) netInterfaceConfig).getPppNum();
    							if (LinuxNetworkUtil.isUp("ppp"+pppNo)) {  
	    							List<IPAddress> servers = linuxDns.getPppDnServers();   
	    							if (servers != null) {
	    								s_logger.debug("Adding PPP dns servers: " + servers);
	    								serverList.addAll(servers);
	    							}
    							}
    						} else {
    							String currentAddress = LinuxNetworkUtil.getCurrentIpAddress(interfaceName);
    							List<IPAddress> servers = linuxDns.getDhcpDnsServers(interfaceName, currentAddress); 
    							if (servers != null) {
    								s_logger.debug("Configured for DHCP - adding DHCP servers: " + servers);
    								serverList.addAll(servers);
    							}
    						}
    					}    					
    				} else {
    					// If static, use the user defined entries
    					s_logger.debug("Configured for static - adding user-defined servers: " + userServers);
    					serverList.addAll(userServers);
    				}
    			}
    		}
    	}    	
    	return serverList;
    }
    
    private static void sleep(int millis) {
        long start = System.currentTimeMillis();
        long now = start;
        long end = start + millis;
        
        while(now < end) {
            try {
                Thread.sleep(end - now);
            } catch (InterruptedException e) {
                s_logger.debug("sleep interrupted: " + e);
            }
            
            now = System.currentTimeMillis();
        }
    }
}
