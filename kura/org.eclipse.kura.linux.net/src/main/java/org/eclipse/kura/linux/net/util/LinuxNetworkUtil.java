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
package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.NetworkServiceImpl;
import org.eclipse.kura.linux.net.wifi.WifiOptions;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxNetworkUtil {

    private static final Logger logger = LoggerFactory.getLogger(LinuxNetworkUtil.class);

    private static Map<String, LinuxIfconfig> ifconfigs = new HashMap<>();

    private static final String[] ignoreIfaces = { "can", "sit", "mon.wlan" };

    private static final ArrayList<String> tools = new ArrayList<>();

    private static final String PPP_IFACE_REGEX = "^ppp\\d+$";
    private static final String MODEM = "MODEM";
    private static final String ETHERNET = "ETHERNET";
    private static final String LINK_ENCAP = "Link encap:";
    private static final String NAME = "name";
    private static final String VERSION = "version";
    private static final String FIRMWARE = "firmware";
    private static final String UNKNOWN = "unknown";
    private static final String IW = "iw";
    private static final String IWCONFIG = "iwconfig";

    private static final String LINE_MSG = "line: {}";

    private static final String ERR_EXECUTING_CMD_MSG = "error executing command --- {} --- exit value={}";

    private LinuxNetworkUtil() {
    }

    public static List<String> getAllInterfaceNames() throws KuraException {
        try {
            IpAddrShow ipAddrShow = new IpAddrShow();
            LinuxIfconfig[] configs = ipAddrShow.exec();
            List<String> ifaces = new ArrayList<>();
            for (LinuxIfconfig config : configs) {
                ifaces.add(config.getName());
            }
            return ifaces;
        } catch (KuraException e) {
            logger.warn("FIXME: IpAddrShow failed. Falling back to old method", e);
            return getAllInterfaceNamesInternal();
        }
    }

    @Deprecated
    private static List<String> getAllInterfaceNamesInternal() throws KuraException {
        SafeProcess proc = null;
        String cmd = "ifconfig -a";
        try {
            // start the process
            proc = ProcessUtil.exec(cmd);
            if (proc.waitFor() != 0) {
                logger.error(ERR_EXECUTING_CMD_MSG, cmd, proc.exitValue());
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
            }
            // get the output
            return getAllInterfaceNamesInternalParse(cmd, proc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formInterruptedCommandMessage(cmd));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    @Deprecated
    private static List<String> getAllInterfaceNamesInternalParse(String cmd, SafeProcess proc) throws KuraException {
        List<String> ifaces = new ArrayList<>();
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.indexOf(LINK_ENCAP) > -1) {
                    StringTokenizer st = new StringTokenizer(line);
                    ifaces.add(st.nextToken());
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
        return ifaces;
    }

    /*
     * Returns null if the interface is not found
     */
    public static String getCurrentIpAddress(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return null;
        }

        LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);

        return ifconfig != null ? ifconfig.getInetAddress() : null;
    }

    /*
     * Returns -1 if the interface is not found
     */
    public static int getCurrentMtu(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return -1;
        }

        LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);

        return ifconfig != null ? ifconfig.getMtu() : -1;
    }

    public static boolean isLinkUp(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return false;
        }
        return isLinkUp(getType(ifaceName), ifaceName);
    }

    /*
     * Returns false if the interface is not found
     */
    public static boolean isLinkUp(NetInterfaceType ifaceType, String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return false;
        }

        try {
            LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);
            // FIXME: should we throw an exception if config is null?
            return ifconfig != null ? ifconfig.isLinkUp() : false;
        } catch (KuraException e) {
            logger.warn("FIXME: IpAddrShow failed. Falling back to old method", e);
            return isLinkUpInternal(ifaceType, ifaceName);
        }
    }

    @Deprecated
    private static boolean isLinkUpInternal(NetInterfaceType ifaceType, String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return false;
        }

        try {
            if (ifaceType == NetInterfaceType.WIFI) {
                Collection<String> supportedWifiOptions = WifiOptions.getSupportedOptions(ifaceName);
                LinkTool linkTool = null;
                if (!supportedWifiOptions.isEmpty()) {
                    if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
                        linkTool = new IwLinkTool(ifaceName);
                    } else if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_WEXT)) {
                        linkTool = new iwconfigLinkTool(ifaceName);
                    }
                }

                if (linkTool != null && linkTool.get()) {
                    return linkTool.isLinkDetected();
                } else {
                    logger.error("link tool failed to detect the status of {}", ifaceName);
                    return false;
                }
            } else if (ifaceType == NetInterfaceType.ETHERNET) {
                LinkTool linkTool = null;
                if (toolExists("ethtool")) {
                    linkTool = new EthTool(ifaceName);
                } else if (toolExists("mii-tool")) {
                    linkTool = new MiiTool(ifaceName);
                }

                if (linkTool != null) {
                    if (linkTool.get()) {
                        return linkTool.isLinkDetected();
                    } else {
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
                                "link tool failed to detect the ethernet status of " + ifaceName);
                    }
                } else {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
                            "ethtool or mii-tool must be included with the Linux distro");
                }
            } else {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported NetInterfaceType: " + ifaceType);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public static boolean toolExists(String tool) {
        boolean ret = false;
        final String[] searchFolders = new String[] { "/sbin/", "/usr/sbin/", "/bin/" };

        if (tools.contains(tool)) {
            ret = true;
        } else {
            for (String folder : searchFolders) {
                File fTool = new File(folder + tool);
                if (fTool.exists()) {
                    tools.add(tool);
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    /**
     * This method is meaningful only for interfaces of type: NetInterfaceType.ETHERNET, NetInterfaceType.WIFI,
     * NetInterfaceType.LOOPBACK.
     */
    public static boolean isAutoConnect(String interfaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(interfaceName.charAt(0))) {
            return false;
        }

        File interfaceFile = new File("/etc/sysconfig/network-scripts/ifcfg-" + interfaceName);
        if (interfaceFile.exists()) {
            return isAutoConnectRedhat(interfaceFile);
        }

        interfaceFile = new File("/etc/network/interfaces");
        if (interfaceFile.exists()) {
            return isAutoConnectDebian(interfaceName, interfaceFile);
        }
        return false;
    }

    private static boolean isAutoConnectRedhat(File interfaceFile) throws KuraException {
        try (FileReader fr = new FileReader(interfaceFile); BufferedReader br = new BufferedReader(fr)) {
            String line = null;
            boolean ret = false;
            while ((line = br.readLine()) != null) {
                if (line.contains("ONBOOT=yes")) {
                    ret = true;
                    break;
                }
            }
            return ret;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    private static boolean isAutoConnectDebian(String ifaceName, File interfaceFile) throws KuraException {
        try (FileReader fr = new FileReader(interfaceFile); BufferedReader br = new BufferedReader(fr)) {
            String line = null;
            boolean ret = false;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("auto") && line.endsWith(ifaceName)) {
                    ret = true;
                    break;
                }
            }
            return ret;
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    /*
     * Returns null if the interface is not found
     * Note: the returned configuration is also stored in the static cache
     */
    public static LinuxIfconfig getInterfaceConfiguration(String ifaceName) throws KuraException {
        try {
            IpAddrShow ipAddrShow = new IpAddrShow(ifaceName);
            LinuxIfconfig[] configs = ipAddrShow.exec();
            if (configs.length == 0) {
                return null;
            }
            LinuxIfconfig config = configs[0];

            // determine if wifi
            if (config.getType() == NetInterfaceType.ETHERNET) {
                Collection<String> wifiOptions = WifiOptions.getSupportedOptions(ifaceName);
                if (!wifiOptions.isEmpty()) {
                    for (String op : wifiOptions) {
                        logger.trace("WiFi option supported on {} : {}", ifaceName, op);
                    }
                    config.setType(NetInterfaceType.WIFI);
                }
            }

            // determine driver
            if (config.getType() == NetInterfaceType.ETHERNET || config.getType() == NetInterfaceType.WIFI) {
                try {
                    Map<String, String> driver = getEthernetDriver(ifaceName);
                    config.setDriver(driver);
                } catch (KuraException e) {
                    logger.error("getInterfaceConfiguration() :: failed to obtain driver information - {}", e);
                }
            }

            // cache information
            ifconfigs.put(ifaceName, config);
            return config;
        } catch (KuraException e) {
            if (e.getCode() == KuraErrorCode.OS_COMMAND_ERROR || e.getCode() == KuraErrorCode.PROCESS_EXECUTION_ERROR) {
                // Assuming ifconfig fails because a PPP link went down and its interface cannot be found
                if (ifaceName.matches(PPP_IFACE_REGEX)) {
                    File pppFile = new File(NetworkServiceImpl.PPP_PEERS_DIR + ifaceName);
                    if (pppFile.exists()) {
                        LinuxIfconfig config = new LinuxIfconfig(ifaceName);
                        config.setType(NetInterfaceType.valueOf(MODEM));
                        return config;
                    }
                }
            } else {
                logger.warn("FIXME: IpAddrShow failed. Falling back to old ifconfig method", e);
                return getInterfaceConfigurationInternal(ifaceName);
            }
        }
        return null;
    }

    @Deprecated
    private static LinuxIfconfig getInterfaceConfigurationInternal(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return null;
        }
        for (String ignoreIface : ignoreIfaces) {
            if (ifaceName.startsWith(ignoreIface)) {
                return null;
            }
        }

        LinuxIfconfig linuxIfconfig = null;
        SafeProcess proc = null;
        linuxIfconfig = new LinuxIfconfig(ifaceName);
        String cmd = formIfconfigIfaceCommand(ifaceName);
        try {
            // start the process
            proc = ProcessUtil.exec(cmd);
            if (proc.waitFor() == 0) {
                // get the output
                getInterfaceConfigurationInternalParse(ifaceName, cmd, linuxIfconfig, proc);
            } else {
                File pppFile = new File(NetworkServiceImpl.PPP_PEERS_DIR + ifaceName);
                if (pppFile.exists() || ifaceName.matches(PPP_IFACE_REGEX)) {
                    linuxIfconfig.setType(NetInterfaceType.valueOf(MODEM));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formInterruptedCommandMessage(cmd));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }

        if (linuxIfconfig.getType() == NetInterfaceType.ETHERNET || linuxIfconfig.getType() == NetInterfaceType.WIFI) {
            try {
                Map<String, String> driver = getEthernetDriver(ifaceName);

                linuxIfconfig.setDriver(driver);
            } catch (KuraException e) {
                logger.error("getInterfaceConfiguration() :: failed to obtain driver information - {}", e);
            }
        }

        ifconfigs.put(ifaceName, linuxIfconfig);
        return linuxIfconfig;
    }

    @Deprecated
    private static void getInterfaceConfigurationInternalParse(String ifaceName, String cmd,
            LinuxIfconfig linuxIfconfig, SafeProcess proc) throws KuraException {
        String line = null;
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {

                int i = line.indexOf(LINK_ENCAP);
                if (i > -1) {
                    linuxIfconfig.setType(getInterfaceType(ifaceName, line));

                    i = line.indexOf("HWaddr ");
                    if (i > -1) {
                        String mac = line.substring(i + 7, line.length() - 2);
                        linuxIfconfig.setMacAddress(mac);
                    }
                }

                i = line.indexOf("inet addr:");
                if (i > -1) {
                    String ipAddress = line.substring(i + 10, line.indexOf(' ', i + 10));
                    linuxIfconfig.setInetAddress(ipAddress);

                    i = line.indexOf("Mask:");
                    if (i > -1) {
                        String netmask = line.substring(i + 5);
                        linuxIfconfig.setInetMask(netmask);
                    }

                    i = line.indexOf("Bcast:");
                    if (i > -1) {
                        String broadcast = line.substring(i + 6, line.indexOf(' ', i + 6));
                        linuxIfconfig.setInetBcast(broadcast);
                    }
                }

                i = line.indexOf("MTU:");
                if (i > -1) {
                    String mtu = line.substring(i + 4, line.indexOf(' ', i + 4));
                    linuxIfconfig.setMtu(Integer.parseInt(mtu));
                }

                if (line.contains("MULTICAST")) {
                    linuxIfconfig.setMulticast(true);
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
    }

    /*
     * Returns false on error
     */
    public static boolean canPing(String ipAddress, int count) throws KuraException {
        SafeProcess proc = null;
        String cmd = new StringBuilder().append("ping -c ").append(count).append(" ").append(ipAddress).toString();
        try {
            proc = ProcessUtil.exec(cmd);
            return proc.waitFor() == 0 ? true : false;
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formInterruptedCommandMessage(cmd));
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    /*
     * Returns NetInterfaceType.UNKNOWN for ignored interfaces or if the interface is not found
     * Note: may return a cached information
     */
    public static NetInterfaceType getType(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return NetInterfaceType.UNKNOWN;
        }
        for (String ignoreIface : ignoreIfaces) {
            if (ifaceName.startsWith(ignoreIface)) {
                return NetInterfaceType.UNKNOWN;
            }
        }

        NetInterfaceType ifaceType = null;

        if (ifconfigs.containsKey(ifaceName)) {
            LinuxIfconfig ifconfig = ifconfigs.get(ifaceName);
            ifaceType = ifconfig.getType();
        } else {
            LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);
            if (ifconfig != null) {
                ifaceType = ifconfig.getType();
            }
        }
        logger.trace("getType() :: interface={}, type={}", ifaceName, ifaceType);
        return ifaceType;
    }

    @Deprecated
    private static NetInterfaceType getInterfaceType(String ifaceName, String line) throws KuraException {

        NetInterfaceType ifaceType = NetInterfaceType.UNKNOWN;

        String stringType;
        StringTokenizer st = new StringTokenizer(line);
        st.nextToken(); // skip iface name
        st.nextToken(); // skip Link
        stringType = st.nextToken();
        stringType = stringType.substring(6).toUpperCase();
        if ("LOCAL".equals(stringType)) {
            stringType = "LOOPBACK";
        } else if (ETHERNET.equals(stringType)) {
            stringType = ETHERNET;
        } else if ("POINT-TO-POINT".equals(stringType)) {
            stringType = MODEM;
        }

        // determine if wifi
        if (ETHERNET.equals(stringType)) {
            Collection<String> wifiOptions = WifiOptions.getSupportedOptions(ifaceName);
            if (!wifiOptions.isEmpty()) {
                for (String op : wifiOptions) {
                    logger.trace("WiFi option supported on {} : {}", ifaceName, op);
                }
                stringType = "WIFI";
            }
        }

        if (stringType != null) {
            try {
                ifaceType = NetInterfaceType.valueOf(stringType);
            } catch (Exception e) {
                // leave it UNKNOWN
            }
        }
        return ifaceType;
    }

    /*
     * Return a dummy driver if the interface cannot be found or in case of an error
     * Note: may return a cached information
     */
    public static Map<String, String> getEthernetDriver(String interfaceName) throws KuraException {
        Map<String, String> driver = null;
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(interfaceName.charAt(0))) {
            driver = new HashMap<>();
            driver.put(NAME, UNKNOWN);
            driver.put(VERSION, UNKNOWN);
            driver.put(FIRMWARE, UNKNOWN);
            return driver;
        }

        if (ifconfigs.containsKey(interfaceName)) {
            LinuxIfconfig ifconfig = ifconfigs.get(interfaceName);
            driver = ifconfig.getDriver();
        }

        if (driver != null) {
            return driver;
        }

        driver = new HashMap<>();
        driver.put(NAME, UNKNOWN);
        driver.put(VERSION, UNKNOWN);
        driver.put(FIRMWARE, UNKNOWN);

        SafeProcess procEthtool = null;
        String ethtoolCmd = "ethtool -i " + interfaceName;
        try {
            // run ethtool
            if (toolExists("ethtool")) {
                procEthtool = ProcessUtil.exec(ethtoolCmd);
                if (procEthtool.waitFor() != 0) {
                    logger.error(ERR_EXECUTING_CMD_MSG, ethtoolCmd, procEthtool.exitValue());
                    return driver;
                }
                getEthernetDriverParse(ethtoolCmd, driver, procEthtool);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e,
                    formInterruptedCommandMessage(ethtoolCmd));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(ethtoolCmd));
        } finally {
            if (procEthtool != null) {
                ProcessUtil.destroy(procEthtool);
            }
        }
        return driver;
    }

    private static void getEthernetDriverParse(String cmd, Map<String, String> driver, SafeProcess proc)
            throws KuraException {
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("driver: ")) {
                    driver.put(NAME, line.substring(line.indexOf(": ") + 1));
                } else if (line.startsWith("version: ")) {
                    driver.put(VERSION, line.substring(line.indexOf(": ") + 1));
                } else if (line.startsWith("firmware-version: ")) {
                    driver.put(FIRMWARE, line.substring(line.indexOf(": ") + 1));
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
    }

    /*
     * Returns an empty capabilities set if the interface is not found or on error
     */
    public static Set<Capability> getWifiCapabilities(String ifaceName) throws KuraException {
        Set<Capability> capabilities = EnumSet.noneOf(Capability.class);

        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return capabilities;
        }

        SafeProcess proc = null;
        String cmd = "iwlist " + ifaceName + " auth";
        try {
            // start the process
            proc = ProcessUtil.exec(cmd);
            if (proc.waitFor() != 0) {
                logger.warn("error executing command --- iwlist --- exit value = {}", proc.exitValue());
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
            }

            // get the output
            getWifiCapabilitiesParse(cmd, capabilities, proc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formInterruptedCommandMessage(cmd));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
        return capabilities;
    }

    private static void getWifiCapabilitiesParse(String cmd, Set<Capability> capabilities, SafeProcess proc)
            throws KuraException {

        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                // Remove all whitespace
                String cleanLine = line.replaceAll("\\s", "");

                if ("WPA".equals(cleanLine)) {
                    capabilities.add(Capability.WPA);
                } else if ("WPA2".equals(cleanLine)) {
                    capabilities.add(Capability.RSN);
                } else if ("CIPHER-TKIP".equals(cleanLine)) {
                    capabilities.add(Capability.CIPHER_TKIP);
                } else if ("CIPHER-CCMP".equals(cleanLine)) {
                    capabilities.add(Capability.CIPHER_CCMP);
                    // TODO: WEP options don't always seem to be displayed?
                } else if ("WEP-104".equals(cleanLine)) {
                    capabilities.add(Capability.CIPHER_WEP104);
                } else if ("WEP-40".equals(cleanLine)) {
                    capabilities.add(Capability.CIPHER_WEP40);
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
    }

    /*
     * Returns WifiMode.UNKNOWN if the interface is not found or on error
     */
    public static WifiMode getWifiMode(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return WifiMode.UNKNOWN;
        }
        WifiMode mode = WifiMode.UNKNOWN;
        SafeProcess procIw = null;
        SafeProcess procIwConfig = null;
        String cmd = "";
        try {
            cmd = formIwDevIfaceInfoCommand(ifaceName);
            if (toolExists(IW)) {
                procIw = ProcessUtil.exec(cmd);
                if (procIw.waitFor() != 0) {
                    // fallback to iwconfig
                    logger.error(ERR_EXECUTING_CMD_MSG, cmd, procIw.exitValue());
                } else {
                    mode = getWifiModeParseIw(cmd, procIw);
                }
            }

            if (mode.equals(WifiMode.UNKNOWN) && toolExists(IWCONFIG)) {
                cmd = formIwconfigIfaceCommand(ifaceName);
                procIwConfig = ProcessUtil.exec(cmd);
                if (procIwConfig.waitFor() != 0) {
                    logger.error(ERR_EXECUTING_CMD_MSG, cmd, procIwConfig.exitValue());
                    throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, procIwConfig.exitValue());
                }

                // get the output
                mode = getWifiModeParseIwconfig(cmd, procIw);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formInterruptedCommandMessage(cmd));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        } finally {
            if (procIw != null) {
                ProcessUtil.destroy(procIw);
            }
            if (procIwConfig != null) {
                ProcessUtil.destroy(procIwConfig);
            }
        }

        return mode;
    }

    private static WifiMode getWifiModeParseIw(String cmd, SafeProcess proc) throws KuraException {

        WifiMode mode = WifiMode.UNKNOWN;
        String line = null;
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                int index = line.indexOf("type ");
                if (index > -1) {
                    logger.debug(LINE_MSG, line);
                    String sMode = line.substring(index + "type ".length());
                    mode = getWifiModeParseGetMode(sMode);
                    break;
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
        return mode;
    }

    private static WifiMode getWifiModeParseIwconfig(String cmd, SafeProcess proc) throws KuraException {
        WifiMode mode = WifiMode.UNKNOWN;
        String line = null;
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                int index = line.indexOf("Mode:");
                if (index > -1) {
                    logger.debug(LINE_MSG, line);
                    StringTokenizer st = new StringTokenizer(line.substring(index));
                    String modeStr = st.nextToken().substring(5);
                    mode = getWifiModeParseGetMode(modeStr);
                    break;
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
        return mode;
    }

    private static WifiMode getWifiModeParseGetMode(String modeStr) {
        WifiMode mode = WifiMode.UNKNOWN;
        if ("Managed".equalsIgnoreCase(modeStr)) {
            mode = WifiMode.INFRA;
        } else if ("Master".equals(modeStr) || "AP".equals(modeStr)) {
            mode = WifiMode.MASTER;
        } else if ("Ad-Hoc".equals(modeStr)) {
            mode = WifiMode.ADHOC;
        }
        return mode;
    }

    /*
     * Returns 0 if the interface is not found or on error
     */
    public static long getWifiBitrate(String ifaceName) throws KuraException {
        long bitRate = 0;
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return bitRate;
        }
        SafeProcess procIw = null;
        SafeProcess procIwConfig = null;
        String cmd = null;
        try {
            if (toolExists(IW)) {
                // start the process
                cmd = formIwDevIfaceLinkCommand(ifaceName);
                procIw = ProcessUtil.exec(cmd);
                if (procIw.waitFor() != 0) {
                    // fallback to iwconfig
                    logger.error(ERR_EXECUTING_CMD_MSG, cmd, procIw.exitValue());
                } else {
                    // get the output
                    bitRate = getWifiBitrateParseIw(cmd, procIw);
                }
            } else if (toolExists(IWCONFIG)) {
                // start the process
                cmd = formIwconfigIfaceCommand(ifaceName);
                procIwConfig = ProcessUtil.exec(cmd);
                if (procIwConfig.waitFor() != 0) {
                    logger.error(ERR_EXECUTING_CMD_MSG, cmd, procIwConfig.exitValue());
                    throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, procIwConfig.exitValue());
                }

                // get the output
                bitRate = getWifiBitrateParseIwconfig(cmd, procIw);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formInterruptedCommandMessage(cmd));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        } finally {
            if (procIw != null) {
                ProcessUtil.destroy(procIw);
            }
            if (procIwConfig != null) {
                ProcessUtil.destroy(procIwConfig);
            }
        }

        return bitRate;
    }

    private static long getWifiBitrateParseIw(String cmd, SafeProcess proc) throws KuraException {
        long bitRate = 0;
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                int index = line.indexOf("tx bitrate: ");
                if (index > -1) {
                    logger.debug(LINE_MSG, line);
                    StringTokenizer st = new StringTokenizer(line.substring(index));
                    st.nextToken(); // skip 'tx'
                    st.nextToken(); // skip 'bitrate:'
                    Double rate = Double.parseDouble(st.nextToken());
                    String unit = st.nextToken();
                    return getWifiBitrateParseGetBitRate(rate, unit);
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
        return bitRate;
    }

    private static long getWifiBitrateParseIwconfig(String cmd, SafeProcess proc) throws KuraException {
        long bitRate = 0;
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                int index = line.indexOf("Bit Rate=");
                if (index > -1) {
                    logger.debug(LINE_MSG, line);
                    StringTokenizer st = new StringTokenizer(line.substring(index));
                    st.nextToken(); // skip 'Bit'
                    Double rate = Double.parseDouble(st.nextToken().substring(5));
                    String unit = st.nextToken();
                    return getWifiBitrateParseGetBitRate(rate, unit);
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
        return bitRate;
    }

    private static long getWifiBitrateParseGetBitRate(Double rate, String unit) {
        int mult = 1;
        if (unit.startsWith("kb")) {
            mult = 1000;
        } else if (unit.startsWith("Mb")) {
            mult = 1000000;
        } else if (unit.startsWith("Gb")) {
            mult = 1000000000;
        }
        return (long) (rate * mult);
    }

    /*
     * Return null if the interface is not found or on error
     */
    public static String getSSID(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return null;
        }

        String ssid = null;
        SafeProcess procIw = null;
        SafeProcess procIwConfig = null;
        String cmd = null;
        try {
            if (toolExists(IW)) {
                // start the process
                cmd = formIwDevIfaceLinkCommand(ifaceName);
                procIw = ProcessUtil.exec(cmd);
                if (procIw.waitFor() != 0) {
                    // fallback to iwconfig
                    logger.error(ERR_EXECUTING_CMD_MSG, cmd, procIw.exitValue());
                } else {
                    // get the output
                    ssid = getSSIDParseIw(cmd, procIw);
                }
            } else if (toolExists(IWCONFIG)) {
                // start the process
                cmd = formIwconfigIfaceCommand(ifaceName);
                procIwConfig = ProcessUtil.exec(cmd);
                if (procIwConfig.waitFor() != 0) {
                    logger.error(ERR_EXECUTING_CMD_MSG, cmd, procIwConfig.exitValue());
                    throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, procIwConfig.exitValue());
                }

                // get the output
                ssid = getSSIDParseIwconfig(cmd, procIwConfig);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formInterruptedCommandMessage(cmd));
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        } finally {
            if (procIw != null) {
                ProcessUtil.destroy(procIw);
            }
            if (procIwConfig != null) {
                ProcessUtil.destroy(procIwConfig);
            }
        }

        return ssid;
    }

    private static String getSSIDParseIw(String cmd, SafeProcess proc) throws KuraException {
        String ssid = null;
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                int index = line.indexOf("SSID:");
                if (index > -1) {
                    logger.debug(LINE_MSG, line);
                    String lineSub = line.substring(index);
                    StringTokenizer st = new StringTokenizer(lineSub);
                    st.nextToken();
                    ssid = st.nextToken();
                    break;
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
        return ssid;
    }

    private static String getSSIDParseIwconfig(String cmd, SafeProcess proc) throws KuraException {
        String ssid = null;
        try (InputStreamReader isr = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                int index = line.indexOf("ESSID:");
                if (index > -1) {
                    logger.debug(LINE_MSG, line);
                    String lineSub = line.substring(index);
                    StringTokenizer st = new StringTokenizer(lineSub);
                    String ssidStr = st.nextToken();
                    if (ssidStr.startsWith("\"") && ssidStr.endsWith("\"")) {
                        // get value between quotes
                        ssid = ssidStr.substring(lineSub.indexOf('"') + 1, lineSub.lastIndexOf('"'));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        }
        return ssid;
    }

    /*
     * Note: this method DOES NOT bring down the interface.
     * Instead, it just deletes the IP address leaving the interface up.
     * The trick leaves the interface powered up allowing to detect a link state change.
     * After a successful call to this method, a call to hasAddress() method returns false.
     */
    public static void disableInterface(String interfaceName) throws Exception {
        if (interfaceName != null) {
            // ignore logical interfaces like "1-1.2"
            if (Character.isDigit(interfaceName.charAt(0))) {
                return;
            }

            // FIXME:
            // * Do we really need to bring down the interface before deleting addresses?
            if (hasAddress(interfaceName)) {
                LinuxProcessUtil.start("ifdown " + interfaceName + "\n");
                LinuxProcessUtil.start("ifconfig " + interfaceName + " down\n");
            }

            // always leave the Ethernet Controller powered
            bringUpDeletingAddress(interfaceName);
        }
    }

    public static void enableInterface(String interfaceName) throws Exception {
        if (interfaceName != null) {
            // ignore logical interfaces like "1-1.2"
            if (Character.isDigit(interfaceName.charAt(0))) {
                return;
            }

            // FIXME: check the exit code and throw an exception
            LinuxProcessUtil.start("ifconfig " + interfaceName + " up\n");

            int result = LinuxProcessUtil.start("ifup --force " + interfaceName + "\n");

            if (result != 0) {
                LinuxProcessUtil.start("ifup " + interfaceName + "\n");
            }
        }
    }

    /*
     * Returns true if both the inet address and inet mask are non-null.
     * Returns false if the interface is not found.
     */
    public static boolean hasAddress(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return false;
        }

        LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);

        // FIXME: should we throw an exception if config is null?
        boolean ret = false;
        if (ifconfig != null && ifconfig.getInetAddress() != null && ifconfig.getInetMask() != null) {
            ret = true;
        }

        return ret;
    }

    /*
     * This method bring up the interface deleting its IP address.
     * The trick powers the interface up allowing to detect a link state change.
     * After a successful call to this method, a call to hasAddress() method returns false.
     */
    public static void bringUpDeletingAddress(String interfaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(interfaceName.charAt(0))) {
            return;
        }

        // Power the controller.
        // This is implementing by setting the IPv4 unspecified address 0.0.0.0.
        // This is equivalent to e.g.:
        // ip addr del 172.16.0.1/32 dev eth0
        // or, to delete all the interface address:
        // ip addr flush dev eth0
        SafeProcess proc = null;
        String cmd = new StringBuilder().append("ifconfig ").append(interfaceName).append(" 0.0.0.0").toString();
        try {
            // start the SafeProcess
            proc = ProcessUtil.exec(cmd);
            if (proc.waitFor() != 0) {
                logger.error(ERR_EXECUTING_CMD_MSG, cmd, proc.exitValue());
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, cmd, proc.exitValue());
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formFailedCommandMessage(cmd));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, formInterruptedCommandMessage(cmd));
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    /*
     * Returns true if the interface is up (e.g. by 'ifup iface' or 'ifconfig iface up').
     * Returns false if the interface is not found.
     */
    public static boolean isUp(String interfaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(interfaceName.charAt(0))) {
            return false;
        }

        LinuxIfconfig config = getInterfaceConfiguration(interfaceName);

        return config != null ? config.isUp() : false;
    }

    private static String formIfconfigIfaceCommand(String ifaceName) {
        StringBuilder sb = new StringBuilder("ifconfig ");
        sb.append(ifaceName);
        return sb.toString();
    }

    private static String formIwDevIfaceInfoCommand(String ifaceName) {
        StringBuilder sb = new StringBuilder("iw dev ");
        sb.append(ifaceName).append(" info");
        return sb.toString();
    }

    private static String formIwDevIfaceLinkCommand(String ifaceName) {
        StringBuilder sb = new StringBuilder("iw dev ");
        sb.append(ifaceName).append(" link");
        return sb.toString();
    }

    private static String formIwconfigIfaceCommand(String ifaceName) {
        StringBuilder sb = new StringBuilder("iwconfig ");
        sb.append(ifaceName);
        return sb.toString();
    }

    private static String formFailedCommandMessage(String cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(cmd).append("' failed");
        return sb.toString();
    }

    private static String formInterruptedCommandMessage(String cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(cmd).append("' interrupted");
        return sb.toString();
    }
}
