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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.eclipse.kura.core.linux.executor.LinuxPid;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Pid;
import org.junit.Test;

public class ExecutorUtilTerminationTest {

    private static Pid pid = new LinuxPid(1234);
    private static String[] commandLine = { "find", "/", "-name", "foo" };
    private static ByteArrayOutputStream out = new ByteArrayOutputStream();
    private ExecutorUtil executorUtil;
    private boolean isStopped;
    private boolean isKilled;

    @Test
    public void shouldNotStopUnprivilegedCommand() {
        givenExecutor();

        whenStopUnprivilegedCommand(pid, LinuxSignal.SIGTERM);

        thenCommandIsNotStopped();
    }

    @Test
    public void shouldStopPrivilegedCommand() {
        givenExecutor();

        whenStopPrivilegedCommand(pid, LinuxSignal.SIGKILL);

        thenCommandIsStopped();
    }

    @Test
    public void shouldNotKillUnprivilegedCommand() {
        givenExecutor();

        whenKillUnprivilegedCommand(commandLine, LinuxSignal.SIGTERM);

        thenCommandIsNotKilled();
    }

    @Test
    public void shouldKillPrivilegedCommand() {
        givenExecutor();

        whenKillPrivilegedCommand(commandLine, LinuxSignal.SIGKILL);

        thenCommandIsKilled();
    }

    private void givenExecutor() {
        DefaultExecutor deMock = mock(DefaultExecutor.class);
        this.executorUtil = new ExecutorUtil() {

            @Override
            protected Executor getExecutor() {
                return deMock;
            }

            @Override
            protected ByteArrayOutputStream createStream() {
                return ExecutorUtilTerminationTest.out;
            }
        };
        configureMock(deMock);
    }

    private void whenStopUnprivilegedCommand(Pid pid, LinuxSignal signal) {
        this.isStopped = this.executorUtil.stopUnprivileged(pid, signal);
    }

    private void whenStopPrivilegedCommand(Pid pid, LinuxSignal signal) {
        this.isStopped = this.executorUtil.stopPrivileged(pid, signal);
    }

    private void whenKillUnprivilegedCommand(String[] commandLine, LinuxSignal signal) {
        this.isKilled = this.executorUtil.killUnprivileged(commandLine, signal);
    }

    private void whenKillPrivilegedCommand(String[] commandLine, LinuxSignal signal) {
        this.isKilled = this.executorUtil.killPrivileged(commandLine, signal);
    }

    private void thenCommandIsStopped() {
        assertTrue(this.isStopped);
    }

    private void thenCommandIsNotStopped() {
        assertFalse(this.isStopped);
    }

    private void thenCommandIsKilled() {
        assertTrue(this.isKilled);
    }

    private void thenCommandIsNotKilled() {
        assertFalse(this.isKilled);
    }

    private void configureMock(DefaultExecutor deMock) {
        String executablePs = "ps";
        String[] argumentsPs1 = { "-p", "1234" };
        String[] argumentsPsAx = { "-ax" };
        String executableUnprivileged = "su";
        String[] argumentsKill1 = { "kura", "-c", "timeout -s SIGTERM 60 kill -15 1234" };
        String[] argumentsKill2 = { "kill", "-9", "1234" };
        try {
            when(deMock.execute(argThat(new CommandLineMatcher(executableUnprivileged, argumentsKill1)))).thenReturn(1);
            when(deMock.execute(argThat(new CommandLineMatcher(executableUnprivileged, argumentsKill2)))).thenReturn(0);
            doAnswer(invocation -> {
                out.reset();
                PrintWriter pw = new PrintWriter(out);
                pw.println(1234);
                pw.flush();
                pw.close();
                return 0;
            }).when(deMock).execute(argThat(new CommandLineMatcher(executablePs, argumentsPs1)));
            doAnswer(invocation -> {
                out.reset();
                PrintWriter pw = new PrintWriter(out);
                pw.println(" 1234 pts/0    R+     0:00 find / -name foo");
                pw.flush();
                pw.close();
                return 0;
            }).when(deMock).execute(argThat(new CommandLineMatcher(executablePs, argumentsPsAx)));
        } catch (IOException e) {
            // Do nothing...
        }
        when(deMock.getWatchdog()).thenReturn(new ExecuteWatchdog(1));
    }

}
