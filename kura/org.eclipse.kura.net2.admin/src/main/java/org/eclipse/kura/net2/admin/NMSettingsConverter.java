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
import java.util.regex.Pattern;

import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

public class NMSettingsConverter {

    public static Map<String, Variant<?>> buildIpv4Settings(Map<String, Object> networkConfiguration, String iface) {
        Map<String, Variant<?>> ipv4Map = new HashMap<>();

        String dhcpClient4EnabledProperty = String.format("net.interface.%s.config.dhcpClient4.enabled", iface);
        Boolean dhcpClient4Enabled = (Boolean) networkConfiguration.get(dhcpClient4EnabledProperty);

        // Should handle net.interface.eth0.config.ip4.status here

        if (Boolean.FALSE.equals(dhcpClient4Enabled)) {
            ipv4Map.put("method", new Variant<>("manual"));

            String dhcpClient4AddressProperty = String.format("net.interface.%s.config.ip4.address", iface);
            String dhcpClient4Address = (String) networkConfiguration.get(dhcpClient4AddressProperty);

            String dhcpClient4PrefixProperty = String.format("net.interface.%s.config.ip4.prefix", iface);
            Short dhcpClient4Prefix = (Short) networkConfiguration.get(dhcpClient4PrefixProperty);

            Map<String, Variant<?>> address = new HashMap<>();
            address.put("address", new Variant<>(dhcpClient4Address));
            address.put("prefix", new Variant<>(new UInt32(dhcpClient4Prefix)));

            List<Map<String, Variant<?>>> addressData = Arrays.asList(address);
            ipv4Map.put("address-data", new Variant<>(addressData, "aa{sv}"));

            String dhcpClient4DNSProperty = String.format("net.interface.%s.config.ip4.dnsServers", iface);
            if (networkConfiguration.containsKey(dhcpClient4DNSProperty)) {
                String dhcpClient4DNS = (String) networkConfiguration.get(dhcpClient4DNSProperty);
                ipv4Map.put("dns-search", new Variant<>(splitCommaSeparatedStrings(dhcpClient4DNS)));
            }
            ipv4Map.put("ignore-auto-dns", new Variant<>(true));

            String dhcpClient4GatewayProperty = String.format("net.interface.%s.config.ip4.gateway", iface);
            if (networkConfiguration.containsKey(dhcpClient4GatewayProperty)) {
                String dhcpClient4Gateway = (String) networkConfiguration.get(dhcpClient4GatewayProperty);
                ipv4Map.put("gateway", new Variant<>(dhcpClient4Gateway));
            }
        } else {
            ipv4Map.put("method", new Variant<>("auto"));

            String dhcpClient4DNSProperty = String.format("net.interface.%s.config.ip4.dnsServers", iface);
            if (networkConfiguration.containsKey(dhcpClient4DNSProperty)) {
                String dhcpClient4DNS = (String) networkConfiguration.get(dhcpClient4DNSProperty);
                ipv4Map.put("ignore-auto-dns", new Variant<>(true));
                ipv4Map.put("dns-search", new Variant<>(splitCommaSeparatedStrings(dhcpClient4DNS)));
            }
        }

        return ipv4Map;
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
