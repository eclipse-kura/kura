/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.junit.Test;

public class IpAddrShowTest {

    private static final String LOOPBACK_LINK = "1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN "
            + "mode DEFAULT group default qlen 1000\\    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00\n";
    private static final String LOOPBACK_INET = "1: lo    inet 127.0.0.1/8 scope host lo\\       "
            + "valid_lft forever preferred_lft forever\n";

    private String linkOutput;
    private String inetOutput;
    private LinuxIfconfig[] configs;

    @Test
    public void shouldRemoveTheParentLinkOfAContainerInterface() throws KuraException {
        givenLinkOutput(LOOPBACK_LINK + "2: eth0@if65: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue "
                + "state UP mode DEFAULT group default \\    link/ether 02:42:ac:11:00:02 brd ff:ff:ff:ff:ff:ff "
                + "link-netnsid 0\n");
        givenInetOutput(LOOPBACK_INET + "2: eth0    inet 172.17.0.2/16 brd 172.17.255.255 scope global eth0\\"
                + "       valid_lft forever preferred_lft forever\n");

        whenIpAddrShowIsExecuted();

        thenInterfaceNamesAre("lo", "eth0");
        thenInterfaceHas("eth0", NetInterfaceType.ETHERNET, "02:42:ac:11:00:02", "172.17.0.2");
    }

    @Test
    public void shouldRemoveTheParentLinkOfABusyBoxContainerInterface() throws KuraException {
        givenLinkOutput(LOOPBACK_LINK + "2: eth0@if97: <BROADCAST,MULTICAST,UP,LOWER_UP,M-DOWN> mtu 1500 qdisc "
                + "noqueue state UP \\    link/ether 46:55:d2:a1:c5:36 brd ff:ff:ff:ff:ff:ff\n");
        givenInetOutput(LOOPBACK_INET + "2: eth0    inet 172.17.0.10/16 brd 172.17.255.255 scope global eth0\\"
                + "       valid_lft forever preferred_lft forever\n");

        whenIpAddrShowIsExecuted();

        thenInterfaceNamesAre("lo", "eth0");
        thenInterfaceHas("eth0", NetInterfaceType.ETHERNET, "46:55:d2:a1:c5:36", "172.17.0.10");
    }

    @Test
    public void shouldRemoveTheParentLinkOfAVlanInterface() throws KuraException {
        givenLinkOutput("3: eth0.100@eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP "
                + "mode DEFAULT group default qlen 1000\\    link/ether dc:a6:32:00:00:01 brd ff:ff:ff:ff:ff:ff\n");
        givenInetOutput("3: eth0.100    inet 192.168.100.2/24 brd 192.168.100.255 scope global eth0.100\\"
                + "       valid_lft forever preferred_lft forever\n");

        whenIpAddrShowIsExecuted();

        thenInterfaceNamesAre("eth0.100");
        thenInterfaceHas("eth0.100", NetInterfaceType.ETHERNET, "dc:a6:32:00:00:01", "192.168.100.2");
    }

    @Test
    public void shouldKeepTheNameOfAnInterfaceWithoutParentLink() throws KuraException {
        givenLinkOutput(LOOPBACK_LINK + "2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc mq state UP "
                + "mode DEFAULT group default qlen 1000\\    link/ether dc:a6:32:00:00:02 brd ff:ff:ff:ff:ff:ff\n");
        givenInetOutput(LOOPBACK_INET + "2: eth0    inet 192.168.1.10/24 brd 192.168.1.255 scope global eth0\\"
                + "       valid_lft forever preferred_lft forever\n");

        whenIpAddrShowIsExecuted();

        thenInterfaceNamesAre("lo", "eth0");
        thenInterfaceHas("eth0", NetInterfaceType.ETHERNET, "dc:a6:32:00:00:02", "192.168.1.10");
    }

    private void givenLinkOutput(String output) {
        this.linkOutput = output;
    }

    private void givenInetOutput(String output) {
        this.inetOutput = output;
    }

    private void whenIpAddrShowIsExecuted() throws KuraException {
        CommandExecutorService executorService = mock(CommandExecutorService.class);
        when(executorService.execute(any(Command.class))).thenAnswer(invocation -> {
            Command command = invocation.getArgument(0);
            boolean inet = Arrays.asList(command.getCommandLine()).contains("addr");
            return status(command, inet ? this.inetOutput : this.linkOutput);
        });

        this.configs = new IpAddrShow(executorService).exec();
    }

    private void thenInterfaceNamesAre(String... names) {
        assertEquals(Arrays.asList(names),
                Arrays.stream(this.configs).map(LinuxIfconfig::getName).collect(Collectors.toList()));
    }

    private void thenInterfaceHas(String name, NetInterfaceType type, String macAddress, String inetAddress) {
        Optional<LinuxIfconfig> config = Arrays.stream(this.configs).filter(c -> name.equals(c.getName()))
                .findFirst();
        assertTrue(config.isPresent());
        assertEquals(type, config.get().getType());
        assertEquals(macAddress, config.get().getMacAddress());
        assertEquals(inetAddress, config.get().getInetAddress());
    }

    private static CommandStatus status(Command command, String output) {
        CommandStatus status = new CommandStatus(command, new LinuxExitStatus(0));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = output.getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
        status.setOutputStream(out);
        return status;
    }
}
