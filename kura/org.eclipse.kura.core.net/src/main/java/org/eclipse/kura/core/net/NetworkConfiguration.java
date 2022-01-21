/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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
 *******************************************************************************/
package org.eclipse.kura.core.net;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:methodLength")
public class NetworkConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(NetworkConfiguration.class);

    private static final String ADDRESS = " :: Address: ";
    private static final String GOT_MESSAGE = "got {}: {}";
    private static final String BGSCAN = ".bgscan";
    private static final String HARDWARE_MODE = ".hardwareMode";
    private static final String PASSPHRASE = ".passphrase";
    private static final String SECURITY_TYPE = ".securityType";
    private static final String NET_INTERFACE = "net.interface.";
    private static final String NET_INTERFACES = "net.interfaces";

    private static final Boolean DEFAULT_PERSIST_VALUE = true;
    private static final Integer DEFAULT_MAXFAIL_VALUE = 5;
    private static final Integer DEFAULT_RESET_TIMEOUT_VALUE = 5;
    private static final Integer DEFAULT_IDLE_VALUE = 95;
    private static final String DEFAULT_ACTIVE_FILTER_VALUE = "inbound";
    private static final Integer DEFAULT_LCP_ECHO_FAILURE_VALUE = 0;
    private static final Integer DEFAULT_LCP_ECHO_INTERVAL_VALUE = 0;
    private static final Boolean DEFAULT_GPS_ENABLED_VALUE = false;
    private static final Boolean DEFAULT_DIVERSITY_ENABLED_VALUE = false;
    private static final Boolean DEFAULT_ENABLED_VALUE = false;
    private static final Integer DEFAULT_PROFILE_ID_VALUE = 0;
    private static final Integer DEFAULT_DATA_COMPRESSION_VALUE = 0;
    private static final Integer DEFAULT_HEADER_COMPRESSION_VALUE = 0;

    private final Map<String, NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs;
    private Map<String, Object> properties;
    private boolean recomputeProperties;
    private List<String> modifiedInterfaceNames;

    public NetworkConfiguration() {
        logger.debug("Created empty NetworkConfiguration");
        this.netInterfaceConfigs = new HashMap<>();
    }

    protected SystemService getSystemService() {
        BundleContext context = FrameworkUtil.getBundle(NetworkConfiguration.class).getBundleContext();
        ServiceReference<SystemService> systemServiceSR = context.getServiceReference(SystemService.class);
        return context.getService(systemServiceSR);
    }

    /**
     * Constructor for create a completely new NetComponentConfiguration based on a
     * set of properties
     *
     * @param properties
     *            The properties that represent the new configuration
     * @throws UnknownHostException
     *             If some hostnames can not be resolved
     * @throws KuraException
     *             It there is an internal error
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
            for (String interfaceName : modifiedInterfaces.split(",")) {
                this.modifiedInterfaceNames.add(interfaceName);
            }
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

        StringBuilder prefix = new StringBuilder(netIfConfigPrefix).append("wifi.")
                .append(mode.toString().toLowerCase());

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

        properties.put(prefix + ".ssid", wifiConfig.getSSID());
        properties.put(prefix + ".driver", wifiConfig.getDriver());
        properties.put(prefix + ".mode", wifiConfig.getMode().toString());
        if (wifiConfig.getSecurity() != null) {
            properties.put(prefix + SECURITY_TYPE, wifiConfig.getSecurity().toString());
        } else {
            properties.put(prefix + SECURITY_TYPE, WifiSecurity.NONE.toString());
        }
        properties.put(prefix + ".channel", sbChannel.toString());
        Password psswd = wifiConfig.getPasskey();
        if (psswd != null) {
            properties.put(prefix + PASSPHRASE, psswd);
        } else {
            properties.put(prefix + PASSPHRASE, new Password(""));
        }
        if (wifiConfig.getRadioMode() != null) {
            properties.put(prefix + ".radioMode", wifiConfig.getRadioMode().toString());
        }

        if (wifiConfig.getBgscan() != null) {
            properties.put(prefix + BGSCAN, wifiConfig.getBgscan().toString());
        } else {
            properties.put(prefix + BGSCAN, "");
        }

        if (wifiConfig.getPairwiseCiphers() != null) {
            properties.put(prefix + ".pairwiseCiphers", wifiConfig.getPairwiseCiphers().name());
        }

        if (wifiConfig.getGroupCiphers() != null) {
            properties.put(prefix + ".groupCiphers", wifiConfig.getGroupCiphers().name());
        }

        properties.put(prefix + ".pingAccessPoint", wifiConfig.pingAccessPoint());

        properties.put(prefix + ".ignoreSSID", wifiConfig.ignoreSSID());
    }

    private static WifiConfig getWifiConfig(String netIfConfigPrefix, WifiMode mode, Map<String, Object> properties)
            throws KuraException {

        String key;
        WifiConfig wifiConfig = new WifiConfig();
        StringBuilder prefix = new StringBuilder(netIfConfigPrefix).append("wifi.")
                .append(mode.toString().toLowerCase());

        // mode
        logger.trace("mode is {}", mode);
        wifiConfig.setMode(mode);

        // ssid
        key = prefix + ".ssid";
        String ssid = (String) properties.get(key);
        if (ssid == null) {
            ssid = "";
        }
        logger.trace("SSID is {}", ssid);
        wifiConfig.setSSID(ssid);

        // driver
        key = prefix + ".driver";
        String driver = (String) properties.get(key);
        if (driver == null) {
            driver = "";
        }
        logger.trace("driver is {}", driver);
        wifiConfig.setDriver(driver);

        // security
        key = prefix + SECURITY_TYPE;
        WifiSecurity wifiSecurity = WifiSecurity.NONE;
        String securityString = (String) properties.get(key);
        logger.trace("securityString is {}", securityString);
        if (securityString != null && !securityString.isEmpty()) {
            try {
                wifiSecurity = WifiSecurity.valueOf(securityString);
            } catch (IllegalArgumentException e) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID,
                        "Could not parse wifi security " + securityString);
            }
        }
        wifiConfig.setSecurity(wifiSecurity);

        // channels
        key = prefix + ".channel";
        String channelsString = (String) properties.get(key);
        logger.trace("channelsString is {}", channelsString);
        if (channelsString != null) {
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
                            logger.error("Error parsing channels!", e);
                        }
                    }
                    wifiConfig.setChannels(channels);
                }
            }
        }

        // passphrase
        key = prefix + PASSPHRASE;
        Object psswdObj = properties.get(key);
        String passphrase = null;
        if (psswdObj instanceof Password) {
            Password psswd = (Password) psswdObj;
            passphrase = new String(psswd.getPassword());
        } else if (psswdObj instanceof String) {
            passphrase = (String) psswdObj;
        }

        logger.trace("passphrase is {}", passphrase);
        wifiConfig.setPasskey(passphrase);

        // hardware mode
        key = prefix + HARDWARE_MODE;
        String hwMode = (String) properties.get(key);
        if (hwMode == null) {
            hwMode = "";
        }
        logger.trace("hwMode is {}", hwMode);
        wifiConfig.setHardwareMode(hwMode);

        // ignore SSID
        key = prefix + ".ignoreSSID";
        boolean ignoreSSID = false;
        if (properties.get(key) != null) {
            ignoreSSID = (Boolean) properties.get(key);
            logger.trace("Ignore SSID is {}", ignoreSSID);
        } else {
            logger.trace("Ignore SSID is null");
        }

        wifiConfig.setIgnoreSSID(ignoreSSID);

        key = prefix + ".pairwiseCiphers";
        String pairwiseCiphers = (String) properties.get(key);
        if (pairwiseCiphers != null) {
            wifiConfig.setPairwiseCiphers(WifiCiphers.valueOf(pairwiseCiphers));
        }

        if (mode == WifiMode.INFRA) {
            key = prefix + BGSCAN;
            String bgscan = (String) properties.get(key);
            if (bgscan == null) {
                bgscan = "";
            }
            logger.trace("bgscan is {}", bgscan);
            wifiConfig.setBgscan(new WifiBgscan(bgscan));

            key = prefix + ".groupCiphers";
            String groupCiphers = (String) properties.get(key);
            if (groupCiphers != null) {
                wifiConfig.setGroupCiphers(WifiCiphers.valueOf(groupCiphers));
            }

            // ping access point?
            key = prefix + ".pingAccessPoint";
            boolean pingAccessPoint = false;
            if (properties.get(key) != null) {
                pingAccessPoint = (Boolean) properties.get(key);
                logger.trace("Ping Access Point is {}", pingAccessPoint);
            } else {
                logger.trace("Ping Access Point is null");
            }

            wifiConfig.setPingAccessPoint(pingAccessPoint);
        }

        // radio mode
        key = prefix + ".radioMode";
        WifiRadioMode radioMode;
        String radioModeString = (String) properties.get(key);
        logger.trace("radioModeString is {}", radioModeString);
        if (radioModeString != null && !radioModeString.isEmpty()) {
            try {
                radioMode = WifiRadioMode.valueOf(radioModeString);
                wifiConfig.setRadioMode(radioMode);
            } catch (IllegalArgumentException e) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID,
                        "Could not parse wifi radio mode " + radioModeString);
            }
        }

        if (!wifiConfig.isValid()) {
            return null;
        } else {
            logger.trace("Returning wifiConfig: {}", wifiConfig);
            return wifiConfig;
        }
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

    private static ModemConfig getModemConfig(String prefix, Map<String, Object> properties) throws KuraException {
        ModemConfig modemConfig = new ModemConfig();

        modemConfig.setApn(getApn(prefix, properties));
        modemConfig.setAuthType(getAuthenticationType(prefix, properties));
        modemConfig.setDataCompression(getDataCompression(prefix, properties));
        modemConfig.setDialString(getDialString(prefix, properties));
        modemConfig.setHeaderCompression(getHeaderCompression(prefix, properties));
        modemConfig.setIpAddress(getIpAddress(prefix, properties));
        modemConfig.setPassword(getPassword(prefix, properties));
        modemConfig.setPdpType(getPdpType(prefix, properties));
        modemConfig.setProfileID(getProfileId(prefix, properties));
        modemConfig.setPersist(isPersist(prefix, properties));
        modemConfig.setMaxFail(getMaximumFailures(prefix, properties));
        modemConfig.setResetTimeout(getResetTimeout(prefix, properties));
        modemConfig.setIdle(getIdle(prefix, properties));
        modemConfig.setActiveFilter(getActiveFilter(prefix, properties));
        modemConfig.setLcpEchoInterval(getLcpEchoInterval(prefix, properties));
        modemConfig.setLcpEchoFailure(getLcpEchoFailure(prefix, properties));
        modemConfig.setUsername((String) properties.get(prefix + "username"));
        modemConfig.setEnabled(isEnabled(prefix, properties));
        modemConfig.setGpsEnabled(isGpsEnabled(prefix, properties));
        modemConfig.setDiversityEnabled(isDiversityEnabled(prefix, properties));

        return modemConfig;
    }

    private static boolean isGpsEnabled(String prefix, Map<String, Object> properties) {
        String key = prefix + "gpsEnabled";
        Object value = properties.getOrDefault(key, DEFAULT_GPS_ENABLED_VALUE);
        return value != null ? (Boolean) value : DEFAULT_GPS_ENABLED_VALUE;
    }

    private static boolean isDiversityEnabled(String prefix, Map<String, Object> properties) {
        String key = prefix + "diversityEnabled";
        Object value = properties.getOrDefault(key, DEFAULT_DIVERSITY_ENABLED_VALUE);
        return value != null ? (Boolean) value : DEFAULT_DIVERSITY_ENABLED_VALUE;
    }

    private static boolean isEnabled(String prefix, Map<String, Object> properties) {
        String key = prefix + "enabled";
        Object value = properties.getOrDefault(key, DEFAULT_ENABLED_VALUE);
        return value != null ? (Boolean) value : DEFAULT_ENABLED_VALUE;
    }

    private static int getLcpEchoFailure(String prefix, Map<String, Object> properties) {
        String key = prefix + "lcpEchoFailure";
        Object value = properties.getOrDefault(key, DEFAULT_LCP_ECHO_FAILURE_VALUE);
        return value != null ? (Integer) value : DEFAULT_LCP_ECHO_FAILURE_VALUE;
    }

    private static int getLcpEchoInterval(String prefix, Map<String, Object> properties) {
        String key = prefix + "lcpEchoInterval";
        Object value = properties.getOrDefault(key, DEFAULT_LCP_ECHO_INTERVAL_VALUE);
        return value != null ? (Integer) value : DEFAULT_LCP_ECHO_INTERVAL_VALUE;
    }

    private static String getActiveFilter(String prefix, Map<String, Object> properties) {
        String key = prefix + "activeFilter";
        Object value = properties.getOrDefault(key, DEFAULT_ACTIVE_FILTER_VALUE);
        return value != null ? (String) value : DEFAULT_ACTIVE_FILTER_VALUE;
    }

    private static int getIdle(String prefix, Map<String, Object> properties) {
        String key = prefix + "idle";
        Object value = properties.getOrDefault(key, DEFAULT_IDLE_VALUE);
        return value != null ? (Integer) value : DEFAULT_IDLE_VALUE;
    }

    private static int getResetTimeout(String prefix, Map<String, Object> properties) {
        String key = prefix + "resetTimeout";
        Object value = properties.getOrDefault(key, DEFAULT_RESET_TIMEOUT_VALUE);
        return value != null ? (Integer) value : DEFAULT_RESET_TIMEOUT_VALUE;
    }

    private static int getMaximumFailures(String prefix, Map<String, Object> properties) {
        String key = prefix + "maxFail";
        Object value = properties.getOrDefault(key, DEFAULT_MAXFAIL_VALUE);
        return value != null ? (Integer) value : DEFAULT_MAXFAIL_VALUE;
    }

    private static boolean isPersist(String prefix, Map<String, Object> properties) {
        String key = prefix + "persist";
        Object value = properties.getOrDefault(key, DEFAULT_PERSIST_VALUE);
        return value != null ? (Boolean) value : DEFAULT_PERSIST_VALUE;
    }

    private static int getProfileId(String prefix, Map<String, Object> properties) {
        String key = prefix + "profileId";
        Object value = properties.getOrDefault(key, DEFAULT_PROFILE_ID_VALUE);
        return value != null ? (Integer) value : DEFAULT_PROFILE_ID_VALUE;
    }

    private static PdpType getPdpType(String prefix, Map<String, Object> properties) {
        String key = prefix + "pdpType";
        Object value = properties.getOrDefault(key, PdpType.IP.name());
        return value != null ? parsePdpType((String) value) : PdpType.IP;
    }

    private static PdpType parsePdpType(String pdpTypeString) {
        PdpType pdpType = PdpType.UNKNOWN;
        try {
            pdpType = PdpType.valueOf(pdpTypeString);
        } catch (IllegalArgumentException e) {
            pdpType = PdpType.IP;
        }
        return pdpType;
    }

    private static Password getPassword(String prefix, Map<String, Object> properties) throws KuraException {
        Password password = null;
        String key = prefix + "password";
        Object psswdObj = properties.get(key);
        if (psswdObj instanceof Password) {
            password = (Password) psswdObj;
        } else if (psswdObj instanceof String) {
            password = new Password((String) psswdObj);
        } else if (psswdObj == null) {
            password = new Password("");
        } else {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, "Invalid password type.", key,
                    psswdObj.getClass());
        }
        return password;
    }

    private static IPAddress getIpAddress(String prefix, Map<String, Object> properties) throws KuraException {
        String ipAddressString = (String) properties.get(prefix + "ipAddress");
        IPAddress ipAddress = null;
        logger.trace("IP address is {}", ipAddressString);
        if (ipAddressString != null && !ipAddressString.isEmpty()) {
            ipAddress = parseIpAddress(ipAddressString);
        } else {
            logger.trace("IP address is null");
        }
        return ipAddress;
    }

    private static IPAddress parseIpAddress(String ipAddressString) throws KuraException {
        try {
            return IPAddress.parseHostAddress(ipAddressString);
        } catch (UnknownHostException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Could not parse ip address " + ipAddressString);
        }
    }

    private static int getHeaderCompression(String prefix, Map<String, Object> properties) {
        String key = prefix + "headerCompression";
        Object value = properties.getOrDefault(key, DEFAULT_HEADER_COMPRESSION_VALUE);
        return value != null ? (Integer) value : DEFAULT_HEADER_COMPRESSION_VALUE;
    }

    private static String getDialString(String prefix, Map<String, Object> properties) {
        String key = prefix + "dialString";
        return (String) properties.get(key);
    }

    private static int getDataCompression(String prefix, Map<String, Object> properties) {
        String key = prefix + "dataCompression";
        Object value = properties.getOrDefault(key, DEFAULT_DATA_COMPRESSION_VALUE);
        return value != null ? (Integer) value : DEFAULT_DATA_COMPRESSION_VALUE;
    }

    private static AuthType getAuthenticationType(String prefix, Map<String, Object> properties) throws KuraException {
        String key = prefix + "authType";
        String authTypeString = (String) properties.get(key);
        AuthType authType;
        logger.trace("Auth type is {}", authTypeString);
        if (authTypeString != null && !authTypeString.isEmpty()) {
            authType = parseAuthenticationType(authTypeString);
        } else {
            logger.trace("Auth type is null");
            authType = AuthType.NONE;
        }
        return authType;
    }

    private static AuthType parseAuthenticationType(String authTypeString) throws KuraException {
        AuthType authType = AuthType.NONE;
        try {
            authType = AuthType.valueOf(authTypeString);
        } catch (IllegalArgumentException e) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Could not parse auth type " + authTypeString);
        }
        return authType;
    }

    private static String getApn(String prefix, Map<String, Object> properties) {
        String key = prefix + "apn";
        return (String) properties.get(key);
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

        switch (type) {
        case LOOPBACK:
            LoopbackInterfaceConfigImpl loopbackInterfaceConfig = new LoopbackInterfaceConfigImpl(interfaceName);
            List<NetInterfaceAddressConfig> loopbackInterfaceAddressConfigs = new ArrayList<>();
            loopbackInterfaceAddressConfigs.add(new NetInterfaceAddressConfigImpl());
            loopbackInterfaceConfig.setNetInterfaceAddresses(loopbackInterfaceAddressConfigs);

            populateNetInterfaceConfiguration(loopbackInterfaceConfig, props);

            this.netInterfaceConfigs.put(interfaceName, loopbackInterfaceConfig);
            break;
        case ETHERNET:
            EthernetInterfaceConfigImpl ethernetInterfaceConfig = new EthernetInterfaceConfigImpl(interfaceName);
            List<NetInterfaceAddressConfig> ethernetInterfaceAddressConfigs = new ArrayList<>();
            ethernetInterfaceAddressConfigs.add(new NetInterfaceAddressConfigImpl());
            ethernetInterfaceConfig.setNetInterfaceAddresses(ethernetInterfaceAddressConfigs);

            populateNetInterfaceConfiguration(ethernetInterfaceConfig, props);

            this.netInterfaceConfigs.put(interfaceName, ethernetInterfaceConfig);
            break;
        case WIFI:
            WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);

            List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = new ArrayList<>();
            wifiInterfaceAddressConfigs.add(new WifiInterfaceAddressConfigImpl());
            wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);

            populateNetInterfaceConfiguration(wifiInterfaceConfig, props);

            this.netInterfaceConfigs.put(interfaceName, wifiInterfaceConfig);
            break;
        case MODEM:
            ModemInterfaceConfigImpl modemInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);

            List<ModemInterfaceAddressConfig> modemInterfaceAddressConfigs = new ArrayList<>();
            modemInterfaceAddressConfigs.add(new ModemInterfaceAddressConfigImpl());
            modemInterfaceConfig.setNetInterfaceAddresses(modemInterfaceAddressConfigs);

            populateNetInterfaceConfiguration(modemInterfaceConfig, props);

            this.netInterfaceConfigs.put(interfaceName, modemInterfaceConfig);
            break;
        case UNKNOWN:
            logger.trace("Found interface of unknown type in current configuration: {}", interfaceName);
            break;
        default:
            logger.error("Unsupported type {} for interface {}", type, interfaceName);
            break;
        }
    }

    private Optional<NetInterfaceType> getInterfaceType(
            AbstractNetInterface<? extends NetInterfaceAddressConfig> netInterfaceConfig, Map<String, Object> props) {
        Optional<NetInterfaceType> interfaceType = Optional.empty();

        String interfaceName = netInterfaceConfig.getName();
        StringBuilder keyBuffer = new StringBuilder();
        keyBuffer.append(NET_INTERFACE).append(interfaceName).append(".type");
        Object type = props.get(keyBuffer.toString());
        if (type == null) {
            logger.debug("Interface {} type not found in properties. Try to infer it from the object class.",
                    interfaceName);
            if (netInterfaceConfig instanceof EthernetInterfaceConfigImpl) {
                interfaceType = Optional.of(NetInterfaceType.ETHERNET);
            } else if (netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                interfaceType = Optional.of(NetInterfaceType.WIFI);
            } else if (netInterfaceConfig instanceof LoopbackInterfaceConfigImpl) {
                interfaceType = Optional.of(NetInterfaceType.LOOPBACK);
            } else if (netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
                interfaceType = Optional.of(NetInterfaceType.MODEM);
            }
        } else {
            interfaceType = Optional.of(NetInterfaceType.valueOf((String) type));
        }
        return interfaceType;
    }

    public void populateNetInterfaceConfiguration(
            AbstractNetInterface<? extends NetInterfaceAddressConfig> netInterfaceConfig, Map<String, Object> props)
            throws UnknownHostException, KuraException {
        String interfaceName = netInterfaceConfig.getName();
        Optional<NetInterfaceType> interfaceTypeOptional = getInterfaceType(netInterfaceConfig, props);
        if (!interfaceTypeOptional.isPresent()) {
            logger.debug("Interface {} type not found.", interfaceName);
            return;
        }
        NetInterfaceType interfaceType = interfaceTypeOptional.get();

        // build the prefixes for all the properties associated with this interface
        StringBuilder sbPrefix = new StringBuilder();
        sbPrefix.append(NET_INTERFACE).append(interfaceName).append(".");
        String netIfReadOnlyPrefix = sbPrefix.toString();

        String netIfPrefix = sbPrefix.append("config.").toString();
        String netIfConfigPrefix = sbPrefix.toString();

        // USB
        String vendorId = (String) props.get(netIfReadOnlyPrefix + "usb.vendor.id");
        String vendorName = (String) props.get(netIfReadOnlyPrefix + "usb.vendor.name");
        String productId = (String) props.get(netIfReadOnlyPrefix + "usb.product.id");
        String productName = (String) props.get(netIfReadOnlyPrefix + "usb.product.name");
        String usbBusNumber = (String) props.get(netIfReadOnlyPrefix + "usb.busNumber");
        String usbDevicePath = (String) props.get(netIfReadOnlyPrefix + "usb.devicePath");

        if (vendorId != null && productId != null) {
            UsbDevice usbDevice = new UsbNetDevice(vendorId, productId, vendorName, productName, usbBusNumber,
                    usbDevicePath, interfaceName);
            logger.trace("adding usbDevice: {}, port: {}", usbDevice, usbDevice.getUsbPort());
            netInterfaceConfig.setUsbDevice(usbDevice);
        }

        // Status
        String configStatus4 = null;
        String configStatus4Key = NET_INTERFACE + interfaceName + ".config.ip4.status";
        if (props.containsKey(configStatus4Key)) {
            configStatus4 = (String) props.get(configStatus4Key);
        } else {
            configStatus4 = NetInterfaceStatus.netIPv4StatusDisabled.name();
            if ((netInterfaceConfig instanceof EthernetInterfaceConfigImpl
                    || netInterfaceConfig instanceof WifiInterfaceConfigImpl) && netInterfaceConfig.isVirtual()) {
                SystemService service = getSystemService();
                if (service != null) {
                    configStatus4 = NetInterfaceStatus.valueOf(service.getNetVirtualDevicesConfig()).name();
                }
            }
        }
        logger.trace("Status Ipv4? {}", configStatus4);

        String configStatus6 = null;
        String configStatus6Key = NET_INTERFACE + interfaceName + ".config.ip6.status";
        if (props.containsKey(configStatus6Key)) {
            configStatus6 = (String) props.get(configStatus6Key);
        }
        if (configStatus6 == null) {
            configStatus6 = NetInterfaceStatus.netIPv6StatusDisabled.name();
        }

        // POPULATE NetInterfaceAddresses
        for (NetInterfaceAddressConfig netInterfaceAddress : netInterfaceConfig.getNetInterfaceAddresses()) {

            List<NetConfig> netConfigs = new ArrayList<>();
            if (netInterfaceAddress instanceof NetInterfaceAddressConfigImpl) {
                ((NetInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(netConfigs);
            } else if (netInterfaceAddress instanceof WifiInterfaceAddressConfigImpl) {
                ((WifiInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(netConfigs);
            } else if (netInterfaceAddress instanceof ModemInterfaceAddressConfigImpl) {
                ((ModemInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(netConfigs);
            }

            // WifiInterfaceAddress
            if (netInterfaceAddress instanceof WifiInterfaceAddressImpl) {
                logger.trace("netInterfaceAddress is instanceof WifiInterfaceAddressImpl");
                WifiInterfaceAddressImpl wifiInterfaceAddressImpl = (WifiInterfaceAddressImpl) netInterfaceAddress;

                // wifi mode
                String configWifiMode = netIfPrefix + "wifi.mode";
                if (props.containsKey(configWifiMode)) {

                    WifiMode mode = WifiMode.INFRA;
                    if (props.get(configWifiMode) != null) {
                        mode = WifiMode.valueOf((String) props.get(configWifiMode));
                    }

                    logger.trace("Adding wifiMode: {}", mode);
                    wifiInterfaceAddressImpl.setMode(mode);
                }
            }

            // ModemInterfaceAddress
            if (netInterfaceAddress instanceof ModemInterfaceAddressConfigImpl) {
                logger.trace("netInterfaceAddress is instanceof ModemInterfaceAddressConfigImpl");
                ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = (ModemInterfaceAddressConfigImpl) netInterfaceAddress;

                // connection type
                String configConnType = netIfPrefix + "connection.type";
                if (props.containsKey(configConnType)) {
                    ModemConnectionType connType = ModemConnectionType.PPP;
                    String connTypeStr = (String) props.get(configConnType);
                    if (connTypeStr != null && !connTypeStr.isEmpty()) {
                        connType = ModemConnectionType.valueOf(connTypeStr);
                    }

                    logger.trace("Adding modem connection type: {}", connType);
                    modemInterfaceAddressImpl.setConnectionType(connType);
                }

                // connection status
                String configConnStatus = netIfPrefix + "connection.status";
                if (props.containsKey(configConnStatus)) {
                    ModemConnectionStatus connStatus = ModemConnectionStatus.UNKNOWN;
                    String connStatusStr = (String) props.get(configConnStatus);
                    if (connStatusStr != null && !connStatusStr.isEmpty()) {
                        connStatus = ModemConnectionStatus.valueOf(connStatusStr);
                    }

                    logger.trace("Adding modem connection status: {}", connStatus);
                    modemInterfaceAddressImpl.setConnectionStatus(connStatus);
                }
            }

            // POPULATE NetConfigs
            // dhcp4
            NetConfigIP4 netConfigIP4;
            boolean dhcpEnabled = isDhcpClient4Enabled(props, interfaceName);

            NetInterfaceStatus status4 = NetInterfaceStatus.valueOf(configStatus4);
            netConfigIP4 = new NetConfigIP4(status4, getAutoConnectProperty(status4));
            netConfigs.add(netConfigIP4);

            if (dhcpEnabled) {
                netConfigIP4.setDhcp(true);
            } else {
                // NetConfigIP4
                String configIp4 = NET_INTERFACE + interfaceName + ".config.ip4.address";
                if (props.containsKey(configIp4)) {
                    logger.trace(GOT_MESSAGE, configIp4, props.get(configIp4));

                    // address
                    String addressIp4 = (String) props.get(configIp4);
                    logger.trace("IPv4 address: {}", addressIp4);
                    if (addressIp4 != null && !addressIp4.isEmpty()) {
                        IP4Address ip4Address = (IP4Address) IPAddress.parseHostAddress(addressIp4);
                        netConfigIP4.setAddress(ip4Address);
                    }

                    // prefix
                    String configIp4Prefix = NET_INTERFACE + interfaceName + ".config.ip4.prefix";
                    short networkPrefixLength = -1;
                    if (props.containsKey(configIp4Prefix)) {
                        if (props.get(configIp4Prefix) instanceof Short) {
                            networkPrefixLength = (Short) props.get(configIp4Prefix);
                        } else if (props.get(configIp4Prefix) instanceof String) {
                            networkPrefixLength = Short.parseShort((String) props.get(configIp4Prefix));
                        }

                        try {
                            netConfigIP4.setNetworkPrefixLength(networkPrefixLength);
                        } catch (KuraException e) {
                            logger.error("Exception while setting Network Prefix length!", e);
                        }
                    }

                    // gateway
                    String configIp4Gateway = NET_INTERFACE + interfaceName + ".config.ip4.gateway";
                    if (props.containsKey(configIp4Gateway)) {

                        String gatewayIp4 = (String) props.get(configIp4Gateway);
                        logger.trace("IPv4 gateway: {}", gatewayIp4);
                        if (gatewayIp4 != null && !gatewayIp4.isEmpty()) {
                            IP4Address ip4Gateway = (IP4Address) IPAddress.parseHostAddress(gatewayIp4);
                            netConfigIP4.setGateway(ip4Gateway);
                        }
                    }
                }
            }

            // dns servers
            String configDNSs = NET_INTERFACE + interfaceName + ".config.ip4.dnsServers";
            if (props.containsKey(configDNSs)) {

                List<IP4Address> dnsIPs = new ArrayList<>();
                String dnsAll = (String) props.get(configDNSs);
                String[] dnss = dnsAll.split(",");
                for (String dns : dnss) {
                    if (dns != null && dns.length() > 0) {
                        logger.trace("IPv4 DNS: {}", dns);
                        IP4Address dnsIp4 = (IP4Address) IPAddress.parseHostAddress(dns);
                        dnsIPs.add(dnsIp4);
                    }
                }
                netConfigIP4.setDnsServers(dnsIPs);
            }

            // win servers
            String configWINSs = NET_INTERFACE + interfaceName + ".config.ip4.winsServers";
            if (props.containsKey(configWINSs)) {

                List<IP4Address> winsIPs = new ArrayList<>();
                String winsAll = (String) props.get(configWINSs);
                String[] winss = winsAll.split(",");
                for (String wins : winss) {
                    logger.trace("WINS: {}", wins);
                    IP4Address winsIp4 = (IP4Address) IPAddress.parseHostAddress(wins);
                    winsIPs.add(winsIp4);
                }
                netConfigIP4.setWinsServers(winsIPs);
            }

            // domains
            String configDomains = NET_INTERFACE + interfaceName + ".config.ip4.domains";
            if (props.containsKey(configDomains)) {

                List<String> domainNames = new ArrayList<>();
                String domainsAll = (String) props.get(configDomains);
                String[] domains = domainsAll.split(",");
                for (String domain : domains) {
                    logger.trace("IPv4 Domain: {}", domain);
                    domainNames.add(domain);
                }
                netConfigIP4.setDomains(domainNames);
            }

            // FirewallNatConfig - see if NAT is enabled
            String configNatEnabled = NET_INTERFACE + interfaceName + ".config.nat.enabled";
            if (props.containsKey(configNatEnabled)) {
                boolean natEnabled = (Boolean) props.get(configNatEnabled);
                logger.trace("NAT enabled? {}", natEnabled);

                if (natEnabled) {
                    FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig(interfaceName, "unknown", true);
                    netConfigs.add(natConfig);
                }
            }

            // DhcpServerConfigIP4 - see if there is a DHCP 4 Server
            String configDhcpServerEnabled = NET_INTERFACE + interfaceName + ".config.dhcpServer4.enabled";
            if (props.containsKey(configDhcpServerEnabled)) {
                boolean dhcpServerEnabled = (Boolean) props.get(configDhcpServerEnabled);
                logger.trace("DHCP Server 4 enabled? {}", dhcpServerEnabled);

                IP4Address subnet = null;
                IP4Address routerAddress = dhcpEnabled ? (IP4Address) netInterfaceAddress.getAddress()
                        : netConfigIP4.getAddress();
                IP4Address subnetMask = null;
                int defaultLeaseTime = -1;
                int maximumLeaseTime = -1;
                short prefix = -1;
                IP4Address rangeStart = null;
                IP4Address rangeEnd = null;
                boolean passDns = false;
                List<IP4Address> dnServers = new ArrayList<>();

                // prefix
                String configDhcpServerPrefix = NET_INTERFACE + interfaceName + ".config.dhcpServer4.prefix";
                if (props.containsKey(configDhcpServerPrefix)) {
                    if (props.get(configDhcpServerPrefix) instanceof Short) {
                        prefix = (Short) props.get(configDhcpServerPrefix);
                    } else if (props.get(configDhcpServerPrefix) instanceof String) {
                        prefix = Short.parseShort((String) props.get(configDhcpServerPrefix));
                    }
                    logger.trace("DHCP Server prefix: {}", prefix);
                }

                // rangeStart
                String configDhcpServerRangeStart = NET_INTERFACE + interfaceName + ".config.dhcpServer4.rangeStart";
                if (props.containsKey(configDhcpServerRangeStart)) {
                    String dhcpServerRangeStart = (String) props.get(configDhcpServerRangeStart);
                    logger.trace("DHCP Server Range Start: {}", dhcpServerRangeStart);
                    if (dhcpServerRangeStart != null && !dhcpServerRangeStart.isEmpty()) {
                        rangeStart = (IP4Address) IPAddress.parseHostAddress(dhcpServerRangeStart);
                    }
                }

                // rangeEnd
                String configDhcpServerRangeEnd = NET_INTERFACE + interfaceName + ".config.dhcpServer4.rangeEnd";
                if (props.containsKey(configDhcpServerRangeEnd)) {
                    String dhcpServerRangeEnd = (String) props.get(configDhcpServerRangeEnd);
                    logger.trace("DHCP Server Range End: {}", dhcpServerRangeEnd);
                    if (dhcpServerRangeEnd != null && !dhcpServerRangeEnd.isEmpty()) {
                        rangeEnd = (IP4Address) IPAddress.parseHostAddress(dhcpServerRangeEnd);
                    }
                }

                // default lease time
                String configDhcpServerDefaultLeaseTime = NET_INTERFACE + interfaceName
                        + ".config.dhcpServer4.defaultLeaseTime";
                if (props.containsKey(configDhcpServerDefaultLeaseTime)) {
                    if (props.get(configDhcpServerDefaultLeaseTime) instanceof Integer) {
                        defaultLeaseTime = (Integer) props.get(configDhcpServerDefaultLeaseTime);
                    } else if (props.get(configDhcpServerDefaultLeaseTime) instanceof String) {
                        defaultLeaseTime = Integer.parseInt((String) props.get(configDhcpServerDefaultLeaseTime));
                    }
                    logger.trace("DHCP Server Default Lease Time: {}", defaultLeaseTime);
                }

                // max lease time
                String configDhcpServerMaxLeaseTime = NET_INTERFACE + interfaceName
                        + ".config.dhcpServer4.maxLeaseTime";
                if (props.containsKey(configDhcpServerMaxLeaseTime)) {
                    if (props.get(configDhcpServerMaxLeaseTime) instanceof Integer) {
                        maximumLeaseTime = (Integer) props.get(configDhcpServerMaxLeaseTime);
                    } else if (props.get(configDhcpServerMaxLeaseTime) instanceof String) {
                        maximumLeaseTime = Integer.parseInt((String) props.get(configDhcpServerMaxLeaseTime));
                    }
                    logger.trace("DHCP Server Maximum Lease Time: {}", maximumLeaseTime);
                }

                // passDns
                String configDhcpServerPassDns = NET_INTERFACE + interfaceName + ".config.dhcpServer4.passDns";
                if (props.containsKey(configDhcpServerPassDns)) {
                    if (props.get(configDhcpServerPassDns) instanceof Boolean) {
                        passDns = (Boolean) props.get(configDhcpServerPassDns);
                    } else if (props.get(configDhcpServerPassDns) instanceof String) {
                        passDns = Boolean.parseBoolean((String) props.get(configDhcpServerPassDns));
                    }
                    logger.trace("DHCP Server Pass DNS?: {}", passDns);
                }

                if (routerAddress != null && rangeStart != null && rangeEnd != null) {
                    // get the netmask and subnet
                    int prefixInt = prefix;
                    int mask = ~((1 << 32 - prefixInt) - 1);
                    String subnetMaskString = NetworkUtil.dottedQuad(mask);
                    String subnetString = NetworkUtil.calculateNetwork(routerAddress.getHostAddress(),
                            subnetMaskString);
                    subnet = (IP4Address) IPAddress.parseHostAddress(subnetString);
                    subnetMask = (IP4Address) IPAddress.parseHostAddress(subnetMaskString);

                    dnServers.add(routerAddress);

                    DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(interfaceName, dhcpServerEnabled, defaultLeaseTime,
                            maximumLeaseTime, passDns);
                    DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix, routerAddress,
                            rangeStart, rangeEnd, dnServers);
                    try {
                        netConfigs.add(new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4));
                    } catch (KuraException e) {
                        logger.warn("This invalid DhcpServerCfgIP4 configuration is ignored - {}, {}", dhcpServerCfg,
                                dhcpServerCfgIP4);
                    }
                } else {
                    if (logger.isTraceEnabled()) {
                        StringBuilder sb = new StringBuilder("Not including DhcpServerConfig - router: ");
                        sb.append(routerAddress);
                        sb.append(", range start: ");
                        sb.append(rangeStart);
                        sb.append(", range end: ");
                        sb.append(rangeEnd);
                        logger.trace(sb.toString());
                    }
                }
            }

            // dhcp6
            String configDhcp6 = NET_INTERFACE + interfaceName + ".config.dhcpClient6.enabled";
            NetConfigIP6 netConfigIP6 = null;
            boolean dhcp6Enabled = false;
            if (props.containsKey(configDhcp6)) {
                dhcp6Enabled = (Boolean) props.get(configDhcp6);
                logger.trace("DHCP 6 enabled? {}", dhcp6Enabled);
            }

            if (!dhcp6Enabled) {
                // ip6
                NetInterfaceStatus status6 = NetInterfaceStatus.valueOf(configStatus6);
                netConfigIP6 = new NetConfigIP6(status6, getAutoConnectProperty(status6), dhcp6Enabled);
                netConfigs.add(netConfigIP6);

                String configIp6 = NET_INTERFACE + interfaceName + ".config.ip6.address";
                if (props.containsKey(configIp6)) {

                    // address
                    String addressIp6 = (String) props.get(configIp6);
                    logger.trace("IPv6 address: {}", addressIp6);
                    if (addressIp6 != null && !addressIp6.isEmpty()) {
                        IP6Address ip6Address = (IP6Address) IPAddress.parseHostAddress(addressIp6);
                        netConfigIP6.setAddress(ip6Address);
                    }

                    // dns servers
                    String configDNSs6 = NET_INTERFACE + interfaceName + ".config.ip6.dnsServers";
                    if (props.containsKey(configDNSs6)) {

                        List<IP6Address> dnsIPs = new ArrayList<>();
                        String dnsAll = (String) props.get(configDNSs6);
                        String[] dnss = dnsAll.split(",");
                        for (String dns : dnss) {
                            logger.trace("IPv6 DNS: {}", dns);
                            IP6Address dnsIp6 = (IP6Address) IPAddress.parseHostAddress(dns);
                            dnsIPs.add(dnsIp6);
                        }
                        netConfigIP6.setDnsServers(dnsIPs);
                    }

                    // domains
                    String configDomains6 = NET_INTERFACE + interfaceName + ".config.ip6.domains";
                    if (props.containsKey(configDomains6)) {

                        List<String> domainNames = new ArrayList<>();
                        String domainsAll = (String) props.get(configDomains6);
                        String[] domains = domainsAll.split(",");
                        for (String domain : domains) {
                            logger.trace("IPv6 Domain: {}", domain);
                            domainNames.add(domain);
                        }
                        netConfigIP6.setDomains(domainNames);
                    }
                }
            }

            if (interfaceType == NetInterfaceType.WIFI) {
                logger.trace("Adding wifi netconfig");

                // Wifi access point config
                WifiConfig apConfig = getWifiConfig(netIfConfigPrefix, WifiMode.MASTER, props);
                if (apConfig != null) {
                    logger.trace("Adding AP wifi config");
                    netConfigs.add(apConfig);
                } else {
                    logger.warn("no AP wifi config specified");
                }

                WifiConfig infraConfig = getWifiConfig(netIfConfigPrefix, WifiMode.INFRA, props);
                if (infraConfig != null) {
                    logger.trace("Adding client INFRA wifi config");
                    netConfigs.add(infraConfig);
                } else {
                    logger.warn("no INFRA wifi config specified");
                }
            }

            if (interfaceType == NetInterfaceType.MODEM) {
                logger.trace("Adding modem netconfig");

                ModemConfig modemConfig = getModemConfig(netIfConfigPrefix, props);
                modemConfig.setPppNumber(((ModemInterfaceConfigImpl) netInterfaceConfig).getPppNum());
                netConfigs.add(modemConfig);
            }
        }

    }

    private boolean isDhcpClient4Enabled(Map<String, Object> props, String interfaceName) {
        String configDhcp4 = NET_INTERFACE + interfaceName + ".config.dhcpClient4.enabled";
        boolean dhcpEnabled = false;
        if (props.containsKey(configDhcp4)) {
            dhcpEnabled = (Boolean) props.get(configDhcp4);
            logger.trace("DHCP 4 enabled? {}", dhcpEnabled);
        }
        return dhcpEnabled;
    }

    private boolean getAutoConnectProperty(NetInterfaceStatus status) {
        boolean autoconnect = false;
        if (status.equals(NetInterfaceStatus.netIPv4StatusEnabledLAN)
                || status.equals(NetInterfaceStatus.netIPv4StatusEnabledWAN)
                || status.equals(NetInterfaceStatus.netIPv4StatusL2Only)
                || status.equals(NetInterfaceStatus.netIPv6StatusEnabledLAN)
                || status.equals(NetInterfaceStatus.netIPv6StatusEnabledWAN)
                || status.equals(NetInterfaceStatus.netIPv6StatusL2Only)) {
            autoconnect = true;
        }
        return autoconnect;
    }
}
