/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiBgscanModule;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.junit.Test;

public class WpaSupplicantConfigTest {

    @Test
    public void testReader() throws KuraException, NoSuchFieldException, IOException {
        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String pass = "Pa$&wrd";
        String ssid = "ID WITH SPACE";

        WpaSupplicantConfigReader reader = new WpaSupplicantConfigReader() {

            @Override
            protected String getWpaSupplicantConfigFilename(String ifaceName) {
                return dir + "/wpa-config-" + ifaceName;
            }

            @Override
            protected String getKuranetProperty(String key) {
                if (key.compareTo("net.interface." + intfName + ".config.wifi.infra.pingAccessPoint") == 0) {
                    return "true";
                } else if (key.compareTo("net.interface." + intfName + ".config.wifi.infra.ignoreSSID") == 0) {
                    return "true";
                } else if (key.compareTo("net.interface." + intfName + ".config.wifi.infra.driver") == 0) {
                    return "driver";
                }

                return null;
            }
        };

        String wpaFile = reader.getWpaSupplicantConfigFilename(intfName);
        try (FileWriter fw = new FileWriter(wpaFile)) {
            fw.write("ctrl_interface=/var/run/wpa_supplicant");
            fw.write("ctrl_interface_group=wheel");
            fw.write("network={");
            fw.write("  mode=0\n");
            fw.write("  ssid=\"" + ssid + "\"\n");
            fw.write("  scan_ssid=1\n");
            fw.write("  key_mgmt=WPA-PSK\n");
            fw.write("  psk=\"" + pass + "\"\n");
            fw.write("  proto=WPA RSN\n");
            fw.write("  pairwise=CCMP\n");
            fw.write("  group=CCMP\n");
            fw.write("  scan_freq=2447\n");
            fw.write("  bgscan=\"\"\n");
            fw.write("}");
        }

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);

        reader.visit(config);

        assertNotNull(netInterfaceConfig.getNetInterfaceAddresses());
        assertEquals(1, netInterfaceConfig.getNetInterfaceAddresses().size());

        WifiInterfaceAddressConfig addressConfig = netInterfaceConfig.getNetInterfaceAddresses().get(0);
        assertNotNull(addressConfig);
        List<NetConfig> configs = addressConfig.getConfigs();
        assertNotNull(configs);
        assertEquals(1, configs.size());

