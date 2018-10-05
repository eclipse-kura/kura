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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.junit.Test;

public class PppConfigTest {

    public void testWriterVisit() {
        // PppConfigWriter writer = new PppConfigWriter();
    }

    @Test
    public void testReaderVisitNpPppdFileNoUmtsConfig() throws KuraException {
        // basic flow with no pppd peers file and no supported modem configured

        String intfName = "ppp2";
        String modemId = "pppid";

        PppConfigReader reader = new PppConfigReader() {

            @Override
            protected String getKuranetProperty(String key) {
                if (key.equals("net.interface." + intfName + ".modem.identifier")) {
                    return modemId;
                }

                return null;
            }

            @Override
            protected boolean hasAddress(int pppNumber) throws KuraException {
                return true;
            }

            @Override
            protected List<IPAddress> getPppDnServers() throws KuraException {
                return new ArrayList<>();
            }
        };

        NetworkConfiguration config = new NetworkConfiguration();

        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        ModemInterfaceAddressConfigImpl modemInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        interfaceAddressConfigs.add(modemInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        reader.visit(config);

        assertEquals(true, reader != null);
        assertTrue(modemInterfaceAddressConfig.getDnsServers().isEmpty());

        assertNotNull(modemInterfaceAddressConfig.getConfigs());
        assertEquals(2, modemInterfaceAddressConfig.getConfigs().size());

        assertTrue(modemInterfaceAddressConfig.getConfigs().get(0) instanceof ModemConfig);
        ModemConfig mc = (ModemConfig) modemInterfaceAddressConfig.getConfigs().get(0);
        assertEquals(2, mc.getPppNumber());
        assertEquals("inbound", mc.getActiveFilter());
        assertTrue(mc.isPersist());
        assertEquals(5, mc.getMaxFail());
        assertEquals(95, mc.getIdle());
        assertFalse(mc.isGpsEnabled());
        assertEquals(0, mc.getLcpEchoInterval());
        assertEquals(0, mc.getLcpEchoFailure());
        assertEquals("", mc.getApn());
        assertEquals(AuthType.NONE, mc.getAuthType());
        assertArrayEquals(new char[] {}, mc.getPasswordAsPassword().getPassword());
        assertEquals(PdpType.IP, mc.getPdpType());
        assertEquals("", mc.getUsername());

        assertTrue(modemInterfaceAddressConfig.getConfigs().get(1) instanceof NetConfigIP4);
        NetConfigIP4 nc = (NetConfigIP4) modemInterfaceAddressConfig.getConfigs().get(1);
        assertNotNull(nc.getDnsServers());
        assertTrue(nc.getDnsServers().isEmpty());
    }

    @Test
    public void testReaderVisitPppdFileWithUmtsConfig() throws KuraException, IOException {
        // basic flow with a pppd peer file, chat file and a supported USB modem configured

        String intfName = "testinterface";
        String modemId = "pppid";

        String dir = "/tmp/kurappp/";
        String peerFile = dir + intfName;
        new File(dir).mkdirs();
        try (FileWriter fw = new FileWriter(peerFile)) {
            fw.write("active-filter=\"filter\"\n");
            fw.write("persist=true\n");
            fw.write("maxfail=2\n");
            fw.write("idle=30\n");
            fw.write("lcp-echo-interval=10\n");
            fw.write("lcp-echo-failure=1\n");
            fw.write("unit=10\n");
            fw.write("user='a user'\n");
            fw.write("connect=connect -f " + dir + "chat\n");
        }
        
        try (FileWriter fw = new FileWriter(dir + "chat")) {    
            fw.write("expectedresult send\n");
            fw.write("dial a number\n");
            fw.write("CONNECT connect\n");
        }

        PppConfigReader reader = new PppConfigReader() {

            @Override
            protected String getKuranetProperty(String key) {
                if (key.equals("net.interface." + intfName + ".modem.identifier")) {
                    return modemId;
                } else if (key.equals("net.interface." + intfName + ".config.dnsServers")) {
                    return "10.10.0.250";
                } else if (key.equals("net.interface." + intfName + ".config.gpsEnabled")) {
                    return "true";
                } else if (key.equals("net.interface." + intfName + ".config.resetTimeout")) {
                    return "123";
                } else if (key.equals("net.interface." + intfName + ".config.apn")) {
                    return "apn";
                } else if (key.equals("net.interface." + intfName + ".config.pdpType")) {
                    return "PPP";
                }

                return null;
            }

            @Override
            protected boolean hasAddress(int pppNumber) throws KuraException {
                return true;
            }

            @Override
            protected List<IPAddress> getPppDnServers() throws KuraException {
                return new ArrayList<>();
            }

            @Override
            protected String getPeerFilename(String interfaceName, UsbDevice usbDevice) {
                return peerFile;
            }
        };

        NetworkConfiguration config = new NetworkConfiguration();

        // FIXME having files /etc/ppp/chap-secrets and /etc/ppp/pap-secrets present on the system might cause problems
        System.setProperty("target.device", "raspbian");
        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);
        UsbDevice usbDevice = new UsbModemDevice("1bc7", "0021", "ct", "", "2:1", "/dev/usb1");
        netInterfaceConfig.setUsbDevice(usbDevice);

        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        ModemInterfaceAddressConfigImpl modemInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        interfaceAddressConfigs.add(modemInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        reader.visit(config);

        assertEquals(true, reader != null);
        assertTrue(modemInterfaceAddressConfig.getDnsServers().isEmpty());

        assertNotNull(modemInterfaceAddressConfig.getConfigs());
        assertEquals(2, modemInterfaceAddressConfig.getConfigs().size());

        assertTrue(modemInterfaceAddressConfig.getConfigs().get(0) instanceof ModemConfig);
        ModemConfig mc = (ModemConfig) modemInterfaceAddressConfig.getConfigs().get(0);
        assertEquals(10, mc.getPppNumber());
        assertEquals("filter", mc.getActiveFilter());
        assertTrue(mc.isPersist());
        assertEquals(2, mc.getMaxFail());
        assertEquals(30, mc.getIdle());
        assertTrue(mc.isGpsEnabled());
        assertEquals(10, mc.getLcpEchoInterval());
        assertEquals(1, mc.getLcpEchoFailure());
        assertEquals("apn", mc.getApn());
        assertEquals(AuthType.NONE, mc.getAuthType());
        assertArrayEquals(new char[] {}, mc.getPasswordAsPassword().getPassword());
        assertEquals(PdpType.PPP, mc.getPdpType());
        assertEquals("a user", mc.getUsername());
        assertEquals("a number", mc.getDialString());
        assertEquals(123, mc.getResetTimeout());

        assertTrue(modemInterfaceAddressConfig.getConfigs().get(1) instanceof NetConfigIP4);
        NetConfigIP4 nc = (NetConfigIP4) modemInterfaceAddressConfig.getConfigs().get(1);
        assertNotNull(nc.getDnsServers());
        assertEquals(1, nc.getDnsServers().size());
        assertEquals("10.10.0.250", nc.getDnsServers().get(0).getHostAddress());
    }
}
