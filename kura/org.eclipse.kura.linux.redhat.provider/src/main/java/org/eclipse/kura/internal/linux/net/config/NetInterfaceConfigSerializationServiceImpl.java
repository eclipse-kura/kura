/*******************************************************************************
 * Copyright (c) 2018, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.linux.net.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.internal.linux.net.NetInterfaceConfigSerializationService;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetInterfaceConfigSerializationServiceImpl implements NetInterfaceConfigSerializationService {

    private static final Logger logger = LoggerFactory.getLogger(NetInterfaceConfigSerializationServiceImpl.class);

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

    @Override
    public Properties read(String interfaceName) throws KuraException {
        logger.debug("Getting config for {}", interfaceName);

        String ifcfgFileName = getIfcfgFileName(interfaceName);
        File ifcfgFile = new File(ifcfgFileName);
        if (!ifcfgFile.exists()) {
            logger.error("getConfig() :: The {} file doesn't exist", interfaceName);
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER);
        }

        return parseRedhatConfigFile(ifcfgFile, interfaceName);
    }

    private String getIfcfgFileName(String interfaceName) {
        String fileName = REDHAT_NET_CONFIGURATION_DIRECTORY;

        fileName += "/ifcfg-" + interfaceName;

        return fileName;
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

    @Override
    public void write(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
        String interfaceName = netInterfaceConfig.getName();
        NetConfigIP4 netConfigIP4 = ((AbstractNetInterface<?>) netInterfaceConfig).getIP4config();
        if (netConfigIP4 == null) {
            logger.warn("The configuration for interface {} in empty", interfaceName);
            return;
        }

        String outputFileName = getIfcfgFileName(interfaceName);
        String tmpOutputFileName = outputFileName + ".tmp";
        logger.debug("Writing config for {}", interfaceName);

        StringBuilder sb = new StringBuilder();
        sb.append("# Networking Interface\n");

        // DEVICE
        sb.append(DEVICE_PROP_NAME).append('=').append(netInterfaceConfig.getName()).append("\n");

        // NAME
        sb.append(NAME_PROP_NAME).append('=').append(netInterfaceConfig.getName()).append("\n");

        // TYPE
        sb.append(TYPE_PROP_NAME).append('=').append(netInterfaceConfig.getType()).append("\n");

        setOnBootProperty(netConfigIP4, sb);
        setBootprotoProperty(netConfigIP4, sb);
        setDefrouteProperty(netConfigIP4, sb);
        setDnsProperty(netConfigIP4, sb);
        setWifiProperties(netInterfaceConfig, sb);

        writeConfigFile(tmpOutputFileName, outputFileName, sb);
    }

    private void setWifiProperties(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig,
            StringBuilder sb) throws KuraException {
        // WIFI
        NetInterfaceAddressConfig netInterfaceAddressConfig = ((AbstractNetInterface<?>) netInterfaceConfig)
                .getNetInterfaceAddressConfig();
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
    }

    private void setDnsProperty(NetConfigIP4 netConfigIP4, StringBuilder sb) {
        // DNS
        List<? extends IPAddress> dnsAddresses = netConfigIP4.getDnsServers();
        if (dnsAddresses != null) {
            for (int i = 0; i < dnsAddresses.size(); i++) {
                IPAddress ipAddr = dnsAddresses.get(i);
                if (!(ipAddr.isLoopbackAddress() || ipAddr.isLinkLocalAddress() || ipAddr.isMulticastAddress())) {
                    sb.append("DNS").append(i + 1).append("=").append(ipAddr.getHostAddress()).append("\n");
                }
            }
        } else {
            logger.debug("no DNS entries");
        }
    }

    private void setDefrouteProperty(NetConfigIP4 netConfigIP4, StringBuilder sb) {
        // DEFROUTE
        if (netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
            sb.append(DEFROUTE_PROP_NAME).append("=yes\n");
        } else {
            sb.append(DEFROUTE_PROP_NAME).append("=no\n");
        }
    }

    private void setBootprotoProperty(NetConfigIP4 netConfigIP4, StringBuilder sb) {
        // BOOTPROTO
        if (netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusL2Only) {
            logger.debug("new config is Layer 2 Only");
            sb.append(BOOTPROTO_PROP_NAME).append("=none\n");
        } else if (netConfigIP4.isDhcp() || netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusDisabled) {
            logger.debug("new config is DHCP");
            sb.append(BOOTPROTO_PROP_NAME).append("=dhcp\n");
        } else {
            logger.debug("new config is STATIC");
            sb.append(BOOTPROTO_PROP_NAME).append("=static\n");

            // IPADDR
            sb.append(IPADDR_PROP_NAME).append('=').append(netConfigIP4.getAddress().getHostAddress()).append("\n");

            // PREFIX
            sb.append(PREFIX_PROP_NAME).append('=').append(netConfigIP4.getNetworkPrefixLength()).append("\n");

            // Gateway
            if (netConfigIP4.getGateway() != null) {
                sb.append(GATEWAY_PROP_NAME).append('=').append(netConfigIP4.getGateway().getHostAddress())
                        .append("\n");
            }
        }
    }

    private void setOnBootProperty(NetConfigIP4 netConfigIP4, StringBuilder sb) {
        // ONBOOT
        sb.append(ONBOOT_PROP_NAME).append('=');
        if ((netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN
                || netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN
                || netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusL2Only)
                && netConfigIP4.isAutoConnect()) {
            sb.append("yes");
        } else {
            sb.append("no");
        }
        sb.append("\n");
    }

    private void writeConfigFile(String tmpFileName, String dstFileName, StringBuilder sb) throws KuraException {
        File srcFile = new File(tmpFileName);
        File dstFile = new File(dstFileName);

        // write tmp configuration file
        try (FileOutputStream fos = new FileOutputStream(srcFile); PrintWriter pw = new PrintWriter(fos)) {
            pw.write(sb.toString());
            pw.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            throw new KuraIOException(e, "Failed to write redhat configuration file");
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
            throw new KuraIOException(e,
                    "Failed to rename tmp config file " + srcFile.getName() + " to " + dstFile.getName());
        }
    }
}
