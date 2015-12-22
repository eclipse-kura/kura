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
package org.eclipse.kura.linux.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.LoopbackInterfaceImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.core.net.WifiAccessPointImpl;
import org.eclipse.kura.core.net.WifiInterfaceAddressImpl;
import org.eclipse.kura.core.net.WifiInterfaceImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.modem.ModemDriver;
import org.eclipse.kura.linux.net.modem.SerialModemAddedEvent;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.linux.net.util.IScanTool;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.linux.net.util.LinuxIfconfig;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.util.ScanTool;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.NetworkState;
import org.eclipse.kura.net.modem.ModemAddedEvent;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemRemovedEvent;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceEvent;
import org.eclipse.kura.usb.UsbDeviceRemovedEvent;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkServiceImpl implements NetworkService, EventHandler {

	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");
    public static final String PPP_PEERS_DIR = "/etc/ppp/peers/";

    private static final Logger s_logger = LoggerFactory.getLogger(NetworkServiceImpl.class);
	
	private static final String UNCONFIGURED_MODEM_REGEX = "^\\d+-\\d+(\\.\\d+)?$";
	
    private static final String[] EVENT_TOPICS = new String[] {
        UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC,
        UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC,
        SerialModemAddedEvent.SERIAL_MODEM_EVENT_ADDED_TOPIC
    };

    private static final String TOOGLE_MODEM_THREAD_NAME = "ToggleModem";
    private static final long TOOGLE_MODEM_THREAD_INTERVAL = 10000; // in msec
    private static final long TOOGLE_MODEM_THREAD_TERMINATION_TOUT = 1; // in sec
    private static final long TOOGLE_MODEM_THREAD_EXECUTION_DELAY = 2; // in min
    
    private ComponentContext      m_ctx;
    
    private EventAdmin m_eventAdmin;
    private UsbService m_usbService;
    
    private Map<String, UsbModemDevice> m_usbModems;
    private SerialModemDevice m_serialModem;
    
    private List<String> m_addedModems;	
    
    private ScheduledExecutorService m_executor;
    
    private static ScheduledFuture<?>  s_task;
    private static AtomicBoolean s_stopThread;
    
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------
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
		
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext) {
        // save the bundle context
        m_ctx = componentContext;
        
        s_stopThread = new AtomicBoolean();
        m_usbModems = new HashMap<String, UsbModemDevice>();
        m_addedModems = new ArrayList<String>();
        
        Dictionary<String, String[]> d = new Hashtable<String, String[]>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        m_ctx.getBundleContext().registerService(EventHandler.class.getName(), this, d);
        
        // Add serial modem if any
        SupportedSerialModemsInfo.getModem();
        
        // Add tty devices
        List<UsbTtyDevice> ttyDevices = m_usbService.getUsbTtyDevices();
        if(ttyDevices != null && !ttyDevices.isEmpty()) {
        	s_logger.debug("activate() :: Total tty devices reported by UsbService: {}", ttyDevices.size());
            for(UsbTtyDevice device : ttyDevices) {
                if(SupportedUsbModemsInfo.isSupported(device.getVendorId(), device.getProductId())) {
                    UsbModemDevice usbModem = null;

                    //found one - see if we have some info for it
                    if(m_usbModems.get(device.getUsbPort()) == null) {
                        usbModem = new UsbModemDevice(device);
                    } else {
                        usbModem = m_usbModems.get(device.getUsbPort());
                    }
                    usbModem.addTtyDev(device.getDeviceNode());
                    s_logger.debug("activate() :: Adding tty resource: {} for {}", device.getDeviceNode(), device.getUsbPort());
                    m_usbModems.put(device.getUsbPort(), usbModem);
                }
            }
        }
            
        // Add block devices
        List<UsbBlockDevice> blockDevices = m_usbService.getUsbBlockDevices();
        if(blockDevices != null && !blockDevices.isEmpty()) {
        	s_logger.debug("activate() :: Total block devices reported by UsbService: {}", blockDevices.size());
            for(UsbBlockDevice device : blockDevices) {
                if(SupportedUsbModemsInfo.isSupported(device.getVendorId(), device.getProductId())) {
                    UsbModemDevice usbModem = null;

                    //found one - see if we have some info for it
                    if(m_usbModems.get(device.getUsbPort()) == null) {
                        usbModem = new UsbModemDevice(device);
                    } else {
                        usbModem = m_usbModems.get(device.getUsbPort());
                    }
                    usbModem.addBlockDev(device.getDeviceNode());
                    s_logger.debug("activate() :: Adding block resource: {} for {}", device.getDeviceNode(), device.getUsbPort());
                    m_usbModems.put(device.getUsbPort(), usbModem);
                }
            }
        }
        
        //At this point, we should have some modems - display them
        Iterator<Entry<String, UsbModemDevice>> it = m_usbModems.entrySet().iterator();
        while(it.hasNext()) {
            final UsbModemDevice usbModem = it.next().getValue();
            final SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbModem.getVendorId(), usbModem.getProductId());
            
            s_logger.debug("activate() :: Found modem: {}", usbModem);
            
            // Check for correct number of resources
            if (modemInfo != null) {
            	s_logger.debug("activate() :: usbModem.getTtyDevs().size()={}, modemInfo.getNumTtyDevs()={}",
                		usbModem.getTtyDevs().size(), modemInfo.getNumTtyDevs());
                s_logger.debug("activate() :: usbModem.getBlockDevs().size()={}, modemInfo.getNumBlockDevs()={}",
                		usbModem.getBlockDevs().size(), modemInfo.getNumBlockDevs());
                
	            if ((usbModem.getTtyDevs().size() == modemInfo.getNumTtyDevs())
						&& (usbModem.getBlockDevs().size() == modemInfo.getNumBlockDevs())) {
	            	s_logger.info("activate () :: posting ModemAddedEvent ... {}", usbModem);
	                m_eventAdmin.postEvent(new ModemAddedEvent(usbModem));
	                m_addedModems.add(usbModem.getUsbPort());
	            } else {
	            	s_logger.warn("activate() :: modem doesn't have correct number of resources, will try to toggle it ...");
	            	m_executor = Executors.newSingleThreadScheduledExecutor();
	            	s_logger.info("activate() :: scheduling {} thread in {} minutes ..", TOOGLE_MODEM_THREAD_NAME, TOOGLE_MODEM_THREAD_EXECUTION_DELAY);
	            	s_stopThread.set(false);
	            	s_task = m_executor.schedule(new Runnable() {
	            		@Override
	            		public void run() {
	            			Thread.currentThread().setName(TOOGLE_MODEM_THREAD_NAME);
	            			try {
			            		toggleModem(modemInfo);
	            			} catch (InterruptedException interruptedException) {
			    				Thread.interrupted();
								s_logger.debug("activate() :: modem monitor interrupted - {}", interruptedException);
							} catch (Throwable t) {
								s_logger.error("activate() :: Exception while monitoring cellular connection {}", t);
							}
	            		}
	            	}, TOOGLE_MODEM_THREAD_EXECUTION_DELAY, TimeUnit.MINUTES);
	            }
            }
        }
	}	
	
	protected void deactivate(ComponentContext componentContext) {
		if ((s_task != null) && (!s_task.isDone())) {
        	s_stopThread.set(true);
        	toggleModemNotity();
    		s_logger.debug("deactivate() :: Cancelling {} task ...", TOOGLE_MODEM_THREAD_NAME);
    		s_task.cancel(true);
    		s_logger.info("deactivate() :: {} task cancelled? = {}", TOOGLE_MODEM_THREAD_NAME, s_task.isDone());
    		s_task = null;
    	}
    	
    	if (m_executor != null) {
    		s_logger.debug("deactivate() :: Terminating {} Thread ...", TOOGLE_MODEM_THREAD_NAME);
    		m_executor.shutdownNow();
    		try {
				m_executor.awaitTermination(TOOGLE_MODEM_THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
    		s_logger.info("deactivate() :: {} Thread terminated? - {}", TOOGLE_MODEM_THREAD_NAME, m_executor.isTerminated());
			m_executor = null;
    	}
    	s_stopThread = null;
	    m_usbModems = null;
        m_ctx = null;
	}
	
	@Override
	public NetworkState getState() throws KuraException {
		//FIXME - this method needs some work
		
		//see if we have global access by trying to ping - maybe there is a better way?
		if(LinuxNetworkUtil.canPing("8.8.8.8", 1)) {
			return NetworkState.CONNECTED_GLOBAL;
		}
		if(LinuxNetworkUtil.canPing("8.8.4.4", 1)) {
			return NetworkState.CONNECTED_GLOBAL;
		}
		
		//if we have a link we at least of network local access
		List<NetInterface<? extends NetInterfaceAddress>> netInterfaces = getNetworkInterfaces();
		for(NetInterface<? extends NetInterfaceAddress> netInterface : netInterfaces) {
			if(netInterface.getType() == NetInterfaceType.ETHERNET) {
				if(((EthernetInterfaceImpl<? extends NetInterfaceAddress>)netInterface).isLinkUp()) {
					return NetworkState.CONNECTED_SITE;
				}
			}
		}
		
		//TODO - should be know if we are CONNECTED_SITE for wifi?
		
		LoopbackInterfaceImpl<? extends NetInterfaceAddress> netInterface = (LoopbackInterfaceImpl<? extends NetInterfaceAddress>) getNetworkInterface("lo");
		if(netInterface.isUp()) {
			return NetworkState.CONNECTED_LOCAL;
		}
		
		//not sure what we're doing...
		return NetworkState.UNKNOWN;
	}
	
	@Override
	public NetInterfaceState getState(String interfaceName) throws KuraException {
		NetInterface<? extends NetInterfaceAddress> netInterface = getNetworkInterface(interfaceName);
		if(netInterface == null) {
			s_logger.error("There is no status available for network interface {}", interfaceName);
			return NetInterfaceState.UNKNOWN;
		} else {
			return netInterface.getState();
		}
	}

	@Override
	public List<String> getAllNetworkInterfaceNames() throws KuraException {
	    ArrayList<String> interfaceNames = new ArrayList<String>();
	    List<String> allInterfaceNames = LinuxNetworkUtil.getAllInterfaceNames();
	    if(allInterfaceNames != null) {
	        interfaceNames.addAll(allInterfaceNames);
	    }
	    
        // include non-connected ppp interfaces and usb port numbers for non-configured modems
	    Iterator<String> it = m_addedModems.iterator();
	    while(it.hasNext()) {
	        String modemId = it.next();
	        UsbModemDevice usbModem = m_usbModems.get(modemId);
	        String pppPort = null;
	        if (usbModem != null) {
		        pppPort = getModemPppPort(usbModem);
	        } else {
	        	// for Serial modem
		    	if (m_serialModem != null) {
		    		pppPort = getModemPppPort(m_serialModem);
		    	}
	        }
	        
	        if(pppPort != null) {
                if(!interfaceNames.contains(pppPort)) {
                    interfaceNames.add(pppPort);
                }
            } else {
                // add the usb port as an interface if there isn't already a ppp interface associated with this port
                interfaceNames.add(modemId);
            }
	    }
	    
		return interfaceNames; 
	}

	@Override
	public List<NetInterface<? extends NetInterfaceAddress>> getNetworkInterfaces() throws KuraException {
		s_logger.trace("getNetworkInterfaces()");
		List<NetInterface<? extends NetInterfaceAddress>> netInterfaces = new ArrayList<NetInterface<? extends NetInterfaceAddress>>();
		
		List<String> interfaceNames = getAllNetworkInterfaceNames();
		for(String interfaceName : interfaceNames) {
			try {
				NetInterface<? extends NetInterfaceAddress> netInterface = getNetworkInterface(interfaceName);
				if(netInterface != null) {
					netInterfaces.add(netInterface);
				}
			} catch (KuraException e) {
				s_logger.error("Can't get network interface info for {} :: exception - {}", interfaceName, e.toString());
			}
		}
		
		// Return an entry for non-connected modems (those w/o a ppp interface)
		Iterator<String> it = m_addedModems.iterator();
		while(it.hasNext()) {
		    String modemId = it.next();
		    UsbModemDevice usbModem = m_usbModems.get(modemId);
		    if (usbModem != null) {
			    // only add if there is not already a ppp interface for this modem
			    boolean addModem = true;
			    for(NetInterface<?> netInterface : netInterfaces) {
			        UsbDevice usbDevice = netInterface.getUsbDevice();
			        if(usbDevice != null) {
			            if(usbDevice.getUsbPort().equals(usbModem.getUsbPort())) {
			                addModem = false;
			                break;
			            }
			        }
			    }
			    if(addModem) {
			        netInterfaces.add(getModemInterface(usbModem.getUsbPort(), false, usbModem));
			    }
		    } else {
		    	// for Serial modem
		    	if (m_serialModem != null) {
				    // only add if there is not already a ppp interface for this modem
				    boolean addModem = true;
				    for(NetInterface<?> netInterface : netInterfaces) {
				    	String iface = netInterface.getName();
				    	if ((iface != null) && iface.startsWith("ppp")) {
				    		ModemInterface<ModemInterfaceAddress> pppModemInterface = getModemInterface(iface, false, m_serialModem);
				    		ModemInterface<ModemInterfaceAddress> serialModemInterface = getModemInterface(m_serialModem.getProductName(), false, m_serialModem);
				    		if ((pppModemInterface != null) && (serialModemInterface != null)) {
				    			String pppModel = pppModemInterface.getModel();
				    			String serialModel = serialModemInterface.getModel();
				    			if((pppModel != null) && pppModel.equals(serialModel)) {
				    				addModem = false;
					                break;
				    			}
				    		}
				    	}
				    }
				    if(addModem) {
				    	netInterfaces.add(getModemInterface(m_serialModem.getProductName(), false, m_serialModem));
				    }
		    	}
		    }
		}
		return netInterfaces;
	}

	@Override
	public List<WifiAccessPoint> getAllWifiAccessPoints() throws KuraException {
		List<String> interfaceNames = getAllNetworkInterfaceNames();
		if(interfaceNames != null && !interfaceNames.isEmpty()) {
			List<WifiAccessPoint> accessPoints = new ArrayList<WifiAccessPoint>();
			for(String interfaceName : interfaceNames) {
				if(LinuxNetworkUtil.getType(interfaceName) == NetInterfaceType.WIFI) {
					accessPoints.addAll(getWifiAccessPoints(interfaceName));
				}
			}
			return accessPoints;
		}
		return null;
	}

	@Override
	public List<WifiAccessPoint> getWifiAccessPoints(String wifiInterfaceName) throws KuraException {
		List<WifiAccessPoint> wifAccessPoints = null;
		IScanTool scanTool = ScanTool.get(wifiInterfaceName);
		if (scanTool != null) {
			wifAccessPoints = scanTool.scan();
		}
		return wifAccessPoints;
	}

	@Override
	public List<NetInterface<? extends NetInterfaceAddress>> getActiveNetworkInterfaces() throws KuraException {
		List<NetInterface<? extends NetInterfaceAddress>> interfaces = getNetworkInterfaces();
		
		if(interfaces != null) {
			for(int i=0; i<interfaces.size(); i++) {
				NetInterface<? extends NetInterfaceAddress> iface = interfaces.get(i);
				if(!LinuxNetworkUtil.isUp(iface.getName())) {
					s_logger.debug("removing interface {} because it is not up", iface.getName());
					interfaces.remove(i);
					i--;
				}
			}
		}
		
		return interfaces;
	}
	
	public NetInterface<? extends NetInterfaceAddress> getNetworkInterface(String interfaceName) throws KuraException {
		// ignore redpine vlan interface 
        if (interfaceName.startsWith("rpine")) {
        	s_logger.debug("Ignoring redpine vlan interface.");
        	return null;
        }
        // ignore usb0 for beaglebone
        if (interfaceName.startsWith("usb0") && "beaglebone".equals(System.getProperty("target.device"))) {
        	s_logger.debug("Ignoring usb0 for beaglebone.");
        	return null;
        }
        
        LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
        if (ifconfig == null) {
        	s_logger.debug("Ignoring {} interface.", interfaceName);
        	return null;
        }
        
		NetInterfaceType type = ifconfig.getType();	
		boolean isUp = ifconfig.isUp();
		if(type == NetInterfaceType.UNKNOWN) {
			 if (interfaceName.matches(UNCONFIGURED_MODEM_REGEX)) {
         		// If the interface name is in a form such as "1-3.4", assume it is a modem
         		type = NetInterfaceType.MODEM;
         	} else if ((m_serialModem != null) && interfaceName.equals(m_serialModem.getProductName())) {
         		type = NetInterfaceType.MODEM;
         	}
		}
 		
		if(type == NetInterfaceType.ETHERNET) {
			EthernetInterfaceImpl<NetInterfaceAddress> netInterface = new EthernetInterfaceImpl<NetInterfaceAddress>(interfaceName);
			
	        Map<String, String> driver = LinuxNetworkUtil.getEthernetDriver(interfaceName);
            netInterface.setDriver(driver.get("name"));
            netInterface.setDriverVersion(driver.get("version"));
            netInterface.setFirmwareVersion(driver.get("firmware"));
            netInterface.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));	  
            netInterface.setHardwareAddress(ifconfig.getMacAddressBytes()); 
            netInterface.setMTU(ifconfig.getMtu());
            netInterface.setSupportsMulticast(ifconfig.isMulticast());
			netInterface.setLinkUp(LinuxNetworkUtil.isLinkUp(type, interfaceName));
			netInterface.setLoopback(false);
			netInterface.setPointToPoint(false);
			netInterface.setUp(isUp);
            netInterface.setVirtual(isVirtual());
            netInterface.setUsbDevice(getUsbDevice(interfaceName));
            netInterface.setState(getState(interfaceName, isUp));
            netInterface.setNetInterfaceAddresses(getNetInterfaceAddresses(interfaceName, type, isUp));
			
            return netInterface;
		} else if(type == NetInterfaceType.LOOPBACK) {	
			LoopbackInterfaceImpl<NetInterfaceAddress> netInterface = new LoopbackInterfaceImpl<NetInterfaceAddress>(interfaceName);
			
			netInterface.setDriver(getDriver());
			netInterface.setDriverVersion(getDriverVersion());
			netInterface.setFirmwareVersion(getFirmwareVersion());
            netInterface.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));         
			netInterface.setHardwareAddress(new byte[]{0, 0, 0, 0, 0, 0});
			netInterface.setLoopback(true);
	        netInterface.setMTU(ifconfig.getMtu());
	        netInterface.setSupportsMulticast(ifconfig.isMulticast());
			netInterface.setPointToPoint(false);
            netInterface.setUp(isUp);
            netInterface.setVirtual(false);
            netInterface.setUsbDevice(null);
			netInterface.setState(getState(interfaceName, isUp));
            netInterface.setNetInterfaceAddresses(getNetInterfaceAddresses(interfaceName, type, isUp));

			return netInterface;
		} else if(type == NetInterfaceType.WIFI) {
			WifiInterfaceImpl<WifiInterfaceAddress> wifiInterface = new WifiInterfaceImpl<WifiInterfaceAddress>(interfaceName);
			
            Map<String, String> driver = LinuxNetworkUtil.getEthernetDriver(interfaceName);
            wifiInterface.setDriver(driver.get("name"));
            wifiInterface.setDriverVersion(driver.get("version"));
            wifiInterface.setFirmwareVersion(driver.get("firmware"));
			wifiInterface.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));	        
	        wifiInterface.setHardwareAddress(ifconfig.getMacAddressBytes());
	        wifiInterface.setMTU(ifconfig.getMtu());
	        wifiInterface.setSupportsMulticast(ifconfig.isMulticast());
			// FIXME:MS Add linkUp in the AbstractNetInterface and populate accordingly
