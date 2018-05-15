/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.net.admin.visitor.linux.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Scanner;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.util.KuraSupportedPlatforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IfcfgConfig {

    private static final Logger logger = LoggerFactory.getLogger(IfcfgConfig.class);
    protected static final String REDHAT_NET_CONFIGURATION_DIRECTORY = "/etc/sysconfig/network-scripts/";
    protected static final String DEBIAN_NET_CONFIGURATION_DIRECTORY = "/etc/network/";

    protected static final String ONBOOT_PROP_NAME = "ONBOOT";
    protected static final String BOOTPROTO_PROP_NAME = "BOOTPROTO";
    protected static final String IPADDR_PROP_NAME = "IPADDR";
    protected static final String NETMASK_PROP_NAME = "NETMASK";
    protected static final String PREFIX_PROP_NAME = "PREFIX";
    protected static final String GATEWAY_PROP_NAME = "GATEWAY";
    protected static final String DEFROUTE_PROP_NAME = "DEFROUTE";
    protected static final String DEVICE_PROP_NAME = "DEVICE";
    protected static final String NAME_PROP_NAME = "NAME";
    protected static final String TYPE_PROP_NAME = "TYPE";

    protected static final String LOCALHOST = "127.0.0.1";
    protected static final String CLASS_A_NETMASK = "255.0.0.0";

    private static String osVersion = System.getProperty("kura.os.version");

    protected boolean isDebian() {
        return osVersion.startsWith(KuraSupportedPlatforms.YOCTO_121.getImageName())
                || osVersion.equals(KuraSupportedPlatforms.RASPBIAN_100.getImageName())
                || osVersion.equals(KuraSupportedPlatforms.RASPBIAN_100.getImageName())
                || osVersion.equals(KuraSupportedPlatforms.UBUNTU_16.getImageName())
                || osVersion.startsWith(
                        KuraSupportedPlatforms.YOCTO_161.getImageName() + "_" + KuraSupportedPlatforms.YOCTO_161.getImageVersion());
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

    public Properties parseDebianConfigFile(File ifcfgFile, String interfaceName) throws KuraException {
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
                            }
                            // once the correct interface is found, read all
                            // configuration information
                            else if ("iface".equals(args[0]) && args[1].equals(interfaceName)) {
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
                            logger.warn("Possible malformed configuration file for " + interfaceName, e);
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
            if (sb.toString().trim().equals("route del default dev " + ifaceName)) {
                kuraProps.setProperty(DEFROUTE_PROP_NAME, "no");
            }
        }
    }
}
