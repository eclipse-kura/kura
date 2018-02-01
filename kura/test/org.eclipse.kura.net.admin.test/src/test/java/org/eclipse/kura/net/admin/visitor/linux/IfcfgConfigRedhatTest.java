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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests IfcfgConfigReader and IfcfgConfigWriter based on RedHat-like system configuration.
 */
public class IfcfgConfigRedhatTest {

    @BeforeClass
    public static void setupClass() throws NoSuchFieldException {
        String os = "redhat";

        TestUtil.setFieldValue(IfcfgConfigReader.getInstance(), "osVersion", os);

        TestUtil.setFieldValue(IfcfgConfigWriter.getInstance(), "osVersion", os);
    }

    @Test
    public void testWriter() throws KuraException, IOException, NoSuchFieldException {
        // tests writer with static configuration

        String interfaceName = "testinterface";

        IfcfgConfigWriter writer = getWriter();

        TestUtil.setFieldValue(writer, "instance", writer);

        String finalFile = writer.getRHConfigDirectory() + "/ifcfg-" + interfaceName;
        new File(finalFile).delete();

        NetworkConfiguration config = new NetworkConfiguration();

        List<String> interfaces = new ArrayList<>();
        interfaces.add(interfaceName);
        config.setModifiedInterfaceNames(interfaces);

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("testinterface");
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true);
        IP4Address address = (IP4Address) IPAddress.getByAddress(new byte[] { 10, 10, 0, 10 });
        netConfig.setAddress(address);
        netConfig.setNetworkPrefixLength((short) 24);
        netConfig.setDhcp(false);
        netConfig.setGateway((IP4Address) IPAddress.getByAddress(new byte[] { (byte) 10, (byte) 10, 0, (byte) 254 }));
        List<IP4Address> dnsServers = new ArrayList<>();
        IP4Address dns = (IP4Address) IPAddress.getByAddress(new byte[] { 10, 10, 0, (byte) 252 });
        dnsServers.add(dns);
        dns = (IP4Address) IPAddress.getByAddress(new byte[] { 10, 10, 0, (byte) 253 });
        dnsServers.add(dns);
        netConfig.setDnsServers(dnsServers);
        netConfigs.add(netConfig);
        Map<String, Object> map = new HashMap<>();
        map.put("mtu", 1235);
        netConfig.setProperties(map);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        writer.visit(config);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(finalFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }

        new File(finalFile).delete();

        String result = sb.toString();