//			wifiInterface.setLinkUp(LinuxNetworkUtil.isLinkUp(type, interfaceName));
			wifiInterface.setLoopback(false);
            wifiInterface.setPointToPoint(false);
            wifiInterface.setUp(isUp);
            wifiInterface.setVirtual(isVirtual());
            wifiInterface.setUsbDevice(getUsbDevice(interfaceName));
            wifiInterface.setState(getState(interfaceName, isUp));
			wifiInterface.setNetInterfaceAddresses(getWifiInterfaceAddresses(interfaceName, isUp));
			wifiInterface.setCapabilities(LinuxNetworkUtil.getWifiCapabilities(interfaceName));

			return wifiInterface;
        } else if(type == NetInterfaceType.MODEM) {
            ModemDevice modemDevice = null;
            if(interfaceName.startsWith("ppp")) {
                // already connected - find the corresponding usb device
                modemDevice = m_usbModems.get(getModemUsbPort(interfaceName));
                if ((modemDevice == null)  &&  (m_serialModem != null)) {
                	modemDevice = m_serialModem;
                }
            } else if (interfaceName.matches(UNCONFIGURED_MODEM_REGEX)){
                // the interface name is in the form of a usb port i.e. "1-3.4"
                modemDevice = m_usbModems.get(interfaceName);
            } else if ((m_serialModem != null) && interfaceName.equals(m_serialModem.getProductName())) {
            	modemDevice = m_serialModem; 
            }
            return (modemDevice != null) ? getModemInterface(interfaceName, isUp, modemDevice) : null;
		} else {
			if(interfaceName.startsWith("can")) {
				s_logger.trace("Ignoring CAN interface: {}", interfaceName);
			} else if (interfaceName.startsWith("ppp")) {
			    s_logger.debug("Ignoring unconfigured ppp interface: {}", interfaceName);
			} else {
				s_logger.debug("Unsupported network type - not adding to network devices: {} of type: ", interfaceName, type.toString());
			}
			return null;
		}
	}
	
    @Override
    public void handleEvent(Event event) {
        s_logger.debug("handleEvent() :: topic: {}", event.getTopic());
        String topic = event.getTopic();
        if (topic.equals(UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC)) {
        	//validate mandatory properties
        	if (event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY) == null) {
        		return;
        	}
        	if (event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY) == null) {
        		return;
        	}
        	if (event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY) == null) {
        		return;
        	}
        	if (event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY) == null) {
        		return;
        	}
        	
            //do we care?
            final SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem((String) event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY),
            		                                                          (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY));
            if(modemInfo != null) {
            	//Found one - see if we have some info for it.
            	//Also check if we are getting more devices than expected.
            	//This can happen if all the modem resources cannot be removed from the OS or from Kura.
            	//In this case we did not receive an UsbDeviceRemovedEvent and we did not post
            	//an ModemRemovedEvent. Should we do it here?
            	List<? extends UsbModemDriver> drivers = modemInfo.getDeviceDrivers();
				for (UsbModemDriver driver : drivers) {
					try {
						driver.install();
					} catch (Exception e) {
						s_logger.error("handleEvent() :: Failed to install modem device driver {} - {}", driver.getName(), e);
					}
				}
            	
            	UsbModemDevice usbModem = m_usbModems.get(event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY));
            	
            	boolean createNewUsbModemDevice = false;
            	if (usbModem == null) {
            		createNewUsbModemDevice = true;
            	} else if ((modemInfo.getNumTtyDevs() > 0) && (modemInfo.getNumBlockDevs() > 0)) {
            		if ((usbModem.getTtyDevs().size() >= modemInfo.getNumTtyDevs()) && (usbModem.getBlockDevs().size() >= modemInfo.getNumBlockDevs())) {
            			createNewUsbModemDevice = true;
            		}
            	} else if (((modemInfo.getNumTtyDevs() > 0) && (usbModem.getTtyDevs().size() >= modemInfo.getNumTtyDevs())) ||
                		((modemInfo.getNumBlockDevs() > 0) && (usbModem.getBlockDevs().size() >= modemInfo.getNumBlockDevs()))) {
            		createNewUsbModemDevice = true;
            	}
            	 
            	if (createNewUsbModemDevice) {
            		if (usbModem == null) {
            			s_logger.debug("handleEvent() :: Modem not found. Create one");
            		} else {
            			s_logger.debug("handleEvent() :: Found modem with too many resources: {}. Create a new one", usbModem);
            		}

            		usbModem = new UsbModemDevice(
            				(String) event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY),
            				(String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY),
            				(String) event.getProperty(UsbDeviceEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY),
            				(String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY),
            				(String) event.getProperty(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY),
            				(String) event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY));
            	}

                String resource = (String) event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY);
                
                s_logger.debug("handleEvent() :: Adding resource: {} for: {}", resource, usbModem.getUsbPort());
                if(resource.contains("tty")) { 
                	usbModem.addTtyDev(resource);
                } else {
                	usbModem.addBlockDev(resource);
                }
                
                m_usbModems.put((String) usbModem.getUsbPort(), usbModem);

                //At this point, we should have some modems - display them
                s_logger.info("handleEvent() :: Modified modem (Added resource): {}", usbModem);
                
                s_logger.debug("handleEvent() :: usbModem.getTtyDevs().size()={}, modemInfo.getNumTtyDevs()={}",
                		usbModem.getTtyDevs().size(), modemInfo.getNumTtyDevs());
                s_logger.debug("handleEvent() :: usbModem.getBlockDevs().size()={}, modemInfo.getNumBlockDevs()={}",
                		usbModem.getBlockDevs().size(), modemInfo.getNumBlockDevs());
                
                // Check for correct number of resources
				if ((usbModem.getTtyDevs().size() == modemInfo.getNumTtyDevs()) &&
					(usbModem.getBlockDevs().size() == modemInfo.getNumBlockDevs())) {
					s_logger.info("handleEvent() :: posting ModemAddedEvent -- USB_EVENT_DEVICE_ADDED_TOPIC: {}", usbModem);
	                m_eventAdmin.postEvent(new ModemAddedEvent(usbModem));
	                m_addedModems.add(usbModem.getUsbPort());
	                
	    			if ((OS_VERSION != null && TARGET_NAME != null) && 
	    					(OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) &&
	    					TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) ||
	    					(OS_VERSION.equals(KuraConstants.Reliagate_10_11.getImageName() + "_" + KuraConstants.Reliagate_10_11.getImageVersion()) &&
	    					TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName()))) {	
		                if (m_serialModem != null) {
		                	if (SupportedUsbModemInfo.Telit_HE910_D.getVendorId().equals( usbModem.getVendorId())
			                		&& SupportedUsbModemInfo.Telit_HE910_D.getProductId().equals(usbModem.getProductId())) {
		                		s_logger.info("handleEvent() :: Removing {} from addedModems", m_serialModem.getProductName());
			                	m_addedModems.remove(m_serialModem.getProductName());
			                }
		                }
	                }
                }
            }
            
            /*
            System.out.println("ADDED Device: " + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_VENDOR_ID_PROPERTY) + ":" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_ID_PROPERTY));
            System.out.println("\t" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY));
            System.out.println("\t" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY));
            System.out.println("\t" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_NAME_PROPERTY));
            System.out.println("\t" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_USB_PORT_PROPERTY));
            */
        } else if(topic.equals(UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC)) {
        	//validate mandatory properties
        	if (event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY) == null) {
        		return;
        	}
        	if (event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY) == null) {
        		return;
        	}
        	if (event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY) == null) {
        		return;
        	}

            //do we care?
            SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem((String)event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY),
            		                                                          (String)event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY));
            if(modemInfo != null) {
            	//found one - remove if it exists
            	UsbModemDevice usbModem = m_usbModems.remove(event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY));
            	if(usbModem != null) {
            		s_logger.info("handleEvent() :: Removing modem: {}", usbModem);
            		m_addedModems.remove(usbModem.getUsbPort());

            		Map<String, String> properties = new HashMap<String, String>();
            		properties.put(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY, usbModem.getUsbBusNumber());
            		properties.put(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY, usbModem.getUsbDevicePath());
            		properties.put(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY, usbModem.getUsbPort());
            		properties.put(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY, usbModem.getVendorId());
            		properties.put(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY, usbModem.getProductId());
            		properties.put(UsbDeviceEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY, usbModem.getManufacturerName());
            		properties.put(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY, usbModem.getProductName());
            		m_eventAdmin.postEvent(new ModemRemovedEvent(properties));
            	}
            }
            
            /*
            System.out.println("REMOVED Device: " + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_VENDOR_ID_PROPERTY) + ":" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_ID_PROPERTY));
            System.out.println("\t" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_RESOURCE_PROPERTY));
            System.out.println("\t" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY));
            System.out.println("\t" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_PRODUCT_NAME_PROPERTY));
            System.out.println("\t" + event.getProperty(UsbDeviceAddedEvent.USB_EVENT_USB_PORT_PROPERTY));
            */
        } else if (topic.equals(SerialModemAddedEvent.SERIAL_MODEM_EVENT_ADDED_TOPIC)) { 
        	SerialModemAddedEvent serialModemAddedEvent = (SerialModemAddedEvent)event;
        	SupportedSerialModemInfo serialModemInfo = serialModemAddedEvent.getSupportedSerialModemInfo();
        	if (serialModemInfo != null) {
    			if ((OS_VERSION != null && TARGET_NAME != null) && 
    					(OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) &&
    					TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) ||
    					(OS_VERSION.equals(KuraConstants.Reliagate_10_11.getImageName() + "_" + KuraConstants.Reliagate_10_11.getImageVersion()) &&
    					TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName()))) {	
        			if (m_usbModems.isEmpty()) {
        				m_serialModem = new SerialModemDevice(
    	    					serialModemInfo.getModemName(),
    	    					serialModemInfo.getManufacturerName(), serialModemInfo
    	    							.getDriver().getComm().getSerialPorts()); 
    	    			if (m_serialModem != null) {
    	    	        	s_logger.debug("handleEvent() :: posting ModemAddedEvent for serial modem: {}", m_serialModem.getProductName());
    	    	            m_eventAdmin.postEvent(new ModemAddedEvent(m_serialModem));
    	    	            m_addedModems.add(m_serialModem.getProductName());
    	    	        }
        			} else {
        				s_logger.info("handleEvent() :: Ignoring {} modem since it has already been detected as a USB device", 
        						serialModemInfo.getModemName());
        			}
        		} else {
	    			m_serialModem = new SerialModemDevice(
	    					serialModemInfo.getModemName(),
	    					serialModemInfo.getManufacturerName(), serialModemInfo
	    							.getDriver().getComm().getSerialPorts()); 
	    			if (m_serialModem != null) {
	    	        	s_logger.debug("handleEvent() :: posting ModemAddedEvent for serial modem: {}", m_serialModem.getProductName());
	    	            m_eventAdmin.postEvent(new ModemAddedEvent(m_serialModem));
	    	            m_addedModems.add(m_serialModem.getProductName());
	    	        }
        		}
    		}
        } else {
            s_logger.error("handleEvent() :: Unexpected event topic: {}", topic);
        }
    }
	
	private String getDriver() {
		//FIXME - hard coded
		return "unknown";
	}
	
	private String getDriverVersion() {
		//FIXME - hard coded
		return "unknown";
	}
	
	private String getFirmwareVersion() {
		//FIXME - hard coded
		return "unknown";
	}
	
	private ModemInterface<ModemInterfaceAddress> getModemInterface(String interfaceName, boolean isUp, ModemDevice modemDevice) throws KuraException {

		ModemInterfaceImpl<ModemInterfaceAddress> modemInterface = new ModemInterfaceImpl<ModemInterfaceAddress>(interfaceName);
        
        modemInterface.setModemDevice(modemDevice);
        if (modemDevice instanceof UsbModemDevice) {

            UsbModemDevice usbModemDevice = (UsbModemDevice) modemDevice;
            SupportedUsbModemInfo supportedUsbModemInfo = null;
        	supportedUsbModemInfo = SupportedUsbModemsInfo.getModem(usbModemDevice.getVendorId(), usbModemDevice.getProductId());
            modemInterface.setTechnologyTypes(supportedUsbModemInfo.getTechnologyTypes());
            modemInterface.setUsbDevice((UsbModemDevice)modemDevice);        	
        } 
        else if (modemDevice instanceof SerialModemDevice) {

            SupportedSerialModemInfo supportedSerialModemInfo = null;
            supportedSerialModemInfo = SupportedSerialModemsInfo.getModem();
            modemInterface.setTechnologyTypes(supportedSerialModemInfo.getTechnologyTypes());
        }
                 
        int pppNum = 0;
        if(interfaceName.startsWith("ppp")) {
            pppNum = Integer.parseInt(interfaceName.substring(3));
        }
        modemInterface.setPppNum(pppNum);        
        modemInterface.setManufacturer(modemDevice.getManufacturerName());
        modemInterface.setModel(modemDevice.getProductName());
        modemInterface.setModemIdentifier(modemDevice.getProductName());
        
        // these properties required net.admin packages
        modemInterface.setDriver(getDriver());
        modemInterface.setDriverVersion(getDriverVersion());
        modemInterface.setFirmwareVersion(getFirmwareVersion());
        modemInterface.setSerialNumber("unknown");

        modemInterface.setLoopback(false);
        modemInterface.setPointToPoint(true);
        modemInterface.setState(getState(interfaceName, isUp));
        modemInterface.setHardwareAddress(new byte[]{0, 0, 0, 0, 0, 0});
        LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
        if (ifconfig != null) {
        	modemInterface.setMTU(ifconfig.getMtu());
        	modemInterface.setSupportsMulticast(ifconfig.isMulticast());
        }
        
        modemInterface.setUp(isUp);
        modemInterface.setVirtual(isVirtual());
        modemInterface.setNetInterfaceAddresses(getModemInterfaceAddresses(interfaceName, isUp));
        
        return modemInterface;

    }
	
	private List<NetInterfaceAddress> getNetInterfaceAddresses(String interfaceName, NetInterfaceType type, boolean isUp) throws KuraException 
	{
        List<NetInterfaceAddress> netInterfaceAddresses = new ArrayList<NetInterfaceAddress>();
		if(isUp) {
			ConnectionInfo conInfo = new ConnectionInfoImpl(interfaceName);			
			NetInterfaceAddressImpl netInterfaceAddress = new NetInterfaceAddressImpl();
			try {
			    // FIXME:MC The whole block of information can be fetched with a single ifconfig?
				LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
				if (ifconfig != null) {
					String currentNetmask = ifconfig.getInetMask();
	                if (currentNetmask != null) {
						netInterfaceAddress.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
						netInterfaceAddress.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
						
						netInterfaceAddress.setNetmask(IPAddress.parseHostAddress(currentNetmask));
						netInterfaceAddress.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
						netInterfaceAddress.setGateway(conInfo.getGateway());
						if(type == NetInterfaceType.MODEM) {
							if(isUp) {
								netInterfaceAddress.setDnsServers(LinuxDns.getInstance().getPppDnServers());
							}
						} else {
							netInterfaceAddress.setDnsServers(conInfo.getDnsServers());
						}
						netInterfaceAddresses.add(netInterfaceAddress);
	                }
				}
			} catch(UnknownHostException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}            	            
		} 
        return netInterfaceAddresses;
	}
	
	private List<WifiInterfaceAddress> getWifiInterfaceAddresses(String interfaceName, boolean isUp) throws KuraException 
	{
        List<WifiInterfaceAddress> wifiInterfaceAddresses = new ArrayList<WifiInterfaceAddress>();
		if(isUp) {
			ConnectionInfo conInfo = new ConnectionInfoImpl(interfaceName);			
			WifiInterfaceAddressImpl wifiInterfaceAddress = new WifiInterfaceAddressImpl();
			wifiInterfaceAddresses.add(wifiInterfaceAddress);			
			try {
				LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
				if (ifconfig != null) {
					String currentNetmask = ifconfig.getInetMask();
	                if (currentNetmask != null) {
						wifiInterfaceAddress.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
						wifiInterfaceAddress.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
						wifiInterfaceAddress.setNetmask(IPAddress.parseHostAddress(currentNetmask));
						wifiInterfaceAddress.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
						wifiInterfaceAddress.setGateway(conInfo.getGateway());
						wifiInterfaceAddress.setDnsServers(conInfo.getDnsServers());
						
						WifiMode wifiMode = LinuxNetworkUtil.getWifiMode(interfaceName);
						wifiInterfaceAddress.setBitrate(LinuxNetworkUtil.getWifiBitrate(interfaceName));
						wifiInterfaceAddress.setMode(wifiMode);
						
						//TODO - should this only be the AP we are connected to in client mode?
						if(wifiMode == WifiMode.INFRA) {
							String currentSSID = LinuxNetworkUtil.getSSID(interfaceName);
	
							if(currentSSID != null) {
								s_logger.debug("Adding access point SSID: {}", currentSSID);
	
								WifiAccessPointImpl wifiAccessPoint = new WifiAccessPointImpl(currentSSID);
	
								// FIXME: fill in other info
								wifiAccessPoint.setMode(WifiMode.INFRA);
								List<Long> bitrate = new ArrayList<Long>();
								bitrate.add(54000000L);
								wifiAccessPoint.setBitrate(bitrate);
								wifiAccessPoint.setFrequency(12345);
								wifiAccessPoint.setHardwareAddress("20AA4B8A6442".getBytes());
								wifiAccessPoint.setRsnSecurity(EnumSet.allOf(WifiSecurity.class));
								wifiAccessPoint.setStrength(1234);
								wifiAccessPoint.setWpaSecurity(EnumSet.allOf(WifiSecurity.class));
	
								wifiInterfaceAddress.setWifiAccessPoint(wifiAccessPoint);
							}
						}
	                } else {
	                	return null;
	                }
				} else {
					return null;
				}
			} catch(UnknownHostException e) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}

		return wifiInterfaceAddresses;
	}
	
    private List<ModemInterfaceAddress> getModemInterfaceAddresses(String interfaceName, boolean isUp) throws KuraException {
        List<ModemInterfaceAddress> modemInterfaceAddresses = new ArrayList<ModemInterfaceAddress>();
        if(isUp) {
            ConnectionInfo conInfo = new ConnectionInfoImpl(interfaceName);            
            ModemInterfaceAddressImpl modemInterfaceAddress = new ModemInterfaceAddressImpl();
            modemInterfaceAddresses.add(modemInterfaceAddress);            
            try {
            	LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
            	if (ifconfig != null) {
					String currentNetmask = ifconfig.getInetMask();
	                if (currentNetmask != null) {
	                    modemInterfaceAddress.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
	                    modemInterfaceAddress.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
	                    modemInterfaceAddress.setNetmask(IPAddress.parseHostAddress(currentNetmask));
	                    modemInterfaceAddress.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
	                    modemInterfaceAddress.setGateway(conInfo.getGateway());
	                    modemInterfaceAddress.setDnsServers(conInfo.getDnsServers());
	                    ModemConnectionStatus connectionStatus = isUp? ModemConnectionStatus.CONNECTED : ModemConnectionStatus.DISCONNECTED;
	                    modemInterfaceAddress.setConnectionStatus(connectionStatus);
	                    // TODO - other attributes
	                } else {
	                    return null;
	                }
            	} else {
                    return null;
                }
            } catch(UnknownHostException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }
        return modemInterfaceAddresses;
    }
	
	private NetInterfaceState getState(String interfaceName, boolean isUp) {
		/** The device is in an unknown state. */
		//UNKNOWN(0),
		/** The device is recognized but not managed by NetworkManager. */
		//UNMANAGED(10),
		/** The device cannot be used (carrier off, rfkill, etc). */
		//UNAVAILABLE(20),
		/** The device is not connected. */
		//DISCONNECTED(30),
		/** The device is preparing to connect. */ 
		//PREPARE(40),
		/** The device is being configured. */
		//CONFIG(50),
		/** The device is awaiting secrets necessary to continue connection. */
		//NEED_AUTH(60),
		/** The IP settings of the device are being requested and configured. */
		//IP_CONFIG(70),
		/** The device's IP connectivity ability is being determined. */
		//IP_CHECK(80),
		/** The device is waiting for secondary connections to be activated. */
		//SECONDARIES(90),
		/** The device is active. */
		//ACTIVATED(100),
		/** The device's network connection is being torn down. */
		//DEACTIVATING(110),
		/** The device is in a failure state following an attempt to activate it. */
		//FAILED(120);
		
		//FIXME - expand to support other States
		if(isUp) {
			return NetInterfaceState.ACTIVATED;
		} else {
			return NetInterfaceState.DISCONNECTED;
		}
	}
	
	private UsbNetDevice getUsbDevice(String interfaceName) {
		List<UsbNetDevice> usbNetDevices = m_usbService.getUsbNetDevices();
		if(usbNetDevices != null && !usbNetDevices.isEmpty()) {
			for(UsbNetDevice usbNetDevice : usbNetDevices) {
				if(usbNetDevice.getInterfaceName().equals(interfaceName)) {
					return usbNetDevice;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Given an interface name (e.g. 'ppp0'), look up the associated usb port
	 * using the ppp peers config files
	 */
	@Override
	public String getModemUsbPort(String interfaceName) {
	    if(interfaceName != null) {
    	    File peersDir = new File(PPP_PEERS_DIR);
    	    if(peersDir.isDirectory()) {
    	        File[] peerFiles = peersDir.listFiles();
    	        for(int i=0; i<peerFiles.length; i++) {
    	            File peerFile = peerFiles[i];
    	            if(peerFile.getName().equals(interfaceName)) {
    	                // should be a symlink...find the file it links to
    	                try {
                            String peerFilename = peerFile.getCanonicalFile().getName();
                            String[] filenameParts = peerFilename.split("_");
                            return filenameParts[filenameParts.length - 1];
                        } catch (IOException e) {
                            s_logger.error("Error splitting peer filename!", e);
                        }
    	            }
                }
            }	    
    	    
    	    // Return the interface name if it looks like a usb port
            if(interfaceName.matches(UNCONFIGURED_MODEM_REGEX)) {
                return interfaceName;
            }
	    }
	    
	    return null;
	}
	
	/**
	 * Given a usb port address, look up the associated ppp interface name
	 * @throws KuraException 
	 */
	@Override
	public String getModemPppPort(ModemDevice modemDevice) throws KuraException {
	    
		String deviceName = null;
		String modemId = null;
		
		if (modemDevice instanceof UsbModemDevice) {
			UsbModemDevice usbModem = (UsbModemDevice)modemDevice;
			SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbModem.getVendorId(), usbModem.getProductId());
			 deviceName = modemInfo.getDeviceName();
			 modemId = usbModem.getUsbPort();
		} else if (modemDevice instanceof SerialModemDevice) {
			SerialModemDevice serialModem = (SerialModemDevice) modemDevice;
			deviceName = serialModem.getProductName();
			modemId = serialModem.getProductName();
		}
		
        // find a matching config file in the ppp peers directory
        File peersDir = new File(PPP_PEERS_DIR);
        if(peersDir.isDirectory()) {
            File[] peerFiles = peersDir.listFiles();
            for(int i=0; i<peerFiles.length; i++) {
                File peerFile = peerFiles[i];
                String peerFilename = peerFile.getName();
                if(peerFilename.startsWith(deviceName) && peerFilename.endsWith(/*usbPort*/ modemId)) {
                	BufferedReader br = null;
                	try {
	                	br = new BufferedReader(new FileReader(peerFile));
	                	String line = null;
	                	StringBuilder sbIfaceName = null;
	                	while ((line = br.readLine()) != null) {
	                		if (line.startsWith("unit")) {
	                			sbIfaceName = new StringBuilder("ppp");
	                			sbIfaceName.append(line.substring("unit".length()).trim());
	                			break;
	                		}
	                	}
	                	return sbIfaceName.toString();
                	} catch (Exception e) {
                		s_logger.error("failed to parse peers file - {}", e);
                	} finally {
                		if (br != null) {
                			try {
								br.close();
							} catch (IOException e) {
								s_logger.error("failed to close buffered reader - {}", e);
							}
                		}
                	}
                    // find a 'pppX' symlink to this peer file
                    /*
                	for(int j=0; j<peerFiles.length; j++) {
                        File pppFile = peerFiles[j];
                        
                        if(peerFilename.equals(pppFile.getName())) {
                            continue;
                        }
                        
                        try {
                            if(pppFile.getName().matches("ppp\\d+") &&
                                    peerFile.getCanonicalPath().equals(pppFile.getCanonicalPath())) {
                                
                                return pppFile.getName();
                            }
                        } catch (IOException e) {
                            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                        }
                    }
                    */
                    
                    break;
                }
            }
        }    
        return null;
	}
	
	private boolean isVirtual() {
		//FIXME - assuming only one to one relationship for network interfaces today
		return false;
	}
	
	private void toggleModem(SupportedUsbModemInfo modemInfo) throws Exception {
		while (!s_stopThread.get()) {
    		ModemDriver modemDriver = null;
    		List<? extends UsbModemDriver> usbDeviceDrivers = modemInfo.getDeviceDrivers();
    		if ((usbDeviceDrivers != null) && (!usbDeviceDrivers.isEmpty())) {
    			modemDriver = usbDeviceDrivers.get(0);
    		}
    		if (modemDriver != null) {
    			boolean status = false;
    			try {
    				s_logger.info("toggleModem() :: turning modem off ...");
					if(modemDriver.turnModemOff()) {
						modemDriver.sleep(3000);
						s_logger.info("toggleModem() :: turning modem on ...");
						status = modemDriver.turnModemOn();
						if (status) {
							s_logger.info("toggleModem() :: modem has been toggled successfully ...");
							s_stopThread.set(status);
							toggleModemNotity();
						}
					}
				} catch (Exception e) {
					s_logger.error("toggleModem() :: failed to toggle modem - {}", e);
				}
    		}
    		if (!s_stopThread.get()) {
    			toggleModemWait();
    		}
		}
	}
	
	private void toggleModemNotity() {
		if (s_stopThread != null) {
			synchronized (s_stopThread) {
				s_stopThread.notifyAll();
			}
		}
	}
	
	private void toggleModemWait() throws InterruptedException {
		if (s_stopThread != null) {
			synchronized (s_stopThread) {
				s_stopThread.wait(TOOGLE_MODEM_THREAD_INTERVAL);
			}
		}
	}
}
