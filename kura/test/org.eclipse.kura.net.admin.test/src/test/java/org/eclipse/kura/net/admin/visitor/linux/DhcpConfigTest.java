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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.junit.Before;
import org.junit.Test;

public class DhcpConfigTest {

    private final String DIR = "/tmp/dhcpconfig";
    private final String FILE = "/dhcpd.conf-";
    private DhcpConfigWriter writer;

    @Before
    public void setup() {
        File f = new File(DIR);
        f.mkdirs();

        writer = new DhcpConfigWriter() {

            @Override
            protected String getConfigFilename(String interfaceName) {
                String file = DIR + FILE + interfaceName;

                return file;
            }
        };
    }

    @Test
    public void testCompleteNoDhcpd() throws KuraException, UnknownHostException, NoSuchFieldException {
        // file remains empty if no DHCP server software is found

        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", DhcpServerTool.NONE);

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

        NetConfigIP4 netConfigIP4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        netConfigs.add(netConfigIP4);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
        return config;
    }

    @Test
    public void testCompleteDhcpd() throws KuraException, NoSuchFieldException, IOException {
        // write out dhcpd's configuration

        DhcpServerTool dhcpTool = DhcpServerTool.DHCPD;
        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", dhcpTool);

        String interfaceName = "testinterface";

        // cleanup the old file
        new File(writer.getConfigFilename(interfaceName)).delete();

        NetworkConfiguration config = getTestConfig(interfaceName);

        writer.visit(config);

        File f = new File(writer.getConfigFilename(interfaceName));
        assertTrue(f.exists());

        verifyDhcpdFile(DIR + FILE + interfaceName);

        new File(writer.getConfigFilename(interfaceName)).delete();
    }

    private void verifyDhcpdFile(String configFilename) throws IOException {
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

    private String readFile(String configFilename) throws IOException {
        Path path = Paths.get(configFilename);
        List<String> readLinesList = Files.readAllLines(path);
        StringBuilder readLines = new StringBuilder();
        readLinesList.forEach(line -> {
            readLines.append(line).append("\n");
        });

        return readLines.toString();
    }

    private void verifyUdhcpdFile(String configFilename) throws IOException {
        String s = readFile(configFilename);

        assertTrue(s.contains("start 10.10.0.10"));
        assertTrue(s.contains("end 10.10.0.15"));
        assertTrue(s.contains("interface testinterface"));
        assertTrue(s.contains("pidfile /var/run/udhcpd-testinterface.pid"));
        assertTrue(s.contains("max_leases 5"));
        assertTrue(s.contains("auto_time 30"));
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
    public void testCompleteUdhcpd() throws KuraException, NoSuchFieldException, IOException {
        // write out dhcpd's configuration

        DhcpServerTool dhcpTool = DhcpServerTool.UDHCPD;
        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", dhcpTool);

        String interfaceName = "testinterface";

        // cleanup the old file
        new File(writer.getConfigFilename(interfaceName)).delete();

        NetworkConfiguration config = getTestConfig(interfaceName);

        writer.visit(config);

        File f = new File(writer.getConfigFilename(interfaceName));
        assertTrue(f.exists());

        // verifyFile(interfaceName, dhcpTool);
        verifyUdhcpdFile(DIR + FILE + interfaceName);

        new File(writer.getConfigFilename(interfaceName)).delete();
    }

}
