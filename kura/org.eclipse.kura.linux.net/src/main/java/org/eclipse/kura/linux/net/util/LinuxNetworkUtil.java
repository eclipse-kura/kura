/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.NetworkServiceImpl;
import org.eclipse.kura.linux.net.wifi.WifiOptions;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.eclipse.kura.net.wifi.WifiMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxNetworkUtil {

    private static final String ETHTOOL_COMMAND = "ethtool";

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
    private static final String IFCONFIG = "ifconfig";
    private static final String IWCONFIG = "iwconfig";

    private static final String LINE_MSG = "line: {}";

    private static final String ERR_EXECUTING_CMD_MSG = "error executing command --- {} --- exit value={}";

    private final CommandExecutorService executorService;
    private final WifiOptions wifiOptions;

    public LinuxNetworkUtil(CommandExecutorService executorService) {
        this.executorService = executorService;
        this.wifiOptions = new WifiOptions(executorService);
    }

    public List<String> getAllInterfaceNames() throws KuraException {
        try {
            IpAddrShow ipAddrShow = new IpAddrShow(this.executorService);
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

    /**
     *
     * @deprecated
     */
    @Deprecated
    private List<String> getAllInterfaceNamesInternal() throws KuraException {
        String[] cmd = { IFCONFIG, "-a" };
        Command command = new Command(cmd);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            if (logger.isErrorEnabled()) {
                logger.error(ERR_EXECUTING_CMD_MSG, String.join(" ", cmd), status.getExitStatus().getExitCode());
            }
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR,
                    formFailedCommandMessage(String.join(" ", cmd)));
        }
        return getAllInterfaceNamesInternalParse(
                new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
    }

    /**
     *
     * @deprecated
     */
    @Deprecated
    private static List<String> getAllInterfaceNamesInternalParse(String commandOutput) {
        List<String> ifaces = new ArrayList<>();
        for (String line : commandOutput.split("\n")) {
            if (line.indexOf(LINK_ENCAP) > -1) {
                StringTokenizer st = new StringTokenizer(line);
                ifaces.add(st.nextToken());
            }
        }
        return ifaces;
    }

    /*
     * Returns null if the interface is not found
     */
    public String getCurrentIpAddress(String ifaceName) throws KuraException {
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
    public int getCurrentMtu(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return -1;
        }

        LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);

        return ifconfig != null ? ifconfig.getMtu() : -1;
    }

    public boolean isLinkUp(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return false;
        }
        return isLinkUp(getType(ifaceName), ifaceName);
    }

    /*
     * Returns false if the interface is not found
     */
    public boolean isLinkUp(NetInterfaceType ifaceType, String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return false;
        }

        try {
            LinuxIfconfig ifconfig = getInterfaceConfiguration(ifaceName);
            // FIXME: should we throw an exception if config is null?
            return ifconfig != null && ifconfig.isLinkUp();
        } catch (KuraException e) {
            logger.warn("FIXME: IpAddrShow failed. Falling back to old method", e);
            return isLinkUpInternal(ifaceType, ifaceName);
        }
    }

    /**
     *
     * @deprecated
     */
    @Deprecated
    private boolean isLinkUpInternal(NetInterfaceType ifaceType, String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return false;
        }

        try {
            if (ifaceType == NetInterfaceType.WIFI) {
                return isWifiLinkUpInternal(ifaceName);
            } else if (ifaceType == NetInterfaceType.ETHERNET) {
                return isEthernetLinkUpInternal(ifaceName);
            } else {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Unsupported NetInterfaceType: " + ifaceType);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    private boolean isEthernetLinkUpInternal(String ifaceName) throws KuraException {
        LinkTool linkTool = null;
        if (toolExists(ETHTOOL_COMMAND)) {
            linkTool = new EthTool(ifaceName, this.executorService);
        } else if (toolExists("mii-tool")) {
            linkTool = new MiiTool(ifaceName, this.executorService);
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
    }

    private boolean isWifiLinkUpInternal(String ifaceName) throws KuraException {
        Collection<String> supportedWifiOptions = this.wifiOptions.getSupportedOptions(ifaceName);
        LinkTool linkTool = null;
        if (!supportedWifiOptions.isEmpty()) {
            if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
                linkTool = new IwLinkTool(ifaceName, this.executorService);
            } else if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_WEXT)) {
                linkTool = new IwconfigLinkTool(ifaceName, this.executorService);
            }
        }

        if (linkTool != null && linkTool.get()) {
            return linkTool.isLinkDetected();
        } else {
            logger.error("link tool failed to detect the status of {}", ifaceName);
            return false;
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
    public LinuxIfconfig getInterfaceConfiguration(String ifaceName) throws KuraException {
        try {
            IpAddrShow ipAddrShow = new IpAddrShow(ifaceName, this.executorService);
            LinuxIfconfig[] configs = ipAddrShow.exec();
            if (configs.length == 0) {
                return null;
            }
            LinuxIfconfig config = configs[0];

            // determine if wifi
            if (config.getType() == NetInterfaceType.ETHERNET) {
                setWifiConfig(ifaceName, config);
            }

            // determine driver
            if (config.getType() == NetInterfaceType.ETHERNET || config.getType() == NetInterfaceType.WIFI) {
                Map<String, String> driver = getEthernetDriver(ifaceName);
                config.setDriver(driver);
            }

            // cache information
            ifconfigs.put(ifaceName, config);
            return config;
        } catch (KuraException e) {
            if (e.getCode() == KuraErrorCode.OS_COMMAND_ERROR || e.getCode() == KuraErrorCode.PROCESS_EXECUTION_ERROR) {
                // Assuming ifconfig fails because a PPP link went down and its interface cannot be found
                return getPppConfig(ifaceName);
            } else {
                logger.warn("FIXME: IpAddrShow failed. Falling back to old ifconfig method", e);
                return getInterfaceConfigurationInternal(ifaceName);
            }
        }
    }

    private void setWifiConfig(String ifaceName, LinuxIfconfig config) {
        Collection<String> wifiSupportedOptions = this.wifiOptions.getSupportedOptions(ifaceName);
        if (!wifiSupportedOptions.isEmpty()) {
            for (String op : wifiSupportedOptions) {
                logger.trace("WiFi option supported on {} : {}", ifaceName, op);
            }
            config.setType(NetInterfaceType.WIFI);
        }
    }

    private LinuxIfconfig getPppConfig(String ifaceName) {
        LinuxIfconfig config = null;
        if (ifaceName.matches(PPP_IFACE_REGEX)) {
            File pppFile = new File(NetworkServiceImpl.PPP_PEERS_DIR + ifaceName);
            if (pppFile.exists()) {
                config = new LinuxIfconfig(ifaceName);
                config.setType(NetInterfaceType.valueOf(MODEM));
            }
        }
        return config;
    }

    /**
     *
     * @deprecated
     */
    @Deprecated
    private LinuxIfconfig getInterfaceConfigurationInternal(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return null;
        }
        for (String ignoreIface : ignoreIfaces) {
            if (ifaceName.startsWith(ignoreIface)) {
                return null;
            }
        }

        LinuxIfconfig linuxIfconfig = new LinuxIfconfig(ifaceName);
        String[] cmd = formIfconfigIfaceCommand(ifaceName);
        Command command = new Command(cmd);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        if (status.getExitStatus().isSuccessful()) {
            getInterfaceConfigurationInternalParse(ifaceName, linuxIfconfig,
                    new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
        } else {
            File pppFile = new File(NetworkServiceImpl.PPP_PEERS_DIR + ifaceName);
            if (pppFile.exists() || ifaceName.matches(PPP_IFACE_REGEX)) {
                linuxIfconfig.setType(NetInterfaceType.valueOf(MODEM));
            }
        }

        if (linuxIfconfig.getType() == NetInterfaceType.ETHERNET || linuxIfconfig.getType() == NetInterfaceType.WIFI) {
            Map<String, String> driver = getEthernetDriver(ifaceName);
            linuxIfconfig.setDriver(driver);
        }

        ifconfigs.put(ifaceName, linuxIfconfig);
        return linuxIfconfig;
    }

    /**
     *
     * @deprecated
     */
    @Deprecated
    private void getInterfaceConfigurationInternalParse(String ifaceName, LinuxIfconfig linuxIfconfig,
            String commandOutput) throws KuraException {
        for (String line : commandOutput.split("\n")) {

            int i = line.indexOf(LINK_ENCAP);
            if (i > -1) {
                linuxIfconfig.setType(getInterfaceType(ifaceName, line));

                i = line.indexOf("HWaddr ");
                if (i > -1) {
                    String mac = line.substring(i + 7, line.length() - 2);
                    linuxIfconfig.setMacAddress(mac);
                }
            }

            getInterfaceAddress(linuxIfconfig, line);

            i = line.indexOf("MTU:");
            if (i > -1) {
                String mtu = line.substring(i + 4, line.indexOf(' ', i + 4));
                linuxIfconfig.setMtu(Integer.parseInt(mtu));
            }

            if (line.contains("MULTICAST")) {
                linuxIfconfig.setMulticast(true);
            }
        }
    }

    private void getInterfaceAddress(LinuxIfconfig linuxIfconfig, String line) {
        int i;
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
    }

    /*
     * Returns false on error
     */
    public boolean canPing(String ipAddress, int count) {
        String[] cmd = { "ping", "-c", String.valueOf(count), ipAddress };
        CommandStatus status = this.executorService.execute(new Command(cmd));
        return status.getExitStatus().isSuccessful();
    }

    /*
     * Returns NetInterfaceType.UNKNOWN for ignored interfaces or if the interface is not found
     * Note: may return a cached information
     */
    public NetInterfaceType getType(String ifaceName) throws KuraException {
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

    /**
     *
     * @deprecated
     */
    @Deprecated
    private NetInterfaceType getInterfaceType(String ifaceName, String line) throws KuraException {

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
            Collection<String> supportedWifiOptions = this.wifiOptions.getSupportedOptions(ifaceName);
            if (!supportedWifiOptions.isEmpty()) {
                for (String op : supportedWifiOptions) {
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
    public Map<String, String> getEthernetDriver(String interfaceName) {
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

        String[] ethtoolCmd = { ETHTOOL_COMMAND, "-i", interfaceName };
        if (toolExists(ETHTOOL_COMMAND)) {
            Command command = new Command(ethtoolCmd);
            command.setTimeout(60);
            command.setOutputStream(new ByteArrayOutputStream());
            CommandStatus status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                if (logger.isErrorEnabled()) {
                    logger.error(ERR_EXECUTING_CMD_MSG, String.join(" ", ethtoolCmd),
                            status.getExitStatus().getExitCode());
                }
                return driver;
            }
            getEthernetDriverParse(driver,
                    new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
        }
        return driver;
    }

    private static void getEthernetDriverParse(Map<String, String> driver, String commandOutput) {
        for (String line : commandOutput.split("\n")) {
            if (line.startsWith("driver: ")) {
                driver.put(NAME, line.substring(line.indexOf(": ") + 1));
            } else if (line.startsWith("version: ")) {
                driver.put(VERSION, line.substring(line.indexOf(": ") + 1));
            } else if (line.startsWith("firmware-version: ")) {
                driver.put(FIRMWARE, line.substring(line.indexOf(": ") + 1));
            }
        }
    }

    /*
     * Returns an empty capabilities set if the interface is not found or on error
     */
    public Set<Capability> getWifiCapabilities(String ifaceName) throws KuraException {
        if (toolExists("iwlist")) {
            return IwlistCapabilityTool.probeCapabilities(ifaceName, this.executorService);
        } else if (toolExists("iw")) {
            return IwCapabilityTool.probeCapabilities(ifaceName, this.executorService);
        } else {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        }
    }

    /*
     * Returns WifiMode.UNKNOWN if the interface is not found or on error
     */
    public WifiMode getWifiMode(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return WifiMode.UNKNOWN;
        }
        WifiMode mode = WifiMode.UNKNOWN;
        CommandStatus status;
        Command command;
        String[] cmd;
        if (toolExists(IW)) {
            cmd = formIwDevIfaceInfoCommand(ifaceName);
            command = new Command(cmd);
            command.setTimeout(60);
            command.setOutputStream(new ByteArrayOutputStream());
            status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                // fallback to iwconfig
                if (logger.isErrorEnabled()) {
                    logger.error(ERR_EXECUTING_CMD_MSG, String.join(" ", cmd), status.getExitStatus().getExitCode());
                }
            } else {
                mode = getWifiModeParseIw(
                        new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
            }
        }

        if (mode.equals(WifiMode.UNKNOWN) && toolExists(IWCONFIG)) {
            cmd = formIwconfigIfaceCommand(ifaceName);
            command = new Command(cmd);
            command.setTimeout(60);
            command.setOutputStream(new ByteArrayOutputStream());
            status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                if (logger.isErrorEnabled()) {
                    logger.error(ERR_EXECUTING_CMD_MSG, String.join(" ", cmd), status.getExitStatus().getExitCode());
                }
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", cmd),
                        status.getExitStatus().getExitCode());
            }
            // get the output
            mode = getWifiModeParseIwconfig(
                    new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
        }

        return mode;
    }

    private static WifiMode getWifiModeParseIw(String commandOutput) {

        WifiMode mode = WifiMode.UNKNOWN;
        for (String line : commandOutput.split("\n")) {
            int index = line.indexOf("type ");
            if (index > -1) {
                logger.debug(LINE_MSG, line);
                String sMode = line.substring(index + "type ".length());
                mode = getWifiModeParseGetMode(sMode);
                break;
            }
        }
        return mode;
    }

    private static WifiMode getWifiModeParseIwconfig(String commandOutput) {
        WifiMode mode = WifiMode.UNKNOWN;
        for (String line : commandOutput.split("\n")) {
            int index = line.indexOf("Mode:");
            if (index > -1) {
                logger.debug(LINE_MSG, line);
                StringTokenizer st = new StringTokenizer(line.substring(index));
                String modeStr = st.nextToken().substring(5);
                mode = getWifiModeParseGetMode(modeStr);
                break;
            }
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
    public long getWifiBitrate(String ifaceName) throws KuraException {
        long bitRate = 0;
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return bitRate;
        }
        CommandStatus status;
        Command command;
        String[] cmd;
        if (toolExists(IW)) {
            // start the process
            cmd = formIwDevIfaceLinkCommand(ifaceName);
            command = new Command(cmd);
            command.setTimeout(60);
            command.setOutputStream(new ByteArrayOutputStream());
            status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                // fallback to iwconfig
                if (logger.isErrorEnabled()) {
                    logger.error(ERR_EXECUTING_CMD_MSG, String.join(" ", cmd), status.getExitStatus().getExitCode());
                }
            } else {
                // get the output
                bitRate = getWifiBitrateParseIw(
                        new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
            }
        } else if (toolExists(IWCONFIG)) {
            // start the process
            cmd = formIwconfigIfaceCommand(ifaceName);
            command = new Command(cmd);
            command.setTimeout(60);
            command.setOutputStream(new ByteArrayOutputStream());
            status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                if (logger.isErrorEnabled()) {
                    logger.error(ERR_EXECUTING_CMD_MSG, String.join(" ", cmd), status.getExitStatus().getExitCode());
                }
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", cmd),
                        status.getExitStatus().getExitCode());
            }

            // get the output
            bitRate = getWifiBitrateParseIwconfig(
                    new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
        }
        return bitRate;
    }

    private static long getWifiBitrateParseIw(String commandOutput) {
        long bitRate = 0;
        for (String line : commandOutput.split("\n")) {
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
        return bitRate;
    }

    private static long getWifiBitrateParseIwconfig(String commandOutput) {
        long bitRate = 0;
        for (String line : commandOutput.split("\n")) {
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
    public String getSSID(String ifaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
            return null;
        }

        CommandStatus status;
        Command command;
        String ssid = null;
        String[] cmd;
        if (toolExists(IW)) {
            // start the process
            cmd = formIwDevIfaceLinkCommand(ifaceName);
            command = new Command(cmd);
            command.setTimeout(60);
            command.setOutputStream(new ByteArrayOutputStream());
            status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                // fallback to iwconfig
                if (logger.isErrorEnabled()) {
                    logger.error(ERR_EXECUTING_CMD_MSG, String.join(" ", cmd), status.getExitStatus().getExitCode());
                }
            } else {
                // get the output
                ssid = getSSIDParseIw(
                        new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
            }
        } else if (toolExists(IWCONFIG)) {
            // start the process
            cmd = formIwconfigIfaceCommand(ifaceName);
            command = new Command(cmd);
            command.setTimeout(60);
            command.setOutputStream(new ByteArrayOutputStream());
            status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                if (logger.isErrorEnabled()) {
                    logger.error(ERR_EXECUTING_CMD_MSG, String.join(" ", cmd), status.getExitStatus().getExitCode());
                }
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", cmd),
                        status.getExitStatus().getExitCode());
            }

            // get the output
            ssid = getSSIDParseIwconfig(
                    new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
        }
        return ssid;
    }

    private static String getSSIDParseIw(String commandOutput) {
        String ssid = null;
        for (String line : commandOutput.split("\n")) {
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
        return ssid;
    }

    private static String getSSIDParseIwconfig(String commandOutput) {
        String ssid = null;
        for (String line : commandOutput.split("\n")) {
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
        return ssid;
    }

    /*
     * Note: this method DOES NOT bring down the interface.
     * Instead, it just deletes the IP address leaving the interface up.
     * The trick leaves the interface powered up allowing to detect a link state change.
     * After a successful call to this method, a call to hasAddress() method returns false.
     */
    public void disableInterface(String interfaceName) throws KuraException {
        if (interfaceName != null) {
            // ignore logical interfaces like "1-1.2"
            if (Character.isDigit(interfaceName.charAt(0))) {
                return;
            }

            // FIXME:
            // * Do we really need to bring down the interface before deleting addresses?
            if (hasAddress(interfaceName)) {
                Command command = new Command(new String[] { "ifdown", interfaceName });
                command.setTimeout(60);
                // Intentionally ignore exit status
                this.executorService.execute(command);

                command = new Command(new String[] { IFCONFIG, interfaceName, "down" });
                command.setTimeout(60);
                // Intentionally ignore exit status
                this.executorService.execute(command);

            }

            // always leave the Ethernet Controller powered
            bringUpDeletingAddress(interfaceName);
        }
    }

    public void enableInterface(String interfaceName) throws KuraException {
        if (interfaceName != null) {
            // ignore logical interfaces like "1-1.2"
            if (Character.isDigit(interfaceName.charAt(0))) {
                return;
            }

            Command command = new Command(new String[] { IFCONFIG, interfaceName, "up" });
            command.setTimeout(60);
            CommandStatus status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                        "Failed to bring up interface " + interfaceName);
            }

            command = new Command(new String[] { "ifup", "--force", interfaceName });
            command.setTimeout(60);
            command.setOutputStream(new ByteArrayOutputStream());
            command.setErrorStream(new ByteArrayOutputStream());
            status = this.executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                command = new Command(new String[] { "ifup", interfaceName });
                command.setTimeout(60);
                command.setOutputStream(new ByteArrayOutputStream());
                command.setErrorStream(new ByteArrayOutputStream());
                status = this.executorService.execute(command);
                if (!status.getExitStatus().isSuccessful()) {
                    throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                            "Failed to bring up interface " + interfaceName);
                }
            }
        }
    }

    /*
     * Returns true if both the inet address and inet mask are non-null.
     * Returns false if the interface is not found.
     */
    public boolean hasAddress(String ifaceName) throws KuraException {
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
    public void bringUpDeletingAddress(String interfaceName) throws KuraException {
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
        String[] cmd = { IFCONFIG, interfaceName, "0.0.0.0" };
        Command command = new Command(cmd);
        command.setTimeout(60);
        CommandStatus status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            if (logger.isErrorEnabled()) {
                logger.error(ERR_EXECUTING_CMD_MSG, String.join(" ", cmd), status.getExitStatus().getExitCode());
            }
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", cmd),
                    status.getExitStatus().getExitCode());
        }
    }

    /*
     * Returns true if the interface is up (e.g. by 'ifup iface' or 'ifconfig iface up').
     * Returns false if the interface is not found.
     */
    public boolean isUp(String interfaceName) throws KuraException {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(interfaceName.charAt(0))) {
            return false;
        }

        LinuxIfconfig config = getInterfaceConfiguration(interfaceName);

        return config != null && config.isUp();
    }

    /*
     * returns the number of times the linux has gone up or down
     * If an error occurs, it return 0
     */
    public int getCarrierChanges(String interfaceName) {
        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(interfaceName.charAt(0))) {
            return 0;
        }

        StringBuilder sb = new StringBuilder("/sys/class/net/").append(interfaceName);
        sb.append("/carrier_changes");

        String fileName = sb.toString();
        try (FileReader fr = new FileReader(fileName); BufferedReader br = new BufferedReader(fr);) {
            int changes = Integer.parseInt(br.readLine());
            logger.debug("interface {} carrier changes {}", interfaceName, changes);
            return changes;
        } catch (IOException | NumberFormatException e) {
            logger.error("error reading {}, error message {}", fileName, e.getMessage());
        }
        return 0;
    }

    protected static String[] formIfconfigIfaceCommand(String ifaceName) {
        return new String[] { IFCONFIG, ifaceName };
    }

    protected static String[] formIwDevIfaceInfoCommand(String ifaceName) {
        return new String[] { "iw", "dev", ifaceName, "info" };
    }

    protected static String[] formIwDevIfaceLinkCommand(String ifaceName) {
        return new String[] { "iw", "dev", ifaceName, "link" };
    }

    protected static String[] formIwconfigIfaceCommand(String ifaceName) {
        return new String[] { IWCONFIG, ifaceName };
    }

    protected static String formFailedCommandMessage(String cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(cmd).append("' failed");
        return sb.toString();
    }

    protected static String formInterruptedCommandMessage(String cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(cmd).append("' interrupted");
        return sb.toString();
    }
}
