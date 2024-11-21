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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.FileAlreadyExistsException;
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
    private boolean toolExists;
    private boolean systemdUnitExists;

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
    public void shouldCheckToolExistence() throws NoSuchFieldException, IllegalAccessException, IOException {
        givenLinuxNetworkUtil();
        givenToolPaths("/tmp/");
        givenTool("/tmp/dhcpd");

        whenCheckTool("dhcpd");

        thenToolExists();
    }

    @Test
    public void shouldCheckSystemdUnitExistence()
            throws NoSuchFieldException, IllegalAccessException, IOException, InterruptedException {
        givenLinuxNetworkUtil();
        givenProcessBuilder(0);

        whenCheckSystemdUnit("dnsmask.service");

        thenSystemdUnitExists();
    }

    @Test
    public void shouldNotCheckSystemdUnitExistence()
            throws NoSuchFieldException, IllegalAccessException, IOException, InterruptedException {
        givenLinuxNetworkUtil();
        givenProcessBuilder(4);

        whenCheckSystemdUnit("dnsmask.service");

        thenSystemdUnitNotExist();
    }

    private void givenLinuxNetworkUtil() {
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        this.commandExecutorServiceStub = new CommandExecutorServiceStub(status);
        this.linuxNetworkUtil = new LinuxNetworkUtil(this.commandExecutorServiceStub);
    }

    private void givenToolPaths(String path) throws NoSuchFieldException, IllegalAccessException {
        setFinalStaticField(LinuxNetworkUtil.class, "DEFAULT_PATH", new String[] { path });
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

    private void givenProcessBuilder(int returnCode)
            throws NoSuchFieldException, IOException, InterruptedException, IllegalAccessException {
        Process mockedProcess = mock(Process.class);
        when(mockedProcess.waitFor()).thenReturn(returnCode);
        when(mockedProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
        ProcessBuilder mockedProcessBuilder = mock(ProcessBuilder.class);
        when(mockedProcessBuilder.start()).thenReturn(mockedProcess);
        setFinalStaticField(LinuxNetworkUtil.class, "PROCESS_BUILDER", mockedProcessBuilder);
    }

    private void whenDedicatedInterfaceName(String dedicatedInterfaceName) {
        this.dedicatedInterfaceName = dedicatedInterfaceName;
    }

    private void whenCheckTool(String toolName) {
        this.toolExists = LinuxNetworkUtil.toolExists(toolName);
    }

    private void whenCheckSystemdUnit(String unitName) {
        this.systemdUnitExists = LinuxNetworkUtil.systemdSystemUnitExists(unitName);
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

    static void setFinalStaticField(Class clazz, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        Field modifiers = field.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }

}
