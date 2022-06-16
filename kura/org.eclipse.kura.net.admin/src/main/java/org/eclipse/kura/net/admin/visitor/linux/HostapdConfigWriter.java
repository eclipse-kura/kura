/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import static org.eclipse.kura.net.admin.visitor.linux.WriterHelper.copyFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.util.IwCapabilityTool;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostapdConfigWriter extends AbstractLinuxConfigWriter {

    private static final Logger logger = LoggerFactory.getLogger(HostapdConfigWriter.class);

    private static final int FIRST_5000MHZ_CHANNEL = 32;

    private static final String HEXES = "0123456789ABCDEF";

    private static final String KURA_SECURITY = "KURA_SECURITY";
    private static final String KURA_PAIRWISE_CIPHER = "KURA_PAIRWISE_CIPHER";
    private static final String KURA_HTCAPAB = "KURA_HTCAPAB";
    private static final String HT_CAPAB_KURA_HTCAPAB = "ht_capab=KURA_HTCAPAB";
    private static final String KURA_IEEE80211N = "KURA_IEEE80211N";
    private static final String KURA_WME_ENABLED = "KURA_WME_ENABLED";
    private static final String KURA_WMM_ENABLED = "KURA_WMM_ENABLED";
    private static final String KURA_HW_MODE = "KURA_HW_MODE";
    private static final String KURA_COUNTRY_CODE = "KURA_COUNTRY_CODE";
    private static final String KURA_IEEE80211D = "KURA_IEEE80211D";
    private static final String KURA_IEEE80211H = "KURA_IEEE80211H";
    private static final String KURA_IEEE80211AC = "KURA_IEEE80211AC";
    private static final String KURA_IEEE80211AC_FULL_LINE = "ieee80211ac=KURA_IEEE80211AC";

    private static final String HOSTAPD_TMP_CONFIG_FILE = "/etc/hostapd.conf.tmp";

    private CommandExecutorService executorService;

    public HostapdConfigWriter() {
        // Do nothing...
    }

    @Override
    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    protected void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {

        if (netInterfaceConfig == null) {
            return;
        }
        List<NetConfig> netConfigs = ((AbstractNetInterface<?>) netInterfaceConfig).getNetConfigs();
        if (netConfigs.isEmpty()) {
            return;
        }
        String interfaceName = netInterfaceConfig.getName();
        WifiConfig apConfig = getAccessPointConfig(netConfigs);
        if (!((AbstractNetInterface<?>) netInterfaceConfig).isInterfaceEnabled()) {
            logger.info("Network interface status for {} is {} - not overwriting hostapd configuration file",
                    interfaceName, ((AbstractNetInterface<?>) netInterfaceConfig).getInterfaceStatus());
            return;
        }
        if (apConfig != null) {
            try {
                generateHostapdConf(apConfig, interfaceName);
            } catch (Exception e) {
                logger.error("Failed to generate hostapd configuration file for {} interface", interfaceName);
                throw KuraException.internalError(e);
            }
        }
    }

    private WifiConfig getAccessPointConfig(List<NetConfig> netConfigs) {
        if (netConfigs == null) {
            return null;
        }
        WifiConfig apConfig = null;
        for (NetConfig netConfig : netConfigs) {
            if (netConfig instanceof WifiConfig && ((WifiConfig) netConfig).getMode() == WifiMode.MASTER) {
                apConfig = (WifiConfig) netConfig;
                break;
            }
        }
        return apConfig;
    }

    private void generateHostapdConf(WifiConfig wifiConfig, String interfaceName) throws KuraException, IOException {

        logger.debug("Generating Hostapd Config");

        String fileAsString;

        if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_NONE || wifiConfig.getSecurity() == WifiSecurity.NONE) {
            fileAsString = readResource("/src/main/resources/wifi/hostapd.conf_no_security");

        } else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WEP) {
            fileAsString = readResource("/src/main/resources/wifi/hostapd.conf_wep");

            fileAsString = updateWepPassKey(wifiConfig, fileAsString);
        } else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA
                || wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA2
                || wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA_WPA2) {

            fileAsString = readResource("/src/main/resources/wifi/hostapd.conf_master_wpa_wpa2_psk");
            fileAsString = updateWPA(wifiConfig, fileAsString);
        } else {
            logger.error(
                    "Unsupported security type: {}. It must be WifiSecurity.NONE, WifiSecurity.SECURITY_NONE, "
                            + "WifiSecurity.SECURITY_WEP, WifiSecurity.SECURITY_WPA, or WifiSecurity.SECURITY_WPA2",
                    wifiConfig.getSecurity());
            throw KuraException.internalError("unsupported security type: " + wifiConfig.getSecurity());
        }

        // common updates
        fileAsString = updateInterfaceAndDriver(interfaceName, wifiConfig.getDriver(), fileAsString);

        fileAsString = updateSsid(wifiConfig, fileAsString);

        fileAsString = updateRadioMode(wifiConfig, fileAsString);

        fileAsString = updateChannels(wifiConfig, fileAsString);

        fileAsString = updateIgnoreBroadcastSsid(wifiConfig, fileAsString);

        fileAsString = updateCountryCode(interfaceName, fileAsString);

        File outputFile = getTemporaryFile();

        // everything is set and we haven't failed - write the file
        copyFile(fileAsString, outputFile.toPath());

        // move the file if we made it this far
        moveFile(interfaceName);
    }

    private String updateCountryCode(String interfaceName, String fileAsString) throws KuraException {

        String countryCode = getWifiCountryCode();

        if (countryCode != null && !countryCode.isEmpty() && !countryCode.equalsIgnoreCase("00")) {
            fileAsString = fileAsString.replaceFirst(KURA_COUNTRY_CODE, countryCode);
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211D, "1");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211H, isDfs(interfaceName) ? "1" : "0");
        } else {
            fileAsString = fileAsString.replaceFirst(KURA_COUNTRY_CODE, "");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211D, "0");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211H, "0");
        }

        return fileAsString;
    }

    private String updateWPA(WifiConfig wifiConfig, String fileAsString) throws KuraException {
        if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA) {
            fileAsString = fileAsString.replaceFirst(KURA_SECURITY, "1");
        } else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA2) {
            fileAsString = fileAsString.replaceFirst(KURA_SECURITY, "2");
        } else if (wifiConfig.getSecurity() == WifiSecurity.SECURITY_WPA_WPA2) {
            fileAsString = fileAsString.replaceFirst(KURA_SECURITY, "3");
        } else {
            throw KuraException.internalError("invalid WiFi Security");
        }

        WifiCiphers wifiCiphers = wifiConfig.getPairwiseCiphers();
        if (wifiCiphers == WifiCiphers.TKIP) {
            fileAsString = fileAsString.replaceFirst(KURA_PAIRWISE_CIPHER, "TKIP");
        } else if (wifiCiphers == WifiCiphers.CCMP) {
            fileAsString = fileAsString.replaceFirst(KURA_PAIRWISE_CIPHER, "CCMP");
        } else if (wifiCiphers == WifiCiphers.CCMP_TKIP) {
            fileAsString = fileAsString.replaceFirst(KURA_PAIRWISE_CIPHER, "CCMP TKIP");
        } else {
            throw KuraException.internalError("invalid WiFi Pairwise Ciphers");
        }

        String passKey = new String(wifiConfig.getPasskey().getPassword());
        if (wifiConfig.getPasskey() != null && passKey.trim().length() > 0) {
            if (passKey.length() < 8 || passKey.length() > 63) {
                throw KuraException.internalError(
                        "the WPA passphrase (passwd) must be between 8 (inclusive) and 63 (inclusive) characters in length: "
                                + wifiConfig.getPasskey());
            } else {
                fileAsString = fileAsString.replaceFirst("KURA_PASSPHRASE", Matcher.quoteReplacement(passKey.trim()));
            }
        } else {
            throw KuraException.internalError("the passwd can not be null");
        }
        return fileAsString;
    }

    private String updateWepPassKey(WifiConfig wifiConfig, String fileAsString) throws KuraException {
        String passKey = new String(wifiConfig.getPasskey().getPassword());

        String exceptionHex = "the WEP key (passwd) must be all HEX characters (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a, b, c, d, e, and f";

        if (passKey.length() == 10) {
            // check to make sure it is all hex
            try {
                Long.parseLong(passKey, 16);
            } catch (NumberFormatException e) {
                throw KuraException.internalError(exceptionHex);
            }
        } else if (passKey.length() == 26) {
            String part1 = passKey.substring(0, 13);
            String part2 = passKey.substring(13);

            try {
                Long.parseLong(part1, 16);
                Long.parseLong(part2, 16);
            } catch (NumberFormatException e) {
                throw KuraException.internalError(exceptionHex);
            }
        } else if (passKey.length() == 32) {
            String part1 = passKey.substring(0, 10);
            String part2 = passKey.substring(10, 20);
            String part3 = passKey.substring(20);
            try {
                Long.parseLong(part1, 16);
                Long.parseLong(part2, 16);
                Long.parseLong(part3, 16);
            } catch (Exception e) {
                throw KuraException.internalError(exceptionHex);
            }
        } else if (passKey.length() == 5 || passKey.length() == 13 || passKey.length() == 16) {
            // 5, 13, or 16 ASCII characters
            passKey = toHex(passKey);
        } else {
            throw KuraException.internalError("the WEP key (passwd) must be 10, 26, or 32 HEX characters in length");
        }

        // since we're here - save the password
        fileAsString = fileAsString.replaceFirst("KURA_WEP_KEY", passKey);

        return fileAsString;
    }

    private String updateSsid(WifiConfig wifiConfig, String fileAsString) throws KuraException {
        if (wifiConfig.getSSID() != null) {
            fileAsString = fileAsString.replaceFirst("KURA_ESSID", Matcher.quoteReplacement(wifiConfig.getSSID()));
        } else {
            throw KuraException.internalError("the essid can not be null");
        }
        return fileAsString;
    }

    private String updateInterfaceAndDriver(String interfaceName, String interfaceDriver, String fileAsString)
            throws KuraException {
        if (interfaceName != null) {
            fileAsString = fileAsString.replaceFirst("KURA_INTERFACE", interfaceName);
        } else {
            throw KuraException.internalError("the interface name can not be null");
        }
        if (interfaceDriver != null && interfaceDriver.length() > 0) {
            fileAsString = fileAsString.replaceFirst("KURA_DRIVER", interfaceDriver);
        } else {
            String drv = new HostapdManager(this.executorService).getDriver(interfaceName);
            logger.warn("The 'driver' parameter must be set: setting to: {}", drv);
            fileAsString = fileAsString.replaceFirst("KURA_DRIVER", drv);
        }
        return fileAsString;
    }

    private String updateChannels(WifiConfig wifiConfig, String fileAsString) throws KuraException {

        if (wifiConfig.getChannels()[0] >= 0 && wifiConfig.getChannels()[0] <= 165) {
            fileAsString = fileAsString.replaceFirst("KURA_CHANNEL", Integer.toString(wifiConfig.getChannels()[0]));
        } else {
            throw KuraException.internalError(String.format(
                    "the channel (%s) must be between 1 (inclusive) and 196 (inclusive) depending on your locale, or 0 for automatic.",
                    wifiConfig.getChannels()[0]));
        }
        return fileAsString;
    }

    private String updateIgnoreBroadcastSsid(WifiConfig wifiConfig, String fileAsString) {
        if (wifiConfig.ignoreSSID()) {
            fileAsString = fileAsString.replaceFirst("KURA_IGNORE_BROADCAST_SSID", "2");
        } else {
            fileAsString = fileAsString.replaceFirst("KURA_IGNORE_BROADCAST_SSID", "0");
        }
        return fileAsString;
    }

    /**
     * Updates configuration with appropriate values.
     *
     * @param wifiConfig
     *            config
     * @param fileAsString
     *            complete configuration string
     * @return
     * @throws KuraException
     *             in case of invalid mode
     */
    private String updateRadioMode(WifiConfig wifiConfig, String fileAsString) throws KuraException {
        WifiRadioMode radioMode = wifiConfig.getRadioMode();

        if (radioMode == null) {
            throw KuraException.internalError("invalid hardware mode");
        }

        switch (radioMode) {

        case RADIO_MODE_80211a:
            fileAsString = fileAsString.replaceFirst(KURA_HW_MODE, "a");
            fileAsString = fileAsString.replaceFirst(KURA_WME_ENABLED, "0");

            if (wifiConfig.getChannels().length >= 1 && wifiConfig.getChannels()[0] >= FIRST_5000MHZ_CHANNEL) {
                fileAsString = fileAsString.replaceFirst(KURA_IEEE80211N, "1");
            } else {
                fileAsString = fileAsString.replaceFirst(KURA_IEEE80211N, "0");
            }

            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211AC_FULL_LINE, "#" + KURA_IEEE80211AC_FULL_LINE);
            fileAsString = fileAsString.replaceFirst(HT_CAPAB_KURA_HTCAPAB, "");

            break;

        case RADIO_MODE_80211b:
            fileAsString = fileAsString.replaceFirst(KURA_HW_MODE, "b");
            fileAsString = fileAsString.replaceFirst(KURA_WME_ENABLED, "0");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211N, "0");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211AC_FULL_LINE, "#" + KURA_IEEE80211AC_FULL_LINE);
            fileAsString = fileAsString.replaceFirst(HT_CAPAB_KURA_HTCAPAB, "");

            break;

        case RADIO_MODE_80211g:
            fileAsString = fileAsString.replaceFirst(KURA_HW_MODE, "g");
            fileAsString = fileAsString.replaceFirst(KURA_WME_ENABLED, "0");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211N, "0");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211AC_FULL_LINE, "#" + KURA_IEEE80211AC_FULL_LINE);
            fileAsString = fileAsString.replaceFirst(HT_CAPAB_KURA_HTCAPAB, "");

            break;

        case RADIO_MODE_80211nHT20:
            fileAsString = fileAsString.replaceFirst(KURA_HW_MODE, "g");
            fileAsString = fileAsString.replaceFirst(KURA_WME_ENABLED, "1");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211N, "1");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211AC_FULL_LINE, "#" + KURA_IEEE80211AC_FULL_LINE);
            fileAsString = fileAsString.replaceFirst(KURA_HTCAPAB, "[SHORT-GI-20]");

            break;

        case RADIO_MODE_80211nHT40above:
            fileAsString = fileAsString.replaceFirst(KURA_HW_MODE, "g");
            fileAsString = fileAsString.replaceFirst(KURA_WME_ENABLED, "1");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211N, "1");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211AC_FULL_LINE, "#" + KURA_IEEE80211AC_FULL_LINE);
            fileAsString = fileAsString.replaceFirst(KURA_HTCAPAB, "[HT40+][SHORT-GI-20][SHORT-GI-40]");

            break;

        case RADIO_MODE_80211nHT40below:
            fileAsString = fileAsString.replaceFirst(KURA_HW_MODE, "g");
            fileAsString = fileAsString.replaceFirst(KURA_WME_ENABLED, "1");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211N, "1");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211AC_FULL_LINE, "#" + KURA_IEEE80211AC_FULL_LINE);
            fileAsString = fileAsString.replaceFirst(KURA_HTCAPAB, "[HT40-][SHORT-GI-20][SHORT-GI-40]");

            break;

        case RADIO_MODE_80211_AC:
            fileAsString = fileAsString.replaceFirst(KURA_HW_MODE, "a");
            fileAsString = fileAsString.replaceFirst(KURA_WME_ENABLED, "1");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211N, "1");
            fileAsString = fileAsString.replaceFirst(KURA_IEEE80211AC, "1");

            fileAsString = fileAsString.replaceFirst(HT_CAPAB_KURA_HTCAPAB, "");

            fileAsString = fileAsString.replaceFirst(HT_CAPAB_KURA_HTCAPAB, "");

            break;

        default:
            throw KuraException.internalError("invalid hardware mode");

        }

        fileAsString = fileAsString.replaceFirst(KURA_WMM_ENABLED, "1");

        return fileAsString;
    }

    protected String readResource(String path) throws IOException {
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        return IOUtil.readResource(bundle, path);
    }

    protected File getFinalFile(String ifaceName) {
        return new File(HostapdManager.getHostapdConfigFileName(ifaceName));
    }

    protected File getTemporaryFile() {
        return new File(HOSTAPD_TMP_CONFIG_FILE);
    }

    private void moveFile(String ifaceName) throws KuraException {
        File tmpFile = getTemporaryFile();
        File file = getFinalFile(ifaceName);
        if (tmpFile.renameTo(file)) {
            logger.trace("Successfully wrote hostapd.conf file");
        } else {
            logger.error("Failed to write hostapd.conf file");
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "error while building up new configuration file for hostapd");
        }
    }

    /*
     * This method converts supplied string to hex
     */
    private String toHex(String s) {
        if (s == null) {
            return null;
        }
        byte[] raw = s.getBytes();

        StringBuilder hex = new StringBuilder(2 * raw.length);
        for (byte element : raw) {
            hex.append(HEXES.charAt((element & 0xF0) >> 4)).append(HEXES.charAt(element & 0x0F));
        }
        return hex.toString();
    }

    private String getWifiCountryCode() throws KuraException {
        return IwCapabilityTool.getWifiCountryCode(this.executorService);
    }

    private boolean isDfs(String interfaceName) throws KuraException {
        return IwCapabilityTool.probeCapabilities(interfaceName, this.executorService).contains(Capability.DFS);
    }

    @Override
    protected CommandExecutorService getExecutorService() {
        return this.executorService;
    }

    @Override
    protected boolean acceptMode(WifiMode mode) {
        return mode == WifiMode.MASTER;
    }

}