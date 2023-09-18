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
 *  Red Hat Inc
 *  Areti
 *******************************************************************************/
package org.eclipse.kura.core.net;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.core.net.vlan.VlanInterfaceConfigImpl;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.usb.UsbDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfiguration {

    private static final String WIFI_CHANNELS_KEY = ".channel";

    private static final String WIFI_IGNORE_SSID_KEY = ".ignoreSSID";

    private static final String WIFI_PING_ACCESS_POINT_KEY = ".pingAccessPoint";

    private static final String WIFI_RADIO_MODE_KEY = ".radioMode";

    private static final String WIFI_GROUP_CIPHERS_KEY = ".groupCiphers";

    private static final String WIFI_PAIRWISE_CIPHERS_KEY = ".pairwiseCiphers";

    private static final String WIFI_MODE_KEY = ".mode";

    private static final String DRIVER_KEY = ".driver";

    private static final String WIFI_SSID_KEY = ".ssid";

    private static final Logger logger = LoggerFactory.getLogger(NetworkConfiguration.class);

    private static final String ADDRESS = " :: Address: ";
    private static final String BGSCAN = ".bgscan";
    private static final String WIFI_PASSPHRASE_KEY = ".passphrase";
    private static final String SECURITY_TYPE = ".securityType";
    private static final String NET_INTERFACE = "net.interface.";
    private static final String NET_INTERFACES = "net.interfaces";
    private static final Pattern COMMA = Pattern.compile(",");

    private final Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs;
    private Map<String, Object> properties;
    private boolean recomputeProperties;
    private List<String> modifiedInterfaceNames;

    public NetworkConfiguration() {
        logger.debug("Created empty NetworkConfiguration");
        this.netInterfaceConfigs = new HashMap<>();
        this.recomputeProperties = true;
    }

    /**
     * Constructor for create a completely new NetComponentConfiguration based on a
     * set of properties
     *
     * @param properties
     *                   The properties that represent the new configuration
     * @throws UnknownHostException
     *                              If some hostnames can not be resolved
     * @throws KuraException
     *                              It there is an internal error
     */
    public NetworkConfiguration(Map<String, Object> properties) throws UnknownHostException, KuraException {
        logger.debug("Creating NetworkConfiguration from properties");
        this.netInterfaceConfigs = new HashMap<>();
        String[] availableInterfaces = null;

        try {
            availableInterfaces = (String[]) properties.get(NET_INTERFACES);
        } catch (ClassCastException e) {
            // this means this configuration came from GWT - so convert the comma separated
            // list
            String interfaces = (String) properties.get(NET_INTERFACES);
            StringTokenizer st = new StringTokenizer(interfaces, ",");

            List<String> interfacesArray = new ArrayList<>();
            while (st.hasMoreTokens()) {
                interfacesArray.add(st.nextToken());
            }
            availableInterfaces = interfacesArray.toArray(new String[interfacesArray.size()]);
        }

        if (availableInterfaces != null) {
            logger.debug("There are {} interfaces to add to the new configuration", availableInterfaces.length);
            for (String currentNetInterface : availableInterfaces) {
                StringBuilder keyBuffer = new StringBuilder();
                keyBuffer.append(NET_INTERFACE).append(currentNetInterface).append(".type");
                NetInterfaceType type = NetInterfaceType.UNKNOWN;
                if (properties.get(keyBuffer.toString()) != null) {
                    type = NetInterfaceType.valueOf((String) properties.get(keyBuffer.toString()));
                }
                logger.trace("Adding interface: {} of type {}", currentNetInterface, type);
                addInterfaceConfiguration(currentNetInterface, type, properties);
            }
        }

        this.modifiedInterfaceNames = new ArrayList<>();
        String modifiedInterfaces = (String) properties.get("modified.interface.names");
        if (modifiedInterfaces != null) {
            COMMA.splitAsStream(modifiedInterfaces).filter(s -> !s.trim().isEmpty())
                    .forEach(this.modifiedInterfaceNames::add);
        }

        this.recomputeProperties = true;
    }

    public void setModifiedInterfaceNames(List<String> modifiedInterfaceNames) {
        if (modifiedInterfaceNames != null && !modifiedInterfaceNames.isEmpty()) {
            this.modifiedInterfaceNames = modifiedInterfaceNames;
            this.recomputeProperties = true;
        }
    }

    public List<String> getModifiedInterfaceNames() {
        return this.modifiedInterfaceNames;
    }

    public void accept(NetworkConfigurationVisitor visitor) throws KuraException {
        visitor.visit(this);
    }

    public void addNetInterfaceConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {
        this.netInterfaceConfigs.put(netInterfaceConfig.getName(), netInterfaceConfig);
        this.recomputeProperties = true;
    }

    public void addNetConfig(String interfaceName, NetInterfaceType netInterfaceType, NetConfig netConfig)
            throws KuraException {
        NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = this.netInterfaceConfigs
                .get(interfaceName);

        if (netInterfaceConfig == null) {
            switch (netInterfaceType) {
                case LOOPBACK:
                    netInterfaceConfig = new LoopbackInterfaceConfigImpl(interfaceName);
                    break;
                case ETHERNET:
                    netInterfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
                    break;
                case WIFI:
                    netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
                    break;
                case MODEM:
                    netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
                    break;
                case VLAN:
                    netInterfaceConfig = new VlanInterfaceConfigImpl(interfaceName);
                    break;
                default:
                    throw new KuraException(KuraErrorCode.INVALID_PARAMETER);
            }
        }

        List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                .getNetInterfaceAddresses();

        logger.trace("Adding a netConfig: {}", netConfig);
        for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
            List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
            netConfigs.add(netConfig);
        }

        this.recomputeProperties = true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        Iterator<String> it = this.netInterfaceConfigs.keySet().iterator();
        while (it.hasNext()) {
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = this.netInterfaceConfigs
                    .get(it.next());

            sb.append("\nname: " + netInterfaceConfig.getName());
            sb.append(" :: Loopback? " + netInterfaceConfig.isLoopback());
            sb.append(" :: Point to Point? " + netInterfaceConfig.isPointToPoint());
            sb.append(" :: Up? " + netInterfaceConfig.isUp());
            sb.append(" :: Virtual? " + netInterfaceConfig.isVirtual());
            sb.append(" :: Driver: " + netInterfaceConfig.getDriver());
            sb.append(" :: Driver Version: " + netInterfaceConfig.getDriverVersion());
            sb.append(" :: Firmware Version: " + netInterfaceConfig.getFirmwareVersion());
            sb.append(" :: MTU: " + netInterfaceConfig.getMTU());
            if (netInterfaceConfig.getHardwareAddress() != null) {
                sb.append(" :: Hardware Address: " + NetworkUtil.macToString(netInterfaceConfig.getHardwareAddress()));
            }
            sb.append(" :: State: " + netInterfaceConfig.getState());
            sb.append(" :: Type: " + netInterfaceConfig.getType());
            sb.append(" :: Usb Device: " + netInterfaceConfig.getUsbDevice());

            appendAddresses(sb, netInterfaceConfig);

            List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                    .getNetInterfaceAddresses();

            if (netInterfaceAddressConfigs != null) {
                netInterfaceAddressConfigs.forEach(netInterfaceAddressConfig -> {
                    List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
                    if (netConfigs != null) {
                        netConfigs.forEach(netConfig -> appendNetworkConfig(sb, netConfig));
                    }
                });
            }
        }

        return sb.toString();
    }

    protected void appendNetworkConfig(StringBuilder sb, NetConfig netConfig) {
        if (netConfig instanceof NetConfigIP4) {
            appendNetConfigIP4(sb, netConfig);
        } else if (netConfig instanceof NetConfigIP6) {
            appendNetConfigIP6(sb, netConfig);
        } else if (netConfig instanceof WifiConfig) {
            appendWifiConfig(sb, netConfig);
        } else if (netConfig instanceof ModemConfig) {
            appendModemConfig(sb, netConfig);
        } else if (netConfig instanceof DhcpServerConfig) {
            appendDhcpServerConfig(sb, netConfig);
        } else if (netConfig instanceof FirewallAutoNatConfig) {
            sb.append("\n\tFirewallAutoNatConfig ");
        } else {
            if (netConfig != null && netConfig.getClass() != null) {
                sb.append("\n\tUNKNOWN CONFIG TYPE???: " + netConfig.getClass().getName());
            } else {
                sb.append("\n\tNULL NETCONFIG PRESENT?!?");
            }
        }
    }

    protected void appendAddresses(StringBuilder sb,
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {
        List<? extends NetInterfaceAddress> netInterfaceAddresses = netInterfaceConfig.getNetInterfaceAddresses();
        for (NetInterfaceAddress netInterfaceAddress : netInterfaceAddresses) {
            if (netInterfaceAddress.getAddress() != null) {
                sb.append(ADDRESS + netInterfaceAddress.getAddress().getHostAddress());
            }
            sb.append(" :: Prefix: " + netInterfaceAddress.getNetworkPrefixLength());
            if (netInterfaceAddress.getNetmask() != null) {
                sb.append(" :: Netmask: " + netInterfaceAddress.getNetmask().getHostAddress());
            }
            if (netInterfaceAddress.getBroadcast() != null) {
                sb.append(" :: Broadcast: " + netInterfaceAddress.getBroadcast().getHostAddress());
            }
        }
    }

    protected void appendModemConfig(StringBuilder sb, NetConfig netConfig) {
        sb.append("\n\tModemConfig ");

        sb.append(" :: APN: " + ((ModemConfig) netConfig).getApn());
        sb.append(" :: Data Compression: " + ((ModemConfig) netConfig).getDataCompression());
        sb.append(" :: Dial String: " + ((ModemConfig) netConfig).getDialString());
        sb.append(" :: Header Compression: " + ((ModemConfig) netConfig).getHeaderCompression());
        sb.append(" :: PPP number: " + ((ModemConfig) netConfig).getPppNumber());
        sb.append(" :: Profile ID: " + ((ModemConfig) netConfig).getProfileID());
        sb.append(" :: Username: " + ((ModemConfig) netConfig).getUsername());
        sb.append(" :: Auth Type: " + ((ModemConfig) netConfig).getAuthType());
        sb.append(" :: IP Address: " + ((ModemConfig) netConfig).getIpAddress());
        sb.append(" :: PDP Type: " + ((ModemConfig) netConfig).getPdpType());
        sb.append(" :: Gps enabled: " + ((ModemConfig) netConfig).isGpsEnabled());
        sb.append(" :: Antenna diversity enabled: " + ((ModemConfig) netConfig).isDiversityEnabled());
    }

    protected void appendWifiConfig(StringBuilder sb, NetConfig netConfig) {
        sb.append("\n\tWifiConfig ");

        sb.append(" :: SSID: " + ((WifiConfig) netConfig).getSSID());
        sb.append(" :: BgScan: " + ((WifiConfig) netConfig).getBgscan());
        int[] channels = ((WifiConfig) netConfig).getChannels();
        if (channels != null && channels.length > 0) {
            sb.append(" :: Channels: ");
            for (int i = 0; i < channels.length; i++) {
                sb.append(channels[i]);
                if (i + 1 < channels.length) {
                    sb.append(",");
                }
            }
        }
        sb.append(" :: Group Ciphers: " + ((WifiConfig) netConfig).getGroupCiphers());
        sb.append(" :: Hardware Mode: " + ((WifiConfig) netConfig).getHardwareMode());
        sb.append(" :: Mode: " + ((WifiConfig) netConfig).getMode());
        sb.append(" :: Pairwise Ciphers: " + ((WifiConfig) netConfig).getPairwiseCiphers());
        sb.append(" :: Security: " + ((WifiConfig) netConfig).getSecurity());
    }

    protected void appendNetConfigIP6(StringBuilder sb, NetConfig netConfig) {
        sb.append("\n\tIPv6 ");
        if (((NetConfigIP6) netConfig).isDhcp()) {
            sb.append(" :: is DHCP client");
            Map<String, Object> dhcp6Map = ((NetConfigIP6) netConfig).getProperties();
            Iterator<String> it2 = dhcp6Map.keySet().iterator();
            while (it2.hasNext()) {
                String dhcpKey = it2.next();
                sb.append(" :: " + dhcpKey + ": " + dhcp6Map.get(dhcpKey));
            }
        } else {
            sb.append(" :: is STATIC client");
            if (((NetConfigIP6) netConfig).getAddress() != null) {
                sb.append(ADDRESS + ((NetConfigIP6) netConfig).getAddress().getHostAddress());
            }

            List<IP6Address> dnsServers = ((NetConfigIP6) netConfig).getDnsServers();
            List<String> domains = ((NetConfigIP6) netConfig).getDomains();
            for (IP6Address dnsServer : dnsServers) {
                sb.append(" :: DNS : " + dnsServer.getHostAddress());
            }
            for (String domain : domains) {
                sb.append(" :: Domains : " + domain);
            }
        }
    }

    protected void appendNetConfigIP4(StringBuilder sb, NetConfig netConfig) {
        sb.append("\n\tIPv4 ");
        if (((NetConfigIP4) netConfig).isDhcp()) {
            appendDhcpConfig(sb, netConfig);
        } else if (((NetConfigIP4) netConfig).getAddress() == null) {
            sb.append(" :: is not configured for STATIC or DHCP");
        } else {
            appendStaticConfig(sb, netConfig);
        }
    }

    protected void appendStaticConfig(StringBuilder sb, NetConfig netConfig) {
        sb.append(" :: is STATIC client");
        if (((NetConfigIP4) netConfig).getAddress() != null) {
            sb.append(ADDRESS + ((NetConfigIP4) netConfig).getAddress().getHostAddress());
        }
        sb.append(" :: Prefix: " + ((NetConfigIP4) netConfig).getNetworkPrefixLength());
        if (((NetConfigIP4) netConfig).getGateway() != null) {
            sb.append(" :: Gateway: " + ((NetConfigIP4) netConfig).getGateway().getHostAddress());
        }

        List<IP4Address> dnsServers = ((NetConfigIP4) netConfig).getDnsServers();
        List<IP4Address> winsServers = ((NetConfigIP4) netConfig).getWinsServers();
        List<String> domains = ((NetConfigIP4) netConfig).getDomains();
        if (dnsServers != null) {
            for (IP4Address dnsServer : dnsServers) {
                sb.append(" :: DNS : " + dnsServer.getHostAddress());
            }
        }
        if (winsServers != null) {
            for (IP4Address winsServer : winsServers) {
                sb.append(" :: WINS Server : " + winsServer.getHostAddress());
            }
        }
        if (domains != null) {
            for (String domain : domains) {
                sb.append(" :: Domains : " + domain);
            }
        }
    }

    protected void appendDhcpConfig(StringBuilder sb, NetConfig netConfig) {
        sb.append(" :: is DHCP client");
        Map<String, Object> dhcp4Map = ((NetConfigIP4) netConfig).getProperties();
        for (Map.Entry<String, Object> entry : dhcp4Map.entrySet()) {
            String dhcpKey = entry.getKey();
            sb.append(" :: " + dhcpKey + ": " + entry.getValue());
        }
    }

    protected void appendDhcpServerConfig(StringBuilder sb, NetConfig netConfig) {
        sb.append("\n\tDhcpServerConfig :: \n");
        sb.append(((DhcpServerConfig) netConfig).toString());
    }

    // Returns a List of all modified NetInterfaceConfigs, or if none are specified,
    // all NetInterfaceConfigs
    public List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getModifiedNetInterfaceConfigs() {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> newNetInterfaceConfigs = null;
        if (this.modifiedInterfaceNames != null && !this.modifiedInterfaceNames.isEmpty()) {
            newNetInterfaceConfigs = new ArrayList<>();
            for (String interfaceName : this.modifiedInterfaceNames) {
                NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = this.netInterfaceConfigs
                        .get(interfaceName);
                if (netInterfaceConfig != null) {
                    newNetInterfaceConfigs.add(this.netInterfaceConfigs.get(interfaceName));
                }
            }
        } else {
            newNetInterfaceConfigs = getNetInterfaceConfigs();
        }

        return newNetInterfaceConfigs;
    }

    public List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getNetInterfaceConfigs() {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> newNetInterfaceConfigs = new ArrayList<>();
        Iterator<String> it = this.netInterfaceConfigs.keySet().iterator();
        while (it.hasNext()) {
            newNetInterfaceConfigs.add(this.netInterfaceConfigs.get(it.next()));
        }
        return newNetInterfaceConfigs;
    }

    public NetInterfaceConfig<? extends NetInterfaceAddressConfig> getNetInterfaceConfig(String interfaceName) {
        return this.netInterfaceConfigs.get(interfaceName);
    }

    public Map<String, Object> getConfigurationProperties() {
        if (this.recomputeProperties) {
            recomputeNetworkProperties();
            this.recomputeProperties = false;
        }

        return this.properties;
    }

    public boolean isValid() {
        Iterator<String> it = this.netInterfaceConfigs.keySet().iterator();
        while (it.hasNext()) {
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = this.netInterfaceConfigs
                    .get(it.next());

            if (netInterfaceConfig.getMTU() < 0) {
                logger.error("MTU must be greater than 0");
                return false;
            }

            NetInterfaceType type = netInterfaceConfig.getType();
            if (type != NetInterfaceType.ETHERNET && type != NetInterfaceType.WIFI && type != NetInterfaceType.MODEM
                    && type != NetInterfaceType.LOOPBACK) {
                logger.error("Type must be ETHERNET, WIFI, MODEM, or LOOPBACK - type is {}", type);
                return false;
            }

            List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                    .getNetInterfaceAddresses();
            return isNetInterfaceAddressConfigValid(netInterfaceAddressConfigs);
        }

        return true;
    }

    // ---------------------------------------------------------------
    //
    // Private Methods
    //
    // ---------------------------------------------------------------

    private boolean isNetInterfaceAddressConfigValid(
            List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs) {
        boolean isValid = true;
        for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
            List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();

            if (netConfigs != null) {
                for (NetConfig netConfig : netConfigs) {
                    if (!netConfig.isValid()) {
                        logger.error("Invalid config {}", netConfig);
                        isValid = false;
                        break;
                    }
                }
            }
        }
        return isValid;
    }

    private void recomputeNetworkProperties() {
        Map<String, Object> newNetworkProperties = new HashMap<>();
        StringBuilder sbInterfaces = new StringBuilder();
        addModifiedInterfaceNames(newNetworkProperties);

        Iterator<String> it = this.netInterfaceConfigs.keySet().iterator();
        while (it.hasNext()) {
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = this.netInterfaceConfigs
                    .get(it.next());

            // add the interface to the list of interface found in the platform
            if (sbInterfaces.length() != 0) {
                sbInterfaces.append(",");
            }
            sbInterfaces.append(netInterfaceConfig.getName());

            // build the prefixes for all the properties associated with this interface
            final StringBuilder sbPrefix = new StringBuilder(NET_INTERFACE).append(netInterfaceConfig.getName())
                    .append(".");
            final String netIfReadOnlyPrefix = sbPrefix.toString();
            final String netIfPrefix = sbPrefix.append("config.").toString();
            final String netIfConfigPrefix = sbPrefix.toString();

            // add the properties of the interface
            newNetworkProperties.put(netIfReadOnlyPrefix + "type", netInterfaceConfig.getType().toString());

            netInterfaceConfig.getNetInterfaceAddresses().forEach(nia -> {
                if (nia != null) {
                    addWifiModeProperty(newNetworkProperties, netIfPrefix, nia);
                    addModemConnectionProperty(newNetworkProperties, netIfConfigPrefix, nia);
                }
            });

            addNetConfigProperties(newNetworkProperties, netInterfaceConfig, netIfConfigPrefix);
        }
        newNetworkProperties.put(NET_INTERFACES, sbInterfaces.toString());

        this.properties = newNetworkProperties;
    }

    private void addNetConfigProperties(Map<String, Object> newNetworkProperties,
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig,
            final String netIfConfigPrefix) {
        // add the properties of the network configurations associated to the interface
        List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                .getNetInterfaceAddresses();
        logger.trace("netInterfaceAddressConfigs.size() for {}: {}", netInterfaceConfig.getName(),
                netInterfaceAddressConfigs.size());

        for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
            List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();

            if (netConfigs != null) {
                logger.trace("netConfigs.size(): {}", netConfigs.size());

                for (NetConfig netConfig : netConfigs) {
                    if (netConfig instanceof WifiConfig) {
                        logger.trace("adding netconfig WifiConfigIP4 for {}", netInterfaceConfig.getName());
                        addWifiConfigIP4Properties((WifiConfig) netConfig, netIfConfigPrefix, newNetworkProperties);
                    } else if (netConfig instanceof ModemConfig) {
                        logger.trace("adding netconfig ModemConfig for {}", netInterfaceConfig.getName());
                        addModemConfigProperties((ModemConfig) netConfig, netIfConfigPrefix, newNetworkProperties);
                    } else if (netConfig instanceof NetConfigIP4) {
                        logger.trace("adding netconfig NetConfigIP4 for {}", netInterfaceConfig.getName());
                        addNetConfigIP4Properties((NetConfigIP4) netConfig, netIfConfigPrefix, newNetworkProperties);
                    } else if (netConfig instanceof NetConfigIP6) {
                        logger.trace("adding netconfig NetConfigIP6 for {}", netInterfaceConfig.getName());
                        addNetConfigIP6Properties((NetConfigIP6) netConfig, netIfConfigPrefix, newNetworkProperties);
                    } else if (netConfig instanceof DhcpServerConfig4) {
                        logger.trace("adding netconfig DhcpServerConfig4 for {}", netInterfaceConfig.getName());
                        addDhcpServerConfig4((DhcpServerConfig4) netConfig, netIfConfigPrefix, newNetworkProperties);
                    } else if (netConfig instanceof FirewallAutoNatConfig) {
                        logger.trace("adding netconfig FirewallNatConfig for {}", netInterfaceConfig.getName());
                        addFirewallNatConfig(netIfConfigPrefix, newNetworkProperties);
                    }
                }
            }
        }
    }

    private void addModemConnectionProperty(Map<String, Object> newNetworkProperties, String netIfConfigPrefix,
            NetInterfaceAddressConfig nia) {
        // Modem interface address
        if (nia instanceof ModemInterfaceAddress && ((ModemInterfaceAddress) nia).getConnectionStatus() != null) {
            newNetworkProperties.put(netIfConfigPrefix + "connection.status",
                    ((ModemInterfaceAddress) nia).getConnectionStatus().toString());
        }
    }

    private void addWifiModeProperty(Map<String, Object> newNetworkProperties, String netIfPrefix,
            NetInterfaceAddressConfig nia) {
        // Wifi interface address
        if (nia instanceof WifiInterfaceAddress) {
            WifiMode wifiMode;
            if (((WifiInterfaceAddress) nia).getMode() != null) {
                wifiMode = ((WifiInterfaceAddress) nia).getMode();
            } else {
                wifiMode = WifiMode.UNKNOWN;
            }
            newNetworkProperties.put(netIfPrefix + "wifi.mode", wifiMode.toString());
        }
    }

    private void addModifiedInterfaceNames(Map<String, Object> newNetworkProperties) {
        if (this.modifiedInterfaceNames != null && !this.modifiedInterfaceNames.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            String prefix = "";
            for (String interfaceName : this.modifiedInterfaceNames) {
                sb.append(prefix);
                prefix = ",";
                sb.append(interfaceName);
            }
            String result = sb.toString();
            logger.debug("Set modified interface names: {}", result);
            newNetworkProperties.put("modified.interface.names", result);
        }
    }

    private static void addWifiConfigIP4Properties(WifiConfig wifiConfig, String netIfConfigPrefix,
            Map<String, Object> properties) {

        WifiMode mode = wifiConfig.getMode();
        if (mode == null) {
            logger.trace("WifiMode is null - could not add wifiConfig: {}", wifiConfig);
            return;
        }

        String prefix = new StringBuilder(netIfConfigPrefix).append("wifi.").append(mode.toString().toLowerCase())
                .toString();

        properties.put(prefix + WIFI_SSID_KEY, wifiConfig.getSSID());
        properties.put(prefix + DRIVER_KEY, wifiConfig.getDriver());
        properties.put(prefix + WIFI_MODE_KEY, wifiConfig.getMode().toString());
        if (wifiConfig.getSecurity() != null) {
            properties.put(prefix + SECURITY_TYPE, wifiConfig.getSecurity().toString());
        } else {
            properties.put(prefix + SECURITY_TYPE, WifiSecurity.NONE.toString());
        }
        properties.put(prefix + WIFI_CHANNELS_KEY, serializeWifiChannels(wifiConfig));
        Password psswd = wifiConfig.getPasskey();
        if (psswd != null) {
            properties.put(prefix + WIFI_PASSPHRASE_KEY, psswd);
        } else {
            properties.put(prefix + WIFI_PASSPHRASE_KEY, new Password(""));
        }
        if (wifiConfig.getRadioMode() != null) {
            properties.put(prefix + WIFI_RADIO_MODE_KEY, wifiConfig.getRadioMode().toString());
        }

        if (wifiConfig.getBgscan() != null) {
            properties.put(prefix + BGSCAN, wifiConfig.getBgscan().toString());
        } else {
            properties.put(prefix + BGSCAN, "");
        }

        if (wifiConfig.getPairwiseCiphers() != null) {
            properties.put(prefix + WIFI_PAIRWISE_CIPHERS_KEY, wifiConfig.getPairwiseCiphers().name());
        }

        if (wifiConfig.getGroupCiphers() != null) {
            properties.put(prefix + WIFI_GROUP_CIPHERS_KEY, wifiConfig.getGroupCiphers().name());
        }

        properties.put(prefix + WIFI_PING_ACCESS_POINT_KEY, wifiConfig.pingAccessPoint());

        properties.put(prefix + WIFI_IGNORE_SSID_KEY, wifiConfig.ignoreSSID());
    }

    private static String serializeWifiChannels(WifiConfig wifiConfig) {
        int[] channels = wifiConfig.getChannels();
        StringBuilder sbChannel = new StringBuilder();
        if (channels != null) {
            for (int i = 0; i < channels.length; i++) {
                sbChannel.append(channels[i]);
                if (i < channels.length - 1) {
                    sbChannel.append(' ');
                }
            }
        }
        return sbChannel.toString();
    }

    private void addModemConfigProperties(ModemConfig modemConfig, String prefix, Map<String, Object> properties) {

        properties.put(prefix + "apn", modemConfig.getApn());
        properties.put(prefix + "authType",
                modemConfig.getAuthType() != null ? modemConfig.getAuthType().toString() : "");
        properties.put(prefix + "dialString", modemConfig.getDialString());
        properties.put(prefix + "ipAddress",
                modemConfig.getIpAddress() != null ? modemConfig.getIpAddress().toString() : "");
        properties.put(prefix + "password", modemConfig.getPasswordAsPassword());
        properties.put(prefix + "pdpType", modemConfig.getPdpType() != null ? modemConfig.getPdpType().toString() : "");
        properties.put(prefix + "persist", modemConfig.isPersist());
        properties.put(prefix + "maxFail", modemConfig.getMaxFail());
        properties.put(prefix + "idle", modemConfig.getIdle());
        properties.put(prefix + "activeFilter", modemConfig.getActiveFilter());
        properties.put(prefix + "resetTimeout", modemConfig.getResetTimeout());
        properties.put(prefix + "lcpEchoInterval", modemConfig.getLcpEchoInterval());
        properties.put(prefix + "lcpEchoFailure", modemConfig.getLcpEchoFailure());
        properties.put(prefix + "username", modemConfig.getUsername());
        properties.put(prefix + "enabled", modemConfig.isEnabled());
        properties.put(prefix + "gpsEnabled", modemConfig.isGpsEnabled());
        properties.put(prefix + "diversityEnabled", modemConfig.isDiversityEnabled());
    }

    private static void addNetConfigIP4Properties(NetConfigIP4 nc, String netIfConfigPrefix,
            Map<String, Object> properties) {

        properties.put(netIfConfigPrefix + "ip4.status", nc.getStatus().toString());

        StringBuilder sbDnsAddresses = new StringBuilder();
        if (nc.getDnsServers() != null) {
            for (IP4Address ip : nc.getDnsServers()) {
                if (sbDnsAddresses.length() != 0) {
                    sbDnsAddresses.append(",");
                }
                sbDnsAddresses.append(ip.getHostAddress());
            }
        }
        properties.put(netIfConfigPrefix + "ip4.dnsServers", sbDnsAddresses.toString());

        if (nc.isDhcp()) {
            properties.put(netIfConfigPrefix + "dhcpClient4.enabled", true);
        } else {
            properties.put(netIfConfigPrefix + "dhcpClient4.enabled", false);

            if (nc.getAddress() != null) {
                properties.put(netIfConfigPrefix + "ip4.address", nc.getAddress().getHostAddress());
            } else {
                properties.put(netIfConfigPrefix + "ip4.address", "");
            }

            properties.put(netIfConfigPrefix + "ip4.prefix", nc.getNetworkPrefixLength());

            if (nc.getGateway() != null) {
                properties.put(netIfConfigPrefix + "ip4.gateway", nc.getGateway().getHostAddress());
            } else {
                properties.put(netIfConfigPrefix + "ip4.gateway", "");
            }
        }
    }

    private static void addNetConfigIP6Properties(NetConfigIP6 nc, String netIfConfigPrefix,
            Map<String, Object> properties) {

        properties.put(netIfConfigPrefix + "ip6.status", nc.getStatus().toString());

        if (nc.isDhcp()) {
            properties.put(netIfConfigPrefix + "dhcpClient6.enabled", true);
        } else {
            properties.put(netIfConfigPrefix + "dhcpClient6.enabled", false);
            if (nc.getAddress() != null) {
                properties.put(netIfConfigPrefix + "address", nc.getAddress().getHostAddress());
            }

            StringBuilder sbDnsAddresses = new StringBuilder();
            for (IP6Address ip : nc.getDnsServers()) {
                if (sbDnsAddresses.length() != 0) {
                    sbDnsAddresses.append(",");
                }
                sbDnsAddresses.append(ip.getHostAddress());
            }
            properties.put(netIfConfigPrefix + "ip6.dnsServers", sbDnsAddresses.toString());
        }
    }

    private void addDhcpServerConfig4(DhcpServerConfig4 nc, String netIfConfigPrefix, Map<String, Object> properties) {
        properties.put(netIfConfigPrefix + "dhcpServer4.enabled", nc.isEnabled());
        properties.put(netIfConfigPrefix + "dhcpServer4.defaultLeaseTime", nc.getDefaultLeaseTime());
        properties.put(netIfConfigPrefix + "dhcpServer4.maxLeaseTime", nc.getMaximumLeaseTime());
        properties.put(netIfConfigPrefix + "dhcpServer4.prefix", nc.getPrefix());
        properties.put(netIfConfigPrefix + "dhcpServer4.rangeStart", nc.getRangeStart().toString());
        properties.put(netIfConfigPrefix + "dhcpServer4.rangeEnd", nc.getRangeEnd().toString());
        properties.put(netIfConfigPrefix + "dhcpServer4.passDns", nc.isPassDns());
    }

    private static void addFirewallNatConfig(String netIfConfigPrefix, Map<String, Object> properties) {

        properties.put(netIfConfigPrefix + "nat.enabled", true);
    }

    private void addInterfaceConfiguration(String interfaceName, NetInterfaceType type, Map<String, Object> props)
            throws UnknownHostException, KuraException {
        if (type == null) {
            logger.error("Null type for {}", interfaceName);
            return;
        }

        UsbDevice usbDevice = IpConfigurationInterpreter.getUsbDeviceInfo(props, interfaceName);
        NetInterfaceConfig<?> interfaceConfig = null;

        switch (type) {
            case LOOPBACK:
                interfaceConfig = new LoopbackInterfaceConfigImpl(interfaceName);
                List<NetInterfaceAddressConfig> loopbackInterfaceAddressConfigs = new ArrayList<>();
                NetInterfaceAddressConfigImpl netInterfaceAddressConfigImpl = new NetInterfaceAddressConfigImpl();
                netInterfaceAddressConfigImpl.setNetConfigs(IpConfigurationInterpreter.populateConfiguration(props,
                        interfaceName, netInterfaceAddressConfigImpl.getAddress(), interfaceConfig.isVirtual()));
                loopbackInterfaceAddressConfigs.add(netInterfaceAddressConfigImpl);
                ((LoopbackInterfaceConfigImpl) interfaceConfig)
                        .setNetInterfaceAddresses(loopbackInterfaceAddressConfigs);

                ((LoopbackInterfaceConfigImpl) interfaceConfig).setUsbDevice(usbDevice);

                break;
            case ETHERNET:
                interfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
                List<NetInterfaceAddressConfig> ethernetInterfaceAddressConfigs = new ArrayList<>();
                netInterfaceAddressConfigImpl = new NetInterfaceAddressConfigImpl();
                netInterfaceAddressConfigImpl.setNetConfigs(IpConfigurationInterpreter.populateConfiguration(props,
                        interfaceName, netInterfaceAddressConfigImpl.getAddress(), interfaceConfig.isVirtual()));
                ethernetInterfaceAddressConfigs.add(netInterfaceAddressConfigImpl);
                ((EthernetInterfaceConfigImpl) interfaceConfig)
                        .setNetInterfaceAddresses(ethernetInterfaceAddressConfigs);

                ((EthernetInterfaceConfigImpl) interfaceConfig).setUsbDevice(usbDevice);

                break;
            case WIFI:
                interfaceConfig = new WifiInterfaceConfigImpl(interfaceName);

                List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = new ArrayList<>();

                WifiInterfaceAddressConfig wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
                List<NetConfig> wifiNetConfigs = IpConfigurationInterpreter.populateConfiguration(props, interfaceName,
                        wifiInterfaceAddressConfig.getAddress(), interfaceConfig.isVirtual());
                wifiNetConfigs.addAll(WifiConfigurationInterpreter.populateConfiguration(props, interfaceName));
                ((WifiInterfaceAddressConfigImpl) wifiInterfaceAddressConfig).setNetConfigs(wifiNetConfigs);
                ((WifiInterfaceAddressConfigImpl) wifiInterfaceAddressConfig)
                        .setMode(WifiConfigurationInterpreter.getWifiMode(props, interfaceName));
                wifiInterfaceAddressConfigs.add(wifiInterfaceAddressConfig);

                ((WifiInterfaceConfigImpl) interfaceConfig).setNetInterfaceAddresses(wifiInterfaceAddressConfigs);

                ((WifiInterfaceConfigImpl) interfaceConfig).setUsbDevice(usbDevice);

                break;
            case MODEM:
                interfaceConfig = new ModemInterfaceConfigImpl(interfaceName);

                ModemInterfaceAddressConfig modemInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
                List<NetConfig> modemNetConfigs = IpConfigurationInterpreter.populateConfiguration(props, interfaceName,
                        modemInterfaceAddressConfig.getAddress(), interfaceConfig.isVirtual());
                modemNetConfigs.addAll(ModemConfigurationInterpreter.populateConfiguration(
                        modemInterfaceAddressConfig, props, interfaceName));
                ((ModemInterfaceAddressConfigImpl) modemInterfaceAddressConfig).setNetConfigs(modemNetConfigs);

                List<ModemInterfaceAddressConfig> modemInterfaceAddressConfigs = new ArrayList<>();
                modemInterfaceAddressConfigs.add(modemInterfaceAddressConfig);
                ((ModemInterfaceConfigImpl) interfaceConfig).setNetInterfaceAddresses(modemInterfaceAddressConfigs);

                ((ModemInterfaceConfigImpl) interfaceConfig).setUsbDevice(usbDevice);

                break;
            case VLAN:
                interfaceConfig = new VlanInterfaceConfigImpl(interfaceName);
                List<NetInterfaceAddressConfig> vlanInterfaceAddressConfigs = new ArrayList<>();
                netInterfaceAddressConfigImpl = new NetInterfaceAddressConfigImpl();
                netInterfaceAddressConfigImpl.setNetConfigs(IpConfigurationInterpreter.populateConfiguration(props,
                        interfaceName, netInterfaceAddressConfigImpl.getAddress(), interfaceConfig.isVirtual()));
                vlanInterfaceAddressConfigs.add(netInterfaceAddressConfigImpl);
                ((VlanInterfaceConfigImpl) interfaceConfig)
                        .setNetInterfaceAddresses(vlanInterfaceAddressConfigs);

                ((VlanInterfaceConfigImpl) interfaceConfig).setUsbDevice(usbDevice);

                break;
            case UNKNOWN:
                logger.trace("Found interface of unknown type in current configuration: {}", interfaceName);
                return;
            default:
                logger.error("Unsupported type {} for interface {}", type, interfaceName);
                return;
        }

        this.netInterfaceConfigs.put(interfaceName, interfaceConfig);
    }

}