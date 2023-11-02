/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Sterwen-Technology
 ******************************************************************************/
package org.eclipse.kura.net;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.dhcp.DhcpLease;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiChannel;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiHotspotInfo;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service API for getting and setting network interface configurations.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetworkAdminService {

    /**
     * Returns a list of all of the configurations associated with all of the
     * interfaces on
     * the system.
     *
     * @return list of NetInterfaceConfigs on the system
     * @throws KuraException
     * 
     * @deprecated since 2.4. Use {@link getNetworkInterfaceConfigs(boolean
     *             recompute)} instead.
     */
    @Deprecated
    public List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getNetworkInterfaceConfigs()
            throws KuraException;

    /**
     * Returns the configuration information for the specified NetworkInterface
     * name.
     * The returned NetConfig captured how the interface was configured; the
     * returned
     * list will have a NetConfig4 instance for IPv4 and an NetConfig6 instance for
     * IPv6.
     * This should not be confused with the currently active NetInterfaceAddress
     * associated
     * with the NetInterface.
     *
     * @param interfaceName
     * @return list of NetConfig for this interface.
     * 
     * @deprecated since 2.4. Use {@link getNetworkInterfaceConfigs(tring
     *             interfaceName, boolean recompute)} instead.
     */
    @Deprecated
    public List<NetConfig> getNetworkInterfaceConfigs(String interfaceName) throws KuraException;

    /**
     * Updates the configuration of the specified EthernetInterface.
     *
     * @param interfaceName
     *                      - name of the Ethernet interface
     * @param autoConnect
     *                      - specifies the auto-connect value for the interface
     * @param mtu
     *                      - required MTU for the interface, -1 to keep the
     *                      automatic default
     * @throws KuraException
     * 
     * @deprecated Since 2.4. Use the {@link ConfigurationService} to update the
     *             configuration of an Ethernet interface.
     */
    @Deprecated
    public void updateEthernetInterfaceConfig(String interfaceName, boolean autoConnect, int mtu,
            List<NetConfig> netConfigs) throws KuraException;

    /**
     * Updates the configuration of the specified WifiInterface.
     *
     * @param interfaceName
     *                      - name of the wifi interface
     * @param autoConnect
     *                      - specifies the auto-connect value for the interface
     * @throws KuraException
     * 
     * @deprecated Since 2.4. Use the {@link ConfigurationService} to update the
     *             configuration of a Wifi interface.
     */
    @Deprecated
    public void updateWifiInterfaceConfig(String interfaceName, boolean autoConnect, WifiAccessPoint accessPoint,
            List<NetConfig> netConfigs) throws KuraException;

    /**
     * Updates the configuration of the specified ModemInterface.
     *
     * @param interfaceName
     *                      - name of the Modem interface
     * @param serialNum
     *                      - the modem's serial number
     * @param modemId
     *                      - user string to identify the modem
     * @param pppNumber
     *                      - ppp number to use for this interface
     * @param autoConnect
     *                      - specifies the auto-connect value for the interface
     * @param mtu
     *                      - required MTU for the interface, -1 to keep the
     *                      automatic default
     * @param netConfigs
     *                      - list of NetConfigs for this interface
     * @throws KuraException
     * 
     * @deprecated Since 2.4. Use the {@link ConfigurationService} to update the
     *             configuration of a Modem interface.
     */
    @Deprecated
    public void updateModemInterfaceConfig(String interfaceName, String serialNum, String modemId, int pppNumber,
            boolean autoConnect, int mtu, List<NetConfig> netConfigs) throws KuraException;

    /**
     * Enables the specified interface.
     *
     * @param interfaceName
     *                      - name of the interface to be enabled.
     */
    public void enableInterface(String interfaceName, boolean dhcp) throws KuraException;

    /**
     * Disables the specified interface.
     *
     * @param interfaceName
     *                      - name of the interface to be disabled.
     */
    public void disableInterface(String interfaceName) throws KuraException;

    /**
     * Used to control DHCP clients on specified interfaces.
     *
     * @param interfaceName
     *                      The interface of the DHCP server to modify the state
     * @param enable
     *                      Whether to enable or disable the DHCP client
     * @throws KuraException
     */
    public void manageDhcpClient(String interfaceName, boolean enable) throws KuraException;

    /**
     * Used to control DHCP servers on specified interfaces.
     *
     * @param interfaceName
     *                      The interface of the DHCP server to modify the state
     * @param enable
     *                      Whether to enable or disable the DHCP server
     * @throws KuraException
     */
    public void manageDhcpServer(String interfaceName, boolean enable) throws KuraException;

    /**
     * Releases current IP address and acquires a new lease for the provided
     * interface.
     *
     * @param interfaceName
     *                      The interface on which to renew the lease
     * @throws KuraException
     */
    public void renewDhcpLease(String interfaceName) throws KuraException;

    /**
     * Gets the firewall configuration of the system as currently specified
     *
     * @return A list of NetConfigs representing the firewall configuration
     * @throws KuraException
     */
    public List<NetConfig> getFirewallConfiguration() throws KuraException;

    /**
     * Sets the 'open port' portion of the firewall configuration
     *
     * @param firewallConfiguration
     *                              A list of FirewallOpenPortConfigIP Objects
     *                              representing the configuration to set
     * @throws KuraException
     * 
     * @deprecated Since 2.4
     */
    @Deprecated
    public void setFirewallOpenPortConfiguration(
            List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException;

    /**
     * Sets the 'port forwarding' portion of the firewall configuration
     *
     * @param firewallConfiguration
     *                              A list of FirewallPortForwardConfigIP Objects
     *                              representing the configuration to set
     * @throws KuraException
     * 
     * @deprecated Since 2.4
     */
    @Deprecated
    public void setFirewallPortForwardingConfiguration(
            List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException;

    /**
     * Sets the 'ip forwarding' portion of the firewall configuration
     *
     * @param natConfigs
     *                   A list of FirewallNatConfig Objects representing the
     *                   configuration to set
     * @throws KuraException
     * 
     * @deprecated Since 2.4
     */
    @Deprecated
    public void setFirewallNatConfiguration(List<FirewallNatConfig> natConfigs) throws KuraException;

    /**
     * Updates the Firewall configuration based on current environmental conditions.
     * This is
     * used to update the firewall in events where NAT rules need to change based on
     * a new WAN
     * interface coming up or going down. This ensures all downstream clients
     * utilizing NAT
     * on the gateway can and will maintain active Internet connections through the
     * gateway.
     *
     * @param gatewayIface
     *                     The new gateway interface that is now active as the WAN
     *                     interface
     * @throws KuraException
     */
    public void manageFirewall(String gatewayIface) throws KuraException;

    /**
     * Obtains information for WiFi hotspots in range.
     *
     * @param ifaceName
     *                  - name of WiFi interface
     * @return list of hotspot information.
     * @throws KuraException
     * @since 1.2
     */
    public List<WifiHotspotInfo> getWifiHotspotList(String ifaceName) throws KuraException;

    /**
     * Verifies WiFi credentials by trying to establish connection with access
     * point.
     *
     * @param ifaceName
     *                   - name of WiFi interface
     * @param wifiConfig
     *                   WiFi configuration
     * @param tout
     *                   - timeout (in seconds)
     * @return status - <i>true</i> if credentials are correct, <i>false</i>
     *         otherwise
     */
    public boolean verifyWifiCredentials(String ifaceName, WifiConfig wifiConfig, int tout);

    /**
     * Obtains information for WiFi Frequencies.
     *
     * @param ifaceName
     *                  - name of WiFi interface
     * @return list of channels and frequencies.
     * @throws KuraException
     * @since 2.2
     */
    public List<WifiChannel> getWifiFrequencies(String ifaceName) throws KuraException;

    /**
     * Obtains information for WiFi Country code.
     *
     * @return Name of the Country Code or 00 if unknown.
     * @throws KuraException
     * @since 2.2
     */
    public String getWifiCountryCode() throws KuraException;

    /**
     * Information on Dynamic Frequencies Selection
     * 
     * @param ifaceName
     *                  - name of WiFi interface
     * @return True if Dynamic Frequencies Selection is supported, false otherwise
     * @since 2.3
     */
    public boolean isWifiDFS(String ifaceName) throws KuraException;

    /**
     * Information on WiFi 802.11ac
     * 
     * @param ifaceName
     *                  - name of WiFi interface
     * @return True if WiFi 802.11ac is supported, false otherwise.
     * @since 2.3
     */
    public boolean isWifiIEEE80211AC(String ifaceName) throws KuraException;

    /**
     * Obtains the DHCP Lease values
     * 
     * @return list of ipAddresses, macAddresses, hostnames;
     * @throws KuraException
     * @since 2.3
     * @deprecated since 2.6. Use {@link getDhcpLeases(String ifaceName)} instead.
     */
    @Deprecated
    public List<DhcpLease> getDhcpLeases() throws KuraException;

    /**
     * Obtains the DHCP Lease values assigned by a DHCP server running on a given
     * network interface
     * 
     * @param ifaceName the name of the network interface
     * @return list of ipAddresses, macAddresses, hostnames;
     * @throws KuraException
     * @since 2.6
     */
    public List<DhcpLease> getDhcpLeases(String ifaceName) throws KuraException;

    /**
     * Returns a list of all of the configurations associated with all of the
     * interfaces on
     * the system and their current values.
     *
     * @param recompute:
     *                   if true the configuration and current values are
     *                   recomputed. Otherwise, a cached value is returned
     * @return list of NetInterfaceConfigs on the system
     * @throws KuraException
     * 
     * @since 2.4
     */
    public List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getNetworkInterfaceConfigs(
            boolean recompute) throws KuraException;

    /**
     * Returns the configuration information for the specified NetworkInterface
     * name.
     * The returned NetConfig captured how the interface was configured; the
     * returned
     * list will have a NetConfig4 instance for IPv4 and an NetConfig6 instance for
     * IPv6.
     * This should not be confused with the currently active NetInterfaceAddress
     * associated
     * with the NetInterface.
     *
     * @param interfaceName:
     *                       the name of the network interface
     * @param recompute:
     *                       if true the configuration are recomputed. Otherwise, a
     *                       cached value is returned
     * @return list of NetConfig for this interface.
     * 
     * @since 2.4
     */
    public List<NetConfig> getNetworkInterfaceConfigs(String interfaceName, boolean recompute) throws KuraException;
}
