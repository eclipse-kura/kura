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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.NetworkConfigurationServiceImpl;
import org.eclipse.kura.net.admin.visitor.linux.util.ChapLinux;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangePair;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangeScript;
import org.eclipse.kura.net.admin.visitor.linux.util.PapLinux;
import org.eclipse.kura.net.admin.visitor.linux.util.PppUtil;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.usb.UsbDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PppConfigReader implements NetworkConfigurationVisitor {

    private static class FileFilter implements FilenameFilter {

        @Override
        public boolean accept(File directory, String filename) {
            return filename.startsWith("ppp") ? true : false;
        }
    }

    private static final Logger s_logger = LoggerFactory.getLogger(PppConfigReader.class);

    public static final String PEERS_DIRECTORY = "/etc/ppp/peers/";
    public static final String SCRIPTS_DIRECTORY = "/etc/ppp/scripts/";

    private static PppConfigReader s_instance;

    public static PppConfigReader getInstance() {
        if (s_instance == null) {
            s_instance = new PppConfigReader();
        }

        return s_instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig.getType() == NetInterfaceType.MODEM) {
                getConfig(netInterfaceConfig);
            }
        }
    }

    private void getConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {

        String interfaceName = netInterfaceConfig.getName();
        s_logger.debug("Getting ppp config for {}", interfaceName);

        if (netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
            StringBuilder key = new StringBuilder(
                    "net.interface." + netInterfaceConfig.getName() + ".modem.identifier");
            String modemId = KuranetConfig.getProperty(key.toString());
            s_logger.debug("Getting modem identifier using key " + key + ": " + modemId);

            if (modemId != null) {
                ((ModemInterfaceConfigImpl) netInterfaceConfig).setModemIdentifier(modemId);
            }
        }

        List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                .getNetInterfaceAddresses();

        for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
            if (netInterfaceAddressConfig instanceof ModemInterfaceAddressConfigImpl) {
                List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();

                if (netConfigs == null) {
                    netConfigs = new ArrayList<NetConfig>();
                    ((ModemInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(netConfigs);
                }

                // Create a ModemConfig
                ModemConfig modemConfig = getModemConfig(interfaceName, netInterfaceConfig.getUsbDevice());
                if (modemConfig != null) {
                    netConfigs.add(modemConfig);
                }

                // Create a NetConfigIP4
                netConfigs.add(getNetConfigIP4(interfaceName));

                // Populate with DNS provided by PPP (displayed as read-only in Denali)
                if (LinuxNetworkUtil.hasAddress("ppp" + modemConfig.getPppNumber())) {
                    List<? extends IPAddress> pppDnsServers = LinuxDns.getInstance().getPppDnServers();
                    if (pppDnsServers != null) {
                        ((ModemInterfaceAddressConfigImpl) netInterfaceAddressConfig).setDnsServers(pppDnsServers);
                    }
                }
            }
        }
    }

    private static ModemConfig getModemConfig(String ifaceName, UsbDevice usbDevice) throws KuraException {
        s_logger.debug("parsePppPeerConfig()");
        boolean isGsmGprsUmtsHspa = false;
        List<ModemTechnologyType> technologyTypes = null;
        if (usbDevice != null) {
            SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem(usbDevice);
            if (usbModemInfo != null) {
                technologyTypes = usbModemInfo.getTechnologyTypes();

            }
        } else {
            SupportedSerialModemInfo serialModemInfo = SupportedSerialModemsInfo.getModem();
            if (serialModemInfo != null) {
                technologyTypes = serialModemInfo.getTechnologyTypes();
            }
        }

        if (technologyTypes != null) {
            for (ModemTechnologyType technologyType : technologyTypes) {
                if (technologyType == ModemTechnologyType.GSM_GPRS || technologyType == ModemTechnologyType.UMTS
                        || technologyType == ModemTechnologyType.HSDPA || technologyType == ModemTechnologyType.HSPA) {
                    isGsmGprsUmtsHspa = true;
                    break;
                }
            }
        }

        boolean enabled = true;
        int unitNum = getUnitNum(ifaceName);

        String apn = "";
        String pdpType = "UNKNOWN";
        String dialString = "";
        String username = "";
        String password = "";
        String model = "";
        AuthType authType = AuthType.NONE;
        boolean persist = false;
        int maxFail = 0;
        int idle = 0;
        String activeFilter = "";
        int lcpEchoInterval = 0;
        int lcpEchoFailure = 0;

        String peerFilename = getPeerFilename(ifaceName, usbDevice);
        File peerFile = new File(peerFilename);
        if (!peerFile.exists()) {
            persist = true;
            maxFail = 5;
            idle = 95;
            activeFilter = "inbound";
            s_logger.warn("getModemConfig() :: PPPD peer file does not exist - {}", peerFilename);
        } else {
            s_logger.debug("getModemConfig() :: PPPD peer file exists - {}", peerFilename);
            // Check if peer file is a symlink. If so, get information from the linked filename.
            try {
                if (!peerFile.getCanonicalPath().equals(peerFile.getAbsolutePath())) {
                    Map<String, String> fileInfo = PppUtil.parsePeerFilename(peerFile.getCanonicalFile().getName());
                    fileInfo.get("technology");
                    model = fileInfo.get("model");
                    fileInfo.get("serialNum");
                    fileInfo.get("modemId");
                }
            } catch (IOException e) {
                s_logger.error("Error checking for symlink", e);
            }

            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(peerFilename);
                props.load(fis);
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Error getting modem config", e);
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Error getting modem config", e);
                    }
                }
            }

            s_logger.debug("peer properties: {}", props);

            if (props.getProperty("unit") != null) {
                unitNum = Integer.parseInt(props.getProperty("unit"));
            }

            if (props.getProperty("user") != null) {
                username = removeQuotes(props.getProperty("user"));
            }

            if (props.getProperty("persist") != null) {
                persist = true;
            }

            if (props.getProperty("maxfail") != null) {
                maxFail = Integer.parseInt(props.getProperty("maxfail"));
            }

            if (props.getProperty("idle") != null) {
                idle = Integer.parseInt(props.getProperty("idle"));
            }

            if (props.getProperty("active-filter") != null) {
                activeFilter = removeQuotes(props.getProperty("active-filter"));
            }

            if (props.getProperty("lcp-echo-interval") != null) {
                lcpEchoInterval = Integer.parseInt(props.getProperty("lcp-echo-interval"));
            }

            if (props.getProperty("lcp-echo-failure") != null) {
                lcpEchoFailure = Integer.parseInt(props.getProperty("lcp-echo-failure"));
            }

            String chatFilename = "";
            String connectProperty = removeQuotes(props.getProperty("connect"));
            String[] args = connectProperty.split("\\s+");
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-f") && args.length > i + 1) {
                    chatFilename = args[i + 1];
                    break;
                }
            }

            // String disconnectFilename = "";
            // String disconnectProperty = removeQuotes(props.getProperty("disconnect"));
            // args = disconnectProperty.split("\\s+");
            // for(int i=0; i<args.length; i++) {
            // if(args[i].equals("-f") && args.length > i+1) {
            // disconnectFilename = args[i+1];
            // break;
            // }
            // }

            // Parse the connect script
            ModemXchangeScript connectScript = null;

            try {
                connectScript = ModemXchangeScript.parseFile(chatFilename);
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Error parsing " + chatFilename, e);
            }

            if (connectScript != null) {
                ModemXchangePair modemXchangePair = connectScript.getFirstModemXchangePair();
                ModemXchangePair prevXchangePair = null;
                String expectedStr, sendStr;

                while (modemXchangePair != null) {
                    expectedStr = modemXchangePair.getExpectString();
                    sendStr = removeQuotes(modemXchangePair.getSendString());

                    if (expectedStr.equals("OK")) {
                        // apn
                        if (sendStr.contains(",")) {
                            String[] sendArgs = sendStr.split(",");
                            if (sendArgs.length == 3 && isGsmGprsUmtsHspa) {
                                pdpType = removeQuotes(sendArgs[1]);
                                apn = removeQuotes(sendArgs[2]);
                            }
                        }
                        // } else if(expectedStr.equals("\"\"")) {
                    } else if (expectedStr.equals("CONNECT")) {
                        // dial string
                        if (prevXchangePair != null) {
                            dialString = removeQuotes(prevXchangePair.getSendString());
                        }
                    }

                    prevXchangePair = modemXchangePair;
                    modemXchangePair = connectScript.getNextModemXchangePair();
                }
            }

            s_logger.debug("* Enabled: {}", enabled);
            s_logger.debug("* CHAT file: {}", chatFilename);
            s_logger.debug("* UnitNum: {}", unitNum);
            s_logger.debug("* dial string: {}", dialString);
            s_logger.debug("* persist: {}", persist);
            s_logger.debug("* maxfail: {}", maxFail);
            s_logger.debug("* idle: {}", idle);
            s_logger.debug("* active-filter: {}", activeFilter);
            s_logger.debug("* LCP Echo Interval: {}", lcpEchoInterval);
            s_logger.debug("* LCP Echo Failure: {}", lcpEchoFailure);

            // Get the auth type and credentials
            // pppd will use CHAP if available, else PAP
            password = "";
            if (isGsmGprsUmtsHspa) {
                String chapSecret = ChapLinux.getInstance().getSecret(model, username, "*", "*");
                String papSecret = PapLinux.getInstance().getSecret(model, username, "*", "*");
                if (chapSecret != null && papSecret != null && chapSecret.equals(papSecret)) {
                    authType = AuthType.AUTO;
                    password = chapSecret;
                } else if (chapSecret != null) {
                    authType = AuthType.CHAP;
                    password = chapSecret;
                } else if (papSecret != null) {
                    authType = AuthType.PAP;
                    password = papSecret;
                }

                s_logger.debug("* APN: {}", apn);
                s_logger.debug("* auth: {}", authType);
                s_logger.debug("* username: {}", username);
                s_logger.debug("* password: {}", password);
            }
        }

        boolean gpsEnabled = false;
        StringBuilder key = new StringBuilder().append("net.interface.").append(ifaceName).append(".config.gpsEnabled");
        String statusString = KuranetConfig.getProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            gpsEnabled = Boolean.parseBoolean(statusString);
        }

        int resetTout = 5;
        key = new StringBuilder().append("net.interface.").append(ifaceName).append(".config.resetTimeout");
        statusString = KuranetConfig.getProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            resetTout = Integer.parseInt(statusString);
        }

        // Populate the modem config
        ModemConfig modemConfig = new ModemConfig();

        modemConfig.setPppNumber(unitNum);
        modemConfig.setPersist(persist);
        modemConfig.setMaxFail(maxFail);
        modemConfig.setIdle(idle);
        modemConfig.setActiveFilter(activeFilter);
        modemConfig.setLcpEchoInterval(lcpEchoInterval);
        modemConfig.setLcpEchoFailure(lcpEchoFailure);
        modemConfig.setEnabled(enabled);    // TODO - from self configuring properties
        modemConfig.setDataCompression(0);  // FIXME
        modemConfig.setDialString(dialString);
        modemConfig.setHeaderCompression(0);    // FIXME
        modemConfig.setGpsEnabled(gpsEnabled);
        modemConfig.setResetTimeout(resetTout);

        if (isGsmGprsUmtsHspa) {
            modemConfig.setApn(apn);
            modemConfig.setAuthType(authType);
            modemConfig.setPassword(password);
            modemConfig.setPdpType(PdpType.valueOf(pdpType.toUpperCase()));
            modemConfig.setUsername(username);
        }

        return modemConfig;
    }

    private static NetConfigIP4 getNetConfigIP4(String interfaceName) throws KuraException {
        NetConfigIP4 netConfigIP4 = null;
        NetInterfaceStatus netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;

        StringBuilder key = new StringBuilder().append("net.interface.").append(interfaceName)
                .append(".config.ip4.status");
        String statusString = KuranetConfig.getProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            netInterfaceStatus = NetInterfaceStatus.valueOf(statusString);
        }
        s_logger.debug("Setting NetInterfaceStatus to " + netInterfaceStatus + " for " + interfaceName);

        netConfigIP4 = new NetConfigIP4(netInterfaceStatus, true, true);

        key = new StringBuilder("net.interface.").append(interfaceName).append(".config.dnsServers");
        String dnsServersStr = KuranetConfig.getProperty(key.toString());
        List<IP4Address> dnsServersList = new ArrayList<IP4Address>();

        if (dnsServersStr != null && !dnsServersStr.isEmpty()) {
            String[] serversArr = dnsServersStr.split(PppConfigWriter.DNS_DELIM);
            for (String server : serversArr) {
                try {
                    dnsServersList.add((IP4Address) IPAddress.parseHostAddress(server));
                } catch (UnknownHostException e) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
            }
            netConfigIP4.setDnsServers(dnsServersList);
        }

        return netConfigIP4;
    }

    private static String getPeerFilename(String interfaceName, UsbDevice usbDevice) {
        String filename = null;

        // if interfaceName is a usb port address
        if (interfaceName.matches(NetworkConfigurationServiceImpl.UNCONFIGURED_MODEM_REGEX)) {
            filename = PppConfigWriter.formPeerFilename(usbDevice);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(PEERS_DIRECTORY).append(interfaceName);
            filename = sb.toString();
        }

        return filename;
    }

    /**
     * Remove quotes if they exist at the start and end of a string.
     * Otherwise, return the string with no changes.
     *
     * @param line
     * @return line without surrounding quotes
     */
    private static String removeQuotes(String line) {
        if (line != null) {
            if (line.startsWith("'") && line.endsWith("'")      // remove surrounding quotes
                    || line.startsWith("\"") && line.endsWith("\"")) {

                return line.substring(1, line.length() - 1);
            }
        }

        return line;
    }

    private static int getUnitNum(String interfaceName) {
        int unitNum = -1;

        if (interfaceName.matches("(?i)ppp\\d+")) {
            unitNum = Integer.parseInt(interfaceName.substring(3));
        } else {
            unitNum = getUnitNum();
        }

        return unitNum;
    }

    private static int getUnitNum() {

        int unit = 0;
        try {
            File peersFolder = new File(PEERS_DIRECTORY).getCanonicalFile();
            String[] files = peersFolder.list(new FileFilter());
            if (files != null && files.length > 0) {
                int[] pppUnitNumbers = new int[files.length];

                for (int i = 0; i < pppUnitNumbers.length; i++) {
                    pppUnitNumbers[i] = Integer.parseInt(files[i].substring(3));
                }
                Arrays.sort(pppUnitNumbers);
                boolean found = false;
                for (int i = 0; i < pppUnitNumbers.length; i++) {
                    if (i < pppUnitNumbers[i]) {
                        found = true;
                        unit = i;
                        break;
                    }
                }
                if (!found) {
                    unit = pppUnitNumbers.length;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return unit;
    }
}
