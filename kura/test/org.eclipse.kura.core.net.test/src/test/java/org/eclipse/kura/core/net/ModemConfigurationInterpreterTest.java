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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.junit.Test;

public class ModemConfigurationInterpreterTest {

    @Test
    public void testGetModemConfigNull() throws Throwable {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.ppp.config.apn", "apn");
        properties.put("net.interface.ppp.config.authType", null);
        properties.put("net.interface.ppp.config.dataCompression", null);
        properties.put("net.interface.ppp.config.dialString", "dialString");
        properties.put("net.interface.ppp.config.headerCompression", null);
        properties.put("net.interface.ppp.config.ipAddress", null);
        properties.put("net.interface.ppp.config.password", "password");
        properties.put("net.interface.ppp.config.pdpType", null);
        properties.put("net.interface.ppp.config.persist", null);
        properties.put("net.interface.ppp.config.maxFail", null);
        properties.put("net.interface.ppp.config.holdoff", null);
        properties.put("net.interface.ppp.config.idle", null);
        properties.put("net.interface.ppp.config.activeFilter", null);
        properties.put("net.interface.ppp.config.resetTimeout", null);
        properties.put("net.interface.ppp.config.lcpEchoInterval", null);
        properties.put("net.interface.ppp.config.lcpEchoFailure", null);
        properties.put("net.interface.ppp.config.profileId", null);
        properties.put("net.interface.ppp.config.username", "username");
        properties.put("net.interface.ppp.config.enabled", null);
        properties.put("net.interface.ppp.config.gpsEnabled", null);
        properties.put("net.interface.ppp.config.pppNum", null);

        ModemConfig expected = new ModemConfig();
        expected.setApn("apn");
        expected.setAuthType(AuthType.NONE);
        expected.setDialString("dialString");
        expected.setIpAddress(null);
        expected.setPassword("password");
        expected.setPdpType(PdpType.IP);
        expected.setUsername("username");
        expected.setEnabled(false);
        expected.setGpsEnabled(false);
        expected.setPersist(true);
        expected.setMaxFail(5);
        expected.setHoldoff(1);
        expected.setIdle(95);
        expected.setActiveFilter("inbound");
        expected.setResetTimeout(5);
        expected.setLcpEchoFailure(0);
        expected.setLcpEchoInterval(0);
        expected.setPppNumber(0);

        ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = new ModemInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = ModemConfigurationInterpreter.populateConfiguration(modemInterfaceAddressImpl,
                properties, "ppp", 0);

        assertNotNull(netConfigs);
        assertEquals(1, netConfigs.size());

        ModemConfig modemConfig = (ModemConfig) netConfigs.get(0);

        assertEquals(expected, modemConfig);
    }

    @Test
    public void testGetModemConfigEmpty() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.ppp.config.apn", "apn");
        properties.put("net.interface.ppp.config.authType", "");
        properties.put("net.interface.ppp.config.dataCompression", 1);
        properties.put("net.interface.ppp.config.dialString", "dialString");
        properties.put("net.interface.ppp.config.headerCompression", 2);
        properties.put("net.interface.ppp.config.ipAddress", "");
        properties.put("net.interface.ppp.config.password", "password");
        properties.put("net.interface.ppp.config.pdpType", "");
        properties.put("net.interface.ppp.config.persist", true);
        properties.put("net.interface.ppp.config.maxFail", 5);
        properties.put("net.interface.ppp.config.holdoff", 1);
        properties.put("net.interface.ppp.config.idle", 6);
        properties.put("net.interface.ppp.config.activeFilter", "activeFilter");
        properties.put("net.interface.ppp.config.resetTimeout", 7);
        properties.put("net.interface.ppp.config.lcpEchoInterval", 8);
        properties.put("net.interface.ppp.config.lcpEchoFailure", 9);
        properties.put("net.interface.ppp.config.profileId", 10);
        properties.put("net.interface.ppp.config.username", "username");
        properties.put("net.interface.ppp.config.enabled", true);
        properties.put("net.interface.ppp.config.gpsEnabled", true);
        properties.put("net.interface.ppp.config.pppNum", 0);

