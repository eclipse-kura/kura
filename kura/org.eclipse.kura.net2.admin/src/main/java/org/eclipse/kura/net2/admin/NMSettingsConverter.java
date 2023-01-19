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
 *******************************************************************************/
package org.eclipse.kura.net2.admin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMSettingsConverter {

    private static final Logger logger = LoggerFactory.getLogger(NMSettingsConverter.class);

    private static final String WIFI_INFRA = "infra";
    private static final String WIFI_MASTER = "master";

    private static final Map<String, String> wifiModeConverter = initWifiModeConverter();
    private static final Map<String, String> wifiBandConverter = initWifiBandConverter();
    private static final Map<String, List<String>> wifiCipherConverter = initWifiCipherConverter();
    private static final Map<String, String> wifiKeyMgmtConverter = initWifiKeyMgmtConverter();

    private NMSettingsConverter() {
        throw new IllegalStateException("Utility class");
    }

    private static Map<String, String> initWifiModeConverter() {
        Map<String, String> map = new HashMap<>();

        map.put("INFRA", "infrastructure");
        map.put("MASTER", "ap");

        return map;
    }

    private static Map<String, String> initWifiBandConverter() {
        Map<String, String> map = new HashMap<>();

        map.put("RADIO_MODE_80211a", "a");
        map.put("RADIO_MODE_80211b", "bg");
        map.put("RADIO_MODE_80211g", "bg");
        map.put("RADIO_MODE_80211nHT20", "bg"); // TBD
        map.put("RADIO_MODE_80211nHT40below", "bg"); // TBD
        map.put("RADIO_MODE_80211nHT40above", "bg"); // TBD
        map.put("RADIO_MODE_80211_AC", "a"); // TBD

        return map;
    }

    private static Map<String, List<String>> initWifiCipherConverter() {
        Map<String, List<String>> map = new HashMap<>();

        map.put("CCMP", Arrays.asList("ccmp"));
        map.put("TKIP", Arrays.asList("tkip"));
        map.put("CCMP TKIP", Arrays.asList("tkip", "ccmp"));

        return map;
    }

    private static Map<String, String> initWifiKeyMgmtConverter() {
        Map<String, String> map = new HashMap<>();

        map.put("NONE", "none");
        map.put("SECURITY_WEP", "none");
        map.put("SECURITY_WPA", "wpa-psk");
        map.put("SECURITY_WPA2", "wpa-psk");
        map.put("SECURITY_WPA_WPA2", "wpa-psk");

        return map;
    }

    public static Map<String, Variant<?>> buildIpv4Settings(Map<String, Object> networkConfiguration, String iface) {
        NetworkProperties props = new NetworkProperties(networkConfiguration);

        Map<String, Variant<?>> settings = new HashMap<>();

        Boolean dhcpClient4Enabled = props.get(Boolean.class, "net.interface.%s.config.dhcpClient4.enabled", iface);

        // Should handle net.interface.eth0.config.ip4.status here

        if (Boolean.FALSE.equals(dhcpClient4Enabled)) {
            settings.put("method", new Variant<>("manual"));

            String address = props.get(String.class, "net.interface.%s.config.ip4.address", iface);
            Short prefix = props.get(Short.class, "net.interface.%s.config.ip4.prefix", iface);

            Map<String, Variant<?>> addressEntry = new HashMap<>();
            addressEntry.put("address", new Variant<>(address));
            addressEntry.put("prefix", new Variant<>(new UInt32(prefix)));

            List<Map<String, Variant<?>>> addressData = Arrays.asList(addressEntry);
            settings.put("address-data", new Variant<>(addressData, "aa{sv}"));

            Optional<String> dnsServers = props.getOpt(String.class, "net.interface.%s.config.ip4.dnsServers", iface);
            if (dnsServers.isPresent()) {
                settings.put("dns-search", new Variant<>(splitCommaSeparatedStrings(dnsServers.get())));
            }
            settings.put("ignore-auto-dns", new Variant<>(true));

            Optional<String> gateway = props.getOpt(String.class, "net.interface.%s.config.ip4.gateway", iface);
            if (gateway.isPresent()) {
                settings.put("gateway", new Variant<>(gateway));
            }
        } else {
            settings.put("method", new Variant<>("auto"));

            Optional<String> dnsServers = props.getOpt(String.class, "net.interface.%s.config.ip4.dnsServers", iface);
            if (dnsServers.isPresent()) {
                settings.put("ignore-auto-dns", new Variant<>(true));
                settings.put("dns-search", new Variant<>(splitCommaSeparatedStrings(dnsServers.get())));
            }
        }

        return settings;
    }

    public static Map<String, Variant<?>> buildIpv6Settings(Map<String, Object> networkConfiguration, String iface) {
        Map<String, Variant<?>> settings = new HashMap<>();

        // Disabled for now
        settings.put("method", new Variant<>("disabled"));

        return settings;
    }

    public static Map<String, Variant<?>> build802_11_WirelessSettings(Map<String, Object> networkConfiguration,
            String iface) {
        NetworkProperties props = new NetworkProperties(networkConfiguration);

        Map<String, Variant<?>> settings = new HashMap<>();

        String propMode = props.get(String.class, "net.interface.%s.config.wifi.mode", iface);

        if (!propMode.equals("INFRA")) {
            logger.warn("Unsupported WiFi mode"); // WIP
            return settings;
        }

        String mode = wifiModeConverter.get(propMode);
        String ssid = props.get(String.class, "net.interface.%s.config.wifi.%s.ssid", iface, propMode.toLowerCase());
        String band = wifiBandConverter.get(
                props.get(String.class, "net.interface.%s.config.wifi.%s.radioMode", iface, propMode.toLowerCase()));
        Optional<String> channel = props.getOpt(String.class, "net.interface.%s.config.wifi.%s.channel", iface,
                propMode.toLowerCase());

        settings.put("mode", new Variant<>(mode));
        settings.put("ssid", new Variant<>(ssid.getBytes(StandardCharsets.UTF_8)));
        settings.put("band", new Variant<>(band));
        if (channel.isPresent()) {
            settings.put("channel", new Variant<>(new UInt32(Short.parseShort(channel.get()))));
        }

        return settings;
    }

    public static Map<String, Variant<?>> build802_11_WirelessSecuritySettings(Map<String, Object> networkConfiguration,
            String iface) {
        NetworkProperties props = new NetworkProperties(networkConfiguration);

        Map<String, Variant<?>> settings = new HashMap<>();

        String propMode = props.get(String.class, "net.interface.%s.config.wifi.mode", iface);

        if (!propMode.equals("INFRA")) {
            logger.warn("Unsupported WiFi mode"); // WIP
            return settings;
        }

        String psk = props.get(String.class, "net.interface.%s.config.wifi.%s.passphrase", iface,
                propMode.toLowerCase());
        String keyMgmt = wifiKeyMgmtConverter.get(
                props.get(String.class, "net.interface.%s.config.wifi.%s.securityType", iface, propMode.toLowerCase()));
        // List<String> group = wifiCipherConverter.get(props.get(String.class,
        // "net.interface.%s.config.wifi.%s.groupCiphers", iface, propMode.toLowerCase()));
        // List<String> pairwise = wifiCipherConverter.get(props.get(String.class,
        // "net.interface.%s.config.wifi.%s.pairwiseCiphers", iface, propMode.toLowerCase()));

        settings.put("psk", new Variant<>(psk)); // Will require decryption in Kura
        settings.put("key-mgmt", new Variant<>(keyMgmt));
        // settings.put("group", new Variant<>(group, "a(s)")); <- Not working: Trying to marshall to unconvertible type
        // (from java.lang.String to ().
        // settings.put("pairwise", new Variant<>(group, "a(s)")); <- Not working: Trying to marshall to unconvertible
        // type (from java.lang.String to ().

        return settings;
    }

    public static List<String> splitCommaSeparatedStrings(String commaSeparatedString) {
        List<String> stringList = new ArrayList<>();
        Pattern comma = Pattern.compile(",");
        if (Objects.nonNull(commaSeparatedString) && !commaSeparatedString.isEmpty()) {
            comma.splitAsStream(commaSeparatedString).filter(s -> !s.trim().isEmpty()).forEach(stringList::add);
        }

        return stringList;
    }
}
