/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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
import java.io.FileNotFoundException;
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
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
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
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(IfcfgConfigWriter.class);

    private static final String REDHAT_NET_CONFIGURATION_DIRECTORY = "/etc/sysconfig/network-scripts/";
    private static final String DEBIAN_NET_CONFIGURATION_FILE = "/etc/network/interfaces";
    private static final String DEBIAN_TMP_NET_CONFIGURATION_FILE = "/etc/network/interfaces.tmp";

    private static final String LOCALHOST = "127.0.0.1";

    private static String OS_VERSION = System.getProperty("kura.os.version");

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

        if (!netInterfaceConfigs.isEmpty()) {
            for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
                if (getInterfaceStatus(netInterfaceConfig) != NetInterfaceStatus.netIPv4StatusUnmanaged) {
                    writeConfig(netInterfaceConfig);
                }
                writeKuraExtendedConfig(netInterfaceConfig);
            }
        }
    }

    private NetInterfaceStatus getInterfaceStatus(
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {
        if (netInterfaceConfig == null) {
            return NetInterfaceStatus.netIPv4StatusUnknown;
        }
        NetInterfaceStatus status = NetInterfaceStatus.netIPv4StatusUnknown;
        for (NetInterfaceAddressConfig addresses : netInterfaceConfig.getNetInterfaceAddresses()) {
            if (addresses != null) {
                List<NetConfig> netConfigs = addresses.getConfigs();
                if (netConfigs != null) {
                    for (NetConfig netConfig : netConfigs) {
                        if (netConfig instanceof NetConfigIP4) {
                            status = ((NetConfigIP4) netConfig).getStatus();
                        }
                    }
                }
            }
        }
        return status;
    }

    private void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        if (isDebian()) {
            NetInterfaceType type = netInterfaceConfig.getType();
            if ((type == NetInterfaceType.LOOPBACK || type == NetInterfaceType.ETHERNET
                    || type == NetInterfaceType.WIFI) && configHasChanged(netInterfaceConfig)) {
                writeDebianConfig(netInterfaceConfig);

            }
        } else {
            writeRedhatConfig(netInterfaceConfig);
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

    private void writeRedhatConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        String interfaceName = netInterfaceConfig.getName();
        String outputFileName = new StringBuilder().append(getRHConfigDirectory()).append("/ifcfg-")
                .append(interfaceName).toString();
        String tmpOutputFileName = new StringBuilder().append(getRHConfigDirectory()).append("/ifcfg-")
                .append(interfaceName).append(".tmp").toString();
        logger.debug("Writing config for {}", interfaceName);

        NetInterfaceType type = netInterfaceConfig.getType();
        if (type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI || type == NetInterfaceType.LOOPBACK) {
            StringBuilder sb = new StringBuilder();
            sb.append("# Networking Interface\n");

            // DEVICE
            sb.append("DEVICE=").append(netInterfaceConfig.getName()).append("\n");

            // NAME
            sb.append("NAME=").append(netInterfaceConfig.getName()).append("\n");

            // TYPE
            sb.append("TYPE=").append(netInterfaceConfig.getType()).append("\n");

            List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                    .getNetInterfaceAddresses();
            logger.debug("There are {} NetInterfaceConfigs in this configuration", netInterfaceAddressConfigs.size());

            boolean allowWrite = false;
            for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
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
                                logger.debug("new config is DHCP");
                                sb.append("dhcp");
                                sb.append("\n");
                            } else {
                                // BOOTPROTO
                                sb.append("BOOTPROTO=");
                                logger.debug("new config is STATIC");
                                sb.append("static");
                                sb.append("\n");

                                // IPADDR
                                sb.append("IPADDR=").append(((NetConfigIP4) netConfig).getAddress().getHostAddress())
                                        .append("\n");

                                // PREFIX
                                sb.append("PREFIX=").append(((NetConfigIP4) netConfig).getNetworkPrefixLength())
                                        .append("\n");

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
                            if (dnsAddresses != null) {
                                for (int i = 0; i < dnsAddresses.size(); i++) {
                                    IPAddress ipAddr = dnsAddresses.get(i);
                                    if (!(ipAddr.isLoopbackAddress() || ipAddr.isLinkLocalAddress()
                                            || ipAddr.isMulticastAddress())) {
                                        sb.append("DNS").append(i + 1).append("=").append(ipAddr.getHostAddress())
                                                .append("\n");
                                    }
                                }
                            } else {
                                logger.debug("no DNS entries");
                            }

                            allowWrite = true;
                        }
                    }
                } else {
                    logger.debug("writeRedhatConfig() :: netConfigs is null");
                }

                // WIFI
                if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfig) {
                    logger.debug("new config is a WifiInterfaceAddressConfig");
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
                    } else if (wifiMode == null) {
                        logger.error("WifiMode is null");
                        mode = "null";
                    } else {
                        mode = wifiMode.toString();
                    }
                    sb.append("MODE=").append(mode).append("\n");
                }
            }

            if (allowWrite) {
                FileOutputStream fos = null;
                PrintWriter pw = null;
                try {
                    fos = new FileOutputStream(tmpOutputFileName);
                    pw = new PrintWriter(fos);
                    pw.write(sb.toString());
                    pw.flush();
                    fos.getFD().sync();
                } catch (Exception e) {
                    logger.error("Failed to write redhat config file", e);
                    throw KuraException.internalError(e.getMessage());
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException ex) {
                            logger.error("I/O Exception while closing BufferedReader!", ex);
                        }
                    }
                    if (pw != null) {
                        pw.close();
                    }
                }

                // move the file if we made it this far
                File tmpFile = new File(tmpOutputFileName);
                File outputFile = new File(outputFileName);
                try {
                    if (!FileUtils.contentEquals(tmpFile, outputFile)) {
                        if (tmpFile.renameTo(outputFile)) {
                            logger.trace("Successfully wrote network interface file for {}", interfaceName);
                        } else {
                            logger.error("Failed to write network interface file");
                            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                                    "error while building up new configuration file for network interface "
                                            + interfaceName);
                        }
                    } else {
                        logger.info("Not rewriting network interfaces file for {} because it is the same",
                                interfaceName);
                    }
                } catch (IOException e) {
                    logger.error("Failed to rename redhat configuration file {} to {} ", tmpFile.getName(),
                            outputFile.getName(), e);
                    throw KuraException.internalError(e.getMessage());
                }
            } else {
                logger.warn("writeNewConfig :: operation is not allowed");
            }
        }
    }

    private void writeDebianConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        StringBuilder sb = new StringBuilder();
        File kuraFile = new File(getFinalFile());
        String iName = netInterfaceConfig.getName();
        boolean appendConfig = true;

        if (kuraFile.exists()) {
            // found our match so load the properties
            Scanner scanner = null;
            try {
                scanner = new Scanner(new FileInputStream(kuraFile));

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

                                    // remove old config lines from the scanner
                                    while (scanner.hasNextLine() && !(line = scanner.nextLine().trim()).isEmpty()) {
                                        if (isDebianInterfaceCommandOption(line)) {
                                            sb.append("\t").append(line).append("\n");
                                        }
                                    }
                                    sb.append("\n");
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
            } catch (FileNotFoundException e1) {
                logger.error("Debian config file is not found", e1);
                throw KuraException.internalError(e1.getMessage());
            } finally {
                scanner.close();
                scanner = null;
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

            FileOutputStream fos = null;
            PrintWriter pw = null;
            try {
                fos = new FileOutputStream(getTemporaryFile());
                pw = new PrintWriter(fos);
                pw.write(sb.toString());
                pw.flush();
                fos.getFD().sync();
            } catch (Exception e) {
                logger.error("Failed to write debian configuration file", e);
                throw KuraException.internalError(e.getMessage());
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ex) {
                        logger.error("I/O Exception while closing BufferedReader!", ex);
                    }
                }
                if (pw != null) {
                    pw.close();
                }
            }

            // move the file if we made it this far
            File tmpFile = new File(getTemporaryFile());
            File file = new File(getFinalFile());
            try {
                if (!FileUtils.contentEquals(tmpFile, file)) {
                    try {
                        // File.renameTo performs rather badly on Windows, if the file already exists
                        Files.move(Paths.get(tmpFile.getAbsolutePath()), Paths.get(file.getAbsolutePath()),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        // TODO: check whether these error messages really make sense - rename is attempted here, but
                        // rename exception is thrown when comparing the original and temp files
                        logger.error("Failed to write network interfaces file", e);
                        throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                                "error while building up new configuration file for network interfaces");
                    }
                } else {
                    logger.info("Not rewriting network interfaces file because it is the same");
                }
            } catch (IOException e) {
                logger.error("Failed to rename debian tmp config file {} to {}", tmpFile.getName(), file.getName(), e);
                throw KuraException.internalError(e.getMessage());
            }
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

        List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                .getNetInterfaceAddresses();
        StringBuilder sb = new StringBuilder();

        logger.debug("There are {} NetInterfaceAddressConfigs in this configuration",
                netInterfaceAddressConfigs.size());

        for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
            List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();

            if (netConfigs != null) {
                for (NetConfig netConfig : netConfigs) {
                    if (netConfig instanceof NetConfigIP4) {
                        logger.debug("Writing netconfig {} for {}", netConfig.getClass().toString(), interfaceName);

                        // ONBOOT
                        if (((NetConfigIP4) netConfig).isAutoConnect()) {
                            if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())
                                    && netInterfaceConfig.getType() == NetInterfaceType.WIFI
                                    && ((NetConfigIP4) netConfig).isDhcp()) {
                                sb.append("#!kura!auto " + interfaceName + "\n");
                            } else {
                                sb.append("auto " + interfaceName + "\n");
                            }
                        }

                        // BOOTPROTO
                        if (OS_VERSION.equals(KuraConstants.Raspberry_Pi.getImageName())
                                && netInterfaceConfig.getType() == NetInterfaceType.WIFI
                                && ((NetConfigIP4) netConfig).isDhcp()) {
                            sb.append("# Commented out to prevent wpa_supplicant from starting dhclient\n");
                            sb.append("#!kura!iface " + interfaceName + " inet ");
                        } else {
                            sb.append("iface " + interfaceName + " inet ");
                        }
                        if (((NetConfigIP4) netConfig).isDhcp()) {
                            logger.debug("new config is DHCP");
                            sb.append("dhcp\n");
                        } else {
                            logger.debug("new config is STATIC");
                            sb.append("static\n");
                        }

                        if (!((NetConfigIP4) netConfig).isDhcp()) {
                            // IPADDR
                            sb.append("\taddress ").append(((NetConfigIP4) netConfig).getAddress().getHostAddress())
                                    .append("\n");

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
                        } else {
                            // DEFROUTE
                            if (((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN) {
                                sb.append("\tpost-up route del default dev ");
                                sb.append(interfaceName);
                                sb.append("\n");
                            }
                        }

                        // DNS
                        List<? extends IPAddress> dnsAddresses = ((NetConfigIP4) netConfig).getDnsServers();
                        boolean setDns = false;
                        for (int i = 0; i < dnsAddresses.size(); i++) {
                            if (!LOCALHOST.equals(dnsAddresses.get(i).getHostAddress())) {
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
                                sb.append(dnsAddresses.get(i).getHostAddress() + " ");
                            }
                        }
                        sb.append("\n");
                    }
                }
            } else {
                logger.debug("debianWriteUtility() :: netConfigs is null");
            }

            // WIFI
            if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfig) {
                logger.debug("new config is a WifiInterfaceAddressConfig");
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
        NetInterfaceStatus netInterfaceStatus = null;

        boolean gotNetConfigIP4 = false;

        List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                .getNetInterfaceAddresses();

        if (netInterfaceAddressConfigs != null) {
            for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
                List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
                if (netConfigs != null) {
                    for (int i = 0; i < netConfigs.size(); i++) {
                        NetConfig netConfig = netConfigs.get(i);
                        if (netConfig instanceof NetConfigIP4) {
                            netInterfaceStatus = ((NetConfigIP4) netConfig).getStatus();
                            gotNetConfigIP4 = true;
                        }
                    }
                }
            }
        }

        if (!gotNetConfigIP4) {
            netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
        }

        logger.debug("Setting NetInterfaceStatus to " + netInterfaceStatus + " for " + netInterfaceConfig.getName());

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

        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof NetConfigIP4) {
                    NetConfigIP4 netConfigIP4 = (NetConfigIP4) netConfig;

                    // ONBOOT
                    props.setProperty("ONBOOT", netConfigIP4.isAutoConnect() ? "yes" : "no");

                    // BOOTPROTO
                    props.setProperty("BOOTPROTO", netConfigIP4.isDhcp() ? "dhcp" : "static");

                    if (!netConfigIP4.isDhcp()) {
                        // IPADDR
                        if (netConfigIP4.getAddress() != null) {
                            props.setProperty("IPADDR", netConfigIP4.getAddress().getHostAddress());
                        }

                        // NETMASK
                        if (netConfigIP4.getSubnetMask() != null) {
                            props.setProperty("NETMASK", netConfigIP4.getSubnetMask().getHostAddress());
                        }

                        // NETWORK
                        // TODO: Handle Debian NETWORK value

                        // GATEWAY
                        if (netConfigIP4.getGateway() != null) {
                            props.setProperty("GATEWAY", netConfigIP4.getGateway().getHostAddress());
                            props.setProperty("DEFROUTE", "yes");
                        } else {
                            props.setProperty("DEFROUTE", "no");
                        }
                    } else {
                        if (((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
                            props.setProperty("DEFROUTE", "yes");
                        } else {
                            props.setProperty("DEFROUTE", "no");
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
            }
        } else {
            logger.debug("netConfigs is null");
        }

        return props;
    }

    private boolean configHasChanged(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        Properties oldConfig = IfcfgConfigReader.parseDebianConfigFile(new File(getFinalFile()),
                netInterfaceConfig.getName());

        // FIXME: assumes only one addressConfig
        Properties newConfig = parseNetInterfaceAddressConfig(netInterfaceConfig.getNetInterfaceAddresses().get(0));

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
