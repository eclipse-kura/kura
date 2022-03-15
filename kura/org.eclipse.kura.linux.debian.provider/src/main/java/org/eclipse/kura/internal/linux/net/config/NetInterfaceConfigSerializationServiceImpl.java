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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.internal.linux.net.NetInterfaceConfigSerializationService;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetInterfaceConfigSerializationServiceImpl implements NetInterfaceConfigSerializationService {

    private static final Logger logger = LoggerFactory.getLogger(NetInterfaceConfigSerializationServiceImpl.class);

    private static final String DEBIAN_NET_CONFIGURATION_FILE = "/etc/network/interfaces";
    private static final String DEBIAN_TMP_NET_CONFIGURATION_FILE = "/etc/network/interfaces.tmp";

    private static final String ONBOOT_PROP_NAME = "ONBOOT";
    private static final String BOOTPROTO_PROP_NAME = "BOOTPROTO";
    private static final String IPADDR_PROP_NAME = "IPADDR";
    private static final String NETMASK_PROP_NAME = "NETMASK";
    private static final String GATEWAY_PROP_NAME = "GATEWAY";
    private static final String DEFROUTE_PROP_NAME = "DEFROUTE";

    private static final String LOCALHOST = "127.0.0.1";
    private static final String CLASS_A_NETMASK = "255.0.0.0";

    private static final String REMOVE_ROUTE_COMMAND = "if ip route show dev ${IFACE} | grep default; then ip route del default dev ${IFACE}; fi";

    private static List<String> debianInterfaceComandOptions = Arrays.asList("pre-up", "up", "post-up", "pre-down",
            "down", "post-down");
    private static List<String> debianIgnoreInterfaceCommands = Arrays.asList("post-up route del default dev", //
            "post-up " + REMOVE_ROUTE_COMMAND //
    );
    private static final String DHCP = "dhcp\n";

    @Override
    public Properties read(String interfaceName) throws KuraException {
        logger.debug("Getting config for {}", interfaceName);

        File ifcfgFile = getIfcfgFile();
        if (!ifcfgFile.exists()) {
            logger.error("getConfig() :: The {} file doesn't exist", interfaceName);
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER);
        }

        return parseDebianConfigFile(ifcfgFile, interfaceName);
    }

    private File getIfcfgFile() {
        return new File(DEBIAN_NET_CONFIGURATION_FILE);
    }

    @Override
    public void write(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
        if (configHasChanged(netInterfaceConfig)) {
            writeDebianConfig(netInterfaceConfig);
        }
    }

    private boolean configHasChanged(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        Properties oldConfig = parseDebianConfigFile(new File(DEBIAN_NET_CONFIGURATION_FILE),
                netInterfaceConfig.getName());

        // FIXME: assumes only one addressConfig
        Properties newConfig = parseNetInterfaceAddressConfig(
                ((AbstractNetInterface<?>) netInterfaceConfig).getNetInterfaceAddressConfig());
        logger.debug("Comparing configs for {}", netInterfaceConfig.getName());
        logger.debug("oldProps: {}", oldConfig);
        logger.debug("newProps: {}", newConfig);

        if (!compare(oldConfig, newConfig, ONBOOT_PROP_NAME)) {
            logger.debug("ONBOOT differs");
            return true;
        } else if (!compare(oldConfig, newConfig, BOOTPROTO_PROP_NAME)) {
            logger.debug("BOOTPROTO differs");
            return true;
        } else if (!compare(oldConfig, newConfig, IPADDR_PROP_NAME)) {
            logger.debug("IPADDR differs");
            return true;
        } else if (!compare(oldConfig, newConfig, NETMASK_PROP_NAME)) {
            logger.debug("NETMASK differs");
            return true;
        } else if (!compare(oldConfig, newConfig, GATEWAY_PROP_NAME)) {
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
        } else if (!compare(oldConfig, newConfig, DEFROUTE_PROP_NAME)) {
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

    @SuppressWarnings("checkstyle:todoComment")
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

    private Properties parseDebianConfigFile(File ifcfgFile, String interfaceName) throws KuraException {
        Properties kuraProps = new Properties();
        try (Scanner scanner = new Scanner(new FileInputStream(ifcfgFile))) {
            // Debian specific routine to create Properties object
            kuraProps.setProperty(ONBOOT_PROP_NAME, "no");
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
                                kuraProps.setProperty(ONBOOT_PROP_NAME, "yes");
                            } else if ("iface".equals(args[0]) && args[1].equals(interfaceName)) { // once the correct
                                                                                                   // interface is
                                                                                                   // found,
                                                                                                   // read all
                                                                                                   // configuration
                                                                                                   // information
                                kuraProps.setProperty(BOOTPROTO_PROP_NAME, args[3]);
                                if ("dhcp".equals(args[3])) {
                                    kuraProps.setProperty(DEFROUTE_PROP_NAME, "yes");
                                }
                                parseDebianConfigFile(interfaceName, scanner, kuraProps);

                                // Debian makes assumptions about lo, handle
                                // those here
                                if ("lo".equals(interfaceName) && kuraProps.getProperty(IPADDR_PROP_NAME) == null
                                        && kuraProps.getProperty(NETMASK_PROP_NAME) == null) {
                                    kuraProps.setProperty(IPADDR_PROP_NAME, LOCALHOST);
                                    kuraProps.setProperty(NETMASK_PROP_NAME, CLASS_A_NETMASK);
                                }
                                break;
                            }
                        } catch (Exception e) {
                            logger.warn("Possible malformed configuration file for {}", interfaceName, e);
                        }
                    }
                }
            }
        } catch (FileNotFoundException err) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, err);
        }
        return kuraProps;
    }

    // Parses section of Debian network interface configuration line for specified network interface
    private void parseDebianConfigFile(String ifaceName, Scanner scanner, Properties kuraProps) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line != null && !line.isEmpty()) {
                if (line.startsWith("auto") || line.startsWith("iface")) {
                    break;
                }
                parseDebianConfigFile(line, ifaceName, kuraProps);
            }
        }
    }

    // Parses a line of Debian interface configuration file for specified interface name
    private void parseDebianConfigFile(String line, String ifaceName, Properties kuraProps) {
        String[] args = line.trim().split("\\s+");
        if ("mtu".equals(args[0])) {
            kuraProps.setProperty("mtu", args[1]);
        } else if ("address".equals(args[0])) {
            kuraProps.setProperty(IPADDR_PROP_NAME, args[1]);
        } else if ("netmask".equals(args[0])) {
            kuraProps.setProperty(NETMASK_PROP_NAME, args[1]);
        } else if ("gateway".equals(args[0])) {
            kuraProps.setProperty(GATEWAY_PROP_NAME, args[1]);
            kuraProps.setProperty(DEFROUTE_PROP_NAME, "yes");
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
            final String trimmed = sb.toString().trim();

            if (trimmed.equals("route del default dev " + ifaceName) || trimmed.equals(REMOVE_ROUTE_COMMAND)) {
                kuraProps.setProperty(DEFROUTE_PROP_NAME, "no");
            }
        }
    }

    private void writeDebianConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        String iName = netInterfaceConfig.getName();
        NetConfigIP4 netConfigIP4 = ((AbstractNetInterface<?>) netInterfaceConfig).getIP4config();
        if (netConfigIP4 == null) {
            logger.warn("The configuration for interface {} in empty", iName);
            return;
        }

        boolean isLoopback = netInterfaceConfig.getType() == NetInterfaceType.LOOPBACK;
        StringBuilder sb = new StringBuilder();
        File kuraFile = getIfcfgFile();
        boolean appendConfig = true;

        if (!kuraFile.exists()) {
            logger.warn("writeDebianConfig() :: The {} file doesn't exist.", kuraFile.getName());
            return;
        }

        appendConfig = readAndReplaceConfig(netConfigIP4, sb, kuraFile, iName, isLoopback);

        // If config not present in file, append to end
        if (appendConfig) {
            appendNetworkInterfaceConfig(netConfigIP4, sb, iName, isLoopback);
        }

        // write configuration file
        writeConfigFile(DEBIAN_TMP_NET_CONFIGURATION_FILE, DEBIAN_NET_CONFIGURATION_FILE, sb);
    }

    @SuppressWarnings("checkstyle:innerAssignment")
    private boolean readAndReplaceConfig(NetConfigIP4 netConfigIP4, StringBuilder sb, File kuraFile, String iName,
            boolean isLoopback) throws KuraIOException {
        boolean appendConfig = true;
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
                                sb.append(debianWriteUtility(netConfigIP4, iName, isLoopback));

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
        } catch (IOException e) {
            throw new KuraIOException(e, "Debian config file is not found");
        }
        return appendConfig;
    }

    private void appendNetworkInterfaceConfig(NetConfigIP4 netConfigIP4, StringBuilder sb, String iName,
            boolean isLoopback) {
        logger.debug("Appending entry to interface file...");
        // append an empty line if not there
        String s = sb.toString();
        if (!"\\n".equals(s.substring(s.length() - 1))) {
            sb.append("\n");
        }
        sb.append(debianWriteUtility(netConfigIP4, iName, isLoopback));
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
            throw new KuraIOException(e, "Failed to write debian configuration file");
        }

        // move tmp configuration file into its final destination
        copyAndDeleteTmpConfigFile(srcFile, dstFile);
    }

    private void copyAndDeleteTmpConfigFile(File tmpSrcFile, File dstFile) throws KuraException {
        try {
            if (!FileUtils.contentEquals(tmpSrcFile, dstFile)) {
                // File.renameTo performs rather badly on Windows, if the file already exists
                Files.move(Paths.get(tmpSrcFile.getAbsolutePath()), Paths.get(dstFile.getAbsolutePath()),
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                logger.info("Not rewriting network interfaces file because it is the same");
            }

            Files.deleteIfExists(Paths.get(tmpSrcFile.getAbsolutePath()));
        } catch (IOException e) {
            throw new KuraIOException(e,
                    "Failed to rename tmp config file " + tmpSrcFile.getName() + " to " + dstFile.getName());
        }
    }

    private String debianWriteUtility(NetConfigIP4 netConfigIP4, String interfaceName, boolean isLoopback) {
        StringBuilder sb = new StringBuilder();
        logger.debug("Writing netconfig {} for {}", netConfigIP4.getClass(), interfaceName);

        setOnBootProperty(interfaceName, sb, netConfigIP4);
        setBootprotoProperty(netConfigIP4, interfaceName, sb, isLoopback);
        setDnsProperty(sb, netConfigIP4);
        if (!"\n".equals(sb.toString().substring(sb.toString().length() - 1))) {
            sb.append("\n");
        }

        return sb.toString();
    }

    private void setDnsProperty(StringBuilder sb, NetConfigIP4 netConfigIP4) {
        // DNS
        List<? extends IPAddress> dnsAddresses = netConfigIP4.getDnsServers();
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
    }

    private void setBootprotoProperty(NetConfigIP4 netConfigIP4, String interfaceName, StringBuilder sb,
            boolean isLoopback) {
        // BOOTPROTO
        sb.append("iface " + interfaceName + " inet ");
        switch (netConfigIP4.getStatus()) {
        case netIPv4StatusL2Only:
            logger.debug("new config is Layer 2 Only for {}", interfaceName);
            sb.append("manual\n");
            break;
        case netIPv4StatusDisabled:
            logger.debug("new config is Disabled for {}, so set it as DHCP", interfaceName);
            sb.append(DHCP);
            break;
        case netIPv4StatusEnabledLAN:
            logger.debug("new config is Enabled for LAN {}", interfaceName);
            if (netConfigIP4.isDhcp()) {
                logger.debug("new config is DHCP for {}", interfaceName);
                sb.append(DHCP);
                // delete default route if configured as LAN
                sb.append("\t post-up ").append(REMOVE_ROUTE_COMMAND).append("\n");
            } else {
                setStaticAddressConfig(netConfigIP4, interfaceName, sb, isLoopback);
            }
            break;
        case netIPv4StatusEnabledWAN:
            if (netConfigIP4.isDhcp()) {
                logger.debug("new config is DHCP for {}", interfaceName);
                sb.append(DHCP);
            } else {
                setStaticAddressConfig(netConfigIP4, interfaceName, sb, isLoopback);
            }
            break;
        case netIPv4StatusUnmanaged:
        case netIPv4StatusUnknown:
        default:
        }
    }

    private void setOnBootProperty(String interfaceName, StringBuilder sb, NetConfigIP4 netConfigIP4) {
        // ONBOOT
        if ((netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusEnabledLAN
                || netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusEnabledWAN
                || netConfigIP4.getStatus() == NetInterfaceStatus.netIPv4StatusL2Only)
                && netConfigIP4.isAutoConnect()) {
            sb.append("auto " + interfaceName + "\n");
        }
    }

    @SuppressWarnings("checkstyle:todoComment")
    private void setStaticAddressConfig(NetConfigIP4 netConfigIP4, String interfaceName, StringBuilder sb,
            boolean isLoopback) {
        // in Debian, loopback interface cannot have assigned a static address
        if (isLoopback) {
            sb.append("loopback\n");
        } else {

            logger.debug("new config is STATIC for {}", interfaceName);

            sb.append("static\n");

            // IPADDR
            sb.append("\taddress ").append(netConfigIP4.getAddress().getHostAddress()).append("\n");

            // NETMASK
            sb.append("\tnetmask ").append(netConfigIP4.getSubnetMask().getHostAddress()).append("\n");

            // NETWORK
            // TODO: Handle Debian NETWORK value

            // Gateway
            if (netConfigIP4.getGateway() != null) {
                sb.append("\tgateway ").append(netConfigIP4.getGateway().getHostAddress()).append("\n");
            }
        }
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
}
