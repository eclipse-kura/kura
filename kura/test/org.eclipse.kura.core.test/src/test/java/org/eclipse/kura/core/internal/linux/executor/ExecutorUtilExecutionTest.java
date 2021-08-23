/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.internal.linux.executor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.junit.Test;

public class ExecutorUtilExecutionTest {

    private static final String COMMAND_STRING = "ls -all";
    private static final Map<String, String> ENV = Collections.singletonMap("ENV1", "VALUE1");

    private ExecutorUtil executorUtil;
    private Command command;
    private CommandStatus status;
    private Consumer<CommandStatus> callback = cs -> this.status = cs;

    @Test
    public void shouldReturnErrorOnUnprivilegedSyncExecution() {
        givenExecutor();
        givenCommand(COMMAND_STRING);

        whenCommandIsUnprivilegedExecuted();

        thenReturnExitValue(1);
    }

    @Test
    public void shouldReturnSuccessOnPrivilegedSyncExecution() {
        givenExecutor();
        givenCommand(COMMAND_STRING);

        whenCommandIsPrivilegedExecuted();

        thenReturnExitValue(0);
    }

    @Test
    public void shouldAcceptErrorOnUnprivilegedAsyncExecution() {
        givenExecutor();
        givenCommand(COMMAND_STRING);

        whenCommandIsUnprivilegedAsyncExecuted(this.callback);

        thenReturnExitValue(1);
    }

    @Test
    public void shouldAcceptSuccessOnPrivilegedAsyncExecution() {
        givenExecutor();
        givenCommand(COMMAND_STRING);

        whenCommandIsPrivilegedAsyncExecuted(this.callback);

        thenReturnExitValue(0);
    }

    private void givenExecutor() {
        DefaultExecutor deMock = mock(DefaultExecutor.class);
        this.executorUtil = new ExecutorUtil() {

            @Override
            protected Executor getExecutor() {
                return deMock;
            }
        };
        configureMock(deMock);
    }

    private void givenCommand(String commandLine) {
        this.command = new Command(commandLine.split(" "));
        this.command.setOutputStream(new ByteArrayOutputStream());
        this.command.setErrorStream(new ByteArrayOutputStream());
        this.command.setEnvironment(ENV);
        this.command.setTimeout(1000);
        this.command.setSignal(LinuxSignal.SIGHUP);
        this.command.setExecuteInAShell(true);
        this.command.setDirectory("/var");
    }

    private void whenCommandIsUnprivilegedExecuted() {
        this.status = this.executorUtil.executeUnprivileged(this.command);
    }

    private void whenCommandIsUnprivilegedAsyncExecuted(Consumer<CommandStatus> callback) {
        this.executorUtil.executeUnprivileged(this.command, callback);
    }

    private void whenCommandIsPrivilegedExecuted() {
        this.status = this.executorUtil.executePrivileged(this.command);
    }

    private void whenCommandIsPrivilegedAsyncExecuted(Consumer<CommandStatus> callback) {
        this.executorUtil.executePrivileged(this.command, callback);
    }

    private void thenReturnExitValue(int expectedExitValue) {
        assertEquals(expectedExitValue, this.status.getExitStatus().getExitCode());
    }

    private void configureMock(DefaultExecutor deMock) {
        CommandStatus csSuccess = new CommandStatus(this.command, new LinuxExitStatus(0));
        CommandStatus csFailure = new CommandStatus(this.command, new LinuxExitStatus(1));
        String executableUnprivileged = "su";
        String[] argumentsUnprivileged = { "kura", "-c", "ENV1=VALUE1 timeout -s SIGHUP 1000 " + COMMAND_STRING };
        String executablePrivileged = "/bin/sh";
        String[] argumentsPrivileged = { "-c", COMMAND_STRING };
        try {
            when(deMock.execute(argThat(new CommandLineMatcher(executableUnprivileged, argumentsUnprivileged)),
                    anyMap())).thenReturn(1);
            when(deMock.execute(argThat(new CommandLineMatcher(executablePrivileged, argumentsPrivileged)), anyMap()))
                    .thenReturn(0);
            doAnswer(invocation -> {
                this.callback.accept(csFailure);
                return null;
            }).when(deMock).execute(argThat(new CommandLineMatcher(executableUnprivileged, argumentsUnprivileged)),
                    anyMap(), anyObject());
            doAnswer(invocation -> {
                this.callback.accept(csSuccess);
                return null;
            }).when(deMock).execute(argThat(new CommandLineMatcher(executablePrivileged, argumentsPrivileged)),
                    anyMap(), anyObject());
        } catch (IOException e) {
            // Do nothing...
        }
        when(deMock.getWatchdog()).thenReturn(new ExecuteWatchdog(1));
    }

}