        ModemConfig expected = new ModemConfig();
        expected.setApn("apn");
        expected.setAuthType(AuthType.NONE);
        expected.setDataCompression(1);
        expected.setDialString("dialString");
        expected.setHeaderCompression(2);
        expected.setIpAddress(null);
        expected.setPassword("password");
        expected.setPdpType(PdpType.IP);
        expected.setPersist(true);
        expected.setMaxFail(5);
        expected.setHoldoff(1);
        expected.setIdle(6);
        expected.setActiveFilter("activeFilter");
        expected.setResetTimeout(7);
        expected.setLcpEchoInterval(8);
        expected.setLcpEchoFailure(9);
        expected.setProfileID(10);
        expected.setUsername("username");
        expected.setEnabled(true);
        expected.setGpsEnabled(true);
        expected.setPppNumber(0);

        ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = new ModemInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = ModemConfigurationInterpreter.populateConfiguration(modemInterfaceAddressImpl,
                properties, "ppp", 0);

        assertNotNull(netConfigs);
        assertEquals(1, netConfigs.size());

        ModemConfig modemConfig = (ModemConfig) netConfigs.get(0);

        assertEquals(expected, modemConfig);
    }

    @Test
    public void testGetModemConfigAll() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.ppp.config.apn", "apn");
        properties.put("net.interface.ppp.config.authType", "AUTO");
        properties.put("net.interface.ppp.config.dataCompression", 1);
        properties.put("net.interface.ppp.config.dialString", "dialString");
        properties.put("net.interface.ppp.config.headerCompression", 2);
        properties.put("net.interface.ppp.config.ipAddress", "10.0.0.1");
        properties.put("net.interface.ppp.config.password", "password");
        properties.put("net.interface.ppp.config.pdpType", "IP");
        properties.put("net.interface.ppp.config.persist", true);
        properties.put("net.interface.ppp.config.maxFail", 5);
        properties.put("net.interface.ppp.config.holdoff", 1);
        properties.put("net.interface.ppp.config.idle", 6);
        properties.put("net.interface.ppp.config.activeFilter", "activeFilter");
        properties.put("net.interface.ppp.config.resetTimeout", 7);
        properties.put("net.interface.ppp.config.lcpEchoInterval", 8);
        properties.put("net.interface.ppp.config.lcpEchoFailure", 9);
        properties.put("net.interface.ppp.config.profileId", 10);
        properties.put("net.interface.ppp.config.username", "username");
        properties.put("net.interface.ppp.config.enabled", true);
        properties.put("net.interface.ppp.config.gpsEnabled", true);
        properties.put("net.interface.ppp.config.pppNum", 0);

        ModemConfig expected = new ModemConfig();
        expected.setApn("apn");
        expected.setAuthType(AuthType.AUTO);
        expected.setDataCompression(1);
        expected.setDialString("dialString");
        expected.setHeaderCompression(2);
        expected.setIpAddress(IPAddress.parseHostAddress("10.0.0.1"));
        expected.setPassword("password");
        expected.setPdpType(PdpType.IP);
        expected.setPersist(true);
        expected.setMaxFail(5);
        expected.setHoldoff(1);
        expected.setIdle(6);
        expected.setActiveFilter("activeFilter");
        expected.setResetTimeout(7);
        expected.setLcpEchoInterval(8);
        expected.setLcpEchoFailure(9);
        expected.setProfileID(10);
        expected.setUsername("username");
        expected.setEnabled(true);
        expected.setGpsEnabled(true);
        expected.setPppNumber(1);

        ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = new ModemInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = ModemConfigurationInterpreter.populateConfiguration(modemInterfaceAddressImpl,
                properties, "ppp", 1);

        assertNotNull(netConfigs);
        assertEquals(1, netConfigs.size());

        ModemConfig modemConfig = (ModemConfig) netConfigs.get(0);

        assertEquals(expected, modemConfig);
    }
    
    @Test
    public void testGetModemConfigAllOverload() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.ppp.config.apn", "apn");
        properties.put("net.interface.ppp.config.authType", "AUTO");
        properties.put("net.interface.ppp.config.dataCompression", 1);
        properties.put("net.interface.ppp.config.dialString", "dialString");
        properties.put("net.interface.ppp.config.headerCompression", 2);
        properties.put("net.interface.ppp.config.ipAddress", "10.0.0.1");
        properties.put("net.interface.ppp.config.password", "password");
        properties.put("net.interface.ppp.config.pdpType", "IP");
        properties.put("net.interface.ppp.config.persist", true);
        properties.put("net.interface.ppp.config.maxFail", 5);
        properties.put("net.interface.ppp.config.holdoff", 1);
        properties.put("net.interface.ppp.config.idle", 6);
        properties.put("net.interface.ppp.config.activeFilter", "activeFilter");
        properties.put("net.interface.ppp.config.resetTimeout", 7);
        properties.put("net.interface.ppp.config.lcpEchoInterval", 8);
        properties.put("net.interface.ppp.config.lcpEchoFailure", 9);
        properties.put("net.interface.ppp.config.profileId", 10);
        properties.put("net.interface.ppp.config.username", "username");
        properties.put("net.interface.ppp.config.enabled", true);
        properties.put("net.interface.ppp.config.gpsEnabled", true);
        properties.put("net.interface.ppp.config.pppNum", 2);

        ModemConfig expected = new ModemConfig();
        expected.setApn("apn");
        expected.setAuthType(AuthType.AUTO);
        expected.setDataCompression(1);
        expected.setDialString("dialString");
        expected.setHeaderCompression(2);
        expected.setIpAddress(IPAddress.parseHostAddress("10.0.0.1"));
        expected.setPassword("password");
        expected.setPdpType(PdpType.IP);
        expected.setPersist(true);
        expected.setMaxFail(5);
        expected.setHoldoff(1);
        expected.setIdle(6);
        expected.setActiveFilter("activeFilter");
        expected.setResetTimeout(7);
        expected.setLcpEchoInterval(8);
        expected.setLcpEchoFailure(9);
        expected.setProfileID(10);
        expected.setUsername("username");
        expected.setEnabled(true);
        expected.setGpsEnabled(true);
        expected.setPppNumber(2);

        ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = new ModemInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = ModemConfigurationInterpreter.populateConfiguration(modemInterfaceAddressImpl,
                properties, "ppp");

        assertNotNull(netConfigs);
        assertEquals(1, netConfigs.size());

        ModemConfig modemConfig = (ModemConfig) netConfigs.get(0);

        assertEquals(expected, modemConfig);
    }

    @Test(expected = KuraException.class)
    public void testGetModemConfigInvalidAuthType() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.ppp.config.authType", "xyz");

        ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = new ModemInterfaceAddressConfigImpl();

        ModemConfigurationInterpreter.populateConfiguration(modemInterfaceAddressImpl, properties, "ppp", 0);
    }

    @Test(expected = KuraException.class)
    public void testGetModemConfigInvalidIPAddress() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.ppp.config.ipAddress", "1.2.3.4.5");

        ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = new ModemInterfaceAddressConfigImpl();

        ModemConfigurationInterpreter.populateConfiguration(modemInterfaceAddressImpl, properties, "ppp", 0);
    }

    @Test
    public void testGetModemConfigInvalidPdpType() throws Throwable {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("net.interface.ppp.config.pdpType", "xyz");

        ModemInterfaceAddressConfigImpl modemInterfaceAddressImpl = new ModemInterfaceAddressConfigImpl();

        List<NetConfig> netConfigs = ModemConfigurationInterpreter.populateConfiguration(modemInterfaceAddressImpl,
                properties, "ppp", 0);

        assertNotNull(netConfigs);
        assertEquals(1, netConfigs.size());

        ModemConfig modemConfig = (ModemConfig) netConfigs.get(0);

        assertEquals(PdpType.IP, modemConfig.getPdpType());
    }

}
