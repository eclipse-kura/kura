/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.junit.Before;
import org.junit.Test;


public class DhcpConfigTest {

    private DhcpConfigWriter writer;

    @Before
    public void setup() {
        String dir = "/tmp/dhcpconfig";
        File f = new File(dir);
        f.mkdirs();

        writer = new DhcpConfigWriter() {

            @Override
            protected String getConfigFilename(String interfaceName) {
                String file = dir + "/dhcpd.conf-" + interfaceName;

                return file;
            }

            @Override
            protected Properties getKuranetProperties() {
                return new Properties();
            }

            @Override
            protected void storeKuranetProperties(Properties kuraExtendedProps) throws IOException, KuraException {
            }
        };
    }

    @Test
    public void testCompleteNoDhcpd() throws KuraException, UnknownHostException, NoSuchFieldException {
        // file remains empty if no DHCP server software is found

        TestUtil.setFieldValue(new DhcpServerManager(), "dhcpServerTool", DhcpServerTool.NONE);

        String interfaceName = "testinterface";

        // cleanup the old file
        new File(writer.getConfigFilename(interfaceName)).delete();

        NetworkConfiguration config = getTestConfig(interfaceName);

        writer.visit(config);

        File f = new File(writer.getConfigFilename(interfaceName));
        assertTrue(f.exists());
        assertEquals(0, f.length());
        f.delete();
    }

