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
package org.eclipse.kura.core.net;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.modem.ModemPowerMode;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterface;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfiguration {

	private static final Logger s_logger = LoggerFactory.getLogger(NetworkConfiguration.class);
	
	private Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>>    m_netInterfaceConfigs;
	private Map<String,Object> m_properties;
	private boolean m_recomputeProperties;
	private List<String> m_modifiedInterfaceNames;

	public NetworkConfiguration()
	{
		s_logger.debug("Created empty NetworkConfiguration");
		m_netInterfaceConfigs = new HashMap<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>>();
	}

	/**
	 * Constructor for create a completely new NetComponentConfiguration based on a set of properties
	 * 
	 * @param properties				The properties that represent the new configuration
	 * @throws UnknownHostException		If some hostnames can not be resolved
	 * @throws KuraException				It there is an internal error
	 */
	public NetworkConfiguration(Map<String,Object> properties)
		throws UnknownHostException, KuraException
	{
		s_logger.debug("Creating NetworkConfiguration from properties");
		m_netInterfaceConfigs = new HashMap<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>>();
		String[] availableInterfaces = null;
		
		try {
			availableInterfaces = (String[]) properties.get("net.interfaces");
		} catch(ClassCastException e) {
			//this means this configuration came from GWT - so convert the comma separated list
			String interfaces = (String) properties.get("net.interfaces");
			StringTokenizer st = new StringTokenizer(interfaces, ",");

			List<String> interfacesArray = new ArrayList<String>();
			while(st.hasMoreTokens()) {
				interfacesArray.add(st.nextToken());
			}
			availableInterfaces = interfacesArray.toArray(new String[interfacesArray.size()]);
		}
		
		if (availableInterfaces != null) {
			s_logger.debug("There are " + availableInterfaces.length + " interfaces to add to the new configuration");
			for(int i=0; i<availableInterfaces.length; i++) {
				String currentNetInterface = availableInterfaces[i];
				StringBuffer keyBuffer = new StringBuffer();
				keyBuffer.append("net.interface.")
				.append(currentNetInterface)
				.append(".type");
				NetInterfaceType type = NetInterfaceType.UNKNOWN;
				if(properties.get(keyBuffer.toString()) != null) {
				    type = NetInterfaceType.valueOf((String) properties.get(keyBuffer.toString()));
				}
				s_logger.trace("Adding interface: " + availableInterfaces[i] + " of type " + type);
				addInterfaceConfiguration(availableInterfaces[i], type, properties);
			}
		}
		
		m_modifiedInterfaceNames = new ArrayList<String>();
		String modifiedInterfaces = (String) properties.get("modified.interface.names");
		if(modifiedInterfaces != null) {
			for(String interfaceName : modifiedInterfaces.split(",")) {
				m_modifiedInterfaceNames.add(interfaceName);
			}
		}
		
		m_recomputeProperties = true;
	}
	
	public void setModifiedInterfaceNames(List<String> modifiedInterfaceNames) {
		if(modifiedInterfaceNames != null && !modifiedInterfaceNames.isEmpty()) {
			m_modifiedInterfaceNames = modifiedInterfaceNames;
			m_recomputeProperties = true;
		}
	}
	
	public List<String> getModifiedInterfaceNames() {
		return m_modifiedInterfaceNames;
	}
	
	public void accept(NetworkConfigurationVisitor visitor) throws KuraException {
	    visitor.visit(this);
	}
	
	public void addNetInterfaceConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
	{
		m_netInterfaceConfigs.put(netInterfaceConfig.getName(), netInterfaceConfig);
		m_recomputeProperties = true;
	}

	
	public void addNetConfig(String interfaceName, NetInterfaceType netInterfaceType, NetConfig netConfig) throws KuraException {
		NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = m_netInterfaceConfigs.get(interfaceName);

		if(netInterfaceConfig == null) {
			switch(netInterfaceType) {
				case LOOPBACK :
					netInterfaceConfig = new LoopbackInterfaceConfigImpl(interfaceName);
					break;
				case ETHERNET :
					netInterfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
					break;
				case WIFI :
					netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
					break;
				case MODEM :
				    netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
				default :
					break;
			}
		}
		
		List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
		
		s_logger.trace("Adding a netConfig: " + netConfig);
		for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
			NetInterfaceAddressConfigImpl netInterfaceAddressConfigImpl = (NetInterfaceAddressConfigImpl)netInterfaceAddressConfig;
			List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
			netConfigs.add(netConfig);
			netInterfaceAddressConfigImpl.setNetConfigs(netConfigs);
		}
		
		m_recomputeProperties = true;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		Iterator<String> it = m_netInterfaceConfigs.keySet().iterator();
		while(it.hasNext()) {
			NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = m_netInterfaceConfigs.get(it.next());
	
			sb.append("\nname: " + netInterfaceConfig.getName());
			sb.append(" :: Loopback? " + netInterfaceConfig.isLoopback());
			sb.append(" :: Point to Point? " + netInterfaceConfig.isPointToPoint());
			sb.append(" :: Up? " + netInterfaceConfig.isUp());
			sb.append(" :: Virtual? " + netInterfaceConfig.isVirtual());
			sb.append(" :: Driver: " + netInterfaceConfig.getDriver());
			sb.append(" :: Driver Version: " + netInterfaceConfig.getDriverVersion());
			sb.append(" :: Firmware Version: " + netInterfaceConfig.getFirmwareVersion());
			sb.append(" :: MTU: " + netInterfaceConfig.getMTU());
			if(netInterfaceConfig.getHardwareAddress() != null) {
				sb.append(" :: Hardware Address: " + new String(netInterfaceConfig.getHardwareAddress()));
			}
			sb.append(" :: State: " + netInterfaceConfig.getState());
			sb.append(" :: Type: " + netInterfaceConfig.getType());
			sb.append(" :: Usb Device: " + netInterfaceConfig.getUsbDevice());
			
			
			List<? extends NetInterfaceAddress> netInterfaceAddresses = netInterfaceConfig.getNetInterfaceAddresses();
			for(NetInterfaceAddress netInterfaceAddress : netInterfaceAddresses) {
				if(netInterfaceAddress.getAddress() != null) {
					sb.append(" :: Address: " + netInterfaceAddress.getAddress().getHostAddress());
				}
				sb.append(" :: Prefix: " + netInterfaceAddress.getNetworkPrefixLength());
				if(netInterfaceAddress.getNetmask() != null) {
					sb.append(" :: Netmask: " + netInterfaceAddress.getNetmask().getHostAddress());
				}
				if(netInterfaceAddress.getBroadcast() != null) {
					sb.append(" :: Broadcast: " + netInterfaceAddress.getBroadcast().getHostAddress());
				}
			}
			
			List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
			
			if(netInterfaceAddressConfigs != null) {
				for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
					List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
	
					if(netConfigs != null) {
						for(NetConfig netConfig : netConfigs) {
							if(netConfig instanceof NetConfigIP4) {
								sb.append("\n\tIPv4 ");
								if(((NetConfigIP4) netConfig).isDhcp()) {
									sb.append(" :: is DHCP client");
									Map<String, Object> dhcp4Map = ((NetConfigIP4)netConfig).getProperties();
									Iterator<String> it2 = dhcp4Map.keySet().iterator();
									while(it2.hasNext()) {
										String dhcpKey = it2.next();
										sb.append(" :: " + dhcpKey + ": " + dhcp4Map.get(dhcpKey));
									}
								} else if(((NetConfigIP4)netConfig).getAddress() == null) {
									sb.append(" :: is not configured for STATIC or DHCP");
								} else {
									sb.append(" :: is STATIC client");
									if(((NetConfigIP4)netConfig).getAddress() != null) {
										sb.append(" :: Address: " + ((NetConfigIP4)netConfig).getAddress().getHostAddress());
									}
									sb.append(" :: Prefix: " + ((NetConfigIP4)netConfig).getNetworkPrefixLength());
									if(((NetConfigIP4)netConfig).getGateway() != null) {
										sb.append(" :: Gateway: " + ((NetConfigIP4)netConfig).getGateway().getHostAddress());
									}
		
									List<IP4Address> dnsServers = ((NetConfigIP4)netConfig).getDnsServers();
									List<IP4Address> winsServers = ((NetConfigIP4)netConfig).getWinsServers();
									List<String> domains = ((NetConfigIP4)netConfig).getDomains();
									if(dnsServers != null) {
										for(IP4Address dnsServer : dnsServers) {
											sb.append(" :: DNS : " + dnsServer.getHostAddress());
										}
									}
									if(winsServers != null) {
										for(IP4Address winsServer : winsServers) {
											sb.append(" :: WINS Server : " + winsServer.getHostAddress());
										}
									}
									if(domains != null) {
										for(String domain : domains) {
											sb.append(" :: Domains : " + domain);
										}
									}
								}
							} else if(netConfig instanceof NetConfigIP6) {
								sb.append("\n\tIPv6 ");
								if(((NetConfigIP6) netConfig).isDhcp()) {
									sb.append(" :: is DHCP client");
									Map<String, Object> dhcp6Map = ((NetConfigIP6)netConfig).getProperties();
									Iterator<String> it2 = dhcp6Map.keySet().iterator();
									while(it2.hasNext()) {
										String dhcpKey = it2.next();
										sb.append(" :: " + dhcpKey + ": " + dhcp6Map.get(dhcpKey));
									}
								} else {
									sb.append(" :: is STATIC client");
									if(((NetConfigIP6)netConfig).getAddress() != null) {
										sb.append(" :: Address: " + ((NetConfigIP6)netConfig).getAddress().getHostAddress());
									}
		
									List<IP6Address> dnsServers = ((NetConfigIP6)netConfig).getDnsServers();
									List<String> domains = ((NetConfigIP6)netConfig).getDomains();
									for(IP6Address dnsServer : dnsServers) {
										sb.append(" :: DNS : " + dnsServer.getHostAddress());
									}
									for(String domain : domains) {
										sb.append(" :: Domains : " + domain);
									}
								}
							} else if(netConfig instanceof WifiConfig) {
								sb.append("\n\tWifiConfig ");
								
								sb.append(" :: SSID: " + ((WifiConfig) netConfig).getSSID());
								sb.append(" :: BgScan: " + ((WifiConfig) netConfig).getBgscan());
								sb.append(" :: Broadcast: " + ((WifiConfig) netConfig).getBroadcast());
								int[] channels = ((WifiConfig) netConfig).getChannels();
								if(channels != null && channels.length > 0) {
									for(int i=0; i<channels.length; i++) {
										sb.append(channels[i]);
										if(i+1 < channels.length) {
											sb.append(",");
										}
									}
								}
								sb.append(" :: Group Ciphers: " + ((WifiConfig) netConfig).getGroupCiphers());
								sb.append(" :: Hardware Mode: " + ((WifiConfig) netConfig).getHardwareMode());
								sb.append(" :: Mode: " + ((WifiConfig) netConfig).getMode());
								sb.append(" :: Pairwise Ciphers: " + ((WifiConfig) netConfig).getPairwiseCiphers());
								sb.append(" :: Passkey: " + ((WifiConfig) netConfig).getPasskey());
								sb.append(" :: Security: " + ((WifiConfig) netConfig).getSecurity());
							} else if(netConfig instanceof ModemConfig) {
								sb.append("\n\tModemConfig ");
								
								sb.append(" :: APN: " + ((ModemConfig) netConfig).getApn());
								sb.append(" :: Data Compression: " + ((ModemConfig) netConfig).getDataCompression());
								sb.append(" :: Dial String: " + ((ModemConfig) netConfig).getDialString());
								sb.append(" :: Header Compression: " + ((ModemConfig) netConfig).getHeaderCompression());
								sb.append(" :: Password: " + ((ModemConfig) netConfig).getPassword());
								sb.append(" :: PPP number: " + ((ModemConfig) netConfig).getPppNumber());
								sb.append(" :: Profile ID: " + ((ModemConfig) netConfig).getProfileID());
								sb.append(" :: Username: " + ((ModemConfig) netConfig).getUsername());
								sb.append(" :: Auth Type: " + ((ModemConfig) netConfig).getAuthType());
								sb.append(" :: IP Address: " + ((ModemConfig) netConfig).getIpAddress());
								sb.append(" :: PDP Type: " + ((ModemConfig) netConfig).getPdpType());
							} else if(netConfig instanceof DhcpServerConfig) {
								sb.append("\n\tDhcpServerConfig ");
								//TODO - finish displaying
							} else if(netConfig instanceof FirewallNatConfig) {
								sb.append("\n\tFirewallNatConfig ");
								//TODO - finish displaying
							} else {
								if(netConfig != null && netConfig.getClass() != null) {
									sb.append("\n\tUNKNOWN CONFIG TYPE???: " + netConfig.getClass().getName());
								} else {
									sb.append("\n\tNULL NETCONFIG PRESENT?!?");
								}
							}
						}
					}
				}
			}
		}
		
		return sb.toString();
	}
	
	// Returns a List of all modified NetInterfaceConfigs, or if none are specified, all NetInterfaceConfigs
	public List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getModifiedNetInterfaceConfigs() {
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = null;
		if(m_modifiedInterfaceNames != null && !m_modifiedInterfaceNames.isEmpty()) {
			netInterfaceConfigs = new ArrayList<NetInterfaceConfig<? extends NetInterfaceAddressConfig>>();
			for(String interfaceName : m_modifiedInterfaceNames) {
				netInterfaceConfigs.add(m_netInterfaceConfigs.get(interfaceName));
			}
		} else {
			netInterfaceConfigs = getNetInterfaceConfigs();
		}
		
		return netInterfaceConfigs;
	}
	
	public List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getNetInterfaceConfigs() {
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = new ArrayList<NetInterfaceConfig<? extends NetInterfaceAddressConfig>>();
		Iterator<String> it = m_netInterfaceConfigs.keySet().iterator();
		while(it.hasNext()) {
			netInterfaceConfigs.add(m_netInterfaceConfigs.get(it.next()));
		}
		return netInterfaceConfigs;
	}
	
	public NetInterfaceConfig<? extends NetInterfaceAddressConfig> getNetInterfaceConfig(String interfaceName) {
	    return m_netInterfaceConfigs.get(interfaceName);
	}
	
	public Map<String,Object> getConfigurationProperties() {
	    if(m_recomputeProperties) {
	        recomputeNetworkProperties();
	        m_recomputeProperties = false;
	    }
	    
		return m_properties;
	}
	
	public boolean isValid() throws KuraException {
		
		//for(NetInterfaceConfig netInterfaceConfig : m_netInterfaceConfigs) {
		Iterator<String> it = m_netInterfaceConfigs.keySet().iterator();
		while(it.hasNext()) {
			NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = m_netInterfaceConfigs.get(it.next());
			
			if(netInterfaceConfig.getMTU() < 0) {
				s_logger.error("MTU must be greater than 0");
				return false;
			}
			
			NetInterfaceType type = netInterfaceConfig.getType();
			if(type != NetInterfaceType.ETHERNET && type != NetInterfaceType.WIFI && type != NetInterfaceType.LOOPBACK) {
				s_logger.error("Type must be ETHERNET, WIFI, or LOOPBACK - type is " + type);
				return false;
			}
			
			List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
			for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
				List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
				
				if(netConfigs != null) {
					for(NetConfig netConfig : netConfigs) {
						if(!netConfig.isValid()) {
							s_logger.error("Invalid config " + netConfig.toString());
							return false;
						}
					}
				}
			}
		}
		
		return true;
	}


	// ---------------------------------------------------------------
	//
	//    Private Methods
	//
	// ---------------------------------------------------------------
	
	private void recomputeNetworkProperties() 
	{
		Map<String,Object> properties = new HashMap<String,Object>();
		
		String netIfPrefix = null;
		String netIfReadOnlyPrefix = null;
		String netIfConfigPrefix = null;
        StringBuilder sbPrefix = null;
		StringBuilder sbInterfaces = new StringBuilder();

		if(m_modifiedInterfaceNames != null && !m_modifiedInterfaceNames.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			
			String prefix = "";
			for (String interfaceName : m_modifiedInterfaceNames) {
			  sb.append(prefix);
			  prefix = ",";
			  sb.append(interfaceName);
			}
			s_logger.debug("Set modified interface names: " + sb.toString());
			properties.put("modified.interface.names", sb.toString());
		}
		
		Iterator<String> it = m_netInterfaceConfigs.keySet().iterator();
		while(it.hasNext()) {
			NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = m_netInterfaceConfigs.get(it.next());
			
			// add the interface to the list of interface found in the platform
			if(sbInterfaces.length() != 0) {
				sbInterfaces.append(",");
			}
			sbInterfaces.append(netInterfaceConfig.getName());

	        // build the prefixes for all the properties associated with this interface
			sbPrefix = new StringBuilder("net.interface.").append(netInterfaceConfig.getName()).append(".");	        
	        netIfReadOnlyPrefix = sbPrefix.toString();
	        netIfPrefix = sbPrefix.append("config.").toString();
	        netIfConfigPrefix = sbPrefix.toString();
	        
			// add the properties of the interface
            properties.put(netIfReadOnlyPrefix+"type",    			netInterfaceConfig.getType().toString());
			properties.put(netIfPrefix+"name",          		  	netInterfaceConfig.getName());
			if(netInterfaceConfig.getState() != null) {
				properties.put(netIfPrefix+"state",			  			netInterfaceConfig.getState().toString());
			}
			properties.put(netIfPrefix+"autoconnect",     			netInterfaceConfig.isAutoConnect());
			properties.put(netIfPrefix+"mtu",             			netInterfaceConfig.getMTU());
			properties.put(netIfReadOnlyPrefix+"driver",          	netInterfaceConfig.getDriver());
			properties.put(netIfReadOnlyPrefix+"driver.version",	netInterfaceConfig.getDriverVersion());
			properties.put(netIfReadOnlyPrefix+"firmware.version",	netInterfaceConfig.getFirmwareVersion());
			properties.put(netIfReadOnlyPrefix+"mac",          		NetUtil.hardwareAddressToString(netInterfaceConfig.getHardwareAddress()));
			properties.put(netIfReadOnlyPrefix+"loopback",          netInterfaceConfig.isLoopback());
			properties.put(netIfReadOnlyPrefix+"ptp",          		netInterfaceConfig.isPointToPoint());
			properties.put(netIfReadOnlyPrefix+"up",          		netInterfaceConfig.isUp());
			properties.put(netIfReadOnlyPrefix+"virtual",			netInterfaceConfig.isVirtual());
			
			// usb
			if(netInterfaceConfig.getUsbDevice() != null) {
			    UsbDevice usbDev = netInterfaceConfig.getUsbDevice();
                properties.put(netIfReadOnlyPrefix+"usb.vendor.id",              usbDev.getVendorId());
                properties.put(netIfReadOnlyPrefix+"usb.vendor.name",            usbDev.getManufacturerName());
			    properties.put(netIfReadOnlyPrefix+"usb.product.id",             usbDev.getProductId());
			    properties.put(netIfReadOnlyPrefix+"usb.product.name",           usbDev.getProductName());
			    properties.put(netIfReadOnlyPrefix+"usb.busNumber",              usbDev.getUsbBusNumber());
			    properties.put(netIfReadOnlyPrefix+"usb.devicePath",             usbDev.getUsbDevicePath());
			}
			
			//custom readonly props for Ethernet and Wifi
			if(netInterfaceConfig instanceof EthernetInterfaceConfigImpl) {
				properties.put(netIfReadOnlyPrefix+"eth.link.up",		((EthernetInterfaceConfigImpl)netInterfaceConfig).isLinkUp());
			} else if(netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
				EnumSet<Capability> capabilities = ((WifiInterfaceConfigImpl)netInterfaceConfig).getCapabilities();
				if(capabilities != null && capabilities.size() > 0) {
					StringBuilder sb = new StringBuilder();
					for(Capability capability : capabilities) {
						sb.append(capability.toString());
						sb.append(",");
					}
					String capabilitiesString = sb.toString();
					capabilitiesString = capabilitiesString.substring(0, capabilitiesString.length() - 1);
					properties.put(netIfReadOnlyPrefix+"wifi.capabilities",			capabilitiesString);
				}
			}
			
	         // add wifi properties
            if(netInterfaceConfig.getType() == NetInterfaceType.WIFI) {
                
                // capabilities
                StringBuilder sbCapabilities = new StringBuilder();
                EnumSet<Capability> capabilities = ((WifiInterface)netInterfaceConfig).getCapabilities();
                if(capabilities != null) {
                    Iterator<Capability> it2 = ((WifiInterface)netInterfaceConfig).getCapabilities().iterator();
                    while(it2.hasNext()) {
                        sbCapabilities.append(it2.next().name()).append(" ");
                    }
                    properties.put(netIfReadOnlyPrefix+"wifi.capabilities", sbCapabilities.toString());
                }
            }
            
            // add modem properties
           if(netInterfaceConfig.getType() == NetInterfaceType.MODEM) {
               String delim;
               
               // revision
               StringBuffer revisionIdBuf = new StringBuffer();
               String[] revisionId = ((ModemInterface<?>)netInterfaceConfig).getRevisionId();
               if(revisionId != null) {
                   delim = null;
                   for(String rev : revisionId) {
                	   if (delim != null) {
                		   revisionIdBuf.append(delim);
                	   }
                       revisionIdBuf.append(rev);
                       delim = ",";
                   }
               }
               
               // technology types
               StringBuffer techTypesBuf = new StringBuffer();
               List<ModemTechnologyType> techTypes = ((ModemInterface<?>)netInterfaceConfig).getTechnologyTypes();
               if(techTypes != null) {
                   delim = null;
                   for(ModemTechnologyType techType : techTypes) {
                	   if (delim != null) {
                		   techTypesBuf.append(delim);
                	   }
                       techTypesBuf.append(techType.toString());
                       delim = ",";
                   }
               }
               
               ModemPowerMode powerMode = ModemPowerMode.UNKNOWN;
               if(((ModemInterface<?>)netInterfaceConfig).getPowerMode() != null) {
                   powerMode = ((ModemInterface<?>)netInterfaceConfig).getPowerMode();
               }
               
               properties.put(netIfReadOnlyPrefix+"manufacturer",           ((ModemInterface<?>)netInterfaceConfig).getManufacturer());
               properties.put(netIfReadOnlyPrefix+"model",                  ((ModemInterface<?>)netInterfaceConfig).getModel());
               properties.put(netIfReadOnlyPrefix+"revisionId",             revisionIdBuf.toString());
               properties.put(netIfReadOnlyPrefix+"serialNum",              ((ModemInterface<?>)netInterfaceConfig).getSerialNumber());
               properties.put(netIfReadOnlyPrefix+"technologyTypes",        techTypesBuf.toString());
               
               properties.put(netIfConfigPrefix+"identifier",               ((ModemInterface<?>)netInterfaceConfig).getModemIdentifier());
               properties.put(netIfConfigPrefix+"powerMode",                powerMode.toString());
               properties.put(netIfConfigPrefix+"pppNum",                   ((ModemInterface<?>)netInterfaceConfig).getPppNum());
               properties.put(netIfConfigPrefix+"poweredOn",                ((ModemInterface<?>)netInterfaceConfig).isPoweredOn());
            }
			
			for (NetInterfaceAddress nia : netInterfaceConfig.getNetInterfaceAddresses()) {
			    String typePrefix = "ip4.";
				if (nia != null) {				    
                    if(nia.getAddress() != null) {
                        properties.put(netIfReadOnlyPrefix+typePrefix+"address", nia.getAddress().getHostAddress());
                    }
                    if(nia.getBroadcast() != null) {
                        properties.put(netIfReadOnlyPrefix+typePrefix+"broadcast", nia.getBroadcast().getHostAddress());
                    }
                    if(nia.getGateway() != null) {
                        properties.put(netIfReadOnlyPrefix+typePrefix+"gateway", nia.getGateway().getHostAddress());
                    }
                    if(nia.getNetmask() != null) {
                        properties.put(netIfReadOnlyPrefix+typePrefix+"netmask", nia.getNetmask().getHostAddress());
                    }
                    if(nia.getNetmask() != null) {
                        properties.put(netIfReadOnlyPrefix+typePrefix+"prefix", Short.valueOf(nia.getNetworkPrefixLength()));
                    }
                    if(nia.getDnsServers() != null) {
                        StringBuilder dnsServers = new StringBuilder();
                        for(IPAddress dnsServer : nia.getDnsServers()) {
                            if(dnsServers.length() != 0) {
                                dnsServers.append(",");
                            }
                            dnsServers.append(dnsServer);
                        }
                        properties.put(netIfReadOnlyPrefix+typePrefix+"dnsServers", dnsServers.toString());
                    }
                    
                    // Wifi interface address
                    if(nia instanceof WifiInterfaceAddress) {
                        // access point
                        WifiAccessPoint wap = ((WifiInterfaceAddress)nia).getWifiAccessPoint();
                        if (wap != null) {
                            /* TODO: need fields to reflect current state?
                            properties.put(sbNetIfPrefix+"wifi.ssid", wap.getSSID());
                            properties.put(sbNetIfPrefix+"wifi.mode", wap.getMode());
                            */
                        }
                        
                        long bitrate = ((WifiInterfaceAddress)nia).getBitrate();
                        properties.put(netIfReadOnlyPrefix+"wifi.bitrate", Long.valueOf(bitrate));
                        
                        WifiMode wifiMode;
                        if(((WifiInterfaceAddress)nia).getMode() != null) {
                            wifiMode = ((WifiInterfaceAddress)nia).getMode();
                        } else {
                            wifiMode = WifiMode.UNKNOWN;
                        }
                        properties.put(netIfPrefix+"wifi.mode", wifiMode.toString());
                    }
                    
                    // Modem interface address
                    if(nia instanceof ModemInterfaceAddress) {
                        if(((ModemInterfaceAddress)nia).getConnectionType() != null) {
                            properties.put(netIfConfigPrefix+"connection.type", ((ModemInterfaceAddress)nia).getConnectionType().toString());
                        }
                        if(((ModemInterfaceAddress)nia).getConnectionStatus() != null) {
                            properties.put(netIfConfigPrefix+"connection.status", ((ModemInterfaceAddress)nia).getConnectionStatus().toString());
                        }
                    }
				}
			}

			// add the properties of the network configurations associated to the interface

			List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig.getNetInterfaceAddresses();
			s_logger.trace("netInterfaceAddressConfigs.size() for " + netInterfaceConfig.getName() + ": " + netInterfaceAddressConfigs.size());

			for(NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
				List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
				
				if(netConfigs != null) {
					s_logger.trace("netConfigs.size(): " + netConfigs.size());
	
					for(NetConfig netConfig : netConfigs) {
						if (netConfig instanceof WifiConfig) {
							s_logger.trace("adding netconfig WifiConfigIP4 for " + netInterfaceConfig.getName());
							addWifiConfigIP4Properties((WifiConfig) netConfig, netIfConfigPrefix, properties);
						} else if (netConfig instanceof ModemConfig) {
						    s_logger.trace("adding netconfig ModemConfig for " + netInterfaceConfig.getName());
                            addModemConfigProperties((ModemConfig) netConfig, netIfConfigPrefix, properties);
						} else if (netConfig instanceof NetConfigIP4) {
							s_logger.trace("adding netconfig NetConfigIP4 for " + netInterfaceConfig.getName());
							addNetConfigIP4Properties((NetConfigIP4) netConfig, netIfConfigPrefix, properties);
	
							/*
							Iterator<String> it2 = properties.keySet().iterator();
							while(it2.hasNext()) {
								String key = it2.next();
								System.out.println("\t\t\t"+key+"="+properties.get(key));
							}*/
						} else if (netConfig instanceof NetConfigIP6) {
							s_logger.trace("adding netconfig NetConfigIP6 for " + netInterfaceConfig.getName());
							addNetConfigIP6Properties((NetConfigIP6) netConfig, netIfConfigPrefix, properties);
	
							/*
							Iterator<String> it = properties.keySet().iterator();
							while(it.hasNext()) {
								String key = it.next();
								System.out.println("\t\t\t"+key+"="+properties.get(key));
							}*/
						} else if (netConfig instanceof DhcpServerConfig4) {
							s_logger.trace("adding netconfig DhcpServerConfig4 for " + netInterfaceConfig.getName());
							addDhcpServerConfig4((DhcpServerConfig4) netConfig, netIfConfigPrefix, properties);
						} else if (netConfig instanceof FirewallNatConfig) {
							s_logger.trace("adding netconfig FirewallNatConfig for " + netInterfaceConfig.getName());
							addFirewallNatConfig((FirewallNatConfig) netConfig, netIfConfigPrefix, properties);
						}
					}
				}
			}
		}
		properties.put("net.interfaces", sbInterfaces.toString());
		
		m_properties = properties;
	}
	
	private void addWifiConfigIP4Properties(WifiConfig wifiConfig,
			String netIfConfigPrefix, 
			Map<String,Object> properties) {
	    
	    WifiMode mode = wifiConfig.getMode();
	    if(mode == null) {
	        s_logger.trace("WifiMode is null - could not add wifiConfig: " + wifiConfig);
	        return;
	    }
	    
		StringBuilder prefix = new StringBuilder(netIfConfigPrefix).append("wifi.").append(mode.toString().toLowerCase());
	    
		int [] channels = wifiConfig.getChannels();
		StringBuffer sbChannel = new StringBuffer();
		if(channels != null) {
			for (int i = 0; i < channels.length; i++) {
				sbChannel.append(channels[i]);
				if (i < (channels.length-1)) {
					sbChannel.append(' ');
				}
			}
		}

		properties.put(prefix+".ssid", wifiConfig.getSSID());
		properties.put(prefix+".driver", wifiConfig.getDriver());
		if(wifiConfig.getMode() != null) {
			properties.put(prefix+".mode", wifiConfig.getMode().toString());
		} else {
			properties.put(prefix+".mode", WifiMode.UNKNOWN.toString());
		}
		if(wifiConfig.getSecurity() != null) {
			properties.put(prefix+".securityType", wifiConfig.getSecurity().toString());
		} else {
			properties.put(prefix+".securityType", WifiSecurity.NONE.toString());
		}
		properties.put(prefix+".channel", sbChannel.toString());
		if(wifiConfig != null && wifiConfig.getPasskey() != null) {
			properties.put(prefix+".passphrase", wifiConfig.getPasskey());
		} else {
			properties.put(prefix+".passphrase", "");
		}
		if(wifiConfig != null && wifiConfig.getHardwareMode() != null) {
			properties.put(prefix+".hardwareMode", wifiConfig.getHardwareMode());
		} else {
			properties.put(prefix+".hardwareMode", "");
		}
		properties.put(prefix+".broadcast", Boolean.valueOf(wifiConfig.getBroadcast()));
		if(wifiConfig.getRadioMode() != null) {
		    properties.put(prefix+".radioMode", wifiConfig.getRadioMode().toString());
		}
		
		if (wifiConfig.getPairwiseCiphers() != null) {
			properties.put(prefix+".pairwiseCiphers", wifiConfig.getPairwiseCiphers().name());
		}
		
		if (wifiConfig.getGroupCiphers() != null) {
			properties.put(prefix+".groupCiphers", wifiConfig.getGroupCiphers().name());
		}
		
		properties.put(prefix+".pingAccessPoint", wifiConfig.pingAccessPoint());
		
		/*
		Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, Object> entry = it.next();
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}*/
	}
	
	private WifiConfig getWifiConfig(String netIfConfigPrefix,
	        WifiMode mode,
	        Map<String, Object> properties) throws KuraException {
	    
	    String key;
	    WifiConfig wifiConfig = new WifiConfig();
	    StringBuilder prefix = new StringBuilder(netIfConfigPrefix).append("wifi.").append(mode.toString().toLowerCase());
	    
	    // mode
	    s_logger.trace("mode is " + mode.toString());
	    wifiConfig.setMode(mode);
        
        // ssid
        key = prefix + ".ssid";
        String ssid = (String)properties.get(key);
        if(ssid == null) {
            ssid = "";
        }
        s_logger.trace("SSID is " + ssid);
        wifiConfig.setSSID(ssid);       
	    
	    // driver
	    key = prefix + ".driver";
	    String driver = (String)properties.get(key);
	    if(driver == null) {
	    	driver = "";
	    }
	    s_logger.trace("driver is " + driver);
	    wifiConfig.setDriver(driver);	    
	    
	    // security
	    key = prefix + ".securityType";
	    WifiSecurity wifiSecurity = WifiSecurity.NONE;
	    String securityString = (String)properties.get(key);
	    s_logger.trace("securityString is " + securityString);
	    if(securityString != null && !securityString.isEmpty()) {
	        try {
	            wifiSecurity = WifiSecurity.valueOf(securityString);
	        } catch (IllegalArgumentException e) {
	            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Could not parse wifi security " + securityString);
	        }
	    }
	    wifiConfig.setSecurity(wifiSecurity);
	    
	    // channels
	    key = prefix + ".channel";
	    String channelsString = (String)properties.get(key);
	    s_logger.trace("channelsString is " + channelsString);
	    if(channelsString != null) {
	    	channelsString = channelsString.trim();
	    	if (channelsString.length() > 0) {
		    	StringTokenizer st = new StringTokenizer(channelsString, " ");
		    	int tokens = st.countTokens();
		    	if (tokens > 0) {
		    		int[] channels = new int[tokens];
					for (int i = 0; i < tokens; i++) {
						String token = st.nextToken();
						try {
							channels[i] = Integer.parseInt(token);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					wifiConfig.setChannels(channels);
		    	}
	    	}
	    }
	    
        // passphrase
        key = prefix + ".passphrase";
        String passphrase = (String)properties.get(key);
        if(passphrase == null) {
            passphrase = "";
        }
	    s_logger.trace("passphrase is " + passphrase);
        wifiConfig.setPasskey(passphrase);       

        // hardware mode
        key = prefix + ".hardwareMode";
        String hwMode = (String)properties.get(key);
        if(hwMode == null) {
            hwMode = "";
        }
	    s_logger.trace("hwMode is " + hwMode);
        wifiConfig.setHardwareMode(hwMode);
        
        // bgscan
        if(mode == WifiMode.INFRA) {
	        key = prefix + ".bgscan";
	        String bgscan = (String)properties.get(key);
	        if(bgscan == null) {
	        	bgscan = "";
	        }
		    s_logger.trace("bgscan is " + bgscan);
	        wifiConfig.setBgscan(new WifiBgscan(bgscan));
	        
	        key = prefix + ".pairwiseCiphers";
	        String pairwiseCiphers = (String)properties.get(key);
	        if (pairwiseCiphers != null) {
	        	wifiConfig.setPairwiseCiphers(WifiCiphers.valueOf(pairwiseCiphers));
	        }
	        
	        key = prefix + ".groupCiphers";
	        String groupCiphers = (String)properties.get(key);
	        if (groupCiphers != null) {
	        	wifiConfig.setGroupCiphers(WifiCiphers.valueOf(groupCiphers));
	        }
	        
	        // ping access point?
	        key = prefix + ".pingAccessPoint";
	        boolean pingAccessPoint = false;
	        if(properties.get(key) != null) {
	            pingAccessPoint = (Boolean)properties.get(key);
	        s_logger.trace("Ping Access Point is {}", pingAccessPoint);
	        } else {
	            s_logger.trace("Ping Access Point is null");
	        }
	        wifiConfig.setPingAccessPoint(pingAccessPoint);
        }
        
        // broadcast
        key = prefix + ".broadcast";
        Boolean broadcast = (Boolean)properties.get(key);
        if(broadcast != null) {
            wifiConfig.setBroadcast(broadcast);
        }
	    s_logger.trace("hwMode is " + hwMode);
	    
	    // radio mode
        key = prefix + ".radioMode";
        WifiRadioMode radioMode;
        String radioModeString = (String)properties.get(key);
        s_logger.trace("radioModeString is " + radioModeString);
        if(radioModeString != null && !radioModeString.isEmpty()) {
            try {
                radioMode = WifiRadioMode.valueOf(radioModeString);
                wifiConfig.setRadioMode(radioMode);
            } catch (IllegalArgumentException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Could not parse wifi radio mode " + radioModeString);
            }
        }
        
	    if(!wifiConfig.isValid()) {
	    	return null;
	    } else {
		    s_logger.trace("Returning wifiConfig: " + wifiConfig);
	    	return wifiConfig;
	    }
	}
	
	private void addModemConfigProperties(ModemConfig modemConfig,
	        String prefix, 
            Map<String, Object> properties) {
	    
        properties.put(prefix+"apn", modemConfig.getApn());
	    properties.put(prefix+"authType", (modemConfig.getAuthType() != null) ? modemConfig.getAuthType().toString() : "");
	    properties.put(prefix+"dataCompression", modemConfig.getDataCompression());
	    properties.put(prefix+"dialString", modemConfig.getDialString());
	    properties.put(prefix+"headerCompression", modemConfig.getHeaderCompression());
	    properties.put(prefix+"ipAddress", (modemConfig.getIpAddress() != null) ? modemConfig.getIpAddress().toString() : "");
	    properties.put(prefix+"password", modemConfig.getPassword());
	    properties.put(prefix+"pdpType", (modemConfig.getPdpType() != null) ? modemConfig.getPdpType().toString() : "");
        properties.put(prefix+"pppNum", modemConfig.getPppNumber());
        properties.put(prefix+"lcpEchoInterval", modemConfig.getLcpEchoInterval());
        properties.put(prefix+"lcpEchoFailure", modemConfig.getLcpEchoFailure());
	    properties.put(prefix+"profileId", modemConfig.getProfileID());
        //properties.put(prefix+"provider", modemConfig.getProvider());
	    properties.put(prefix+"username", modemConfig.getUsername());;
	    properties.put(prefix+"enabled", modemConfig.isEnabled());
	    properties.put(prefix+"gpsEnabled", modemConfig.isGpsEnabled());
	}
	
    private ModemConfig getModemConfig(String prefix,
            Map<String, Object> properties) throws KuraException {
     
        String key;
        ModemConfig modemConfig = new ModemConfig();
        
        // apn
        key = prefix + "apn";
        String apn = (String)properties.get(key);
        s_logger.trace("APN is " + apn);
        modemConfig.setApn(apn);       

        // auth type
        key = prefix + "authType";
        String authTypeString = (String)properties.get(key);
        AuthType authType = AuthType.NONE;
        s_logger.trace("Auth type is " + authTypeString);
        if(authTypeString != null && !authTypeString.isEmpty()) {
            try {
                authType = AuthType.valueOf(authTypeString);
            } catch (IllegalArgumentException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Could not parse auth type " + authTypeString);
            }
        } else {
            s_logger.trace("Auth type is null");
        }
        modemConfig.setAuthType(authType);
        
        // data compression
        key = prefix + "dataCompression";
        if(properties.get(key) != null) {
            int dataCompression = (Integer)properties.get(key);
        s_logger.trace("Data compression is " + dataCompression);
        modemConfig.setDataCompression(dataCompression);     
        } else {
            s_logger.trace("Data compression is null");
        }

        // dial string
        key = prefix + "dialString";
        String dialString = (String)properties.get(key);
        s_logger.trace("Dial string is " + dialString);
        modemConfig.setDialString(dialString);       

        // header compression
        key = prefix + "headerCompression";
        if(properties.get(key) != null) {
            int headerCompression = (Integer)properties.get(key);
        s_logger.trace("Header compression is " + headerCompression);
        modemConfig.setHeaderCompression(headerCompression);       
        } else {
            s_logger.trace("Header compression is null");
        }

        // ip address
        String ipAddressString = (String)properties.get(prefix + "ipAddress");
        IPAddress ipAddress = null;
        s_logger.trace("IP address is " + ipAddressString);
        if(ipAddressString != null && !ipAddressString.isEmpty()) {
            try {
                IP4Address.parseHostAddress(ipAddressString);
            } catch (UnknownHostException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Could not parse ip address " + ipAddressString);
            }
        } else {
            s_logger.trace("IP address is null");
        }
        modemConfig.setIpAddress(ipAddress);       

        // password
        String password = (String)properties.get(prefix + "password");
        s_logger.trace("Password is " + password);
        modemConfig.setPassword(password);       

        // pdp type
        String pdpTypeString = (String)properties.get(prefix + "pdpType");
        PdpType pdpType = PdpType.UNKNOWN;
        if(pdpTypeString != null && !pdpTypeString.isEmpty()) {
            try {
                pdpType = PdpType.valueOf(pdpTypeString);
            } catch (IllegalArgumentException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Could not parse pdp type " + pdpTypeString);
            }
        }
        s_logger.trace("Pdp type is " + pdpTypeString);
        modemConfig.setPdpType(pdpType);       

        // profile id
        key = prefix + "profileId";
        if(properties.get(key) != null) {
            int profileId = (Integer)properties.get(key);
        s_logger.trace("Profile id is " + profileId);
        modemConfig.setProfileID(profileId);       
        } else {
            s_logger.trace("Profile id is null");
        }

        // ppp number
        key = prefix + "pppNum";
        if(properties.get(key) != null) {
            int pppNum = (Integer)properties.get(key);
            s_logger.trace("PPP number is " + pppNum);
            modemConfig.setPppNumber(pppNum);
        } else {
            s_logger.trace("PPP number is null");
        }
        
        // LCP echo interval
        key = prefix + "lcpEchoInterval";
        if(properties.get(key) != null) {
        	int lcpEchoInterval = (Integer)properties.get(key);
        	s_logger.trace("LCP Echo Interval is " + lcpEchoInterval);
        	modemConfig.setLcpEchoInterval(lcpEchoInterval);
        } else {
        	s_logger.trace("LCP Echo Interval  is null");
        }
        
        // LCP echo failure
        key = prefix + "lcpEchoFailure";
        if(properties.get(key) != null) {
        	int lcpEchoFailure = (Integer)properties.get(key);
        	s_logger.trace("LCP Echo Failure is " + lcpEchoFailure);
        	modemConfig.setLcpEchoFailure(lcpEchoFailure);
        } else {
        	s_logger.trace("LCP Echo Failure is null");
        }
        
        // username
        String username = (String)properties.get(prefix + "username");
        s_logger.trace("Username is " + username);
        modemConfig.setUsername(username);       

        // enabled
        key = prefix + "enabled";
        boolean enabled = false;
        if(properties.get(key) != null) {
            enabled = (Boolean)properties.get(key);
        s_logger.trace("Enabled is " + enabled);
        } else {
            s_logger.trace("Enabled is null");
        }
        modemConfig.setEnabled(enabled);      
        
        // GPS enabled
        key = prefix + "gpsEnabled";
        boolean gpsEnabled = false;
        if(properties.get(key) != null) {
            gpsEnabled = (Boolean)properties.get(key);
        s_logger.trace("GPS Enabled is {}", gpsEnabled);
        } else {
            s_logger.trace("GPS Enabled is null");
        }
        modemConfig.setGpsEnabled(gpsEnabled);
        
        return modemConfig;
    }
			
	private void addNetConfigIP4Properties(NetConfigIP4 nc,
			String netIfConfigPrefix, 
			Map<String,Object> properties) {

		properties.put(netIfConfigPrefix+"autoconnect", nc.isAutoConnect());
		properties.put(netIfConfigPrefix+"ip4.status", nc.getStatus().toString());
		
		StringBuilder sbDnsAddresses = new StringBuilder();
        if(nc.getDnsServers() != null) {
            for (IP4Address ip : nc.getDnsServers()) {
                if(sbDnsAddresses.length() != 0) {
                    sbDnsAddresses.append(",");
                }
                sbDnsAddresses.append(ip.getHostAddress());
            }
        }
        properties.put(netIfConfigPrefix+"ip4.dnsServers", sbDnsAddresses.toString());

		if(nc.isDhcp()) {
			properties.put(netIfConfigPrefix+"dhcpClient4.enabled", true);
		} else {
			properties.put(netIfConfigPrefix+"dhcpClient4.enabled", false);
			
			if(nc.getAddress() != null) {
				properties.put(netIfConfigPrefix+"ip4.address", nc.getAddress().getHostAddress());
			} else {
				properties.put(netIfConfigPrefix+"ip4.address", "");
			}
			
			properties.put(netIfConfigPrefix+"ip4.prefix",  nc.getNetworkPrefixLength());
			
			if(nc.getGateway() != null) {
				properties.put(netIfConfigPrefix+"ip4.gateway", nc.getGateway().getHostAddress());
			} else {
				properties.put(netIfConfigPrefix+"ip4.gateway", "");
			}

			StringBuilder sbWinsAddresses = new StringBuilder();
			if(nc.getWinsServers() != null) {
				for (IP4Address ip : nc.getWinsServers()) {
					if(sbWinsAddresses.length() != 0) {
						sbWinsAddresses.append(",");
					}
					sbWinsAddresses.append(ip.getHostAddress());
				}
			}
			properties.put(netIfConfigPrefix+"winsServers", sbWinsAddresses.toString());

			StringBuilder sbDomains = new StringBuilder();
			if(nc.getDomains() != null) {
				for (String domain : nc.getDomains()) {
					if(sbDomains.length() != 0) {
						sbDomains.append(",");
					}
					sbDomains.append(domain);
				}
			}
			properties.put(netIfConfigPrefix+"domains", sbDomains.toString());
		}
	}
	
	private void addNetConfigIP6Properties(NetConfigIP6 nc,
			String netIfConfigPrefix, 
			Map<String,Object> properties) {
		
		properties.put(netIfConfigPrefix+"ip6.status", nc.getStatus().toString());
		
		if(nc.isDhcp()) {
			properties.put(netIfConfigPrefix+"dhcpClient6.enabled", true);
		} else {
			properties.put(netIfConfigPrefix+"dhcpClient6.enabled", false);
			if(nc.getAddress() != null) {
			    properties.put(netIfConfigPrefix+"address", nc.getAddress().getHostAddress());
			}

			StringBuilder sbDnsAddresses = new StringBuilder();
			for (IP6Address ip : nc.getDnsServers()) {
				if(sbDnsAddresses.length() != 0) {
					sbDnsAddresses.append(",");
				}
				sbDnsAddresses.append(ip.getHostAddress());
			}
			properties.put(netIfConfigPrefix+"ip6.dnsServers", sbDnsAddresses.toString());

			StringBuilder sbDomains = new StringBuilder();
			for (String domain : nc.getDomains()) {
				if(sbDomains.length() != 0) {
					sbDomains.append(",");
				}
				sbDomains.append(domain);
			}
			properties.put(netIfConfigPrefix+"domains", sbDomains.toString());
		}
	}
	
	private void addDhcpServerConfig4(DhcpServerConfig4 nc,
			String netIfConfigPrefix, 
			Map<String,Object> properties) {
		
		/*
		 * .config.dhcpServer4.defaultLeaseTime
		 * .config.dhcpServer4.maxLeaseTime
		 * .config.dhcpServer4.prefix
		 * .config.dhcpServer4.rangeStart
		 * .config.dhcpServer4.rangeEnd
		 * .config.dhcpServer4.passDns
		 */

		properties.put(netIfConfigPrefix+"dhcpServer4.enabled", nc.isEnabled());
		properties.put(netIfConfigPrefix+"dhcpServer4.defaultLeaseTime", nc.getDefaultLeaseTime());
		properties.put(netIfConfigPrefix+"dhcpServer4.maxLeaseTime", nc.getMaximumLeaseTime());
		properties.put(netIfConfigPrefix+"dhcpServer4.prefix", nc.getPrefix());
		properties.put(netIfConfigPrefix+"dhcpServer4.rangeStart", nc.getRangeStart().toString());
		properties.put(netIfConfigPrefix+"dhcpServer4.rangeEnd", nc.getRangeEnd().toString());
		properties.put(netIfConfigPrefix+"dhcpServer4.passDns", nc.isPassDns());
		
	}
	
	private void addFirewallNatConfig(FirewallNatConfig nc,
			String netIfConfigPrefix, 
			Map<String,Object> properties) {
		
		/*
		 * .config.nat.enabled
		 */
		
		properties.put(netIfConfigPrefix+"nat.enabled", true);
	}

	private void addInterfaceConfiguration(String interfaceName, NetInterfaceType type,
			Map<String,Object> props)
		throws UnknownHostException, KuraException
	{
		if(type == null) {
			s_logger.error("Null type for " + interfaceName);
			return;
		}
		
        switch(type) {
        case LOOPBACK:
            LoopbackInterfaceConfigImpl loopbackInterfaceConfig = new LoopbackInterfaceConfigImpl(interfaceName);            
            List<NetInterfaceAddressConfig> loopbackInterfaceAddressConfigs = new ArrayList<NetInterfaceAddressConfig>();                            
            loopbackInterfaceAddressConfigs.add(new NetInterfaceAddressConfigImpl());
            loopbackInterfaceConfig.setNetInterfaceAddresses(loopbackInterfaceAddressConfigs);
            
            this.populateNetInterfaceConfiguration(loopbackInterfaceConfig, props);
            
            m_netInterfaceConfigs.put(interfaceName, loopbackInterfaceConfig);
            break;
        case ETHERNET:
            EthernetInterfaceConfigImpl ethernetInterfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
            List<NetInterfaceAddressConfig> ethernetInterfaceAddressConfigs = new ArrayList<NetInterfaceAddressConfig>();
            ethernetInterfaceAddressConfigs.add(new NetInterfaceAddressConfigImpl());
            ethernetInterfaceConfig.setNetInterfaceAddresses(ethernetInterfaceAddressConfigs);
            
            this.populateNetInterfaceConfiguration(ethernetInterfaceConfig, props);
            
            m_netInterfaceConfigs.put(interfaceName, ethernetInterfaceConfig);            
            break;
        case WIFI:
            WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
            
            List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = new ArrayList<WifiInterfaceAddressConfig>();
            wifiInterfaceAddressConfigs.add(new WifiInterfaceAddressConfigImpl());
            wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);

            this.populateNetInterfaceConfiguration(wifiInterfaceConfig, props);
            
            m_netInterfaceConfigs.put(interfaceName, wifiInterfaceConfig);            
            break;
        case MODEM:
            ModemInterfaceConfigImpl modemInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);

            List<ModemInterfaceAddressConfig> modemInterfaceAddressConfigs = new ArrayList<ModemInterfaceAddressConfig>();
            modemInterfaceAddressConfigs.add(new ModemInterfaceAddressConfigImpl());
            modemInterfaceConfig.setNetInterfaceAddresses(modemInterfaceAddressConfigs);
            
            this.populateNetInterfaceConfiguration(modemInterfaceConfig, props);
            
            m_netInterfaceConfigs.put(interfaceName, modemInterfaceConfig);            
            break;
        case UNKNOWN:
            s_logger.trace("Found interface of unknown type in current configuration: " + interfaceName);
            break;
/*
        default:
            
		switch (type) {
		case ETHERNET:
			addEthernetConfiguration(interfaceName, props);
			break;
		case WIFI:
			addWifiConfiguration(interfaceName, props); 
			break;
*/
		default:
			s_logger.error("Unsupported type " + type.toString() + " for interface " + interfaceName);
			break;
		}
	}
	
	
	private void populateNetInterfaceConfiguration(
	        AbstractNetInterface<? extends NetInterfaceAddressConfig> netInterfaceConfig,
	        Map<String, Object> props) throws UnknownHostException, KuraException
    {
	    String interfaceName = netInterfaceConfig.getName();

	    StringBuffer keyBuffer = new StringBuffer();
	    keyBuffer.append("net.interface.").append(interfaceName).append(".type");
	    NetInterfaceType interfaceType = NetInterfaceType.valueOf((String) props.get(keyBuffer.toString()));
	    s_logger.trace("Populating interface: " + interfaceName + " of type " + interfaceType);
        
	    
        // build the prefixes for all the properties associated with this interface
        StringBuilder sbPrefix = new StringBuilder();
        sbPrefix.append("net.interface.").append(interfaceName).append(".");
        
        String netIfReadOnlyPrefix = sbPrefix.toString();
        String netIfPrefix = sbPrefix.append("config.").toString();
        String netIfConfigPrefix = sbPrefix.toString();
        
        //[RO] State
        String stateConfig = netIfReadOnlyPrefix + "state";
        if (props.containsKey(stateConfig)) {
            try {
                NetInterfaceState state = (NetInterfaceState) props.get(stateConfig);
                s_logger.trace("got state: " + state);
                netInterfaceConfig.setState(state);
            }
            catch (Exception e) {
                s_logger.error("Could not process State configuration. Retaining current value.", e);
            }
        }
        
        // Auto connect
        boolean autoConnect = false;
        String autoConnectKey = netIfPrefix + "autoconnect";
        if (props.containsKey(autoConnectKey)) {
            autoConnect = (Boolean) props.get(autoConnectKey);
            s_logger.trace("got autoConnect: " + autoConnect);
            netInterfaceConfig.setAutoConnect(autoConnect);
        }
        
        // MTU
        String mtuConfig = netIfPrefix + "mtu";
        if (props.containsKey(mtuConfig)) {
            int mtu = (Integer) props.get(mtuConfig);
            s_logger.trace("got MTU: " + mtu);
            netInterfaceConfig.setMTU(mtu);
        }
        
        // Driver
        String driverKey = netIfReadOnlyPrefix + "driver";
        if (props.containsKey(driverKey)) {
            String driver = (String) props.get(driverKey);
            s_logger.trace("got Driver: " + driver);
            netInterfaceConfig.setDriver(driver);
        }
        
        // Driver Version
        String driverVersionKey = netIfReadOnlyPrefix + "driver.version";
        if (props.containsKey(driverVersionKey)) {
            String driverVersion = (String) props.get(driverVersionKey);
            s_logger.trace("got Driver Version: " + driverVersion);
            netInterfaceConfig.setDriverVersion(driverVersion);
        }
        
        // Firmware Version
        String firmwardVersionKey = netIfReadOnlyPrefix + "firmware.version";
        if (props.containsKey(firmwardVersionKey)) {
            String firmwareVersion = (String) props.get(firmwardVersionKey);
            s_logger.trace("got Firmware Version: " + firmwareVersion);
            netInterfaceConfig.setFirmwareVersion(firmwareVersion);
        }
        
        // Mac Address
        String macAddressKey = netIfReadOnlyPrefix + "mac";
        if (props.containsKey(macAddressKey)) {
            String macAddress = (String) props.get(macAddressKey);
            s_logger.trace("got Mac Address: " + macAddress);
            netInterfaceConfig.setHardwareAddress(NetUtil.hardwareAddressToBytes(macAddress));
        }
        
        // Is Loopback
        String loopbackKey = netIfReadOnlyPrefix + "loopback";
        if (props.containsKey(loopbackKey)) {
            Boolean isLoopback = (Boolean) props.get(loopbackKey);
            s_logger.trace("got Is Loopback: " + isLoopback);
            netInterfaceConfig.setLoopback(isLoopback);
        }
        
        // Is Point to Point
        String ptpKey = netIfReadOnlyPrefix + "ptp";
        if (props.containsKey(ptpKey)) {
            Boolean isPtp = (Boolean) props.get(ptpKey);
            s_logger.trace("got Is PtP: " + isPtp);
            netInterfaceConfig.setPointToPoint(isPtp);
        }
        
        // Is Up
        String upKey = netIfReadOnlyPrefix + "up";
        if (props.containsKey(upKey)) {
            Boolean isUp = (Boolean) props.get(upKey);
            s_logger.trace("got Is Up: " + isUp);
            netInterfaceConfig.setUp(isUp);
            
            if(isUp) {
            	netInterfaceConfig.setState(NetInterfaceState.ACTIVATED);
            } else {
            	netInterfaceConfig.setState(NetInterfaceState.DISCONNECTED);
            }
        } else {
        	s_logger.trace("Setting state to");
        	netInterfaceConfig.setState(NetInterfaceState.DISCONNECTED);
        }
        
        // Is Virtual
        String virtualKey = netIfReadOnlyPrefix + "virtual";
        if (props.containsKey(virtualKey)) {
            Boolean isVirtual = (Boolean) props.get(virtualKey);
            s_logger.trace("got Is Virtual: " + isVirtual);
            netInterfaceConfig.setVirtual(isVirtual);
        }
        
        // USB
        String vendorId = (String)props.get(netIfReadOnlyPrefix+"usb.vendor.id");
        String vendorName = (String)props.get(netIfReadOnlyPrefix+"usb.vendor.name");
        String productId = (String)props.get(netIfReadOnlyPrefix+"usb.product.id");
        String productName = (String)props.get(netIfReadOnlyPrefix+"usb.product.name");
        String usbBusNumber = (String)props.get(netIfReadOnlyPrefix+"usb.busNumber");
        String usbDevicePath = (String)props.get(netIfReadOnlyPrefix+"usb.devicePath");
        
        if ((vendorId != null) && (productId != null)) {
	        UsbDevice usbDevice = new UsbNetDevice(vendorId, productId, vendorName, productName, usbBusNumber, usbDevicePath, interfaceName);
	        s_logger.trace("adding usbDevice: " + usbDevice + ", port: " + usbDevice.getUsbPort());	  
	        netInterfaceConfig.setUsbDevice(usbDevice);
        }
   		
   		if(netInterfaceConfig instanceof EthernetInterfaceConfigImpl) {
   			// Is Up
   	        String linkUpKey = netIfReadOnlyPrefix + "eth.link.up";
   	        if (props.containsKey(linkUpKey)) {
   	         Boolean linkUp = (Boolean) props.get(linkUpKey);
   	            s_logger.trace("got Is Link Up: " + linkUp);
   	            ((EthernetInterfaceConfigImpl)netInterfaceConfig).setLinkUp(linkUp);
   	        }
   		} else if(netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
   			// Wifi Capabilities
   	        String capabilitiesKey = netIfReadOnlyPrefix + "wifi.capabilities";
   	        if (props.containsKey(capabilitiesKey)) {
   	        	String capabilitiesString = (String) props.get(capabilitiesKey);
   	        	if(capabilitiesString != null) {
   	        		String[] capabilities = capabilitiesString.split(" ");
   	        		if(capabilities != null && capabilities.length > 0) {
   	        			EnumSet<Capability> capabilitiesEnum = EnumSet.noneOf(Capability.class);
   	        			for(String capability : capabilities) {
   	        			    if(capability != null && !capability.isEmpty()) {
   	        			        capabilitiesEnum.add(Capability.valueOf(capability));
   	        			    }
   	        			}
   	        			((WifiInterfaceConfigImpl)netInterfaceConfig).setCapabilities(capabilitiesEnum);
   	        		}
   	        	}
   	        }
   		} else if(netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
   		    ModemInterfaceConfigImpl modemInterfaceConfig = (ModemInterfaceConfigImpl) netInterfaceConfig;
   		    String key;
            
            // manufacturer
            key = netIfReadOnlyPrefix+"manufacturer";
            if(props.containsKey(key)) {
                modemInterfaceConfig.setManufacturer((String)props.get(key));
            }
   		    
   		    // manufacturer
   		    key = netIfReadOnlyPrefix+"model";
   		    if(props.containsKey(key)) {
                modemInterfaceConfig.setModel((String)props.get(key));
   		    }
   		    
   		    // revision id
   		    key = netIfReadOnlyPrefix+"revisionId";
   		    if(props.containsKey(key)) {
       		    String revisionIdString = (String)props.get(key);
                modemInterfaceConfig.setRevisionId(revisionIdString.split(","));
   		    }
   		                
            // serial number
            key = netIfReadOnlyPrefix+"serialNum";
            if(props.containsKey(key)) {
                modemInterfaceConfig.setSerialNumber((String)props.get(key));
            }
   		    
   		    // technology types
            key = netIfReadOnlyPrefix+"technologyTypes";
            if(props.containsKey(key)) {
       		    ArrayList<ModemTechnologyType> technologyTypes = new ArrayList<ModemTechnologyType>();
       		    String techTypesString = (String)props.get(netIfReadOnlyPrefix+"technologyTypes");
       		    if(techTypesString != null && !techTypesString.isEmpty()) {
       		        for(String techTypeString : techTypesString.split(",")) {
       		            if(techTypeString != null && !techTypeString.isEmpty()) {
       		                try{
           		                ModemTechnologyType modemTechType = ModemTechnologyType.valueOf(techTypeString);
           		                technologyTypes.add(modemTechType);
       		                } catch (IllegalArgumentException e) {
       		                    s_logger.error("Could not parse type " + techTypeString);
       		                }
       		            }
       		        }
       	            modemInterfaceConfig.setTechnologyTypes(technologyTypes);
       		    }
            }

            // modem identifier
            key = netIfConfigPrefix+"identifier";
            if(props.containsKey(key)) {
                modemInterfaceConfig.setModemIdentifier((String)props.get(key));
            }
   		    
   		    // power mode
            key = netIfConfigPrefix+"powerMode";
            if(props.containsKey(key)) {
       		    ModemPowerMode powerMode = ModemPowerMode.UNKNOWN;
       		    String modemPowerModeString = (String)props.get(netIfConfigPrefix+"powerMode");
       		    if(modemPowerModeString != null) {
       		        powerMode = ModemPowerMode.valueOf(modemPowerModeString);
                    modemInterfaceConfig.setPowerMode(powerMode);
       		    }
            }
            
            // ppp number
            key = netIfConfigPrefix+"pppNum";
            if(props.containsKey(key)) {
                if(props.get(key) != null) {
                    modemInterfaceConfig.setPppNum((Integer)props.get(key));
                }
            }
            
            // powered on
            key = netIfConfigPrefix+"poweredOn";
            if(props.containsKey(key)) {
                if(props.get(key) != null) {
                    modemInterfaceConfig.setPoweredOn((Boolean)props.get(key));
                }
            }
   		}
        
        
        // Status
        String configStatus4 = null;
        String configStatus4Key = "net.interface." + interfaceName+".config.ip4.status";
        if (props.containsKey(configStatus4Key)) {
            configStatus4 = (String) props.get(configStatus4Key);
        }
        if(configStatus4 == null) {
            configStatus4 = NetInterfaceStatus.netIPv4StatusDisabled.name();
        }
        s_logger.trace("Status Ipv4? " + configStatus4);
        
        String configStatus6 = null;
        String configStatus6Key = "net.interface." + interfaceName+".config.ip6.status";
        if (props.containsKey(configStatus6Key)) {
            configStatus6 = (String) props.get(configStatus6Key);
        }
        if(configStatus6 == null) {
            configStatus6 = NetInterfaceStatus.netIPv6StatusDisabled.name();
        }


        // POPULATE NetInterfaceAddresses
        for(NetInterfaceAddressConfig netInterfaceAddress : netInterfaceConfig.getNetInterfaceAddresses()) {

            List<NetConfig> netConfigs = new ArrayList<NetConfig>();            
            if(netInterfaceAddress instanceof NetInterfaceAddressConfigImpl) {
                ((NetInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(netConfigs);
            } else if (netInterfaceAddress instanceof WifiInterfaceAddressConfigImpl) {
                ((WifiInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(netConfigs);
            } else if (netInterfaceAddress instanceof ModemInterfaceAddressConfigImpl) {
                ((ModemInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(netConfigs);
            }
            
            
            // Common NetInterfaceAddress
            if(netInterfaceAddress instanceof NetInterfaceAddressImpl) {
                s_logger.trace("netInterfaceAddress is instanceof NetInterfaceAddressImpl");
                NetInterfaceAddressImpl netInterfaceAddressImpl = (NetInterfaceAddressImpl) netInterfaceAddress;

                String addressType = ".ip4";       // TODO: determine dynamically
                
                // populate current address status
                String key = "net.interface." + interfaceName + addressType + ".address";
                if(props.containsKey(key)) {
                    IPAddress address = IP4Address.parseHostAddress((String)props.get(key));
                    s_logger.trace("got " + key + ": " + address);
                    netInterfaceAddressImpl.setAddress(address);
                }
    
                key = "net.interface." + interfaceName + addressType + ".broadcast";
                if(props.containsKey(key)) {
                    IPAddress broadcast = IP4Address.parseHostAddress((String)props.get(key));
                    s_logger.trace("got " + key + ": " + broadcast);
                    netInterfaceAddressImpl.setBroadcast(broadcast);
                }
                
                key = "net.interface." + interfaceName + addressType + ".dnsServers";
                if(props.containsKey(key)) {
                    List<IPAddress> dnsServers = new ArrayList<IPAddress>();
                    String dnsServersString = (String) props.get(key);
                    s_logger.trace("got " + key + ": " + dnsServersString);
                    for(String dnsServer : dnsServersString.split(",")) {
                        dnsServers.add(IP4Address.parseHostAddress(dnsServer));
                    }
                    netInterfaceAddressImpl.setDnsServers(dnsServers);
                }
    
                key = "net.interface." + interfaceName + addressType + ".gateway";
                if(props.containsKey(key)) {
                	if(props.get(key) != null && !((String)props.get(key)).trim().equals("")) {
                		IPAddress gateway = IP4Address.parseHostAddress((String)props.get(key));
                		s_logger.trace("got " + key + ": " + gateway);
                		netInterfaceAddressImpl.setGateway(gateway);
                	} else {
                		s_logger.trace("got " + key + ": null");
                		netInterfaceAddressImpl.setGateway(null);
                	}
                }
    
                key = "net.interface." + interfaceName + addressType + ".netmask";
                if(props.containsKey(key)) {
                    IPAddress netmask = IP4Address.parseHostAddress((String)props.get(key));
                    s_logger.trace("got " + key + ": " + netmask);
                    netInterfaceAddressImpl.setBroadcast(netmask);
                }
    
                key = "net.interface." + interfaceName + addressType + ".prefix";
                if(props.containsKey(key)) {
                    Short prefix = (Short) props.get(key);
                    s_logger.trace("got " + key + ": " + prefix);
                    netInterfaceAddressImpl.setNetworkPrefixLength(prefix);
                }                
            }

            // WifiInterfaceAddress
            if(netInterfaceAddress instanceof WifiInterfaceAddressImpl) {
                s_logger.trace("netInterfaceAddress is instanceof WifiInterfaceAddressImpl");
                WifiInterfaceAddressImpl wifiInterfaceAddressImpl = (WifiInterfaceAddressImpl) netInterfaceAddress;
                
                // wifi mode
                String configWifiMode = netIfPrefix + "wifi.mode";
                if (props.containsKey(configWifiMode)) {
                    
                    WifiMode mode = WifiMode.INFRA;     // FIXME: INFRA for now while debugging - probably want this as UNKNOWN
                    if(props.get(configWifiMode) != null) {
                        mode = WifiMode.valueOf((String) props.get(configWifiMode));
                    }
                    
                    s_logger.trace("Adding wifiMode: " + mode);
                    wifiInterfaceAddressImpl.setMode(mode);
                }
            }
            
            // ModemInterfaceAddress
            if(netInterfaceAddress instanceof ModemInterfaceAddressConfigImpl) {
                s_logger.trace("netInterfaceAddress is instanceof ModemInterfaceAddressConfigImpl");
                ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = (ModemInterfaceAddressConfigImpl) netInterfaceAddress;
                
                // connection type
                String configConnType = netIfPrefix + "connection.type";
                if (props.containsKey(configConnType)) {
                    ModemConnectionType connType = ModemConnectionType.PPP;
                    String connTypeStr = (String)props.get(configConnType); 
                    if(connTypeStr != null && !connTypeStr.isEmpty()) {
                        connType = ModemConnectionType.valueOf(connTypeStr);
                    }
                    
                    s_logger.trace("Adding modem connection type: " + connType);
                    modemInterfaceAddressImpl.setConnectionType(connType);
                }
                
                // connection type
                String configConnStatus = netIfPrefix + "connection.status";
                if (props.containsKey(configConnStatus)) {
                    ModemConnectionStatus connStatus = ModemConnectionStatus.UNKNOWN;
                    String connStatusStr = (String)props.get(configConnStatus);
                    if(connStatusStr != null && !connStatusStr.isEmpty()) {
                        connStatus = ModemConnectionStatus.valueOf(connStatusStr);
                    }
                    
                    s_logger.trace("Adding modem connection status: " + connStatus);
                    modemInterfaceAddressImpl.setConnectionStatus(connStatus);
                }                
            }
            
            

            // POPULATE NetConfigs
            
            // dhcp4
            String configDhcp4 = "net.interface." + interfaceName+".config.dhcpClient4.enabled";
            NetConfigIP4 netConfigIP4 = null;
            boolean dhcpEnabled = false;
            if (props.containsKey(configDhcp4)) {
                dhcpEnabled = (Boolean) props.get(configDhcp4);
                s_logger.trace("DHCP 4 enabled? " + dhcpEnabled);
            }
            
            netConfigIP4 = new NetConfigIP4(NetInterfaceStatus.valueOf(configStatus4), autoConnect, dhcpEnabled);
            netConfigs.add(netConfigIP4);
            
            if(!dhcpEnabled) {
                // NetConfigIP4
                String configIp4 = "net.interface." + interfaceName+".config.ip4.address";
                if (props.containsKey(configIp4)) {
                	s_logger.trace("got " + configIp4 + ": " + props.get(configIp4));
                                        
                    // address
                    String addressIp4 = (String) props.get(configIp4);
                    s_logger.trace("IPv4 address: " + addressIp4);
                    if(addressIp4 != null && !addressIp4.isEmpty()) {
                        IP4Address ip4Address = (IP4Address) IPAddress.parseHostAddress(addressIp4);
                        netConfigIP4.setAddress(ip4Address);
                    }
    
                    // prefix
                    String configIp4Prefix = "net.interface." + interfaceName+".config.ip4.prefix";
                    short networkPrefixLength = -1;
                    if (props.containsKey(configIp4Prefix)) {
                        if(props.get(configIp4Prefix) instanceof Short) {
                            networkPrefixLength = (Short) props.get(configIp4Prefix);
                        } else if(props.get(configIp4Prefix) instanceof String) {
                            networkPrefixLength = Short.parseShort((String) props.get(configIp4Prefix));
                        }
                        
                        try {
                            netConfigIP4.setNetworkPrefixLength(networkPrefixLength);
                        } catch (KuraException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                        /*
                        s_logger.trace("IPv4 prefix: " + networkPrefixLength);
                        netInterfaceAddress.setNetworkPrefixLength(networkPrefixLength);
                        //FIXME - hack for now
                        netInterfaceAddress.setBroadcast((IP4Address) IPAddress.parseHostAddress("192.168.1.255"));
                        ip4Config.setNetworkPrefixLength(networkPrefixLength);
                        */
                    }
                    
                    // gateway
                    String configIp4Gateway = "net.interface." + interfaceName+".config.ip4.gateway";
                    if (props.containsKey(configIp4Gateway)) {
    
                        String gatewayIp4 = (String) props.get(configIp4Gateway);
                        s_logger.trace("IPv4 gateway: " + gatewayIp4);
                        if(gatewayIp4 != null && !gatewayIp4.isEmpty()) {
                            IP4Address ip4Gateway = (IP4Address) IPAddress.parseHostAddress(gatewayIp4);
                            netConfigIP4.setGateway(ip4Gateway);
                        }
                    }
                }
            }

            // dns servers
            String configDNSs = "net.interface." + interfaceName+".config.ip4.dnsServers";
            if (props.containsKey(configDNSs)) {
                
                List<IP4Address> dnsIPs = new ArrayList<IP4Address>();
                String dnsAll = (String) props.get(configDNSs);
                String[] dnss = dnsAll.split(",");
                for (String dns : dnss) {
                	if (dns != null && dns.length() > 0) {
	                    s_logger.trace("IPv4 DNS: " + dns);
	                    IP4Address dnsIp4 = (IP4Address) IPAddress.parseHostAddress(dns);
	                    dnsIPs.add(dnsIp4);
                	}
                }
                netConfigIP4.setDnsServers(dnsIPs);
            }

            // win servers
            String configWINSs = "net.interface." + interfaceName+".config.ip4.winsServers";
            if (props.containsKey(configWINSs)) {
                
                List<IP4Address> winsIPs = new ArrayList<IP4Address>();
                String winsAll = (String) props.get(configWINSs);
                String[] winss = winsAll.split(",");
                for (String wins : winss) {
                    s_logger.trace("WINS: " + wins);
                    IP4Address winsIp4 = (IP4Address) IPAddress.parseHostAddress(wins);
                    winsIPs.add(winsIp4);
                }
                netConfigIP4.setWinsServers(winsIPs);
            }
            
            // domains
            String configDomains = "net.interface." + interfaceName + ".config.ip4.domains";
            if (props.containsKey(configDomains)) {
                
                List<String> domainNames = new ArrayList<String>();
                String domainsAll = (String) props.get(configDomains);
                String[] domains = domainsAll.split(",");
                for (String domain : domains) {
                    s_logger.trace("IPv4 Domain: " + domain);
                    domainNames.add(domain);
                }
                netConfigIP4.setDomains(domainNames);
            }
            
            // FirewallNatConfig - see if NAT is enabled
            String configNatEnabled = "net.interface." + interfaceName + ".config.nat.enabled";
            if (props.containsKey(configNatEnabled)) {
                boolean natEnabled = (Boolean) props.get(configNatEnabled);
                s_logger.trace("NAT enabled? " + natEnabled);
                
                if(natEnabled) {
                    FirewallNatConfig natConfig = new FirewallNatConfig(interfaceName, "unknown", true);
                    netConfigs.add(natConfig);
                }
            }
            
            // DhcpServerConfigIP4 - see if there is a DHCP 4 Server
            String configDhcpServerEnabled = "net.interface." + interfaceName + ".config.dhcpServer4.enabled";
            if (props.containsKey(configDhcpServerEnabled)) {
                boolean dhcpServerEnabled = (Boolean) props.get(configDhcpServerEnabled);
                s_logger.trace("DHCP Server 4 enabled? " + dhcpServerEnabled);
                
                IP4Address subnet = null;
                IP4Address routerAddress = (dhcpEnabled) ? (IP4Address)netInterfaceAddress.getAddress() : netConfigIP4.getAddress();
                IP4Address subnetMask = null;
                int defaultLeaseTime = -1;
                int maximumLeaseTime = -1;
                short prefix = -1;
                IP4Address rangeStart = null;
                IP4Address rangeEnd = null;
                boolean passDns = false;
                List<IP4Address> dnServers = new ArrayList<IP4Address>();   
                
                // prefix
                String configDhcpServerPrefix = "net.interface." + interfaceName+".config.dhcpServer4.prefix";
                if (props.containsKey(configDhcpServerPrefix)) {
                    if(props.get(configDhcpServerPrefix) instanceof Short) {
                        prefix = (Short) props.get(configDhcpServerPrefix);
                    } else if(props.get(configDhcpServerPrefix) instanceof String) {
                        prefix = Short.parseShort((String) props.get(configDhcpServerPrefix));
                    }
                    s_logger.trace("DHCP Server prefix: " + prefix);
                }
                
                // rangeStart
                String configDhcpServerRangeStart = "net.interface." + interfaceName+".config.dhcpServer4.rangeStart";
                if (props.containsKey(configDhcpServerRangeStart)) {
                    String dhcpServerRangeStart = (String) props.get(configDhcpServerRangeStart);
                    s_logger.trace("DHCP Server Range Start: " + dhcpServerRangeStart);
                    if(dhcpServerRangeStart != null && !dhcpServerRangeStart.isEmpty()) {
                        rangeStart = (IP4Address) IPAddress.parseHostAddress(dhcpServerRangeStart);
                    }
                }
                
                // rangeEnd
                String configDhcpServerRangeEnd = "net.interface." + interfaceName+".config.dhcpServer4.rangeEnd";
                if (props.containsKey(configDhcpServerRangeEnd)) {
                    String dhcpServerRangeEnd = (String) props.get(configDhcpServerRangeEnd);
                    s_logger.trace("DHCP Server Range End: " + dhcpServerRangeEnd);
                    if(dhcpServerRangeEnd != null && !dhcpServerRangeEnd.isEmpty()) {
                        rangeEnd = (IP4Address) IPAddress.parseHostAddress(dhcpServerRangeEnd);
                    }
                }
                
                // default lease time
                String configDhcpServerDefaultLeaseTime = "net.interface." + interfaceName+".config.dhcpServer4.defaultLeaseTime";
                if (props.containsKey(configDhcpServerDefaultLeaseTime)) {
                    if(props.get(configDhcpServerDefaultLeaseTime) instanceof Integer) {
                        defaultLeaseTime = (Integer) props.get(configDhcpServerDefaultLeaseTime);
                    } else if(props.get(configDhcpServerDefaultLeaseTime) instanceof String) {
                        defaultLeaseTime = Integer.parseInt((String) props.get(configDhcpServerDefaultLeaseTime));
                    }
                    s_logger.trace("DHCP Server Default Lease Time: " + defaultLeaseTime);
                }
                
                // max lease time
                String configDhcpServerMaxLeaseTime = "net.interface." + interfaceName+".config.dhcpServer4.maxLeaseTime";
                if (props.containsKey(configDhcpServerMaxLeaseTime)) {
                    if(props.get(configDhcpServerMaxLeaseTime) instanceof Integer) {
                        maximumLeaseTime = (Integer) props.get(configDhcpServerMaxLeaseTime);
                    } else if(props.get(configDhcpServerMaxLeaseTime) instanceof String) {
                        maximumLeaseTime = Integer.parseInt((String) props.get(configDhcpServerMaxLeaseTime));
                    }
                    s_logger.trace("DHCP Server Maximum Lease Time: " + maximumLeaseTime);
                }
                
                // passDns
                String configDhcpServerPassDns = "net.interface." + interfaceName+".config.dhcpServer4.passDns";
                if (props.containsKey(configDhcpServerPassDns)) {
                    if(props.get(configDhcpServerPassDns) instanceof Boolean) {
                        passDns = (Boolean) props.get(configDhcpServerPassDns);
                    } else if(props.get(configDhcpServerPassDns) instanceof String) {
                        passDns = Boolean.parseBoolean((String) props.get(configDhcpServerPassDns));
                    }
                    s_logger.trace("DHCP Server Pass DNS?: " + passDns);
                }
                
                if(routerAddress != null && rangeStart != null && rangeEnd != null) {
                    //get the netmask and subnet
                    int prefixInt = (int)prefix;
                    int mask = ~((1 << (32 - prefixInt)) - 1);                  
                    String subnetMaskString = NetworkUtil.dottedQuad(mask);
                    String subnetString =  NetworkUtil.calculateNetwork(routerAddress.getHostAddress(), subnetMaskString);
                    subnet = (IP4Address) IPAddress.parseHostAddress(subnetString);
                    subnetMask = (IP4Address) IPAddress.parseHostAddress(subnetMaskString);
                    
                    dnServers.add(routerAddress);
                    
                    DhcpServerConfigIP4 dhcpServerConfig = new DhcpServerConfigIP4(interfaceName, dhcpServerEnabled, subnet, routerAddress, subnetMask, defaultLeaseTime,
                            maximumLeaseTime, prefix, rangeStart, rangeEnd, passDns, dnServers);
                    netConfigs.add(dhcpServerConfig);
                } else {
                    s_logger.trace("Not including DhcpServerConfig - router: " + routerAddress + ", range start: " + rangeStart + ", range end: " + rangeEnd);
                }
            }

            // dhcp6
            String configDhcp6 = "net.interface." + interfaceName + ".config.dhcpClient6.enabled";
            NetConfigIP6 netConfigIP6 = null;
            boolean dhcp6Enabled = false;
            if (props.containsKey(configDhcp6)) {
                dhcp6Enabled = (Boolean) props.get(configDhcp6);
                s_logger.trace("DHCP 6 enabled? " + dhcp6Enabled);
            }
            
            if(!dhcp6Enabled) {
                // ip6
                String configIp6 = "net.interface." + interfaceName + ".config.ip6.address";
                if (props.containsKey(configIp6)) {                    
                    
                    // address
                    String addressIp6 = (String) props.get(configIp6);
                    s_logger.trace("IPv6 address: " + addressIp6);
                    if(addressIp6 != null && !addressIp6.isEmpty()) {
                        IP6Address ip6Address = (IP6Address) IPAddress.parseHostAddress(addressIp6); 
                        netConfigIP6.setAddress(ip6Address);
                    }
    
                    // dns servers
                    String configDNSs6 = "net.interface." + interfaceName + ".config.ip6.dnsServers";
                    if (props.containsKey(configDNSs6)) {
                        
                        List<IP6Address> dnsIPs = new ArrayList<IP6Address>();
                        String dnsAll = (String) props.get(configDNSs6);
                        String[] dnss = dnsAll.split(",");
                        for (String dns : dnss) {
                            s_logger.trace("IPv6 DNS: " + dns);
                            IP6Address dnsIp6 = (IP6Address) IPAddress.parseHostAddress(dns);
                            dnsIPs.add(dnsIp6);
                        }
                        netConfigIP6.setDnsServers(dnsIPs);
                    }
                    
                    // domains
                    String configDomains6 = "net.interface." + interfaceName + ".config.ip6.domains";
                    if (props.containsKey(configDomains6)) {
                        
                        List<String> domainNames = new ArrayList<String>();
                        String domainsAll = (String) props.get(configDomains6);
                        String[] domains = domainsAll.split(",");
                        for (String domain : domains) {
                            s_logger.trace("IPv6 Domain: " + domain);
                            domainNames.add(domain);
                        }
                        netConfigIP6.setDomains(domainNames);
                    }
                }
            }
            
            if(interfaceType == NetInterfaceType.WIFI) {
            	s_logger.trace("Adding wifi netconfig");
                  	
            	 // Wifi access point config
            	WifiConfig apConfig = getWifiConfig(netIfConfigPrefix, WifiMode.MASTER, props);
            	if(apConfig != null) {
            		s_logger.trace("Adding AP wifi config");
            		netConfigs.add(apConfig);
            	} else {
            		s_logger.warn("no AP wifi config specified");
            	}
                
                // Wifi client/adhoc config
            	//WifiConfig adhocConfig = getWifiConfig(netIfConfigPrefix, WifiMode.ADHOC, props);
            	WifiConfig infraConfig = getWifiConfig(netIfConfigPrefix, WifiMode.INFRA, props);
            	/*
            	if(adhocConfig != null && infraConfig != null) {
            		s_logger.warn("Two conflicting client wifi configs specified");
            	}*/
            	if(infraConfig != null) {
            		s_logger.trace("Adding client INFRA wifi config");
            		netConfigs.add(infraConfig);
            	} else {
            		s_logger.warn("no INFRA wifi config specified");
            	}
            	/*
            	if(adhocConfig != null){
            		s_logger.trace("Adding client ADHOC wifi config");
            		netConfigs.add(adhocConfig);
            	}*/
            }
            
            if(interfaceType == NetInterfaceType.MODEM) {
                s_logger.trace("Adding modem netconfig");
                    
                netConfigs.add(getModemConfig(netIfConfigPrefix, props));
            }
        }

	}
}
