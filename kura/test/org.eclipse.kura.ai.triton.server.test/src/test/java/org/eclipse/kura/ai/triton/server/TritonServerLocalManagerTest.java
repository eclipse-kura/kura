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
 ******************************************************************************/

package org.eclipse.kura.ai.triton.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.CommandExecutorService;
import org.junit.Test;

public class TritonServerLocalManagerTest {

    private static final String[] TRITONSERVERCMD = new String[] { "tritonserver" };
    private static final String MOCK_DECRYPT_FOLDER = "test";

    private Map<String, Object> properties = new HashMap<>();
    private TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

    private CommandExecutorService ces;
    private TritonServerLocalManager manager;

    @Test
    public void killMethodShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.TRUE);
        givenServiceOptionsBuiltWith(properties);

        givenMockCommandExecutionService();
        givenMockCommandExecutionServiceReturnsTritonIsRunning();

        givenLocalManagerBuiltWith(this.options, this.ces, MOCK_DECRYPT_FOLDER);

        whenKillIsCalled();

        thenCommandServiceKillWasCalledWith(TRITONSERVERCMD, LinuxSignal.SIGKILL);
    }

    @Test
    public void stopMethodShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.TRUE);
        givenServiceOptionsBuiltWith(properties);

        givenMockCommandExecutionService();
        givenMockCommandExecutionServiceReturnsTritonIsRunning();

        givenLocalManagerBuiltWith(this.options, this.ces, MOCK_DECRYPT_FOLDER);

        whenStopIsCalled();

        thenCommandServiceKillWasCalledWith(TRITONSERVERCMD, LinuxSignal.SIGINT);
    }

    /*
     * Given
     */
    private void givenPropertyWith(String name, Object value) {
        this.properties.put(name, value);
    }

    private void givenServiceOptionsBuiltWith(Map<String, Object> properties) {
        this.options = new TritonServerServiceOptions(properties);
    }

    private void givenMockCommandExecutionService() {
        this.ces = mock(CommandExecutorService.class);
    }

    private void givenMockCommandExecutionServiceReturnsTritonIsRunning() {
        when(this.ces.isRunning(new String[] { "tritonserver" })).thenReturn(true);
    }

    private void givenLocalManagerBuiltWith(TritonServerServiceOptions options, CommandExecutorService ces,
            String decryptionFolder) {
        this.manager = new TritonServerLocalManager(options, ces, decryptionFolder);
    }

    /*
     * When
     */
    private void whenKillIsCalled() {
        this.manager.kill();
    }

    private void whenStopIsCalled() {
        this.manager.stop();
    }

    /*
     * Then
     */
    private void thenCommandServiceKillWasCalledWith(String[] expectedCmd, LinuxSignal expectedSignal) {
        verify(this.ces, times(1)).kill(expectedCmd, expectedSignal);
    }
}
