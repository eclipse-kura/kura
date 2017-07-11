/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.net.dhcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IP4Address;
import org.junit.Test;


public class DhcpServerConfigIP4Test {

    @Test
    public void testValidityChecks() throws UnknownHostException, KuraException {
        String interfaceName = "eth0";
        boolean enabled = true;
        int defaultLeaseTime = 1200;
        int maximumLeaseTime = 1200;
        boolean passDns = true;
        DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(interfaceName, enabled, defaultLeaseTime, maximumLeaseTime,
                passDns);

        IP4Address subnet = (IP4Address) IP4Address.parseHostAddress("172.16.4.0");
        IP4Address subnetMask = (IP4Address) IP4Address.parseHostAddress("255.255.255.0");
        short prefix = 24;
        IP4Address routerAddress = (IP4Address) IP4Address.parseHostAddress("172.16.4.1");
        IP4Address rangeStart = (IP4Address) IP4Address.parseHostAddress("172.16.4.100");
        IP4Address rangeEnd = (IP4Address) IP4Address.parseHostAddress("172.16.4.110");
        List<IP4Address> dnsServers = new ArrayList<>();
        DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix, routerAddress, rangeStart,
                rangeEnd, dnsServers);

        DhcpServerConfigIP4 configIP4 = new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);

        assertTrue(configIP4.isValid());
    }

    @Test
    public void testValidityChecksInvalidLeaseTime() throws UnknownHostException, KuraException {
        String interfaceName = "eth0";
        boolean enabled = true;
        int defaultLeaseTime = 0; // error
        int maximumLeaseTime = 1200;
        boolean passDns = true;
        DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(interfaceName, enabled, defaultLeaseTime, maximumLeaseTime,
                passDns);

        IP4Address subnet = (IP4Address) IP4Address.parseHostAddress("172.16.4.0");
        IP4Address subnetMask = (IP4Address) IP4Address.parseHostAddress("255.255.255.0");
        short prefix = 24;
        IP4Address routerAddress = (IP4Address) IP4Address.parseHostAddress("172.16.4.1");
        IP4Address rangeStart = (IP4Address) IP4Address.parseHostAddress("172.16.4.100");
        IP4Address rangeEnd = (IP4Address) IP4Address.parseHostAddress("172.16.4.110");
        List<IP4Address> dnsServers = new ArrayList<>();
        DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix, routerAddress, rangeStart,
                rangeEnd, dnsServers);

        DhcpServerConfigIP4 configIP4;
        try {
            configIP4 = new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);
            fail("Exception expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_ERROR, e.getCode());
        }

    }

    @Test
    public void testValidityChecksInvalidRange() throws UnknownHostException, KuraException {
        String interfaceName = "eth0";
        boolean enabled = true;
        int defaultLeaseTime = 1200;
        int maximumLeaseTime = 1200;
        boolean passDns = true;
        DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(interfaceName, enabled, defaultLeaseTime, maximumLeaseTime,
                passDns);

        IP4Address subnet = (IP4Address) IP4Address.parseHostAddress("172.16.4.0");
        IP4Address subnetMask = (IP4Address) IP4Address.parseHostAddress("255.255.255.0");
        short prefix = 24;
        IP4Address routerAddress = (IP4Address) IP4Address.parseHostAddress("172.16.4.1");
        IP4Address rangeStart = (IP4Address) IP4Address.parseHostAddress("172.16.4.100");
        IP4Address rangeEnd = null; // error
        List<IP4Address> dnsServers = new ArrayList<>();
        DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix, routerAddress, rangeStart,
                rangeEnd, dnsServers);

        DhcpServerConfigIP4 configIP4;
        try {
            configIP4 = new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);
            fail("Exception expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_ERROR, e.getCode());
        }

    }

    @Test
    public void testValidityChecksInvalidRange2() throws UnknownHostException, KuraException {
        String interfaceName = "eth0";
        boolean enabled = true;
        int defaultLeaseTime = 1200;
        int maximumLeaseTime = 1200;
        boolean passDns = true;
        DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(interfaceName, enabled, defaultLeaseTime, maximumLeaseTime,
                passDns);

        IP4Address subnet = (IP4Address) IP4Address.parseHostAddress("172.16.4.0");
        IP4Address subnetMask = (IP4Address) IP4Address.parseHostAddress("255.255.255.0");
        short prefix = 24;
        IP4Address routerAddress = (IP4Address) IP4Address.parseHostAddress("172.16.4.1");
        IP4Address rangeStart = (IP4Address) IP4Address.parseHostAddress("172.16.4.100");
        IP4Address rangeEnd = (IP4Address) IP4Address.parseHostAddress("172.16.5.110"); // error
        List<IP4Address> dnsServers = new ArrayList<>();
        DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix, routerAddress, rangeStart,
                rangeEnd, dnsServers);

        DhcpServerConfigIP4 configIP4;
        try {
            configIP4 = new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);
            fail("Exception expected.");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.CONFIGURATION_ERROR, e.getCode());
        }

    }

}
