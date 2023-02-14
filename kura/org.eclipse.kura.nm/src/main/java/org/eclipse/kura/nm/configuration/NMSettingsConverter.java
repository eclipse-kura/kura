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
import org.eclipse.kura.nm.NMDeviceType;
import org.eclipse.kura.nm.NetworkProperties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMSettingsConverter {

    private static final Logger logger = LoggerFactory.getLogger(NMSettingsConverter.class);

    private NMSettingsConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static Map<String, Map<String, Variant<?>>> buildSettings(NetworkProperties properties,
            Optional<Connection> oldConnection, String iface, NMDeviceType deviceType) {
        Map<String, Map<String, Variant<?>>> newConnectionSettings = new HashMap<>();

        Map<String, Variant<?>> connectionMap = buildConnectionSettings(oldConnection, iface, deviceType);
        newConnectionSettings.put("connection", connectionMap);

        Map<String, Variant<?>> ipv4Map = NMSettingsConverter.buildIpv4Settings(properties, iface);
        Map<String, Variant<?>> ipv6Map = NMSettingsConverter.buildIpv6Settings(properties, iface);
        newConnectionSettings.put("ipv4", ipv4Map);
        newConnectionSettings.put("ipv6", ipv6Map);

        if (deviceType == NMDeviceType.NM_DEVICE_TYPE_WIFI) {
            Map<String, Variant<?>> wifiSettingsMap = NMSettingsConverter.build80211WirelessSettings(properties, iface);
            Map<String, Variant<?>> wifiSecuritySettingsMap = NMSettingsConverter
                    .build80211WirelessSecuritySettings(properties, iface);
            newConnectionSettings.put("802-11-wireless", wifiSettingsMap);
            newConnectionSettings.put("802-11-wireless-security", wifiSecuritySettingsMap);
        }

        return newConnectionSettings;
    }

    public static Map<String, Variant<?>> buildIpv4Settings(NetworkProperties props, String iface) {
        Map<String, Variant<?>> settings = new HashMap<>();

        Boolean dhcpClient4Enabled = props.get(Boolean.class, "net.interface.%s.config.dhcpClient4.enabled", iface);

        KuraIpStatus ip4Status = KuraIpStatus
                .fromString(props.get(String.class, "net.interface.%s.config.ip4.status", iface));

        if (Boolean.FALSE.equals(dhcpClient4Enabled)) {
            settings.put("method", new Variant<>("manual"));

            String address = props.get(String.class, "net.interface.%s.config.ip4.address", iface);
            Short prefix = props.get(Short.class, "net.interface.%s.config.ip4.prefix", iface);

            Map<String, Variant<?>> addressEntry = new HashMap<>();
            addressEntry.put("address", new Variant<>(address));
            addressEntry.put("prefix", new Variant<>(new UInt32(prefix)));

            List<Map<String, Variant<?>>> addressData = Arrays.asList(addressEntry);
            settings.put("address-data", new Variant<>(addressData, "aa{sv}"));
        } else {
            settings.put("method", new Variant<>("auto"));
        }

        if (ip4Status.equals(KuraIpStatus.ENABLEDLAN)) {
            settings.put("ignore-auto-dns", new Variant<>(true));
            settings.put("ignore-auto-routes", new Variant<>(true));
        } else if (ip4Status.equals(KuraIpStatus.ENABLEDWAN)) {
            Optional<List<String>> dnsServers = props.getOptStringList("net.interface.%s.config.ip4.dnsServers", iface);
            if (dnsServers.isPresent()) {
                settings.put("dns", new Variant<>(convertIp4(dnsServers.get()), "au"));
                settings.put("ignore-auto-dns", new Variant<>(true));
            }
            Optional<String> gateway = props.getOpt(String.class, "net.interface.%s.config.ip4.gateway", iface);
            if (gateway.isPresent()) {
                settings.put("gateway", new Variant<>(gateway.get()));
            }
        } else {
            logger.warn("Unexpected ip status received: \"{}\". Ignoring", ip4Status);
        }

        return settings;
    }

    public static Map<String, Variant<?>> buildIpv6Settings(NetworkProperties props, String iface) {
        Map<String, Variant<?>> settings = new HashMap<>();

        // Disabled for now
        settings.put("method", new Variant<>("disabled"));

        return settings;
    }

    public static Map<String, Variant<?>> build80211WirelessSettings(NetworkProperties props, String iface) {
        Map<String, Variant<?>> settings = new HashMap<>();

        String propMode = props.get(String.class, "net.interface.%s.config.wifi.mode", iface);

        String mode = wifiModeConvert(propMode);
        String ssid = props.get(String.class, "net.interface.%s.config.wifi.%s.ssid", iface, propMode.toLowerCase());
        String band = wifiBandConvert(
                props.get(String.class, "net.interface.%s.config.wifi.%s.radioMode", iface, propMode.toLowerCase()));
        Optional<String> channel = props.getOpt(String.class, "net.interface.%s.config.wifi.%s.channel", iface,
                propMode.toLowerCase());
        Optional<Boolean> hidden = props.getOpt(Boolean.class, "net.interface.%s.config.wifi.%s.ignoreSSID", iface,
                propMode.toLowerCase());

        settings.put("mode", new Variant<>(mode));
        settings.put("ssid", new Variant<>(ssid.getBytes(StandardCharsets.UTF_8)));
        settings.put("band", new Variant<>(band));
        if (channel.isPresent()) {
            settings.put("channel", new Variant<>(new UInt32(Short.parseShort(channel.get()))));
        }
        if (hidden.isPresent()) {
            settings.put("hidden", new Variant<>(hidden.get()));
        }

        return settings;
    }

    public static Map<String, Variant<?>> build80211WirelessSecuritySettings(NetworkProperties props, String iface) {
        Map<String, Variant<?>> settings = new HashMap<>();

        String propMode = props.get(String.class, "net.interface.%s.config.wifi.mode", iface);

        String psk = props
                .get(Password.class, "net.interface.%s.config.wifi.%s.passphrase", iface, propMode.toLowerCase())
                .toString();
        String keyMgmt = wifiKeyMgmtConvert(
                props.get(String.class, "net.interface.%s.config.wifi.%s.securityType", iface, propMode.toLowerCase()));
        settings.put("psk", new Variant<>(psk));
        settings.put("key-mgmt", new Variant<>(keyMgmt));

        Optional<String> group = props.getOpt(String.class, "net.interface.%s.config.wifi.%s.groupCiphers", iface,
                propMode.toLowerCase());
        if (group.isPresent()) {
            List<String> nmGroup = wifiCipherConvert(group.get());
            settings.put("group", new Variant<>(nmGroup, "as"));
        }

        Optional<String> pairwise = props.getOpt(String.class, "net.interface.%s.config.wifi.%s.pairwiseCiphers", iface,
                propMode.toLowerCase());
        if (pairwise.isPresent()) {
            List<String> nmPairwise = wifiCipherConvert(pairwise.get());
            settings.put("pairwise", new Variant<>(nmPairwise, "as"));
        }

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
            for (String key : connectionSettings.get("connection").keySet()) {
                connectionMap.put(key, connectionSettings.get("connection").get(key));
            }
        }

        return connectionMap;
    }

    private static Map<String, Variant<?>> createConnectionSettings(String iface) {
        Map<String, Variant<?>> connectionMap = new HashMap<>();

        String connectionName = String.format("kura-%s-connection", iface);
        connectionMap.put("id", new Variant<>(connectionName));
        connectionMap.put("interface-name", new Variant<>(iface));

        return connectionMap;
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

    private static String wifiBandConvert(String kuraBand) {
        switch (kuraBand) {
        case "RADIO_MODE_80211a":
        case "RADIO_MODE_80211_AC":
            return "a";
        case "RADIO_MODE_80211b":
        case "RADIO_MODE_80211g":
            return "bg";
        case "RADIO_MODE_80211nHT20":
        case "RADIO_MODE_80211nHT40below":
        case "RADIO_MODE_80211nHT40above":
            return "bg"; // TBD
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

    private static String wifiKeyMgmtConvert(String kuraSecurityType) {
        switch (kuraSecurityType) {
        case "NONE":
        case "SECURITY_WEP":
            return "none";
        case "SECURITY_WPA":
        case "SECURITY_WPA2":
        case "SECURITY_WPA_WPA2":
            return "wpa-psk";
        default:
            throw new IllegalArgumentException(
                    String.format("Unsupported WiFi key management \"%s\"", kuraSecurityType));
        }
    }

    private static String connectionTypeConvert(NMDeviceType deviceType) {
        switch (deviceType) {
        case NM_DEVICE_TYPE_ETHERNET:
            return "802-3-ethernet";
        case NM_DEVICE_TYPE_WIFI:
            return "802-11-wireless";
        // ... WIP
        default:
            throw new IllegalArgumentException(String
                    .format("Unsupported connection type conversion from NMDeviceType \"%s\"", deviceType.toString()));
        }
    }

}
