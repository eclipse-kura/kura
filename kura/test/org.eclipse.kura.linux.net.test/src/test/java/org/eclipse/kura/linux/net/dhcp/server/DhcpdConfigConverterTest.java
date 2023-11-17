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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DhcpdConfigConverterTest {

    private final String EXPECTED_STRING_BASIC_CONFIG = "# enabled? false\n"
            + "# prefix: 24\n"
            + "# pass DNS? false\n"
            + "\n"
            + "lease-file-name \"/var/lib/dhcp/dhcpd-eth0.leases\";\n"
            + "\n"
            + "subnet 172.16.0.0 netmask 255.255.255.0 {\n"
            + "    interface eth0;\n"
            + "    ddns-update-style none;\n"
            + "    ddns-updates off;\n"
            + "    default-lease-time 900;\n"
            + "    max-lease-time 900;\n"
            + "    pool {\n"
            + "        range 172.16.0.100 172.16.0.120;\n"
            + "    }\n"
            + "}\n";
    private final String EXPECTED_STRING_ENABLED_CONFIG = "# enabled? true\n"
            + "# prefix: 24\n"
            + "# pass DNS? false\n"
            + "\n"
            + "lease-file-name \"/var/lib/dhcp/dhcpd-eth0.leases\";\n"
            + "\n"
            + "subnet 172.16.0.0 netmask 255.255.255.0 {\n"
            + "    interface eth0;\n"
            + "    ddns-update-style none;\n"
            + "    ddns-updates off;\n"
            + "    default-lease-time 900;\n"
            + "    max-lease-time 900;\n"
            + "    pool {\n"
            + "        range 172.16.0.100 172.16.0.120;\n"
            + "    }\n"
            + "}\n";
    private final String EXPECTED_STRING_PASS_DNS_CONFIG = "# enabled? false\n"
            + "# prefix: 24\n"
            + "# pass DNS? true\n"
            + "\n"
            + "lease-file-name \"/var/lib/dhcp/dhcpd-eth0.leases\";\n"
            + "\n"
            + "subnet 172.16.0.0 netmask 255.255.255.0 {\n"
            + "    interface eth0;\n"
            + "    default-lease-time 900;\n"
            + "    max-lease-time 900;\n"
            + "    pool {\n"
            + "        range 172.16.0.100 172.16.0.120;\n"
            + "    }\n"
            + "}\n";
    private final String EXPECTED_STRING_ROUTER_CONFIG = "# enabled? false\n"
            + "# prefix: 24\n"
            + "# pass DNS? false\n"
            + "\n"
            + "lease-file-name \"/var/lib/dhcp/dhcpd-eth0.leases\";\n"
            + "\n"
            + "subnet 172.16.0.0 netmask 255.255.255.0 {\n"
            + "    interface eth0;\n"
            + "    option routers 192.168.2.1;\n"
            + "    ddns-update-style none;\n"
            + "    ddns-updates off;\n"
            + "    default-lease-time 900;\n"
            + "    max-lease-time 900;\n"
            + "    pool {\n"
            + "        range 172.16.0.100 172.16.0.120;\n"
            + "    }\n"
            + "}\n";
    private final String EXPECTED_STRING_DNS_ADDRESSES_CONFIG = "# enabled? false\n"
            + "# prefix: 24\n"
            + "# pass DNS? true\n"
            + "\n"
            + "lease-file-name \"/var/lib/dhcp/dhcpd-eth0.leases\";\n"
            + "\n"
            + "subnet 172.16.0.0 netmask 255.255.255.0 {\n"
            + "    option domain-name-servers 8.8.8.8,8.8.4.4;\n\n"
            + "    interface eth0;\n"
            + "    default-lease-time 900;\n"
            + "    max-lease-time 900;\n"
            + "    pool {\n"
            + "        range 172.16.0.100 172.16.0.120;\n"
            + "    }\n"
            + "}\n";
    private final String EXPECTED_STRING_FULL_CONFIG = "# enabled? true\n"
            + "# prefix: 24\n"
            + "# pass DNS? true\n"
            + "\n"
            + "lease-file-name \"/var/lib/dhcp/dhcpd-eth0.leases\";\n"
            + "\n"
            + "subnet 172.16.0.0 netmask 255.255.255.0 {\n"
            + "    option domain-name-servers 8.8.8.8,8.8.4.4;\n\n"
            + "    interface eth0;\n"
            + "    option routers 192.168.2.1;\n"
            + "    default-lease-time 900;\n"
            + "    max-lease-time 900;\n"
            + "    pool {\n"
            + "        range 172.16.0.100 172.16.0.120;\n"
            + "    }\n"
            + "}\n";
    private DhcpServerConfig config;
    private DhcpdConfigConverter converter;
    private String convertedConfig;

    @Test
    public void shouldReturnBasicConfig() throws UnknownHostException, KuraException {
        givenBasicDhcpServerConfig();
        givenDhcpdConfigConverter();

        whenDhcpdServerConfigIsConverted();

        thenDhcpdServerConfigIs(EXPECTED_STRING_BASIC_CONFIG);
    }

    @Test
    public void shouldReturnConfigWithEnabledInterface() throws UnknownHostException, KuraException {
        givenDhcpServerConfigWithEnabledInterface();
        givenDhcpdConfigConverter();

        whenDhcpdServerConfigIsConverted();

        thenDhcpdServerConfigIs(EXPECTED_STRING_ENABLED_CONFIG);
    }

    @Test
    public void shouldReturnConfigWithPassDns() throws UnknownHostException, KuraException {
        givenDhcpServerConfigWithPassDns();
        givenDhcpdConfigConverter();

        whenDhcpdServerConfigIsConverted();

        thenDhcpdServerConfigIs(EXPECTED_STRING_PASS_DNS_CONFIG);
    }

    @Test
    public void shouldReturnConfigWithRouterAddress() throws UnknownHostException, KuraException {
        givenDhcpServerConfigWithRouterAddress();
        givenDhcpdConfigConverter();

        whenDhcpdServerConfigIsConverted();

        thenDhcpdServerConfigIs(EXPECTED_STRING_ROUTER_CONFIG);
    }

    @Test
    public void shouldReturnConfigWithDnsAddresses() throws UnknownHostException, KuraException {
        givenDhcpServerConfigWithDnsAddresses();
        givenDhcpdConfigConverter();

        whenDhcpdServerConfigIsConverted();

        thenDhcpdServerConfigIs(EXPECTED_STRING_DNS_ADDRESSES_CONFIG);
    }

    @Test
    public void shouldReturnFullConfig() throws UnknownHostException, KuraException {
        givenDhcpServerFullConfig();
        givenDhcpdConfigConverter();

        whenDhcpdServerConfigIsConverted();

        thenDhcpdServerConfigIs(EXPECTED_STRING_FULL_CONFIG);
    }

    private void givenBasicDhcpServerConfig() throws UnknownHostException, KuraException {
        DhcpServerCfg dhcpServerConfig = new DhcpServerCfg("eth0", false, 900, 900, false);
        DhcpServerCfgIP4 dhcpServerConfigIP4 = new DhcpServerCfgIP4(
                (IP4Address) IPAddress.parseHostAddress("172.16.0.0"),
                (IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24, (IP4Address) null,
                (IP4Address) IPAddress.parseHostAddress("172.16.0.100"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.120"),
                (List<IP4Address>) null);

        this.config = new DhcpServerConfigIP4(dhcpServerConfig, dhcpServerConfigIP4);
    }

    private void givenDhcpServerConfigWithEnabledInterface() throws UnknownHostException, KuraException {
        DhcpServerCfg dhcpServerConfig = new DhcpServerCfg("eth0", true, 900, 900, false);
        DhcpServerCfgIP4 dhcpServerConfigIP4 = new DhcpServerCfgIP4(
                (IP4Address) IPAddress.parseHostAddress("172.16.0.0"),
                (IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24, (IP4Address) null,
                (IP4Address) IPAddress.parseHostAddress("172.16.0.100"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.120"),
                (List<IP4Address>) null);

        this.config = new DhcpServerConfigIP4(dhcpServerConfig, dhcpServerConfigIP4);
    }

    private void givenDhcpServerConfigWithPassDns() throws UnknownHostException, KuraException {
        DhcpServerCfg dhcpServerConfig = new DhcpServerCfg("eth0", false, 900, 900, true);
        DhcpServerCfgIP4 dhcpServerConfigIP4 = new DhcpServerCfgIP4(
                (IP4Address) IPAddress.parseHostAddress("172.16.0.0"),
                (IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24, (IP4Address) null,
                (IP4Address) IPAddress.parseHostAddress("172.16.0.100"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.120"),
                (List<IP4Address>) null);

        this.config = new DhcpServerConfigIP4(dhcpServerConfig, dhcpServerConfigIP4);
    }

    private void givenDhcpServerConfigWithRouterAddress() throws UnknownHostException, KuraException {
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

    private void givenDhcpServerConfigWithDnsAddresses() throws UnknownHostException, KuraException {
        DhcpServerCfg dhcpServerConfig = new DhcpServerCfg("eth0", false, 900, 900, true);
        List<IP4Address> dnsAddresses = new ArrayList<>();
        dnsAddresses.add((IP4Address) IPAddress.parseHostAddress("8.8.8.8"));
        dnsAddresses.add((IP4Address) IPAddress.parseHostAddress("8.8.4.4"));
        DhcpServerCfgIP4 dhcpServerConfigIP4 = new DhcpServerCfgIP4(
                (IP4Address) IPAddress.parseHostAddress("172.16.0.0"),
                (IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24, (IP4Address) null,
                (IP4Address) IPAddress.parseHostAddress("172.16.0.100"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.120"),
                dnsAddresses);

        this.config = new DhcpServerConfigIP4(dhcpServerConfig, dhcpServerConfigIP4);
    }

    private void givenDhcpServerFullConfig() throws UnknownHostException, KuraException {
        DhcpServerCfg dhcpServerConfig = new DhcpServerCfg("eth0", true, 900, 900, true);
        List<IP4Address> dnsAddresses = new ArrayList<>();
        dnsAddresses.add((IP4Address) IPAddress.parseHostAddress("8.8.8.8"));
        dnsAddresses.add((IP4Address) IPAddress.parseHostAddress("8.8.4.4"));
        DhcpServerCfgIP4 dhcpServerConfigIP4 = new DhcpServerCfgIP4(
                (IP4Address) IPAddress.parseHostAddress("172.16.0.0"),
                (IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24,
                (IP4Address) IPAddress.parseHostAddress("192.168.2.1"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.100"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.120"),
                dnsAddresses);

        this.config = new DhcpServerConfigIP4(dhcpServerConfig, dhcpServerConfigIP4);
    }

    private void givenDhcpdConfigConverter() {
        this.converter = new DhcpdConfigConverter();
    }

    private void whenDhcpdServerConfigIsConverted() {
        try (MockedStatic<DhcpServerManager> dhcpManagerMock = Mockito.mockStatic(DhcpServerManager.class)) {
            dhcpManagerMock.when(() -> {
                DhcpServerManager.getLeasesFilename("eth0");
            }).thenReturn("/var/lib/dhcp/dhcpd-eth0.leases");
            this.convertedConfig = this.converter.convert(this.config);
        }
    }

    private void thenDhcpdServerConfigIs(String expected) {
        assertEquals(expected, this.convertedConfig);
    }
}
