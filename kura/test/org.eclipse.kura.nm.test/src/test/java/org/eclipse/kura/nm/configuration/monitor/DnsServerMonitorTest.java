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
 ******************************************************************************/
package org.eclipse.kura.nm.configuration.monitor;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.internal.linux.net.dns.DnsServerService;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.dns.DnsServerConfigIP4;
import org.eclipse.kura.nm.NetworkProperties;
import org.junit.Test;

public class DnsServerMonitorTest {

    private DnsServerMonitor monitor;
    private CommandExecutorService commandExecutorServiceMock;
    private DnsServerService dnsServerServiceMock;

    private final Object lock = new Object();
    private NetworkProperties networkProperties;
    private String dnsServer1 = "8.8.8.8";
    private String dnsServer2 = "1.0.0.1";

    @Test
    public void shouldStartDnsServerMonitor() throws KuraException, InterruptedException, UnknownHostException {
        givenNetworkProperties();
        givenDnsServerMonitor();

        whenDnsServerMonitorIsStarted();

        thenDnsServerIsStarted();
    }

    @Test
    public void shouldStopDnsServerMonitor()
            throws KuraException, InterruptedException, UnknownHostException, NoSuchFieldException {
        givenNetworkProperties();
        givenDnsServerMonitor();

        whenDnsServerMonitorIsStopped();

        thenDnsMonitorIsStopped();
    }

    private void givenDnsServerMonitor() throws KuraException, UnknownHostException {
        this.commandExecutorServiceMock = mock(CommandExecutorService.class);
        this.dnsServerServiceMock = mock(DnsServerService.class);

        Set<IP4Address> dnsServers = new HashSet<>();

        dnsServers.add((IP4Address) IP4Address.parseHostAddress(this.dnsServer1));
        dnsServers.add((IP4Address) IP4Address.parseHostAddress(this.dnsServer2));

        when(this.dnsServerServiceMock.getConfig()).thenReturn(new DnsServerConfigIP4(dnsServers, null));

        this.monitor = new DnsServerMonitor(this.dnsServerServiceMock, this.commandExecutorServiceMock);

        this.monitor.setNetworkProperties(this.networkProperties);
    }

    private void givenNetworkProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put("net.interfaces", "wlan0,lo,eth0");
        properties.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "172.16.0.110");
        properties.put("net.interface.wlan0.config.wifi.master.mode", "MASTER");
        properties.put("net.interface.wlan0.config.dhcpServer4.rangeStart", "172.16.1.100");
        properties.put("net.interface.lo.config.ip4.status", "netIPv4StatusUnmanaged");
        properties.put("net.interface.eth0.config.dhcpServer4.prefix", "24");
        properties.put("net.interface.wlan0.config.wifi.master.radioMode", "RADIO_MODE_80211a");
        properties.put("net.interface.wlan0.config.ip4.address", "172.16.1.1");
        properties.put("net.interface.wlan0.config.wifi.mode", "MASTER");
        properties.put("net.interface.wlan0.config.dhcpServer4.prefix", "24");
        properties.put("net.interface.wlan0.config.wifi.master.securityType", "SECURITY_WPA2");
        properties.put("net.interface.eth0.config.dhcpClient4.enabled", true);
        properties.put("net.interface.wlan0.config.wifi.master.channel", "0");
        properties.put("net.interface.wlan0.config.wifi.master.ignoreSSID", false);
        properties.put("net.interface.wlan0.config.dhcpServer4.passDns", true);
        properties.put("net.interface.wlan0.config.wifi.master.ssid", "kura_gateway_test_dns");
        properties.put("net.interface.wlan0.type", "WIFI");
        properties.put("net.interface.wlan0.config.wifi.infra.mode", "INFRA");
        properties.put("net.interface.eth0.config.dhcpServer4.enabled", false);
        properties.put("net.interface.wlan0.config.ip4.dnsServers", "");
        properties.put("net.interface.wlan0.config.winsServers", "");
        properties.put("net.interface.eth0.config.ip4.dnsServers", String.join(",", this.dnsServer1, this.dnsServer2));
        properties.put("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", "7200");
        properties.put("net.interface.wlan0.config.wifi.master.driver", "nl80211");
        properties.put("net.interface.wlan0.config.wifi.infra.ssid", "");
        properties.put("net.interface.wlan0.config.dhcpServer4.defaultLeaseTime", "7200");
        properties.put("net.interface.wlan0.config.dhcpServer4.enabled", true);
        properties.put("net.interface.lo.type", "LOOPBACK");
        properties.put("net.interface.wlan0.config.dhcpServer4.maxLeaseTime", "7200");
        properties.put("net.interface.wlan0.config.dhcpServer4.rangeEnd", "172.16.1.110");
        properties.put("net.interface.wlan0.config.wifi.infra.passphrase", "wepKeyInfraWlan0");
        properties.put("net.interface.wlan0.config.ip4.prefix", "24");
        properties.put("net.interface.wlan0.config.dhcpClient4.enabled", false);
        properties.put("net.interface.eth0.type", "ETHERNET");
        properties.put("net.interface.eth0.config.dhcpServer4.rangeStart", "172.16.0.100");
        properties.put("net.interface.wlan0.config.nat.enabled", true);
        properties.put("net.interface.wlan0.config.wifi.master.groupCiphers", "CCMP");
        properties.put("net.interface.wlan0.config.wifi.master.passphrase", "testKEYS");
        properties.put("net.interface.wlan0.config.wifi.infra.securityType", "NONE");
        properties.put("net.interface.wlan0.config.ip4.gateway", "");
        properties.put("net.interface.eth0.config.dhcpServer4.maxLeaseTime", "7200");
        properties.put("net.interface.wlan0.config.wifi.master.pairwiseCiphers", "CCMP");
        properties.put("net.interface.wlan0.config.wifi.infra.driver", "nl80211");
        properties.put("net.interface.eth0.config.nat.enabled", false);
        properties.put("net.interface.eth0.config.dhcpServer4.passDns", false);
        properties.put("net.interface.wlan0.config.ip4.status", "netIPv4StatusEnabledLAN");
        properties.put("net.interface.wlan0.config.wifi.infra.broadcast", false);
        properties.put("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");

        this.networkProperties = new NetworkProperties(properties);
    }

    private void whenDnsServerMonitorIsStarted() {
        this.monitor.start();
    }

    private void whenDnsServerMonitorIsStopped() {
        this.monitor.stop();
    }

    private void thenDnsServerIsStarted() throws KuraException, InterruptedException {
        waitForSeconds(1);
        verify(this.dnsServerServiceMock, atLeast(1)).start();
    }

    private void thenDnsMonitorIsStopped() throws KuraException, InterruptedException, NoSuchFieldException {
        waitForSeconds(1);

        Future<?> monitorTask = (Future<?>) TestUtil.getFieldValue(this.monitor, "monitorTask");
        ScheduledExecutorService executor = (ScheduledExecutorService) TestUtil.getFieldValue(this.monitor, "executor");

        assertNull(monitorTask);
        assertNull(executor);
    }

    private void waitForSeconds(int timeout) throws InterruptedException {
        synchronized (this.lock) {
            this.lock.wait(timeout * 1000);
        }
    }
}
