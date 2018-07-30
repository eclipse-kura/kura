/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.linux.net.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dhcp.DhcpClientLeases;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigManager;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigManager implements NetConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(IfcfgConfigManager.class);

    protected static final String REDHAT_NET_CONFIGURATION_DIRECTORY = "/etc/sysconfig/network-scripts/";

    private static final String ONBOOT_PROP_NAME = "ONBOOT";
    private static final String BOOTPROTO_PROP_NAME = "BOOTPROTO";
    private static final String IPADDR_PROP_NAME = "IPADDR";
    private static final String PREFIX_PROP_NAME = "PREFIX";
    private static final String GATEWAY_PROP_NAME = "GATEWAY";
    private static final String DEFROUTE_PROP_NAME = "DEFROUTE";
    private static final String DEVICE_PROP_NAME = "DEVICE";
    private static final String NAME_PROP_NAME = "NAME";
    private static final String TYPE_PROP_NAME = "TYPE";
    private static final String NETMASK_PROP_NAME = "NETMASK";

    @Override
    public void readConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig,
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

        Properties kuraProps = parseRedhatConfigFile(ifcfgFile, interfaceName);
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

    private File getIfcfgFile(String interfaceName) {
        String fileName = REDHAT_NET_CONFIGURATION_DIRECTORY;

        fileName += "/ifcfg-" + interfaceName;

        return new File(fileName);
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
            autoConnect = "yes".equals(kuraProps.getProperty(ONBOOT_PROP_NAME));
            boolean defroute = "yes".equals(kuraProps.getProperty(DEFROUTE_PROP_NAME));

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

    public Properties parseRedhatConfigFile(File ifcfgFile, String interfaceName) {
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
            setNetInterfaceAddressConfigs(interfaceName, netConfigs, netInterfaceAddressConfig, dhcp);
        } else if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
            setWifiNetInterfaceAddressConfigs(interfaceName, netConfigs, netInterfaceAddressConfig, dhcp);
        }
    }

    private void setNetInterfaceAddressConfigs(String interfaceName, List<NetConfig> netConfigs,
            NetInterfaceAddressConfig netInterfaceAddressConfig, boolean dhcp) {
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
    }

    private void setWifiNetInterfaceAddressConfigs(String interfaceName, List<NetConfig> netConfigs,
            NetInterfaceAddressConfig netInterfaceAddressConfig, boolean dhcp) {
        ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(netConfigs);
        if (dhcp) {
            // obtain gateway provided by DHCP server
            List<? extends IPAddress> dhcpRouters = getDhcpRouters(interfaceName,
                    netInterfaceAddressConfig.getAddress());
            if (!dhcpRouters.isEmpty()) {
                ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setGateway(dhcpRouters.get(0));
            }
            // Replace with DNS provided by DHCP server (displayed as read-only in Denali)
            List<? extends IPAddress> dhcpDnsServers = getDhcpDnsServers(interfaceName,
                    netInterfaceAddressConfig.getAddress());
            if (!dhcpDnsServers.isEmpty()) {
                ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setDnsServers(dhcpDnsServers);
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

    private List<? extends IPAddress> getDhcpRouters(String interfaceName, IPAddress address) {
        List<IPAddress> routers = null;
        if (address != null) {
            DhcpClientLeases dhcpClientLeases = DhcpClientLeases.getInstance();
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

    @Override
    public void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {

        NetInterfaceType type = netInterfaceConfig.getType();
        if (type != NetInterfaceType.ETHERNET && type != NetInterfaceType.WIFI && type != NetInterfaceType.LOOPBACK) {
            logger.info("writeConfig() :: Cannot write configuration file for this type of interface - {}", type);
            return;
        }

        writeRedhatConfig(netInterfaceConfig);
    }

    private void writeRedhatConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        String interfaceName = netInterfaceConfig.getName();
        String outputFileName = new StringBuilder().append(REDHAT_NET_CONFIGURATION_DIRECTORY).append("/ifcfg-")
                .append(interfaceName).toString();
        String tmpOutputFileName = new StringBuilder().append(REDHAT_NET_CONFIGURATION_DIRECTORY).append("/ifcfg-")
                .append(interfaceName).append(".tmp").toString();
        logger.debug("Writing config for {}", interfaceName);

        StringBuilder sb = new StringBuilder();
        sb.append("# Networking Interface\n");

        // DEVICE
        sb.append(DEVICE_PROP_NAME).append('=').append(netInterfaceConfig.getName()).append("\n");

        // NAME
        sb.append(NAME_PROP_NAME).append('=').append(netInterfaceConfig.getName()).append("\n");

        // TYPE
        sb.append(TYPE_PROP_NAME).append('=').append(netInterfaceConfig.getType()).append("\n");

        NetInterfaceAddressConfig netInterfaceAddressConfig = ((AbstractNetInterface<?>) netInterfaceConfig)
                .getNetInterfaceAddressConfig();
        boolean allowWrite = false;
        List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (!(netConfig instanceof NetConfigIP4)) {
                    continue;
                }
                // ONBOOT
                sb.append(ONBOOT_PROP_NAME).append('=');
                if (((NetConfigIP4) netConfig).isAutoConnect()) {
                    sb.append("yes");
                } else {
                    sb.append("no");
                }
                sb.append("\n");
                if (((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusL2Only) {
                    logger.debug("new config is Layer 2 Only");
                    sb.append(BOOTPROTO_PROP_NAME).append("=none\n");
                } else if (((NetConfigIP4) netConfig).isDhcp()) {
                    logger.debug("new config is DHCP");
                    sb.append(BOOTPROTO_PROP_NAME).append("=dhcp\n");
                } else {
                    logger.debug("new config is STATIC");
                    sb.append(BOOTPROTO_PROP_NAME).append("=static\n");

                    // IPADDR
                    sb.append(IPADDR_PROP_NAME).append('=')
                            .append(((NetConfigIP4) netConfig).getAddress().getHostAddress()).append("\n");

                    // PREFIX
                    sb.append(PREFIX_PROP_NAME).append('=').append(((NetConfigIP4) netConfig).getNetworkPrefixLength())
                            .append("\n");

                    // Gateway
                    if (((NetConfigIP4) netConfig).getGateway() != null) {
                        sb.append(GATEWAY_PROP_NAME).append('=')
                                .append(((NetConfigIP4) netConfig).getGateway().getHostAddress()).append("\n");
                    }
                }

                // DEFROUTE
                if (((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
                    sb.append(DEFROUTE_PROP_NAME).append("=yes\n");
                } else {
                    sb.append(DEFROUTE_PROP_NAME).append("=no\n");
                }

                // DNS
                List<? extends IPAddress> dnsAddresses = ((NetConfigIP4) netConfig).getDnsServers();
                if (dnsAddresses != null) {
                    for (int i = 0; i < dnsAddresses.size(); i++) {
                        IPAddress ipAddr = dnsAddresses.get(i);
                        if (!(ipAddr.isLoopbackAddress() || ipAddr.isLinkLocalAddress()
                                || ipAddr.isMulticastAddress())) {
                            sb.append("DNS").append(i + 1).append("=").append(ipAddr.getHostAddress()).append("\n");
                        }
                    }
                } else {
                    logger.debug("no DNS entries");
                }

                allowWrite = true;
            }
        } else {
            logger.debug("writeRedhatConfig() :: netConfigs is null");
        }

        // WIFI
        if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfig) {
            logger.debug("new config is a WifiInterfaceAddressConfig");
            sb.append("\n#Wireless configuration\n");

            // MODE
            String mode;
            WifiMode wifiMode = ((WifiInterfaceAddressConfig) netInterfaceAddressConfig).getMode();
            if (wifiMode == WifiMode.INFRA) {
                mode = "Managed";
            } else if (wifiMode == WifiMode.MASTER) {
                mode = "Master";
            } else if (wifiMode == WifiMode.ADHOC) {
                mode = "Ad-Hoc";
            } else if (wifiMode == null) {
                logger.error("WifiMode is null");
                mode = "null";
            } else {
                mode = wifiMode.toString();
            }
            sb.append("MODE=").append(mode).append("\n");
        }

        if (allowWrite) {
            // write configuration file
            writeConfigFile(tmpOutputFileName, outputFileName, sb);
        } else {
            logger.warn("writeNewConfig :: operation is not allowed");
        }
    }

    private void writeConfigFile(String tmpFileName, String dstFileName, StringBuilder sb) throws KuraException {
        File srcFile = new File(tmpFileName);
        File dstFile = new File(dstFileName);

        // write tmp configuration file
        try (FileOutputStream fos = new FileOutputStream(srcFile); PrintWriter pw = new PrintWriter(fos)) {
            pw.write(sb.toString());
            pw.flush();
            fos.getFD().sync();
        } catch (Exception e) {
            logger.error("Failed to write debian configuration file", e);
            throw KuraException.internalError(e.getMessage());
        }

        // move tmp configuration file into its final destination
        copyConfigFile(srcFile, dstFile);
    }

    private void copyConfigFile(File srcFile, File dstFile) throws KuraException {
        try {
            if (!FileUtils.contentEquals(srcFile, dstFile)) {
                // File.renameTo performs rather badly on Windows, if the file already exists
                Files.move(Paths.get(srcFile.getAbsolutePath()), Paths.get(dstFile.getAbsolutePath()),
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                logger.info("Not rewriting network interfaces file because it is the same");
            }
        } catch (IOException e) {
            logger.error("Failed to rename tmp config file {} to {}", srcFile.getName(), dstFile.getName(), e);
            throw KuraException.internalError(e.getMessage());
        }
    }
}
