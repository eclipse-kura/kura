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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.ConnectionInfoImpl;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.admin.modem.CellularModem;
import org.eclipse.kura.net.admin.modem.CellularModemFactory;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.IModemLinkService;
import org.eclipse.kura.net.admin.modem.ModemManagerService;
import org.eclipse.kura.net.admin.modem.PppFactory;
import org.eclipse.kura.net.admin.modem.PppState;
import org.eclipse.kura.net.admin.modem.SupportedSerialModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedSerialModemsFactoryInfo.SerialModemFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo.UsbModemFactoryInfo;
import org.eclipse.kura.net.modem.ModemAddedEvent;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemMonitorListener;
import org.eclipse.kura.net.modem.ModemMonitorService;
import org.eclipse.kura.net.modem.ModemRemovedEvent;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.usb.UsbDeviceEvent;
import org.eclipse.kura.usb.UsbModemDevice;

public class ModemMonitorServiceImpl implements ModemMonitorService, ModemManagerService, EventHandler {
	
	private static final Logger s_logger = LoggerFactory.getLogger(ModemMonitorServiceImpl.class);
	
	private ComponentContext      m_ctx;
	private final static String[] EVENT_TOPICS = new String[] {
			NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC,
			ModemAddedEvent.MODEM_EVENT_ADDED_TOPIC,
			ModemRemovedEvent.MODEM_EVENT_REMOVED_TOPIC, };
	
	private final static long THREAD_INTERVAL = 30000;
	private final static long THREAD_TERMINATION_TOUT = 1; // in seconds
	
	private static Future<?>  task;
	private static boolean stopThread;

	private SystemService m_systemService;
	private NetworkService m_networkService;
	private NetworkAdminService m_networkAdminService;
	private EventAdmin m_eventAdmin;
	
	private List<ModemMonitorListener>m_listeners;
	
	private ExecutorService m_executor;
	
	private Map<String, CellularModem> m_modems;
	private Map<String, InterfaceState> m_interfaceStatuses;
	
	private Boolean m_gpsSupported = null;
	
	private boolean m_serviceActivated; 
	    
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
    
    public void setNetworkAdminService(NetworkAdminService networkAdminService) {
    	m_networkAdminService = networkAdminService;
    }
    
    public void unsetNetworkAdminService(NetworkAdminService networkAdminService) {
    	m_networkAdminService = null;
    }
        
	public void setSystemService(SystemService systemService) {
		m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		m_systemService = null;
	}
	
    protected void activate(ComponentContext componentContext)  {
    	// save the bundle context
    	m_ctx = componentContext;
    	
    	Dictionary<String, String[]> d = new Hashtable<String, String[]>();
    	d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
    	m_ctx.getBundleContext().registerService(EventHandler.class.getName(), this, d);
    	
		m_modems = new HashMap<String, CellularModem>();
		m_interfaceStatuses = new HashMap<String, InterfaceState>();
		m_listeners = new ArrayList<ModemMonitorListener>();
		
		// track currently installed modems
		try {
			for(NetInterface<? extends NetInterfaceAddress> netInterface : m_networkService.getNetworkInterfaces()) {
				if(netInterface instanceof ModemInterface) {
					ModemDevice modemDevice = ((ModemInterface<?>) netInterface).getModemDevice();
					trackModem(modemDevice);
				}
			}
		} catch (Exception e) {
			s_logger.error("Error getting installed modems", e);
		}
		
		stopThread = false;
		m_executor = Executors.newSingleThreadExecutor();
		task = m_executor.submit(new Runnable() {
    		@Override
    		public void run() {
    			while (!stopThread) {
	    			Thread.currentThread().setName("ModemMonitor");
	    			monitor();
	    			try {
						Thread.sleep(THREAD_INTERVAL);
					} catch (InterruptedException e) {
						s_logger.debug(e.getMessage());
					}
    			}
    	}});
		
		m_serviceActivated = true;
		s_logger.debug("ModemMonitor activated and ready to receive events");
    }
    
