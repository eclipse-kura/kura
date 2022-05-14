/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.net;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiConfigurationInterpreter {

    private static final Logger logger = LoggerFactory.getLogger(WifiConfigurationInterpreter.class);

    private static final String BGSCAN = ".bgscan";
    private static final String HARDWARE_MODE = ".hardwareMode";
    private static final String WIFI_PASSPHRASE_KEY = ".passphrase";
    private static final String SECURITY_TYPE = ".securityType";
    private static final String NET_INTERFACE = "net.interface.";
    private static final String WIFI_CHANNELS_KEY = ".channel";
    private static final String WIFI_IGNORE_SSID_KEY = ".ignoreSSID";
    private static final String WIFI_PING_ACCESS_POINT_KEY = ".pingAccessPoint";
    private static final String WIFI_RADIO_MODE_KEY = ".radioMode";
    private static final String WIFI_GROUP_CIPHERS_KEY = ".groupCiphers";
    private static final String WIFI_PAIRWISE_CIPHERS_KEY = ".pairwiseCiphers";
    private static final String DRIVER_KEY = ".driver";
    private static final String WIFI_SSID_KEY = ".ssid";

    private WifiConfigurationInterpreter() {

    }

    public static List<NetConfig> populateConfiguration(Map<String, Object> props, String interfaceName)
            throws KuraException {
        List<NetConfig> netConfigs = new ArrayList<>();

        if (isNull(props)) {
            return netConfigs;
        }

        StringBuilder sbPrefix = new StringBuilder();
        sbPrefix.append(NET_INTERFACE).append(interfaceName).append(".").append("config.");

        String netIfConfigPrefix = sbPrefix.toString();

        netConfigs.add(getWifiConfig(netIfConfigPrefix, WifiMode.MASTER, props));

        netConfigs.add(getWifiConfig(netIfConfigPrefix, WifiMode.INFRA, props));

        return netConfigs;

    }

    public static WifiMode getWifiMode(Map<String, Object> props, String interfaceName) {
        StringBuilder sbPrefix = new StringBuilder();
        sbPrefix.append(NET_INTERFACE).append(interfaceName).append(".");

        String configWifiMode = sbPrefix.append("config.").toString() + "wifi.mode";
        WifiMode mode = WifiMode.MASTER;
        if (!isNull(props) && props.containsKey(configWifiMode) && props.get(configWifiMode) != null) {
            mode = WifiMode.valueOf((String) props.get(configWifiMode));
        }
        return mode;
    }

    private static WifiConfig getWifiConfig(String netIfConfigPrefix, WifiMode mode, Map<String, Object> properties)
            throws KuraException {
        String prefix = new StringBuilder(netIfConfigPrefix).append("wifi.").append(mode.toString().toLowerCase())
                .toString();

        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(mode);

        String ssid = getSsid(properties, prefix);
        if (!isNull(ssid)) {
            wifiConfig.setSSID(ssid);
        }

        String driver = getDriver(properties, prefix);
        if (!isNull(driver)) {
            wifiConfig.setDriver(driver);
        }

        wifiConfig.setSecurity(getWifiSecurity(properties, prefix));

        wifiConfig.setChannels(getWifiChannels(properties, prefix));

        String psswdObj = getWifiPassphrase(properties, prefix);
        if (!isNull(psswdObj)) {
            wifiConfig.setPasskey(psswdObj);
        }

        String hwMode = getHwMode(properties, prefix);
        if (!isNull(hwMode)) {
            wifiConfig.setHardwareMode(hwMode);
        }

        wifiConfig.setIgnoreSSID(isSsidIgnored(properties, prefix));

        String pairwiseCiphers = getPairwiseCiphers(properties, prefix);
        if (!isNull(pairwiseCiphers)) {
            wifiConfig.setPairwiseCiphers(WifiCiphers.valueOf(pairwiseCiphers));
        }

        if (mode == WifiMode.INFRA) {
            wifiConfig.setBgscan(getBgScan(properties, prefix));

            String groupCiphers = getGroupCiphers(properties, prefix);
            if (!isNull(groupCiphers)) {
                wifiConfig.setGroupCiphers(WifiCiphers.valueOf(groupCiphers));
            }

            wifiConfig.setPingAccessPoint(isPingAccessPoint(properties, prefix));

        }

        String radioMode = getRadioMode(properties, prefix);
        if (!isNull(radioMode)) {
            try {
                wifiConfig.setRadioMode(WifiRadioMode.valueOf(radioMode));
            } catch (IllegalArgumentException e) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID,
                        "Could not parse wifi radio mode " + radioMode);
            }
        }

        return wifiConfig;

    }

    private static String getRadioMode(Map<String, Object> properties, String prefix) {
        return (String) properties.get(prefix + WIFI_RADIO_MODE_KEY);
    }

    private static boolean isPingAccessPoint(Map<String, Object> properties, String prefix) {
        boolean pingAccessPoint = false;
        Object value = properties.get(prefix + WIFI_PING_ACCESS_POINT_KEY);
        if (!isNull(value) && value instanceof Boolean) {
            pingAccessPoint = (Boolean) value;
        }
        return pingAccessPoint;
    }

    private static String getGroupCiphers(Map<String, Object> properties, String prefix) {
        return (String) properties.get(prefix + WIFI_GROUP_CIPHERS_KEY);
    }

    private static WifiBgscan getBgScan(Map<String, Object> properties, String prefix) {
        String bgscan = (String) properties.get(prefix + BGSCAN);
        if (bgscan == null) {
            bgscan = "";
        }
        return new WifiBgscan(bgscan);
    }

    private static String getPairwiseCiphers(Map<String, Object> properties, String prefix) {
        return (String) properties.get(prefix + WIFI_PAIRWISE_CIPHERS_KEY);
    }

    private static boolean isSsidIgnored(Map<String, Object> properties, String prefix) {
        boolean ignoreSSID = false;
        Object value = properties.get(prefix + WIFI_IGNORE_SSID_KEY);
        if (!isNull(value) && value instanceof Boolean) {
            ignoreSSID = (Boolean) value;
        }
        return ignoreSSID;
    }

    private static String getHwMode(Map<String, Object> properties, String prefix) {
        return (String) properties.get(prefix + HARDWARE_MODE);
    }

    private static String getWifiPassphrase(Map<String, Object> properties, String prefix) {
        Object psswdObj = properties.get(prefix + WIFI_PASSPHRASE_KEY);
        String passphrase = null;
        if (psswdObj instanceof Password) {
            Password psswd = (Password) psswdObj;
            passphrase = new String(psswd.getPassword());
        } else if (psswdObj instanceof String) {
            passphrase = (String) psswdObj;
        }
        return passphrase;
    }

    private static int[] getWifiChannels(Map<String, Object> properties, String prefix) {
        String channelsString = (String) properties.get(prefix + WIFI_CHANNELS_KEY);

        if (!isNull(channelsString) && !channelsString.trim().isEmpty()) {

            StringTokenizer st = new StringTokenizer(channelsString.trim(), " ");
            int tokens = st.countTokens();
            if (tokens > 0) {
                int[] channels = new int[tokens];
                for (int i = 0; i < tokens; i++) {
                    String token = st.nextToken();
                    try {
                        channels[i] = Integer.parseInt(token);
                    } catch (NumberFormatException e) {
                        logger.error("Error parsing channels!", e);
                    }
                }
                return channels;
            }
        }

        return new int[] { 1 };
    }

    private static WifiSecurity getWifiSecurity(Map<String, Object> properties, String prefix) throws KuraException {

        String securityString = (String) properties.get(prefix + SECURITY_TYPE);
        if (securityString != null && !securityString.isEmpty()) {
            try {
                return WifiSecurity.valueOf(securityString);
            } catch (IllegalArgumentException e) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID,
                        "Could not parse wifi security " + securityString);
            }
        }
        return WifiSecurity.NONE;
    }

    private static String getDriver(Map<String, Object> properties, String prefix) {
        String key = prefix + DRIVER_KEY;
        return (String) properties.get(key);
    }

    private static String getSsid(Map<String, Object> properties, String prefix) {
        String key = prefix + WIFI_SSID_KEY;
        return (String) properties.get(key);
    }

}
