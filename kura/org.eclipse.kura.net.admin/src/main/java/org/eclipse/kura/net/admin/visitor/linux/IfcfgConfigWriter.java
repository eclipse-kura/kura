/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.IfcfgConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigWriter extends IfcfgConfig implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(IfcfgConfigWriter.class);

    private static final String REDHAT_NET_CONFIGURATION_DIRECTORY = "/etc/sysconfig/network-scripts/";
    private static final String DEBIAN_NET_CONFIGURATION_FILE = "/etc/network/interfaces";
    private static final String DEBIAN_TMP_NET_CONFIGURATION_FILE = "/etc/network/interfaces.tmp";

    private static String osVersion = System.getProperty("kura.os.version");

    private static IfcfgConfigWriter instance;

    private static List<String> debianInterfaceComandOptions = new ArrayList<>(
            Arrays.asList("pre-up", "up", "post-up", "pre-down", "down", "post-down"));
    private static List<String> debianIgnoreInterfaceCommands = new ArrayList<>(
            Arrays.asList("post-up route del default dev"));

    public static synchronized IfcfgConfigWriter getInstance() {
        if (instance == null) {
            instance = new IfcfgConfigWriter();
        }

        return instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig != null) {
                if (((AbstractNetInterface<?>) netInterfaceConfig).isInterfaceManaged()) {
                    writeConfig(netInterfaceConfig);
                }
                writeKuraExtendedConfig(netInterfaceConfig);
            }
        }
    }

    private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {

        NetInterfaceType type = netInterfaceConfig.getType();
        if (type != NetInterfaceType.ETHERNET && type != NetInterfaceType.WIFI && type != NetInterfaceType.LOOPBACK) {
            logger.info("writeConfig() :: Cannot write configuration file for this type of interface - {}", type);
            return;
        }
        if (isDebian()) {
            if (configHasChanged(netInterfaceConfig)) {
                writeDebianConfig(netInterfaceConfig);
            }
        } else {
            writeRedhatConfig(netInterfaceConfig);
        }
    }

    private void writeRedhatConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        String interfaceName = netInterfaceConfig.getName();
        String outputFileName = new StringBuilder().append(getRHConfigDirectory()).append("/ifcfg-")
                .append(interfaceName).toString();
        String tmpOutputFileName = new StringBuilder().append(getRHConfigDirectory()).append("/ifcfg-")
                .append(interfaceName).append(".tmp").toString();
        logger.debug("Writing config for {}", interfaceName);

        StringBuilder sb = new StringBuilder();
        sb.append("# Networking Interface\n");

        // DEVICE
        sb.append("DEVICE=").append(netInterfaceConfig.getName()).append("\n");

        // NAME
        sb.append("NAME=").append(netInterfaceConfig.getName()).append("\n");

        // TYPE
        sb.append("TYPE=").append(netInterfaceConfig.getType()).append("\n");

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
                sb.append("ONBOOT=");
                if (((NetConfigIP4) netConfig).isAutoConnect()) {
                    sb.append("yes");
                } else {
                    sb.append("no");
                }
                sb.append("\n");
                if (((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusL2Only) {
                    logger.debug("new config is Layer 2 Only");
                    sb.append("BOOTPROTO=none\n");
                } else if (((NetConfigIP4) netConfig).isDhcp()) {
                    logger.debug("new config is DHCP");
                    sb.append("BOOTPROTO=dhcp\n");
                } else {
                    logger.debug("new config is STATIC");
                    sb.append("BOOTPROTO=static\n");

                    // IPADDR
                    sb.append("IPADDR=").append(((NetConfigIP4) netConfig).getAddress().getHostAddress()).append("\n");

                    // PREFIX
                    sb.append("PREFIX=").append(((NetConfigIP4) netConfig).getNetworkPrefixLength()).append("\n");

                    // Gateway
                    if (((NetConfigIP4) netConfig).getGateway() != null) {
                        sb.append("GATEWAY=").append(((NetConfigIP4) netConfig).getGateway().getHostAddress())
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

    private void writeDebianConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        StringBuilder sb = new StringBuilder();
        File kuraFile = new File(getFinalFile());
        String iName = netInterfaceConfig.getName();
        boolean appendConfig = true;

        if (!kuraFile.exists()) {
            logger.warn("writeDebianConfig() :: The {} file doesn't exist.", kuraFile.getName());
            return;
        }

        // found our match so load the properties
        try (FileInputStream fis = new FileInputStream(kuraFile); Scanner scanner = new Scanner(fis)) {
            // need to loop through the existing file and replace only the desired interface
            while (scanner.hasNextLine()) {
                String noTrimLine = scanner.nextLine();
                String line = noTrimLine.trim();
                // ignore comments and blank lines
                if (!line.isEmpty()) {
                    if (line.startsWith("#!kura!")) {
                        line = line.substring("#!kura!".length());
                    }

                    if (!line.startsWith("#")) {
                        String[] args = line.split("\\s+");
                        // must be a line stating that interface starts on boot
                        if (args.length > 1) {
                            if (args[1].equals(iName)) {
                                logger.debug("Found entry in interface file...");
                                appendConfig = false;
                                sb.append(debianWriteUtility(netInterfaceConfig, iName));

                                // append Debian interface command options
                                while (scanner.hasNextLine() && !(line = scanner.nextLine().trim()).isEmpty()) {
                                    if (isDebianInterfaceCommandOption(line)) {
                                        sb.append("\t").append(line).append("\n");
                                    }
                                }
                                if (!sb.toString().endsWith("\n\n")) {
                                    sb.append("\n");
                                }
                            } else {
                                sb.append(noTrimLine + "\n");
                            }
                        }
                    } else {
                        sb.append(noTrimLine + "\n");
                    }
                } else {
                    sb.append(noTrimLine + "\n");
                }
            }
        } catch (Exception e) {
            logger.error("Debian config file is not found", e);
            throw KuraException.internalError(e.getMessage());
        }

        // If config not present in file, append to end
        if (appendConfig) {
            logger.debug("Appending entry to interface file...");
            // append an empty line if not there
            String s = sb.toString();
            if (!"\\n".equals(s.substring(s.length() - 1))) {
                sb.append("\n");
            }
            sb.append(debianWriteUtility(netInterfaceConfig, iName));
            sb.append("\n");
        }

        // write configuration file
        writeConfigFile(getTemporaryFile(), getFinalFile(), sb);
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

    protected Properties getKuranetProperties() {
        return KuranetConfig.getProperties();
    }

    protected String getFinalFile() {
        return DEBIAN_NET_CONFIGURATION_FILE;
    }

    protected String getTemporaryFile() {
        return DEBIAN_TMP_NET_CONFIGURATION_FILE;
    }

    protected String getRHConfigDirectory() {
        return REDHAT_NET_CONFIGURATION_DIRECTORY;
    }

    private String debianWriteUtility(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig,
            String interfaceName) {
        StringBuilder sb = new StringBuilder();
        List<NetConfig> netConfigs = ((AbstractNetInterface<?>) netInterfaceConfig).getNetConfigs();
        if (netConfigs == null) {
            logger.debug("debianWriteUtility() :: netConfigs is null");
            return sb.toString();
        }
        for (NetConfig netConfig : netConfigs) {
            if (!(netConfig instanceof NetConfigIP4)) {
                continue;
            }
            logger.debug("Writing netconfig {} for {}", netConfig.getClass().toString(), interfaceName);

            // ONBOOT
            if (((NetConfigIP4) netConfig).isAutoConnect()) {
                sb.append("auto " + interfaceName + "\n");
            }

            // BOOTPROTO
            sb.append("iface " + interfaceName + " inet ");
            if (((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusL2Only) {
                logger.debug("new config is Layer 2 Only for {}", interfaceName);
                sb.append("manual\n");
            } else if (((NetConfigIP4) netConfig).isDhcp()) {
                logger.debug("new config is DHCP for {}", interfaceName);
                sb.append("dhcp\n");
                if (((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN) {
                    // delete default route if configured as LAN
                    sb.append("\tpost-up route del default dev ");
                    sb.append(interfaceName);
                    sb.append("\n");
                }
            } else {
                logger.debug("new config is STATIC for {}", interfaceName);
                sb.append("static\n");
                // IPADDR
                sb.append("\taddress ").append(((NetConfigIP4) netConfig).getAddress().getHostAddress()).append("\n");

                // NETMASK
                sb.append("\tnetmask ").append(((NetConfigIP4) netConfig).getSubnetMask().getHostAddress())
                        .append("\n");

                // NETWORK
                // TODO: Handle Debian NETWORK value

                // Gateway
                if (((NetConfigIP4) netConfig).getGateway() != null) {
                    sb.append("\tgateway ").append(((NetConfigIP4) netConfig).getGateway().getHostAddress())
                            .append("\n");
                }
            }

            // DNS
            List<? extends IPAddress> dnsAddresses = ((NetConfigIP4) netConfig).getDnsServers();
            boolean setDns = false;
            for (IPAddress dnsAddress : dnsAddresses) {
                if (LOCALHOST.equals(dnsAddress.getHostAddress())) {
                    continue;
                }
                if (!setDns) {
                    /*
                     * IAB:
                     * If DNS servers are listed, those entries will be appended to the
                     * /etc/resolv.conf
                     * file on every ifdown/ifup sequence resulting in multiple entries for the same
                     * servers.
                     * (Tested on 10-20, 10-10, and Raspberry Pi).
                     * Commenting out dns-nameservers in the /etc/network interfaces file allows DNS
                     * servers
                     * to be picked up by the IfcfgConfigReader and be displayed on the Web UI but
                     * the
                     * /etc/resolv.conf file will only be updated by Kura.
                     */
                    sb.append("\t#dns-nameservers ");
                    setDns = true;
                }
                sb.append(dnsAddress.getHostAddress() + " ");
            }
            if (!"\n".equals(sb.toString().substring(sb.toString().length() - 1))) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private boolean isDebianInterfaceCommandOption(String line) {
        boolean ret = false;
        for (String debIfaceCmdOp : debianInterfaceComandOptions) {
            if (line.startsWith(debIfaceCmdOp)) {
                ret = true;
                break;
            }
        }
        if (ret) {
            for (String debIgnoreIfaceCmd : debianIgnoreInterfaceCommands) {
                if (line.startsWith(debIgnoreIfaceCmd)) {
                    ret = false;
                    break;
                }
            }
        }
        return ret;
    }

    public static void writeKuraExtendedConfig(
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {

        NetInterfaceStatus netInterfaceStatus = ((AbstractNetInterface<?>) netInterfaceConfig).getInterfaceStatus();
        logger.debug("Setting NetInterfaceStatus to {} for {}", netInterfaceStatus, netInterfaceConfig.getName());

        // set it all
        Properties kuraExtendedProps = getInstance().getKuranetProperties();

        // write it
        if (!kuraExtendedProps.isEmpty()) {
            StringBuilder sb = new StringBuilder().append("net.interface.").append(netInterfaceConfig.getName())
                    .append(".config.ip4.status");
            kuraExtendedProps.put(sb.toString(), netInterfaceStatus.toString());
            try {
                KuranetConfig.storeProperties(kuraExtendedProps);
            } catch (IOException e) {
                logger.error("Failed to store properties in the kuranet.conf file.", e);
                throw KuraException.internalError(e.getMessage());
            }
        }
    }

    public static void removeKuraExtendedConfig(String interfaceName) throws KuraException {
        try {
            StringBuilder sb = new StringBuilder().append("net.interface.").append(interfaceName)
                    .append(".config.ip4.status");
            KuranetConfig.deleteProperty(sb.toString());
        } catch (IOException e) {
            logger.error("Failed to remove net.interface..config.ip4.status property from the kuranet.conf file.", e);
            throw KuraException.internalError(e.getMessage());
        }
    }

    private Properties parseNetInterfaceAddressConfig(NetInterfaceAddressConfig netInterfaceAddressConfig) {
        Properties props = new Properties();

        List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();

        if (netConfigs == null) {
            logger.debug("netConfigs is null");
            return props;
        }
        for (NetConfig netConfig : netConfigs) {
            if (!(netConfig instanceof NetConfigIP4)) {
                continue;
            }
            NetConfigIP4 netConfigIP4 = (NetConfigIP4) netConfig;

            // ONBOOT
            props.setProperty(ONBOOT_PROP_NAME, netConfigIP4.isAutoConnect() ? "yes" : "no");

            if (netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusL2Only) {
                props.setProperty(BOOTPROTO_PROP_NAME, "none");
            } else if (netConfigIP4.isDhcp()) {
                props.setProperty(BOOTPROTO_PROP_NAME, "dhcp");
                if (((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
                    props.setProperty(DEFROUTE_PROP_NAME, "yes");
                } else {
                    props.setProperty(DEFROUTE_PROP_NAME, "no");
                }
            } else {
                props.setProperty(BOOTPROTO_PROP_NAME, "static");
                // IPADDR
                if (netConfigIP4.getAddress() != null) {
                    props.setProperty(IPADDR_PROP_NAME, netConfigIP4.getAddress().getHostAddress());
                }

                // NETMASK
                if (netConfigIP4.getSubnetMask() != null) {
                    props.setProperty(NETMASK_PROP_NAME, netConfigIP4.getSubnetMask().getHostAddress());
                }

                // NETWORK
                // TODO: Handle Debian NETWORK value

                // GATEWAY
                if (netConfigIP4.getGateway() != null) {
                    props.setProperty(GATEWAY_PROP_NAME, netConfigIP4.getGateway().getHostAddress());
                    props.setProperty(DEFROUTE_PROP_NAME, "yes");
                } else {
                    props.setProperty(DEFROUTE_PROP_NAME, "no");
                }
            }

            // DNS
            List<? extends IPAddress> dnsAddresses = ((NetConfigIP4) netConfig).getDnsServers();
            for (int i = 0; i < dnsAddresses.size(); i++) {
                if (!LOCALHOST.equals(dnsAddresses.get(i).getHostAddress())) {
                    props.setProperty("DNS" + Integer.toString(i + 1), dnsAddresses.get(i).getHostAddress());
                }
            }
        }

        return props;
    }

    private boolean configHasChanged(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        Properties oldConfig = parseDebianConfigFile(new File(getFinalFile()), netInterfaceConfig.getName());

        // FIXME: assumes only one addressConfig
        Properties newConfig = parseNetInterfaceAddressConfig(
                ((AbstractNetInterface<?>) netInterfaceConfig).getNetInterfaceAddressConfig());
        logger.debug("Comparing configs for {}", netInterfaceConfig.getName());
        logger.debug("oldProps: {}", oldConfig);
        logger.debug("newProps: {}", newConfig);

        if (!compare(oldConfig, newConfig, "ONBOOT")) {
            logger.debug("ONBOOT differs");
            return true;
        } else if (!compare(oldConfig, newConfig, "BOOTPROTO")) {
            logger.debug("BOOTPROTO differs");
            return true;
        } else if (!compare(oldConfig, newConfig, "IPADDR")) {
            logger.debug("IPADDR differs");
            return true;
        } else if (!compare(oldConfig, newConfig, "NETMASK")) {
            logger.debug("NETMASK differs");
            return true;
        } else if (!compare(oldConfig, newConfig, "GATEWAY")) {
            logger.debug("GATEWAY differs");
            return true;
        } else if (!compare(oldConfig, newConfig, "DNS1")) {
            logger.debug("DNS1 differs");
            return true;
        } else if (!compare(oldConfig, newConfig, "DNS2")) {
            logger.debug("DNS2 differs");
            return true;
        } else if (!compare(oldConfig, newConfig, "DNS3")) {
            logger.debug("DNS3 differs");
            return true;
        } else if (!compare(oldConfig, newConfig, "DEFROUTE")) {
            logger.debug("DEFROUTE differs");
            return true;
        }

        logger.debug("Configs match");
        return false;
    }

    private boolean compare(Properties prop1, Properties prop2, String key) {
        String val1 = prop1.getProperty(key);
        String val2 = prop2.getProperty(key);

        return val1 != null ? val1.equals(val2) : val2 == null;
    }
}
