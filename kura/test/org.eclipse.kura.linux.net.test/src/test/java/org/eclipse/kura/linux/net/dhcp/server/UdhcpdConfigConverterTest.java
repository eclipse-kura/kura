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

public class UdhcpdConfigConverterTest {

    private final String EXPECTED_STRING_BASIC_CONFIG = "start 172.16.0.100\n"
            + "end 172.16.0.120\n"
            + "interface eth0\n"
            + "pidfile /var/run/udhcpd-eth0.pid\n"
            + "lease_file /var/lib/dhcp/udhcpd-eth0.leases\n"
            + "max_leases 20\n"
            + "auto_time 30\n"
            + "decline_time 900\n"
            + "conflict_time 900\n"
            + "offer_time 900\n"
            + "min_lease 900\n"
            + "opt subnet 255.255.255.0\n"
            + "opt router 192.168.2.1\n"
            + "opt lease 900\n";
    private final String EXPECTED_STRING_DNS_ADDRESSES_CONFIG = "start 172.16.0.100\n"
            + "end 172.16.0.120\n"
            + "interface eth0\n"
            + "pidfile /var/run/udhcpd-eth0.pid\n"
            + "lease_file /var/lib/dhcp/udhcpd-eth0.leases\n"
            + "max_leases 20\n"
            + "auto_time 30\n"
            + "decline_time 900\n"
            + "conflict_time 900\n"
            + "offer_time 900\n"
            + "min_lease 900\n"
            + "opt subnet 255.255.255.0\n"
            + "opt router 192.168.2.1\n"
            + "opt lease 900\n"
            + "opt dns 8.8.8.8 8.8.4.4\n";
    private DhcpServerConfig config;
    private UdhcpdConfigConverter converter;
    private String convertedConfig;

    @Test
    public void shouldReturnBasicConfig() throws UnknownHostException, KuraException {
        givenBasicDhcpServerConfig();
        givenUdhcpdConfigConverter();

        whenUdhcpdServerConfigIsConverted();

        thenUdhcpdServerConfigIs(EXPECTED_STRING_BASIC_CONFIG);
    }

    @Test
    public void shouldReturnConfigWithDnsAddresses() throws UnknownHostException, KuraException {
        givenDhcpServerConfigWithDnsAddresses();
        givenUdhcpdConfigConverter();

        whenUdhcpdServerConfigIsConverted();

        thenUdhcpdServerConfigIs(EXPECTED_STRING_DNS_ADDRESSES_CONFIG);
    }

    private void givenBasicDhcpServerConfig() throws UnknownHostException, KuraException {
        DhcpServerCfg dhcpServerConfig = new DhcpServerCfg("eth0", true, 900, 900, true);
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
                (IP4Address) IPAddress.parseHostAddress("255.255.255.0"), (short) 24,
                (IP4Address) IPAddress.parseHostAddress("192.168.2.1"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.100"),
                (IP4Address) IPAddress.parseHostAddress("172.16.0.120"),
                dnsAddresses);

        this.config = new DhcpServerConfigIP4(dhcpServerConfig, dhcpServerConfigIP4);
    }

    private void givenUdhcpdConfigConverter() {
        this.converter = new UdhcpdConfigConverter();
    }

    private void whenUdhcpdServerConfigIsConverted() {
        try (MockedStatic<DhcpServerManager> dhcpManagerMock = Mockito.mockStatic(DhcpServerManager.class)) {
            dhcpManagerMock.when(() -> {
                DhcpServerManager.getLeasesFilename("eth0");
            }).thenReturn("/var/lib/dhcp/udhcpd-eth0.leases");
            dhcpManagerMock.when(() -> {
                DhcpServerManager.getPidFilename("eth0");
            }).thenReturn("/var/run/udhcpd-eth0.pid");
            this.convertedConfig = this.converter.convert(this.config);
        }
    }

    private void thenUdhcpdServerConfigIs(String expected) {
        assertEquals(expected, this.convertedConfig);
    }
}
