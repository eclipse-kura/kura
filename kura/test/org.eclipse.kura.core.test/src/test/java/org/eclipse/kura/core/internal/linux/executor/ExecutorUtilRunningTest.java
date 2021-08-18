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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.eclipse.kura.core.linux.executor.LinuxPid;
import org.eclipse.kura.executor.Pid;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

public class ExecutorUtilRunningTest {

    private static String[] commandLine1 = { "find", "/", "-name", "foo" };
    private static String[] commandLine2 = { "find", "/", "-name", "bar" };
    private static ByteArrayOutputStream out = new ByteArrayOutputStream();
    private ExecutorUtil executorUtil;
    private boolean isRunning;
    private Map<String, Pid> pids = new HashMap<>();

    @Test
    public void shouldNotBeRunningPid() {
        givenExecutor();

        whenCheckIfRunning(new LinuxPid(1234));

        thenCommandIsNotRunning();
    }

    @Test
    public void shouldBeRunningPid() {
        givenExecutor();

        whenCheckIfRunning(new LinuxPid(12345));

        thenCommandIsRunning();
    }

    @Test
    public void shouldNotBeRunningCommandLine() {
        givenExecutor();

        whenCheckIfRunning(commandLine1);

        thenCommandIsNotRunning();
    }

    @Test
    public void shouldBeRunningCommandLine() {
        givenExecutor();

        whenCheckIfRunning(commandLine2);

        thenCommandIsRunning();
    }

    @Test
    public void shouldNotRetrievePids() {
        givenExecutor();

        whenRetrievePid(commandLine1);

        thenPidIsNotFound();
    }

    @Test
    public void shouldRetrievePids() {
        givenExecutor();

        whenRetrievePid(commandLine2);

        thenPidIsFound();
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
                return ExecutorUtilRunningTest.out;
            }
        };
        configureMock(deMock);
    }

    private void whenCheckIfRunning(Pid pid) {
        this.isRunning = this.executorUtil.isRunning(pid);
    }

    private void whenCheckIfRunning(String[] commandLine) {
        this.isRunning = this.executorUtil.isRunning(commandLine);
    }

    private void whenRetrievePid(String[] commandLine) {
        this.pids = this.executorUtil.getPids(commandLine);
    }

    private void thenCommandIsRunning() {
        assertTrue(this.isRunning);
    }

    private void thenCommandIsNotRunning() {
        assertFalse(this.isRunning);
    }

    private void thenPidIsFound() {
        assertFalse(this.pids.isEmpty());
    }

    private void thenPidIsNotFound() {
        assertTrue(this.pids.isEmpty());
    }

    private void configureMock(DefaultExecutor deMock) {
        String executablePs = "ps";
        String[] argumentsPs1 = { "-p", "1234" };
        String[] argumentsPs2 = { "-p", "12345" };
        String[] argumentsPsAx = { "-ax" };
        try {
            when(deMock.execute(argThat(new CommandLineMatcher(executablePs, argumentsPs1)))).thenReturn(1);
            doAnswer(invocation -> {
                out.reset();
                PrintWriter pw = new PrintWriter(out);
                pw.println(12345);
                pw.flush();
                pw.close();
                return 0;
            }).when(deMock).execute(argThat(new CommandLineMatcher(executablePs, argumentsPs2)));
            doAnswer(invocation -> {
                out.reset();
                PrintWriter pw = new PrintWriter(out);
                pw.println(" 4333 pts/0    R+     0:00 find / -name bar");
                pw.flush();
                pw.close();
                return 0;
            }).when(deMock).execute(argThat(new CommandLineMatcher(executablePs, argumentsPsAx)));
        } catch (IOException e) {
            // Do nothing...
        }
    }

    class CommandLineMatcher extends ArgumentMatcher<CommandLine> {

        String executable;
        List<String> arguments;

        CommandLineMatcher(String executable, String[] arguments) {
            this.executable = executable;
            this.arguments = new ArrayList<>();
            Arrays.asList(arguments).forEach(this.arguments::add);
        }

        @Override
        public String toString() {
            return executable + " " + String.join(" ", arguments);
        }

        @Override
        public boolean matches(Object argument) {
            boolean matched = false;
            if (argument instanceof CommandLine) {
                CommandLine cl = (CommandLine) argument;
                if (this.executable.equals(cl.getExecutable()) && this.arguments.size() == cl.getArguments().length
                        && this.arguments.containsAll(Arrays.asList(cl.getArguments()))) {
                    matched = true;
                }
            }
            return matched;
        }
    }
}
