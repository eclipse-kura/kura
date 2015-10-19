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
package org.eclipse.kura.net;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiHotspotInfo;

/**
 * Service API for getting and setting network interface configurations.
 * 
 * @author eurotech
 *
 */
public interface NetworkAdminService {
	
    /**
     * Returns a list of all of the configurations associated with all of the interfaces on
     * the system.
     *
     * @return list of NetInterfaceConfigs on the system
     * @throws KuraException
     */
    public List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getNetworkInterfaceConfigs() throws KuraException;

	
	/**
	 * Returns the configuration information for the specified NetworkInterface name.
	 * The returned NetConfig captured how the interface was configured; the returned
	 * list will have a NetConfig4 instance for IPv4 and an NetConfig6 instance for IPv6. 
	 * This should not be confused with the currently active NetInterfaceAddress associated 
	 * with the NetInterface.
	 *   
	 * @param interfaceName
	 * @return list of NetConfig for this interface.
	 */
	public List<NetConfig> getNetworkInterfaceConfigs(String interfaceName) throws KuraException;
	
	
	/**
	 * Updates the configuration of the specified EthernetInterface.
	 * 
	 * @param interfaceName - name of the Ethernet interface
	 * @param autoConnect - specifies the auto-connect value for the interface
	 * @param mtu - required MTU for the interface, -1 to keep the automatic default
	 * @param netConfig4 - the IPv4 configuration
	 * @param netConfig6 - the IPv6 configuration
	 * @throws KuraException
	 */
	public void updateEthernetInterfaceConfig(String interfaceName,
											  boolean autoConnect,
											  int mtu,
											  List<NetConfig> netConfigs) 
	    throws KuraException;

	
	/**
	 * Updates the configuration of the specified WifiInterface.
	 * 
	 * @param interfaceName - name of the wifi interface
	 * @param autoConnect - specifies the auto-connect value for the interface
	 * @param mtu - required MTU for the interface, -1 to keep the automatic default
	 * @param netConfig4 - the IPv4 configuration
	 * @param netConfig6 - the IPv6 configuration
	 * @throws KuraException
	 */
	public void updateWifiInterfaceConfig(String interfaceName,
										  boolean autoConnect,
										  WifiAccessPoint accessPoint,
										  List<NetConfig> netConfigs) 
	    throws KuraException;
	
	
	/**
	 * Updates the configuration of the specified ModemInterface.
	 * 
	 * @param interfaceName - name of the Modem interface
	 * @param serialNum - the modem's serial number
	 * @param modemId - user string to identify the modem
	 * @param pppNumber - ppp number to use for this interface
	 * @param autoConnect - specifies the auto-connect value for the interface
	 * @param mtu - required MTU for the interface, -1 to keep the automatic default
	 * @param netConfigs - list of NetConfigs for this interface
	 * @throws KuraException
	 */
	public void updateModemInterfaceConfig(String interfaceName,
											  String serialNum,
											  String modemId,
											  int pppNumber,
											  boolean autoConnect,
											  int mtu,
											  List<NetConfig> netConfigs) 
	    throws KuraException;
	
	
	/**
	 * Enables the specified interface.
	 * 
	 * @param interfaceName - name of the interface to be enabled.
	 */
	public void enableInterface(String interfaceName, boolean dhcp) throws KuraException; 	

	
	/**
	 * Disables the specified interface.
	 * 
	 * @param interfaceName - name of the interface to be disabled.
	 */
	public void disableInterface(String interfaceName) throws KuraException;
	
	/**
	 * Used to control DHCP clients on specified interfaces.
	 * 
	 * @param interfaceName		The interface of the DHCP server to modify the state
	 * @param enable			Whether to enable or disable the DHCP client
	 * @throws KuraException
	 */
	public void manageDhcpClient(String interfaceName, boolean enable) throws KuraException; 
	
	/**
	 * Used to control DHCP servers on specified interfaces.
	 * 
	 * @param interfaceName		The interface of the DHCP server to modify the state
	 * @param enable			Whether to enable or disable the DHCP server
	 * @throws KuraException
	 */
	public void manageDhcpServer(String interfaceName, boolean enable) throws KuraException;
	
	/**
	 * Releases current IP address and acquires a new lease for the provided interface.
	 * 
	 * @param interfaceName		The interface on which to renew the lease
	 * @throws KuraException
	 */
	public void renewDhcpLease(String interfaceName) throws KuraException;
	
	/**
	 * Gets the firewall configuration of the system as currently specified
	 * 
	 * @return					A list of NetConfigs representing the firewall configuration
	 * @throws KuraException
	 */
	public List<NetConfig> getFirewallConfiguration() throws KuraException;

	/**
	 * Sets the 'open port' portion of the firewall configuration
	 * 
	 * @param firewallConfiguration		A list of FirewallOpenPortConfigIP Objects representing the configuration to set
	 * @throws KuraException
	 */
	public void setFirewallOpenPortConfiguration(List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException;
	
	/**
	 * Sets the 'port forwarding' portion of the firewall configuration
	 * 
	 * @param firewallConfiguration		A list of FirewallPortForwardConfigIP Objects representing the configuration to set
	 * @throws KuraException
	 */
	public void setFirewallPortForwardingConfiguration(List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException;

	//public List<FirewallReverseNatConfig> getFirewallNatConfiguration(String sourceIface) throws KuraException;
	
	public void setFirewallNatConfiguration(List<FirewallNatConfig> natConfigs) throws KuraException;
	
	/**
	 * Updates the Firewall configuration based on current environmental conditions. This is
	 * used to update the firewall in events where NAT rules need to change based on a new WAN
	 * interface coming up or going down.  This ensures all downstream clients utilizing NAT 
	 * on the gateway can and will maintain active Internet connections through the gateway.
	 * 
	 * @param gatewayIface		The new gateway interface that is now active as the WAN interface
	 * @throws KuraException
	 */
	public void manageFirewall (String gatewayIface) throws KuraException;
	
	public Map<String, WifiHotspotInfo> getWifiHotspots(String ifaceName) throws KuraException;
	
	public boolean verifyWifiCredentials(String ifaceName, WifiConfig wifiConfig, int tout);
	
	public boolean rollbackDefaultConfiguration() throws KuraException;
}