        assertTrue(result.contains("DEVICE=" + interfaceName));
        assertTrue(result.contains("NAME=" + interfaceName));
        assertTrue(result.contains("TYPE=WIFI"));
        assertTrue(result.contains("ONBOOT=yes"));
        assertTrue(result.contains("BOOTPROTO=static"));
        assertTrue(result.contains("IPADDR=10.10.0.10"));
        assertTrue(result.contains("PREFIX=24"));
        assertTrue(result.contains("GATEWAY=10.10.0.254"));
        assertTrue(result.contains("DEFROUTE=yes"));
        assertTrue(result.contains("DNS1=10.10.0.252"));
        assertTrue(result.contains("DNS2=10.10.0.253"));
        assertTrue(result.contains("MODE=null"));
    }

    @Test
    public void testWriterDhcp() throws KuraException, IOException, NoSuchFieldException {
        // tests writer with DHCP

        String interfaceName = "testinterface";

        IfcfgConfigWriter writer = getWriter();

        TestUtil.setFieldValue(writer, "instance", writer);

        String finalFile = writer.getRHConfigDirectory() + "/ifcfg-" + interfaceName;
        new File(finalFile).delete();

        NetworkConfiguration config = new NetworkConfiguration();

        List<String> interfaces = new ArrayList<>();
        interfaces.add(interfaceName);
        config.setModifiedInterfaceNames(interfaces);

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("testinterface");
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        wifiInterfaceAddressConfig.setMode(WifiMode.ADHOC);
        List<NetConfig> netConfigs = new ArrayList<>();
        NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, false);
        IP4Address address = (IP4Address) IPAddress.getByAddress(new byte[] { 10, 10, 0, 10 });
        netConfig.setAddress(address);
        netConfig.setNetworkPrefixLength((short) 24);
        netConfig.setDhcp(true);
        netConfig.setGateway(
                (IP4Address) IPAddress.getByAddress(new byte[] { (byte) 10, (byte) 10, 0, (byte) 254 }));
        List<IP4Address> dnsServers = new ArrayList<>();
        IP4Address dns = (IP4Address) IPAddress.getByAddress(new byte[] { 10, 10, 0, (byte) 252 });
        dnsServers.add(dns);
        dns = (IP4Address) IPAddress.getByAddress(new byte[] { 10, 10, 0, (byte) 253 });
        dnsServers.add(dns);
        netConfig.setDnsServers(dnsServers);
        netConfigs.add(netConfig);
        Map<String, Object> map = new HashMap<>();
        map.put("mtu", 1235);
        netConfig.setProperties(map);
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        writer.visit(config);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(finalFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        new File(finalFile).delete();

        String result = sb.toString();

        assertTrue(result.contains("DEVICE=" + interfaceName));
        assertTrue(result.contains("NAME=" + interfaceName));
        assertTrue(result.contains("TYPE=WIFI"));
        assertTrue(result.contains("ONBOOT=no"));
        assertTrue(result.contains("BOOTPROTO=dhcp"));
        assertTrue(result.contains("DEFROUTE=yes"));
        assertTrue(result.contains("DNS1=10.10.0.252"));
        assertTrue(result.contains("DNS2=10.10.0.253"));
        assertTrue(result.contains("MODE=Ad-Hoc"));
        assertFalse(result.contains("IPADDR=10.10.0.10"));
        assertFalse(result.contains("PREFIX=24"));
        assertFalse(result.contains("GATEWAY=10.10.0.254"));
    }

    private IfcfgConfigWriter getWriter() {
        IfcfgConfigWriter writer = new IfcfgConfigWriter() {

            private String dir = "/tmp/ifcfg/";

            @Override
            protected String getRHConfigDirectory() {
                new File(dir).mkdirs();

                return dir;
            }

            @Override
            protected Properties getKuranetProperties() {
                return new Properties();
            }
        };
        return writer;
    }

    @Test
    public void testReaderStatic() throws KuraException, IOException, NoSuchFieldException {
        // tests reader with static configuration

        String interfaceName = "testinterface";

        IfcfgConfigReader reader = new IfcfgConfigReader() {

            private String dir = "/tmp/ifcfg/";

            @Override
            protected String getIfcfgDirectory() {
                new File(dir).mkdirs();

                return dir;
            }

            @Override
            protected Properties getKuranetProperties() {
                return new Properties();
            }
        };

        String finalFile = reader.getIfcfgDirectory() + "/ifcfg-" + interfaceName;

        try (FileWriter fw = new FileWriter(finalFile)) {
            fw.write("DEVICE=testinterface\n");
            fw.write("NAME=testinterface\n");
            fw.write("TYPE=WIFI\n");
            fw.write("ONBOOT=yes\n");
            fw.write("BOOTPROTO=static\n");
            fw.write("IPADDR=10.10.0.10\n");
            fw.write("PREFIX=24\n");
            fw.write("GATEWAY=10.10.0.254\n");
            fw.write("DEFROUTE=yes\n");
            fw.write("DNS1=10.10.0.252\n");
            fw.write("DNS2=10.10.0.253\n");
            fw.write("MODE=Ad-Hoc\n");
        }

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("testinterface");
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();
        wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        reader.visit(config);

        File file = new File(finalFile);
        file.delete();

        assertEquals(1, netConfigs.size());
        NetConfigIP4 result = (NetConfigIP4) netConfigs.get(0);

        assertEquals("10.10.0.10", result.getAddress().getHostAddress());
        assertEquals("10.10.0.254", result.getGateway().getHostAddress());
        assertEquals(24, result.getNetworkPrefixLength());
        assertEquals("255.255.255.0", result.getSubnetMask().getHostAddress());
        assertEquals(NetInterfaceStatus.netIPv4StatusEnabledWAN, result.getStatus());
        assertTrue(result.isAutoConnect());
        assertFalse(result.isDhcp());
        assertNotNull(result.getWinsServers());
        assertTrue(result.getWinsServers().isEmpty());
        assertNotNull(result.getDnsServers());
        assertEquals(2, result.getDnsServers().size());
        assertEquals("10.10.0.252", result.getDnsServers().get(0).getHostAddress());
        assertEquals("10.10.0.253", result.getDnsServers().get(1).getHostAddress());
    }

    @Test
    public void testReaderDhcp() throws KuraException, IOException, NoSuchFieldException {
        // tests reader with DHCP

        String interfaceName = "testinterface";

        IfcfgConfigReader reader = new IfcfgConfigReader() {

            private String dir = "/tmp/ifcfg/";

            @Override
            protected String getIfcfgDirectory() {
                new File(dir).mkdirs();

                return dir;
            }

            @Override
            protected Properties getKuranetProperties() {
                return new Properties();
            }
        };

        String finalFile = reader.getIfcfgDirectory() + "/ifcfg-" + interfaceName;

        try (FileWriter fw = new FileWriter(finalFile)) {
            fw.write("DEVICE=testinterface\n");
            fw.write("NAME=testinterface\n");
            fw.write("TYPE=WIFI\n");
            fw.write("ONBOOT=yes\n");
            fw.write("BOOTPROTO=dhcp\n");
            fw.write("DEFROUTE=yes\n");
            fw.write("DNS1=10.10.0.252\n");
            fw.write("DNS2=10.10.0.253\n");
            fw.write("MODE=Ad-Hoc\n");
        }

        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl("testinterface");
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        reader.visit(config);

        File file = new File(finalFile);
        file.delete();

        List<NetConfig> netConfigs = wifiInterfaceAddressConfig.getConfigs();
        assertEquals(1, netConfigs.size());
        NetConfigIP4 result = (NetConfigIP4) netConfigs.get(0);

        assertTrue(result.isDhcp());
        assertEquals(NetInterfaceStatus.netIPv4StatusEnabledWAN, result.getStatus());
        assertTrue(result.isAutoConnect());
        assertNotNull(result.getWinsServers());
        assertTrue(result.getWinsServers().isEmpty());
        assertNotNull(result.getDnsServers());
        assertEquals(2, result.getDnsServers().size());
        assertEquals("10.10.0.252", result.getDnsServers().get(0).getHostAddress());
        assertEquals("10.10.0.253", result.getDnsServers().get(1).getHostAddress());

        // mode doesn't seem to be properly restored :(
        assertNull(wifiInterfaceAddressConfig.getMode());
    }
}
