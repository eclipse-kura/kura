/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
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
import java.util.Objects;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
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

    private static final String NET_INTERFACE = "net.interface.";

    private static class FileFilter implements FilenameFilter {

        @Override
        public boolean accept(File directory, String filename) {
            return filename.startsWith("ppp");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(PppConfigReader.class);

    public static final String PEERS_DIRECTORY = "/etc/ppp/peers/";
    public static final String SCRIPTS_DIRECTORY = "/etc/ppp/scripts/";

    private static PppConfigReader instance;

    public static PppConfigReader getInstance() {
        if (instance == null) {
            instance = new PppConfigReader();
        }

        return instance;
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        // Not needed
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

    protected String getKuranetProperty(String key) {
        return KuranetConfig.getProperty(key);
    }

    private void getConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {

        String interfaceName = netInterfaceConfig.getName();
        logger.debug("Getting ppp config for {}", interfaceName);

        if (netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
            StringBuilder key = new StringBuilder(NET_INTERFACE + netInterfaceConfig.getName() + ".modem.identifier");
            String modemId = getKuranetProperty(key.toString());
            logger.debug("Getting modem identifier using key {} : {}", key, modemId);

            if (modemId != null) {
                ((ModemInterfaceConfigImpl) netInterfaceConfig).setModemIdentifier(modemId);
            }
        }

        NetInterfaceAddressConfig netInterfaceAddressConfig = ((AbstractNetInterface<?>) netInterfaceConfig)
                .getNetInterfaceAddressConfig();
        if (netInterfaceAddressConfig instanceof ModemInterfaceAddressConfigImpl) {
            addNetConfigs((ModemInterfaceAddressConfigImpl) netInterfaceAddressConfig,
                    netInterfaceConfig.getUsbDevice(), interfaceName);
        }
    }

    private void addNetConfigs(ModemInterfaceAddressConfigImpl netInterfaceAddressConfig, UsbDevice usbDevice,
            String interfaceName) throws KuraException {
        List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();

        if (netConfigs == null) {
            netConfigs = new ArrayList<>();
            netInterfaceAddressConfig.setNetConfigs(netConfigs);
        }

        // Create a ModemConfig
        ModemConfig modemConfig = getModemConfig(interfaceName, usbDevice);
        netConfigs.add(modemConfig);

        // Create a NetConfigIP4
        netConfigs.add(getNetConfigIP4(interfaceName));

        // Populate with DNS provided by PPP (displayed as read-only in Denali)
        List<? extends IPAddress> pppDnsServers = getPppDnServers();
        if (pppDnsServers != null) {
            netInterfaceAddressConfig.setDnsServers(pppDnsServers);
        }
    }

    protected List<IPAddress> getPppDnServers() throws KuraException {
        return LinuxDns.getInstance().getPppDnServers();
    }

    private ModemConfig getModemConfig(String ifaceName, UsbDevice usbDevice) throws KuraException {
        logger.debug("parsePppPeerConfig()");

        List<ModemTechnologyType> technologyTypes = getModemTechnologyTypes(usbDevice);

        boolean isGsmGprsUmtsHspa = isGsmGprsUmtsHspa(technologyTypes);

        boolean enabled = true;
        int unitNum = getUnitNum(ifaceName);

        String apn = "";
        String pdpType = "UNKNOWN";
        String dialString = "";
        String username = "";
        String pass = "";
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

            logger.warn("getModemConfig() :: PPPD peer file does not exist - {}", peerFilename);
        } else {
            logger.debug("getModemConfig() :: PPPD peer file exists - {}", peerFilename);

            Properties props = loadPeerFileProperties(peerFilename);

            logger.debug("peer properties: {}", props);

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

            String chatFilename = getChatFileName(props);

            ModemXchangeScript connectScript = getConnectScript(chatFilename);

            if (connectScript != null) {
                ModemXchangePair modemXchangePair = connectScript.getFirstModemXchangePair();
                ModemXchangePair prevXchangePair = null;

                while (modemXchangePair != null) {
                    String expectedStr = modemXchangePair.getExpectString();

                    if ("CONNECT".equals(expectedStr) && prevXchangePair != null) {
                        // dial string
                        dialString = removeQuotes(prevXchangePair.getSendString());
                    }

                    prevXchangePair = modemXchangePair;
                    modemXchangePair = connectScript.getNextModemXchangePair();
                }
            }

            pdpType = getPdpType(ifaceName);
            apn = getApn(ifaceName);

            logger.debug("* Enabled: {}", enabled);
            logger.debug("* CHAT file: {}", chatFilename);
            logger.debug("* UnitNum: {}", unitNum);
            logger.debug("* APN: {}", apn);
            logger.debug("* dial string: {}", dialString);
            logger.debug("* persist: {}", persist);
            logger.debug("* maxfail: {}", maxFail);
            logger.debug("* idle: {}", idle);
            logger.debug("* active-filter: {}", activeFilter);
            logger.debug("* LCP Echo Interval: {}", lcpEchoInterval);
            logger.debug("* LCP Echo Failure: {}", lcpEchoFailure);

            // Get the auth type and credentials
            // pppd will use CHAP if available, else PAP
            if (isGsmGprsUmtsHspa) {
                String model = checkIsPeerSymlink("", peerFile);

                String chapSecret = ChapLinux.getInstance().getSecret(model, username, "*", "*");
                String papSecret = PapLinux.getInstance().getSecret(model, username, "*", "*");

                if (chapSecret != null && papSecret != null && chapSecret.equals(papSecret)) {
                    authType = AuthType.AUTO;
                    pass = chapSecret;
                } else if (chapSecret != null) {
                    authType = AuthType.CHAP;
                    pass = chapSecret;
                } else if (papSecret != null) {
                    authType = AuthType.PAP;
                    pass = papSecret;
                }

                logger.debug("* auth: {}", authType);
                logger.debug("* username: {}", username);
                logger.debug("* password: {}", pass);
            }
        }

        boolean gpsEnabled = isGpsEnabled(ifaceName);
        boolean diversityEnabled = isDiversityEnabled(ifaceName);

        int resetTout = getResetTimeout(ifaceName);
        // Populate the modem config
        ModemConfig modemConfig = new ModemConfig();

        modemConfig.setPppNumber(unitNum);
        modemConfig.setPersist(persist);
        modemConfig.setMaxFail(maxFail);
        modemConfig.setIdle(idle);
        modemConfig.setActiveFilter(activeFilter);
        modemConfig.setLcpEchoInterval(lcpEchoInterval);
        modemConfig.setLcpEchoFailure(lcpEchoFailure);
        modemConfig.setEnabled(enabled); // TODO - from self configuring properties
        modemConfig.setDataCompression(0); // FIXME
        modemConfig.setDialString(dialString);
        modemConfig.setHeaderCompression(0); // FIXME
        modemConfig.setGpsEnabled(gpsEnabled);
        modemConfig.setDiversityEnabled(diversityEnabled);
        modemConfig.setResetTimeout(resetTout);

        if (isGsmGprsUmtsHspa) {
            modemConfig.setApn(apn);
            modemConfig.setAuthType(authType);
            modemConfig.setPassword(pass);
            modemConfig.setPdpType(PdpType.valueOf(pdpType.toUpperCase()));
            modemConfig.setUsername(username);
        }

        return modemConfig;
    }

    private ModemXchangeScript getConnectScript(String chatFilename) throws KuraException {
        // Parse the connect script
        ModemXchangeScript connectScript = null;

        try {
            connectScript = ModemXchangeScript.parseFile(chatFilename);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Error parsing " + chatFilename, e);
        }

        return connectScript;
    }

    private String getChatFileName(Properties props) {
        String chatFilename = "";

        String connectProperty = removeQuotes(props.getProperty("connect"));
        String[] args = connectProperty.split("\\s+");
        for (int i = 0; i < args.length; i++) {
            if ("-f".equals(args[i]) && args.length > i + 1) {
                chatFilename = args[i + 1];
                break;
            }
        }

        return chatFilename;
    }

    private Properties loadPeerFileProperties(String peerFilename) throws KuraException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(peerFilename);) {
            props.load(fis);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Error getting modem config", e);
        }

        return props;
    }

    private int getResetTimeout(String ifaceName) {
        int resetTout = 5;
        StringBuilder key = new StringBuilder().append(NET_INTERFACE).append(ifaceName).append(".config.resetTimeout");
        String statusString = getKuranetProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            resetTout = Integer.parseInt(statusString);
        }
        return resetTout;
    }

    private boolean isGpsEnabled(String ifaceName) {
        boolean gpsEnabled = false;

        StringBuilder key = new StringBuilder().append(NET_INTERFACE).append(ifaceName).append(".config.gpsEnabled");
        String statusString = getKuranetProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            gpsEnabled = Boolean.parseBoolean(statusString);
        }

        return gpsEnabled;
    }

    private boolean isDiversityEnabled(String ifaceName) {
        boolean diversityEnabled = false;

        StringBuilder key = new StringBuilder().append(NET_INTERFACE).append(ifaceName)
                .append(".config.diversityEnabled");
        String statusString = getKuranetProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            diversityEnabled = Boolean.parseBoolean(statusString);
        }

        return diversityEnabled;
    }

    private String getPdpType(String ifaceName) {
        StringBuilder key = new StringBuilder().append(NET_INTERFACE).append(ifaceName).append(".config.pdpType");
        String pdpType = getKuranetProperty(key.toString());
        return Objects.toString(pdpType, ModemConfig.PdpType.UNKNOWN.name());
    }

    private String getApn(String ifaceName) {
        StringBuilder key = new StringBuilder().append(NET_INTERFACE).append(ifaceName).append(".config.apn");
        String apn = getKuranetProperty(key.toString());
        return Objects.toString(apn, "");
    }

    private String checkIsPeerSymlink(String defaultModel, File peerFile) {
        String model = defaultModel;

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
            logger.error("Error checking for symlink", e);
        }

        return model;
    }

    private boolean isGsmGprsUmtsHspa(List<ModemTechnologyType> technologyTypes) {
        boolean isGsmGprsUmtsHspa = false;

        if (technologyTypes != null) {
            for (ModemTechnologyType technologyType : technologyTypes) {
                if (technologyType == ModemTechnologyType.GSM_GPRS || technologyType == ModemTechnologyType.UMTS
                        || technologyType == ModemTechnologyType.HSDPA || technologyType == ModemTechnologyType.HSPA) {
                    isGsmGprsUmtsHspa = true;
                    break;
                }
            }
        }

        return isGsmGprsUmtsHspa;
    }

    private List<ModemTechnologyType> getModemTechnologyTypes(UsbDevice usbDevice) {
        List<ModemTechnologyType> technologyTypes = null;
        if (usbDevice != null) {
            SupportedUsbModemInfo usbModemInfo = SupportedUsbModemsInfo.getModem(usbDevice);
            if (usbModemInfo != null) {
                technologyTypes = usbModemInfo.getTechnologyTypes();
            }
        }
        return technologyTypes;
    }

    private NetConfigIP4 getNetConfigIP4(String interfaceName) throws KuraException {
        NetInterfaceStatus netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;

        StringBuilder key = new StringBuilder().append(NET_INTERFACE).append(interfaceName)
                .append(".config.ip4.status");
        String statusString = getKuranetProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            netInterfaceStatus = NetInterfaceStatus.valueOf(statusString);
        }
        logger.debug("Setting NetInterfaceStatus to {} for {}", netInterfaceStatus, interfaceName);

        NetConfigIP4 netConfigIP4 = new NetConfigIP4(netInterfaceStatus, true, true);

        key = new StringBuilder(NET_INTERFACE).append(interfaceName).append(".config.dnsServers");
        String dnsServersStr = getKuranetProperty(key.toString());
        List<IP4Address> dnsServersList = new ArrayList<>();

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

    protected String getPeerFilename(String interfaceName, UsbDevice usbDevice) {
        String filename;

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
    private String removeQuotes(String line) {
        if (line != null) {
            if (line.startsWith("'") && line.endsWith("'") // remove surrounding quotes
                    || line.startsWith("\"") && line.endsWith("\"")) {

                return line.substring(1, line.length() - 1);
            }
        }

        return line;
    }

    private int getUnitNum(String interfaceName) {
        int unitNum;

        if (interfaceName.matches("(?i)ppp\\d+")) {
            unitNum = Integer.parseInt(interfaceName.substring(3));
        } else {
            unitNum = getUnitNum();
        }

        return unitNum;
    }

    private int getUnitNum() {

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
            logger.info(e.getMessage(), e);
        }

        return unit;
    }
}
