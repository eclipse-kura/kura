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
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostapdConfigReader extends WifiConfigReaderHelper implements NetworkConfigurationVisitor {

    private static final String WPA_PAIRWISE = "wpa_pairwise";

    private static final Logger logger = LoggerFactory.getLogger(HostapdConfigReader.class);

    private static HostapdConfigReader instance;

    public static HostapdConfigReader getInstance() {
        if (instance == null) {
            instance = new HostapdConfigReader();
        }

        return instance;
    }

    @Override
    public WifiConfig getWifiConfig(String ifaceName) throws KuraException {
        try {
            WifiConfig wifiConfig = new WifiConfig();
            wifiConfig.setMode(WifiMode.MASTER);

            File configFile = getFinalFile(ifaceName);
            logger.debug("parsing hostapd config file: {}", configFile.getAbsolutePath());

            if (configFile.exists()) {
                Properties hostapdProps = parseHostapdConfigFile(configFile);

                String iface = hostapdProps.getProperty("interface");

                if (ifaceName != null && ifaceName.equals(iface)) {
                    String driver = hostapdProps.getProperty("driver");
                    String essid = hostapdProps.getProperty("ssid");
                    int channel = Integer.parseInt(hostapdProps.getProperty("channel"));
                    int ignoreSSID = Integer.parseInt(hostapdProps.getProperty("ignore_broadcast_ssid"));

                    WifiRadioMode wifiRadioMode = getRadioMode(hostapdProps);

                    // Determine security and pass
                    WifiSecurity security = WifiSecurity.SECURITY_NONE;
                    String password = "";

                    if (hostapdProps.containsKey("wpa")) {
                        security = getWifiSecurity(hostapdProps);
                        password = getWifiPassword(hostapdProps);
                    } else if (hostapdProps.containsKey("wep_key0")) {
                        security = WifiSecurity.SECURITY_WEP;
                        password = hostapdProps.getProperty("wep_key0");
                    }

                    WifiCiphers pairwise = getWifiCiphers(hostapdProps);

                    // Populate the config
                    wifiConfig.setSSID(essid);
                    wifiConfig.setDriver(driver);
                    wifiConfig.setChannels(new int[] { channel });
                    wifiConfig.setPasskey(password);
                    wifiConfig.setSecurity(security);
                    wifiConfig.setPairwiseCiphers(pairwise);
                    wifiConfig.setRadioMode(wifiRadioMode);

                    if (ignoreSSID == 0) {
                        wifiConfig.setIgnoreSSID(false);
                        wifiConfig.setBroadcast(true);
                    } else {
                        wifiConfig.setIgnoreSSID(true);
                        wifiConfig.setBroadcast(false);
                    }

                    setWifiRadioMode(wifiConfig, wifiRadioMode);
                }
            } else {
                logger.warn("getWifiHostConfig() :: {} file doesn't exist, will generate default wifiConfig",
                        configFile.getName());
                wifiConfig.setSSID("kura_gateway");
                wifiConfig.setDriver("nl80211");
                wifiConfig.setChannels(new int[] { 11 });
                wifiConfig.setPasskey("");
                wifiConfig.setSecurity(WifiSecurity.SECURITY_NONE);
                wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP);
                wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
                wifiConfig.setIgnoreSSID(false);
                wifiConfig.setBroadcast(true);
                wifiConfig.setHardwareMode("b");
            }
            return wifiConfig;
        } catch (KuraException | IOException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e, "Malformed config file : {}",
                    getFinalFile(ifaceName).getAbsolutePath());
        }
    }

    private void setWifiRadioMode(WifiConfig wifiConfig, WifiRadioMode wifiRadioMode) {
        // hw mode
        if (wifiRadioMode == WifiRadioMode.RADIO_MODE_80211b) {
            wifiConfig.setHardwareMode("b");
        } else if (wifiRadioMode == WifiRadioMode.RADIO_MODE_80211g) {
            wifiConfig.setHardwareMode("g");
        } else if (wifiRadioMode == WifiRadioMode.RADIO_MODE_80211nHT20
                || wifiRadioMode == WifiRadioMode.RADIO_MODE_80211nHT40above
                || wifiRadioMode == WifiRadioMode.RADIO_MODE_80211nHT40below) {

            // TODO: specify these 'n' modes separately?
            wifiConfig.setHardwareMode("n");
        }
    }

    private WifiCiphers getWifiCiphers(Properties hostapdProps) throws KuraException {
        WifiCiphers pairwise = null;
        if (hostapdProps.containsKey(WPA_PAIRWISE)) {
            if ("TKIP".equals(hostapdProps.getProperty(WPA_PAIRWISE))) {
                pairwise = WifiCiphers.TKIP;
            } else if ("CCMP".equals(hostapdProps.getProperty(WPA_PAIRWISE))) {
                pairwise = WifiCiphers.CCMP;
            } else if ("CCMP TKIP".equals(hostapdProps.getProperty(WPA_PAIRWISE))) {
                pairwise = WifiCiphers.CCMP_TKIP;
            } else {
                throw KuraException.internalError("malformatted config file");
            }
        }
        return pairwise;
    }

    private String getWifiPassword(Properties hostapdProps) throws KuraException {
        String password = "";
        if (hostapdProps.containsKey("wpa_passphrase")) {
            password = hostapdProps.getProperty("wpa_passphrase");
        } else if (hostapdProps.containsKey("wpa_psk")) {
            password = hostapdProps.getProperty("wpa_psk");
        } else {
            throw KuraException.internalError("malformatted config file, no wpa passphrase");
        }
        return password;
    }

    private WifiSecurity getWifiSecurity(Properties hostapdProps) throws KuraException {
        WifiSecurity security;
        if ("1".equals(hostapdProps.getProperty("wpa"))) {
            security = WifiSecurity.SECURITY_WPA;
        } else if ("2".equals(hostapdProps.getProperty("wpa"))) {
            security = WifiSecurity.SECURITY_WPA2;
        } else if ("3".equals(hostapdProps.getProperty("wpa"))) {
            security = WifiSecurity.SECURITY_WPA_WPA2;
        } else {
            throw KuraException.internalError("malformatted config file");
        }
        return security;
    }

    private WifiRadioMode getRadioMode(Properties hostapdProps) throws KuraException {
        // Determine radio mode
        WifiRadioMode wifiRadioMode = null;
        String hwModeStr = hostapdProps.getProperty("hw_mode");
        if ("a".equals(hwModeStr)) {
            wifiRadioMode = WifiRadioMode.RADIO_MODE_80211a;
        } else if ("b".equals(hwModeStr)) {
            wifiRadioMode = WifiRadioMode.RADIO_MODE_80211b;
        } else if ("g".equals(hwModeStr)) {
            wifiRadioMode = WifiRadioMode.RADIO_MODE_80211g;
            if ("1".equals(hostapdProps.getProperty("ieee80211n"))) {
                wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT20;
                String htCapab = hostapdProps.getProperty("ht_capab");
                if (htCapab != null) {
                    if (htCapab.contains("HT40+")) {
                        wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT40above;
                    } else if (htCapab.contains("HT40-")) {
                        wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT40below;
                    }
                }
            }
        } else {
            throw KuraException.internalError("malformatted config file, unexpected hw_mode");
        }
        return wifiRadioMode;
    }

    private Properties parseHostapdConfigFile(File configFile) throws IOException {
        Properties hostapdProps = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            hostapdProps.load(fis);
        }
        // remove any quotes around the values
        Enumeration<Object> keys = hostapdProps.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            String val = hostapdProps.getProperty(key);
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
                hostapdProps.setProperty(key, val.substring(1, val.length() - 1));
            }
        }
        return hostapdProps;
    }

    protected File getFinalFile(String ifaceName) {
        return new File(HostapdManager.getHostapdConfigFileName(ifaceName));
    }
}
