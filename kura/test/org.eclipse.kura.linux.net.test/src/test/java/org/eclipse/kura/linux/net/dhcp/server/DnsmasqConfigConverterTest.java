/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.dhcp.server;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.junit.Test;

public class DnsmasqConfigConverterTest {

    private final String EXPECTED_STRING_BASIC_CONFIG = "interface=eth0\n"
            + "dhcp-range=eth0,172.16.0.100,172.16.0.120,900s\n"
            + "dhcp-option=eth0,1,255.255.255.0\n"
            + "dhcp-option=eth0,3,192.168.2.1\n"
            + "dhcp-option=eth0,6\n"
            + "dhcp-ignore-names=eth0\n"
            + "dhcp-option=eth0,27,1\n";
    private final String EXPECTED_STRING_PASS_DNS_CONFIG = "interface=eth0\n"
            + "dhcp-range=eth0,172.16.0.100,172.16.0.120,900s\n"
            + "dhcp-option=eth0,1,255.255.255.0\n"
            + "dhcp-option=eth0,3,192.168.2.1\n"
            + "dhcp-option=eth0,6,0.0.0.0\n"
            + "dhcp-option=eth0,27,1\n";
    private DhcpServerConfig config;
    private DnsmasqConfigConverter converter;
    private String convertedConfig;

    @Test
    public void shouldReturnBasicConfig() throws UnknownHostException, KuraException {
        givenBasicDhcpServerConfig();
        givenDnsmasqConfigConverter();

        whenDnsmasqServerConfigIsConverted();

        thenDnsmasqServerConfigIs(EXPECTED_STRING_BASIC_CONFIG);
    }

    @Test
    public void shouldReturnConfigWithPassDns() throws UnknownHostException, KuraException {
        givenDhcpServerConfigWithPassDns();
        givenDnsmasqConfigConverter();

        whenDnsmasqServerConfigIsConverted();

        thenDnsmasqServerConfigIs(EXPECTED_STRING_PASS_DNS_CONFIG);
    }

    private void givenBasicDhcpServerConfig() throws UnknownHostException, KuraException {
        DhcpServerCfg dhcpServerConfig = new DhcpServerCfg("eth0", false, 900, 900, false);
        DhcpServerCfgIP4 dhcpServerConfigIP4 = new DhcpServerCfgIP4(
                (IP4Address) IPAddress.parseHostAddress("172.16.0.0"),
                (IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24,
                (IP4Address) IPAddress.parseHostAddress("192.168.2.1"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.100"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.120"),
                (List<IP4Address>) null);

        this.config = new DhcpServerConfigIP4(dhcpServerConfig, dhcpServerConfigIP4);
    }

    private void givenDhcpServerConfigWithPassDns() throws UnknownHostException, KuraException {
        DhcpServerCfg dhcpServerConfig = new DhcpServerCfg("eth0", false, 900, 900, true);
        DhcpServerCfgIP4 dhcpServerConfigIP4 = new DhcpServerCfgIP4(
                (IP4Address) IPAddress.parseHostAddress("172.16.0.0"),
                (IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24,
                (IP4Address) IPAddress.parseHostAddress("192.168.2.1"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.100"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.120"),
                (List<IP4Address>) null);

        this.config = new DhcpServerConfigIP4(dhcpServerConfig, dhcpServerConfigIP4);
    }

    private void givenDnsmasqConfigConverter() {
        this.converter = new DnsmasqConfigConverter();
    }

    private void whenDnsmasqServerConfigIsConverted() {
        this.convertedConfig = this.converter.convert(this.config);
    }

    private void thenDnsmasqServerConfigIs(String expected) {
        assertEquals(expected, this.convertedConfig);
    }
}
