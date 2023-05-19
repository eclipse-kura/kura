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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.ExitStatus;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

public class DnsmasqToolTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private File tmpConfigFile;
    private CommandExecutorService mockExecutor;
    private static MockedStatic<DhcpServerManager> mockServerManager;
    private DnsmasqTool tool;
    private boolean isRunning;
    private CommandStatus startInterfaceStatus;
    private boolean interfaceDisabled;
    private Exception occurredException;
    
    /*
     * Scenarios
     */

    @Test
    public void isRunningShouldReturnTrueIfAlreadyStarted() throws Exception {
        givenExecutorReturnsExitStatus(0, true);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDhcpServerManagerReturn("eth0", this.tmpConfigFile.getAbsolutePath());
        givenDnsmasqTool();
        givenStartInterface("eth0");

        whenIsRunning("eth0");

        thenIsRunningReturned(true);
    }

    @Test
    public void isRunningShouldReturnFalseIfNeverStarted() throws Exception {
        givenExecutorReturnsExitStatus(0, true);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDhcpServerManagerReturn("eth0", this.tmpConfigFile.getAbsolutePath());
        givenDnsmasqTool();

        whenIsRunning("eth0");

        thenIsRunningReturned(false);
    }

    @Test
    public void isRunningShouldReturnFalseIfServiceIsNotActive() throws Exception {
        givenExecutorReturnsExitStatus(1, false);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDhcpServerManagerReturn("eth0", this.tmpConfigFile.getAbsolutePath());
        givenDnsmasqTool();

        whenIsRunning("eth0");

        thenIsRunningReturned(false);
    }

    @Test
    public void shouldRemoveInterfaceConfigIfStartFails() throws Exception {
        givenExecutorReturnsExitStatus(1, false);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDhcpServerManagerReturn("eth0", this.tmpConfigFile.getAbsolutePath());
        givenDnsmasqTool();

        whenStartInterface("eth0");

        thenInterfaceNotStarted();
        thenConfigFileNotPresent("eth0");
    }

    @Test
    public void shouldRemoveInterfaceConfigIfInterfaceDisabled() throws Exception {
        givenExecutorReturnsExitStatus(0, true);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDhcpServerManagerReturn("eth0", this.tmpConfigFile.getAbsolutePath());
        givenDnsmasqTool();
        givenStartInterface("eth0");

        whenDisableInterface("eth0");

        thenConfigFileNotPresent("eth0");
        thenDisableWasSuccessful();
    }

    @Test
    public void shouldReturnExceptionWhenFailToDisableInterface() throws Exception {
        givenExecutorReturnsExitStatus(1, false);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDhcpServerManagerReturn("eth0", this.tmpConfigFile.getAbsolutePath());
        givenDnsmasqTool();

        whenDisableInterface("eth0");

        thenConfigFileNotPresent("eth0");
        thenKuraProcessExecutionErrorExceptionOccurred();
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenExecutorReturnsExitStatus(int exitCode, boolean isSuccessful) {
        this.mockExecutor = mock(CommandExecutorService.class);

        ExitStatus returnedExitStatus = new ExitStatus() {

            @Override
            public int getExitCode() {
                return exitCode;
            }

            @Override
            public boolean isSuccessful() {
                return isSuccessful;
            }
            
        };
        CommandStatus returnedStatus = new CommandStatus(DnsmasqTool.IS_ACTIVE_COMMAND, returnedExitStatus);

        when(this.mockExecutor.execute(any())).thenReturn(returnedStatus);
    }

    private void givenDhcpServerManagerReturn(String interfaceName, String returnedConfigFilename) {
        mockServerManager = mockStatic(DhcpServerManager.class);
        mockServerManager.when(() -> DhcpServerManager.getConfigFilename(any())).thenReturn(returnedConfigFilename);
    }

    private void givenConfigFile(String filename) throws IOException {
        try {
            this.tmpFolder.newFolder("etc", "dnsmasq.d");
        } catch (Exception e) {}
        this.tmpConfigFile = this.tmpFolder.newFile(filename);
    }

    private void givenDnsmasqTool() throws Exception {
        this.tool = new DnsmasqTool(this.mockExecutor);
        this.tool.setDnsmasqGlobalConfigFile(this.tmpConfigFile.getAbsoluteFile().getParent() + "/dnsmasq-globals.conf");
    }

    private void givenStartInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        this.tool.startInterface(interfaceName);
    }

    /*
     * When
     */

    private void whenIsRunning(String interfaceName) throws KuraProcessExecutionErrorException {
        this.isRunning = this.tool.isRunning(interfaceName);
    }

    private void whenStartInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        this.startInterfaceStatus = this.tool.startInterface(interfaceName);
    }

    private void whenDisableInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        try {
            this.interfaceDisabled = this.tool.disableInterface(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }        
    }

    /*
     * Then
     */

    private void thenIsRunningReturned(boolean expectedResult) {
        assertEquals(expectedResult, this.isRunning);
    }

    private void thenInterfaceNotStarted() {
        assertFalse(this.startInterfaceStatus.getExitStatus().isSuccessful());
    }

    private void thenConfigFileNotPresent(String interfaceName) {
        assertFalse(this.tmpConfigFile.exists());
    }

    private void thenDisableWasSuccessful() {
        assertTrue(this.interfaceDisabled);
    }

    private void thenKuraProcessExecutionErrorExceptionOccurred() {
        assertTrue(this.occurredException instanceof KuraProcessExecutionErrorException);
    }

    /*
     * Utilities
     */

    @After
    public void closeStaticMock() {
        mockServerManager.close();
    }
}
