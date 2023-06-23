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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.ExitStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.MockedStatic;

@RunWith(Parameterized.class)
public class DhcpdToolTest {

    @Parameters
    public static Collection<Object[]> SimTypeParams() {
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[] { DhcpServerTool.DHCPD });
        params.add(new Object[] { DhcpServerTool.UDHCPD });
        return params;
    }

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private CommandExecutorService mockExecutor;
    private DhcpdTool tool;
    private boolean isRunningResult;
    private CommandStatus interfaceStartStatus;
    private static MockedStatic<DhcpServerManager> mockServerManager;
    private boolean isInterfaceDisabled;
    private Exception occurredException;
    private File tmpConfigFile;
    private Map<String, Pid> runningPids;
    private DhcpServerTool dhcpServerTool;

    public DhcpdToolTest(DhcpServerTool dhcpServerTool) {
        this.dhcpServerTool = dhcpServerTool;
    }

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnIsRunningWhenToolSucceeds() {
        givenExecutorReturnsExitStatus(0, true);
        givenDhcpdTool(this.dhcpServerTool);

        whenIsRunning("eth0");

        thenToolReportedRunning();
    }

    @Test
    public void shouldReturnNotRunningWhenToolFails() {
        givenExecutorReturnsExitStatus(1, false);
        givenDhcpdTool(this.dhcpServerTool);

        whenIsRunning("eth0");

        thenToolReportedNotRunning();
    }

    @Test
    public void shouldStartInterfaceWhenToolSucceeds() {
        givenExecutorReturnsExitStatus(0, true);
        givenDhcpdTool(this.dhcpServerTool);

        whenStartInterface("eth0");

        thenInterfaceStarted();
    }

    @Test
    public void shouldNotStartInterfaceWhenToolFails() {
        givenExecutorReturnsExitStatus(1, false);
        givenDhcpdTool(this.dhcpServerTool);

        whenStartInterface("eth0");

        thenInterfaceNotStarted();
    }

    @Test
    public void shouldRemovePidFileWhenDisablingInterface() throws Exception {
        givenExecutorReturnsExitStatus(0, true);
        givenConfigFile("dhcpd-eth0.pid");
        givenDhcpServerManagerReturn("eth0", this.tmpConfigFile.getAbsolutePath());
        givenDhcpdTool(this.dhcpServerTool);
        givenRunningPids();
        givenExecutorCanStopToolSuccessfully();

        whenDisableInterface("eth0");

        thenFileWasDeleted("dhcpd-eth0.pid");
        thenDisableInterfaceReturned(true);
    }

    @Test
    public void shouldReturnExceptionWhenDisablingInterfaceFails() throws Exception {
        givenExecutorReturnsExitStatus(0, true);
        givenConfigFile("dhcpd-eth0.pid");
        givenDhcpServerManagerReturn("eth0", this.tmpConfigFile.getAbsolutePath());
        givenDhcpdTool(this.dhcpServerTool);
        givenRunningPids();
        givenExecutorFailsStopTool();

        whenDisableInterface("eth0");

        thenKuraProcessExecutionErrorExceptionOccurred();
        thenFileWasNotDeleted("dhcpd-eth0.pid");
    }

    @Test
    public void shouldReturnFalseWhenDisablingInterfaceandNoRunningTool() throws Exception {
        givenExecutorReturnsExitStatus(0, true);
        givenConfigFile("dhcpd-eth0.pid");
        givenDhcpServerManagerReturn("eth0", this.tmpConfigFile.getAbsolutePath());
        givenDhcpdTool(this.dhcpServerTool);
        givenNoRunningPids();
        givenExecutorCanStopToolSuccessfully();

        whenDisableInterface("eth0");

        thenDisableInterfaceReturned(false);
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
        when(this.mockExecutor.isRunning(any(String[].class))).thenReturn(isSuccessful);
    }

    private void givenDhcpdTool(DhcpServerTool tool) {
        this.tool = new DhcpdTool(mockExecutor, tool);
    }

    private void givenDhcpServerManagerReturn(String interfaceName, String returnedConfigFilename) {
        mockServerManager = mockStatic(DhcpServerManager.class);
        mockServerManager.when(() -> DhcpServerManager.getPidFilename(interfaceName)).thenReturn(returnedConfigFilename);
    }

    private void givenConfigFile(String filename) throws IOException {
        this.tmpConfigFile = this.tmpFolder.newFile(filename);
    }

    private void givenRunningPids() {
        this.runningPids = new HashMap<>();
        this.runningPids.put("test", new Pid() {

            @Override
            public int getPid() {
                return 1234;
            }
            
        });

        when(this.mockExecutor.getPids(any())).thenReturn(this.runningPids);
    }

    private void givenNoRunningPids() {
        this.runningPids = new HashMap<>();

        when(this.mockExecutor.getPids(any())).thenReturn(this.runningPids);
    }

    private void givenExecutorCanStopToolSuccessfully() {
        when(this.mockExecutor.stop(any(), any())).thenReturn(true);
    }

    private void givenExecutorFailsStopTool() {
        when(this.mockExecutor.stop(any(), any())).thenReturn(false);
    }

    /*
     * When
     */

    private void whenIsRunning(String interfaceName) {
        this.isRunningResult = this.tool.isRunning(interfaceName);
    }

    private void whenStartInterface(String interfaceName) {
        this.interfaceStartStatus = this.tool.startInterface(interfaceName);
    }

    private void whenDisableInterface(String interfaceName) {
        try {
            this.isInterfaceDisabled = this.tool.disableInterface(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenToolReportedRunning() {
        assertTrue(this.isRunningResult);
    }

    private void thenToolReportedNotRunning() {
        assertFalse(this.isRunningResult);
    }

    private void thenInterfaceStarted() {
        assertTrue(this.interfaceStartStatus.getExitStatus().isSuccessful());
    }

    private void thenInterfaceNotStarted() {
        assertFalse(this.interfaceStartStatus.getExitStatus().isSuccessful());
    }

    private void thenFileWasDeleted(String filename) {
        assertFalse(this.tmpConfigFile.exists());
    }

    private void thenFileWasNotDeleted(String filename) {
        assertTrue(this.tmpConfigFile.exists());
    }

    private void thenDisableInterfaceReturned(boolean expectedResult) {
        assertEquals(expectedResult, this.isInterfaceDisabled);
    }

    private void thenKuraProcessExecutionErrorExceptionOccurred() {
        assertTrue(this.occurredException instanceof KuraProcessExecutionErrorException);
    }

    /*
     * Utilities
     */

    @After
    public void closeStaticMock() {
        if (mockServerManager != null && !mockServerManager.isClosed()) {
            mockServerManager.close();
        }
    }

}
