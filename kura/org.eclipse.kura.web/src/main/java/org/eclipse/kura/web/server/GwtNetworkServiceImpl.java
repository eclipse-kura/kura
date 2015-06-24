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
package org.eclipse.kura.web.server;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.modem.ModemManagerService;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiBgscanModule;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiClientMonitorService;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiHotspotInfo;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.web.server.util.KuraExceptionHandler;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtModemAuthType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtModemPdpType;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.ListLoadResult;


public class GwtNetworkServiceImpl extends OsgiRemoteServiceServlet implements GwtNetworkService 
{
	private static final long serialVersionUID = -4188750359099902616L;

	private static Logger s_logger = LoggerFactory.getLogger(GwtNetworkServiceImpl.class);
	
	
	public ListLoadResult<GwtNetInterfaceConfig> findNetInterfaceConfigurations() 
		throws GwtKuraException 
	{
		s_logger.debug("Starting");
		
		NetworkAdminService nas = null;
		try {
			nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		} catch (Throwable t) {
			s_logger.warn("Exception: {}", t.toString());
			return null;
		}
		
		ModemManagerService modemManagerService = null;
		try {
			modemManagerService = ServiceLocator.getInstance().getService(ModemManagerService.class);
		} catch (Throwable t) {
			s_logger.warn("{ModemManagerService} Exception: {}", t.toString());
		}
		
		WifiClientMonitorService wifiClientMonitorService = null;
		try {
			wifiClientMonitorService = ServiceLocator.getInstance().getService(WifiClientMonitorService.class);
		} catch (Throwable t) {
			s_logger.warn("{WifiClientMonitorService} Exception: {}", t.toString());
		}
		
		List<GwtNetInterfaceConfig> gwtNetConfigs = new ArrayList<GwtNetInterfaceConfig>();
		try {

		    GwtNetInterfaceConfig gwtNetConfig = null;		
			for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netIfConfig : nas.getNetworkInterfaceConfigs()) {
				s_logger.debug("Getting config for " + netIfConfig.getName() + " with type " + netIfConfig.getType());
				
				s_logger.debug("Interface State: " + netIfConfig.getState());
				
				if (netIfConfig.getType() == NetInterfaceType.WIFI) {
					gwtNetConfig = new GwtWifiNetInterfaceConfig();
				} else if (netIfConfig.getType() == NetInterfaceType.MODEM) {
					gwtNetConfig = new GwtModemInterfaceConfig();
					((GwtModemInterfaceConfig)gwtNetConfig).setModemId(((ModemInterface)netIfConfig).getModemIdentifier());
                    ((GwtModemInterfaceConfig)gwtNetConfig).setManufacturer(((ModemInterface)netIfConfig).getManufacturer());
                    ((GwtModemInterfaceConfig)gwtNetConfig).setModel(((ModemInterface)netIfConfig).getModel());
                    
                    List<String> technologyList = new ArrayList<String>();
                    List<ModemTechnologyType> technologyTypes = ((ModemInterface)netIfConfig).getTechnologyTypes();
                    if(technologyTypes != null) {
                        for(ModemTechnologyType techType : technologyTypes) {
                            technologyList.add(techType.name());
                        }
                    }
                    ((GwtModemInterfaceConfig)gwtNetConfig).setNetworkTechnology(technologyList);
				} else {
					gwtNetConfig = new GwtNetInterfaceConfig();
					gwtNetConfig.setHwRssi("N/A");
				}
				
				gwtNetConfig.setName(netIfConfig.getName());
				gwtNetConfig.setHwName(netIfConfig.getName());
				if (netIfConfig.getType() != null) {
					gwtNetConfig.setHwType(netIfConfig.getType().name());
				}
				if (netIfConfig.getState() != null) {
					gwtNetConfig.setHwState(netIfConfig.getState().name());
				}
				s_logger.debug("MAC: "+NetUtil.hardwareAddressToString(netIfConfig.getHardwareAddress()));
				gwtNetConfig.setHwAddress(NetUtil.hardwareAddressToString(netIfConfig.getHardwareAddress()));
				gwtNetConfig.setHwDriver(netIfConfig.getDriver());
				gwtNetConfig.setHwDriverVersion(netIfConfig.getDriverVersion());
				gwtNetConfig.setHwFirmware(netIfConfig.getFirmwareVersion());
				gwtNetConfig.setHwMTU(netIfConfig.getMTU());
				if (netIfConfig.getUsbDevice() != null) {
					gwtNetConfig.setHwUsbDevice(netIfConfig.getUsbDevice().getUsbDevicePath());
				}
				else {
					gwtNetConfig.setHwUsbDevice("N/A");
				}
								
				List<? extends NetInterfaceAddressConfig> addressConfigs = netIfConfig.getNetInterfaceAddresses();
				
				if (addressConfigs != null && addressConfigs.size() > 0) {
					for (NetInterfaceAddressConfig addressConfig : addressConfigs) {
						//current status - not configuration!
						if(addressConfig.getAddress() != null) {
							s_logger.debug("current address: "+addressConfig.getAddress().getHostAddress());
						}
						if(addressConfig.getNetworkPrefixLength() >= 0 && addressConfig.getNetworkPrefixLength() <= 32) {
							s_logger.debug("current prefix length: "+addressConfig.getNetworkPrefixLength());
						}
						if(addressConfig.getNetmask() != null) {
							s_logger.debug("current netmask: "+addressConfig.getNetmask().getHostAddress());						
						}
						
						List<NetConfig> netConfigs = addressConfig.getConfigs();
						if(netConfigs != null && netConfigs.size() > 0) {
							boolean isNatEnabled = false;
							boolean isDhcpServerEnabled = false;
							
							for(NetConfig netConfig : netConfigs) {
								if(netConfig instanceof NetConfigIP4) {
									s_logger.debug("Setting up NetConfigIP4 with status " + ((NetConfigIP4)netConfig).getStatus().toString());
									
									//we are enabled - for LAN or WAN?
									if(((NetConfigIP4)netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN) {
										gwtNetConfig.setStatus(GwtNetIfStatus.netIPv4StatusEnabledLAN.name());
									} else if(((NetConfigIP4)netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
										gwtNetConfig.setStatus(GwtNetIfStatus.netIPv4StatusEnabledWAN.name());
									} else {
									    gwtNetConfig.setStatus(GwtNetIfStatus.netIPv4StatusDisabled.name());
									}

									if(((NetConfigIP4)netConfig).isDhcp()) {
										gwtNetConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());

										//since DHCP - populate current data
										if(addressConfig.getAddress() != null) {
											gwtNetConfig.setIpAddress(addressConfig.getAddress().getHostAddress());
										} else {
											gwtNetConfig.setIpAddress("");
										}
										if(addressConfig.getNetworkPrefixLength() >= 0 && addressConfig.getNetworkPrefixLength() <= 32) {
											gwtNetConfig.setSubnetMask(NetworkUtil.getNetmaskStringForm(addressConfig.getNetworkPrefixLength()));
										} else {
											if(addressConfig.getNetmask() != null) {
												gwtNetConfig.setSubnetMask(addressConfig.getNetmask().getHostAddress());
											} else {
												gwtNetConfig.setSubnetMask("");
											}
										}
										if(addressConfig.getGateway() != null) {
											gwtNetConfig.setGateway(addressConfig.getGateway().getHostAddress());
										} else {
											gwtNetConfig.setGateway("");
										}

										// DHCP supplied DNS servers
										StringBuffer sb = new StringBuffer();
										List<? extends IPAddress> dnsServers = addressConfig.getDnsServers();
										if(dnsServers != null && dnsServers.size() > 0) {
											String sep = "";
											for(IPAddress dnsServer : dnsServers) {
												sb.append(sep).append(dnsServer.getHostAddress());
												sep = "\n";
											}

											s_logger.debug("DNS Servers: " + sb.toString());
											gwtNetConfig.setReadOnlyDnsServers(sb.toString());
										} else {
											s_logger.debug("DNS Servers: [empty String]");
											gwtNetConfig.setReadOnlyDnsServers("");
										}
									} else {
										gwtNetConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeManual.name());

										//since STATIC - populate with configured values
										//TODO - should we throw an error if current state doesn't match configuration?
										if(((NetConfigIP4)netConfig).getAddress() != null) {										
											gwtNetConfig.setIpAddress(((NetConfigIP4)netConfig).getAddress().getHostAddress());
										} else {
											gwtNetConfig.setIpAddress("");
										}
										if(((NetConfigIP4)netConfig).getSubnetMask() != null) {
											gwtNetConfig.setSubnetMask(((NetConfigIP4)netConfig).getSubnetMask().getHostAddress());
										} else {
											gwtNetConfig.setSubnetMask("");
										}
										if(((NetConfigIP4)netConfig).getGateway() != null) {
											s_logger.debug("Gateway for " + netIfConfig.getName() + " is: " + ((NetConfigIP4)netConfig).getGateway().getHostAddress());
											gwtNetConfig.setGateway(((NetConfigIP4)netConfig).getGateway().getHostAddress());
										} else {
											gwtNetConfig.setGateway("");
										}
									}

									// Custom DNS servers
									StringBuffer sb = new StringBuffer();
									List<IP4Address> dnsServers = ((NetConfigIP4)netConfig).getDnsServers();
									if(dnsServers != null && dnsServers.size() > 0) {
										String sep = "";
										for(IP4Address dnsServer : dnsServers) {
											if(!dnsServer.getHostAddress().equals("127.0.0.1")) {
												sb.append(sep).append(dnsServer.getHostAddress());
												sep = "\n";
											}
										}

										s_logger.debug("DNS Servers: " + sb.toString());
										gwtNetConfig.setDnsServers(sb.toString());
									} else {
										s_logger.debug("DNS Servers: [empty String]");
										gwtNetConfig.setDnsServers("");
									}

									// Search domains
									sb = new StringBuffer();
									List<IP4Address> winsServers = ((NetConfigIP4)netConfig).getWinsServers();
									if(winsServers != null && winsServers.size() > 0) {
										for(IP4Address winServer : winsServers) {
											sb.append(winServer.getHostAddress());
											sb.append("\n");
										}

										s_logger.debug("Search Domains: " + sb.toString());
										gwtNetConfig.setSearchDomains(sb.toString());
									} else {
										s_logger.debug("Search Domains: [empty String]");
										gwtNetConfig.setSearchDomains("");
									}
								}

								// The NetConfigIP4 section above should also apply for a wireless interface
								// Note that this section is used to configure both a station config and an access point config
								if(netConfig instanceof WifiConfig) {
									s_logger.debug("Setting up WifiConfigIP4");
									
									WifiConfig wifiConfig = (WifiConfig) netConfig;									
									GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

									// mode
									if (wifiConfig.getMode() == WifiMode.MASTER) {
										gwtWifiConfig.setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name());

										// set as the access point config for this interface
										((GwtWifiNetInterfaceConfig)gwtNetConfig).setAccessPointWifiConfig(gwtWifiConfig);
									} else if (wifiConfig.getMode() == WifiMode.INFRA) {
										gwtWifiConfig.setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeStation.name());

										// set as the station config for this interface
										((GwtWifiNetInterfaceConfig)gwtNetConfig).setStationWifiConfig(gwtWifiConfig);
									} else if (wifiConfig.getMode() == WifiMode.ADHOC) {
										gwtWifiConfig.setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name());

										// set as the adhoc config for this interface
										((GwtWifiNetInterfaceConfig)gwtNetConfig).setAdhocWifiConfig(gwtWifiConfig);
									}

									// ssid
									gwtWifiConfig.setWirelessSsid(wifiConfig.getSSID());

                                    // driver
                                    gwtWifiConfig.setDriver(wifiConfig.getDriver());

									// security
									if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA) {
										gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWPA.name());
									} else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA2) {
										gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWPA2.name());
									} else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA_WPA2) {
										gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name());
									} else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WEP) {
										gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityWEP.name());
									} else {
										gwtWifiConfig.setSecurity(GwtWifiSecurity.netWifiSecurityNONE.name());
									}
									
									if (wifiConfig.getPairwiseCiphers() == WifiCiphers.CCMP_TKIP) {
										gwtWifiConfig.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name());
									} else if (wifiConfig.getPairwiseCiphers() == WifiCiphers.TKIP) {
										gwtWifiConfig.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_TKIP.name());
									} else if (wifiConfig.getPairwiseCiphers() == WifiCiphers.CCMP) {
										gwtWifiConfig.setPairwiseCiphers(GwtWifiCiphers.netWifiCiphers_CCMP.name());
									}
									
									if (wifiConfig.getGroupCiphers() == WifiCiphers.CCMP_TKIP) {
										gwtWifiConfig.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name());
									} else if (wifiConfig.getGroupCiphers() == WifiCiphers.TKIP) {
										gwtWifiConfig.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_TKIP.name());
									} else if (wifiConfig.getGroupCiphers() == WifiCiphers.CCMP) {
										gwtWifiConfig.setGroupCiphers(GwtWifiCiphers.netWifiCiphers_CCMP.name());
									}
									
									// bgscan
									WifiBgscan wifiBgscan = wifiConfig.getBgscan();			
									if (wifiBgscan != null) {
										if (wifiBgscan.getModule() == WifiBgscanModule.NONE) {
											gwtWifiConfig.setBgscanModule(GwtWifiBgscanModule.netWifiBgscanMode_NONE.name());
										} else if (wifiBgscan.getModule() == WifiBgscanModule.SIMPLE) {
											gwtWifiConfig.setBgscanModule(GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name());
										} else if (wifiBgscan.getModule() == WifiBgscanModule.LEARN) {
											gwtWifiConfig.setBgscanModule(GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name());
										}
										gwtWifiConfig.setBgscanRssiThreshold(wifiBgscan.getRssiThreshold());
										gwtWifiConfig.setBgscanShortInterval(wifiBgscan.getShortInterval());
										gwtWifiConfig.setBgscanLongInterval(wifiBgscan.getLongInterval());
									}
									
									// ping access point?
									gwtWifiConfig.setPingAccessPoint(wifiConfig.pingAccessPoint());
									
									// ignore SSID?
									gwtWifiConfig.setIgnoreSSID(wifiConfig.ignoreSSID());
									
									// passkey
									gwtWifiConfig.setPassword(wifiConfig.getPasskey());

									// channel
									int [] channels = wifiConfig.getChannels();
									if(channels != null) {
										ArrayList<Integer> alChannels = new ArrayList<Integer>();
										for (int channel : channels) {
											alChannels.add(new Integer(channel));
										}
										gwtWifiConfig.setChannels(alChannels);							
									}
									
									// radio mode
									GwtWifiRadioMode gwtWifiRadioMode = null;
									if(wifiConfig.getRadioMode() != null) {
    									switch(wifiConfig.getRadioMode()) {
    									case RADIO_MODE_80211a:
    									    gwtWifiRadioMode = GwtWifiRadioMode.netWifiRadioModeA;
    									    break;
                                        case RADIO_MODE_80211b:
                                            gwtWifiRadioMode = GwtWifiRadioMode.netWifiRadioModeB;
                                            break;
                                        case RADIO_MODE_80211g:
                                            gwtWifiRadioMode = GwtWifiRadioMode.netWifiRadioModeBG;
                                            break;
                                        case RADIO_MODE_80211nHT20:
                                        case RADIO_MODE_80211nHT40above:
                                        case RADIO_MODE_80211nHT40below:
                                            gwtWifiRadioMode = GwtWifiRadioMode.netWifiRadioModeBGN;
                                            break;
                                        default:
                                            break;
    									}
									}
									if (gwtWifiRadioMode != null) {
										gwtWifiConfig.setRadioMode(gwtWifiRadioMode.name());
									}

									// set the currently active mode based on the address config
									WifiMode activeWirelessMode = ((WifiInterfaceAddressConfig)addressConfig).getMode();
									if (activeWirelessMode == WifiMode.MASTER) {
										((GwtWifiNetInterfaceConfig)gwtNetConfig).setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name());
										gwtNetConfig.setHwRssi("N/A");
									} else if (activeWirelessMode == WifiMode.INFRA) {
										((GwtWifiNetInterfaceConfig)gwtNetConfig).setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeStation.name());
										if (wifiClientMonitorService != null) {
											if (wifiConfig.getMode().equals(WifiMode.INFRA)) {
												if (gwtNetConfig.getStatus().equals(GwtNetIfStatus.netIPv4StatusDisabled.name())) {
													gwtNetConfig.setHwRssi("N/A");
												} else {
													try {
														int rssi = wifiClientMonitorService.getSignalLevel(netIfConfig.getName(), wifiConfig.getSSID());
														s_logger.debug("Setting Received Signal Strength to {}", rssi);
														gwtNetConfig.setHwRssi(Integer.toString(rssi));
													} catch (KuraException e) {
														e.printStackTrace();
													}
												}
											}
										}
									} else if (activeWirelessMode == WifiMode.ADHOC) {
										((GwtWifiNetInterfaceConfig)gwtNetConfig).setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name());
										gwtNetConfig.setHwRssi("N/A");
									} else {
										((GwtWifiNetInterfaceConfig)gwtNetConfig).setWirelessMode(GwtWifiWirelessMode.netWifiWirelessModeDisabled.name());
										gwtNetConfig.setHwRssi("N/A");
									}
								}
								
								if(netConfig instanceof ModemConfig) {
									s_logger.debug("Setting up ModemConfig");
									
									ModemConfig modemConfig = (ModemConfig) netConfig;									
									GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) gwtNetConfig;
									
									//gwtModemConfig.setHwSerial(((ModemInterface)netIfConfig).getSerialNumber());
									
									if (modemManagerService != null) {
										UsbDevice usbDevice = netIfConfig.getUsbDevice();
										String modemServiceId = null;
										if (usbDevice != null) {
											modemServiceId = netIfConfig.getUsbDevice().getUsbPort();
										} else {
											Collection<CellularModem> modemServices = modemManagerService.getAllModemServices();
											for (CellularModem modemService : modemServices) {
												ModemDevice modemDevice = modemService.getModemDevice();
												if (modemDevice instanceof SerialModemDevice) {
													modemServiceId = modemDevice.getProductName();
													break;
												}
											}
										}
										
										if (modemServiceId != null) {
					                    	CellularModem cellModemService = modemManagerService.getModemService(modemServiceId); 
					                    	if (cellModemService != null) { 
					                    		
					                    		try {
					                    			String imei = cellModemService.getSerialNumber();
					                    			s_logger.debug("Setting IMEI/MEID to {}", imei);
					                    			gwtModemConfig.setHwSerial(imei);
					                    		} catch (KuraException e) {
					                    			s_logger.warn("Failed to get IMEI from modem", e);
					                    		}
					                    		try {
						                    		int rssi = cellModemService.getSignalStrength();
						                    		s_logger.debug("Setting Received Signal Strength to {}", rssi);
						                    		gwtModemConfig.setHwRssi(Integer.toString(rssi));
					                    		} catch (KuraException e) {
					                    			s_logger.warn("Failed to get Received Signal Strength from modem", e);
					                    		}
					                    		
					                    		try {
						                    		String sModel = cellModemService.getModel();
						                    		((GwtModemInterfaceConfig)gwtNetConfig).setModel(sModel);
					                    		} catch (KuraException e) {
					                    			s_logger.warn("Failed to get model information from modem", e);
					                    		}
					                    		
					                    		try {
					                    			boolean gpsSupported = cellModemService.isGpsSupported();
					                    			s_logger.debug("Setting GPS supported to {}", gpsSupported);
					                    			((GwtModemInterfaceConfig)gwtNetConfig).setGpsSupported(gpsSupported);
					                    		} catch (KuraException e) {
					                    			s_logger.warn("Failed to get GPS supported from modem", e);
					                    		}
					                    	}
										}
									}

                                    // set as DHCP - populate current address
									gwtModemConfig.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
                                    if(addressConfig.getAddress() != null) {
                                        gwtModemConfig.setIpAddress(addressConfig.getAddress().getHostAddress());
                                    }
                                    if(addressConfig.getNetmask() != null) {
                                        gwtModemConfig.setSubnetMask(addressConfig.getNetmask().getHostAddress());
                                    }
									
									gwtModemConfig.setDialString(modemConfig.getDialString());
									
									AuthType authType = modemConfig.getAuthType();
									if (authType == AuthType.AUTO) {
										gwtModemConfig.setAuthType(GwtModemAuthType.netModemAuthAUTO);
									} else if(authType == AuthType.CHAP) {
										gwtModemConfig.setAuthType(GwtModemAuthType.netModemAuthCHAP);
									} else if(authType == AuthType.PAP) {
										gwtModemConfig.setAuthType(GwtModemAuthType.netModemAuthPAP);
									} else {
										gwtModemConfig.setAuthType(GwtModemAuthType.netModemAuthNONE);
									}

									gwtModemConfig.setUsername(modemConfig.getUsername());
									
									gwtModemConfig.setPassword(modemConfig.getPassword());
                                    
                                    gwtModemConfig.setPppNum(modemConfig.getPppNumber());
                                     
                                    gwtModemConfig.setResetTimeout(modemConfig.getResetTimeout());
                                    
                                    gwtModemConfig.setPersist(modemConfig.isPersist());
                                    
                                    gwtModemConfig.setMaxFail(modemConfig.getMaxFail());
                                    
                                    gwtModemConfig.setIdle(modemConfig.getIdle());
                                    
                                    gwtModemConfig.setActiveFilter(modemConfig.getActiveFilter());
                                    
                                    gwtModemConfig.setLcpEchoInterval(modemConfig.getLcpEchoInterval());
                                    
                                    gwtModemConfig.setLcpEchoFailure(modemConfig.getLcpEchoFailure());
                                    
                                    gwtModemConfig.setGpsEnabled(modemConfig.isGpsEnabled());
									
									gwtModemConfig.setProfileID(modemConfig.getProfileID());
                                    
									PdpType pdpType = modemConfig.getPdpType();
									if(pdpType == PdpType.IP) {
										gwtModemConfig.setPdpType(GwtModemPdpType.netModemPdpIP);
									} else if(pdpType == PdpType.PPP) {
										gwtModemConfig.setPdpType(GwtModemPdpType.netModemPdpPPP);
									} else if(pdpType == PdpType.IPv6) {
										gwtModemConfig.setPdpType(GwtModemPdpType.netModemPdpIPv6);
									} else {
									    gwtModemConfig.setPdpType(GwtModemPdpType.netModemPdpUnknown);
									}
									
									gwtModemConfig.setApn(modemConfig.getApn());
									
									gwtModemConfig.setDataCompression(modemConfig.getDataCompression());
									
									gwtModemConfig.setHeaderCompression(modemConfig.getHeaderCompression());
                                    
                                    ModemConnectionStatus connectionStatus = ((ModemInterfaceAddressConfig)addressConfig).getConnectionStatus();
                                    if(connectionStatus == ModemConnectionStatus.DISCONNECTED) {
                                        gwtModemConfig.setHwState(NetInterfaceState.DISCONNECTED.name());
                                    } else if(connectionStatus == ModemConnectionStatus.CONNECTING) {
                                        gwtModemConfig.setHwState(NetInterfaceState.IP_CONFIG.name());
                                    } else if(connectionStatus == ModemConnectionStatus.CONNECTED) {
                                        gwtModemConfig.setHwState(NetInterfaceState.ACTIVATED.name());
                                    } else {
                                        gwtModemConfig.setHwState(NetInterfaceState.UNKNOWN.name());
                                    }
                                    
                                    gwtModemConfig.setConnectionType(((ModemInterfaceAddressConfig)addressConfig).getConnectionType().name());
								}

								if(netConfig instanceof DhcpServerConfigIP4) {									
									s_logger.debug("Setting up DhcpServerConfigIP4: " + ((DhcpServerConfigIP4) netConfig).getRangeStart().getHostAddress() + " to " + ((DhcpServerConfigIP4) netConfig).getRangeEnd().getHostAddress());
									s_logger.debug("Setting up DhcpServerConfigIP4: " + ((DhcpServerConfigIP4) netConfig).toString());
									
									isDhcpServerEnabled = ((DhcpServerConfigIP4) netConfig).isEnabled();

									gwtNetConfig.setRouterDhcpBeginAddress(((DhcpServerConfigIP4) netConfig).getRangeStart().getHostAddress());
									gwtNetConfig.setRouterDhcpEndAddress(((DhcpServerConfigIP4) netConfig).getRangeEnd().getHostAddress());
									gwtNetConfig.setRouterDhcpSubnetMask(((DhcpServerConfigIP4) netConfig).getSubnetMask().getHostAddress());
									gwtNetConfig.setRouterDhcpDefaultLease(((DhcpServerConfigIP4) netConfig).getDefaultLeaseTime());
									gwtNetConfig.setRouterDhcpMaxLease(((DhcpServerConfigIP4) netConfig).getMaximumLeaseTime());
									gwtNetConfig.setRouterDnsPass(((DhcpServerConfigIP4) netConfig).isPassDns());
								}

								if(netConfig instanceof FirewallAutoNatConfig) {
									s_logger.debug("Setting up FirewallAutoNatConfig");
									
									isNatEnabled = true;
								}

								//TODO - only dealing with IPv4 right now
							}
							
							//set up the DHCP and NAT config
							if(isDhcpServerEnabled && isNatEnabled) {
								s_logger.debug("setting router mode to DHCP and NAT");
								gwtNetConfig.setRouterMode(GwtNetRouterMode.netRouterDchpNat.name());
							} else if(isDhcpServerEnabled && !isNatEnabled) {
								s_logger.debug("setting router mode to DHCP only");
								gwtNetConfig.setRouterMode(GwtNetRouterMode.netRouterDchp.name());
							} else if(!isDhcpServerEnabled && isNatEnabled) {
								s_logger.debug("setting router mode to NAT only");
								gwtNetConfig.setRouterMode(GwtNetRouterMode.netRouterNat.name());
							} else {
								s_logger.debug("setting router mode to disabled");
								gwtNetConfig.setRouterMode(GwtNetRouterMode.netRouterOff.name());
							}
						}
					}
				}
				
				gwtNetConfigs.add(gwtNetConfig);
			}
		} 
		catch (Throwable t) {
			KuraExceptionHandler.handle(t);
		}
		
		s_logger.debug("Returning");
		return new BaseListLoadResult<GwtNetInterfaceConfig>(gwtNetConfigs);
	}


	
	public void updateNetInterfaceConfigurations(GwtNetInterfaceConfig config) 
		throws GwtKuraException 
	{		
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		
		s_logger.debug("config.getStatus(): " + config.getStatus());
		
		boolean autoConnect = true;
		if(GwtNetIfStatus.netIPv4StatusDisabled.name().equals(config.getStatus())) {
			autoConnect = false;
		}
		
		try {
			// Interface status
			NetInterfaceStatus netInterfaceStatus = null;
			if(config.getStatus().equals(GwtNetIfStatus.netIPv4StatusDisabled.name())) {
				netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
			} else if(config.getStatus().equals(GwtNetIfStatus.netIPv4StatusEnabledLAN.name())) {
				netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledLAN;
			} else if(config.getStatus().equals(GwtNetIfStatus.netIPv4StatusEnabledWAN.name())) {
				netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledWAN;
			}
			
			
			//Set up configs
			List<NetConfig> netConfigs = new ArrayList<NetConfig>();

			// Initialize NetConfigIP4 object
			NetConfigIP4 netConfig4 = new NetConfigIP4(netInterfaceStatus, autoConnect);
			
			//build the appropriate NetConfig objects for ethernet type
			if(config.getHwTypeEnum() == GwtNetIfType.ETHERNET ||
					config.getHwTypeEnum() == GwtNetIfType.WIFI ||
					config.getHwTypeEnum() == GwtNetIfType.MODEM) {
				
				s_logger.debug("config.getConfigMode(): " + config.getConfigMode());
                String regexp = "[\\s,;\\n\\t]+";
								
				if(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name().equals(config.getConfigMode())) {
					s_logger.debug("mode is DHCP");
					netConfig4.setDhcp(true);
				} else {
					s_logger.debug("mode is STATIC");
					netConfig4.setDhcp(false);
					
					if(config.getIpAddress() != null && !config.getIpAddress().isEmpty()) {
						s_logger.debug("setting address: " + config.getIpAddress());
						netConfig4.setAddress((IP4Address) IPAddress.parseHostAddress(config.getIpAddress()));
					}
					
					if(config.getSubnetMask() != null && !config.getSubnetMask().isEmpty()) {
						s_logger.debug("setting subnet mask: " + config.getSubnetMask());
						netConfig4.setSubnetMask((IP4Address) IPAddress.parseHostAddress(config.getSubnetMask()));
					}
					if(config.getGateway() != null && !config.getGateway().isEmpty()) {
						s_logger.debug("setting gateway: " + config.getGateway());
						netConfig4.setGateway((IP4Address) IPAddress.parseHostAddress(config.getGateway()));
					}
					
					String[] winServersString = config.getSearchDomains().split(regexp);
	                if(winServersString != null && winServersString.length > 0) {
	                    IP4Address winServer;
	                    List<IP4Address> dnsServers = new ArrayList<IP4Address>();
	                    for (String winsEntry : winServersString) {
	                        if(!winsEntry.trim().isEmpty()) {
	                        	s_logger.debug("setting WINs: " + winsEntry);
	                            winServer = (IP4Address) IPAddress.parseHostAddress(winsEntry);
	                            dnsServers.add(winServer);
	                        }
	                    }
	                    netConfig4.setDnsServers(dnsServers);
	                }
				}
				
                String[] dnsServersString = config.getDnsServers().split(regexp);
                if(dnsServersString != null && dnsServersString.length > 0) {
                    IP4Address dnsServer;
                    List<IP4Address> dnsServers = new ArrayList<IP4Address>();
                    for (String dnsEntry : dnsServersString) {
                        if(!dnsEntry.trim().isEmpty()) {
                        	s_logger.debug("setting DNS: " + dnsEntry);
                            dnsServer = (IP4Address) IPAddress.parseHostAddress(dnsEntry);
                            dnsServers.add(dnsServer);
                        }
                    }
                    netConfig4.setDnsServers(dnsServers);
                }

				netConfigs.add(netConfig4);
				
				//TODO - add IPv6 support later...
			
				//Set up DHCP and NAT
				if(!GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name().equals(config.getConfigMode())) {
					List<NetConfig> dhcpConfigs = getDhcpConfig(config); // <-- 
					if(dhcpConfigs != null) {
						s_logger.debug("Adding dhcp and/or nat configs to interface update config");
						netConfigs.addAll(dhcpConfigs);
					}
				}
				
				if(config.getHwTypeEnum() == GwtNetIfType.ETHERNET) {
					nas.updateEthernetInterfaceConfig(config.getName(), autoConnect, config.getHwMTU(), netConfigs);
				}
			}
			
			if(config.getHwTypeEnum() == GwtNetIfType.WIFI) {
				
				if (config instanceof GwtWifiNetInterfaceConfig) {
					//WifiConfig wifiConfig = new WifiConfig();					
					GwtWifiConfig gwtWifiConfig = ((GwtWifiNetInterfaceConfig) config).getActiveWifiConfig();

					if(gwtWifiConfig != null) {
						gwtWifiConfig.setWirelessMode(((GwtWifiNetInterfaceConfig)config).getWirelessMode());
						WifiConfig wifiConfig = this.getWifiConfig(gwtWifiConfig);
						netConfigs.add(wifiConfig);
						nas.updateWifiInterfaceConfig(config.getName(), autoConnect, null, netConfigs);
					}
				}
			} else if(config.getHwTypeEnum() == GwtNetIfType.MODEM) {
				if (config instanceof GwtModemInterfaceConfig) {
					GwtModemInterfaceConfig gwtModemConfig = (GwtModemInterfaceConfig) config;
					
					ModemConfig modemConfig = new ModemConfig();

					String serialNum = gwtModemConfig.getHwSerial();
					String modemId = gwtModemConfig.getModemId();
					int pppNum = gwtModemConfig.getPppNum();
					
					// modem enabled/disabled
				    if(netInterfaceStatus.equals(NetInterfaceStatus.netIPv4StatusEnabledWAN)) {
					    modemConfig.setEnabled(true);
					} else {
					    modemConfig.setEnabled(false);
					}
					
					modemConfig.setApn(gwtModemConfig.getApn());
					modemConfig.setPppNumber(gwtModemConfig.getPppNum());
					modemConfig.setDataCompression(gwtModemConfig.getDataCompression());
					modemConfig.setDialString(gwtModemConfig.getDialString());
					modemConfig.setHeaderCompression(gwtModemConfig.getHeaderCompression());
					modemConfig.setPassword(gwtModemConfig.getPassword());
					modemConfig.setUsername(gwtModemConfig.getUsername());
					modemConfig.setResetTimeout(gwtModemConfig.getResetTimeout());
					modemConfig.setPersist(gwtModemConfig.isPersist());
					modemConfig.setMaxFail(gwtModemConfig.getMaxFail());
					modemConfig.setIdle(gwtModemConfig.getIdle());
					modemConfig.setActiveFilter(gwtModemConfig.getActiveFilter());
					modemConfig.setLcpEchoInterval(gwtModemConfig.getLcpEchoInterval());
					modemConfig.setLcpEchoFailure(gwtModemConfig.getLcpEchoFailure());
					modemConfig.setGpsEnabled(gwtModemConfig.isGpsEnabled());
					
					GwtModemAuthType authType = gwtModemConfig.getAuthType();
					if (authType != null) {
						if (authType.equals(GwtModemAuthType.netModemAuthNONE)) {
							modemConfig.setAuthType(ModemConfig.AuthType.NONE);
						} else if (authType.equals(GwtModemAuthType.netModemAuthAUTO)) {
							modemConfig.setAuthType(ModemConfig.AuthType.AUTO);
						} else if (authType.equals(GwtModemAuthType.netModemAuthCHAP)) {
							modemConfig.setAuthType(ModemConfig.AuthType.CHAP);
						} else if (authType.equals(GwtModemAuthType.netModemAuthPAP)) {
							modemConfig.setAuthType(ModemConfig.AuthType.PAP);
						}
					}
					
					GwtModemPdpType pdpType = gwtModemConfig.getPdpType();
					if (pdpType != null) {
						if (pdpType.equals(GwtModemPdpType.netModemPdpIP)) {
							modemConfig.setPdpType(ModemConfig.PdpType.IP);
						} else if (pdpType.equals(GwtModemPdpType.netModemPdpIPv6)) {
							modemConfig.setPdpType(ModemConfig.PdpType.IPv6);
						} else if (pdpType.equals(GwtModemPdpType.netModemPdpPPP)) {
							modemConfig.setPdpType(ModemConfig.PdpType.PPP);
						} else {
						    modemConfig.setPdpType(ModemConfig.PdpType.UNKNOWN);
						}
					}
					
					netConfigs.add(modemConfig);
					
					nas.updateModemInterfaceConfig(config.getName(), serialNum, modemId, pppNum, autoConnect, -1, netConfigs);
				}
			} else {
				//TODO - more types
			}

			
		} catch(Exception e) {
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	
	public ListLoadResult<GwtFirewallOpenPortEntry> findDeviceFirewallOpenPorts() throws GwtKuraException 
	{
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		List<GwtFirewallOpenPortEntry> gwtOpenPortEntries = new ArrayList<GwtFirewallOpenPortEntry>();
		
		try {
			List<NetConfig> firewallConfigs = nas.getFirewallConfiguration();
			if(firewallConfigs != null && firewallConfigs.size() > 0) {
				for(NetConfig netConfig : firewallConfigs) {
					if(netConfig instanceof FirewallOpenPortConfigIP4) {
						s_logger.debug("findDeviceFirewallOpenPorts() :: adding new Open Port Entry: " + ((FirewallOpenPortConfigIP4) netConfig).getPort());
						GwtFirewallOpenPortEntry entry = new GwtFirewallOpenPortEntry();
						entry.setPort(((FirewallOpenPortConfigIP4) netConfig).getPort());
						entry.setProtocol(((FirewallOpenPortConfigIP4) netConfig).getProtocol().toString());
						entry.setPermittedNetwork(((FirewallOpenPortConfigIP4) netConfig).getPermittedNetwork().getIpAddress().getHostAddress() + "/" + ((FirewallOpenPortConfigIP4) netConfig).getPermittedNetwork().getPrefix());
						entry.setPermittedInterfaceName(((FirewallOpenPortConfigIP4) netConfig).getPermittedInterfaceName());
						entry.setUnpermittedInterfaceName(((FirewallOpenPortConfigIP4) netConfig).getUnpermittedInterfaceName());
						entry.setPermittedMAC(((FirewallOpenPortConfigIP4) netConfig).getPermittedMac());
						entry.setSourcePortRange(((FirewallOpenPortConfigIP4) netConfig).getSourcePortRange());
						
						gwtOpenPortEntries.add(entry);
					}
				}
			}
			
			return new BaseListLoadResult<GwtFirewallOpenPortEntry>(gwtOpenPortEntries);
			
		} catch (KuraException e) {
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public ListLoadResult<GwtWifiHotspotEntry> findWifiHotspots(String interfaceName) throws GwtKuraException {
		
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
		List<GwtWifiHotspotEntry> gwtWifiHotspotsEntries = new ArrayList<GwtWifiHotspotEntry>();

		try {
			Map<String, WifiHotspotInfo> wifiHotspotInfoMap = nas.getWifiHotspots(interfaceName);
			if ((wifiHotspotInfoMap != null) && (!wifiHotspotInfoMap.isEmpty())) {
				Collection<WifiHotspotInfo> wifiHotspotInfoCollection = wifiHotspotInfoMap.values();
				Iterator<WifiHotspotInfo> it = wifiHotspotInfoCollection.iterator();
				while (it.hasNext()) {
					WifiHotspotInfo wifiHotspotInfo = it.next();
					if (wifiHotspotInfo.getChannel() <= systemService.getKuraWifiTopChannel()) {
						GwtWifiHotspotEntry gwtWifiHotspotEntry = new GwtWifiHotspotEntry();
						gwtWifiHotspotEntry.setMacAddress(wifiHotspotInfo.getMacAddress());
						gwtWifiHotspotEntry.setSSID(wifiHotspotInfo.getSsid());
						gwtWifiHotspotEntry.setsignalStrength(wifiHotspotInfo.getSignalLevel());
						gwtWifiHotspotEntry.setChannel(wifiHotspotInfo.getChannel());
						gwtWifiHotspotEntry.setFrequency(wifiHotspotInfo.getFrequency());
						
						if ((wifiHotspotInfo.getSecurity() == WifiSecurity.NONE)
								|| (wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_NONE)) {
							gwtWifiHotspotEntry.setSecurity("None");
						} else if (wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_WEP) {
							gwtWifiHotspotEntry.setSecurity("WEP");
						} else if (wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_WPA) {
							gwtWifiHotspotEntry.setSecurity("WPA");
						} else if (wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_WPA2) {
							gwtWifiHotspotEntry.setSecurity("WPA2");
						} else if (wifiHotspotInfo.getSecurity() == WifiSecurity.SECURITY_WPA_WPA2) {
							gwtWifiHotspotEntry.setSecurity("WPA/WPA2");
						}
						gwtWifiHotspotsEntries.add(gwtWifiHotspotEntry);
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			KuraExceptionHandler.handle(t);
		}
		
		return new BaseListLoadResult<GwtWifiHotspotEntry>(gwtWifiHotspotsEntries);
	}

	public boolean verifyWifiCredentials(String interfaceName, GwtWifiConfig gwtWifiConfig) throws GwtKuraException {
		
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		WifiConfig wifiConfig = getWifiConfig(gwtWifiConfig);
		boolean status = nas.verifyWifiCredentials(interfaceName, wifiConfig, 60);
		return status;
	}
	
	public ListLoadResult<GwtFirewallPortForwardEntry> findDeviceFirewallPortForwards() throws GwtKuraException 
	{
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		List<GwtFirewallPortForwardEntry> gwtPortForwardEntries = new ArrayList<GwtFirewallPortForwardEntry>();
		
		try {
			List<NetConfig> firewallConfigs = nas.getFirewallConfiguration();
			if(firewallConfigs != null && firewallConfigs.size() > 0) {
				for(NetConfig netConfig : firewallConfigs) {
					if(netConfig instanceof FirewallPortForwardConfigIP4) {
						s_logger.debug("findDeviceFirewallPortForwards() :: adding new Port Forward Entry");
						GwtFirewallPortForwardEntry entry = new GwtFirewallPortForwardEntry();
						entry.setInboundInterface(((FirewallPortForwardConfigIP4) netConfig).getInboundInterface());
						entry.setOutboundInterface(((FirewallPortForwardConfigIP4) netConfig).getOutboundInterface());
						entry.setAddress(((FirewallPortForwardConfigIP4) netConfig).getAddress().getHostAddress());
						entry.setProtocol(((FirewallPortForwardConfigIP4) netConfig).getProtocol().toString());
						entry.setInPort(((FirewallPortForwardConfigIP4) netConfig).getInPort());
						entry.setOutPort(((FirewallPortForwardConfigIP4) netConfig).getOutPort());
						String masquerade = ((FirewallPortForwardConfigIP4)netConfig).isMasquerade()? "yes" : "no";
						entry.setMasquerade(masquerade);
						entry.setPermittedNetwork(((FirewallPortForwardConfigIP4) netConfig).getPermittedNetwork().toString());
						entry.setPermittedMAC(((FirewallPortForwardConfigIP4) netConfig).getPermittedMac());
						entry.setSourcePortRange(((FirewallPortForwardConfigIP4) netConfig).getSourcePortRange());
						
						gwtPortForwardEntries.add(entry);
					}
				}
			}
			
			return new BaseListLoadResult<GwtFirewallPortForwardEntry>(gwtPortForwardEntries);
			
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public ListLoadResult<GwtFirewallNatEntry> findDeficeFirewallNATs() throws GwtKuraException {
		
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		List<GwtFirewallNatEntry> gwtNatEntries = new ArrayList<GwtFirewallNatEntry>();
		
		try {
			List<NetConfig> firewallConfigs = nas.getFirewallConfiguration();
			if(firewallConfigs != null && firewallConfigs.size() > 0) {
				for(NetConfig netConfig : firewallConfigs) {
					if (netConfig instanceof FirewallNatConfig) {
						s_logger.debug("findDeficeFirewallNATs() :: adding new NAT Entry");
						GwtFirewallNatEntry entry = new GwtFirewallNatEntry();
						entry.setInInterface(((FirewallNatConfig)netConfig).getSourceInterface());
						entry.setOutInterface(((FirewallNatConfig)netConfig).getDestinationInterface());
						entry.setProtocol(((FirewallNatConfig)netConfig).getProtocol());
						entry.setSourceNetwork(((FirewallNatConfig)netConfig).getSource());
						entry.setDestinationNetwork(((FirewallNatConfig)netConfig).getDestination());
						String masquerade = ((FirewallNatConfig)netConfig).isMasquerade()? "yes" : "no";
						entry.setMasquerade(masquerade);
						gwtNatEntries.add(entry);
					}
				}
			}
			
			return new BaseListLoadResult<GwtFirewallNatEntry>(gwtNatEntries);
			
		} catch (KuraException e) {
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	// -------------------------------------------------------------------------------------
	// 
	//   Private Methods
	//
	// -------------------------------------------------------------------------------------

	private List<NetConfig> getDhcpConfig(GwtNetInterfaceConfig config) throws KuraException 
	{
		//Setup the DHCP and NAT if necessary
		String routerMode = config.getRouterMode();
		if(routerMode.equals(GwtNetRouterMode.netRouterOff.name())) {
			s_logger.debug("DCHP and NAT are disabled");
			return null;
		} else if(routerMode.equals(GwtNetRouterMode.netRouterDchp.name()) || routerMode.equals(GwtNetRouterMode.netRouterDchpNat.name()) || routerMode.equals(GwtNetRouterMode.netRouterNat.name())) {
				try {
					List<NetConfig> netConfigs = new ArrayList<NetConfig>();

					if(routerMode.equals(GwtNetRouterMode.netRouterDchp.name()) || routerMode.equals(GwtNetRouterMode.netRouterDchpNat.name())) {
						int defaultLeaseTime = config.getRouterDhcpDefaultLease();
						int maximumLeaseTime = config.getRouterDhcpMaxLease();
						IP4Address routerAddress = (IP4Address) IPAddress.parseHostAddress(config.getIpAddress());
						IP4Address rangeStart = (IP4Address) IPAddress.parseHostAddress(config.getRouterDhcpBeginAddress());
						IP4Address rangeEnd = (IP4Address) IPAddress.parseHostAddress(config.getRouterDhcpEndAddress());
						boolean passDns = config.getRouterDnsPass();

						IP4Address subnetMask = (IP4Address) IPAddress.parseHostAddress(config.getRouterDhcpSubnetMask());
						IP4Address subnet = (IP4Address) IPAddress.parseHostAddress(NetworkUtil.calculateNetwork(config.getIpAddress(), config.getSubnetMask()));						
						short prefix = NetworkUtil.getNetmaskShortForm(subnetMask.getHostAddress());
						
						//Use our IP as the DNS server and we'll use named to proxy DNS queries
						List<IP4Address> dnsServers = new ArrayList<IP4Address>();
						dnsServers.add((IP4Address) IPAddress.parseHostAddress(config.getIpAddress()));

						s_logger.debug("DhcpServerConfigIP4 - start:" + rangeStart.getHostAddress() + ", end:" + rangeEnd.getHostAddress() + ", prefix:" + prefix + ", subnet:" + subnet.getHostAddress() + ", subnetMask:" + subnetMask.getHostAddress());
						DhcpServerConfigIP4 dhcpServerConfigIP4 = new DhcpServerConfigIP4(config.getName(), true, subnet, routerAddress, subnetMask, defaultLeaseTime, maximumLeaseTime,
																						prefix, rangeStart, rangeEnd, passDns, dnsServers);
								
						netConfigs.add(dhcpServerConfigIP4);
					}

					if(routerMode.equals(GwtNetRouterMode.netRouterDchpNat.name()) || routerMode.equals(GwtNetRouterMode.netRouterNat.name())) {

						/*
                                IPAddress m_sourceNetwork;                      //192.168.1.0
                                IPAddress m_netmask;                            //255.255.255.0
                                String m_sourceInterface;                       //eth0
                                String m_destinationInterface;          		//ppp0 or something similar
                                boolean m_masquerade;                           //yes
						 */

						String sourceInterface = config.getName();
						String destinationInterface = "unknown";                        //dynamic and defined at runtime
						boolean masquerade = true;

						FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig(sourceInterface, destinationInterface, masquerade);
						netConfigs.add(natConfig);
					}

					return netConfigs;
				} catch(Exception e) {
					throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
				}
		} else {
			s_logger.error("Unsupported routerMode: " + routerMode);
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "Unsupported routerMode: " + routerMode);
		}
	}
	
	public void updateDeviceFirewallOpenPorts( List<GwtFirewallOpenPortEntry> entries) throws GwtKuraException {
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallOpenPortConfigIPs = new ArrayList<FirewallOpenPortConfigIP<? extends IPAddress>>();
		s_logger.debug("updating open ports");
		
		try {
			for(GwtFirewallOpenPortEntry entry : entries) {
				String network = null;
				String prefix = null;
				
				if(entry.getPermittedNetwork() != null) {
					String[] parts = entry.getPermittedNetwork().split("/");
					network = parts[0];
					prefix = parts[1];
				}
				
				FirewallOpenPortConfigIP<IP4Address> firewallOpenPortConfigIP = new FirewallOpenPortConfigIP4();
				firewallOpenPortConfigIP.setPort(entry.getPort());
				firewallOpenPortConfigIP.setProtocol(NetProtocol.valueOf(entry.getProtocol()));
				if(network != null && prefix != null) {
					firewallOpenPortConfigIP.setPermittedNetwork(new NetworkPair<IP4Address>((IP4Address)IPAddress.parseHostAddress(network), Short.parseShort(prefix)));
				}
				firewallOpenPortConfigIP.setPermittedInterfaceName(entry.getPermittedInterfaceName());
				firewallOpenPortConfigIP.setUnpermittedInterfaceName(entry.getUnpermittedInterfaceName());
				firewallOpenPortConfigIP.setPermittedMac(entry.getPermittedMAC());
				firewallOpenPortConfigIP.setSourcePortRange(entry.getSourcePortRange());
				
				s_logger.debug("adding open port entry for " + entry.getPort());
				firewallOpenPortConfigIPs.add(firewallOpenPortConfigIP);
			}
			
			nas.setFirewallOpenPortConfiguration(firewallOpenPortConfigIPs);
		} catch (KuraException e) {
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public void updateDeviceFirewallPortForwards(List<GwtFirewallPortForwardEntry> entries) throws GwtKuraException {
		
		s_logger.debug("updateDeviceFirewallPortForwards() :: updating port forward entries");
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallPortForwardConfigIPs = new ArrayList<FirewallPortForwardConfigIP<? extends IPAddress>>();
		
		try {
			for(GwtFirewallPortForwardEntry entry : entries) {
				String network = null;
				String prefix = null;
				
				if(entry.getPermittedNetwork() != null) {
					String[] parts = entry.getPermittedNetwork().split("/");
					network = parts[0];
					prefix = parts[1];
				}
				
				FirewallPortForwardConfigIP<IP4Address> firewallPortForwardConfigIP = new FirewallPortForwardConfigIP4();
				firewallPortForwardConfigIP.setInboundInterface(entry.getInboundInterface());
				firewallPortForwardConfigIP.setOutboundInterface(entry.getOutboundInterface());
				firewallPortForwardConfigIP.setAddress((IP4Address) IPAddress.parseHostAddress(entry.getAddress()));
				firewallPortForwardConfigIP.setProtocol(NetProtocol.valueOf(entry.getProtocol()));
				firewallPortForwardConfigIP.setInPort(entry.getInPort());
				firewallPortForwardConfigIP.setOutPort(entry.getOutPort());
				boolean masquerade = entry.getMasquerade().equals("yes")? true : false;
				firewallPortForwardConfigIP.setMasquerade(masquerade);
				if(network != null && prefix != null) {
					firewallPortForwardConfigIP.setPermittedNetwork(new NetworkPair<IP4Address>((IP4Address)IPAddress.parseHostAddress(network), Short.parseShort(prefix)));
				}
				firewallPortForwardConfigIP.setPermittedMac(entry.getPermittedMAC());
				firewallPortForwardConfigIP.setSourcePortRange(entry.getSourcePortRange());
				
				s_logger.debug("adding port forward entry for inbound iface {} - port {}", entry.getInboundInterface(), entry.getInPort());
				firewallPortForwardConfigIPs.add(firewallPortForwardConfigIP);
			}
			
			nas.setFirewallPortForwardingConfiguration(firewallPortForwardConfigIPs);
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public void updateDeviceFirewallNATs(List<GwtFirewallNatEntry> entries) throws GwtKuraException {
		
		s_logger.debug("updateDeviceFirewallNATs() :: updating NAT entries");
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		List<FirewallNatConfig> firewallNatConfigs = new ArrayList<FirewallNatConfig>();
		
		for (GwtFirewallNatEntry entry : entries) {

			String srcNetwork = entry.getSourceNetwork();
			String dstNetwork = entry.getDestinationNetwork();
			if (srcNetwork == null) {
				srcNetwork = "0.0.0.0/0";
			}
			if (dstNetwork == null) {
				dstNetwork = "0.0.0.0/0";
			}
			
			boolean masquerade = entry.getMasquerade().equals("yes")? true : false;
				
			FirewallNatConfig firewallNatConfig = new FirewallNatConfig(
					entry.getInInterface(), entry.getOutInterface(),
					entry.getProtocol(), srcNetwork, dstNetwork, masquerade);
			
			firewallNatConfigs.add(firewallNatConfig);
		}
		
		try {
			nas.setFirewallNatConfiguration(firewallNatConfigs);
		} catch (KuraException e) {
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public void renewDhcpLease(String interfaceName) throws GwtKuraException {
		NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
		try {
			nas.renewDhcpLease(interfaceName);
		} catch (KuraException e) {
			e.printStackTrace();
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
		}
	}
	
	public void rollbackDefaultConfiguration() {
		s_logger.debug("Rolling back to default configuration ...");
		try {
			NetworkAdminService nas = ServiceLocator.getInstance().getService(NetworkAdminService.class);
			if (nas != null) {
				try {
					nas.rollbackDefaultConfiguration();
					s_logger.debug("ESF is set to default configuration.");
				} catch (KuraException e) {
					e.printStackTrace();
					throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
				}
			}
		} catch (GwtKuraException e) {
			s_logger.warn("Failed to obtain the NetworkAdminService. This is ok if running the 'No-Network' version.");
		}
	}
	
	private WifiConfig getWifiConfig(GwtWifiConfig gwtWifiConfig) {
		
		WifiConfig wifiConfig = null;
		if (gwtWifiConfig != null) {
			
			wifiConfig = new WifiConfig();		
			String mode = gwtWifiConfig.getWirelessMode();
			if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
				wifiConfig.setMode(WifiMode.MASTER);
			} else if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeStation.name())) {
				wifiConfig.setMode(WifiMode.INFRA);						
			} else if (mode != null && mode.equals(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name())) {
				wifiConfig.setMode(WifiMode.ADHOC);
			} else {
				wifiConfig.setMode(WifiMode.UNKNOWN);
			}
	        
	        // ssid
	        wifiConfig.setSSID(gwtWifiConfig.getWirelessSsid());
			
			// driver
			wifiConfig.setDriver(gwtWifiConfig.getDriver());
			
			// radio mode
			GwtWifiRadioMode radioMode = (gwtWifiConfig.getRadioModeEnum());
			if (radioMode == GwtWifiRadioMode.netWifiRadioModeA) {
			    wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
				wifiConfig.setHardwareMode("a");
			} else if (radioMode.equals(GwtWifiRadioMode.netWifiRadioModeB)) {
			    wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
				wifiConfig.setHardwareMode("b");
			} else if (radioMode.equals(GwtWifiRadioMode.netWifiRadioModeBG)) {
			    wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211g);
				wifiConfig.setHardwareMode("g");
			} else if (radioMode.equals(GwtWifiRadioMode.netWifiRadioModeBGN)) {
			    wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211nHT20);
				wifiConfig.setHardwareMode("n");
			}
			
			// channel
			ArrayList<Integer> alChannels = (gwtWifiConfig.getChannels());
			if (alChannels != null) {
				int [] channels = new int [alChannels.size()];
				for (int i = 0; i < channels.length; i++) {
					channels[i] = alChannels.get(i).intValue();
				}
				wifiConfig.setChannels(channels);
			}
			
			// security
			wifiConfig.setSecurity(WifiSecurity.SECURITY_NONE);
			String security = (gwtWifiConfig.getSecurity());
			if (security != null) {
				if (security.equals(GwtWifiSecurity.netWifiSecurityWPA.name())) {
					//wifiConfig.setSecurity(WifiSecurity.KEY_MGMT_PSK);
					wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA);
				} else if (security.equals(GwtWifiSecurity.netWifiSecurityWPA2.name())) {
					//wifiConfig.setSecurity(WifiSecurity.KEY_MGMT_PSK);
					wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA2);
				} else if (security.equals(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name())) {
					//wifiConfig.setSecurity(WifiSecurity.KEY_MGMT_PSK);
					wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA_WPA2);
				} else if (security.equals(GwtWifiSecurity.netWifiSecurityWEP.name())) {
					//wifiConfig.setSecurity(WifiSecurity.PAIR_WEP104);
					wifiConfig.setSecurity(WifiSecurity.SECURITY_WEP);
				}
			}
			
			String pairwiseCiphers = (gwtWifiConfig.getPairwiseCiphers());
			if (pairwiseCiphers != null) {
				if (pairwiseCiphers.equals(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name())) {
					wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP_TKIP);
				} else if (pairwiseCiphers.equals(GwtWifiCiphers.netWifiCiphers_TKIP.name())) {
					wifiConfig.setPairwiseCiphers(WifiCiphers.TKIP);
				} else if (pairwiseCiphers.equals(GwtWifiCiphers.netWifiCiphers_CCMP.name())) {
					wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP);
				}
			}
			
			String groupCiphers = (gwtWifiConfig.getGroupCiphers());
			if (groupCiphers != null) {
				if (groupCiphers.equals(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name())) {
					wifiConfig.setGroupCiphers(WifiCiphers.CCMP_TKIP);
				} else if (groupCiphers.equals(GwtWifiCiphers.netWifiCiphers_TKIP.name())) {
					wifiConfig.setGroupCiphers(WifiCiphers.TKIP);
				} else if (groupCiphers.equals(GwtWifiCiphers.netWifiCiphers_CCMP.name())) {
					wifiConfig.setGroupCiphers(WifiCiphers.CCMP);
				}
			}
			
			// bgscan
			String bgscanModule = (gwtWifiConfig.getBgscanModule());
			if (bgscanModule != null) {
				WifiBgscanModule wifiBgscanModule = null;
				if (bgscanModule.equals(GwtWifiBgscanModule.netWifiBgscanMode_NONE.name())) {
					wifiBgscanModule = WifiBgscanModule.NONE;
				} else if (bgscanModule.equals(GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name())) {
					wifiBgscanModule = WifiBgscanModule.SIMPLE;
				} else if (bgscanModule.equals(GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name())) {
					wifiBgscanModule = WifiBgscanModule.LEARN;
				}
					
				int bgscanRssiThreshold = (gwtWifiConfig.getBgscanRssiThreshold());
				int bgscanShortInterval = (gwtWifiConfig.getBgscanShortInterval());
				int bgscanLongInterval = (gwtWifiConfig.getBgscanLongInterval());
	
				WifiBgscan wifiBgscan = new WifiBgscan(wifiBgscanModule,
						bgscanShortInterval, bgscanRssiThreshold,
						bgscanLongInterval);
				wifiConfig.setBgscan(wifiBgscan);
			}
			
			// passkey
			wifiConfig.setPasskey(gwtWifiConfig.getPassword());
			
			// ping access point?
			wifiConfig.setPingAccessPoint(gwtWifiConfig.pingAccessPoint());
			
			// ignore SSID?
			wifiConfig.setIgnoreSSID(gwtWifiConfig.ignoreSSID());
		}
		
		return wifiConfig;
	}
	
	/*
	private int getChannelFrequencyMHz(int channel) {
		
		int frequency = -1;
		if ((channel >=1) && (channel <=13)) {
			frequency = 2407 + channel * 5;
		}
		return frequency;
	}
	*/
}
