/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.monitor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.linux.net.route.RouteService;
import org.eclipse.kura.linux.net.route.RouteServiceImpl;
import org.eclipse.kura.linux.net.util.IScanTool;
import org.eclipse.kura.linux.net.util.IwLinkTool;
import org.eclipse.kura.linux.net.util.LinkTool;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.util.ScanTool;
import org.eclipse.kura.linux.net.util.iwconfigLinkTool;
import org.eclipse.kura.linux.net.wifi.WifiOptions;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.route.RouteConfig;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiClientMonitorListener;
import org.eclipse.kura.net.wifi.WifiClientMonitorService;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiMonitorServiceImpl implements WifiClientMonitorService, EventHandler {
	
    private static final Logger s_logger = LoggerFactory.getLogger(WifiMonitorServiceImpl.class);
    
    //private static final String OS_VERSION = System.getProperty("kura.os.version");
    
    private final static String[] EVENT_TOPICS = new String[] {
        NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC,
    };
    
    private static Object s_lock = new Object();
    
    private final static long THREAD_INTERVAL = /*30000*/10000;
    private final static long THREAD_TERMINATION_TOUT = 1; // in seconds
    
    private static Future<?> monitorTask;
    private static AtomicBoolean stopThread;
    
    private boolean m_first;
    
    private NetworkService m_networkService;
    private EventAdmin m_eventAdmin;
    private NetworkAdminService m_netAdminService;
    private NetworkConfigurationService m_netConfigService;
    private List<WifiClientMonitorListener>m_listeners;
    
    private Set<String> m_enabledInterfaces;
    private Set<String> m_disabledInterfaces;
    private Map<String, InterfaceState> m_interfaceStatuses;
    private ExecutorService m_executor;
		
	private NetworkConfiguration m_currentNetworkConfiguration;
	private NetworkConfiguration m_newNetConfiguration;
	
	
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

        s_logger.debug("Activating WifiMonitor Service...");

        m_first = true;
        
        m_enabledInterfaces = new HashSet<String>();
        m_disabledInterfaces = new HashSet<String>();
        m_interfaceStatuses = new HashMap<String, InterfaceState>();
        
        m_executor = Executors.newSingleThreadExecutor();
        
        stopThread = new AtomicBoolean();
		
        Dictionary<String, String[]> d = new Hashtable<String, String[]>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, d);
        m_listeners = new ArrayList<WifiClientMonitorListener>();
        try {
        	m_currentNetworkConfiguration = m_netConfigService.getNetworkConfiguration();
            initializeMonitoredInterfaces(m_currentNetworkConfiguration);
        	
        } catch (KuraException e) {
            s_logger.error("Could not update list of interfaces", e);
        }
    }

    protected void deactivate(ComponentContext componentContext) {
    	m_listeners = null;
        if ((monitorTask != null) && (!monitorTask.isDone())) {
        	stopThread.set(true);
        	monitorNotify();
        	s_logger.debug("Cancelling WifiMonitor task ...");
        	monitorTask.cancel(true);
    		s_logger.info("WifiMonitor task cancelled? = {}", monitorTask.isDone());
            monitorTask = null;
        }
        
        if (m_executor != null) {
        	s_logger.debug("Terminating WifiMonitor Thread ...");
        	m_executor.shutdownNow();
    		try {
				m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
    		s_logger.info("WifiMonitor Thread terminated? - {}", m_executor.isTerminated());
        	m_executor = null;
        }
    }
    
    private void monitor() {
        synchronized(s_lock) {
            try {
            	// Track the interfaces being reconfigured
            	List<String> interfacesToReconfigure = new ArrayList<String>();
            	
                // Check to see if the configuration has changed
            	//s_logger.debug("m_newNetConfiguration: " + m_newNetConfiguration);
            	//s_logger.debug("m_currentNetworkConfiguration: " + m_currentNetworkConfiguration);
            	
            	// Find and disable interfaces affected by the configuration change
                if(m_newNetConfiguration != null && !m_newNetConfiguration.equals(m_currentNetworkConfiguration)) {
                    s_logger.info("monitor() :: Found a new network configuration, will check if wifi has been reconfigured ...");
                    
                    // Note that the call to getReconfiguredWifiInterfaces() may also update
                    // m_enabledInterfaces or m_disabledInterfaces
                    interfacesToReconfigure.addAll(getReconfiguredWifiInterfaces());
                        
                    m_currentNetworkConfiguration = m_newNetConfiguration;
                    
                    // The interface being reconfigured is first disabled calling disableInterface().
                    // Note that calling disableInterface() does not update
                    // m_enabledInterfaces or m_disabledInterfaces.
                    // After calling disableInterface() and refreshing the list of
                    // interface statuses, a call to WifiState.isUp() should return false.
                    for(String interfaceName : interfacesToReconfigure) {
                        s_logger.debug("monitor() :: configuration has changed for {} , disabling...", interfaceName);
                        disableInterface(interfaceName);
                    }
                }

                // Check all interfaces configured to be enabled.
                // This includes the interfaces that might have been enabled by the above configuration change.
                // Get fresh interface statuses and post status change events.
                Map<String, InterfaceState> newStatuses = getInterfaceStatuses(m_enabledInterfaces);
                checkStatusChange(m_interfaceStatuses, newStatuses);
                m_interfaceStatuses = newStatuses;

                for(String interfaceName : m_enabledInterfaces) {
                	// Get current configuration
                	WifiInterfaceConfigImpl wifiInterfaceConfig = (WifiInterfaceConfigImpl) m_currentNetworkConfiguration.getNetInterfaceConfig(interfaceName);
                    WifiConfig wifiConfig = getWifiConfig(wifiInterfaceConfig);
    
                    // Make sure we have enough information
                    if (wifiInterfaceConfig == null) {
                    	s_logger.warn("monitor() :: missing WifiInterfaceConfigImpl for {}", interfaceName);
                    	continue;
                    }
                    if (wifiConfig == null) {
                    	s_logger.warn("monitor() :: missing WifiConfig for {}", interfaceName);
                    	continue;
                    }
                    
                    // There are interfaces for which we need to initially check if
                    // the right kernel module is loaded for the desired mode.
                    // If not we treat the interface as if needing to be reconfigured.
                    if (m_first &&
                    	!LinuxNetworkUtil.isKernelModuleLoadedForMode(interfaceName, wifiConfig.getMode())) {
                    	s_logger.info("monitor() :: {} kernel module not suitable for WiFi mode {}", interfaceName, wifiConfig.getMode());
                    	m_first = false;
                    	interfacesToReconfigure.add(interfaceName);
                    	disableInterface(interfaceName);
                    }

                	// Get current state
                	InterfaceState wifiState = m_interfaceStatuses.get(interfaceName);
                    if (wifiState == null) {
                    	s_logger.warn("monitor() :: missing InterfaceState for {}", interfaceName);
                    	continue;
                    }
                    
                    //s_logger.debug("Evaluating: " + interfaceName + " and is currently up? " + wifiState.isUp());
                    //s_logger.debug("Evaluating: " + interfaceName + " and is currently link up? " + wifiState.isLinkUp());
                    
                    // This flag is changed if the interface is disabled intentionally by the code below
                	boolean up = wifiState.isUp();
                    if(up) {
                    	// Infrastructure (Station) mode:
                    	// * Notify RSSI to listeners
                    	// * Detect interface link down: disable the interface
                    	// * Cannot ping Access Point: renew DHCP lease
                    	// * Check if enabled for LAN in DHCP mode: remove default gateway from route table
                    	//
                    	// Master (Access Point) mode:
                    	// * Detect interface link down: enable interface.
                    	//   FIXME should we just disable it like in the Infrastructure case above?
                        if(WifiMode.INFRA.equals(wifiConfig.getMode())) {
                        	// get signal strength only if somebody needs it
                        	if ((m_listeners != null) && (m_listeners.size() > 0)) {
                        		int rssi = 0;
                        		try {
                        			s_logger.debug("monitor() :: Getting Signal Level for {} -> {}", interfaceName,wifiConfig.getSSID());
                        			rssi = getSignalLevel(interfaceName,wifiConfig.getSSID());
                        			s_logger.debug("monitor() :: Wifi RSSI is {}", rssi);
                        		} catch (KuraException e) {
                        			s_logger.error("monitor() :: Failed to get Signal Level for {} -> {}", interfaceName, wifiConfig.getSSID());
                        			s_logger.error("monitor() :: Failed to get Signal Level - {}", e);
                        			rssi = 0;
                        		} 
                        		for (WifiClientMonitorListener listener : m_listeners) {
                        			listener.setWifiSignalLevel(rssi);
                        		}
                        	}
                        	
                        	if(!wifiState.isLinkUp()) {
                                s_logger.debug("monitor() :: link is down - disabling {}", interfaceName);                                
                                disableInterface(interfaceName);
                                up = false;
                            }
                            
                        	s_logger.debug("monitor() :: pingAccessPoint()? {}", wifiConfig.pingAccessPoint());
                        	if(wifiConfig.pingAccessPoint()) {
	                            NetConfigIP4 netConfigIP4 = getIP4config(wifiInterfaceConfig);
	                            if ((netConfigIP4 != null) && netConfigIP4.isDhcp()) {
	                            	boolean isApReachable = false;
	                            	for (int i = 0; i < 3; i++) {
	                            		isApReachable = isAccessPointReachable(interfaceName, 1000);
	                            		if (isApReachable) {
	                            			break;
	                            		}
	                            		try {Thread.sleep(1000);} catch (InterruptedException e) {}
	                            	}
		                            if (!isApReachable) {
		                            	m_netAdminService.renewDhcpLease(interfaceName);
		                            }
	                        	}
                        	}
                        	
                        	NetConfigIP4 netConfigIP4 = getIP4config(wifiInterfaceConfig);
                        	if(netConfigIP4.getStatus().equals(NetInterfaceStatus.netIPv4StatusEnabledLAN)) {
                        		if (netConfigIP4.isDhcp()) {
        	            			RouteService rs = RouteServiceImpl.getInstance();
        	            			RouteConfig rconf = rs.getDefaultRoute(interfaceName);
        	            			if (rconf != null) {
        	            				s_logger.debug("monitor() :: {} is configured for LAN/DHCP - removing GATEWAY route ...", rconf.getInterfaceName());
        	            				rs.removeStaticRoute(rconf.getDestination(), rconf.getGateway(), rconf.getNetmask(), rconf.getInterfaceName());
        	            			}
        	            		}
                        	}
                        } else if (WifiMode.MASTER.equals(wifiConfig.getMode())) {
                        	if(!wifiState.isLinkUp()) {
                        		enableInterface(wifiInterfaceConfig);
                        	}
                        }
                    }
                    
                    // Either initially down or initially up and we disabled it explicitly
                    // * Check if the interface is being reconfigured and
                    //   reload the kernel module (this may be ignored by the platform)
                    // * Infrastructure (Station) mode:
                    //   * Configured to ignore SSID: just enable interface. Otherwise:
                    //   * enable interface only if Access Point is available
                    //
                    // * Master (Access Point) mode:
                    //   * just enable interface
                    if (!up) {
                    	// Some interfaces may require reloading the kernel module
                    	// accordingly to the desired WifiMode.
                    	// FIXME ideally we only need to this if the WifiMode changes.
                    	// FIXME if reloading fails it won't be retried.
                    	if (interfacesToReconfigure.contains(interfaceName)) {
                    		try {
                    			s_logger.info("monitor() :: reload {} kernel module for WiFi mode {}",
                    					interfaceName, wifiConfig.getMode());
                    			reloadKernelModule(interfaceName, wifiConfig.getMode());
                    		} catch (KuraException e) {
                    			s_logger.warn("monitor() :: failed to reload {} kernel module."
                    					+ " FIXME: THIS WON'T BE RETRIED",
                    					interfaceName, e);
                    			continue;
                    		}
                    	}

                    	try {
	                        if(WifiMode.MASTER.equals(wifiConfig.getMode())) {
	                            s_logger.debug("monitor() :: enable {} in master mode", interfaceName);                            
	                            enableInterface(wifiInterfaceConfig);
	                        } else if (WifiMode.INFRA.equals(wifiConfig.getMode())) {
	                        	if (wifiConfig.ignoreSSID()) {
	                        		s_logger.info("monitor() :: enable {} in infra mode", interfaceName);                                
	                        		enableInterface(wifiInterfaceConfig);
	                        	} else {
		                            if(isAccessPointAvailable(interfaceName, wifiConfig.getSSID())) {
		                                s_logger.info("monitor() :: found access point - enable {} in infra mode", interfaceName);                                
		                                enableInterface(wifiInterfaceConfig);
		                            } else {
		                            	s_logger.warn("monitor() :: {} - access point is not available", wifiConfig.getSSID());
		                            }
	                        	}
	                        }
						} catch (KuraException e) {
							s_logger.error("monitor() :: Error enabling {} interface, will try to reset wifi", interfaceName, e);
							resetWifiDevice(interfaceName);
						}
                    }
                }
                
                // Check all interfaces configured to be disabled
                for(String interfaceName : m_disabledInterfaces) {
                    InterfaceState wifiState = m_interfaceStatuses.get(interfaceName);
                    if(wifiState != null && wifiState.isUp()) {
                        s_logger.debug("monitor() :: {} is currently up - disable interface", interfaceName);
                        disableInterface(interfaceName);
                    }
                }
                                
                // Shut down the monitor if no interface is configured to be enabled
                if(m_enabledInterfaces.size() == 0) {
                    if(monitorTask != null) {
                        s_logger.debug("monitor() :: No enabled wifi interfaces - shutting down monitor thread");
                        stopThread.set(true);
                        monitorTask.cancel(true);
                        monitorTask = null;
                    }
                }
    
            } catch (Exception e) {
                s_logger.warn("Error during WiFi Monitor handle event", e);
            }
        }
    }
    
    private void checkStatusChange(Map<String, InterfaceState> oldStatuses, Map<String, InterfaceState> newStatuses) {
		
		if (newStatuses != null) {
	        // post NetworkStatusChangeEvent on current and new interfaces
			Iterator<String> it = newStatuses.keySet().iterator();
			while (it.hasNext()) {
				String interfaceName = it.next();
				if ((oldStatuses != null) && oldStatuses.containsKey(interfaceName)) {
					if (!newStatuses.get(interfaceName).equals(oldStatuses.get(interfaceName))) {
						s_logger.debug("Posting NetworkStatusChangeEvent on interface: {}", interfaceName);
						m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, newStatuses.get(interfaceName), null));
					}
				} else {
					s_logger.debug("Posting NetworkStatusChangeEvent on enabled interface: {}", interfaceName);
					m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, newStatuses.get(interfaceName), null));
				}
			}
	        
	        // post NetworkStatusChangeEvent on interfaces that are no longer there
	        if (oldStatuses != null) {
                it = oldStatuses.keySet().iterator();
                while (it.hasNext()) {                    
                    String interfaceName = it.next();
                    if(!newStatuses.containsKey(interfaceName)) {
                        s_logger.debug("Posting NetworkStatusChangeEvent on disabled interface: {}", interfaceName);
                        m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, oldStatuses.get(interfaceName), null));
                    }
                }
	        }
		}
	}
    
    @Override
	public void registerListener(WifiClientMonitorListener newListener) {
    	boolean found = false;
		if (m_listeners == null) {
			m_listeners = new ArrayList<WifiClientMonitorListener>();
		}
		if (m_listeners.size() > 0) {
			for (WifiClientMonitorListener listener : m_listeners) {
				if (listener.equals(newListener)) {
					found = true;
					break;
				}
			}
		}
		if (!found) {
			m_listeners.add(newListener);
		}
	}
    
    @Override
	public void unregisterListener(WifiClientMonitorListener listenerToUnregister) {
    	if ((m_listeners != null) && (m_listeners.size() > 0)) {
    		
    		for (int i = 0; i < m_listeners.size(); i++) {
    			if (((WifiClientMonitorListener)m_listeners.get(i)).equals(listenerToUnregister)) {
    				m_listeners.remove(i);
    			}
    		}
		}
	}

    @Override
    public void handleEvent(Event event) {
        s_logger.debug("handleEvent - topic: {}", event.getTopic());
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
                    m_newNetConfiguration = new NetworkConfiguration(props);
                    
                    // Initialize the monitor thread if needed
                    if(monitorTask == null) {
                        initializeMonitoredInterfaces(m_newNetConfiguration);
                    } else {
                    	monitorNotify();
                    }
                } catch (Exception e) {
                    s_logger.warn("Error during WiFi Monitor handle event", e);
                }
            }
        }
    }
    
    private boolean isWifiEnabled(WifiInterfaceConfigImpl wifiInterfaceConfig) {
        WifiMode wifiMode = WifiMode.UNKNOWN;
        NetInterfaceStatus status = NetInterfaceStatus.netIPv4StatusUnknown;
        
        if(wifiInterfaceConfig != null) {
            for(WifiInterfaceAddressConfig wifiInterfaceAddressConfig : wifiInterfaceConfig.getNetInterfaceAddresses()) {
                wifiMode = wifiInterfaceAddressConfig.getMode();
                
                for(NetConfig netConfig : wifiInterfaceAddressConfig.getConfigs()) {
                    if(netConfig instanceof NetConfigIP4) {
                        status = ((NetConfigIP4)netConfig).getStatus();
                    }
                }
            }
        } else {
        	s_logger.debug("wifiInterfaceConfig is null");
        }
        
        boolean statusEnabled = (status.equals(NetInterfaceStatus.netIPv4StatusEnabledLAN)
                || status.equals(NetInterfaceStatus.netIPv4StatusEnabledWAN));
        boolean wifiEnabled = (wifiMode.equals(WifiMode.INFRA) 
                || wifiMode.equals(WifiMode.MASTER));

        s_logger.debug("statusEnabled: " + statusEnabled);
        s_logger.debug("wifiEnabled: " + wifiEnabled);
         
        return statusEnabled && wifiEnabled;
    }
    
    private WifiConfig getWifiConfig(WifiInterfaceConfigImpl wifiInterfaceConfig) {
        WifiConfig selectedWifiConfig = null;
        WifiMode wifiMode = WifiMode.UNKNOWN;
        
        if(wifiInterfaceConfig != null) {
            loop:
            for(WifiInterfaceAddressConfig wifiInterfaceAddressConfig : wifiInterfaceConfig.getNetInterfaceAddresses()) {
                wifiMode = wifiInterfaceAddressConfig.getMode();
                
                for(NetConfig netConfig : wifiInterfaceAddressConfig.getConfigs()) {
                    if(netConfig instanceof WifiConfig) {
                        WifiConfig wifiConfig = (WifiConfig)netConfig;
                        if(wifiMode.equals(wifiConfig.getMode())) {
                            selectedWifiConfig = wifiConfig;
                            break loop;
                        }
                    }
                }
            }
        }
        return selectedWifiConfig;
    }
    
    private NetConfigIP4 getIP4config(WifiInterfaceConfigImpl wifiInterfaceConfig) {
    	
    	NetConfigIP4 netConfigIP4 = null;
    	if(wifiInterfaceConfig != null) {
    		loop:
    		for(WifiInterfaceAddressConfig wifiInterfaceAddressConfig : wifiInterfaceConfig.getNetInterfaceAddresses()) {
    			for(NetConfig netConfig : wifiInterfaceAddressConfig.getConfigs()) {
    				if(netConfig instanceof NetConfigIP4) {
    					netConfigIP4 = (NetConfigIP4)netConfig;
    					break loop;
    				}
    			}
    		}
    	}
    	
    	return netConfigIP4;
    }
    
    private void disableInterface(String interfaceName) throws KuraException {
        s_logger.debug("Disabling {}", interfaceName);
        m_netAdminService.disableInterface(interfaceName);
        m_netAdminService.manageDhcpServer(interfaceName, false);
    }
    
    private void enableInterface(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
        
        s_logger.debug("enableInterface: {}", netInterfaceConfig);
        WifiInterfaceConfigImpl wifiInterfaceConfig = null;
        
        if(netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
            wifiInterfaceConfig = (WifiInterfaceConfigImpl) netInterfaceConfig;
        } else {
            return;
        }
        
        String interfaceName = wifiInterfaceConfig.getName();
        
        WifiMode wifiMode = WifiMode.UNKNOWN;
        NetInterfaceStatus status = NetInterfaceStatus.netIPv4StatusUnknown;
        boolean isDhcpClient = false;
        boolean enableDhcpServer = false;
        
        if(wifiInterfaceConfig != null) {
            for(WifiInterfaceAddressConfig wifiInterfaceAddressConfig : wifiInterfaceConfig.getNetInterfaceAddresses()) {
                wifiMode = wifiInterfaceAddressConfig.getMode();

                for(NetConfig netConfig : wifiInterfaceAddressConfig.getConfigs()) {
                    if(netConfig instanceof NetConfigIP4) {
                        status = ((NetConfigIP4) netConfig).getStatus();
                        isDhcpClient = ((NetConfigIP4) netConfig).isDhcp();
                    } else if(netConfig instanceof DhcpServerConfig4) {
                        enableDhcpServer = ((DhcpServerConfig4) netConfig).isEnabled();
                    }
                }
            }
        }

        if(status.equals(NetInterfaceStatus.netIPv4StatusEnabledLAN)
                || status.equals(NetInterfaceStatus.netIPv4StatusEnabledWAN)) {
            
            if(wifiMode.equals(WifiMode.INFRA) || wifiMode.equals(WifiMode.MASTER)) {
                m_netAdminService.enableInterface(interfaceName, isDhcpClient);

                if(enableDhcpServer) {
                    m_netAdminService.manageDhcpServer(interfaceName, true);
                }
            }
        }
    }
        
    private void reloadKernelModule(String interfaceName, WifiMode wifiMode) throws KuraException {
    	s_logger.info("monitor() :: reload {} using kernel module for WiFi mode {}",
    			interfaceName, wifiMode);
    	if (LinuxNetworkUtil.isKernelModuleLoaded(interfaceName, wifiMode)) {
    		LinuxNetworkUtil.unloadKernelModule(interfaceName);
    	}
    	LinuxNetworkUtil.loadKernelModule(interfaceName, wifiMode);
    }
    
    private void initializeMonitoredInterfaces(NetworkConfiguration networkConfiguration) throws KuraException {
        synchronized (s_lock) {
            m_enabledInterfaces.clear();
            m_disabledInterfaces.clear();
    
            if(networkConfiguration != null) {
                for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig: networkConfiguration.getNetInterfaceConfigs()) {
                    
                    String interfaceName = netInterfaceConfig.getName(); 
                    if (netInterfaceConfig.getType() != NetInterfaceType.WIFI) {
                        continue;
                    }
    
                    // ignore "mon" interface
                    if(interfaceName.startsWith("mon")) {
                        continue;
                    }
                    
                    if(netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                        if(isWifiEnabled((WifiInterfaceConfigImpl)netInterfaceConfig)) {
                            s_logger.debug("Adding {} to enabledInterfaces", interfaceName);
                            m_enabledInterfaces.add(interfaceName);
                        } else {
                            s_logger.debug("Adding {} to disabledInterfaces", interfaceName);
                            m_disabledInterfaces.add(interfaceName);
                        }
                    }
                }
            } else {
            	s_logger.info("networkConfiguration is null");
            }
            
            if(m_enabledInterfaces.size() > 0) {
                m_interfaceStatuses = getInterfaceStatuses(m_enabledInterfaces);
                
                if(monitorTask == null) {
	                s_logger.info("Starting WifiMonitor thread...");
	                stopThread.set(false);
	                monitorTask = m_executor.submit(new Runnable() {
	                    @Override
	                    public void run() {
	                    	while (!stopThread.get()) {
	                    		Thread.currentThread().setName("WifiMonitor Thread");
	                        	try {
	                        		monitor();
	                        		monitorWait();
								} catch (InterruptedException interruptedException) {
									Thread.interrupted();
	                                s_logger.debug("WiFi monitor interrupted - {}", interruptedException);
								} catch (Throwable t) {
	                                s_logger.error("Exception while monitoring WiFi connection - {}", t);
								}
	                    	}
	                }});
                } else {
                	monitorNotify();
                }
            }
        }
    }
    
    private Collection<String> getReconfiguredWifiInterfaces() throws KuraException {
        
    	Set<String> reconfiguredInterfaces = new HashSet<String>();
        m_enabledInterfaces = new HashSet<String>();
        m_disabledInterfaces = new HashSet<String>();
        
        for(String interfaceName : m_networkService.getAllNetworkInterfaceNames()) {
            // skip non-wifi interfaces
            if(LinuxNetworkUtil.getType(interfaceName) != NetInterfaceType.WIFI) {
                continue;
            }

            // ignore "mon" interface
            if(interfaceName.startsWith("mon")) {
                continue;
            }
            
            // Get the old wifi config
            WifiInterfaceConfigImpl currentConfig = null;
            if(m_currentNetworkConfiguration != null) {
                NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = m_currentNetworkConfiguration.getNetInterfaceConfig(interfaceName);
                if(netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                	currentConfig = (WifiInterfaceConfigImpl) netInterfaceConfig;
                }
            }
            
            // Get the new wifi config
            WifiInterfaceConfigImpl newConfig = null;
            if(m_newNetConfiguration != null) {
                NetInterfaceConfig<? extends NetInterfaceAddressConfig> newNetInterfaceConfig = m_newNetConfiguration.getNetInterfaceConfig(interfaceName);
                if(newNetInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                    newConfig = (WifiInterfaceConfigImpl) newNetInterfaceConfig;
                }
            }
            
            if(newConfig != null && currentConfig != null) {
            	List<WifiInterfaceAddressConfig> currentInterfaceAddressConfigs = currentConfig.getNetInterfaceAddresses();
            	List<WifiInterfaceAddressConfig> newInterfaceAddressConfigs = newConfig.getNetInterfaceAddresses();
            	
            	if(currentInterfaceAddressConfigs == null && newInterfaceAddressConfigs == null) {
            		//no config changed - continue
            		continue;
            	}
            	
            	if(currentInterfaceAddressConfigs == null || newInterfaceAddressConfigs == null) {
            		reconfiguredInterfaces.add(interfaceName);
            		continue;
            	}
            	
            	// TODO: compare interfaceAddressConfigs
            	
            	//FIXME - assuming one InterfaceAddressConfig for now
            	WifiInterfaceAddressConfig currentInterfaceAddressConfig = currentInterfaceAddressConfigs.get(0);
            	WifiInterfaceAddressConfig newInterfaceAddressConfig = newInterfaceAddressConfigs.get(0);
            	WifiMode newWifiMode = newInterfaceAddressConfig.getMode();
            	
            	if(currentInterfaceAddressConfig.getConfigs() == null && newInterfaceAddressConfig.getConfigs() == null) {
            		continue;
            	}
            	
            	if(currentInterfaceAddressConfig.getConfigs() == null || newInterfaceAddressConfig.getConfigs() == null) {
            		reconfiguredInterfaces.add(interfaceName);
            		continue;
            	}
            	
            	// Remove other WifiConfigs that don't match the selected mode, for comparison purposes
            	List<NetConfig> currentNetConfigs = new ArrayList<NetConfig>(currentInterfaceAddressConfig.getConfigs());
            	List<NetConfig> newNetConfigs = new ArrayList<NetConfig>(newInterfaceAddressConfig.getConfigs());            	

            	Iterator<NetConfig> it = currentNetConfigs.iterator();
            	while(it.hasNext()) {
            		NetConfig nc = it.next();
            		if(nc instanceof WifiConfig && ((WifiConfig) nc).getMode() != newWifiMode) {
            			s_logger.debug("removing current non-active WifiConfig for comparison: {}", nc);
						it.remove();
						reconfiguredInterfaces.add(interfaceName);
        			}
            	}
            	
            	it = newNetConfigs.iterator();
            	while(it.hasNext()) {
            		NetConfig nc = it.next();
            		if(nc instanceof WifiConfig && ((WifiConfig) nc).getMode() != newWifiMode) {
            			s_logger.debug("removing new non-active WifiConfig for comparison: {}", nc);
						it.remove();
        			}
            	}
            	
            	if(currentNetConfigs.size() != newNetConfigs.size()) {
            	    s_logger.debug("\tNumber of configs changed - Old config has: {}", currentNetConfigs.size());
            	    s_logger.debug("\tNumber of configs changed - New config has: {}", newNetConfigs.size());
                    reconfiguredInterfaces.add(interfaceName);
            	} else {
                	for(int i = 0; i < currentNetConfigs.size(); i++) {
                		boolean foundConfigMatch = false;
                		NetConfig currentNetConfig = currentNetConfigs.get(i);
                		for(int j = 0; j < newNetConfigs.size(); j++) {
                			NetConfig newNetConfig = newNetConfigs.get(j);
                			
                			if(newNetConfig.getClass() == currentNetConfig.getClass()) {
        						foundConfigMatch = true;        						
        						
        						//if the config is different and is not the FirewallNatConfig
        						if(!newNetConfig.equals(currentNetConfig) && newNetConfig.getClass() != FirewallAutoNatConfig.class) {
        							s_logger.debug("\tConfig changed - Old config: {}", currentNetConfig);
        							s_logger.debug("\tConfig changed - New config: {}", newNetConfig);
        							reconfiguredInterfaces.add(interfaceName);
        						}
        						break;
        					}
                		}
                		
                		if(!foundConfigMatch) {
                		    s_logger.debug("\tConfig was removed - Old config: {}", currentNetConfig);
                			reconfiguredInterfaces.add(interfaceName);
                			break;
                		}
                	}
            	}
            	
            	/*
                if(!newConfig.equals(currentConfig)) {
                    s_logger.debug("Configuration for " + interfaceName + " has changed - need to reconfigure");
                    reconfiguredInterfaces.add(interfaceName);
                }*/
            } else if(newConfig != null) {
            	//only newConfig - oldConfig is null
            	s_logger.debug("oldConfig was null, adding newConfig");
            	reconfiguredInterfaces.add(interfaceName);
            } else if(currentConfig != null) {
                s_logger.debug("Configuration for {} has changed", interfaceName);
                reconfiguredInterfaces.add(interfaceName);
                s_logger.debug("Removing {} from list of enabled interfaces because it is not configured", interfaceName);
                m_disabledInterfaces.add(interfaceName);
            } else {
            	s_logger.debug("old and new wifi config are null...");
            }
            
            // do we need to monitor?
            if(isWifiEnabled(newConfig)) {
                s_logger.debug("Adding {} to list of enabled interfaces", interfaceName);
                m_enabledInterfaces.add(interfaceName);
            } else {
                s_logger.debug("Removing {} from list of enabled interfaces because it is disabled", interfaceName);
                m_disabledInterfaces.add(interfaceName);
            }
        }
        
        return reconfiguredInterfaces;
    }
    
    private boolean isAccessPointAvailable(String interfaceName, String ssid) throws KuraException {
        boolean available = false;
		if (ssid != null) {
			IScanTool scanTool = ScanTool.get(interfaceName);
			if (scanTool != null) {
				List<WifiAccessPoint> wifiAccessPoints = scanTool.scan();
				for (WifiAccessPoint wap : wifiAccessPoints) {
					if (ssid.equals(wap.getSSID())) {
						s_logger.trace("isAccessPointAvailable() :: SSID={} is available :: strength={}", ssid, wap.getStrength());
						available = Math.abs(wap.getStrength()) > 0;
						break;
					}
				}
			}
		}
        
        return available;
    }
    
    @Override
	public int getSignalLevel(String interfaceName, String ssid)
			throws KuraException {
		int rssi = 0;
		InterfaceState wifiState = m_interfaceStatuses.get(interfaceName);
		if ((wifiState != null) && (ssid != null)) {
			if(wifiState.isUp()) {
				s_logger.trace("getSignalLevel() :: using 'iw dev wlan0 link' command ...");
				//IwLinkTool iwLinkTool = new IwLinkTool(interfaceName);
				Collection<String> supportedWifiOptions = WifiOptions.getSupportedOptions(interfaceName);
				LinkTool linkTool = null;
				if ((supportedWifiOptions != null) && (supportedWifiOptions.size() > 0)) {
		            if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
		            	linkTool = new IwLinkTool(interfaceName);
		            } else if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_WEXT)) {
		            	linkTool = new iwconfigLinkTool(interfaceName);
		            }
				}
				
				if((linkTool != null) && linkTool.get()) { 
					if (linkTool.isLinkDetected()) {
						rssi = linkTool.getSignal();
						s_logger.debug("getSignalLevel() :: rssi={} (using 'iw dev wlan0 link')", rssi);
					}
				}
			} 
			
			if (rssi == 0) {
				s_logger.trace("getSignalLevel() :: using 'iw dev wlan0 scan' command ...");
				IScanTool scanTool = ScanTool.get(interfaceName);
				if (scanTool != null) {
					List<WifiAccessPoint> wifiAccessPoints = scanTool.scan();
					for (WifiAccessPoint wap : wifiAccessPoints) {
						if (ssid.equals(wap.getSSID())) {
							if (wap.getStrength() > 0) {
								rssi = 0 - wap.getStrength();
								s_logger.debug("getSignalLevel() :: rssi={} (using 'iw dev wlan0 scan')", rssi);
							}
							break;
						}
					}
				}
			}
		}

		return rssi;
	}
    
    private boolean isAccessPointReachable(String interfaceName, int tout) throws KuraException {
    	
    	boolean ret = true;
    	RouteService rs = RouteServiceImpl.getInstance();
    	RouteConfig rconf = rs.getDefaultRoute(interfaceName);
    	if (rconf != null) {
    		IPAddress ipAddress = rconf.getGateway();
    		String iface = rconf.getInterfaceName();
    		if ((ipAddress != null) && (iface != null) && iface.equals(interfaceName)) {
	    		try {
					InetAddress inetAddress = InetAddress.getByName(ipAddress.getHostAddress());
					try {
						ret = inetAddress.isReachable(tout);
						s_logger.info("Access point reachable? " + ret);
					} catch (IOException e) {
						throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
					}
				} catch (UnknownHostException e) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
				}
    		}
    	}
    	return ret;
    }
        
    private Map<String, InterfaceState> getInterfaceStatuses(Collection<String> interfaceList) throws KuraException {
        Map<String, InterfaceState> statuses = new HashMap<String, InterfaceState>();
        
        for(String interfaceName : interfaceList) {
        	WifiInterfaceConfigImpl wifiInterfaceConfig = (WifiInterfaceConfigImpl) m_currentNetworkConfiguration.getNetInterfaceConfig(interfaceName);
        	if(wifiInterfaceConfig == null){
        		continue;
        	}
        	WifiConfig wifiConfig = getWifiConfig(wifiInterfaceConfig);
        	if(wifiConfig != null){
        		statuses.put(interfaceName, new WifiInterfaceState(interfaceName, wifiConfig.getMode()));
        	}
        }
        
        return statuses;
    }
    
    private boolean resetWifiDevice(String interfaceName) throws Exception {
    	boolean ret = false;
    	if (LinuxNetworkUtil.isWifiDeviceOn(interfaceName)) {
    		LinuxNetworkUtil.turnWifiDeviceOff(interfaceName);
    	}
    	if (isWifiDeviceReady(interfaceName, false, 10)) {
    		LinuxNetworkUtil.turnWifiDeviceOn(interfaceName);
    		ret = this.isWifiDeviceReady(interfaceName, true, 20);
    	}
    	return ret;
    }
    
    private boolean isWifiDeviceReady(String interfaceName, boolean expected, int tout) {
    	boolean deviceReady = false;
    	long tmrStart = System.currentTimeMillis();
    	do {
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
    		boolean deviceOn = LinuxNetworkUtil.isWifiDeviceOn(interfaceName);
    		s_logger.trace("isWifiDeviceReady()? :: deviceOn={}, expected={}", deviceOn, expected);
    		if (deviceOn == expected) {
    			deviceReady = true;
    			break;
    		}
    	} while ((System.currentTimeMillis()-tmrStart) < tout*1000);
    	
    	s_logger.debug("isWifiDeviceReady()? :: deviceReady={}", deviceReady);
    	return deviceReady;
    }
        
	private void monitorNotify() {
		if (stopThread != null) {
			synchronized (stopThread) {
				stopThread.notifyAll();
			}
		}
	}
	
	private void monitorWait() throws InterruptedException {
		if (stopThread != null) {
			synchronized (stopThread) {
				stopThread.wait(THREAD_INTERVAL);
			}
		}
	}
}
