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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantManager;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.WpaSupplicantUtil;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpaSupplicantConfigReader implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(WpaSupplicantConfigReader.class);

    private static WpaSupplicantConfigReader instance;

    public static WpaSupplicantConfigReader getInstance() {
        if (instance == null) {
            instance = new WpaSupplicantConfigReader();
        }

        return instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getNetInterfaceConfigs();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                getConfig((WifiInterfaceConfigImpl) netInterfaceConfig);
            }
        }
    }

    private void getConfig(WifiInterfaceConfigImpl wifiInterfaceConfig) throws KuraException {
        String interfaceName = wifiInterfaceConfig.getName();
        logger.debug("Getting wpa_supplicant config for {}", interfaceName);

        List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = wifiInterfaceConfig.getNetInterfaceAddresses();

        if (wifiInterfaceAddressConfigs == null || wifiInterfaceAddressConfigs.isEmpty()) {
            wifiInterfaceAddressConfigs = new ArrayList<>();
            wifiInterfaceAddressConfigs.add(new WifiInterfaceAddressConfigImpl());
            wifiInterfaceConfig.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);
        }

        WifiInterfaceAddressConfig wifiInterfaceAddressConfig = wifiInterfaceAddressConfigs.get(0);
        if (wifiInterfaceAddressConfig != null
                && wifiInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
            List<NetConfig> netConfigs = wifiInterfaceAddressConfig.getConfigs();

            if (netConfigs == null) {
                netConfigs = new ArrayList<>();
                ((WifiInterfaceAddressConfigImpl) wifiInterfaceAddressConfig).setNetConfigs(netConfigs);
            }

            // Get infrastructure config
            netConfigs.add(getWifiClientConfig(interfaceName, WifiMode.INFRA));
        }
    }

    private WifiConfig getWifiClientConfig(String ifaceName, WifiMode wifiMode) throws KuraException {

        WifiConfig wifiConfig = new WifiConfig();

        String ssid = "";
        WifiSecurity wifiSecurity = WifiSecurity.NONE;
        String pass = "";
        int[] channels = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 };
        WifiBgscan bgscan = new WifiBgscan("");
        WifiCiphers pairwiseCiphers = null;
        WifiCiphers groupCiphers = null;

        // Get properties from config file
        Properties props = parseConfigFile(ifaceName);

        if (props == null) {
            logger.warn("WPA in client mode is not configured");
        } else {
            ssid = props.getProperty("ssid");
            if (ssid == null) {
                logger.warn("WPA in client mode is not configured");
            } else {
                logger.debug("current wpa_supplicant.conf: ssid={}", ssid);

                // wifi mode
                int currentMode = props.getProperty("mode") != null ? Integer.parseInt(props.getProperty("mode"))
                        : WpaSupplicantUtil.MODE_INFRA;
                logger.debug("current wpa_supplicant.conf: mode={}", currentMode);

                switch (wifiMode) {
                case INFRA:
                    channels = getChannels(channels, props);
                    break;

                case MASTER:
                    throw KuraException
                            .internalError("failed to get wpa_supplicant configuration: MASTER mode is invalid");
                default:
                    throw KuraException
                            .internalError("failed to get wpa_supplicant configuration: invalid mode: " + wifiMode);
                }

                String proto = props.getProperty("proto");
                if (proto != null) {
                    logger.debug("current wpa_supplicant.conf: proto={}", proto);
                }

                pairwiseCiphers = getCipher(props, "pairwise");
                groupCiphers = getCipher(props, "group");

                // security
                String keyMgmt = props.getProperty("key_mgmt");
                logger.debug("current wpa_supplicant.conf: key_mgmt={}", keyMgmt);
                if (keyMgmt != null && "WPA-PSK".equalsIgnoreCase(keyMgmt)) {
                    pass = props.getProperty("psk");
                    wifiSecurity = getSecurityFromProto(proto);
                } else {
                    pass = props.getProperty("wep_key0");
                    wifiSecurity = (pass != null) ? WifiSecurity.SECURITY_WEP : WifiSecurity.SECURITY_NONE;

                    pairwiseCiphers = null;
                    groupCiphers = null;
                }
                if (pass == null) {
                    pass = "";
                }

                String sBgscan = props.getProperty("bgscan");
                if (sBgscan != null) {
                    logger.debug("current wpa_supplicant.conf: bgscan={}", sBgscan);
                    bgscan = new WifiBgscan(sBgscan);
                }
            }
        }

        // Populate the config
        wifiConfig.setMode(wifiMode);
        wifiConfig.setSSID(ssid);
        wifiConfig.setSecurity(wifiSecurity);
        wifiConfig.setPasskey(pass);
        wifiConfig.setHardwareMode("");
        wifiConfig.setPairwiseCiphers(pairwiseCiphers);
        wifiConfig.setGroupCiphers(groupCiphers);
        wifiConfig.setChannels(channels);
        wifiConfig.setBgscan(bgscan);

        // Get self-stored properties

        boolean pingAP = false;
        StringBuilder key = new StringBuilder().append("net.interface.").append(ifaceName)
                .append(".config.wifi.infra.pingAccessPoint");
        String statusString = getKuranetProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            pingAP = Boolean.parseBoolean(statusString);
        }
        wifiConfig.setPingAccessPoint(pingAP);

        boolean ignoreSSID = false;
        key = new StringBuilder().append("net.interface.").append(ifaceName).append(".config.wifi.infra.ignoreSSID");
        statusString = getKuranetProperty(key.toString());
        if (statusString != null && !statusString.isEmpty()) {
            ignoreSSID = Boolean.parseBoolean(statusString);
        }
        wifiConfig.setIgnoreSSID(ignoreSSID);

        StringBuilder infraDriverKey = new StringBuilder("net.interface.").append(ifaceName)
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

    protected int[] getChannels(int[] defaultChannels, Properties props) {
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

        BufferedReader br = null;
        try {
            File wpaConfigFile = new File(getWpaSupplicantConfigFilename(ifaceName));
            if (wpaConfigFile.exists()) {

                // Read into a string
                br = new BufferedReader(new FileReader(wpaConfigFile));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }

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
                            if (val != null) {
                                if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
                                    props.setProperty(key, val.substring(1, val.length() - 1));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw KuraException.internalError("wpa_supplicant failed to parse its configuration file");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    logger.error("I/O Exception while closing BufferedReader!");
                }
            }
        }

        return props;
    }

    protected String getWpaSupplicantConfigFilename(String ifaceName) {
        return WpaSupplicantManager.getWpaSupplicantConfigFilename(ifaceName);
    }
}
