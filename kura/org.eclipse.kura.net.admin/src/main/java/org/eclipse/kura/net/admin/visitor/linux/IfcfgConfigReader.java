/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Benjamin Cabé - fix for GH issue #299
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.FileInputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dhcp.DhcpClientLeases;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.IfcfgConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigReader extends IfcfgConfig implements NetworkConfigurationVisitor {

    private class IfaceConfig {

        private NetInterfaceStatus netInterfaceStatus = null;
        private boolean autoConnect = false;
        private boolean dhcp = false;
        private IP4Address address = null;
        private String prefixString = null;
        private String netmask = null;
        private String gateway = null;

        private IfaceConfig(NetInterfaceStatus netInterfaceStatus, boolean autoConnect, boolean dhcp,
                IP4Address address, String prefixString, String netmask, String gateway) {
            this.netInterfaceStatus = netInterfaceStatus;
            this.autoConnect = autoConnect;
            this.dhcp = dhcp;
            this.address = address;
            this.prefixString = prefixString;
            this.netmask = netmask;
            this.gateway = gateway;
        }

        public NetInterfaceStatus getNetInterfaceStatus() {
            return netInterfaceStatus;
        }

        public boolean isAutoConnect() {
            return autoConnect;
        }

        public boolean isDhcp() {
            return dhcp;
        }

        public IP4Address getAddress() {
            return address;
        }

        public String getPrefixString() {
            return prefixString;
        }

        public String getNetmask() {
            return netmask;
        }

        public String getGateway() {
            return gateway;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(IfcfgConfigReader.class);

    private static IfcfgConfigReader instance;

    public static IfcfgConfigReader getInstance() {
        if (instance == null) {
            instance = new IfcfgConfigReader();
        }
        return instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getNetInterfaceConfigs();

        Properties kuraExtendedProps = getKuranetProperties();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            getConfig(netInterfaceConfig, kuraExtendedProps);
        }
    }

    protected Properties getKuranetProperties() {
        return KuranetConfig.getProperties();
    }

    protected String getIfcfgDirectory() {
        if (isDebian()) {
            return DEBIAN_NET_CONFIGURATION_DIRECTORY;
        }

        return REDHAT_NET_CONFIGURATION_DIRECTORY;
    }

    private void getConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig,
            Properties kuraExtendedProps) throws KuraException {

        String interfaceName = netInterfaceConfig.getName();
        logger.debug("Getting config for {}", interfaceName);

        File ifcfgFile = getIfcfgFile(interfaceName);
        if (!ifcfgFile.exists()) {
            logger.error("getConfig() :: The {} file doesn't exist", interfaceName);
            return;
        }

        NetInterfaceType type = netInterfaceConfig.getType();
        if (type != NetInterfaceType.ETHERNET && type != NetInterfaceType.WIFI && type != NetInterfaceType.LOOPBACK) {
            logger.info("getConfig() :: The {} file doesn't contain configuration for the {} interface.", ifcfgFile,
                    interfaceName);
            return;
        }

        Properties kuraProps = getKuraPropertiesFromIfaceConfigFile(interfaceName, ifcfgFile);
        IfaceConfig ifaceConfig = getIfaceConfig(interfaceName, kuraProps, kuraExtendedProps);

        NetInterfaceAddressConfig netInterfaceAddressConfig = netInterfaceConfig.getNetInterfaceAddresses().get(0);
        if (netInterfaceAddressConfig == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "InterfaceAddressConfig list is null");
        }
        List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
        if (netConfigs == null) {
            netConfigs = new ArrayList<>();
            setNetInterfaceAddressConfigs(interfaceName, ifaceConfig.isDhcp(), netConfigs, netInterfaceAddressConfig);
        }
        NetConfigIP4 netConfig = new NetConfigIP4(ifaceConfig.getNetInterfaceStatus(), ifaceConfig.isAutoConnect());
        setNetConfigIP4(netConfig, ifaceConfig.isDhcp(), ifaceConfig.getAddress(), ifaceConfig.getGateway(),
                ifaceConfig.getPrefixString(), ifaceConfig.getNetmask(), kuraProps);
        logger.debug("NetConfig: {}", netConfig);
        netConfigs.add(netConfig);
    }

    private IfaceConfig getIfaceConfig(String ifaceName, Properties kuraProps, Properties kuraExtendedProps)
            throws KuraException {
        boolean autoConnect = false;
        boolean dhcp = false;
        IP4Address address = null;
        String prefixString = null;
        String netmask = null;
        String gateway = null;
        NetInterfaceStatus netInterfaceStatus = getNetInterfaceStatus(ifaceName, kuraExtendedProps);
        if (kuraProps != null) {
            autoConnect = "yes".equals(kuraProps.getProperty(IfcfgConfig.ONBOOT_PROP_NAME)) ? true : false;
            boolean defroute = "yes".equals(kuraProps.getProperty(DEFROUTE_PROP_NAME)) ? true : false;

            String bootproto = kuraProps.getProperty(BOOTPROTO_PROP_NAME);
            if (bootproto == null) {
                bootproto = "static";
            }

            // correct the status if needed by validating against the actual properties
            netInterfaceStatus = getNetInterfaceStatus(netInterfaceStatus, autoConnect, defroute);

            // check for dhcp or static configuration
            gateway = kuraProps.getProperty(GATEWAY_PROP_NAME);
            logger.debug("got gateway for {}: {}", ifaceName, gateway);

            if ("dhcp".equals(bootproto)) {
                logger.debug(
                        "getIfaceConfig() :: Interface configuration mode for the {} interface is currently set for DHCP",
                        ifaceName);
                dhcp = true;
            } else if ("static".equals(bootproto)) {
                logger.debug(
                        "getIfaceConfig() :: Interface configuration mode for the {} interface is currently set for static IP address",
                        ifaceName);
                String ipAddress = kuraProps.getProperty(IPADDR_PROP_NAME);
                prefixString = kuraProps.getProperty(PREFIX_PROP_NAME);
                netmask = kuraProps.getProperty(NETMASK_PROP_NAME);
                if (autoConnect) {
                    // make sure at least prefix or netmask is present
                    address = parseIpAddress(ipAddress, netmask, prefixString);
                }
            }
        }
        return new IfaceConfig(netInterfaceStatus, autoConnect, dhcp, address, prefixString, netmask, gateway);
    }

    private IP4Address parseIpAddress(String ipAddress, String netmask, String prefix) throws KuraException {
        if (netmask == null && prefix == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "malformatted network interface configuration file: must contain NETMASK and/or PREFIX");
        }
        IP4Address address = null;
        if (ipAddress != null && !ipAddress.isEmpty()) {
            try {
                address = (IP4Address) IPAddress.parseHostAddress(ipAddress);
            } catch (UnknownHostException e) {
                logger.warn("parseIpAddress() :: Error parsing address: {}", ipAddress, e);
            }
        }
        return address;
    }

    private NetInterfaceStatus getNetInterfaceStatus(String ifaceName, Properties kuraExtendedProps) {
        NetInterfaceStatus netInterfaceStatus;
        StringBuilder sb = new StringBuilder().append("net.interface.").append(ifaceName).append(".config.ip4.status");
        if (kuraExtendedProps != null && kuraExtendedProps.getProperty(sb.toString()) != null) {
            netInterfaceStatus = NetInterfaceStatus.valueOf(kuraExtendedProps.getProperty(sb.toString()));
        } else {
            netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
        }
        logger.debug("Setting NetInterfaceStatus to {} for {}", netInterfaceStatus, ifaceName);
        return netInterfaceStatus;
    }

    private NetInterfaceStatus getNetInterfaceStatus(NetInterfaceStatus netInterfaceStatus, boolean autoConnect,
            boolean defroute) {

        NetInterfaceStatus ret = netInterfaceStatus;
        if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusDisabled && autoConnect) {
            if (!defroute) {
                ret = NetInterfaceStatus.netIPv4StatusEnabledLAN;
            } else {
                ret = NetInterfaceStatus.netIPv4StatusEnabledWAN;
            }
        }
        return ret;
    }

    private File getIfcfgFile(String interfaceName) {
        String fileName = getIfcfgDirectory();

        if (isDebian()) {
            fileName += "/interfaces";
        } else {
            fileName += "/ifcfg-" + interfaceName;
        }

        return new File(fileName);
    }

    private Properties getKuraPropertiesFromIfaceConfigFile(String ifaceName, File ifcfgFile) throws KuraException {
        Properties props;
        // found our match so load the properties
        if (isDebian()) {
            props = parseDebianConfigFile(ifcfgFile, ifaceName);
        } else {
            props = parseRedhatConfigFile(ifcfgFile, ifaceName);
        }
        return props;
    }

    private Properties parseRedhatConfigFile(File ifcfgFile, String interfaceName) {
        Properties kuraProps = new Properties();
        try (FileInputStream fis = new FileInputStream(ifcfgFile)) {
            kuraProps.load(fis);
            // Values in the config file may be surrounded with double quotes or single quotes.
            for (String key : kuraProps.stringPropertyNames()) {
                String value = kuraProps.getProperty(key);
                if (value.length() >= 2 && (value.startsWith("'") && value.endsWith("'")
                        || value.startsWith("\"") && value.endsWith("\""))) {
                    value = value.substring(1, value.length() - 1);
                    kuraProps.put(key, value);
                }
            }
        } catch (Exception e) {
            logger.error("Could not get configuration for " + interfaceName, e);
        }
        return kuraProps;
    }

    private void setNetInterfaceAddressConfigs(String interfaceName, boolean dhcp, List<NetConfig> netConfigs,
            NetInterfaceAddressConfig netInterfaceAddressConfig) {
        if (netInterfaceAddressConfig instanceof NetInterfaceAddressConfigImpl) {
            ((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(netConfigs);
            if (dhcp) {
                // obtain gateway provided by DHCP server
                List<? extends IPAddress> dhcpRouters = getDhcpRouters(interfaceName,
                        netInterfaceAddressConfig.getAddress());
                if (!dhcpRouters.isEmpty()) {
                    ((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig).setGateway(dhcpRouters.get(0));
                }

                // Replace with DNS provided by DHCP server (displayed as read-only in Denali)
                List<? extends IPAddress> dhcpDnsServers = getDhcpDnsServers(interfaceName,
                        netInterfaceAddressConfig.getAddress());
                if (!dhcpDnsServers.isEmpty()) {
                    ((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig).setDnsServers(dhcpDnsServers);
                }
            }
        } else if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
            ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(netConfigs);
            if (dhcp) {
                // obtain gateway provided by DHCP server
                List<? extends IPAddress> dhcpRouters = getDhcpRouters(interfaceName,
                        netInterfaceAddressConfig.getAddress());
                if (!dhcpRouters.isEmpty()) {
                    ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setGateway(dhcpRouters.get(0));
                }

                // Replace with DNS provided by DHCP server
                // (displayed as read-only in Denali)
                List<? extends IPAddress> dhcpDnsServers = getDhcpDnsServers(interfaceName,
                        netInterfaceAddressConfig.getAddress());
                if (!dhcpDnsServers.isEmpty()) {
                    ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setDnsServers(dhcpDnsServers);
                }
            }
        }
    }

    private void setNetConfigIP4(NetConfigIP4 netConfig, boolean dhcp, IP4Address address, String gateway,
            String prefixString, String netmask, Properties kuraProps) throws KuraException {

        netConfig.setDhcp(dhcp);
        if (kuraProps == null) {
            return;
        }
        netConfig.setDnsServers(getDnsServers(kuraProps));
        if (!dhcp) {
            netConfig.setAddress(address);
            if (gateway != null && !gateway.isEmpty()) {
                try {
                    netConfig.setGateway((IP4Address) IPAddress.parseHostAddress(gateway));
                } catch (UnknownHostException e) {
                    logger.error("Could not parse address: " + gateway, e);
                }
            }
            if (prefixString != null) {
                short prefix = Short.parseShort(prefixString);
                netConfig.setNetworkPrefixLength(prefix);
            }
            if (netmask != null) {
                netConfig.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(netmask));
            }
        }
    }

    // Get the DNS servers from kura properties
    private List<IP4Address> getDnsServers(Properties kuraProps) {
        List<IP4Address> dnsServers = new ArrayList<>();
        int count = 1;
        while (true) {
            String dns;
            if ((dns = kuraProps.getProperty("DNS" + count)) != null) {
                try {
                    dnsServers.add((IP4Address) IPAddress.parseHostAddress(dns));
                } catch (UnknownHostException e) {
                    logger.error("Could not parse address: {}", dns, e);
                }
                count++;
            } else {
                break;
            }
        }
        return dnsServers;
    }

    protected DhcpClientLeases getDhcpClientLeases() {
        return DhcpClientLeases.getInstance();
    }

    private List<? extends IPAddress> getDhcpRouters(String interfaceName, IPAddress address) {
        List<IPAddress> routers = null;
        if (address != null) {
            DhcpClientLeases dhcpClientLeases = getDhcpClientLeases();
            try {
                routers = dhcpClientLeases.getDhcpGateways(interfaceName, address);
            } catch (KuraException e) {
                logger.error("Error getting DHCP DNS servers", e);
            }
        }
        if (routers == null) {
            routers = new ArrayList<>();
        }
        return routers;
    }

    private List<? extends IPAddress> getDhcpDnsServers(String interfaceName, IPAddress address) {
        List<IPAddress> dnsServers = null;
        if (address != null) {
            LinuxDns linuxDns = LinuxDns.getInstance();
            try {
                dnsServers = linuxDns.getDhcpDnsServers(interfaceName, address);
            } catch (KuraException e) {
                logger.error("Error getting DHCP DNS servers", e);
            }
        }
        if (dnsServers == null) {
            dnsServers = new ArrayList<>();
        }
        return dnsServers;
    }
}