    protected void deactivate(ComponentContext componentContext) {
    	m_listeners = null;
    	stopThread = true;
    	PppFactory.releaseAllPppServices();
    	if ((task != null) && (!task.isDone())) {
    		s_logger.debug("Cancelling ModemMonitor task ...");
    		task.cancel(true);
    		s_logger.info("ModemMonitor task cancelled? = {}", task.isDone());
    		task = null;
    	}
    	
    	if (m_executor != null) {
    		s_logger.debug("Terminating ModemMonitor Thread ...");
    		m_executor.shutdownNow();
    		try {
				m_executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
    		s_logger.info("ModemMonitor Thread terminated? - {}", m_executor.isTerminated());
			m_executor = null;
    	}
    	m_serviceActivated = false;
	}
    
    @Override
	public void handleEvent(Event event) {
    	s_logger.debug("handleEvent - topic: " + event.getTopic());
        String topic = event.getTopic();
        if (topic.equals(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC)) {
        	Set<String> keySet = m_modems.keySet();
    		Iterator<String> keySetItetrator = keySet.iterator();
    		while (keySetItetrator.hasNext()) {
    			String usbPort = keySetItetrator.next();
    			CellularModem modem = m_modems.get(usbPort);
    			try {
    				String ifaceName = null;
	    			if (m_networkService != null) {
	    				ifaceName = m_networkService.getModemPppPort(modem.getModemDevice());
	    			}
	    			if (ifaceName != null) {
		    			List<NetConfig> oldNetConfigs = modem.getConfiguration();
		    			List<NetConfig>newNetConfigs = m_networkAdminService.getNetworkInterfaceConfigs(ifaceName);
		    			if ((oldNetConfigs == null) || !oldNetConfigs.equals(newNetConfigs)) {
		    				s_logger.info("new configuration for cellular modem on usb port " + usbPort + " netinterface " + ifaceName); 
		    				int ifaceNo = getInterfaceNumber(oldNetConfigs);
		    				if (ifaceNo >= 0) {
		    					IModemLinkService pppService = PppFactory.obtainPppService(ifaceNo, modem.getDataPort());
		    					if (pppService != null) {
		    						PppState pppState = pppService.getPppState();
									if ((pppState == PppState.CONNECTED) || (pppState == PppState.IN_PROGRESS)) {
										s_logger.info("disconnecting " + pppService.getIfaceName());
										pppService.disconnect();
									}
									PppFactory.releasePppService(pppService.getIfaceName());
		    					}
		    				}
		    				
		    				modem.setConfiguration(newNetConfigs);
		    				if (m_gpsSupported == null) {
		    					boolean gpsSupported = modem.isGpsSupported();
		    					m_gpsSupported = gpsSupported;
		    				}
		    				s_logger.debug("handleEvent() :: gpsSupported={}", m_gpsSupported);
		    				if (m_gpsSupported) {
		    					if (isGpsEnabledInConfig(newNetConfigs)) {
		    						s_logger.debug("handleEvent() :: enabling GPS");
		    						modem.enableGps();
		    					} else {
		    						s_logger.debug("handleEvent() :: disabling GPS");
		    						modem.disableGps();
		    					}
		    				}
		    				if (modem instanceof EvdoCellularModem) {
			    				NetInterfaceStatus netIfaceStatus = getNetInterfaceStatus(newNetConfigs);
								if (netIfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
									if (!((EvdoCellularModem) modem).isProvisioned()) {
										s_logger.info("NetworkConfigurationChangeEvent :: The " + modem.getModel() + " is not provisioned, will try to provision it ...");
										
										if ((task != null) && !task.isCancelled()) {
											s_logger.info("NetworkConfigurationChangeEvent :: Cancelling monitor task");
											stopThread = true;
											task.cancel(true);
											task = null;
										}
										
										((EvdoCellularModem) modem).provision();
										if (task == null) {
											s_logger.info("NetworkConfigurationChangeEvent :: Restarting monitor task");
											stopThread = false;
											task = m_executor.submit(new Runnable() {
									    		@Override
									    		public void run() {
									    			while (!stopThread) {
									    				Thread.currentThread().setName("ModemMonitor");
									    				monitor();
									    				try {
															Thread.sleep(THREAD_INTERVAL);
														} catch (InterruptedException e) {
															s_logger.debug(e.getMessage());
														}
									    			}
									    	}});
										}
									} else {
										s_logger.info("NetworkConfigurationChangeEvent :: The " + modem.getModel() + " is provisioned");
									}
								}
		    				}
		    			}
	    			}
    			} catch (KuraException e) {
    				e.printStackTrace();
    			}
    		}
        } else if (topic.equals(ModemAddedEvent.MODEM_EVENT_ADDED_TOPIC)) {
        	
        	ModemAddedEvent modemAddedEvent = (ModemAddedEvent)event;
        	ModemDevice modemDevice = modemAddedEvent.getModemDevice();
        	if (m_serviceActivated) {
        		trackModem(modemDevice);
        	}
        } else if (topic.equals(ModemRemovedEvent.MODEM_EVENT_REMOVED_TOPIC)) {
        	ModemRemovedEvent modemRemovedEvent = (ModemRemovedEvent)event;
        	String usbPort = (String)modemRemovedEvent.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY);
        	m_modems.remove(usbPort);
        }	
	}

	
    @Override
	public CellularModem getModemService (String usbPort) {
		return m_modems.get(usbPort);
	}
    
