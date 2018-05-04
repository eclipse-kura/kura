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
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests IfcfgConfigReader and IfcfgConfigWriter based on Debian-like system configuration.
 */
public class IfcfgConfigTest {

    @BeforeClass
    public static void setupClass() throws NoSuchFieldException {
        String os = "raspbian";

        TestUtil.setFieldValue(IfcfgConfigReader.getInstance(), "osVersion", os);

        TestUtil.setFieldValue(IfcfgConfigWriter.getInstance(), "osVersion", os);

    }

    @Test
    public void testWriter() throws KuraException, IOException, NoSuchFieldException {
        // tests writer and part of reader as existing configuration file is expected for comparison

        String interfaceName = "testinterface";
        String dir = "/tmp/ifcfg";
        new File(dir).mkdirs();

        IfcfgConfigWriter writer = new IfcfgConfigWriter() {

            @Override
            protected String getFinalFile() {
                return dir + "/ifcfg";
            }

            @Override
            protected String getTemporaryFile() {
                return dir + "/ifcfg-temp";
            }

            @Override
            protected Properties getKuranetProperties() {
                return new Properties();
            }
        };

        TestUtil.setFieldValue(writer, "instance", writer);

        String finalFile = writer.getFinalFile();

        try (FileWriter fw = new FileWriter(finalFile)) {
            fw.write("auto " + interfaceName + "\n");
            fw.write("iface " + interfaceName + " inet static\n");
            fw.write(" mtu 1234\n");
            fw.write(" address 10.10.0.5\n");
            fw.write(" netmask 255.255.255.0\n");
            fw.write(" gateway 10.10.0.250\n");
            fw.write(" #dns-nameservers 10.10.0.251 10.10.0.252\n");
            fw.write(" post-up\n");
            fw.write(" route del default dev " + interfaceName + "\n");
            fw.write("auto eth0\n"); // finish...
        }

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

        assertTrue(result.contains("auto " + interfaceName));
        assertTrue(result.contains("iface " + interfaceName + " inet static"));
        assertTrue(result.contains("address 10.10.0.10"));
        assertTrue(result.contains("netmask 255.255.255.0"));
        assertTrue(result.contains("gateway 10.10.0.254"));
        assertTrue(result.contains("#dns-nameservers 10.10.0.252 10.10.0.253"));
        assertTrue(result.contains("post-up"));
        assertFalse(result.contains("10.10.0.251"));
        assertFalse(result.contains("route"));
        assertFalse(result.contains("mtu "));
        assertFalse(result.contains("auto eth0"));
    }

    @Test
    public void testReader() throws KuraException, IOException, NoSuchFieldException {
        // tests reader on a static configuration sample

        String interfaceName = "testinterface";

        String finalFile = "/tmp/ifcfg/ifcfg";
        File file = new File(finalFile);
        file.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("#!kura!auto eth0\n");
            fw.write("iface eth0 inet static\n");
            fw.write(" mtu 1234\n");
            fw.write(" address 10.0.0.5\n");
            fw.write("auto " + interfaceName + "\n");
            fw.write("iface " + interfaceName + " inet static\n");
            fw.write(" mtu 1234\n");
            fw.write(" address 10.10.0.5\n");
            fw.write(" netmask 255.255.255.0\n");
            fw.write(" gateway 10.10.0.250\n");
            fw.write(" #dns-nameservers 10.10.0.251 10.10.0.252\n");
            fw.write(" post-up route del default dev " + interfaceName + "\n");
            fw.write("auto eth1\n");
        }
        
        IfcfgConfigReader ifcfgConfigReader = new IfcfgConfigReader();
        Properties result = ifcfgConfigReader.parseDebianConfigFile(file, interfaceName);
        file.delete();

        assertNotNull(result);
        assertEquals(9, result.size());

        assertEquals("static", result.getProperty("BOOTPROTO"));
        assertEquals("yes", result.getProperty("ONBOOT"));
        assertEquals("no", result.getProperty("DEFROUTE"));
        assertEquals("10.10.0.5", result.getProperty("IPADDR"));
        assertEquals("255.255.255.0", result.getProperty("NETMASK"));
        assertEquals("10.10.0.250", result.getProperty("GATEWAY"));
        assertEquals("10.10.0.251", result.getProperty("DNS1"));
        assertEquals("10.10.0.252", result.getProperty("DNS2"));
        assertEquals("1234", result.getProperty("mtu"));
    }

    @Test
    public void testReaderLoopback() throws KuraException, IOException, NoSuchFieldException {
        // tests reader on a basic loopback configuration

        String interfaceName = "lo";

        String finalFile = "/tmp/ifcfg/ifcfg";
        File file = new File(finalFile);
        file.getParentFile().mkdirs();

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("#!kura!auto eth0\n");
            fw.write("iface eth0 inet static\n");
            fw.write(" mtu 1234\n");
            fw.write(" address 10.0.0.5\n");
            fw.write("auto " + interfaceName + "\n");
            fw.write("iface " + interfaceName + " inet loopback\n");
            fw.write("auto eth1\n");
        }

        IfcfgConfigReader ifcfgConfigReader = new IfcfgConfigReader();
        Properties result = ifcfgConfigReader.parseDebianConfigFile(file, interfaceName);
        file.delete();

        assertNotNull(result);
        assertEquals(4, result.size());

        assertEquals("loopback", result.getProperty("BOOTPROTO"));
        assertEquals("yes", result.getProperty("ONBOOT"));
        assertEquals("127.0.0.1", result.getProperty("IPADDR"));
        assertEquals("255.0.0.0", result.getProperty("NETMASK"));
    }
}
