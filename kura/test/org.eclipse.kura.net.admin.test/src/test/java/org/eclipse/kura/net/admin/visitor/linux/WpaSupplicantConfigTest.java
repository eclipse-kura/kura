/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.visitor.linux;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.wifi.WifiBgscan;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.junit.Test;

public class WpaSupplicantConfigTest {

    @Test
    public void testWriterDisabled() throws KuraException {
        // finishes before getting to file creation - helper for testing disabled interface log message

        String dir = "/tmp/wpaconfig";
        new File(dir).mkdirs();

        String intfName = "testinterface-disabled";
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

        File f = new File(dir + "/" + intfName);
        assertFalse("File should have been moved", f.exists());
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
        String encodedPass = "3a859f5abdd14de95f99e572c31bc94650a0fd499b01a6d46056ea3bc18dc879";

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
        assertTrue(configFileContents.contains("psk=" + encodedPass + "\n"));
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
        String encodedPass = "3a859f5abdd14de95f99e572c31bc94650a0fd499b01a6d46056ea3bc18dc879";

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
        assertTrue(configFileContents.contains("psk=" + encodedPass + "\n"));
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

        File f = new File(dir + "/wpaconf-" + intfName);
        assertFalse(f.exists());
    }

    private WpaSupplicantConfigWriter getWriter(String dir) {
        WpaSupplicantConfigWriter writer = new WpaSupplicantConfigWriter() {

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
        };

        CommandExecutorService esMock = mock(CommandExecutorService.class);
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        status.setOutputStream(new ByteArrayOutputStream());
        String networkConfig = "network={\n" + "        ssid=\"testSSID\"\n"
                + "        #psk=\"12345678901234567890123456789012\"\n"
                + "        psk=3a859f5abdd14de95f99e572c31bc94650a0fd499b01a6d46056ea3bc18dc879\n" + "}";

        Writer networkConfigwriter = new OutputStreamWriter(status.getOutputStream(), StandardCharsets.UTF_8);
        try {
            networkConfigwriter.write(networkConfig);
            networkConfigwriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(esMock.execute(anyObject())).thenReturn(status);

        writer.setExecutorService(esMock);

        return writer;
    }

}
