/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates and others
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigReader implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(IfcfgConfigReader.class);

    private static final String REDHAT_NET_CONFIGURATION_DIRECTORY = "/etc/sysconfig/network-scripts/";
    private static final String DEBIAN_NET_CONFIGURATION_DIRECTORY = "/etc/network/";

    private static String OS_VERSION = System.getProperty("kura.os.version");

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

        NetInterfaceType type = netInterfaceConfig.getType();
        if (type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI || type == NetInterfaceType.LOOPBACK) {

            NetInterfaceStatus netInterfaceStatus;

            StringBuilder sb = new StringBuilder().append("net.interface.").append(netInterfaceConfig.getName())
                    .append(".config.ip4.status");
            if (kuraExtendedProps != null && kuraExtendedProps.getProperty(sb.toString()) != null) {
                netInterfaceStatus = NetInterfaceStatus.valueOf(kuraExtendedProps.getProperty(sb.toString()));
            } else {
                netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
            }
            logger.debug("Setting NetInterfaceStatus to {} for {}", netInterfaceStatus, netInterfaceConfig.getName());

            boolean autoConnect = false;
            // int mtu = -1; // MTU is not currently used
            boolean dhcp = false;
            IP4Address address = null;
            String ipAddress = null;
            String prefixString = null;
            String netmask = null;
            String gateway = null;

            File ifcfgFile = getIfcfgFile(interfaceName);

            if (ifcfgFile.exists()) {
                Properties kuraProps;
                // found our match so load the properties
                if (isDebian()) {
                    kuraProps = parseDebianConfigFile(ifcfgFile, interfaceName);
                } else {
                    kuraProps = parseRedhatConfigFile(ifcfgFile, interfaceName);
                }

                if (kuraProps != null) {
                    String onBoot = kuraProps.getProperty("ONBOOT");
                    if ("yes".equals(onBoot)) {
                        logger.debug("Setting autoConnect to true");
                        autoConnect = true;
                    } else {
                        logger.debug("Setting autoConnect to false");
                        autoConnect = false;
                    }

                    // override MTU with what is in config if it is present
                    /*
                     * IAB: MTU is not currently used
                     * String stringMtu = kuraProps.getProperty("MTU");
                     * if (stringMtu == null) {
                     * try {
                     * mtu = LinuxNetworkUtil.getCurrentMtu(interfaceName);
                     * } catch (KuraException e) {
                     * // just assume ???
                     * if (interfaceName.equals("lo")) {
                     * mtu = 16436;
                     * } else {
                     * mtu = 1500;
                     * }
                     * }
                     * } else {
                     * mtu = Short.parseShort(stringMtu);
                     * }
                     */
                    // get the bootproto
                    String bootproto = kuraProps.getProperty("BOOTPROTO");
                    if (bootproto == null) {
                        bootproto = "static";
                    }

                    // get the defroute
                    String defroute = kuraProps.getProperty("DEFROUTE");
                    if (defroute == null) {
                        defroute = "no";
                    }

                    // correct the status if needed by validating against the
                    // actual properties
                    if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusDisabled) {
                        if (autoConnect) {
                            if ("no".equals(defroute)) {
                                netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledLAN;
                            } else {
                                netInterfaceStatus = NetInterfaceStatus.netIPv4StatusEnabledWAN;
                            }
                        }
                    }

                    // check for dhcp or static configuration
                    try {
                        ipAddress = kuraProps.getProperty("IPADDR");
                        prefixString = kuraProps.getProperty("PREFIX");
                        netmask = kuraProps.getProperty("NETMASK");
                        kuraProps.getProperty("BROADCAST");
                        try {
                            gateway = kuraProps.getProperty("GATEWAY");
                            logger.debug("got gateway for {}: {}", interfaceName, gateway);
                        } catch (Exception e) {
                            logger.warn("missing gateway stanza for {}", interfaceName);
                        }

                        if ("dhcp".equals(bootproto)) {
                            logger.debug("currently set for DHCP");
                            dhcp = true;
                            ipAddress = null;
                            netmask = null;
                        } else {
                            logger.debug("currently set for static address");
                            dhcp = false;
                        }
                    } catch (Exception e) {
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
                                "malformatted config file: " + ifcfgFile.toString(), e);
                    }

                    if (ipAddress != null && !ipAddress.isEmpty()) {
                        try {
                            address = (IP4Address) IPAddress.parseHostAddress(ipAddress);
                        } catch (UnknownHostException e) {
                            logger.warn("Error parsing address: " + ipAddress, e);
                        }
                    }

                    // make sure at least prefix or netmask is present if static
                    if (autoConnect && !dhcp && prefixString == null && netmask == null) {
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "malformatted config file: "
                                + ifcfgFile.toString() + " must contain NETMASK and/or PREFIX");
                    }
                }

                List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                        .getNetInterfaceAddresses();

                if (netInterfaceAddressConfigs == null) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "InterfaceAddressConfig list is null");
                } else if (netInterfaceAddressConfigs.isEmpty()) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "InterfaceAddressConfig list has no entries");
                }

                for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
                    List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();

                    if (netConfigs == null) {
                        netConfigs = new ArrayList<>();
                        if (netInterfaceAddressConfig instanceof NetInterfaceAddressConfigImpl) {
                            ((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(netConfigs);
                            if (dhcp) {
                                // Replace with DNS provided by DHCP server
                                // (displayed as read-only in Denali)
                                List<? extends IPAddress> dhcpDnsServers = getDhcpDnsServers(interfaceName,
                                        netInterfaceAddressConfig.getAddress());
                                if (dhcpDnsServers != null) {
                                    ((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig)
                                            .setDnsServers(dhcpDnsServers);
                                }
                            }
                        } else if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
                            ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(netConfigs);
                            if (dhcp) {
                                // Replace with DNS provided by DHCP server
                                // (displayed as read-only in Denali)
                                List<? extends IPAddress> dhcpDnsServers = getDhcpDnsServers(interfaceName,
                                        netInterfaceAddressConfig.getAddress());
                                if (dhcpDnsServers != null) {
                                    ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig)
                                            .setDnsServers(dhcpDnsServers);
                                }
                            }
                        }
                    }

                    NetConfigIP4 netConfig = new NetConfigIP4(netInterfaceStatus, autoConnect);
                    setNetConfigIP4(netConfig, autoConnect, dhcp, address, gateway, prefixString, netmask, kuraProps);
                    logger.debug("NetConfig: {}", netConfig);
                    netConfigs.add(netConfig);
                }
            }
        }
    }

    private boolean isDebian() {
        return OS_VERSION
                .equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion())
                || OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())
                || OS_VERSION.equals(KuraConstants.BeagleBone.getImageName())
                || OS_VERSION.equals(
                        KuraConstants.Intel_Edison.getImageName() + "_" + KuraConstants.Intel_Edison.getImageVersion()
                                + "_" + KuraConstants.Intel_Edison.getTargetName())
                || OS_VERSION.equals(KuraConstants.ReliaGATE_50_21_Ubuntu.getImageName() + "_"
                        + KuraConstants.ReliaGATE_50_21_Ubuntu.getImageVersion());
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

    private Properties parseRedhatConfigFile(File ifcfgFile, String interfaceName) {
        Properties kuraProps = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(ifcfgFile);
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
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    logger.error("I/O Exception while closing BufferedReader!", ex);
                }
            }
        }
        return kuraProps;
    }

    static Properties parseDebianConfigFile(File ifcfgFile, String interfaceName) throws KuraException {
        Properties kuraProps = new Properties();
        try (Scanner scanner = new Scanner(new FileInputStream(ifcfgFile))) {

            // Debian specific routine to create Properties object
            kuraProps.setProperty("ONBOOT", "no");

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                // ignore comments and blank lines
                if (!line.isEmpty()) {
                    if (line.startsWith("#!kura!")) {
                        line = line.substring("#!kura!".length());
                    }
                    if (!line.startsWith("#")) {
                        String[] args = line.split("\\s+");
                        try {
                            // must be a line stating that interface starts on
                            // boot
                            if ("auto".equals(args[0]) && args[1].equals(interfaceName)) {
                                logger.debug("Setting ONBOOT to yes for {}", interfaceName);
                                kuraProps.setProperty("ONBOOT", "yes");
                            }
                            // once the correct interface is found, read all
                            // configuration information
                            else if ("iface".equals(args[0]) && args[1].equals(interfaceName)) {
                                kuraProps.setProperty("BOOTPROTO", args[3]);
                                if ("dhcp".equals(args[3])) {
                                    kuraProps.setProperty("DEFROUTE", "yes");
                                }
                                while (scanner.hasNextLine()) {
                                    line = scanner.nextLine().trim();
                                    if (line != null && !line.isEmpty()) {
                                        if (line.startsWith("auto") || line.startsWith("iface")) {
                                            break;
                                        }

                                        args = line.trim().split("\\s+");
                                        if ("mtu".equals(args[0])) {
                                            kuraProps.setProperty("mtu", args[1]);
                                        } else if ("address".equals(args[0])) {
                                            kuraProps.setProperty("IPADDR", args[1]);
                                        } else if ("netmask".equals(args[0])) {
                                            kuraProps.setProperty("NETMASK", args[1]);
                                        } else if ("gateway".equals(args[0])) {
                                            kuraProps.setProperty("GATEWAY", args[1]);
                                            kuraProps.setProperty("DEFROUTE", "yes");
                                        } else if ("#dns-nameservers".equals(args[0])) {
                                            /*
                                             * IAB:
                                             * If DNS servers are listed,
                                             * those entries will be appended to
                                             * the /etc/resolv.conf file on
                                             * every ifdown/ifup sequence
                                             * resulting in multiple entries for
                                             * the same servers. (Tested on
                                             * 10-20, 10-10, and Raspberry Pi).
                                             * Commenting out dns-nameservers in
                                             * the /etc/network interfaces file
                                             * allows DNS servers to be picked
                                             * up by the IfcfgConfigReader and
                                             * be displayed on the Web UI but
                                             * the /etc/resolv.conf file will
                                             * only be updated by Kura.
                                             */
                                            if (args.length > 1) {
                                                for (int i = 1; i < args.length; i++) {
                                                    kuraProps.setProperty("DNS" + Integer.toString(i), args[i]);
                                                }
                                            }
                                        } else if ("post-up".equals(args[0])) {
                                            StringBuilder sb = new StringBuilder();
                                            for (int i = 1; i < args.length; i++) {
                                                sb.append(args[i]);
                                                sb.append(' ');
                                            }
                                            if (sb.toString().trim().equals("route del default dev " + interfaceName)) {
                                                kuraProps.setProperty("DEFROUTE", "no");
                                            }
                                        }
                                    }
                                }
                                // Debian makes assumptions about lo, handle
                                // those here
                                if ("lo".equals(interfaceName) && kuraProps.getProperty("IPADDR") == null
                                        && kuraProps.getProperty("NETMASK") == null) {
                                    kuraProps.setProperty("IPADDR", "127.0.0.1");
                                    kuraProps.setProperty("NETMASK", "255.0.0.0");
                                }
                                break;
                            }
                        } catch (Exception e) {
                            logger.warn("Possible malformed configuration file for " + interfaceName, e);
                        }
                    }
                }
            }

        } catch (FileNotFoundException err) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, err);
        }
        return kuraProps;
    }

    private static void setNetConfigIP4(NetConfigIP4 netConfig, boolean autoConnect, boolean dhcp, IP4Address address,
            String gateway, String prefixString, String netmask, Properties kuraProps) throws KuraException {

        netConfig.setDhcp(dhcp);
        if (kuraProps != null) {
            // get the DNS
            List<IP4Address> dnsServers = new ArrayList<>();
            int count = 1;
            while (true) {
                String dns;
                if ((dns = kuraProps.getProperty("DNS" + count)) != null) {
                    try {
                        dnsServers.add((IP4Address) IPAddress.parseHostAddress(dns));
                    } catch (UnknownHostException e) {
                        logger.error("Could not parse address: " + dns, e);
                    }
                    count++;
                } else {
                    break;
                }
            }
            netConfig.setDnsServers(dnsServers);

            if (!dhcp) {
                netConfig.setAddress(address);
                // TODO ((NetConfigIP4)netConfig).setDomains(domains);
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
                // TODO netConfig.setWinsServers(winsServers);
            }
        }
    }

    private static List<? extends IPAddress> getDhcpDnsServers(String interfaceName, IPAddress address) {
        List<IPAddress> dnsServers = null;

        if (address != null) {
            LinuxDns linuxDns = LinuxDns.getInstance();
            try {
                dnsServers = linuxDns.getDhcpDnsServers(interfaceName, address);
            } catch (KuraException e) {
                logger.error("Error getting DHCP DNS servers", e);
            }
        }

        return dnsServers;
    }
}
