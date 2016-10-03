/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedHatNetworkInterface extends GenericNetworkInterface {

    private static final Logger s_logger = LoggerFactory.getLogger(RedHatNetworkInterface.class);

    public static NetInterfaceConfig getCurrentConfiguration(String interfaceName, NetInterfaceType type,
            NetInterfaceStatus status, boolean dhcpServerEnabled, boolean passDns) throws KuraException {
        NET_CONFIGURATION_DIRECTORY = "/etc/sysconfig/network-scripts/";

        NetInterfaceConfig netInterfaceConfig = null;

        FileInputStream fis = null;
        try {
            // build up the configuration
            Properties kuraProps = new Properties();

            kuraFile = new File(NET_CONFIGURATION_DIRECTORY + "ifcfg-" + interfaceName);
            if (type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI
                    || type == NetInterfaceType.LOOPBACK) {
                if (kuraFile.exists()) {
                    // found our match so load the properties
                    fis = new FileInputStream(kuraFile);
                    kuraProps.load(fis);

                    s_logger.debug("getting args for {}", interfaceName);

                    netInterfaceConfig = getCurrentConfig(interfaceName, type, status, dhcpServerEnabled, passDns,
                            kuraProps);
                } else {
                    netInterfaceConfig = getCurrentConfig(interfaceName, type, NetInterfaceStatus.netIPv4StatusDisabled,
                            dhcpServerEnabled, passDns, null);
                }
            } else if (type == NetInterfaceType.MODEM) {
                s_logger.debug("getting args for {}", interfaceName);
                kuraProps.setProperty("BOOTPROTO", "dhcp");
                kuraProps.setProperty("ONBOOT", "yes");
                netInterfaceConfig = getCurrentConfig(interfaceName, type, status, dhcpServerEnabled, passDns,
                        kuraProps);
            } else {
                s_logger.error("Unsupported type: " + type.toString() + " for network interface: " + interfaceName);
            }

        } catch (Exception e) {
            s_logger.error("Error getting configuration for interface: " + interfaceName, e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    s_logger.error("I/O Exception while closing FileInputStream!");
                }
            }
        }
        return netInterfaceConfig;
    }

    public static void writeNewConfig(NetInterfaceConfig netInterfaceConfig) throws KuraException {
        try {
            String outputFileName = "/etc/sysconfig/network-scripts/ifcfg-" + netInterfaceConfig.getName();

            StringBuffer sb = new StringBuffer();
            sb.append("# Networking Interface\n");

            // DEVICE
            sb.append("DEVICE=").append(netInterfaceConfig.getName()).append("\n");

            // NAME
            sb.append("NAME=").append(netInterfaceConfig.getName()).append("\n");

            // TYPE
            sb.append("TYPE=").append(netInterfaceConfig.getType()).append("\n");

            List<? extends NetInterfaceAddressConfig> netInterfaceConfigs = netInterfaceConfig
                    .getNetInterfaceAddresses();
            s_logger.debug("There are {} NetInterfaceConfigs in this configuration", netInterfaceConfigs.size());

            boolean allowWrite = false;
            for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceConfigs) {
                List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();

                if (netConfigs != null) {
                    for (NetConfig netConfig : netConfigs) {
                        if (netConfig instanceof NetConfigIP4) {
                            // ONBOOT
                            sb.append("ONBOOT=");
                            if (((NetConfigIP4) netConfig).isAutoConnect()) {
                                sb.append("yes");
                            } else {
                                sb.append("no");
                            }
                            sb.append("\n");

                            if (((NetConfigIP4) netConfig).isDhcp()) {
                                // BOOTPROTO
                                sb.append("BOOTPROTO=");
                                s_logger.debug("new config is DHCP");
                                sb.append("dhcp");
                                sb.append("\n");
                            } else {
                                // BOOTPROTO
                                sb.append("BOOTPROTO=");
                                s_logger.debug("new config is STATIC");
                                sb.append("static");
                                sb.append("\n");

                                // IPADDR
                                sb.append("IPADDR=").append(((NetConfigIP4) netConfig).getAddress().getHostAddress())
                                        .append("\n");

                                // PREFIX
                                sb.append("PREFIX=").append(((NetConfigIP4) netConfig).getNetworkPrefixLength())
                                        .append("\n");

                                // TODO - do we need to deal with Netmask and Broadcast here???

                                // Gateway
                                if (((NetConfigIP4) netConfig).getGateway() != null) {
                                    sb.append("GATEWAY=")
                                            .append(((NetConfigIP4) netConfig).getGateway().getHostAddress())
                                            .append("\n");
                                }
                            }

                            // DEFROUTE
                            if (((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
                                sb.append("DEFROUTE=yes\n");
                            } else {
                                sb.append("DEFROUTE=no\n");
                            }

                            // DNS
                            List<? extends IPAddress> dnsAddresses = ((NetConfigIP4) netConfig).getDnsServers();
                            for (int i = 0; i < dnsAddresses.size(); i++) {
                                IPAddress ipAddr = dnsAddresses.get(i);
                                if (!(ipAddr.isLoopbackAddress() || ipAddr.isLinkLocalAddress()
                                        || ipAddr.isMulticastAddress())) {
                                    sb.append("DNS").append(i + 1).append("=").append(ipAddr.getHostAddress())
                                            .append("\n");
                                }
                            }

                            allowWrite = true;
                        }
                    }
                } else {
                    s_logger.debug("netConfigs is null");
                }

                // WIFI
                if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfig) {
                    s_logger.debug("new config is a WifiInterfaceAddressConfig");
                    sb.append("\n#Wireless configuration\n");

                    // MODE
                    String mode = null;
                    WifiMode wifiMode = ((WifiInterfaceAddressConfig) netInterfaceAddressConfig).getMode();
                    if (wifiMode == WifiMode.INFRA) {
                        mode = "Managed";
                    } else if (wifiMode == WifiMode.MASTER) {
                        mode = "Master";
                    } else if (wifiMode == WifiMode.ADHOC) {
                        mode = "Ad-Hoc";
                    } else {
                        mode = wifiMode.toString();
                    }
                    sb.append("MODE=").append(mode).append("\n");
                }
            }

            if (allowWrite) {
                FileOutputStream fos = new FileOutputStream(outputFileName);
                PrintWriter pw = new PrintWriter(fos);
                pw.write(sb.toString());
                pw.flush();
                fos.getFD().sync();
                pw.close();
                fos.close();
            } else {
                s_logger.warn("writeNewConfig :: operation is not allowed");
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }
}
