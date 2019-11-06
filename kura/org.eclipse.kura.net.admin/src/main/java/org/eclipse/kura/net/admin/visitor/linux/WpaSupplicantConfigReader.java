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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantManager;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.WpaSupplicantUtil;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpaSupplicantConfigReader extends WifiConfigReaderHelper implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(WpaSupplicantConfigReader.class);
    private static final String NET_INTERFACE = "net.interface.";
    private static final int[] defaultChannels = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 };

    private static WpaSupplicantConfigReader instance;

    public static WpaSupplicantConfigReader getInstance() {
        if (instance == null) {
            instance = new WpaSupplicantConfigReader();
        }

        return instance;
    }

    @Override
    public WifiConfig getWifiConfig(String ifaceName) throws KuraException {

        WifiConfig wifiConfig = buildDefaultWifiConfig();

        // Get properties from config file
        Properties props = parseConfigFile(ifaceName);

        if (props == null) {
            logger.warn("WPA in client mode is not configured");
        } else {
            buildWifiConfig(wifiConfig, props);
        }

        // Get self-stored properties

        boolean pingAP = false;
        StringBuilder key = new StringBuilder().append(NET_INTERFACE).append(ifaceName)
                .append(".config.wifi.infra.pingAccessPoint");
        String statusString = getKuranetProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            pingAP = Boolean.parseBoolean(statusString);
        }
        wifiConfig.setPingAccessPoint(pingAP);

        boolean ignoreSSID = false;
        key = new StringBuilder().append(NET_INTERFACE).append(ifaceName).append(".config.wifi.infra.ignoreSSID");
        statusString = getKuranetProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            ignoreSSID = Boolean.parseBoolean(statusString);
        }
        wifiConfig.setIgnoreSSID(ignoreSSID);

        StringBuilder infraDriverKey = new StringBuilder(NET_INTERFACE).append(ifaceName)
                .append(".config.wifi.infra.driver");
        String wifiDriver = getKuranetProperty(infraDriverKey.toString());
        if (wifiDriver == null || wifiDriver.isEmpty()) {
            wifiDriver = "nl80211";
        }
        wifiConfig.setDriver(wifiDriver);

        return wifiConfig;
    }

    protected WifiSecurity getSecurityFromProto(String proto) {
        WifiSecurity wifiSecurity = WifiSecurity.NONE;

        if (proto != null) {
            proto = proto.trim();

            if ("WPA".equals(proto)) {
                wifiSecurity = WifiSecurity.SECURITY_WPA;
            } else if ("RSN".equals(proto)) {
                wifiSecurity = WifiSecurity.SECURITY_WPA2;
            } else if ("WPA RSN".equals(proto)) {
                wifiSecurity = WifiSecurity.SECURITY_WPA_WPA2;
            }
        } else {
            wifiSecurity = WifiSecurity.SECURITY_WPA2;
        }

        return wifiSecurity;
    }

    protected WifiCiphers getCipher(Properties props, String key) {
        WifiCiphers cipher = null;

        String val = props.getProperty(key);
        if (val != null) {
            logger.debug("current wpa_supplicant.conf: {}={}", key, val);

            if (val.contains(WifiCiphers.toString(WifiCiphers.CCMP_TKIP))) {
                cipher = WifiCiphers.CCMP_TKIP;
            } else if (val.contains(WifiCiphers.toString(WifiCiphers.TKIP))) {
                cipher = WifiCiphers.TKIP;
            } else if (val.contains(WifiCiphers.toString(WifiCiphers.CCMP))) {
                cipher = WifiCiphers.CCMP;
            }
        }

        return cipher;
    }

    protected int[] getChannels(Properties props) {
        int[] channels = defaultChannels;

        String scanFreq = props.getProperty("scan_freq");
        if (scanFreq != null) {
            logger.debug("current wpa_supplicant.conf: scan_freq={}", scanFreq);
            String[] saScanFreq = scanFreq.split(" ");
            channels = new int[saScanFreq.length];
            for (int i = 0; i < channels.length; i++) {
                try {
                    channels[i] = WpaSupplicantUtil.convFrequencyToChannel(Integer.parseInt(saScanFreq[i]));
                } catch (NumberFormatException e) {
                    // don't worry
                }
            }
        }

        return channels;
    }

    protected String getKuranetProperty(String key) {
        return KuranetConfig.getProperty(key);
    }

    private Properties parseConfigFile(String ifaceName) throws KuraException {

        Properties props = null;
        File wpaConfigFile = new File(getWpaSupplicantConfigFilename(ifaceName));
        if (wpaConfigFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(wpaConfigFile))) {
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }

                props = getProperties(sb);
            } catch (IOException e) {
                throw KuraException.internalError("wpa_supplicant failed to parse its configuration file");
            }
        }

        return props;
    }

    private Properties getProperties(StringBuilder sb) throws IOException {
        Properties props = null;
        String newConfig = null;
        int beginIndex = sb.toString().indexOf("network");
        int endIndex = sb.toString().indexOf('}');
        if (beginIndex >= 0 && endIndex > beginIndex) {
            newConfig = sb.toString().substring(beginIndex, endIndex);
            beginIndex = newConfig.indexOf('{');
            if (beginIndex >= 0) {
                newConfig = newConfig.substring(beginIndex + 1);
                props = new Properties();
                props.load(new StringReader(newConfig));
                Enumeration<Object> keys = props.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement().toString();
                    String val = props.getProperty(key);
                    if (val != null && val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
                        props.setProperty(key, val.substring(1, val.length() - 1));
                    }
                }
            }
        }
        return props;
    }

    protected String getWpaSupplicantConfigFilename(String ifaceName) {
        return WpaSupplicantManager.getWpaSupplicantConfigFilename(ifaceName);
    }

    private WifiConfig buildDefaultWifiConfig() {
        WifiConfig wifiConfig = new WifiConfig();

        // Populate the wifi config with default values
        wifiConfig.setMode(WifiMode.INFRA);
        wifiConfig.setSSID("");
        wifiConfig.setSecurity(WifiSecurity.NONE);
        wifiConfig.setPasskey("");
        wifiConfig.setHardwareMode("");
        wifiConfig.setPairwiseCiphers(null);
        wifiConfig.setGroupCiphers(null);
        wifiConfig.setChannels(defaultChannels);
        wifiConfig.setBgscan(new WifiBgscan(""));

        return wifiConfig;
    }

    private void buildWifiConfig(WifiConfig wifiConfig, Properties props) {
        String ssid = props.getProperty("ssid");
        if (ssid == null) {
            logger.warn("WPA in client mode is not configured");
        } else {
            logger.debug("current wpa_supplicant.conf: ssid={}", ssid);
            wifiConfig.setSSID(ssid);

            // wifi mode
            int currentMode = props.getProperty("mode") != null ? Integer.parseInt(props.getProperty("mode"))
                    : WpaSupplicantUtil.MODE_INFRA;
            logger.debug("current wpa_supplicant.conf: mode={}", currentMode);

            wifiConfig.setChannels(getChannels(props));

            setWifiConfigSecurity(wifiConfig, props);

            String sBgscan = props.getProperty("bgscan");
            if (sBgscan != null) {
                logger.debug("current wpa_supplicant.conf: bgscan={}", sBgscan);
                wifiConfig.setBgscan(new WifiBgscan(sBgscan));
            }
        }
    }

    private void setWifiConfigSecurity(WifiConfig wifiConfig, Properties props) {
        String proto = props.getProperty("proto");
        if (proto != null) {
            logger.debug("current wpa_supplicant.conf: proto={}", proto);
        }

        wifiConfig.setPairwiseCiphers(getCipher(props, "pairwise"));
        wifiConfig.setGroupCiphers(getCipher(props, "group"));

        // security
        String keyMgmt = props.getProperty("key_mgmt");
        String pass = "";
        WifiSecurity wifiSecurity = null;
        logger.debug("current wpa_supplicant.conf: key_mgmt={}", keyMgmt);
        if (keyMgmt != null && "WPA-PSK".equalsIgnoreCase(keyMgmt)) {
            pass = props.getProperty("psk");
            wifiSecurity = getSecurityFromProto(proto);
        } else {
            pass = props.getProperty("wep_key0");
            wifiSecurity = (pass != null) ? WifiSecurity.SECURITY_WEP : WifiSecurity.SECURITY_NONE;
            wifiConfig.setPairwiseCiphers(null);
            wifiConfig.setGroupCiphers(null);
        }
        if (pass == null) {
            pass = "";
        }
        wifiConfig.setPasskey(pass);
        wifiConfig.setSecurity(wifiSecurity);
    }
}
