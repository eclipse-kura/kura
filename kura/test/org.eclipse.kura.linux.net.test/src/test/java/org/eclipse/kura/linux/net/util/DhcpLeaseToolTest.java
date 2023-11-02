/*******************************************************************************
 * Copyright (c) 2022, 2023 Sterwen Technology and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.eclipse.kura.linux.net.dhcp.server.DhcpdLeaseReader;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqLeaseReader;
import org.eclipse.kura.linux.net.dhcp.server.UdhcpdLeaseReader;
import org.eclipse.kura.net.dhcp.DhcpLease;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DhcpLeaseToolTest {
    private static final String INTERFACE_NAME = "eth0";
    private static final String DNSMASQ_FILENAME = "/tmp/dnsmasq.leases";
    protected static final CommandStatus successStatus = new CommandStatus(new Command(new String[] {}),
            new LinuxExitStatus(0));

    private String dhcpLeaseInfo;
    private List<DhcpLease> leases;
    private CommandExecutorServiceStub executorServiceStub;

    @Test
    public void shouldReturnDhcpdLease() throws KuraException {
        givenDhcpLeaseInfo(
                "MAC a8:6d:aa:0b:53:ff IP 172.16.1.100 HOSTNAME DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n");
        givenCommandExecutorServiceStub();
        whenDhcpLeaseIsParsed(DhcpServerTool.DHCPD);
        thenDhcpLeasesAreDetected(0, "a8:6d:aa:0b:53:ff", "172.16.1.100", "DESKTOP-4E3ACHI");
    }

    @Test
    public void shouldReturnDhcpdLeases() throws KuraException {
        givenDhcpLeaseInfo(
                "MAC a8:6d:aa:0b:53:ff IP 172.16.1.100 HOSTNAME DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n"
                        + "MAC bc:a8:a6:9a:0f:ff IP 172.16.1.101 HOSTNAME 20HEPF0UV1B5 BEGIN 2021-12-17 18:35:42 END 2021-12-17 20:35:42 MANUFACTURER Intel Corporate\n"
                        + "MAC cd:fg:a7:54:1f:ff IP 172.16.1.102 HOSTNAME DELLGEEK BEGIN 2021-12-17 19:26:55 END 2021-12-17 21:25:22 MANUFACTURER Intel Corporate\n"
                        + "MAC a9:bc:c8:88:bb:ff IP 172.16.1.103 HOSTNAME TECHTERMS BEGIN 2021-12-17 19:47:25 END 2021-12-17 21:35:43 MANUFACTURER Intel Corporate\n");
        givenCommandExecutorServiceStub();
        whenDhcpLeaseIsParsed(DhcpServerTool.DHCPD);
        thenDhcpLeasesAreDetected(0, "a8:6d:aa:0b:53:ff", "172.16.1.100", "DESKTOP-4E3ACHI");
        thenDhcpLeasesAreDetected(1, "bc:a8:a6:9a:0f:ff", "172.16.1.101", "20HEPF0UV1B5");
        thenDhcpLeasesAreDetected(2, "cd:fg:a7:54:1f:ff", "172.16.1.102", "DELLGEEK");
        thenDhcpLeasesAreDetected(3, "a9:bc:c8:88:bb:ff", "172.16.1.103", "TECHTERMS");
    }

    @Test
    public void shouldReturnNothingWhenDhpcdLeaseIsMalformed() throws KuraException {
        givenDhcpLeaseInfo(
                "MACX a8:6d:aa:0b:53:ff IP 172.16.1.100 HOSTNAME DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n"
                        + "MACX a8:6d:aa:0b:53:ff IP 172.16.1.100 HOSTNAME DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n"
                        + "MAC a8:6d:aa:0b:53:ff IPY 172.16.1.100 HOSTNAME DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n"
                        + "MAC a8:6d:aa:0b:53:ff IP 172.16.1.100 HOSTNAMET DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n"
                        + "MAC a8:6d:aa:0b:53:ff IP 172.16.1.100"
                        + "IP 172.16.1.100 HOSTNAME DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n"
                        + "MAC a8:6d:aa:0b:53:ff HOSTNAME DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n"
                        + "MAC a8:6d:aa:0b:53:ff IP 172.16.1.100 BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n");
        givenCommandExecutorServiceStub();
        whenDhcpLeaseIsParsed(DhcpServerTool.DHCPD);
        thenDhcpLeasesIsEmpty();
    }

    @Test
    public void shouldReturnUdhcpdLease() throws KuraException {
        givenDhcpLeaseInfo("Mac Address       IP Address      Host Name           Expires in\n" +
                "a8:6d:aa:0b:53:ff 172.16.1.100    DESKTOP-4E3ACHI      01:28:03");
        givenCommandExecutorServiceStub();
        whenDhcpLeaseIsParsed(DhcpServerTool.UDHCPD);
        thenDhcpLeasesAreDetected(0, "a8:6d:aa:0b:53:ff", "172.16.1.100", "DESKTOP-4E3ACHI");
    }

    @Test
    public void shouldReturnUdhcpdLeases() throws KuraException {
        givenDhcpLeaseInfo(
                "a8:6d:aa:0b:53:ff       172.16.1.100       DESKTOP-4E3ACHI       01:23:23\n"
                        + "bc:a8:a6:9a:0f:ff       172.16.1.101       20HEPF0UV1B5       01:23:23\n"
                        + "cd:fg:a7:54:1f:ff       172.16.1.102       DELLGEEK       01:23:23\n"
                        + "a9:bc:c8:88:bb:ff       172.16.1.103       TECHTERMS       01:23:23\n");
        givenCommandExecutorServiceStub();
        whenDhcpLeaseIsParsed(DhcpServerTool.UDHCPD);
        thenDhcpLeasesAreDetected(0, "a8:6d:aa:0b:53:ff", "172.16.1.100", "DESKTOP-4E3ACHI");
        thenDhcpLeasesAreDetected(1, "bc:a8:a6:9a:0f:ff", "172.16.1.101", "20HEPF0UV1B5");
        thenDhcpLeasesAreDetected(2, "cd:fg:a7:54:1f:ff", "172.16.1.102", "DELLGEEK");
        thenDhcpLeasesAreDetected(3, "a9:bc:c8:88:bb:ff", "172.16.1.103", "TECHTERMS");
    }

    @Test
    public void shouldReturnDnsmasqLease() throws KuraException, IOException {
        givenDhcpLeaseInfo("1698418392 a8:6d:aa:0b:53:ff 172.16.1.100 DESKTOP-4E3ACHI a8:6d:aa:0b:53:ff");
        givenDnsmasqLeaseFile();
        whenDhcpLeaseIsParsed(DhcpServerTool.DNSMASQ);
        thenDhcpLeasesAreDetected(0, "a8:6d:aa:0b:53:ff", "172.16.1.100", "DESKTOP-4E3ACHI");
    }

    @Test
    public void shouldReturnDnsmasqLeases() throws KuraException, IOException {
        givenDhcpLeaseInfo("1698418392 a8:6d:aa:0b:53:ff 172.16.1.100 DESKTOP-4E3ACHI a8:6d:aa:0b:53:ff\n"
                + "1698418392 bc:a8:a6:9a:0f:ff 172.16.1.101 20HEPF0UV1B5 bc:a8:a6:9a:0f:ff\n"
                + "1698418392 cd:fg:a7:54:1f:ff 172.16.1.102 DELLGEEK cd:fg:a7:54:1f:ff\n"
                + "1698418392 a9:bc:c8:88:bb:ff 172.16.1.103 TECHTERMS a9:bc:c8:88:bb:ff\n");
        givenDnsmasqLeaseFile();
        whenDhcpLeaseIsParsed(DhcpServerTool.DNSMASQ);
        thenDhcpLeasesAreDetected(0, "a8:6d:aa:0b:53:ff", "172.16.1.100", "DESKTOP-4E3ACHI");
        thenDhcpLeasesAreDetected(1, "bc:a8:a6:9a:0f:ff", "172.16.1.101", "20HEPF0UV1B5");
        thenDhcpLeasesAreDetected(2, "cd:fg:a7:54:1f:ff", "172.16.1.102", "DELLGEEK");
        thenDhcpLeasesAreDetected(3, "a9:bc:c8:88:bb:ff", "172.16.1.103", "TECHTERMS");
    }

    @Test
    public void shouldReturnEmptyList() throws KuraException {
        givenCommandExecutorServiceStub();
        whenDhcpLeaseIsParsed(DhcpServerTool.NONE);
        thenDhcpLeasesIsEmpty();
    }

    private void givenDhcpLeaseInfo(String dhcpLeaseInfo) {
        this.dhcpLeaseInfo = dhcpLeaseInfo;
    }

    private void givenCommandExecutorServiceStub() {
        this.executorServiceStub = new CommandExecutorServiceStub(successStatus);
        this.executorServiceStub.writeOutput(this.dhcpLeaseInfo);
    }

    private void givenDnsmasqLeaseFile() throws IOException {
        Path dnsmasqFilePath = Paths.get(DNSMASQ_FILENAME);
        Files.write(dnsmasqFilePath, this.dhcpLeaseInfo.getBytes(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void whenDhcpLeaseIsParsed(DhcpServerTool tool) throws KuraException {
        try (MockedStatic<DhcpServerManager> dhcpManagerMock = Mockito.mockStatic(DhcpServerManager.class)) {
            if (tool == DhcpServerTool.DHCPD) {
                dhcpManagerMock.when(DhcpServerManager::getLeaseReader).thenReturn(Optional.of(new DhcpdLeaseReader()));
            } else if (tool == DhcpServerTool.UDHCPD) {
                dhcpManagerMock.when(DhcpServerManager::getLeaseReader)
                        .thenReturn(Optional.of(new UdhcpdLeaseReader()));
            } else if (tool == DhcpServerTool.DNSMASQ) {
                dhcpManagerMock.when(DhcpServerManager::getLeaseReader)
                        .thenReturn(Optional.of(new DnsmasqLeaseReader()));
                dhcpManagerMock.when(() -> DhcpServerManager.getLeasesFilename(INTERFACE_NAME))
                        .thenReturn(DNSMASQ_FILENAME);
            } else {
                dhcpManagerMock.when(DhcpServerManager::getLeaseReader).thenReturn(Optional.empty());
            }
            this.leases = DhcpLeaseTool.probeLeases(INTERFACE_NAME, this.executorServiceStub);
            for (DhcpLease x : this.leases) {
                System.out.println(x.toString());
            }
        }
    }

    private void thenDhcpLeasesAreDetected(int id, String mac, String ipAddress, String hostName) {
        assertEquals(this.leases.get(id).getMacAddress(), mac);
        assertEquals(this.leases.get(id).getIpAddress(), ipAddress);
        assertEquals(this.leases.get(id).getHostname(), hostName);
    }

    private void thenDhcpLeasesIsEmpty() {
        assertTrue(this.leases.isEmpty());
    }

    @After
    public void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(DNSMASQ_FILENAME));
    }
}
