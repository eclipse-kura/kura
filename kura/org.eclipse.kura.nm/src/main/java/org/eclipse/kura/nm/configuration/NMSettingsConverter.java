/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.nm.configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.nm.KuraIpStatus;
import org.eclipse.kura.nm.NetworkProperties;
import org.eclipse.kura.nm.enums.NMDeviceType;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMSettingsConverter {

    private static final Logger logger = LoggerFactory.getLogger(NMSettingsConverter.class);

    private static final String NM_SETTINGS_CONNECTION = "connection";
    private static final String NM_SETTINGS_IPV4_METHOD = "method";
    private static final String NM_SETTINGS_IPV6_METHOD = "method";

    private static final String PPP_REFUSE_EAP = "refuse-eap";
    private static final String PPP_REFUSE_CHAP = "refuse-chap";
    private static final String PPP_REFUSE_PAP = "refuse-pap";
    private static final String PPP_REFUSE_MSCHAP = "refuse-mschap";
    private static final String PPP_REFUSE_MSCHAPV2 = "refuse-mschapv2";

    private static final int NM_WEP_KEY_TYPE_KEY = 1;

    private static final String KURA_PROPS_KEY_WIFI_MODE = "net.interface.%s.config.wifi.mode";
    private static final String KURA_PROPS_KEY_WIFI_SECURITY_TYPE = "net.interface.%s.config.wifi.%s.securityType";

    private NMSettingsConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static Map<String, Map<String, Variant<?>>> buildSettings(NetworkProperties properties,
            Optional<Connection> oldConnection, String deviceId, String iface, NMDeviceType deviceType) {
        Map<String, Map<String, Variant<?>>> newConnectionSettings = new HashMap<>();

        Map<String, Variant<?>> connectionMap = buildConnectionSettings(oldConnection, iface, deviceType);
        newConnectionSettings.put(NM_SETTINGS_CONNECTION, connectionMap);

        Map<String, Variant<?>> ipv4Map = NMSettingsConverter.buildIpv4Settings(properties, deviceId);
        Map<String, Variant<?>> ipv6Map = NMSettingsConverter.buildIpv6Settings(properties, deviceId);
        newConnectionSettings.put("ipv4", ipv4Map);
        newConnectionSettings.put("ipv6", ipv6Map);

        if (deviceType == NMDeviceType.NM_DEVICE_TYPE_WIFI) {
            Map<String, Variant<?>> wifiSettingsMap = NMSettingsConverter.build80211WirelessSettings(properties,
                    deviceId);
            newConnectionSettings.put("802-11-wireless", wifiSettingsMap);

            String propMode = properties.get(String.class, KURA_PROPS_KEY_WIFI_MODE, deviceId);
            String securityType = properties.get(String.class, KURA_PROPS_KEY_WIFI_SECURITY_TYPE, deviceId,
                    propMode.toLowerCase());
            if (!"NONE".equals(securityType)) {
                // Only populate "802-11-wireless-security" field if security is enabled
                Map<String, Variant<?>> wifiSecuritySettingsMap = NMSettingsConverter
                        .build80211WirelessSecuritySettings(properties, deviceId);
                newConnectionSettings.put("802-11-wireless-security", wifiSecuritySettingsMap);
            }
        } else if (deviceType == NMDeviceType.NM_DEVICE_TYPE_MODEM) {
            Map<String, Variant<?>> gsmSettingsMap = NMSettingsConverter.buildGsmSettings(properties, deviceId);
            Map<String, Variant<?>> pppSettingsMap = NMSettingsConverter.buildPPPSettings(properties, deviceId);
            newConnectionSettings.put("gsm", gsmSettingsMap);
            newConnectionSettings.put("ppp", pppSettingsMap);
        }

        return newConnectionSettings;
    }

    public static Map<String, Variant<?>> buildIpv4Settings(NetworkProperties props, String deviceId) {
        Map<String, Variant<?>> settings = new HashMap<>();

        Boolean dhcpClient4Enabled = props.get(Boolean.class, "net.interface.%s.config.dhcpClient4.enabled", deviceId);

        KuraIpStatus ip4Status = KuraIpStatus
                .fromString(props.get(String.class, "net.interface.%s.config.ip4.status", deviceId));

        if (Boolean.FALSE.equals(dhcpClient4Enabled)) {
            settings.put(NM_SETTINGS_IPV4_METHOD, new Variant<>("manual"));

            String address = props.get(String.class, "net.interface.%s.config.ip4.address", deviceId);
            Short prefix = props.get(Short.class, "net.interface.%s.config.ip4.prefix", deviceId);

            Map<String, Variant<?>> addressEntry = new HashMap<>();
            addressEntry.put("address", new Variant<>(address));
            addressEntry.put("prefix", new Variant<>(new UInt32(prefix)));

            if (ip4Status.equals(KuraIpStatus.ENABLEDWAN)) {
                Optional<String> gateway = props.getOpt(String.class, "net.interface.%s.config.ip4.gateway", deviceId);
                gateway.ifPresent(gatewayAddress -> settings.put("gateway", new Variant<>(gatewayAddress)));
            }

            List<Map<String, Variant<?>>> addressData = Arrays.asList(addressEntry);
            settings.put("address-data", new Variant<>(addressData, "aa{sv}"));
        } else {
            settings.put(NM_SETTINGS_IPV4_METHOD, new Variant<>("auto"));
        }

        if (ip4Status.equals(KuraIpStatus.ENABLEDLAN)) {
            settings.put("ignore-auto-dns", new Variant<>(true));
            settings.put("ignore-auto-routes", new Variant<>(true));
        } else if (ip4Status.equals(KuraIpStatus.ENABLEDWAN)) {
            Optional<List<String>> dnsServers = props.getOptStringList("net.interface.%s.config.ip4.dnsServers",
                    deviceId);
            if (dnsServers.isPresent()) {
                settings.put("dns", new Variant<>(convertIp4(dnsServers.get()), "au"));
                settings.put("ignore-auto-dns", new Variant<>(true));
            }

            Optional<Integer> wanPriority = props.getOpt(Integer.class, "net.interface.%s.config.ip4.wan.priority",
                    deviceId);
            if (wanPriority.isPresent()) {
                Long supportedByNM = wanPriority.get().longValue();
                settings.put("route-metric", new Variant<>(supportedByNM));
            }
        } else {
            logger.warn("Unexpected ip status received: \"{}\". Ignoring", ip4Status);
        }

        return settings;
    }

    public static Map<String, Variant<?>> buildIpv6Settings(NetworkProperties props, String deviceId) {
        Map<String, Variant<?>> settings = new HashMap<>();

        // Disabled for now
        settings.put(NM_SETTINGS_IPV6_METHOD, new Variant<>("disabled"));

        return settings;
    }

    public static Map<String, Variant<?>> build80211WirelessSettings(NetworkProperties props, String deviceId) {
        Map<String, Variant<?>> settings = new HashMap<>();

        String propMode = props.get(String.class, KURA_PROPS_KEY_WIFI_MODE, deviceId);

        String mode = wifiModeConvert(propMode);
        settings.put("mode", new Variant<>(mode));

        String ssid = props.get(String.class, "net.interface.%s.config.wifi.%s.ssid", deviceId, propMode.toLowerCase());
        settings.put("ssid", new Variant<>(ssid.getBytes(StandardCharsets.UTF_8)));

        short channel = Short.parseShort(
                props.get(String.class, "net.interface.%s.config.wifi.%s.channel", deviceId, propMode.toLowerCase()));
        settings.put("channel", new Variant<>(new UInt32(channel)));

        Optional<String> band = wifiBandConvert(
                props.get(String.class, "net.interface.%s.config.wifi.%s.radioMode", deviceId, propMode.toLowerCase()),
                channel);
        band.ifPresent(bandString -> settings.put("band", new Variant<>(bandString)));

        Optional<Boolean> hidden = props.getOpt(Boolean.class, "net.interface.%s.config.wifi.%s.ignoreSSID", deviceId,
                propMode.toLowerCase());
        hidden.ifPresent(hiddenString -> settings.put("hidden", new Variant<>(hiddenString)));

        return settings;
    }

    public static Map<String, Variant<?>> build80211WirelessSecuritySettings(NetworkProperties props, String deviceId) {
        String propMode = props.get(String.class, KURA_PROPS_KEY_WIFI_MODE, deviceId);
        String securityType = props.get(String.class, KURA_PROPS_KEY_WIFI_SECURITY_TYPE, deviceId,
                propMode.toLowerCase());

        if ("SECURITY_WEP".equals(securityType)) {
            return createWEPSettings(props, deviceId, propMode);
        } else if ("SECURITY_WPA".equals(securityType) || "SECURITY_WPA2".equals(securityType)
                || "SECURITY_WPA_WPA2".equals(securityType)) {
            return createWPAWPA2Settings(props, deviceId, propMode);
        } else {
            throw new IllegalArgumentException("Security type \"" + securityType + "\" is not supported.");
        }
    }

    private static Map<String, Variant<?>> createWEPSettings(NetworkProperties props, String deviceId,
            String propMode) {
        Map<String, Variant<?>> settings = new HashMap<>();

        settings.put("key-mgmt", new Variant<>("none"));
        settings.put("wep-key-type", new Variant<>(NM_WEP_KEY_TYPE_KEY));

        String wepKey = props
                .get(Password.class, "net.interface.%s.config.wifi.%s.passphrase", deviceId, propMode.toLowerCase())
                .toString();
        settings.put("wep-key0", new Variant<>(wepKey));

        return settings;
    }

    private static Map<String, Variant<?>> createWPAWPA2Settings(NetworkProperties props, String deviceId,
            String propMode) {
        Map<String, Variant<?>> settings = new HashMap<>();

        settings.put("key-mgmt", new Variant<>("wpa-psk"));

        String psk = props
                .get(Password.class, "net.interface.%s.config.wifi.%s.passphrase", deviceId, propMode.toLowerCase())
                .toString();
        settings.put("psk", new Variant<>(psk));

        String securityType = props.get(String.class, KURA_PROPS_KEY_WIFI_SECURITY_TYPE, deviceId,
                propMode.toLowerCase());
        List<String> proto = wifiProtoConvert(securityType);
        settings.put("proto", new Variant<>(proto, "as"));

        Optional<String> group = props.getOpt(String.class, "net.interface.%s.config.wifi.%s.groupCiphers", deviceId,
                propMode.toLowerCase());
        if (group.isPresent()) {
            List<String> nmGroup = wifiCipherConvert(group.get());
            settings.put("group", new Variant<>(nmGroup, "as"));
        }

        Optional<String> pairwise = props.getOpt(String.class, "net.interface.%s.config.wifi.%s.pairwiseCiphers",
                deviceId, propMode.toLowerCase());
        if (pairwise.isPresent()) {
            List<String> nmPairwise = wifiCipherConvert(pairwise.get());
            settings.put("pairwise", new Variant<>(nmPairwise, "as"));
        }

        return settings;
    }

    public static Map<String, Variant<?>> buildGsmSettings(NetworkProperties props, String deviceId) {
        Map<String, Variant<?>> settings = new HashMap<>();

        String apn = props.get(String.class, "net.interface.%s.config.apn", deviceId);
        settings.put("apn", new Variant<>(apn));

        Optional<String> username = props.getOpt(String.class, "net.interface.%s.config.username", deviceId);
        username.ifPresent(usernameString -> settings.put("username", new Variant<>(usernameString)));

        Optional<Password> password = props.getOpt(Password.class, "net.interface.%s.config.password", deviceId);
        password.ifPresent(passwordString -> settings.put("password", new Variant<>(passwordString.toString())));

        Optional<String> number = props.getOpt(String.class, "net.interface.%s.config.dialString", deviceId);
        number.ifPresent(numberString -> settings.put("number", new Variant<>(numberString)));

        return settings;
    }

    public static Map<String, Variant<?>> buildPPPSettings(NetworkProperties props, String deviceId) {
        Map<String, Variant<?>> settings = new HashMap<>();

        Optional<Integer> lcpEchoInterval = props.getOpt(Integer.class, "net.interface.%s.config.lcpEchoInterval",
                deviceId);
        lcpEchoInterval.ifPresent(interval -> settings.put("lcp-echo-interval", new Variant<>(interval)));
        Optional<Integer> lcpEchoFailure = props.getOpt(Integer.class, "net.interface.%s.config.lcpEchoFailure",
                deviceId);
        lcpEchoFailure.ifPresent(failure -> settings.put("lcp-echo-failure", new Variant<>(failure)));

        Optional<String> authType = props.getOpt(String.class, "net.interface.%s.config.authType", deviceId);
        authType.ifPresent(authenticationType -> setAuthenticationType(authenticationType, settings));

        return settings;
    }

    public static Map<String, Variant<?>> buildConnectionSettings(Optional<Connection> connection, String iface,
            NMDeviceType deviceType) {
        Map<String, Variant<?>> connectionMap = new HashMap<>();

        if (!connection.isPresent()) {
            connectionMap = createConnectionSettings(iface);
            connectionMap.put("type", new Variant<>(connectionTypeConvert(deviceType)));
        } else {
            Map<String, Map<String, Variant<?>>> connectionSettings = connection.get().GetSettings();
            for (String key : connectionSettings.get(NM_SETTINGS_CONNECTION).keySet()) {
                connectionMap.put(key, connectionSettings.get(NM_SETTINGS_CONNECTION).get(key));
            }
        }

        connectionMap.put("autoconnect-retries", new Variant<>(1)); // Prevent retries on failure to avoid
                                                                    // triggering the configuration
                                                                    // enforcement mechanism

        return connectionMap;
    }

    private static Map<String, Variant<?>> createConnectionSettings(String iface) {
        Map<String, Variant<?>> connectionMap = new HashMap<>();

        String connectionName = String.format("kura-%s-connection", iface);
        connectionMap.put("id", new Variant<>(connectionName));
        connectionMap.put("interface-name", new Variant<>(iface));

        return connectionMap;
    }

    private static void setAuthenticationType(String authenticationType, Map<String, Variant<?>> settings) {
        if (authenticationType.equals("AUTO")) {
            return;
        }

        if (authenticationType.equals("NONE")) {
            settings.put(PPP_REFUSE_EAP, new Variant<>(true));
            settings.put(PPP_REFUSE_CHAP, new Variant<>(true));
            settings.put(PPP_REFUSE_PAP, new Variant<>(true));
            settings.put(PPP_REFUSE_MSCHAP, new Variant<>(true));
            settings.put(PPP_REFUSE_MSCHAPV2, new Variant<>(true));
        } else if (authenticationType.equals("CHAP")) {
            settings.put(PPP_REFUSE_EAP, new Variant<>(true));
            settings.put(PPP_REFUSE_CHAP, new Variant<>(false));
            settings.put(PPP_REFUSE_PAP, new Variant<>(true));
            settings.put(PPP_REFUSE_MSCHAP, new Variant<>(true));
            settings.put(PPP_REFUSE_MSCHAPV2, new Variant<>(true));
        } else if (authenticationType.equals("PAP")) {
            settings.put(PPP_REFUSE_EAP, new Variant<>(true));
            settings.put(PPP_REFUSE_CHAP, new Variant<>(true));
            settings.put(PPP_REFUSE_PAP, new Variant<>(false));
            settings.put(PPP_REFUSE_MSCHAP, new Variant<>(true));
            settings.put(PPP_REFUSE_MSCHAPV2, new Variant<>(true));
        } else {
            throw new IllegalArgumentException(
                    String.format("Unsupported PPP authentication method: \"%s\"", authenticationType));
        }
    }

    private static List<UInt32> convertIp4(List<String> ipAddrList) {
        List<UInt32> uint32Addresses = new ArrayList<>();
        for (String address : ipAddrList) {
            try {
                UInt32 uint32Addr = convertIp4(address);
                uint32Addresses.add(uint32Addr);
            } catch (UnknownHostException e) {
                logger.warn("Cannot convert ip address \"{}\" because: ", address, e);
            }
        }
        return uint32Addresses;
    }

    private static UInt32 convertIp4(String ipAddrString) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(ipAddrString);
        byte[] addrBytes = address.getAddress();

        long result = 0;
        result = result << 8 | addrBytes[3] & 0xFF;
        result = result << 8 | addrBytes[2] & 0xFF;
        result = result << 8 | addrBytes[1] & 0xFF;
        result = result << 8 | addrBytes[0] & 0xFF;

        return new UInt32(result);
    }

    private static String wifiModeConvert(String kuraMode) {
        switch (kuraMode) {
        case "INFRA":
            return "infrastructure";
        case "MASTER":
            return "ap";
        default:
            throw new IllegalArgumentException(String.format("Unsupported WiFi mode \"%s\"", kuraMode));
        }
    }

    private static Optional<String> wifiBandConvert(String kuraBand, short channel) {
        List<String> bothFrequencyBands = Arrays.asList("RADIO_MODE_80211nHT20", "RADIO_MODE_80211nHT40below",
                "RADIO_MODE_80211nHT40above");
        boolean automaticChannelSelection = channel == 0;
        boolean automaticBandSelection = bothFrequencyBands.contains(kuraBand);

        if (automaticBandSelection && automaticChannelSelection) {
            // Omit band if full-auto
            return Optional.empty();
        }

        if (automaticBandSelection && !automaticChannelSelection) {
            // Our own interpretation of the Wifi standard
            return channel < 32 ? Optional.of("bg") : Optional.of("a");
        }

        switch (kuraBand) {
        case "RADIO_MODE_80211a":
        case "RADIO_MODE_80211_AC":
            return Optional.of("a");
        case "RADIO_MODE_80211b":
        case "RADIO_MODE_80211g":
            return Optional.of("bg");
        default:
            throw new IllegalArgumentException(String.format("Unsupported WiFi band \"%s\"", kuraBand));
        }
    }

    private static List<String> wifiCipherConvert(String kuraCipher) {
        switch (kuraCipher) {
        case "CCMP":
            return Arrays.asList("ccmp");
        case "TKIP":
            return Arrays.asList("tkip");
        case "CCMP_TKIP":
            return Arrays.asList("tkip", "ccmp");
        default:
            throw new IllegalArgumentException(String.format("Unsupported WiFi cipher \"%s\"", kuraCipher));
        }
    }

    private static List<String> wifiProtoConvert(String kuraSecurityProto) {
        switch (kuraSecurityProto) {
        case "SECURITY_WPA":
            return Arrays.asList("wpa");
        case "SECURITY_WPA2":
            return Arrays.asList("rsn");
        case "SECURITY_WPA_WPA2":
            return Arrays.asList();
        default:
            throw new IllegalArgumentException(String.format("Unsupported WiFi proto \"%s\"", kuraSecurityProto));
        }
    }

    private static String connectionTypeConvert(NMDeviceType deviceType) {
        switch (deviceType) {
        case NM_DEVICE_TYPE_ETHERNET:
            return "802-3-ethernet";
        case NM_DEVICE_TYPE_WIFI:
            return "802-11-wireless";
        case NM_DEVICE_TYPE_MODEM:
            return "gsm";
        // ... WIP
        default:
            throw new IllegalArgumentException(String
                    .format("Unsupported connection type conversion from NMDeviceType \"%s\"", deviceType.toString()));
        }
    }

}
