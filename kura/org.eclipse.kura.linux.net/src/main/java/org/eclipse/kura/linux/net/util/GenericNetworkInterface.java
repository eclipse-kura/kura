/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix build warnigns
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.LoopbackInterfaceConfigImpl;
import org.eclipse.kura.core.net.LoopbackInterfaceImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiAccessPointImpl;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.ConnectionInfoImpl;
import org.eclipse.kura.linux.net.dhcp.DhcpServerFactory;
import org.eclipse.kura.linux.net.dhcp.DhcpServerImpl;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.wifi.Hostapd;
import org.eclipse.kura.linux.net.wifi.WpaSupplicant;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericNetworkInterface {
	
	private static final Logger s_logger = LoggerFactory.getLogger(GenericNetworkInterface.class);
	protected static String NET_CONFIGURATION_DIRECTORY;
	protected static File kuraFile;
	
	protected static NetInterfaceConfig<?> getCurrentConfig(String interfaceName,
			NetInterfaceType type, NetInterfaceStatus status,
			boolean dhcpServerEnabled, boolean passDns, Properties kuraProps)
			throws KuraException {
		
		try {
			NetInterfaceConfig<?> netInterfaceConfig = null;
			boolean autoConnect = false;
			int mtu = -1;
			boolean dhcp = false;
			IP4Address address = null;
			String ipAddress = null;
			String prefixString = null;
			String netmask = null;
			@SuppressWarnings("unused")
			String broadcast = null;
			String gateway = null;
			boolean interfaceEnabled = false;
			
			if(kuraProps != null) {
				String onBoot = kuraProps.getProperty("ONBOOT");
				if("yes".equals(onBoot)) {
					autoConnect = true;
					
					//we are enabled - just not sure if for LAN or WAN
					if(status == NetInterfaceStatus.netIPv4StatusUnknown) {
						interfaceEnabled = true;
					}
				} else {
					autoConnect = false;
				}
	
				//override MTU with what is in config if it is present
				String stringMtu = kuraProps.getProperty("MTU");
				if(stringMtu == null) {
					try {
						mtu = LinuxNetworkUtil.getCurrentMtu(interfaceName);
					} catch(KuraException e) {
						//just assume ???
						if(interfaceName.equals("lo")) {
							mtu = 16436;
						} else {
							mtu = 1500;
						}
					}
				} else {
					mtu = Short.parseShort(stringMtu);
				}
	
				//get the bootproto
				String bootproto = kuraProps.getProperty("BOOTPROTO");
				if(bootproto == null) {
					bootproto="static";
				}
				
				//get the defroute
				String defroute = kuraProps.getProperty("DEFROUTE");
				if(defroute == null) {
					defroute="no";
				}
				
				if(interfaceEnabled) {
					if(defroute.equals("yes")) {
						status = NetInterfaceStatus.netIPv4StatusEnabledWAN;
					} else {
						status = NetInterfaceStatus.netIPv4StatusEnabledLAN;
					}
				}
	
				//check for dhcp or static configuration
				try {
					ipAddress = kuraProps.getProperty("IPADDR");
					prefixString = kuraProps.getProperty("PREFIX");
					netmask = kuraProps.getProperty("NETMASK");
					broadcast = kuraProps.getProperty("BROADCAST");
					try {
						gateway = kuraProps.getProperty("GATEWAY");
						s_logger.debug("got gateway for {}: {}", interfaceName, gateway);
					} catch(Exception e) {
						s_logger.warn("missing gateway stanza for " + interfaceName);
					}
	
					if (bootproto.equals("dhcp")) {
						s_logger.debug("currently set for DHCP");
						dhcp = true;
						ipAddress = null;
						netmask = null;
					} else {
						s_logger.debug("currently set for static address");
						dhcp = false;
					}
				} catch(Exception e) {
					e.printStackTrace();
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "malformatted config file: " + NET_CONFIGURATION_DIRECTORY + "ifcfg-" + interfaceName);
				}
	
				if(ipAddress != null && !ipAddress.isEmpty()) {
					address = (IP4Address) IPAddress.parseHostAddress(ipAddress);
				}
				
				//make sure at least prefix or netmask is present if static
				if(!dhcp && prefixString == null && netmask == null) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "malformatted config file: " + NET_CONFIGURATION_DIRECTORY + "ifcfg-" + interfaceName + " must contain NETMASK and/or PREFIX");
				}
			}
			
			ConnectionInfo conInfo = new ConnectionInfoImpl(interfaceName);
			LinuxDns dnsService = LinuxDns.getInstance();
			
			//note - we only add the fields we need/care about from a configuration standpoint
			if(type == NetInterfaceType.LOOPBACK) {
				s_logger.debug("Adding a Loopback interface");
				netInterfaceConfig = new LoopbackInterfaceConfigImpl(interfaceName);

				((LoopbackInterfaceImpl<?>)netInterfaceConfig).setMTU(mtu);
				((LoopbackInterfaceImpl<?>)netInterfaceConfig).setAutoConnect(true);		//loopback autoConnect should always be true?
				((LoopbackInterfaceImpl<?>)netInterfaceConfig).setLoopback(true);

				List<NetInterfaceAddressConfig> netInterfaceAddressConfigs = new ArrayList<NetInterfaceAddressConfig>();
				List<NetInterfaceAddress> netInterfaceAddresses = new ArrayList<NetInterfaceAddress>();

				NetInterfaceAddressConfigImpl netInterfaceAddressConfig = new NetInterfaceAddressConfigImpl();
				netInterfaceAddressConfigs.add(netInterfaceAddressConfig);
				netInterfaceAddresses.add(netInterfaceAddressConfig);
				LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
				if((ifconfig != null) && ifconfig.isUp()) {
					netInterfaceAddressConfig.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
					netInterfaceAddressConfig.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
					netInterfaceAddressConfig.setNetmask(IPAddress.parseHostAddress(ifconfig.getInetMask()));
					netInterfaceAddressConfig.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(ifconfig.getInetMask()));
                    netInterfaceAddressConfig.setGateway(conInfo.getGateway());
					if(dhcp) {
					    netInterfaceAddressConfig.setDnsServers(dnsService.getDhcpDnsServers(interfaceName, netInterfaceAddressConfig.getAddress())); 
					} else {
	                    netInterfaceAddressConfig.setDnsServers(conInfo.getDnsServers());
					}
				}
				((LoopbackInterfaceConfigImpl)netInterfaceConfig).setNetInterfaceAddresses(netInterfaceAddressConfigs);
				
				List<NetConfig> netConfigs = new ArrayList<NetConfig>();
				netInterfaceAddressConfig.setNetConfigs(netConfigs);
				
				//FIXME - hardcoded
				NetConfig netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
				((NetConfigIP4)netConfig).setAddress(address);
				((NetConfigIP4)netConfig).setDhcp(dhcp);
				((NetConfigIP4)netConfig).setDnsServers(null);
				((NetConfigIP4)netConfig).setDomains(null);
				((NetConfigIP4)netConfig).setGateway(null);
				((NetConfigIP4)netConfig).setNetworkPrefixLength((short) 8);
				((NetConfigIP4)netConfig).setSubnetMask((IP4Address) IPAddress.parseHostAddress("255.0.0.0"));
				((NetConfigIP4)netConfig).setWinsServers(null);
				netConfigs.add(netConfig);
				

			} else if(type == NetInterfaceType.ETHERNET) {
				s_logger.debug("Adding an Ethernet interface - {}", interfaceName);
				netInterfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
				
				((EthernetInterfaceImpl<?>)netInterfaceConfig).setMTU(mtu);
				((EthernetInterfaceImpl<?>)netInterfaceConfig).setAutoConnect(autoConnect);
				((EthernetInterfaceImpl<?>)netInterfaceConfig).setLoopback(false);

				List<NetInterfaceAddressConfig> netInterfaceAddressConfigs = new ArrayList<NetInterfaceAddressConfig>();
				List<NetInterfaceAddress> netInterfaceAddresses = new ArrayList<NetInterfaceAddress>();

				NetInterfaceAddressConfigImpl netInterfaceAddressConfig = new NetInterfaceAddressConfigImpl();
				netInterfaceAddressConfigs.add(netInterfaceAddressConfig);
				netInterfaceAddresses.add(netInterfaceAddressConfig);
				LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
				if (ifconfig != null) {
					((EthernetInterfaceImpl<?>)netInterfaceConfig).setHardwareAddress(ifconfig.getMacAddressBytes());
					if(ifconfig.isUp()) {
						try {
							netInterfaceAddressConfig.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
							netInterfaceAddressConfig.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
							netInterfaceAddressConfig.setNetmask(IPAddress.parseHostAddress(ifconfig.getInetMask()));
							netInterfaceAddressConfig.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(ifconfig.getInetMask()));
							netInterfaceAddressConfig.setGateway(conInfo.getGateway());
		                    if(dhcp) {
		                        netInterfaceAddressConfig.setDnsServers(dnsService.getDhcpDnsServers(interfaceName, netInterfaceAddressConfig.getAddress()));
		                    } else {
		                        netInterfaceAddressConfig.setDnsServers(conInfo.getDnsServers());
		                    }
						} catch(KuraException e) {
							s_logger.warn("The interface went down " + interfaceName + " not including current state in status because it is not up");
							netInterfaceAddressConfig.setAddress(null);
							netInterfaceAddressConfig.setBroadcast(null);
							netInterfaceAddressConfig.setNetmask(null);
							netInterfaceAddressConfig.setNetworkPrefixLength((short)-1);
							netInterfaceAddressConfig.setGateway(null);
							netInterfaceAddressConfig.setDnsServers(null);
						}
					}
				}
				((EthernetInterfaceConfigImpl)netInterfaceConfig).setNetInterfaceAddresses(netInterfaceAddressConfigs);

				//add the config					
				List<NetConfig> netConfigs = new ArrayList<NetConfig>();
				netInterfaceAddressConfig.setNetConfigs(netConfigs);
				
				NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, autoConnect);
				setNetConfigIP4(netConfig, status, autoConnect, dhcp, address, gateway, prefixString, netmask, kuraProps);
				netConfigs.add(netConfig);
				
				if (dhcpServerEnabled) {
					// add DHCP server configuration to the list
					DhcpServerImpl dhcpServer = DhcpServerFactory.getInstance(interfaceName, dhcpServerEnabled, passDns);
					DhcpServerConfig4 dhcpServerConfig = dhcpServer.getDhcpServerConfig(dhcpServerEnabled, passDns);
					if(dhcpServerConfig != null) {
						netConfigs.add(dhcpServerConfig);
					}
				}
			} else if(type == NetInterfaceType.WIFI) {
				s_logger.debug("Adding a Wireless interface - {}", interfaceName);
				WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
				netInterfaceConfig = wifiInterfaceConfig;

				wifiInterfaceConfig.setMTU(mtu);
				wifiInterfaceConfig.setAutoConnect(autoConnect);
				wifiInterfaceConfig.setLoopback(false);

				List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = new ArrayList<WifiInterfaceAddressConfig>();
				List<WifiInterfaceAddress> wifiInterfaceAddresses = new ArrayList<WifiInterfaceAddress>();

				WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
				wifiInterfaceAddressConfigs.add(wifiInterfaceAddressConfig);
				wifiInterfaceAddresses.add(wifiInterfaceAddressConfig);
				
				String currentSSID = LinuxNetworkUtil.getSSID(interfaceName);
				LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
				if (ifconfig != null) {
					wifiInterfaceConfig.setHardwareAddress(ifconfig.getMacAddressBytes());
					if(ifconfig.isUp()) {
						wifiInterfaceAddressConfig.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
						wifiInterfaceAddressConfig.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
						String currentNetmask = ifconfig.getInetMask();
						if (currentNetmask != null) {
							wifiInterfaceAddressConfig.setNetmask(IPAddress.parseHostAddress(currentNetmask));
							wifiInterfaceAddressConfig.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
						}
	
						wifiInterfaceAddressConfig.setBitrate(LinuxNetworkUtil.getWifiBitrate(interfaceName));
						wifiInterfaceAddressConfig.setGateway(conInfo.getGateway());
	                    if(dhcp) {
	                        wifiInterfaceAddressConfig.setDnsServers(dnsService.getDhcpDnsServers(interfaceName, wifiInterfaceAddressConfig.getAddress()));
	                    } else {
	                        wifiInterfaceAddressConfig.setDnsServers(conInfo.getDnsServers());
	                    }
	
	
						WifiAccessPointImpl ap = null;
						 
						if(currentSSID != null) {
							s_logger.debug("Adding access point SSID: {}", currentSSID);
	
							ap = new WifiAccessPointImpl(currentSSID);
	
							// TODO: fill in other info
							ap.setMode(WifiMode.INFRA);
							List<Long> bitrate = new ArrayList<Long>();
							bitrate.add(54000000L);
							ap.setBitrate(bitrate);
							ap.setFrequency(12345);
							ap.setHardwareAddress("20AA4B8A6442".getBytes());
							ap.setRsnSecurity(EnumSet.allOf(WifiSecurity.class));
							ap.setStrength(1234);
							ap.setWpaSecurity(EnumSet.allOf(WifiSecurity.class));
						}
						wifiInterfaceAddressConfig.setWifiAccessPoint(ap);						
					}
				}
				
				// mode
				WifiMode wifiMode = WifiMode.UNKNOWN;

				s_logger.debug("Get WifiMode...");
				try {
					// get from config file
					String mode = (String) kuraProps.getProperty("MODE");
					if (mode != null) {
						s_logger.debug("Getting wifi mode from {}", kuraFile.getAbsolutePath());
						if (mode.equalsIgnoreCase("Managed")) {
							wifiMode = WifiMode.INFRA;
						} else if (mode.equalsIgnoreCase("Master")) {
							wifiMode = WifiMode.MASTER;
						} else if (mode.equalsIgnoreCase("Ad-Hoc")) {
							wifiMode = WifiMode.ADHOC;
						} else {
							wifiMode =  WifiMode.valueOf(mode);
						}
					} else {
						// get current setting using iwconfig
						s_logger.debug("Getting wifi mode from iwconfig");
						wifiMode = LinuxNetworkUtil.getWifiMode(interfaceName);
					}
				} catch (Exception e) {
					// leave as unknown
				}					

				s_logger.debug("Current WifiMode: {}", wifiMode);
				wifiInterfaceAddressConfig.setMode(wifiMode);
				
				wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);
				
				// TODO: fix
				wifiInterfaceConfig.setCapabilities(EnumSet.allOf(Capability.class));

				// add the configs - one for client (managed) mode, one for access point (master) mode
				List<NetConfig> netConfigs = new ArrayList<NetConfig>();
				wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
				
				// get the NetConfig
				NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, autoConnect);
				setNetConfigIP4(netConfig, status, autoConnect, dhcp, address, gateway, prefixString, netmask, kuraProps);
				netConfigs.add(netConfig);
				 
				// get the wpa_supplicant configuration
				WifiConfig wifiClientConfig = new WifiConfig();
				setWifiClientConfig(interfaceName, wifiClientConfig, wifiMode);
				
				// get the hostapd configuration
				WifiConfig wifiAPConfig = new WifiConfig();
				setWifiAccessPointConfig(wifiAPConfig);
				
				// add WiFi configurations to the list
				netConfigs.add(wifiClientConfig);
				netConfigs.add(wifiAPConfig);
				
				if (dhcpServerEnabled) {
					// add DHCP server configuration to the list
					DhcpServerImpl dhcpServer = DhcpServerFactory.getInstance(interfaceName, dhcpServerEnabled, passDns);
					DhcpServerConfig4 dhcpServerConfig = dhcpServer.getDhcpServerConfig(dhcpServerEnabled, passDns);
					if(dhcpServerConfig != null) {
						netConfigs.add(dhcpServerConfig);
					}
				}
			} else if(type == NetInterfaceType.MODEM) {
				s_logger.debug("Adding a Modem interface");
				netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
				
				((ModemInterfaceConfigImpl)netInterfaceConfig).setMTU(mtu);
				((ModemInterfaceConfigImpl)netInterfaceConfig).setAutoConnect(autoConnect);
				((ModemInterfaceConfigImpl)netInterfaceConfig).setLoopback(false);
				((ModemInterfaceConfigImpl)netInterfaceConfig).setPointToPoint(true);

				List<ModemInterfaceAddressConfig> modemInterfaceAddressConfigs = new ArrayList<ModemInterfaceAddressConfig>();
				List<ModemInterfaceAddress> netInterfaceAddresses = new ArrayList<ModemInterfaceAddress>();

				ModemInterfaceAddressConfigImpl netInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
				modemInterfaceAddressConfigs.add(netInterfaceAddressConfig);
				netInterfaceAddresses.add(netInterfaceAddressConfig);
				LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
				if (ifconfig != null) {
					((ModemInterfaceConfigImpl)netInterfaceConfig).setHardwareAddress(ifconfig.getMacAddressBytes());
					if(ifconfig.isUp()) {
						netInterfaceAddressConfig.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
						netInterfaceAddressConfig.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
						netInterfaceAddressConfig.setNetmask(IPAddress.parseHostAddress(ifconfig.getInetMask()));
						netInterfaceAddressConfig.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(ifconfig.getInetMask()));
	                    netInterfaceAddressConfig.setGateway(conInfo.getGateway());
	                    netInterfaceAddressConfig.setDnsServers(conInfo.getDnsServers());
					}
				}
				((ModemInterfaceConfigImpl)netInterfaceConfig).setNetInterfaceAddresses(modemInterfaceAddressConfigs);

				//add the config					
				List<NetConfig> netConfigs = new ArrayList<NetConfig>();
				netInterfaceAddressConfig.setNetConfigs(netConfigs);
				
				NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, autoConnect);
				setNetConfigIP4(netConfig, status, autoConnect, dhcp, address, gateway, prefixString, netmask, kuraProps);
				netConfigs.add(netConfig);
			} else {
				s_logger.warn("Unsupported Type: " + type);
			}
			
			return netInterfaceConfig;
			
		} catch(UnknownHostException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	
	
	
	/**
	 * Populate a NetConfigIP4 object using the given values
	 * 
	 * @param netConfig
	 * @param status
	 * @param autoConnect
	 * @param dhcp
	 * @param address
	 * @param gateway
	 * @param prefixString
	 * @param netmask
	 * @param kuraProps
	 * @throws UnknownHostException
	 * @throws KuraException
	 */
	private static void setNetConfigIP4(NetConfigIP4 netConfig,
			NetInterfaceStatus status,
			boolean autoConnect,
			boolean dhcp,
			IP4Address address,
			String gateway,
			String prefixString,
			String netmask,
			Properties kuraProps) throws UnknownHostException, KuraException {

		if(status != null) {
			netConfig.setStatus(status);
		} else {
			//FIXME - make a best guess??
			if(autoConnect) {
				if(dhcp) {
					netConfig.setStatus(NetInterfaceStatus.netIPv4StatusEnabledWAN);
				} else {
					if(gateway != null) {
						netConfig.setStatus(NetInterfaceStatus.netIPv4StatusEnabledWAN);
					} else {
						netConfig.setStatus(NetInterfaceStatus.netIPv4StatusEnabledLAN);
					}
				}
			} else {
				netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, autoConnect);
			}
		}

		netConfig.setDhcp(dhcp);
		if(kuraProps != null) {
			//get the DNS
			List<IP4Address> dnsServers = new ArrayList<IP4Address>();
			int count = 1;
			while(true) {
				String dns = null;
				if((dns = kuraProps.getProperty("DNS" + count)) != null) {
					dnsServers.add((IP4Address) IP4Address.parseHostAddress(dns));
					count++;
				} else {
					break;
				}
			}
            netConfig.setDnsServers(dnsServers);

			if(!dhcp) {
    			netConfig.setAddress(address);
    			//TODO ((NetConfigIP4)netConfig).setDomains(domains);
    			if(gateway != null && !gateway.isEmpty()) {
    				netConfig.setGateway((IP4Address) IP4Address.parseHostAddress(gateway));
    			}
    			if(prefixString != null) {
    				short prefix = Short.parseShort(prefixString);
    				netConfig.setNetworkPrefixLength(prefix);
    			}
    			if(netmask != null) {
    				netConfig.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(netmask));
    			}
    			//TODO netConfig.setWinsServers(winsServers);
			}
		}
	}
		
	/**
	 * Populate a WifiConfigIP4 object using the wpa_supplicant config
	 * 
	 * @param ifaceName - interface name as {@link String}
	 * @param wifiConfig - WiFi configuration as {@link WifiConfig}
	 * @param wifiMode - WiFi mode as {@link wifiMode}
	 * @throws KuraException
	 */
	private static void setWifiClientConfig(String ifaceName,
			WifiConfig wifiConfig, WifiMode wifiMode) throws KuraException {
		
		WpaSupplicant supplicant = WpaSupplicant.getWpaSupplicant(ifaceName);
		if (supplicant != null) {
	        wifiConfig.setMode(supplicant.getMode());
			wifiConfig.setSSID(supplicant.getSSID());
			wifiConfig.setSecurity(supplicant.getWifiSecurity());
			wifiConfig.setPasskey(supplicant.getPassword());
			wifiConfig.setPairwiseCiphers(supplicant.getPairwiseCiphers());
			wifiConfig.setGroupCiphers(supplicant.getGroupCiphers());
			wifiConfig.setChannels(supplicant.getChannels());
			wifiConfig.setBgscan(supplicant.getBgscan());
		}
	}
	
	/**
	 * Populate a WifiConfigIP4 object using the hostapd config
	 * 
	 * @param wifiConfig
	 * @throws KuraException
	 */
	private static void setWifiAccessPointConfig(WifiConfig wifiConfig) throws KuraException {
		
		wifiConfig.setMode(WifiMode.MASTER);
		
		Hostapd hostapd = Hostapd.getHostapd();
		if(hostapd != null) {
			wifiConfig.setSSID(hostapd.getSSID());
			//wifiConfig.setChannel((short)hostapd.getChannel());
			int [] channels = new int[1];
			channels[0] = hostapd.getChannel();
			wifiConfig.setChannels(channels);
			wifiConfig.setPasskey(hostapd.getPassword());
			wifiConfig.setBroadcast(true);		// TODO: always true?  is this needed?

			// security
			wifiConfig.setSecurity(hostapd.getSecurity());
			
			// hw mode
			WifiRadioMode radioMode = hostapd.getRadioMode();
			if(radioMode == WifiRadioMode.RADIO_MODE_80211b) {
				wifiConfig.setHardwareMode("b");
			} else if(radioMode == WifiRadioMode.RADIO_MODE_80211g) {
				wifiConfig.setHardwareMode("g");
			} else if(radioMode == WifiRadioMode.RADIO_MODE_80211nHT20 ||
					radioMode == WifiRadioMode.RADIO_MODE_80211nHT40above ||
					radioMode == WifiRadioMode.RADIO_MODE_80211nHT40below) {
				
				// TODO: specify these 'n' modes separately?
				wifiConfig.setHardwareMode("n");
			}
			
		}
	}
}
