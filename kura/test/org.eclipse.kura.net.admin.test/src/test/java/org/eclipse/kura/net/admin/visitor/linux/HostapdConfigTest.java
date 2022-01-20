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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
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

    private static final String REGULATORY_DOMAIN = "global\n" + "country IT: DFS-ETSI\n"
            + "(2400 - 2483 @ 40), (N/A, 20), (N/A)\n" + "(5150 - 5250 @ 80), (N/A, 23), (N/A), NO-OUTDOOR, AUTO-BW\n"
            + "(5250 - 5350 @ 80), (N/A, 20), (0 ms), NO-OUTDOOR, DFS, AUTO-BW\n"
            + "(5470 - 5725 @ 160), (N/A, 26), (0 ms), DFS\n" + "(5725 - 5875 @ 80), (N/A, 13), (N/A)\n"
            + "(57000 - 66000 @ 2160), (N/A, 40), (N/A)";

    private static final String IW_INTERFACE_INFO = "Interface wlan0\n" + "        ifindex 3\n" + "        wdev 0x1\n"
            + "        addr e4:5f:01:35:80:32\n" + "        type managed\n" + "        wiphy 0\n"
            + "        channel 1 (2412 MHz), width: 20 MHz, center1: 2412 MHz\n" + "        txpower 31.00 dBm";

    private static final String DFS_PART_IW_INFO = "        Supported extended features:\n"
            + "                * [ 4WAY_HANDSHAKE_STA_PSK ]: 4-way handshake with PSK in station mode\n"
            + "                * [ 4WAY_HANDSHAKE_STA_1X ]: 4-way handshake with 802.1X in station mode\n"
            + "                * [ DFS_OFFLOAD ]: DFS offload";

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

        CommandExecutorService esMock = mock(CommandExecutorService.class);
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        when(esMock.execute(anyObject())).thenReturn(status);

        writer.setExecutorService(esMock);
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
            CommandExecutorService esMock = mock(CommandExecutorService.class);
            CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
            when(esMock.execute(anyObject())).thenReturn(status);

            writer.setExecutorService(esMock);
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
    public void testCompleteNoSecurity() throws KuraException, IOException {
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

        CommandExecutorService esMock = mock(CommandExecutorService.class);
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));

        Command getRegDom = new Command(new String[] { "iw", "reg", "get" });
        CommandStatus iwRegGetStatus = new CommandStatus(getRegDom, new LinuxExitStatus(0));
        iwRegGetStatus.setOutputStream(loadStringToOutPutStream(REGULATORY_DOMAIN));

        Command iwInterFaceInfoCommand = new Command(new String[] { "iw", intfName, "info" });
        CommandStatus iwInterFaceInfoCommandStatus = new CommandStatus(iwInterFaceInfoCommand, new LinuxExitStatus(0));
        iwInterFaceInfoCommandStatus.setOutputStream(loadStringToOutPutStream(IW_INTERFACE_INFO));

        Command iwPhyInterfaceInfoCommand = new Command(new String[] { "iw", "phy0", "info" });
        CommandStatus iwPhyInterfaceInfoCommandStatus = new CommandStatus(iwPhyInterfaceInfoCommand,
                new LinuxExitStatus(0));
        iwPhyInterfaceInfoCommandStatus.setOutputStream(loadStringToOutPutStream(DFS_PART_IW_INFO));

        when(esMock.execute(anyObject())).thenReturn(status);
        when(esMock.execute(getRegDom)).thenReturn(iwRegGetStatus);
        when(esMock.execute(iwInterFaceInfoCommand)).thenReturn(iwInterFaceInfoCommandStatus);
        when(esMock.execute(iwPhyInterfaceInfoCommand)).thenReturn(iwPhyInterfaceInfoCommandStatus);

        writer.setExecutorService(esMock);
        writer.visit(config);

        File f = new File(TEMP_FILE);
        File ff = new File(FINAL_FILE + intfName);

        assertFalse("File should have been moved", f.exists());
        assertTrue("File should have been created", ff.exists());
        verifyHostapdNoSecurityFileContent(FINAL_FILE + intfName);
    }

    @Test
    public void testCompleteSecurityWep() throws KuraException, IOException {
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

        CommandExecutorService esMock = mock(CommandExecutorService.class);
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));

        Command getRegDom = new Command(new String[] { "iw", "reg", "get" });
        CommandStatus iwRegGetStatus = new CommandStatus(getRegDom, new LinuxExitStatus(0));
        iwRegGetStatus.setOutputStream(loadStringToOutPutStream(REGULATORY_DOMAIN));

        Command iwInterFaceInfoCommand = new Command(new String[] { "iw", intfName, "info" });
        CommandStatus iwInterFaceInfoCommandStatus = new CommandStatus(iwInterFaceInfoCommand, new LinuxExitStatus(0));
        iwInterFaceInfoCommandStatus.setOutputStream(loadStringToOutPutStream(IW_INTERFACE_INFO));

        Command iwPhyInterfaceInfoCommand = new Command(new String[] { "iw", "phy0", "info" });
        CommandStatus iwPhyInterfaceInfoCommandStatus = new CommandStatus(iwPhyInterfaceInfoCommand,
                new LinuxExitStatus(0));
        iwPhyInterfaceInfoCommandStatus.setOutputStream(loadStringToOutPutStream(DFS_PART_IW_INFO));

        when(esMock.execute(anyObject())).thenReturn(status);
        when(esMock.execute(getRegDom)).thenReturn(iwRegGetStatus);
        when(esMock.execute(iwInterFaceInfoCommand)).thenReturn(iwInterFaceInfoCommandStatus);
        when(esMock.execute(iwPhyInterfaceInfoCommand)).thenReturn(iwPhyInterfaceInfoCommandStatus);

        writer.setExecutorService(esMock);
        writer.visit(config);

        File f = new File(TEMP_FILE);
        File ff = new File(FINAL_FILE + intfName);

        assertFalse("File should have been moved", f.exists());
        assertTrue("File should have been created", ff.exists());
        verifyHostapdWepFileContent(FINAL_FILE + intfName);
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

    @Test
    public void testCompleteSecurityWpa() throws KuraException, IOException {
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
        String intfName = "testinterfacewpa";
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
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA2);
        String pass = "CAFEBABE00";
        wifiConfig.setPasskey(pass);
        wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP_TKIP);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211nHT40below);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);
        netConfigs.add(wifiConfig);
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true, true);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        CommandExecutorService esMock = mock(CommandExecutorService.class);
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));

        Command getRegDom = new Command(new String[] { "iw", "reg", "get" });
        CommandStatus iwRegGetStatus = new CommandStatus(getRegDom, new LinuxExitStatus(0));
        iwRegGetStatus.setOutputStream(loadStringToOutPutStream(REGULATORY_DOMAIN));

        Command iwInterFaceInfoCommand = new Command(new String[] { "iw", intfName, "info" });
        CommandStatus iwInterFaceInfoCommandStatus = new CommandStatus(iwInterFaceInfoCommand, new LinuxExitStatus(0));
        iwInterFaceInfoCommandStatus.setOutputStream(loadStringToOutPutStream(IW_INTERFACE_INFO));

        Command iwPhyInterfaceInfoCommand = new Command(new String[] { "iw", "phy0", "info" });
        CommandStatus iwPhyInterfaceInfoCommandStatus = new CommandStatus(iwPhyInterfaceInfoCommand,
                new LinuxExitStatus(0));
        iwPhyInterfaceInfoCommandStatus.setOutputStream(loadStringToOutPutStream(DFS_PART_IW_INFO));

        when(esMock.execute(anyObject())).thenReturn(status);
        when(esMock.execute(getRegDom)).thenReturn(iwRegGetStatus);
        when(esMock.execute(iwInterFaceInfoCommand)).thenReturn(iwInterFaceInfoCommandStatus);
        when(esMock.execute(iwPhyInterfaceInfoCommand)).thenReturn(iwPhyInterfaceInfoCommandStatus);

        writer.setExecutorService(esMock);
        writer.visit(config);

        File f = new File(TEMP_FILE);
        File ff = new File(FINAL_FILE + intfName);

        assertFalse("File should have been moved", f.exists());
        assertTrue("File should have been created", ff.exists());
        verifyHostapdWpaFileContent(FINAL_FILE + intfName);
    }

    @Test
    public void testWpa() throws Throwable {
        HostapdConfigWriter writer = new HostapdConfigWriter();

        String pass = "CAFEBABE00";
        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.MASTER);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA);
        wifiConfig.setPairwiseCiphers(WifiCiphers.TKIP);
        wifiConfig.setPasskey(pass);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);

        String hostapd = "wpa=KURA_SECURITY\nwpa_pairwise=KURA_PAIRWISE_CIPHER\nwpa_passphrase=KURA_PASSPHRASE\n";

        String result = (String) TestUtil.invokePrivate(writer, "updateWPA", wifiConfig, hostapd);

        assertEquals("wpa=1\nwpa_pairwise=TKIP\nwpa_passphrase=CAFEBABE00\n", result);

        wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA2);
        wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP);
        result = (String) TestUtil.invokePrivate(writer, "updateWPA", wifiConfig, hostapd);
        assertEquals("wpa=2\nwpa_pairwise=CCMP\nwpa_passphrase=CAFEBABE00\n", result);

        wifiConfig.setSecurity(WifiSecurity.SECURITY_WPA_WPA2);
        wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP_TKIP);
        wifiConfig.setPasskey("CAFEBABE000000000");
        result = (String) TestUtil.invokePrivate(writer, "updateWPA", wifiConfig, hostapd);
        assertEquals("wpa=3\nwpa_pairwise=CCMP TKIP\nwpa_passphrase=CAFEBABE000000000\n", result);

        // test no cipher
        wifiConfig.setPairwiseCiphers(null);
        try {
            result = (String) TestUtil.invokePrivate(writer, "updateWPA", wifiConfig, hostapd);
            fail("Exception was expected");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
        }

        // test wrong password lengths
        wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP_TKIP);
        wifiConfig.setPasskey("");
        try {
            result = (String) TestUtil.invokePrivate(writer, "updateWPA", wifiConfig, hostapd);
            fail("Exception was expected");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
        }

        String key = "";
        for (int i = 1; i < 70; i++) {
            key += i % 10;
            wifiConfig.setPasskey(key);
            try {
                result = (String) TestUtil.invokePrivate(writer, "updateWPA", wifiConfig, hostapd);
                if (i < 8 || i >= 64) {
                    fail("Exception was expected: " + i);
                }
            } catch (KuraException e) {
                if (i < 8 || i >= 64) {
                    assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
                } else {
                    fail("Password length should be OK: " + i);
                }
            }
        }
    }

    @Test
    public void testRadioModes() throws Throwable {
        HostapdConfigWriter writer = new HostapdConfigWriter();

        WifiConfig wifiConfig = new WifiConfig();
        wifiConfig.setMode(WifiMode.MASTER);
        wifiConfig.setDriver("wifiDriver");
        wifiConfig.setSecurity(WifiSecurity.NONE);
        wifiConfig.setSSID("testSSID");
        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211a);
        int[] channels = { 1 };
        wifiConfig.setChannels(channels);

        String hostapd = "hw_mode=KURA_HW_MODE\nwme_enabled=KURA_WME_ENABLED\nieee80211n=KURA_IEEE80211N\nht_capab=KURA_HTCAPAB";

        String result = (String) TestUtil.invokePrivate(writer, "updateRadioMode", wifiConfig, hostapd);

        assertEquals("hw_mode=a\nwme_enabled=0\nieee80211n=0\n", result);

        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
        result = (String) TestUtil.invokePrivate(writer, "updateRadioMode", wifiConfig, hostapd);
        assertEquals("hw_mode=b\nwme_enabled=0\nieee80211n=0\n", result);

        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211g);
        result = (String) TestUtil.invokePrivate(writer, "updateRadioMode", wifiConfig, hostapd);
        assertEquals("hw_mode=g\nwme_enabled=0\nieee80211n=0\n", result);

        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211nHT20);
        result = (String) TestUtil.invokePrivate(writer, "updateRadioMode", wifiConfig, hostapd);
        assertEquals("hw_mode=g\nwme_enabled=1\nieee80211n=1\nht_capab=[SHORT-GI-20]", result);

        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211nHT40above);
        result = (String) TestUtil.invokePrivate(writer, "updateRadioMode", wifiConfig, hostapd);
        assertEquals("hw_mode=g\nwme_enabled=1\nieee80211n=1\nht_capab=[HT40+][SHORT-GI-20][SHORT-GI-40]", result);

        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211nHT40below);
        result = (String) TestUtil.invokePrivate(writer, "updateRadioMode", wifiConfig, hostapd);
        assertEquals("hw_mode=g\nwme_enabled=1\nieee80211n=1\nht_capab=[HT40-][SHORT-GI-20][SHORT-GI-40]", result);

        wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211_AC);
        result = (String) TestUtil.invokePrivate(writer, "updateRadioMode", wifiConfig, hostapd);
        assertEquals("hw_mode=a\nwme_enabled=1\nieee80211n=1\n", result);

        wifiConfig.setRadioMode(null);
        try {
            result = (String) TestUtil.invokePrivate(writer, "updateRadioMode", wifiConfig, hostapd);
            fail("Exception was expected");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INTERNAL_ERROR, e.getCode());
        }
    }

    private void verifyHostapdNoSecurityFileContent(String configFilename) throws IOException {
        String s = readFile(configFilename);

        assertTrue(s.contains("# /etc/hostapd/hostapd.conf"));
        assertTrue(s.contains("interface=testinterface"));
        assertTrue(s.contains("driver=wifiDriver"));
        assertTrue(s.contains("ssid=testSSID"));
        assertTrue(s.contains("hw_mode=a"));
        assertTrue(s.contains("wme_enabled=0"));
        assertTrue(s.contains("ieee80211n=0"));
        assertTrue(s.contains("channel=1"));
        assertTrue(s.contains("logger_syslog=-1"));
        assertTrue(s.contains("logger_syslog_level=2"));
        assertTrue(s.contains("logger_stdout=-1"));
        assertTrue(s.contains("logger_stdout_level=2"));
        assertTrue(s.contains("dump_file=/tmp/hostapd.dump"));
        assertTrue(s.contains("ignore_broadcast_ssid=0"));
    }

    private void verifyHostapdWepFileContent(String configFilename) throws IOException {
        String s = readFile(configFilename);

        assertTrue(s.contains("# /etc/hostapd/hostapd.conf"));
        assertTrue(s.contains("interface=testinterfacewep"));
        assertTrue(s.contains("driver=wifiDriver"));
        assertTrue(s.contains("ssid=testSSID"));
        assertTrue(s.contains("hw_mode=b"));
        assertTrue(s.contains("wme_enabled=0"));
        assertTrue(s.contains("ieee80211n=0"));
        assertTrue(s.contains("channel=1"));
        assertTrue(s.contains("logger_syslog=-1"));
        assertTrue(s.contains("logger_syslog_level=2"));
        assertTrue(s.contains("logger_stdout=-1"));
        assertTrue(s.contains("logger_stdout_level=2"));
        assertTrue(s.contains("dump_file=/tmp/hostapd.dump"));
        assertTrue(s.contains("wep_key0=CAFEBABE00"));
        assertTrue(s.contains("ignore_broadcast_ssid=0"));
    }

    private void verifyHostapdWpaFileContent(String configFilename) throws IOException {
        String s = readFile(configFilename);

        assertTrue(s.contains("# /etc/hostapd/hostapd.conf"));
        assertTrue(s.contains("interface=testinterfacewpa"));
        assertTrue(s.contains("driver=wifiDriver"));
        assertTrue(s.contains("ssid=testSSID"));
        assertTrue(s.contains("hw_mode=g"));
        assertTrue(s.contains("wme_enabled=1"));
        assertTrue(s.contains("ieee80211n=1"));
        assertTrue(s.contains("ht_capab=[HT40-][SHORT-GI-20][SHORT-GI-40]"));
        assertTrue(s.contains("channel=1"));
        assertTrue(s.contains("logger_syslog=-1"));
        assertTrue(s.contains("logger_syslog_level=2"));
        assertTrue(s.contains("logger_stdout=-1"));
        assertTrue(s.contains("logger_stdout_level=2"));
        assertTrue(s.contains("dump_file=/tmp/hostapd.dump"));
        assertTrue(s.contains("wpa=2"));
        assertTrue(s.contains("wpa_passphrase=CAFEBABE00"));
        assertTrue(s.contains("wpa_key_mgmt=WPA-PSK"));
        assertTrue(s.contains("wpa_pairwise=CCMP TKIP"));
        assertTrue(s.contains("wpa_group_rekey=600"));
        assertTrue(s.contains("wpa_gmk_rekey=86400"));
        assertTrue(s.contains("ignore_broadcast_ssid=0"));
    }

    private String readFile(String configFilename) throws IOException {
        Path path = Paths.get(configFilename);
        List<String> readLinesList = Files.readAllLines(path);
        StringBuilder readLines = new StringBuilder();
        readLinesList.forEach(line -> {
            readLines.append(line).append("\n");
        });

        return readLines.toString();
    }

    private OutputStream loadStringToOutPutStream(String string) throws IOException {
        OutputStream os = new ByteArrayOutputStream();

        OutputStreamWriter osw = new OutputStreamWriter(os);
        osw.write(string);
        osw.flush();

        return os;
    }
}
