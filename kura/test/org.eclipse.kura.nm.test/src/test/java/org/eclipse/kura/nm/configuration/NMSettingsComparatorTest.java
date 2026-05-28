/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.junit.Test;

public class NMSettingsComparatorTest {

    private Map<String, Map<String, Variant<?>>> newSettings = new HashMap<>();
    private Map<String, Map<String, Variant<?>>> oldSettings = new HashMap<>();

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenNewSettingsAreNull() {
        NMSettingsComparator.areSettingsEqual(null, oldSettings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenOldSettingsAreNull() {
        NMSettingsComparator.areSettingsEqual(newSettings, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenBothSettingsAreNull() {
        NMSettingsComparator.areSettingsEqual(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenNewNestedMapIsNull() {
        newSettings.put("connection", null);

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        NMSettingsComparator.areSettingsEqual(newSettings, oldSettings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenOldNestedMapIsNull() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("connection", null);
        NMSettingsComparator.areSettingsEqual(newSettings, oldSettings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenBothNestedMapAreNull() {
        newSettings.put("connection", null);
        oldSettings.put("connection", null);
        NMSettingsComparator.areSettingsEqual(newSettings, oldSettings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenNewVariantIsNull() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", null);

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));
        NMSettingsComparator.areSettingsEqual(newSettings, oldSettings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenOldVariantIsNull() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", null);

        NMSettingsComparator.areSettingsEqual(newSettings, oldSettings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenBothVariantsAreNull() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", null);

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", null);
        NMSettingsComparator.areSettingsEqual(newSettings, oldSettings);
    }

    @Test
    public void returnsTrueWhenSettingsAreEqual() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsTrueWhenSettingsAreEqualWithByteArray() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        newSettings.put("802-11-wireless", new HashMap<>());
        String newSSID = "MyWiFiSSID";
        newSettings.get("802-11-wireless").put("ssid", new Variant<>(newSSID.getBytes(StandardCharsets.UTF_8)));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("802-11-wireless", new HashMap<>());
        String oldSSID = "MyWiFiSSID";
        oldSettings.get("802-11-wireless").put("ssid", new Variant<>(oldSSID.getBytes(StandardCharsets.UTF_8)));

        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenSettingsAreEqualButSubset() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("ipv4", new HashMap<>());
        oldSettings.get("ipv4").put("method", new Variant<String>("auto"));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsTrueWhenSettingsAreEqualAndMapVariant() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        newSettings.put("ipv4", new HashMap<>());
        newSettings.get("ipv4").put("method", new Variant<String>("manual"));

        Map<String, Variant<?>> newAddressEntry = new HashMap<>();
        newAddressEntry.put("address", new Variant<>("172.16.1.1"));
        newAddressEntry.put("prefix", new Variant<>(new UInt32(24)));
        List<Map<String, Variant<?>>> newAddressData = Arrays.asList(newAddressEntry);
        newSettings.get("ipv4").put("address-data", new Variant<>(newAddressData, "aa{sv}"));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("ipv4", new HashMap<>());
        oldSettings.get("ipv4").put("method", new Variant<String>("manual"));

        Map<String, Variant<?>> oldAddressEntry = new HashMap<>();
        oldAddressEntry.put("address", new Variant<>("172.16.1.1"));
        oldAddressEntry.put("prefix", new Variant<>(new UInt32(24)));
        List<Map<String, Variant<?>>> oldAddressData = Arrays.asList(oldAddressEntry);
        oldSettings.get("ipv4").put("address-data", new Variant<>(oldAddressData, "aa{sv}"));

        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsTrueWhenBothSettingsAreEmpty() {
        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenNewSettingsAreEmpty() {
        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenOldSettingsAreEmpty() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenSettingsAreNotEqual() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My Ethernet"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(2));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenSettingsAreNotEqualWithByteArray() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        newSettings.put("802-11-wireless", new HashMap<>());
        String newSSID = "MyWiFiSSID";
        newSettings.get("802-11-wireless").put("ssid", new Variant<>(newSSID.getBytes(StandardCharsets.UTF_8)));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("802-11-wireless", new HashMap<>());
        String oldSSID = "MyOtherWiFiSSID";
        oldSettings.get("802-11-wireless").put("ssid", new Variant<>(oldSSID.getBytes(StandardCharsets.UTF_8)));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }
    @Test
    public void returnsFalseWhenSettingsAreNotEqualAndMapVariant() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        newSettings.put("ipv4", new HashMap<>());
        newSettings.get("ipv4").put("method", new Variant<String>("manual"));

        Map<String, Variant<?>> newAddressEntry = new HashMap<>();
        newAddressEntry.put("address", new Variant<>("172.16.1.1"));
        newAddressEntry.put("prefix", new Variant<>(new UInt32(24)));
        List<Map<String, Variant<?>>> newAddressData = Arrays.asList(newAddressEntry);
        newSettings.get("ipv4").put("address-data", new Variant<>(newAddressData, "aa{sv}"));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("ipv4", new HashMap<>());
        oldSettings.get("ipv4").put("method", new Variant<String>("manual"));

        Map<String, Variant<?>> oldAddressEntry = new HashMap<>();
        oldAddressEntry.put("address", new Variant<>("172.16.1.2"));
        oldAddressEntry.put("prefix", new Variant<>(new UInt32(24)));
        List<Map<String, Variant<?>>> oldAddressData = Arrays.asList(oldAddressEntry);
        oldSettings.get("ipv4").put("address-data", new Variant<>(oldAddressData, "aa{sv}"));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenSettingsAreNotEqualButSubset() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My Ethernet"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(2));

        oldSettings.put("ipv4", new HashMap<>());
        oldSettings.get("ipv4").put("method", new Variant<String>("auto"));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenSettingsAreEqualButSuperset() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        newSettings.put("ipv4", new HashMap<>());
        newSettings.get("ipv4").put("method", new Variant<String>("auto"));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My Ethernet"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(2));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenSettingsIntersectionIsEmpty() {
        newSettings.put("ipv4", new HashMap<>());
        newSettings.get("ipv4").put("method", new Variant<String>("auto"));

        oldSettings.put("ipv4", new HashMap<>());
        oldSettings.get("ipv4").put("method", new Variant<String>("manual"));

        Map<String, Variant<?>> oldAddressEntry = new HashMap<>();
        oldAddressEntry.put("address", new Variant<>("172.16.1.2"));
        oldAddressEntry.put("prefix", new Variant<>(new UInt32(24)));
        List<Map<String, Variant<?>>> oldAddressData = Arrays.asList(oldAddressEntry);
        oldSettings.get("ipv4").put("address-data", new Variant<>(oldAddressData, "aa{sv}"));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsTrueWhenSettingsAreEqualWithStringList() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));

        newSettings.put("ipv4", new HashMap<>());
        newSettings.get("ipv4").put("method", new Variant<String>("manual"));

        List<String> newDnsServers = Arrays.asList("8.8.8.8", "8.8.4.4");
        newSettings.get("ipv4").put("dns", new Variant<>(newDnsServers, "as"));
        newSettings.get("ipv4").put("ignore-auto-dns", new Variant<>(true));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));

        oldSettings.put("ipv4", new HashMap<>());
        oldSettings.get("ipv4").put("method", new Variant<String>("manual"));

        List<String> oldDnsServers = Arrays.asList("8.8.8.8", "8.8.4.4");
        oldSettings.get("ipv4").put("dns", new Variant<>(oldDnsServers, "as"));
        oldSettings.get("ipv4").put("ignore-auto-dns", new Variant<>(true));

        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenSettingsAreNotEqualWithStringList() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));

        newSettings.put("ipv4", new HashMap<>());
        newSettings.get("ipv4").put("method", new Variant<String>("manual"));

        List<String> newDnsServers = Arrays.asList("8.8.8.8", "8.8.4.4");
        newSettings.get("ipv4").put("dns", new Variant<>(newDnsServers, "as"));
        newSettings.get("ipv4").put("ignore-auto-dns", new Variant<>(true));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));

        oldSettings.put("ipv4", new HashMap<>());
        oldSettings.get("ipv4").put("method", new Variant<String>("manual"));

        List<String> oldDnsServers = Arrays.asList("1.1.1.1", "1.0.0.1");
        oldSettings.get("ipv4").put("dns", new Variant<>(oldDnsServers, "as"));
        oldSettings.get("ipv4").put("ignore-auto-dns", new Variant<>(true));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }
}