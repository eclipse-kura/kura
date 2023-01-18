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

public class NMSettingsConverter {

    public static Map<String, Variant<?>> buildIpv4Settings(Map<String, Object> networkConfiguration, String iface) {
        Map<String, Variant<?>> ipv4Map = new HashMap<>();

        Boolean dhcpClient4Enabled = get(networkConfiguration, "net.interface.%s.config.dhcpClient4.enabled", iface);

        // Should handle net.interface.eth0.config.ip4.status here

        if (Boolean.FALSE.equals(dhcpClient4Enabled)) {
            ipv4Map.put("method", new Variant<>("manual"));

            String dhcpClient4Address = get(networkConfiguration, "net.interface.%s.config.ip4.address", iface);
            Short dhcpClient4Prefix = get(networkConfiguration, "net.interface.%s.config.ip4.prefix", iface);

            Map<String, Variant<?>> address = new HashMap<>();
            address.put("address", new Variant<>(dhcpClient4Address));
            address.put("prefix", new Variant<>(new UInt32(dhcpClient4Prefix)));

            List<Map<String, Variant<?>>> addressData = Arrays.asList(address);
            ipv4Map.put("address-data", new Variant<>(addressData, "aa{sv}"));

            Optional<String> dhcpClient4DNS = getOpt(networkConfiguration, "net.interface.%s.config.ip4.dnsServers",
                    iface);
            if (dhcpClient4DNS.isPresent()) {
                ipv4Map.put("dns-search", new Variant<>(splitCommaSeparatedStrings(dhcpClient4DNS.get())));
            }
            ipv4Map.put("ignore-auto-dns", new Variant<>(true));

            Optional<String> dhcpClient4Gateway = getOpt(networkConfiguration, "net.interface.%s.config.ip4.gateway",
                    iface);
            if (dhcpClient4Gateway.isPresent()) {
                ipv4Map.put("gateway", new Variant<>(dhcpClient4Gateway));
            }
        } else {
            ipv4Map.put("method", new Variant<>("auto"));

            Optional<String> dhcpClient4DNS = getOpt(networkConfiguration, "net.interface.%s.config.ip4.dnsServers",
                    iface);
            if (dhcpClient4DNS.isPresent()) {
                ipv4Map.put("ignore-auto-dns", new Variant<>(true));
                ipv4Map.put("dns-search", new Variant<>(splitCommaSeparatedStrings(dhcpClient4DNS.get())));
            }
        }

        return ipv4Map;
    }

    public static <T> T get(Map<String, Object> properties, String key, Object... args) {
        String formattedKey = String.format(key, args);
        return (T) properties.get(formattedKey);
    }

    public static <T> Optional<T> getOpt(Map<String, Object> properties, String key, Object... args) {
        String formattedKey = String.format(key, args);
        if (properties.containsKey(formattedKey)) {
            return Optional.of((T) properties.get(formattedKey));
        } else {
            return Optional.empty();
        }
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