	private NetInterfaceStatus getNetInterfaceStatus (List<NetConfig> netConfigs) {
		
		NetInterfaceStatus interfaceStatus = NetInterfaceStatus.netIPv4StatusUnknown;
		if ((netConfigs != null) && (netConfigs.size() > 0)) {
			for (NetConfig netConfig : netConfigs) {
				if (netConfig instanceof NetConfigIP4) {
					interfaceStatus = ((NetConfigIP4) netConfig).getStatus();
					break;
				}
			}
		}
		return interfaceStatus;
	}
	
	@Override
	public void registerListener(ModemMonitorListener newListener) {
		boolean found = false;
		if (m_listeners == null) {
			m_listeners = new ArrayList<ModemMonitorListener>();
		}
		if (m_listeners.size() > 0) {
			for (ModemMonitorListener listener : m_listeners) {
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
	public void unregisterListener(ModemMonitorListener listenerToUnregister) {
		if ((m_listeners != null) && (m_listeners.size() > 0)) {
			
			for (int i = 0; i < m_listeners.size(); i++) {
				if (((ModemMonitorListener)m_listeners.get(i)).equals(listenerToUnregister)) {
					m_listeners.remove(i);
				}
			}
		}
	}
	
	private int getInterfaceNumber (List<NetConfig> netConfigs) {
		int ifaceNo = -1;
		if ((netConfigs != null) && (netConfigs.size() > 0)) {
			for (NetConfig netConfig : netConfigs) {
				if (netConfig instanceof ModemConfig) {
					ifaceNo = ((ModemConfig) netConfig).getPppNumber();
					break;
				}
			}
		}
		return ifaceNo;
	}
	
	private boolean isGpsEnabledInConfig(List<NetConfig> netConfigs) {
		boolean isGpsEnabled = false;
		if ((netConfigs != null) && (netConfigs.size() > 0)) {
			for (NetConfig netConfig : netConfigs) {
				if (netConfig instanceof ModemConfig) {
					isGpsEnabled = ((ModemConfig) netConfig).isGpsEnabled();
					break;
				}
			}
		}
		return isGpsEnabled;
	}
	
 	private void monitor() {
 		HashMap<String, InterfaceState> newInterfaceStatuses = new HashMap<String, InterfaceState>();
		Set<String> keySet = m_modems.keySet();
		Iterator<String> keySetItetrator = keySet.iterator();
		while (keySetItetrator.hasNext()) {
			String usbPort = keySetItetrator.next();
			CellularModem modem = m_modems.get(usbPort);
			
			// get signal strength only if somebody needs it
			if ((m_listeners != null) && (m_listeners.size() > 0)) {
				for (ModemMonitorListener listener : m_listeners) {
					try {
						int rssi = modem.getSignalStrength();
						listener.setCellularSignalLevel(rssi);
					} catch (KuraException e) {
						listener.setCellularSignalLevel(0);
						e.printStackTrace();
					}
				}
			}
			
			NetInterfaceStatus netInterfaceStatus = getNetInterfaceStatus(modem.getConfiguration());
			if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
				try {
					String ifaceName = m_networkService.getModemPppPort(modem.getModemDevice());
					if (ifaceName != null) {
						IModemLinkService pppService = PppFactory.obtainPppService(ifaceName, modem.getDataPort());
						PppState pppState = pppService.getPppState();
						if (pppState == PppState.NOT_CONNECTED) {
							if (modem.getTechnologyType() == ModemTechnologyType.HSDPA) {
								if(((HspaCellularModem)modem).isSimCardReady()) {
									s_logger.info("!!! SIM CARD IS READY !!! connecting ...");
									pppService.connect();
								}
							} else {
								s_logger.info("connecting ...");
								pppService.connect();
							}
						}
						ConnectionInfo connInfo = new ConnectionInfoImpl(ifaceName);
						InterfaceState interfaceState = new InterfaceState(ifaceName, 
								LinuxNetworkUtil.isUp(ifaceName), 
								pppState == PppState.CONNECTED, 
								connInfo.getIpAddress());
						newInterfaceStatuses.put(ifaceName, interfaceState);
					}
				} catch (KuraException e) {
					e.printStackTrace();
				}
			}
		}
		
		// post event for any status changes
		checkStatusChange(m_interfaceStatuses, newInterfaceStatuses);
		m_interfaceStatuses = newInterfaceStatuses;
 	}
 	
    private void checkStatusChange(Map<String, InterfaceState> oldStatuses, Map<String, InterfaceState> newStatuses) {
		
		if (newStatuses != null) {
	        // post NetworkStatusChangeEvent on current and new interfaces
			for(String interfaceName : newStatuses.keySet()) {
				if ((oldStatuses != null) && oldStatuses.containsKey(interfaceName)) {
					if (!newStatuses.get(interfaceName).equals(oldStatuses.get(interfaceName))) {
						s_logger.debug("Posting NetworkStatusChangeEvent on interface: " + interfaceName);
						m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, newStatuses.get(interfaceName), null));
					}
				} else {
					s_logger.debug("Posting NetworkStatusChangeEvent on enabled interface: " + interfaceName);
					m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, newStatuses.get(interfaceName), null));
				}
			}
	        
