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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.junit.Test;

public class HostapdConfigTest {

    private static final String TEMP_FILE = "/tmp/kura/hostapd/hostapd.config-temp";
    private static final String FINAL_FILE = "/tmp/kura/hostapd/hostapd.config-";

    @Test
    public void testWriteOnlyWifi() throws UnknownHostException, KuraException {
        // solely wifi AP configuration doesn't get written

        HostapdConfigWriter writer = new HostapdConfigWriter() {

            @Override
            protected File getTemporaryFile() {
                File f = new File(TEMP_FILE);
                f.getParentFile().mkdirs();

                f.deleteOnExit();

                return f;
            }
        };

        NetworkConfiguration config = new NetworkConfiguration();

        List<String> interfaces = new ArrayList<>();
        interfaces.add("testinterface");
        config.setModifiedInterfaceNames(interfaces);

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("testinterface");
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.MASTER);
        wifiConfig.setDriver("wifiDriver");
        netConfigs.add(wifiConfig);
        wifiConfig = new WifiConfig(); // add one without a driver
        wifiConfig.setMode(WifiMode.MASTER);
        netConfigs.add(wifiConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        writer.visit(config);

        File f = new File(TEMP_FILE);

        assertFalse("File shouldn't have been created", f.exists());
    }

    @Test
    public void testWriteUnsupportedSecurityType() throws UnknownHostException, KuraException {
        // don't configure a supported security type => exception
        HostapdConfigWriter writer = new HostapdConfigWriter() {

            @Override
            protected File getTemporaryFile() {
                File f = new File(TEMP_FILE);
                f.getParentFile().mkdirs();

                f.deleteOnExit();

                return f;
            }
        };

        NetworkConfiguration config = new NetworkConfiguration();

        List<String> interfaces = new ArrayList<>();
        interfaces.add("testinterface");
        config.setModifiedInterfaceNames(interfaces);

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("testinterface");
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.MASTER);
        wifiConfig.setDriver("wifiDriver");
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        try {
            writer.visit(config);
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
            assertTrue(e.getCause().getMessage().contains("security"));
            assertTrue(e.getCause().getMessage().contains(" null"));
        }

        File f = new File(TEMP_FILE);

