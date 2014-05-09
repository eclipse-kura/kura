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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.LoopbackInterfaceConfigImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceImpl;
import org.eclipse.kura.core.util.ExecutorUtil;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.LoopbackInterface;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.modem.CellularModem;
import org.eclipse.kura.net.admin.modem.ModemManagerService;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.visitor.linux.LinuxReadVisitor;
import org.eclipse.kura.net.admin.visitor.linux.LinuxWriteVisitor;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfigurationServiceImpl implements NetworkConfigurationService, SelfConfiguringComponent, EventHandler {
    
    public static final String UNCONFIGURED_MODEM_REGEX = "^\\d+-\\d+(\\.\\d+)?$";

    private static final Logger s_logger = LoggerFactory.getLogger(NetworkConfigurationServiceImpl.class);
        
    private static Object lock = new Object();
    
	private final static String[] EVENT_TOPICS = {
			DeploymentAgentService.EVENT_INSTALLED_TOPIC,
			DeploymentAgentService.EVENT_UNINSTALLED_TOPIC };
    
    private NetworkService m_networkService;
    private EventAdmin m_eventAdmin;
    private UsbService m_usbService;
    private ModemManagerService m_modemManagerService;
    
    private List<NetworkConfigurationVisitor> m_readVisitors;
    private List<NetworkConfigurationVisitor> m_writeVisitors;
    
    private ExecutorUtil m_executorUtil;
    private boolean m_firstConfig = true;
    
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
    
    public void setUsbService(UsbService usbService) {
        m_usbService = usbService;
    }
    
    public void unsetUsbService(UsbService usbService) {
        m_usbService = null;
    }
    
    public void setModemManagerService(ModemManagerService modemManagerService) {
    	s_logger.debug("Set the modem manager service");
		m_modemManagerService = modemManagerService;
	}
	
	public void unsetModemManagerService(ModemManagerService modemManagerService) {
    	s_logger.debug("Unset the modem manager service");
		modemManagerService = null;
	}
    
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	/* Do not have a default activate for this self configuring component because we are not using it at startup
    protected void activate(ComponentContext componentContext) {}
    */
    
    protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
    {
        s_logger.debug("activate(componentContext, properties)...");
        
        m_readVisitors = new ArrayList<NetworkConfigurationVisitor>();
        m_readVisitors.add(LinuxReadVisitor.getInstance());

        m_writeVisitors = new ArrayList<NetworkConfigurationVisitor>();
        m_writeVisitors.add(LinuxWriteVisitor.getInstance());
        
        // we are intentionally ignoring the properties from ConfigAdmin at startup
        if(properties == null) {
        	s_logger.debug("Got null properties...");
        } else {
        	s_logger.debug("Props..." + properties);
        }
        
        Dictionary<String, String[]> d = new Hashtable<String, String[]>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, d);
        
        m_executorUtil = ExecutorUtil.getInstance();
        m_executorUtil.schedule(new Runnable() {
    		@Override
    		public void run() {
    			//make sure we don't miss the setting of firstConfig
    			m_firstConfig = false;
    		}
    	}, /*5*/3, TimeUnit.MINUTES);
    }
    
    protected void deactivate(ComponentContext componentContext) {
        s_logger.debug("deactivate()");
        m_writeVisitors = null;
        m_readVisitors = null;
    }
    
    @Override
	public void handleEvent(Event event) {
		s_logger.debug("handleEvent - topic: " + event.getTopic());
        String topic = event.getTopic();
        if (topic.equals(DeploymentAgentService.EVENT_INSTALLED_TOPIC)) {
        	m_firstConfig = false;
        }
	}
    
    @Override
    public void setNetworkConfiguration(NetworkConfiguration networkConfiguration) throws KuraException {
    	updated(networkConfiguration.getConfigurationProperties());
    }
    
    public void updated(Map<String,Object> properties) {
    	synchronized (lock) {    	
    		//skip the first config
    		if(m_firstConfig) {
    			s_logger.debug("Ignoring first configuration");
    			m_firstConfig = false;
    			return;
    		}
    		
	        try {
	        	if(properties != null) {
	        		s_logger.debug("new properties - updating");
	        		s_logger.debug("modified.interface.names: " + properties.get("modified.interface.names"));
	        		
	        		//dynamically insert the type properties..
	        		Map<String,Object> modifiedProps = new HashMap<String, Object>();
	        		modifiedProps.putAll(properties);
	        		String interfaces = (String) properties.get("net.interfaces");
	    			StringTokenizer st = new StringTokenizer(interfaces, ",");
	    			while(st.hasMoreTokens()) {
	    				String interfaceName = st.nextToken();
	    				StringBuilder sb = new StringBuilder();
	    				sb.append("net.interface.")
	    				.append(interfaceName)
	    				.append(".type");
	    				
	    				NetInterfaceType type = LinuxNetworkUtil.getType(interfaceName);
	                    if(type == NetInterfaceType.UNKNOWN) {
	                        if (interfaceName.matches(UNCONFIGURED_MODEM_REGEX)) {
	                        	// If the interface name is in a form such as "1-3.4" (USB address), assume it is a modem
	                    		type = NetInterfaceType.MODEM;
	                    	} else {
	                    		SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
	                    		if ((serialModemInfo != null) && (serialModemInfo.getModemName().equals(interfaceName))) {
	                    			type = NetInterfaceType.MODEM;
	                    		}
	                    	}
	                    }
	    				modifiedProps.put(sb.toString(), type.toString());
	    			}
	        		
	        		NetworkConfiguration networkConfig = new NetworkConfiguration(modifiedProps);
	        	
	        		for(NetworkConfigurationVisitor visitor : m_writeVisitors) {
	        			networkConfig.accept(visitor);
	        		}
	    
	        		//raise the event because there was a change
					m_eventAdmin.postEvent(new NetworkConfigurationChangeEvent(modifiedProps));
	            } else {
	            	s_logger.debug("properties are null");
	            }
	        } catch (Exception e) {
	            // TODO - would still want an event if partially successful?
	        	s_logger.error("Error updating the configuration", e);
	        }
    	}
    }
    
    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
    	synchronized (lock) {
	        s_logger.debug("getConfiguration()");
	        try {
	            NetworkConfiguration networkConfiguration = getNetworkConfiguration();
	            return new ComponentConfigurationImpl(PID, getDefinition(), networkConfiguration.getConfigurationProperties());
	        } catch (Exception e) {
	            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
	        }
    	}
    }
    
    //@Override
    public NetworkConfiguration getNetworkConfiguration() throws KuraException 
    {	
        try {
        	synchronized (lock) {
	            NetworkConfiguration networkConfiguration = new NetworkConfiguration();
	            
	            // Get the current values
	            List<NetInterface<? extends NetInterfaceAddress>> allNetworkInterfaces = m_networkService.getNetworkInterfaces();
	            Map<String, NetInterface<? extends NetInterfaceAddress>> allNetworkInterfacesMap = new HashMap<String, NetInterface<? extends NetInterfaceAddress>>();
	            for(NetInterface<? extends NetInterfaceAddress> netInterface : allNetworkInterfaces) {
	                allNetworkInterfacesMap.put(netInterface.getName(), netInterface);
	            }
	            
	            List<NetInterface<? extends NetInterfaceAddress>> activeNetworkInterfaces = m_networkService.getActiveNetworkInterfaces();
	            Map<String, NetInterface<? extends NetInterfaceAddress>> activeNetworkInterfacesMap = new HashMap<String, NetInterface<? extends NetInterfaceAddress>>();
	            for(NetInterface<? extends NetInterfaceAddress> netInterface : activeNetworkInterfaces) {
	                activeNetworkInterfacesMap.put(netInterface.getName(), netInterface);
	            }
	            
	            // Create the NetInterfaceConfig objects
	            List<String> interfaceNames = m_networkService.getAllNetworkInterfaceNames();        // TODO - include non-active modem interfaces
	            s_logger.debug("Getting configs for " + interfaceNames.size() + " interfaces");
	    
	            if(interfaceNames != null) {
	                for(String interfaceName : interfaceNames) {
	                    // ignore mon interface
	                    if(interfaceName.startsWith("mon.")) {
	                        continue;
	                    }
	                    
	                    NetInterfaceType type = LinuxNetworkUtil.getType(interfaceName);
	                    if(type == NetInterfaceType.UNKNOWN) {
	                    	if (interfaceName.matches(UNCONFIGURED_MODEM_REGEX)) {
	                    		// If the interface name is in a form such as "1-3.4", assume it is a modem
	                    		type = NetInterfaceType.MODEM;
	                    	} else {
	                    		SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
	                    		if ((serialModemInfo != null) && (serialModemInfo.getModemName().equals(interfaceName))) {
	                    			type = NetInterfaceType.MODEM;
	                    		}
	                    	}
	                    }
	                    s_logger.debug("Getting config for " + interfaceName + ", type: " + type);
	                    
	                    NetInterface<? extends NetInterfaceAddress> activeNetInterface = activeNetworkInterfacesMap.get(interfaceName);                    
	                    switch(type) {
	                    case LOOPBACK:                    	
	                        try {
	                            LoopbackInterfaceConfigImpl loopbackInterfaceConfig = null;
	                        	loopbackInterfaceConfig = buildLoopbackInterfaceConfig(interfaceName, activeNetInterface);
	                        	networkConfiguration.addNetInterfaceConfig(loopbackInterfaceConfig);
	                        }
	                        catch (KuraException e) {
	                        	s_logger.error("Error building LoopbackInterfaceConfig "+interfaceName+". Ignoring it.", e);
	                        }                        
	                        break;
	                        
	                    case ETHERNET:
	                    	try {
	                    		EthernetInterfaceConfigImpl ethernetInterfaceConfig = null;                        
	                    		ethernetInterfaceConfig = buildEthernetInterfaceConfig(interfaceName, activeNetInterface);
	                    		networkConfiguration.addNetInterfaceConfig(ethernetInterfaceConfig);
	                        }
	                        catch (KuraException e) {
	                        	s_logger.error("Error building EthernetInterfaceConfig "+interfaceName+". Ignoring it.", e);
	                        }                        
	                        break;
	                        
	                    case WIFI:
	                    	try {
	                    		WifiInterfaceConfigImpl wifiInterfaceConfig = null;                        
	                    		wifiInterfaceConfig = buildWifiInterfaceConfig(interfaceName, activeNetInterface);                        
	                    		networkConfiguration.addNetInterfaceConfig(wifiInterfaceConfig);
	                        }
	                        catch (KuraException e) {
	                        	s_logger.error("Error building WifiInterfaceConfig "+interfaceName+". Ignoring it.", e);
	                        }                                            		
	                        break;
	                        
	                    case MODEM:
	                    	try {
		                        ModemInterfaceConfigImpl modemInterfaceConfig = null;                        
		                        modemInterfaceConfig = buildModemInterfaceConfig(allNetworkInterfacesMap, 
		                        											     interfaceName, activeNetInterface);
		                        networkConfiguration.addNetInterfaceConfig(modemInterfaceConfig);
	                        }
	                        catch (KuraException e) {
	                        	s_logger.error("Error building ModemInterfaceConfig "+interfaceName+". Ignoring it.", e);
	                        }                                            			                        
	                        break;
	                        
	                    case UNKNOWN:
	                        s_logger.info("Found interface of unknown type in current configuration: {}. Ignoring it.", interfaceName);
	                        break;
	                        
	                    default:
	                        s_logger.info("Unsupported type : " + type + " - not adding to configuration. Ignoring it.");
	                    }
	                }
	            }
	            
	            // populate the NetInterfaceConfigs
	            for(NetworkConfigurationVisitor visitor : m_readVisitors) {
	                networkConfiguration.accept(visitor);
	            }
	  
	/*
	// DEBUG
	s_logger.debug("*************** networkConfiguration.getConfigurationProperties()");
	Map<String, Object> props = networkConfiguration.getConfigurationProperties();
	for(String key : props.keySet()) {
	    System.out.println("*** key: " + key + " === " + props.get(key));
	}
	*/
	            return networkConfiguration;
        	}
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, e.getMessage());
        }
    }

    
    
	private ModemInterfaceConfigImpl buildModemInterfaceConfig(Map<String, NetInterface<? extends NetInterfaceAddress>> allNetworkInterfacesMap,
														       String interfaceName,
														       NetInterface<? extends NetInterfaceAddress> activeNetInterface)
		throws KuraException 
	{
		ModemInterfaceConfigImpl modemInterfaceConfig;
		if(activeNetInterface != null && activeNetInterface instanceof ModemInterfaceImpl) {
		    // Copy current values
		    modemInterfaceConfig = new ModemInterfaceConfigImpl((ModemInterfaceImpl<? extends NetInterfaceAddress>)activeNetInterface);
		    s_logger.debug("getNetworkConfiguration() - new ModemInterfaceConfigImpl() - copying activeNetInterface");
		} else {
		    //modemInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
		    NetInterface<?> netInterface = allNetworkInterfacesMap.get(interfaceName);
		    if(netInterface != null) {
		        modemInterfaceConfig = new ModemInterfaceConfigImpl((ModemInterfaceImpl<? extends NetInterfaceAddress>)netInterface);
		        s_logger.debug("getNetworkConfiguration() - new ModemInterfaceConfigImpl() - copying (inactive) netInterface");
		    } else {
		        modemInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
		        s_logger.debug("getNetworkConfiguration() - new ModemInterfaceConfigImpl() - new instance");
		    }
		}
		
		boolean isIfaceUp = LinuxNetworkUtil.isUp(interfaceName);
		
		if (modemInterfaceConfig.getNetInterfaceAddresses() == null
				|| modemInterfaceConfig.getNetInterfaceAddresses().size() == 0) {

			List<ModemInterfaceAddressConfig> modemInterfaceAddressConfigs = new ArrayList<ModemInterfaceAddressConfig>();
			ModemInterfaceAddressConfigImpl modemInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
			modemInterfaceAddressConfigs.add(modemInterfaceAddressConfig);
			modemInterfaceAddressConfig.setConnectionType(ModemConnectionType.PPP);
			if (isIfaceUp) {
				modemInterfaceAddressConfig.setConnectionStatus(ModemConnectionStatus.CONNECTED);
			} else {
				modemInterfaceAddressConfig.setConnectionStatus(ModemConnectionStatus.DISCONNECTED);
			}
			modemInterfaceConfig.setNetInterfaceAddresses(modemInterfaceAddressConfigs);
		}
		    
		int mtu = -1;
		NetInterfaceState netIfaceState = NetInterfaceState.DISCONNECTED;
		if (isIfaceUp) {
			try {
				mtu = LinuxNetworkUtil.getCurrentMtu(interfaceName);
				netIfaceState = NetInterfaceState.ACTIVATED;
			} catch (Exception e) {
				s_logger.warn("Could not get mtu for interface " + interfaceName);
			}
		}
		
		modemInterfaceConfig.setState(netIfaceState);
		
		modemInterfaceConfig.setMTU(mtu);
		modemInterfaceConfig.setHardwareAddress(LinuxNetworkUtil.getMacAddressBytes(interfaceName));
		modemInterfaceConfig.setLoopback(false);
		modemInterfaceConfig.setPointToPoint(true);
		modemInterfaceConfig.setSupportsMulticast(LinuxNetworkUtil.isSupportsMulticast(interfaceName));
		modemInterfaceConfig.setUp(LinuxNetworkUtil.isUp(interfaceName));

		String modemUsbPort = m_networkService.getModemUsbPort(interfaceName);
		if (modemUsbPort == null) {
			modemUsbPort = interfaceName;
		}
		
		if(m_modemManagerService != null) {
			CellularModem modem = m_modemManagerService.getModemService(modemUsbPort);
			if (modem != null) {
				/*
				s_logger.warn("<DEBUG> Manufacturer: " + modem.getManufacturer());
				s_logger.warn("<DEBUG> Model: " + modem.getModel());
				s_logger.warn("<DEBUG> Service Type: " + modem.getServiceType());
				s_logger.warn("<DEBUG> isReachable? " + modem.isReachable());
				s_logger.warn("<DEBUG> isRoaming? " + modem.isRoaming());
				s_logger.warn("<DEBUG> Signal Strength = " + modem.getSignalStrength());
				s_logger.warn("<DEBUG> Tx Counter = " + modem.getCallTxCounter());
				s_logger.warn("<DEBUG> Rx Counter = "+ modem.getCallRxCounter());
				*/
				List<? extends UsbModemDriver> drivers = null;
				ModemDevice modemDevice = modem.getModemDevice();
				if (modemDevice instanceof UsbModemDevice) {
					UsbModemDevice usbModemDevice = (UsbModemDevice)modemDevice;
					modemInterfaceConfig.setUsbDevice(usbModemDevice);
					drivers = SupportedUsbModemsFactoryInfo.getDeviceDrivers(usbModemDevice.getVendorId(), usbModemDevice.getProductId());
				}
				
				String modemSerialNo = "";
				String modemModel = "";
				String modemRevisionID = "";
				boolean gpsSupported = false; 
				try {
					modemSerialNo = modem.getSerialNumber();
					modemModel = modem.getModel();
					modemRevisionID = modem.getRevisionID();
					gpsSupported = modem.isGpsSupported();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				modemInterfaceConfig.setSerialNumber(modemSerialNo);
				modemInterfaceConfig.setModel(modemModel);
				modemInterfaceConfig.setFirmwareVersion(modemRevisionID);
				modemInterfaceConfig.setGpsSupported(gpsSupported);
				
				if ((drivers != null) && (drivers.size() > 0)) {
					UsbModemDriver driver = drivers.get(0);
					modemInterfaceConfig.setDriver(driver.getName());
				}
				modemInterfaceConfig.setDriverVersion("unknown");
			}
		}

		modemInterfaceConfig.setVirtual(false);
		return modemInterfaceConfig;
	}

	
	
	private WifiInterfaceConfigImpl buildWifiInterfaceConfig(String interfaceName,
															 NetInterface<? extends NetInterfaceAddress> activeNetInterface)
		throws KuraException 
	{
		WifiInterfaceConfigImpl wifiInterfaceConfig;
		if(activeNetInterface != null && activeNetInterface instanceof WifiInterfaceImpl) {
		    // Copy current values
		    wifiInterfaceConfig = new WifiInterfaceConfigImpl((WifiInterfaceImpl<? extends NetInterfaceAddress>)activeNetInterface);
		} else {
		    wifiInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
		}
		
		if(wifiInterfaceConfig.getNetInterfaceAddresses() == null 
		        || wifiInterfaceConfig.getNetInterfaceAddresses().size() == 0) {

		    List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = new ArrayList<WifiInterfaceAddressConfig>();
		    wifiInterfaceAddressConfigs.add(new WifiInterfaceAddressConfigImpl());
		    wifiInterfaceConfig.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));
		    wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);
		    s_logger.debug("MAC for " + interfaceName + ": " + LinuxNetworkUtil.getMacAddress(interfaceName));
		    wifiInterfaceConfig.setHardwareAddress(LinuxNetworkUtil.getMacAddressBytes(interfaceName));
		    wifiInterfaceConfig.setLoopback(false);
		    wifiInterfaceConfig.setMTU(LinuxNetworkUtil.getCurrentMtu(interfaceName));
		    wifiInterfaceConfig.setPointToPoint(false);
		    wifiInterfaceConfig.setSupportsMulticast(LinuxNetworkUtil.isSupportsMulticast(interfaceName));
		    wifiInterfaceConfig.setUp(LinuxNetworkUtil.isUp(interfaceName));
		    wifiInterfaceConfig.setCapabilities(LinuxNetworkUtil.getWifiCapabilities(interfaceName));
		    
		    //FIXME
		    wifiInterfaceConfig.setDriver("unknown");
		    wifiInterfaceConfig.setDriverVersion("unknown");
		    wifiInterfaceConfig.setFirmwareVersion("unknown");
		    wifiInterfaceConfig.setVirtual(false);
		    /*
		    wifiInterfaceConfig.setAutoConnect();
		    wifiInterfaceConfig.setState(???);
		    wifiInterfaceConfig.setUsbDevice(???);
		    */
		}
		return wifiInterfaceConfig;
	}

	
	
	private EthernetInterfaceConfigImpl buildEthernetInterfaceConfig(String interfaceName,
																	 NetInterface<? extends NetInterfaceAddress> activeNetInterface)
		throws KuraException 
	{
		EthernetInterfaceConfigImpl ethernetInterfaceConfig;
		if(activeNetInterface != null && activeNetInterface instanceof EthernetInterfaceImpl) {
		    // Copy current values
		    ethernetInterfaceConfig = new EthernetInterfaceConfigImpl((EthernetInterfaceImpl<? extends NetInterfaceAddress>)activeNetInterface);
		    s_logger.debug("Active something? " + interfaceName + " with " + activeNetInterface);
		} else {
		    ethernetInterfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
		}
		
		if(ethernetInterfaceConfig.getNetInterfaceAddresses() == null 
		        || ethernetInterfaceConfig.getNetInterfaceAddresses().size() == 0) {

		    List<NetInterfaceAddressConfig> ethernetInterfaceAddressConfigs = new ArrayList<NetInterfaceAddressConfig>();
		    ethernetInterfaceAddressConfigs.add(new NetInterfaceAddressConfigImpl());
		    ethernetInterfaceConfig.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));
		    ethernetInterfaceConfig.setNetInterfaceAddresses(ethernetInterfaceAddressConfigs);
		    s_logger.debug("MAC for " + interfaceName + ": " + LinuxNetworkUtil.getMacAddress(interfaceName));
		    ethernetInterfaceConfig.setHardwareAddress(LinuxNetworkUtil.getMacAddressBytes(interfaceName));
		    ethernetInterfaceConfig.setLinkUp(LinuxNetworkUtil.isLinkUp(NetInterfaceType.ETHERNET, interfaceName));
		    ethernetInterfaceConfig.setLoopback(false);
		    ethernetInterfaceConfig.setMTU(LinuxNetworkUtil.getCurrentMtu(interfaceName));
		    ethernetInterfaceConfig.setPointToPoint(false);
		    ethernetInterfaceConfig.setSupportsMulticast(LinuxNetworkUtil.isSupportsMulticast(interfaceName));
		    ethernetInterfaceConfig.setUp(LinuxNetworkUtil.isUp(interfaceName));

		    //FIXME
		    
		    ethernetInterfaceConfig.setVirtual(false);
		    /*
		    ethernetInterfaceConfig.setAutoConnect();
		    ethernetInterfaceConfig.setState(???);
		    ethernetInterfaceConfig.setUsbDevice(???);
		    */
		}
		Map<String, String> driver = LinuxNetworkUtil.getEthernetDriver(interfaceName);
	    ethernetInterfaceConfig.setDriver(driver.get("name"));
	    ethernetInterfaceConfig.setDriverVersion(driver.get("version"));
	    ethernetInterfaceConfig.setFirmwareVersion(driver.get("firmware"));
	    
	    if (LinuxNetworkUtil.isUp(interfaceName)) {
	    	ethernetInterfaceConfig.setState(NetInterfaceState.ACTIVATED);
	    }
	    else {
	    	ethernetInterfaceConfig.setState(NetInterfaceState.DISCONNECTED);
	    }
	    
	    s_logger.debug("Interface State: " + ethernetInterfaceConfig.getState());
	    
		return ethernetInterfaceConfig;
	}

	
	
	private LoopbackInterfaceConfigImpl buildLoopbackInterfaceConfig(String interfaceName,
																	 NetInterface<? extends NetInterfaceAddress> activeNetInterface)
		throws KuraException 
	{
		LoopbackInterfaceConfigImpl loopbackInterfaceConfig;
		if(activeNetInterface != null && activeNetInterface instanceof LoopbackInterface) {
		    // Copy current values
		    loopbackInterfaceConfig = new LoopbackInterfaceConfigImpl((LoopbackInterface<? extends NetInterfaceAddress>)activeNetInterface);
		} else {
		    loopbackInterfaceConfig = new LoopbackInterfaceConfigImpl(interfaceName);
		}
		
		if(loopbackInterfaceConfig.getNetInterfaceAddresses() == null 
		        || loopbackInterfaceConfig.getNetInterfaceAddresses().size() == 0) {

		    List<NetInterfaceAddressConfig> loopbackInterfaceAddressConfigs = new ArrayList<NetInterfaceAddressConfig>();                            
		    loopbackInterfaceAddressConfigs.add(new NetInterfaceAddressConfigImpl());
		    loopbackInterfaceConfig.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));
		    loopbackInterfaceConfig.setNetInterfaceAddresses(loopbackInterfaceAddressConfigs);
		    s_logger.debug("MAC for " + interfaceName + ": " + LinuxNetworkUtil.getMacAddress(interfaceName));
		    loopbackInterfaceConfig.setHardwareAddress(LinuxNetworkUtil.getMacAddressBytes(interfaceName));
		    loopbackInterfaceConfig.setLoopback(true);
		    loopbackInterfaceConfig.setMTU(LinuxNetworkUtil.getCurrentMtu(interfaceName));
		    loopbackInterfaceConfig.setPointToPoint(false);
		    loopbackInterfaceConfig.setSupportsMulticast(LinuxNetworkUtil.isSupportsMulticast(interfaceName));
		    loopbackInterfaceConfig.setUp(LinuxNetworkUtil.isUp(interfaceName));

		    //FIXME
		    loopbackInterfaceConfig.setDriver("unknown");
		    loopbackInterfaceConfig.setDriverVersion("unknown");
		    loopbackInterfaceConfig.setFirmwareVersion("unknown");
		    loopbackInterfaceConfig.setVirtual(false);
		    /*
		    loopbackInterfaceConfig.setAutoConnect();
		    loopbackInterfaceConfig.setState(???);
		    loopbackInterfaceConfig.setUsbDevice(???);
		    */
		    
		}
		return loopbackInterfaceConfig;
	}
    
    
	private Tocd getDefinition() throws KuraException {
		ObjectFactory objectFactory = new ObjectFactory();
		Tocd tocd = objectFactory.createTocd();
		
		tocd.setName("NetworkConfigurationService");
		tocd.setId("org.eclipse.kura.net.admin.NetworkConfigurationService");
		tocd.setDescription("Network Configuration Service");
		
		//get the USB network interfaces (if any)
		List<UsbNetDevice> usbNetDevices = m_usbService.getUsbNetDevices();
		
		Tad tad = objectFactory.createTad();
		tad.setId("net.interfaces");
		tad.setName("net.interfaces");
		tad.setType(Tscalar.STRING);
		tad.setCardinality(10000);
		tad.setRequired(true);
		tad.setDefault("");
		tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.PLATFORM_INTERFACES));
		tocd.addAD(tad);

		//Get the network interfaces on the platform
		try {
			List<String> networkInterfaceNames = LinuxNetworkUtil.getAllInterfaceNames();
			for(String ifaceName : networkInterfaceNames) {
				//get the current configuration for this interface
				NetInterfaceType type = LinuxNetworkUtil.getType(ifaceName);

				String prefix = "net.interface.";
				
				if(type == NetInterfaceType.LOOPBACK) {
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.mtu")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.mtu")).toString());
					tad.setType(Tscalar.INTEGER);
					tad.setCardinality(0);
					tad.setRequired(true);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_MTU));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.autoconnect")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.autoconnect")).toString());
					tad.setType(Tscalar.BOOLEAN);
					tad.setCardinality(0);
					tad.setRequired(true);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_AUTOCONNECT));
					tocd.addAD(tad);
                    
                    tad = objectFactory.createTad();
                    tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.driver")).toString());
                    tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.driver")).toString());
                    tad.setType(Tscalar.STRING);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_DRIVER));
                    tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.address")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.address")).toString());
					tad.setType(Tscalar.STRING);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_ADDRESS));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.prefix")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.prefix")).toString());
					tad.setType(Tscalar.SHORT);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_PREFIX));
					tocd.addAD(tad);
				} else if(type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI) {
					if(usbNetDevices != null) {
						for(UsbNetDevice usbNetDevice : usbNetDevices) {
							if(usbNetDevice.getInterfaceName().equals(ifaceName)) {
								//found a match - add the read only fields?
								tad = objectFactory.createTad();
								tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".usb.port")).toString());
								tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".usb.port")).toString());
								tad.setType(Tscalar.STRING);
								tad.setCardinality(0);
								tad.setRequired(false);
								tad.setDefault("");
								tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_PORT));
								tocd.addAD(tad);
								
								tad = objectFactory.createTad();
								tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".usb.manufacturer")).toString());
								tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".usb.manfacturer")).toString());
								tad.setType(Tscalar.STRING);
								tad.setCardinality(0);
								tad.setRequired(false);
								tad.setDefault("");
								tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_MANUFACTURER));
								tocd.addAD(tad);
								
								tad = objectFactory.createTad();
								tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".usb.product")).toString());
								tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".usb.product")).toString());
								tad.setType(Tscalar.STRING);
								tad.setCardinality(0);
								tad.setRequired(false);
								tad.setDefault("");
								tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_PRODUCT));
								tocd.addAD(tad);
								
								tad = objectFactory.createTad();
								tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".usb.manufacturer.id")).toString());
								tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".usb.manfacturer.id")).toString());
								tad.setType(Tscalar.STRING);
								tad.setCardinality(0);
								tad.setRequired(false);
								tad.setDefault("");
								tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_MANUFACTURER_ID));
								tocd.addAD(tad);
								
								tad = objectFactory.createTad();
								tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".usb.product.id")).toString());
								tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".usb.product.id")).toString());
								tad.setType(Tscalar.STRING);
								tad.setCardinality(0);
								tad.setRequired(false);
								tad.setDefault("");
								tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_PRODUCT_ID));
								tocd.addAD(tad);
								
								//no need to continue
								break;
							}
						}
					}
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.mtu")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.mtu")).toString());
					tad.setType(Tscalar.INTEGER);
					tad.setCardinality(0);
					tad.setRequired(true);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_MTU));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.autoconnect")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.autoconnect")).toString());
					tad.setType(Tscalar.BOOLEAN);
					tad.setCardinality(0);
					tad.setRequired(true);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_AUTOCONNECT));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpClient4.enabled")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpClient4.enabled")).toString());
					tad.setType(Tscalar.BOOLEAN);
					tad.setCardinality(0);
					tad.setRequired(true);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_CLIENT_ENABLED));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.address")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.address")).toString());
					tad.setType(Tscalar.STRING);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_ADDRESS));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.prefix")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.prefix")).toString());
					tad.setType(Tscalar.SHORT);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_PREFIX));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.gateway")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.gateway")).toString());
					tad.setType(Tscalar.STRING);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_GATEWAY));
					tocd.addAD(tad);
					
					//DNS and WINS
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.dnsServers")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.dnsServers")).toString());
					tad.setType(Tscalar.STRING);
					tad.setCardinality(10000);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_DNS_SERVERS));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.winsServers")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.winsServers")).toString());
					tad.setType(Tscalar.STRING);
					tad.setCardinality(10000);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WINS_SERVERS));
					tocd.addAD(tad);					
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.enabled")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.enabled")).toString());
					tad.setType(Tscalar.BOOLEAN);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_ENABLED));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.defaultLeaseTime")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.defaultLeaseTime")).toString());
					tad.setType(Tscalar.INTEGER);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_DEFAULT_LEASE_TIME));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.maxLeaseTime")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.maxLeaseTime")).toString());
					tad.setType(Tscalar.INTEGER);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_MAX_LEASE_TIME));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.prefix")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.prefix")).toString());
					tad.setType(Tscalar.SHORT);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_PREFIX));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.rangeStart")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.rangeStart")).toString());
					tad.setType(Tscalar.STRING);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_RANGE_START));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.rangeEnd")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.rangeEnd")).toString());
					tad.setType(Tscalar.STRING);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_RANGE_END));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.passDns")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.passDns")).toString());
					tad.setType(Tscalar.BOOLEAN);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_PASS_DNS));
					tocd.addAD(tad);
					
					tad = objectFactory.createTad();
					tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.nat.enabled")).toString());
					tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.nat.enabled")).toString());
					tad.setType(Tscalar.BOOLEAN);
					tad.setCardinality(0);
					tad.setRequired(false);
					tad.setDefault("");
					tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_NAT_ENABLED));
					tocd.addAD(tad);
					
					if(type == NetInterfaceType.WIFI) {
					    // Common
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".wifi.capabilities")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".wifi.capabilities")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.WIFI_CAPABILITIES));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.mode")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.mode")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MODE));
                        tocd.addAD(tad);

                        // INFRA
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.ssid")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.ssid")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_SSID));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.hardwareMode")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.hardwareMode")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_HARDWARE_MODE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.radioMode")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.radioMode")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_HARDWARE_MODE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.securityType")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.securityType")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_SECURITY_TYPE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.passphrase")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.passphrase")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_PASSPHRASE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.pairwiseCiphers")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.pairwiseCiphers")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.groupCiphers")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.groupCiphers")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_GROUP_CIPHERS));
                        tocd.addAD(tad);
                        
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.channel")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.channel")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_CHANNEL));
                        tocd.addAD(tad);
                        
                        // MASTER
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.ssid")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.ssid")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_SSID));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.broadcast")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.broadcast")).toString());
                        tad.setType(Tscalar.BOOLEAN);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_BROADCAST_ENABLED));
                        tocd.addAD(tad);
                          
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.hardwareMode")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.hardwareMode")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_HARDWARE_MODE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.radioMode")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.radioMode")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_HARDWARE_MODE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.securityType")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.securityType")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_SECURITY_TYPE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.passphrase")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.passphrase")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_PASSPHRASE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.channel")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.channel")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_CHANNEL));
                        tocd.addAD(tad);
                        
                        /*
                        // ADHOC
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.ssid")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.ssid")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_ADHOC_SSID));
                        tocd.addAD(tad);
                                                  
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.hardwareMode")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.hardwareMode")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_ADHOC_HARDWARE_MODE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.radioMode")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.radioMode")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_ADHOC_HARDWARE_MODE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.securityType")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.securityType")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_ADHOC_SECURITY_TYPE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.passphrase")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.passphrase")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_ADHOC_PASSPHRASE));
                        tocd.addAD(tad);
                        
                        tad = objectFactory.createTad();
                        tad.setId((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.channel")).toString());
                        tad.setName((new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.adhoc.channel")).toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_ADHOC_CHANNEL));
                        tocd.addAD(tad);
                        */
					}
					
					//TODO - deal with USB devices (READ ONLY)
				}
			}
		} catch(Exception e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
		}
				
		return tocd;
	}
}
