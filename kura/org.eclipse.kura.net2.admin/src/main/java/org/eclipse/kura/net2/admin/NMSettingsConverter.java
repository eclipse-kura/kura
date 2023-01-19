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

    private NMSettingsConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static Map<String, Variant<?>> buildIpv4Settings(Map<String, Object> networkConfiguration, String iface) {
        Map<String, Variant<?>> settings = new HashMap<>();

        Boolean dhcpClient4Enabled = get(networkConfiguration, Boolean.class,
                "net.interface.%s.config.dhcpClient4.enabled", iface);

        // Should handle net.interface.eth0.config.ip4.status here

        if (Boolean.FALSE.equals(dhcpClient4Enabled)) {
            settings.put("method", new Variant<>("manual"));

            String address = get(networkConfiguration, String.class, "net.interface.%s.config.ip4.address", iface);
            Short prefix = get(networkConfiguration, Short.class, "net.interface.%s.config.ip4.prefix", iface);

            Map<String, Variant<?>> addressEntry = new HashMap<>();
            addressEntry.put("address", new Variant<>(address));
            addressEntry.put("prefix", new Variant<>(new UInt32(prefix)));

            List<Map<String, Variant<?>>> addressData = Arrays.asList(addressEntry);
            settings.put("address-data", new Variant<>(addressData, "aa{sv}"));

            Optional<String> dnsServers = getOpt(networkConfiguration, String.class,
                    "net.interface.%s.config.ip4.dnsServers", iface);
            if (dnsServers.isPresent()) {
                settings.put("dns-search", new Variant<>(splitCommaSeparatedStrings(dnsServers.get())));
            }
            settings.put("ignore-auto-dns", new Variant<>(true));

            Optional<String> gateway = getOpt(networkConfiguration, String.class, "net.interface.%s.config.ip4.gateway",
                    iface);
            if (gateway.isPresent()) {
                settings.put("gateway", new Variant<>(gateway));
            }
        } else {
            settings.put("method", new Variant<>("auto"));

            Optional<String> dnsServers = getOpt(networkConfiguration, String.class,
                    "net.interface.%s.config.ip4.dnsServers", iface);
            if (dnsServers.isPresent()) {
                settings.put("ignore-auto-dns", new Variant<>(true));
                settings.put("dns-search", new Variant<>(splitCommaSeparatedStrings(dnsServers.get())));
            }
        }

        return settings;
    }

    public static <T> T get(Map<String, Object> properties, Class<T> clazz, String key, Object... args) {
        String formattedKey = String.format(key, args);
        return clazz.cast(properties.get(formattedKey));
    }

    public static <T> Optional<T> getOpt(Map<String, Object> properties, Class<T> clazz, String key, Object... args) {
        String formattedKey = String.format(key, args);
        if (properties.containsKey(formattedKey)) {
            return Optional.of(clazz.cast(properties.get(formattedKey)));
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
