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
package org.eclipse.kura.linux.net.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.junit.Test;

public class LinuxNetworkUtilTest {

    private LinuxNetworkUtil linuxNetworkUtil;
    private String interfaceName;
    private String dedicatedInterfaceName;
    private CommandExecutorServiceStub commandExecutorServiceStub;
    private String macAddress;
    private String linkStatus;

    @Test
    public void createApNetworkInterface() {
        givenLinuxNetworkUtil();
        givenInterfaceName("testInterface");

        whenDedicatedInterfaceName("testInterface_ap");

        thenApNetworkInterfaceIsCreated();
    }

    @Test
    public void setNetworkInterfaceMacAddress() {
        givenLinuxNetworkUtil();
        givenMacAddress("12:34:56:78:ab:cd");

        whenDedicatedInterfaceName("testInterface_ap");

        thenNetworkInterfaceMacAddressIsSet();
    }

    @Test
    public void setNetworkInterfaceLinkUp() {
        givenLinuxNetworkUtil();
        givenLinkStatus("up");

        whenDedicatedInterfaceName("testInterface_ap");

        thenNetworkInterfaceLinkIsUP();
    }

    @Test
    public void setNetworkInterfaceLinkDown() {
        givenLinuxNetworkUtil();
        givenLinkStatus("down");

        whenDedicatedInterfaceName("testInterface_ap");

        thenNetworkInterfaceLinkIsDown();
    }

    @Test
    public void addToolsSearchFolder() {
        givenLinuxNetworkUtil();
        givenIwToolInFolder("tool", System.getProperty("java.io.tmpdir"));

        whenNewToolsSearchFolderIs(System.getProperty("java.io.tmpdir"));

        thenToolsIsFound("tool");
    }

    private void givenIwToolInFolder(String tool, String folder) {
        Path toolPath = Paths.get(folder, tool);
        try {
            if (Files.notExists(toolPath)) {
                Files.createFile(Paths.get(folder, tool));
            }
        } catch (IOException e) {
            fail();
        }
    }

    private void givenLinuxNetworkUtil() {
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        this.commandExecutorServiceStub = new CommandExecutorServiceStub(status);
        this.linuxNetworkUtil = new LinuxNetworkUtil(this.commandExecutorServiceStub);

    }

    private void givenInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    private void givenMacAddress(String macAddress) {
        this.macAddress = macAddress;

    }

    private void whenDedicatedInterfaceName(String dedicatedInterfaceName) {
        this.dedicatedInterfaceName = dedicatedInterfaceName;

    }

    private void whenNewToolsSearchFolderIs(String folder) {
        LinuxNetworkUtil.addToolSearchFolder(folder);
    }

    private void givenLinkStatus(String linkStatus) {
        this.linkStatus = linkStatus;
    }

    private void thenApNetworkInterfaceIsCreated() {
        try {
            this.linuxNetworkUtil.createApNetworkInterface(this.interfaceName, this.dedicatedInterfaceName);
            assertArrayEquals(
                    LinuxNetworkUtil.formIwDevIfaceInterfaceAddAp(this.interfaceName, this.dedicatedInterfaceName),
                    this.commandExecutorServiceStub.getLastCommand());
        } catch (KuraException e) {
            fail();
        }
    }

    private void thenNetworkInterfaceMacAddressIsSet() {
        try {
            this.linuxNetworkUtil.setNetworkInterfaceMacAddress(this.dedicatedInterfaceName);
            assertArrayEquals(LinuxNetworkUtil.formIpLinkSetAddress(this.dedicatedInterfaceName, this.macAddress),
                    this.commandExecutorServiceStub.getLastCommand());
        } catch (KuraException e) {
            fail();
        }
    }

    private void thenNetworkInterfaceLinkIsUP() {
        try {
            this.linuxNetworkUtil.setNetworkInterfaceLinkUp(this.dedicatedInterfaceName);
            assertArrayEquals(LinuxNetworkUtil.formIpLinkSetStatus(this.dedicatedInterfaceName, this.linkStatus),
                    this.commandExecutorServiceStub.getLastCommand());
        } catch (KuraException e) {
            fail();
        }
    }

    private void thenNetworkInterfaceLinkIsDown() {
        try {
            this.linuxNetworkUtil.setNetworkInterfaceLinkDown(this.dedicatedInterfaceName);
            assertArrayEquals(LinuxNetworkUtil.formIpLinkSetStatus(this.dedicatedInterfaceName, this.linkStatus),
                    this.commandExecutorServiceStub.getLastCommand());
        } catch (KuraException e) {
            fail();
        }
    }

    private void thenToolsIsFound(String tool) {
        assertTrue(LinuxNetworkUtil.toolExists(tool));
    }

}
