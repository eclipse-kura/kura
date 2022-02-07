/*******************************************************************************
 * Copyright (c) 2022 Sterwen Technology and others
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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.junit.Test;
import org.eclipse.kura.net.dhcp.DhcpLease;
import org.eclipse.kura.linux.net.util.DhcpLeaseTool;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.Signal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;


public class DhcpLeaseToolTest {
    protected static final CommandStatus successStatus = new CommandStatus(new Command(new String[] {}),
            new LinuxExitStatus(0));

    private String dhcpLeaseInfo;
    private List<DhcpLease> leases;

    private void givenDhcpLeaseInfo(String dhcpLeaseInfo) {
         this.dhcpLeaseInfo = dhcpLeaseInfo;
    }

    private void whenDhcpLeaseIsParsed() throws KuraException {
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(this.dhcpLeaseInfo);
        this.leases = DhcpLeaseTool.probeLeases(executorServiceStub);
        for (DhcpLease x : this.leases) {
            System.out.println(x.toString());
        }
    }

    private void thenDhcpLeasesAreDetected(int id, String mac, String ipAddress, String hostName) {
        assertEquals(leases.get(id).getMacAddress(), mac);
        assertEquals(leases.get(id).getIpAddress(), ipAddress);
        assertEquals(leases.get(id).getHostname(), hostName);
    }

    @Test
    public void checkDhcpLease() throws KuraException {
        givenDhcpLeaseInfo("MAC a8:6d:aa:0b:53:ff IP 172.16.1.100 HOSTNAME DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n");
        whenDhcpLeaseIsParsed();
        thenDhcpLeasesAreDetected(0, "a8:6d:aa:0b:53:ff", "172.16.1.100", "DESKTOP-4E3ACHI");
    }

	@Test
    public void checkMultipleLines() throws KuraException {
        givenDhcpLeaseInfo("MAC a8:6d:aa:0b:53:ff IP 172.16.1.100 HOSTNAME DESKTOP-4E3ACHI BEGIN 2021-12-17 18:19:13 END 2021-12-17 20:19:13 MANUFACTURER Intel Corporate\n"
               + "MAC bc:a8:a6:9a:0f:ff IP 172.16.1.101 HOSTNAME 20HEPF0UV1B5 BEGIN 2021-12-17 18:35:42 END 2021-12-17 20:35:42 MANUFACTURER Intel Corporate\n"
               + "MAC cd:fg:a7:54:1f:ff IP 172.16.1.102 HOSTNAME DELLGEEK BEGIN 2021-12-17 19:26:55 END 2021-12-17 21:25:22 MANUFACTURER Intel Corporate\n"
               + "MAC a9:bc:c8:88:bb:ff IP 172.16.1.103 HOSTNAME TECHTERMS BEGIN 2021-12-17 19:47:25 END 2021-12-17 21:35:43 MANUFACTURER Intel Corporate\n");
        whenDhcpLeaseIsParsed();
        thenDhcpLeasesAreDetected(0, "a8:6d:aa:0b:53:ff", "172.16.1.100", "DESKTOP-4E3ACHI");
        thenDhcpLeasesAreDetected(1, "bc:a8:a6:9a:0f:ff", "172.16.1.101", "20HEPF0UV1B5");
        thenDhcpLeasesAreDetected(2, "cd:fg:a7:54:1f:ff", "172.16.1.102", "DELLGEEK");
        thenDhcpLeasesAreDetected(3, "a9:bc:c8:88:bb:ff", "172.16.1.103", "TECHTERMS");
    }
}
