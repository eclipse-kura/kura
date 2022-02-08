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
 *******************************************************************************/
package org.eclipse.kura.core.net;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiBgscanModule;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.junit.Ignore;
import org.junit.Test;


public class WifiConfigurationInterpreterTest {

    @Test
    public void testGetWifiConfigMinimal1() throws Throwable {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.wlan.config.wifi.master.passphrase", new Password("password"));
        properties.put("net.interface.wlan.config.wifi.master.broadcast", (Boolean) true);
        properties.put("net.interface.wlan.config.wifi.master.driver", "");
        properties.put("net.interface.wlan.config.wifi.master.hardwareMode", "");

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.MASTER);
        expected.setSSID("");
        expected.setDriver("");
        expected.setSecurity(WifiSecurity.NONE);
        expected.setPasskey("password");
        expected.setHardwareMode("");
        expected.setIgnoreSSID(false);
        
        List<NetConfig> netConfigs = WifiConfigurationInterpreter.populateConfiguration(properties, "wlan");

        assertNotNull(netConfigs);
        assertEquals(2, netConfigs.size());

        WifiConfig wifiConfig = (WifiConfig) netConfigs.get(0);

        assertEquals(expected, wifiConfig);
    }

    @Test
    public void testGetWifiConfigMinimal2() throws Throwable {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.wlan.config.wifi.infra.ssid", "");
        properties.put("net.interface.wlan.config.wifi.infra.driver", "");
        properties.put("net.interface.wlan.config.wifi.infra.securityType", "");
        properties.put("net.interface.wlan.config.wifi.infra.channel", "");
        properties.put("net.interface.wlan.config.wifi.infra.passphrase", "password");
        properties.put("net.interface.wlan.config.wifi.infra.radioMode", "RADIO_MODE_80211b");

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.INFRA);
        expected.setSSID("");
        expected.setDriver("");
        expected.setSecurity(WifiSecurity.NONE);
        expected.setPasskey("password");
        expected.setHardwareMode("b");
        expected.setIgnoreSSID(false);
        expected.setBgscan(new WifiBgscan(""));

        List<NetConfig> netConfigs = WifiConfigurationInterpreter.populateConfiguration(properties, "wlan");

        assertNotNull(netConfigs);
        assertEquals(2, netConfigs.size());

        WifiConfig wifiConfig = (WifiConfig) netConfigs.get(1);

        assertEquals(expected, wifiConfig);
    }

    @Test(expected = KuraException.class)
    public void testGetWifiConfigInvalidSecurityType() throws Throwable {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.wlan.config.wifi.infra.securityType", "xyz");

        WifiConfigurationInterpreter.populateConfiguration(properties, "wlan");
    }

    @Test
    public void testGetWifiConfigInvalidChannel() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.wlan.config.wifi.infra.channel", "1 a 3");
        properties.put("net.interface.wlan.config.wifi.infra.ssid", "ssid");
        properties.put("net.interface.wlan.config.wifi.infra.driver", "driver");
        properties.put("net.interface.wlan.config.wifi.infra.securityType", "GROUP_CCMP");
        properties.put("net.interface.wlan.config.wifi.infra.passphrase", new Password("password"));
        properties.put("net.interface.wlan.config.wifi.infra.hardwareMode", "HW mode");
        properties.put("net.interface.wlan.config.wifi.infra.broadcast", (Boolean) true);
        properties.put("net.interface.wlan.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        properties.put("net.interface.wlan.config.wifi.infra.bgscan", "learn:1:2:3");
        properties.put("net.interface.wlan.config.wifi.infra.pairwiseCiphers", "CCMP");
        properties.put("net.interface.wlan.config.wifi.infra.groupCiphers", "TKIP");
        properties.put("net.interface.wlan.config.wifi.infra.pingAccessPoint", (Boolean) true);
        properties.put("net.interface.wlan.config.wifi.infra.ignoreSSID", (Boolean) true);

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.INFRA);
        expected.setChannels(new int[] { 1, 0, 3 });
        expected.setSSID("ssid");
        expected.setDriver("driver");
        expected.setSecurity(WifiSecurity.GROUP_CCMP);
        expected.setPasskey("password");
        expected.setHardwareMode("HW mode");
        expected.setBroadcast(true);
        expected.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        expected.setBgscan(new WifiBgscan(WifiBgscanModule.LEARN, 1, 2, 3));
        expected.setPairwiseCiphers(WifiCiphers.CCMP);
        expected.setGroupCiphers(WifiCiphers.TKIP);
        expected.setPingAccessPoint(true);
        expected.setIgnoreSSID(true);

        List<NetConfig> netConfigs = WifiConfigurationInterpreter.populateConfiguration(properties, "wlan");

        assertNotNull(netConfigs);
        assertEquals(2, netConfigs.size());

        WifiConfig wifiConfig = (WifiConfig) netConfigs.get(1);
        
        assertEquals(expected, wifiConfig);
    }

    @Test(expected = KuraException.class)
    public void testGetWifiConfigInvalidRadioMode() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.wlan.config.wifi.infra.radioMode", "xyz");

        WifiConfigurationInterpreter.populateConfiguration(properties, "wlan");
    }

    @Test
    public void testGetWifiConfigFullInfra() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.wlan.config.wifi.infra.ssid", "ssid");
        properties.put("net.interface.wlan.config.wifi.infra.driver", "driver");
        properties.put("net.interface.wlan.config.wifi.infra.securityType", "GROUP_CCMP");
        properties.put("net.interface.wlan.config.wifi.infra.channel", "1 2 3");
        properties.put("net.interface.wlan.config.wifi.infra.passphrase", new Password("password"));
        properties.put("net.interface.wlan.config.wifi.infra.hardwareMode", "HW mode");
        properties.put("net.interface.wlan.config.wifi.infra.broadcast", (Boolean) true);
        properties.put("net.interface.wlan.config.wifi.infra.radioMode", "RADIO_MODE_80211a");
        properties.put("net.interface.wlan.config.wifi.infra.bgscan", "learn:1:2:3");
        properties.put("net.interface.wlan.config.wifi.infra.pairwiseCiphers", "CCMP");
        properties.put("net.interface.wlan.config.wifi.infra.groupCiphers", "TKIP");
        properties.put("net.interface.wlan.config.wifi.infra.pingAccessPoint", (Boolean) true);
        properties.put("net.interface.wlan.config.wifi.infra.ignoreSSID", (Boolean) true);

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.INFRA);
        expected.setChannels(new int[] { 1, 2, 3 });
        expected.setSSID("ssid");
        expected.setDriver("driver");
        expected.setSecurity(WifiSecurity.GROUP_CCMP);
        expected.setPasskey("password");
        expected.setHardwareMode("HW mode");
        expected.setBroadcast(true);
        expected.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        expected.setBgscan(new WifiBgscan(WifiBgscanModule.LEARN, 1, 2, 3));
        expected.setPairwiseCiphers(WifiCiphers.CCMP);
        expected.setGroupCiphers(WifiCiphers.TKIP);
        expected.setPingAccessPoint(true);
        expected.setIgnoreSSID(true);

        List<NetConfig> netConfigs = WifiConfigurationInterpreter.populateConfiguration(properties, "wlan");

        assertNotNull(netConfigs);
        assertEquals(2, netConfigs.size());

        WifiConfig wifiConfig = (WifiConfig) netConfigs.get(1);
        
        assertEquals(expected, wifiConfig);
    }

    @Test
    public void testGetWifiConfigFullNonInfra() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.wlan.config.wifi.master.ssid", "ssid");
        properties.put("net.interface.wlan.config.wifi.master.driver", "driver");
        properties.put("net.interface.wlan.config.wifi.master.securityType", "GROUP_CCMP");
        properties.put("net.interface.wlan.config.wifi.master.channel", "1 2 3");
        properties.put("net.interface.wlan.config.wifi.master.passphrase", new Password("password"));
        properties.put("net.interface.wlan.config.wifi.master.hardwareMode", "HW mode");
        properties.put("net.interface.wlan.config.wifi.master.broadcast", (Boolean) true);
        properties.put("net.interface.wlan.config.wifi.master.radioMode", "RADIO_MODE_80211a");
        properties.put("net.interface.wlan.config.wifi.master.pairwiseCiphers", "CCMP");
        properties.put("net.interface.wlan.config.wifi.master.ignoreSSID", (Boolean) true);

        WifiConfig expected = new WifiConfig();
        expected.setMode(WifiMode.MASTER);
        expected.setChannels(new int[] { 1, 2, 3 });
        expected.setSSID("ssid");
        expected.setDriver("driver");
        expected.setSecurity(WifiSecurity.GROUP_CCMP);
        expected.setPasskey("password");
        expected.setHardwareMode("HW mode");
        expected.setBroadcast(true);
        expected.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        expected.setPairwiseCiphers(WifiCiphers.CCMP);
        expected.setIgnoreSSID(true);

        List<NetConfig> netConfigs = WifiConfigurationInterpreter.populateConfiguration(properties, "wlan");

        assertNotNull(netConfigs);
        assertEquals(2, netConfigs.size());

        WifiConfig wifiConfig = (WifiConfig) netConfigs.get(0);
        
        assertEquals(expected, wifiConfig);
    }
    
    @Test
    public void testPopulateNetInterfaceConfigurationWifi() throws Throwable {
        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("if1");
        List<WifiInterfaceAddressConfig> interfaceAddresses = new ArrayList<>();
        interfaceAddresses.add(new WifiInterfaceAddressConfigImpl());
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddresses);

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.if1.type", "WIFI");
        properties.put("net.interface.if1.up", false);
        properties.put("net.interface.if1.wifi.capabilities", "CIPHER_CCMP CIPHER_TKIP");
        properties.put("net.interface.if1.config.wifi.mode", "ADHOC");
        properties.put("net.interface.if1.config.wifi.master.passphrase", "password");
        properties.put("net.interface.if1.config.wifi.infra.passphrase", "password");

        WifiInterfaceConfigImpl expected = new WifiInterfaceConfigImpl("if1");
        expected.setAutoConnect(false);
        expected.setUp(false);
        expected.setCapabilities(EnumSet.of(Capability.CIPHER_CCMP, Capability.CIPHER_TKIP));

        List<NetConfig> expectedNetConfigs = new ArrayList<>();
        
        WifiConfig netConfig1 = new WifiConfig();
        netConfig1.setMode(WifiMode.MASTER);
        netConfig1.setSSID("");
        netConfig1.setSecurity(WifiSecurity.NONE);
        netConfig1.setHardwareMode("b");
        netConfig1.setPasskey("password");
        expectedNetConfigs.add(netConfig1);

        WifiConfig netConfig2 = new WifiConfig();
        netConfig2.setMode(WifiMode.INFRA);
        netConfig2.setSSID("");
        netConfig2.setSecurity(WifiSecurity.NONE);
        netConfig2.setHardwareMode("b");
        netConfig2.setPasskey("password");
        netConfig2.setBgscan(new WifiBgscan(""));
        expectedNetConfigs.add(netConfig2);
        
        List<NetConfig> netConfigs = WifiConfigurationInterpreter.populateConfiguration(properties, "if1");

        assertEquals(expectedNetConfigs, netConfigs);
    }
}