	        // post NetworkStatusChangeEvent on interfaces that are no longer there
	        if (oldStatuses != null) {
	        	for(String interfaceName : oldStatuses.keySet()) {
                    if(!newStatuses.containsKey(interfaceName)) {
                        s_logger.debug("Posting NetworkStatusChangeEvent on disabled interface: " + interfaceName);
                        m_eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, oldStatuses.get(interfaceName), null));
                    }
                }
	        }
		}
	}
    
	private void trackModem(ModemDevice modemDevice) {
		
		Class<? extends CellularModemFactory> modemFactoryClass = null;
		
		if (modemDevice instanceof UsbModemDevice) {
			SupportedUsbModemInfo supportedUsbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice)modemDevice);
			UsbModemFactoryInfo usbModemFactoryInfo = SupportedUsbModemsFactoryInfo.getModem(supportedUsbModemInfo);
			modemFactoryClass = usbModemFactoryInfo.getModemFactoryClass();
		} else if (modemDevice instanceof SerialModemDevice) {
			SupportedSerialModemInfo supportedSerialModemInfo = SupportedSerialModemsInfo.getModem();
			SerialModemFactoryInfo serialModemFactoryInfo = SupportedSerialModemsFactoryInfo.getModem(supportedSerialModemInfo);
			modemFactoryClass = serialModemFactoryInfo.getModemFactoryClass();
		}
		
		if (modemFactoryClass != null) {
			CellularModemFactory modemFactoryService = null;
			try {
				try {
					Method getInstanceMethod = modemFactoryClass.getDeclaredMethod("getInstance", (Class<?>[]) null);
					getInstanceMethod.setAccessible(true);
					modemFactoryService = (CellularModemFactory) getInstanceMethod.invoke(null, (Object[]) null);
				} catch (Exception e) {
					s_logger.error("Error calling getInstance() method on " + modemFactoryClass.getName() + e);
				}
				
				// if unsuccessful in calling getInstance()
				if (modemFactoryService == null) {
					modemFactoryService = (CellularModemFactory) modemFactoryClass.newInstance();
				}
				
				String platform = null;
				if(m_systemService != null) {
					platform = m_systemService.getPlatform();
				}
				CellularModem modem = modemFactoryService.obtainCellularModemService(modemDevice, platform);
				
				String ifaceName = m_networkService.getModemPppPort(modemDevice);
				List<NetConfig> netConfigs = null;
				if (ifaceName != null) {
					netConfigs = m_networkAdminService.getNetworkInterfaceConfigs(ifaceName);
					if ((netConfigs != null) && (netConfigs.size() > 0)) {
						modem.setConfiguration(netConfigs);
					}
				}
				
				if (modem instanceof EvdoCellularModem) {
					NetInterfaceStatus netIfaceStatus = getNetInterfaceStatus(netConfigs);
					if (netIfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
						if (!((EvdoCellularModem) modem).isProvisioned()) {
							s_logger.info("trackModem() :: The " + modem.getModel() + " is not provisioned, will try to provision it ...");
							if ((task != null) && !task.isCancelled()) {
								s_logger.info("trackModem() :: Cancelling monitor task");
								stopThread = true;
								task.cancel(true);
								task = null;
							}
							((EvdoCellularModem) modem).provision();
							if (task == null) {
								s_logger.info("trackModem() :: Restarting monitor task");
								stopThread = false;
								task = m_executor.submit(new Runnable() {
						    		@Override
						    		public void run() {
						    			while (!stopThread) {
						    				Thread.currentThread().setName("ModemMonitor");
						    				monitor();
						    			}
						    	}});
							}
						} else {
							s_logger.info("trackModem() :: The " + modem.getModel() + " is provisioned");
						}
					}
				}
				
				if (m_gpsSupported == null) {
					boolean gpsSupported = modem.isGpsSupported();
					m_gpsSupported = gpsSupported;
				}
				s_logger.debug("trackModem() :: gpsSupported={}", m_gpsSupported);
				if (m_gpsSupported) {
					if (isGpsEnabledInConfig(netConfigs)) {
						s_logger.debug("trackModem() :: enabling GPS");
						modem.enableGps();
					} else {
						s_logger.debug("trackModem() :: disabling GPS");
						modem.disableGps();
					}
				}
				
				if (modemDevice instanceof UsbModemDevice) {
					m_modems.put(((UsbModemDevice)modemDevice).getUsbPort(), modem);
				} else if (modemDevice instanceof SerialModemDevice) {
					m_modems.put(modemDevice.getProductName(), modem);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