    private NetworkConfiguration getTestConfig(String interfaceName) throws UnknownHostException, KuraException {
        NetworkConfiguration config = new NetworkConfiguration();

        List<String> interfaces = new ArrayList<>();
        interfaces.add(interfaceName);
        config.setModifiedInterfaceNames(interfaces);

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        boolean enabled = true;
        int defaultLeaseTime = 900;
        int maximumLeaseTime = 1000;
        boolean passDns = true;
        DhcpServerCfg svrCfg = new DhcpServerCfg(interfaceName, enabled, defaultLeaseTime, maximumLeaseTime, passDns);
        IP4Address subnet = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, 0x00 });
        IP4Address subnetMask = (IP4Address) IPAddress
                .getByAddress(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00 });
        short prefix = 24;
        IP4Address routerAddress = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, (byte) 0xFA });
        IP4Address rangeStart = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, 0x0A });
        IP4Address rangeEnd = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, 0x0F });
        List<IP4Address> dnsServers = new ArrayList<>();
        IP4Address dnsAddress = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, (byte) 0xFE });
        dnsServers.add(dnsAddress);
        DhcpServerCfgIP4 svrCfg4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix, routerAddress, rangeStart, rangeEnd,
                dnsServers);
        DhcpServerConfig4 netConfig = new DhcpServerConfigIP4(svrCfg, svrCfg4);
        netConfigs.add(netConfig);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        return config;
    }

    @Test
    public void testCompleteDhcpd() throws KuraException, UnknownHostException, NoSuchFieldException {
        // write out dhcpd's configuration

        DhcpServerTool dhcpTool = DhcpServerTool.DHCPD;
        TestUtil.setFieldValue(new DhcpServerManager(), "dhcpServerTool", dhcpTool);

        String interfaceName = "testinterface";

        // cleanup the old file
        new File(writer.getConfigFilename(interfaceName)).delete();

        NetworkConfiguration config = getTestConfig(interfaceName);

        writer.visit(config);

        File f = new File(writer.getConfigFilename(interfaceName));
        assertTrue(f.exists());

        verifyFile(interfaceName, dhcpTool);

        new File(writer.getConfigFilename(interfaceName)).delete();
    }

    private void verifyFile(String interfaceName, DhcpServerTool dhcpTool) throws KuraException, UnknownHostException {
        DhcpConfigReader reader = new DhcpConfigReader() {

            @Override
            protected String getConfigFilename(String interfaceName) {
                String dir = "/tmp/dhcpconfig";
                return dir + "/dhcpd.conf-" + interfaceName;
            }

            @Override
            protected Properties getKuranetProperties() {
                return new Properties();
            }
        };

        NetworkConfiguration config2 = new NetworkConfiguration();
        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(interfaceName);
        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        config2.addNetInterfaceConfig(netInterfaceConfig);

        reader.visit(config2);

        IP4Address subnet = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, 0x00 });
        IP4Address subnetMask = (IP4Address) IPAddress
                .getByAddress(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00 });
        IP4Address routerAddress = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, (byte) 0xFA });
        IP4Address rangeStart = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, 0x0A });
        IP4Address rangeEnd = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, 0x0F });
        IP4Address dnsAddress = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, (byte) 0xFE });

        assertNotNull(wifiInterfaceAddressConfig.getConfigs());
        assertEquals(1, wifiInterfaceAddressConfig.getConfigs().size());
        NetConfig cfg = wifiInterfaceAddressConfig.getConfigs().iterator().next();
        assertTrue(cfg instanceof DhcpServerConfigIP4);
        DhcpServerConfigIP4 cfgdhcp = (DhcpServerConfigIP4) cfg;
        assertEquals(interfaceName, cfgdhcp.getInterfaceName());
        assertNotNull(cfgdhcp.getDnsServers());
        assertEquals(24, cfgdhcp.getPrefix());
        assertEquals(subnet, cfgdhcp.getSubnet());
        assertEquals(subnetMask, cfgdhcp.getSubnetMask());
        assertEquals(rangeStart, cfgdhcp.getRangeStart());
        assertEquals(rangeEnd, cfgdhcp.getRangeEnd());
        assertEquals(routerAddress, cfgdhcp.getRouterAddress());
        assertEquals(true, cfgdhcp.isEnabled());
        assertEquals(true, cfgdhcp.isPassDns());
        assertEquals(900, cfgdhcp.getDefaultLeaseTime());
        assertEquals(1, cfgdhcp.getDnsServers().size());
        assertEquals(dnsAddress, cfgdhcp.getDnsServers().get(0));

        // address a peculiarity of udhcpd config
        if (dhcpTool.equals(DhcpServerTool.DHCPD)) {
            assertEquals(1000, cfgdhcp.getMaximumLeaseTime());

            verifyDhcpdFile(reader.getConfigFilename(interfaceName));
        } else if (dhcpTool.equals(DhcpServerTool.UDHCPD)) {
            assertEquals(900, cfgdhcp.getMaximumLeaseTime());

            verifyUdhcpdFile(reader.getConfigFilename(interfaceName));
        }
    }

    private void verifyDhcpdFile(String configFilename) {
        String s = readFile(configFilename);

        assertTrue(s.contains("# enabled? true"));
        assertTrue(s.contains("# prefix: 24"));
        assertTrue(s.contains("# pass DNS? true"));
        assertTrue(s.contains("subnet 10.10.0.0 netmask 255.255.255.0 {"));
        assertTrue(s.contains("option domain-name-servers 10.10.0.254;"));
        assertTrue(s.contains("interface testinterface;"));
        assertTrue(s.contains("option routers 10.10.0.250;"));
        assertTrue(s.contains("default-lease-time 900;"));
        assertTrue(s.contains("max-lease-time 1000;"));
        assertTrue(s.contains("pool {"));
        assertTrue(s.contains("range 10.10.0.10 10.10.0.15;"));
    }

    private String readFile(String configFilename) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader fr = new BufferedReader(new FileReader(configFilename))) {
            String s = null;
            while ((s = fr.readLine()) != null) {
                sb.append(s).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
        }

        return null;
    }

    private void verifyUdhcpdFile(String configFilename) {
        String s = readFile(configFilename);

        assertTrue(s.contains("start 10.10.0.10"));
        assertTrue(s.contains("end 10.10.0.15"));
        assertTrue(s.contains("interface testinterface"));
        assertTrue(s.contains("pidfile /var/run/udhcpd-testinterface.pid"));
        assertTrue(s.contains("max_leases 5"));
        assertTrue(s.contains("auto_time 0"));
        assertTrue(s.contains("decline_time 900"));
        assertTrue(s.contains("conflict_time 900"));
        assertTrue(s.contains("offer_time 900"));
        assertTrue(s.contains("min_lease 900"));
        assertTrue(s.contains("opt subnet 255.255.255.0"));
        assertTrue(s.contains("opt router 10.10.0.250"));
        assertTrue(s.contains("opt lease 900"));
        assertTrue(s.contains("opt dns 10.10.0.254"));
    }

    @Test
    public void testCompleteUdhcpd() throws KuraException, UnknownHostException, NoSuchFieldException {
        // write out dhcpd's configuration

        DhcpServerTool dhcpTool = DhcpServerTool.UDHCPD;
        TestUtil.setFieldValue(new DhcpServerManager(), "dhcpServerTool", dhcpTool);

        String interfaceName = "testinterface";

        // cleanup the old file
        new File(writer.getConfigFilename(interfaceName)).delete();

        NetworkConfiguration config = getTestConfig(interfaceName);

        writer.visit(config);

        File f = new File(writer.getConfigFilename(interfaceName));
        assertTrue(f.exists());

        verifyFile(interfaceName, dhcpTool);

        new File(writer.getConfigFilename(interfaceName)).delete();
    }

}
