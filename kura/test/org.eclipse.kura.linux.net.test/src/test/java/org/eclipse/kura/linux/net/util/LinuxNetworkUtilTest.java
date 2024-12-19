/*******************************************************************************
 * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.junit.Ignore;
import org.junit.Test;

public class LinuxNetworkUtilTest {

    private LinuxNetworkUtil linuxNetworkUtil;
    private String interfaceName;
    private String dedicatedInterfaceName;
    private CommandExecutorServiceStub commandExecutorServiceStub;
    private String macAddress;
    private String linkStatus;
    private boolean toolExists;
    private boolean systemdUnitExists;
    private Optional<String> toolPath;

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
    public void shouldCheckToolExistence() throws IOException {
        givenLinuxNetworkUtil();
        givenTool("/tmp/dhcpd");

        whenCheckTool("dhcpd", "/tmp/");

        thenToolExists();
    }

    @Test
    @Ignore("Given the current implementation, we cannot inject the exit code of the command execution and therefore this test will fail.")
    public void shouldCheckSystemdUnitExistence() throws IOException {
        givenLinuxNetworkUtil();
        givenTool("/tmp/systemctl");

        whenCheckSystemdUnit("dnsmask.service", "/tmp/");

        thenSystemdUnitExists();
    }

    @Test
    public void shouldNotCheckSystemdUnitExistence() throws IOException {
        givenLinuxNetworkUtil();
        givenTool("/tmp/systemctl");

        whenCheckSystemdUnit("dnsmask.service", "/tmp/");

        thenSystemdUnitNotExist();
    }

    @Test
    public void shouldNotCheckSystemdUnitExistenceIfSystemctlNotExist() {
        givenLinuxNetworkUtil();

        whenCheckSystemdUnit("dnsmask.service", "/tmp/");

        thenSystemdUnitNotExist();
    }

    @Test
    public void shouldGetToolPath() throws IOException {
        givenLinuxNetworkUtil();
        givenTool("/tmp/myAwesomeCommand");

        whenGetTool("myAwesomeCommand", "/tmp/");

        thenToolIsRetrieved("/tmp/myAwesomeCommand");
    }

    private void givenLinuxNetworkUtil() {
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        this.commandExecutorServiceStub = new CommandExecutorServiceStub(status);
        this.linuxNetworkUtil = new LinuxNetworkUtil(this.commandExecutorServiceStub);
    }

    private void givenTool(String tool) throws IOException {
        Path newFilePath = Paths.get(tool);
        try {
            Files.createFile(newFilePath);
        } catch (FileAlreadyExistsException e) {
            // do nothing
        }
    }

    private void givenInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    private void givenMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    private void givenLinkStatus(String linkStatus) {
        this.linkStatus = linkStatus;
    }

    private void whenDedicatedInterfaceName(String dedicatedInterfaceName) {
        this.dedicatedInterfaceName = dedicatedInterfaceName;
    }

    private void whenCheckTool(String toolName, String searchPath) {
        this.toolExists = LinuxNetworkUtil.toolExists(toolName, new String[] { searchPath });
    }

    private void whenCheckSystemdUnit(String unitName, String searchPath) {
        this.systemdUnitExists = LinuxNetworkUtil.systemdSystemUnitExists(unitName, new String[] { searchPath });
    }

    private void whenGetTool(String tool, String searchPath) {
        this.toolPath = LinuxNetworkUtil.getToolPath(tool, new String[] { searchPath });
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

    private void thenToolExists() {
        assertTrue(this.toolExists);
    }

    private void thenSystemdUnitExists() {
        assertTrue(this.systemdUnitExists);
    }

    private void thenSystemdUnitNotExist() {
        assertFalse(this.systemdUnitExists);
    }

    private void thenToolIsRetrieved(String expectedToolPath) {
        assertTrue(this.toolPath.isPresent());
        assertEquals(expectedToolPath, this.toolPath.get());
    }

}