        WifiConfig cfg = (WifiConfig) configs.get(0);
        assertEquals(WifiBgscanModule.NONE, cfg.getBgscan().getModule());
        assertArrayEquals(new int[] { 8 }, cfg.getChannels());
        assertEquals("driver", cfg.getDriver());
        assertEquals("", cfg.getHardwareMode());
        assertEquals(WifiMode.INFRA, cfg.getMode());
        assertEquals(WifiCiphers.CCMP, cfg.getGroupCiphers());
        assertEquals(WifiCiphers.CCMP, cfg.getPairwiseCiphers());
        assertArrayEquals(pass.toCharArray(), cfg.getPasskey().getPassword());
        assertNull(cfg.getRadioMode());
        assertEquals(WifiSecurity.SECURITY_WPA_WPA2, cfg.getSecurity());
        assertEquals(ssid, cfg.getSSID());
        assertEquals(true, cfg.ignoreSSID());
    }

    @Test
    public void testReaderVariation2() throws KuraException, NoSuchFieldException, IOException {
        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String ssid = "ID WITH SPACE";

        WpaSupplicantConfigReader reader = new WpaSupplicantConfigReader() {

            @Override
            protected String getWpaSupplicantConfigFilename(String ifaceName) {
                return dir + "/wpa-config-" + ifaceName;
            }

            @Override
            protected String getKuranetProperty(String key) {
                if (key.compareTo("net.interface." + intfName + ".config.wifi.infra.pingAccessPoint") == 0) {
                    return "true";
                } else if (key.compareTo("net.interface." + intfName + ".config.wifi.infra.ignoreSSID") == 0) {
                    return "false";
                } else if (key.compareTo("net.interface." + intfName + ".config.wifi.infra.driver") == 0) {
                    return "";
                }

                return null;
            }
        };

        String wpaFile = reader.getWpaSupplicantConfigFilename(intfName);
        try (FileWriter fw = new FileWriter(wpaFile)) {
            fw.write("ctrl_interface=/var/run/wpa_supplicant");
            fw.write("ctrl_interface_group=wheel");
            fw.write("network={");
            fw.write("  mode=1\n");
            fw.write("  ssid=\"" + ssid + "\"\n");
            fw.write("  scan_ssid=1\n");
            fw.write("  key_mgmt=WPA-PSK\n");
            fw.write("  proto=RSN\n");
            fw.write("  pairwise=CCMP TKIP\n");
            fw.write("  group=CCMP TKIP\n");
            fw.write("  scan_freq=2452 2457\n");
            fw.write("  bgscan=\"\"\n");
            fw.write("}");
        }

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);

        reader.visit(config);

        assertNotNull(netInterfaceConfig.getNetInterfaceAddresses());
        assertEquals(1, netInterfaceConfig.getNetInterfaceAddresses().size());

        WifiInterfaceAddressConfig addressConfig = netInterfaceConfig.getNetInterfaceAddresses().get(0);
        assertNotNull(addressConfig);
        List<NetConfig> configs = addressConfig.getConfigs();
        assertNotNull(configs);
        assertEquals(1, configs.size());

        WifiConfig cfg = (WifiConfig) configs.get(0);
        assertEquals(WifiBgscanModule.NONE, cfg.getBgscan().getModule());
        assertArrayEquals(new int[] { 9, 10 }, cfg.getChannels());
        assertEquals("nl80211", cfg.getDriver());
        assertEquals("", cfg.getHardwareMode());
        assertEquals(WifiMode.INFRA, cfg.getMode());
        assertEquals(WifiCiphers.CCMP_TKIP, cfg.getGroupCiphers());
        assertEquals(WifiCiphers.CCMP_TKIP, cfg.getPairwiseCiphers());
        assertArrayEquals("".toCharArray(), cfg.getPasskey().getPassword());
        assertNull(cfg.getRadioMode());
        assertEquals(WifiSecurity.SECURITY_WPA2, cfg.getSecurity());
        assertEquals(ssid, cfg.getSSID());
        assertEquals(false, cfg.ignoreSSID());
    }

    @Test
    public void testReaderVariationWep() throws KuraException, NoSuchFieldException, IOException {
        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String pass = "Pa$&wrd";
        String ssid = "ID WITH SPACE";

        WpaSupplicantConfigReader reader = new WpaSupplicantConfigReader() {

            @Override
            protected String getWpaSupplicantConfigFilename(String ifaceName) {
                return dir + "/wpa-config-" + ifaceName;
            }

            @Override
            protected String getKuranetProperty(String key) {
                if (key.compareTo("net.interface." + intfName + ".config.wifi.infra.pingAccessPoint") == 0) {
                    return "true";
                } else if (key.compareTo("net.interface." + intfName + ".config.wifi.infra.ignoreSSID") == 0) {
                    return "false";
                } else if (key.compareTo("net.interface." + intfName + ".config.wifi.infra.driver") == 0) {
                    return "";
                }

                return null;
            }
        };

        String wpaFile = reader.getWpaSupplicantConfigFilename(intfName);
        try (FileWriter fw = new FileWriter(wpaFile)) {
            fw.write("ctrl_interface=/var/run/wpa_supplicant");
            fw.write("ctrl_interface_group=wheel");
            fw.write("network={");
            fw.write("  mode=1\n");
            fw.write("  ssid=\"" + ssid + "\"\n");
            fw.write("  scan_ssid=1\n");
            fw.write("  key_mgmt=WEP\n");
            fw.write("  proto=RSN\n");
            fw.write("  pairwise=TKIP\n");
            fw.write("  group=TKIP\n");
            fw.write("  wep_key0=" + pass + "\n");
            fw.write("  scan_freq=2452 2457\n");
            fw.write("  bgscan=\"\"\n");
            fw.write("}");
        }

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);

        reader.visit(config);

        assertNotNull(netInterfaceConfig.getNetInterfaceAddresses());
        assertEquals(1, netInterfaceConfig.getNetInterfaceAddresses().size());

        WifiInterfaceAddressConfig addressConfig = netInterfaceConfig.getNetInterfaceAddresses().get(0);
        assertNotNull(addressConfig);
        List<NetConfig> configs = addressConfig.getConfigs();
        assertNotNull(configs);
        assertEquals(1, configs.size());

        WifiConfig cfg = (WifiConfig) configs.get(0);
        assertEquals(WifiBgscanModule.NONE, cfg.getBgscan().getModule());
        assertArrayEquals(new int[] { 9, 10 }, cfg.getChannels());
        assertEquals("nl80211", cfg.getDriver());
        assertEquals("", cfg.getHardwareMode());
        assertEquals(WifiMode.INFRA, cfg.getMode());
        assertNull(cfg.getGroupCiphers());
        assertNull(cfg.getPairwiseCiphers());
        assertArrayEquals(pass.toCharArray(), cfg.getPasskey().getPassword());
        assertNull(cfg.getRadioMode());
        assertEquals(WifiSecurity.SECURITY_WEP, cfg.getSecurity());
        assertEquals(ssid, cfg.getSSID());
        assertEquals(false, cfg.ignoreSSID());
    }

    @Test
    public void testWriterDisabled() throws KuraException {
        // finishes before getting to file creation - helper for testing disabled interface log message

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String pass = "Pa$&wrd";
        String ssid = "ID WITH SPACE";

        WpaSupplicantConfigWriter writer = new WpaSupplicantConfigWriter();

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusDisabled, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);
    }

    @Test
    public void testWriterWepInfra() throws KuraException, NoSuchFieldException, IOException {
        System.setProperty("kura.os.version", "raspbian");

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String pass = "Pa$&w";

        WpaSupplicantConfigWriter writer = getWriter(dir);

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.INFRA);
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.ADHOC);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WEP);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.INFRA);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WEP);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSIDi");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211g);
        channels = new int[] { 2 };
        wifiConfig.setChannels(channels);
        WifiBgscan bgscan = new WifiBgscan("simple:1:2:3");
        wifiConfig.setBgscan(bgscan);
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);

        Map<String, String> map = (Map<String, String>) TestUtil.getFieldValue(writer, "map");

        assertEquals(3, map.size());
        assertEquals("false", map.get("net.interface." + intfName + ".config.wifi.infra.pingAccessPoint"));
        assertEquals("false", map.get("net.interface." + intfName + ".config.wifi.infra.ignoreSSID"));
        assertEquals("wifiDriver", map.get("net.interface." + intfName + ".config.wifi.infra.driver"));

        File f = new File(dir + "/wpaconf-" + intfName);
        assertTrue(f.exists());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        f.delete();

        String configFileContents = sb.toString();
        assertTrue(configFileContents.contains("#ctrl_interface_group=wheel\n"));
        assertFalse(configFileContents.contains("\nctrl_interface_group=wheel\n"));
        assertTrue(configFileContents.contains("network={\n"));
        assertTrue(configFileContents.contains("mode=0\n"));
        assertTrue(configFileContents.contains("ssid=\"testSSIDi\"\n"));
        assertTrue(configFileContents.contains("scan_ssid=1\n"));
        assertTrue(configFileContents.contains("key_mgmt=NONE\n"));
        assertTrue(configFileContents.contains("wep_key0=5061242677\n"));
        assertTrue(configFileContents.contains("scan_freq=2417\n"));
        assertTrue(configFileContents.contains("bgscan=\"simple:1:2:3\"\n"));
        assertTrue(configFileContents.contains("}"));
    }

    @Test
    public void testWriterWepAdhoc() throws KuraException, NoSuchFieldException, IOException {
        System.setProperty("kura.os.version", "raspbian");

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String pass = "12345678901234567890123456789012";

        WpaSupplicantConfigWriter writer = getWriter(dir);

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.ADHOC);
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.ADHOC);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WEP);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.INFRA);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WEP);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSIDi");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211g);
        channels = new int[] { 2 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);

        Map<String, String> map = (Map<String, String>) TestUtil.getFieldValue(writer, "map");

        assertEquals(1, map.size());
        assertEquals("wifiDriver", map.get("net.interface." + intfName + ".config.wifi.adhoc.driver"));

        File f = new File(dir + "/wpaconf-" + intfName);
        assertTrue(f.exists());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        f.delete();

        String configFileContents = sb.toString();
        assertTrue(configFileContents.contains("#ctrl_interface_group=wheel\n"));
        assertFalse(configFileContents.contains("\nctrl_interface_group=wheel\n"));
        assertTrue(configFileContents.contains("ap_scan=2\n"));
        assertTrue(configFileContents.contains("network={\n"));
        assertTrue(configFileContents.contains("mode=1\n"));
        assertTrue(configFileContents.contains("ssid=\"testSSID\"\n"));
        assertTrue(configFileContents.contains("key_mgmt=NONE\n"));
        assertTrue(configFileContents.contains("wep_key0=" + pass + "\n"));
        assertTrue(configFileContents.contains("frequency=2412\n"));
        assertTrue(configFileContents.contains("}"));
    }

    @Test
    public void testWriterWpaAdhoc() throws KuraException, NoSuchFieldException, IOException {
        System.setProperty("kura.os.version", "raspbian");

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String pass = "12345678901234567890123456789012";

        WpaSupplicantConfigWriter writer = getWriter(dir);

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.ADHOC);
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.ADHOC);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.INFRA);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSIDi");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211g);
        channels = new int[] { 2 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);

        Map<String, String> map = (Map<String, String>) TestUtil.getFieldValue(writer, "map");

        assertEquals(1, map.size());
        assertEquals("wifiDriver", map.get("net.interface." + intfName + ".config.wifi.adhoc.driver"));

        File f = new File(dir + "/wpaconf-" + intfName);
        assertTrue(f.exists());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        f.delete();

        String configFileContents = sb.toString();
        assertTrue(configFileContents.contains("#ctrl_interface_group=wheel\n"));
        assertFalse(configFileContents.contains("\nctrl_interface_group=wheel\n"));
        assertTrue(configFileContents.contains("ap_scan=2\n"));
        assertTrue(configFileContents.contains("network={\n"));
        assertTrue(configFileContents.contains("mode=1\n"));
        assertTrue(configFileContents.contains("ssid=\"testSSID\"\n"));
        assertTrue(configFileContents.contains("key_mgmt=WPA-NONE\n"));
        assertTrue(configFileContents.contains("psk=\"" + pass + "\"\n"));
        assertTrue(configFileContents.contains("frequency=2412\n"));
        assertTrue(configFileContents.contains("group=TKIP\n"));
        assertTrue(configFileContents.contains("pairwise=NONE\n"));
        assertTrue(configFileContents.contains("}"));
    }

    @Test
    public void testWriterWpaInfra() throws KuraException, NoSuchFieldException, IOException {
        System.setProperty("kura.os.version", "raspbian");

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String pass = "12345678901234567890123456789012";

        WpaSupplicantConfigWriter writer = getWriter(dir);

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.MASTER); // produces INFRA in the end
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.ADHOC);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.INFRA);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA_WPA2);
        wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP);
        wifiConfig.setGroupCiphers(WifiCiphers.CCMP_TKIP);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSIDi");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211g);
        channels = new int[] { 2 };
        wifiConfig.setChannels(channels);
        WifiBgscan bgscan = new WifiBgscan("learn:1:2:3");
        wifiConfig.setBgscan(bgscan);
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);

        Map<String, String> map = (Map<String, String>) TestUtil.getFieldValue(writer, "map");

        assertEquals(1, map.size());
        assertEquals("wifiDriver", map.get("net.interface." + intfName + ".config.wifi.infra.driver"));

        File f = new File(dir + "/wpaconf-" + intfName);
        assertTrue(f.exists());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        f.delete();

        String configFileContents = sb.toString();
        assertTrue(configFileContents.contains("#ctrl_interface_group=wheel\n"));
        assertFalse(configFileContents.contains("\nctrl_interface_group=wheel\n"));
        assertTrue(configFileContents.contains("network={\n"));
        assertTrue(configFileContents.contains("mode=0\n"));
        assertTrue(configFileContents.contains("ssid=\"testSSIDi\"\n"));
        assertTrue(configFileContents.contains("key_mgmt=WPA-PSK\n"));
        assertTrue(configFileContents.contains("psk=\"" + pass + "\"\n"));
        assertTrue(configFileContents.contains("scan_freq=2417\n"));
        assertTrue(configFileContents.contains("proto=WPA RSN\n"));
        assertTrue(configFileContents.contains("pairwise=CCMP\n"));
        assertTrue(configFileContents.contains("group=CCMP TKIP\n"));
        assertTrue(configFileContents.contains("bgscan=\"learn:1:2:3\"\n"));
        assertTrue(configFileContents.contains("}"));
    }

    @Test
    public void testWriterNoSecInfra() throws KuraException, NoSuchFieldException, IOException {
        System.setProperty("kura.os.version", "raspbian");

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String pass = "12345678901234567890123456789012";

        WpaSupplicantConfigWriter writer = getWriter(dir);

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.MASTER); // produces INFRA in the end
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.ADHOC);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.INFRA);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_NONE);
        wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP);
        wifiConfig.setGroupCiphers(WifiCiphers.CCMP_TKIP);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSIDi");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211g);
        channels = new int[] { 2 };
        wifiConfig.setChannels(channels);
        WifiBgscan bgscan = new WifiBgscan("invalid:1:2:3");
        wifiConfig.setBgscan(bgscan);
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);

        Map<String, String> map = (Map<String, String>) TestUtil.getFieldValue(writer, "map");

        assertEquals(1, map.size());
        assertEquals("wifiDriver", map.get("net.interface." + intfName + ".config.wifi.infra.driver"));

        File f = new File(dir + "/wpaconf-" + intfName);
        assertTrue(f.exists());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        f.delete();

        String configFileContents = sb.toString();
        assertTrue(configFileContents.contains("#ctrl_interface_group=wheel\n"));
        assertFalse(configFileContents.contains("\nctrl_interface_group=wheel\n"));
        assertTrue(configFileContents.contains("network={\n"));
        assertTrue(configFileContents.contains("mode=0\n"));
        assertTrue(configFileContents.contains("ssid=\"testSSIDi\"\n"));
        assertTrue(configFileContents.contains("scan_freq=2417\n"));
        assertTrue(configFileContents.contains("key_mgmt=NONE\n"));
        assertTrue(configFileContents.contains("bgscan=\"\"\n"));
        assertTrue(configFileContents.contains("}"));
    }

    @Test
    public void testWriterNoSecAdhoc() throws KuraException, NoSuchFieldException, IOException {
        System.setProperty("kura.os.version", "raspbian");

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";
        String pass = "12345678901234567890123456789012";

        WpaSupplicantConfigWriter writer = getWriter(dir);

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.ADHOC);
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.ADHOC);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_NONE);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211nHT20);
        WifiBgscan bgscan = new WifiBgscan("invalid:1:2:3");
        wifiConfig.setBgscan(bgscan);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.INFRA);
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);

        Map<String, String> map = (Map<String, String>) TestUtil.getFieldValue(writer, "map");

        assertEquals(1, map.size());
        assertEquals("wifiDriver", map.get("net.interface." + intfName + ".config.wifi.adhoc.driver"));

        File f = new File(dir + "/wpaconf-" + intfName);
        assertTrue(f.exists());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        f.delete();

        String configFileContents = sb.toString();
        assertTrue(configFileContents.contains("#ctrl_interface_group=wheel\n"));
        assertFalse(configFileContents.contains("\nctrl_interface_group=wheel\n"));
        assertTrue(configFileContents.contains("network={\n"));
        assertTrue(configFileContents.contains("mode=1\n"));
        assertTrue(configFileContents.contains("ssid=\"testSSID\"\n"));
        assertTrue(configFileContents.contains("frequency=2412\n"));
        assertTrue(configFileContents.contains("key_mgmt=NONE\n"));
        assertTrue(configFileContents.contains("}"));
        assertTrue(configFileContents.contains("ap_scan=2\n"));
    }

    @Test
    public void testWriterBadInfra() throws KuraException, NoSuchFieldException, IOException {
        System.setProperty("kura.os.version", "raspbian");

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";

        WpaSupplicantConfigWriter writer = getWriter(dir);

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.INFRA);
        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);

        Map<String, String> map = (Map<String, String>) TestUtil.getFieldValue(writer, "map");

        assertEquals(0, map.size());

        File f = new File(dir + "/wpaconf-" + intfName);
        assertFalse(f.exists());
    }

    @Test
    public void testWriterBadAdhoc() throws KuraException, NoSuchFieldException, IOException {
        System.setProperty("kura.os.version", "raspbian");

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";

        WpaSupplicantConfigWriter writer = getWriter(dir);

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.ADHOC);
        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);

        Map<String, String> map = (Map<String, String>) TestUtil.getFieldValue(writer, "map");

        assertEquals(0, map.size());

        File f = new File(dir + "/wpaconf-" + intfName);
        assertFalse(f.exists());
    }

    @Test
    public void testWriterBadMaster() throws KuraException, NoSuchFieldException, IOException {
        System.setProperty("kura.os.version", "raspbian");

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface";

        WpaSupplicantConfigWriter writer = getWriter(dir);

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("mon.test");
        config.addNetInterfaceConfig(netInterfaceConfig);

        netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.MASTER);
        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);

        writer.visit(config);

        Map<String, String> map = (Map<String, String>) TestUtil.getFieldValue(writer, "map");

        assertEquals(0, map.size());

        File f = new File(dir + "/wpaconf-" + intfName);
        assertFalse(f.exists());
    }

    private WpaSupplicantConfigWriter getWriter(String dir) {
        WpaSupplicantConfigWriter writer = new WpaSupplicantConfigWriter() {

            private Map<String, String> map = new HashMap<>();

            @Override
            protected String getFinalConfigFile(String ifaceName) {
                return dir + "/wpaconf-" + ifaceName;
            }

            @Override
            protected String getTemporaryConfigFile() {
                return dir + "/wpaconf-temp";
            }

            @Override
            protected String readResource(String path) throws IOException {
                URL url = new File("../../org.eclipse.kura.net.admin", path).getAbsoluteFile().toURI().toURL();

                String s = IOUtil.readResource(url);

                return s;
            }

            @Override
            protected void setKuranetProperty(String key, String value) throws IOException, KuraException {
                map.put(key, value);
            }
        };

        return writer;
    }

}