        assertFalse("File shouldn't have been created", f.exists());

    }

    @Test
    public void testCompleteNoSecurity() throws UnknownHostException, KuraException {
        HostapdConfigWriter writer = new HostapdConfigWriter() {

            @Override
            protected File getTemporaryFile() {
                File f = new File(TEMP_FILE);
                f.getParentFile().mkdirs();

                f.deleteOnExit();

                return f;
            }

            @Override
            protected File getFinalFile(String ifaceName) {
                File f = new File(FINAL_FILE + ifaceName);
                f.getParentFile().mkdirs();

                f.deleteOnExit();

                return f;
            }

            @Override
            protected String readResource(String path) throws IOException {
                URL url = new File("../../org.eclipse.kura.net.admin", path).getAbsoluteFile().toURI().toURL();

                String s = IOUtil.readResource(url);

                return s;
            }
        };

        NetworkConfiguration config = new NetworkConfiguration();

        List<String> interfaces = new ArrayList<>();
        String intfName = "testinterface";
        interfaces.add(intfName);
        config.setModifiedInterfaceNames(interfaces);

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.MASTER);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.NONE);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        writer.visit(config);

        File f = new File(TEMP_FILE);
        File ff = new File(FINAL_FILE + intfName);

        assertFalse("File should have been moved", f.exists());
        assertTrue("File should have been created", ff.exists());

        HostapdConfigReader reader = new HostapdConfigReader() {

            @Override
            protected File getFinalFile(String ifaceName) {
                File f = new File(FINAL_FILE + ifaceName);
                f.getParentFile().mkdirs();

                f.deleteOnExit();

                return f;
            }
        };

        NetworkConfiguration config2 = new NetworkConfiguration();
        WifiInterfaceConfigImpl netInterfaceConfig2 = new WifiInterfaceConfigImpl(intfName);
        config2.addNetInterfaceConfig(netInterfaceConfig2);
        reader.visit(config2);

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config2
                .getNetInterfaceConfigs();
        assertEquals(1, netInterfaceConfigs.size());

        NetInterfaceConfig<? extends NetInterfaceAddressConfig> intfCfg0 = netInterfaceConfigs.get(0);
        assertEquals(intfName, intfCfg0.getName());
        List<? extends NetInterfaceAddressConfig> na0 = intfCfg0.getNetInterfaceAddresses();
        assertEquals(1, na0.size());
        List<NetConfig> na0cfgs = na0.get(0).getConfigs();
        assertEquals(1, na0cfgs.size()); // no, NetConfigIP4 will not be read, here...

        NetConfig na0cfg0 = na0cfgs.get(0);
        assertTrue(na0cfg0 instanceof WifiConfig);
        WifiConfig wc = (WifiConfig) na0cfg0;
        assertEquals(WifiMode.MASTER, wc.getMode());
        assertEquals("wifiDriver", wc.getDriver());
        assertEquals(WifiSecurity.SECURITY_NONE, wc.getSecurity());
        assertEquals("testSSID", wc.getSSID());
        assertEquals(WifiRadioMode.RADIO_MODE_80211a, wc.getRadioMode());
        assertArrayEquals(channels, wc.getChannels());
    }

    @Test
    public void testCompleteSecurityWep() throws UnknownHostException, KuraException {
        HostapdConfigWriter writer = new HostapdConfigWriter() {

            @Override
            protected File getTemporaryFile() {
                File f = new File(TEMP_FILE);
                f.getParentFile().mkdirs();

                f.deleteOnExit();

                return f;
            }

            @Override
            protected File getFinalFile(String ifaceName) {
                File f = new File(FINAL_FILE + ifaceName);
                f.getParentFile().mkdirs();

                f.deleteOnExit();

                return f;
            }

            @Override
            protected String readResource(String path) throws IOException {
                URL url = new File("../../org.eclipse.kura.net.admin", path).getAbsoluteFile().toURI().toURL();

                String s = IOUtil.readResource(url);

                return s;
            }
        };

        NetworkConfiguration config = new NetworkConfiguration();

        List<String> interfaces = new ArrayList<>();
        String intfName = "testinterfacewep";
        interfaces.add(intfName);
        config.setModifiedInterfaceNames(interfaces);

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.MASTER);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WEP);
        String pass = "CAFEBABE00";
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        writer.visit(config);

        File f = new File(TEMP_FILE);
        File ff = new File(FINAL_FILE + intfName);

        assertFalse("File should have been moved", f.exists());
        assertTrue("File should have been created", ff.exists());

        HostapdConfigReader reader = new HostapdConfigReader() {

            @Override
            protected File getFinalFile(String ifaceName) {
                File f = new File(FINAL_FILE + ifaceName);
                f.getParentFile().mkdirs();

                f.deleteOnExit();

                return f;
            }
        };

        NetworkConfiguration config2 = new NetworkConfiguration();
        WifiInterfaceConfigImpl netInterfaceConfig2 = new WifiInterfaceConfigImpl(intfName);
        config2.addNetInterfaceConfig(netInterfaceConfig2);
        reader.visit(config2);

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config2
                .getNetInterfaceConfigs();
        assertEquals(1, netInterfaceConfigs.size());

        NetInterfaceConfig<? extends NetInterfaceAddressConfig> intfCfg0 = netInterfaceConfigs.get(0);
        assertEquals(intfName, intfCfg0.getName());
        List<? extends NetInterfaceAddressConfig> na0 = intfCfg0.getNetInterfaceAddresses();
        assertEquals(1, na0.size());
        List<NetConfig> na0cfgs = na0.get(0).getConfigs();
        assertEquals(1, na0cfgs.size()); // no, NetConfigIP4 will not be read, here...

        NetConfig na0cfg0 = na0cfgs.get(0);
        assertTrue(na0cfg0 instanceof WifiConfig);
        WifiConfig wc = (WifiConfig) na0cfg0;
        assertEquals(WifiMode.MASTER, wc.getMode());
        assertEquals("wifiDriver", wc.getDriver());
        assertEquals(WifiSecurity.SECURITY_WEP, wc.getSecurity());
        assertArrayEquals(pass.toCharArray(), wc.getPasskey().getPassword());
        assertEquals("testSSID", wc.getSSID());
        assertEquals(WifiRadioMode.RADIO_MODE_80211b, wc.getRadioMode());
        assertArrayEquals(channels, wc.getChannels());
    }

    @Test
    public void testWepPass() throws Throwable {
        HostapdConfigWriter writer = new HostapdConfigWriter();

        String pass = "CAFEBABE00";
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.MASTER);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WEP);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);

        String hostapd = "wep_key0=KURA_WEP_KEY\n";

        String result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);

        assertEquals("wep_key0=CAFEBABE00\n", result);

        wifiConfig.setPasskey("12345678901234567890123456");
        result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
        assertEquals("wep_key0=12345678901234567890123456\n", result);

        wifiConfig.setPasskey("12345678901234567890123456789012");
        result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
        assertEquals("wep_key0=12345678901234567890123456789012\n", result);

        wifiConfig.setPasskey("12345");
        result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
        assertEquals("wep_key0=3132333435\n", result);

        wifiConfig.setPasskey("asdfg");
        result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
        assertEquals("wep_key0=6173646667\n", result);

        wifiConfig.setPasskey("asdfgasdfgasd");
        result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
        assertEquals("wep_key0=61736466676173646667617364\n", result);

        wifiConfig.setPasskey("asdfgasdfgasdfga");
        result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
        assertEquals("wep_key0=61736466676173646667617364666761\n", result);

        wifiConfig.setPasskey(null);
        try {
            result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
            fail("Exception was expected");
        } catch (NullPointerException e) {
            // OK
        }

        // test wrong password lengths
        wifiConfig.setPasskey("");
        try {
            result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
            fail("Exception was expected");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
        }

        String key = "";
        for (int i = 1; i < 35; i++) {
            key += i % 10;
            wifiConfig.setPasskey(key);
            try {
                result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
                if (i != 5 && i != 10 && i != 13 && i != 16 && i != 26 && i != 32) {
                    fail("Exception was expected: " + i);
                }
            } catch (KuraException e) {
                if (i != 5 && i != 10 && i != 13 && i != 16 && i != 26 && i != 32) {
                    assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
                } else {
                    fail("Password length should be OK: " + i);
                }
            }
        }

        // test wrong characters at different lengths
        wifiConfig.setPasskey("CAFEBABE0G");
        try {
            result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
            fail("Exception was expected");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("HEX"));
        }
        wifiConfig.setPasskey("1234567890123456789012345H");
        try {
            result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
            fail("Exception was expected");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("HEX"));
        }
        wifiConfig.setPasskey("1234567890123456789012345678901H");
        try {
            result = (String) TestUtil.invokePrivate(writer, "updateWepPassKey", wifiConfig, hostapd);
            fail("Exception was expected");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("HEX"));
        }
    }

}
