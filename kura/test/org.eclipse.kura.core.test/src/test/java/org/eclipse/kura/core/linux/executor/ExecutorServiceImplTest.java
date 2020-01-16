/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.linux.executor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.eclipse.kura.core.linux.executor.privileged.PrivilegedExecutorServiceImpl;
import org.eclipse.kura.core.linux.executor.unprivileged.UnprivilegedExecutorServiceImpl;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.ExecutorFactory;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.AdditionalMatchers;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(Parameterized.class)
public class ExecutorServiceImplTest {

    private static final String LIST_COMMAND = "ls -all";
    private static final String SIGTERM = "SIGTERM";
    private static final String TIMEOUT = "timeout";
    private static final String TMP = "/tmp";
    private CommandExecutorService service;

    public ExecutorServiceImplTest(CommandExecutorService service) {
        this.service = service;
    }

    @Parameterized.Parameters
    public static Collection<CommandExecutorService> getServices() {
        return Arrays.asList(new UnprivilegedExecutorServiceImpl(), new PrivilegedExecutorServiceImpl());
    }

    @Test
    public void executeSyncCommand() throws IOException {
        System.out.println(service.getClass().getSimpleName() + ": Test synchronous command execution...");

        Executor mockExecutor = mock(DefaultExecutor.class);
        when(mockExecutor.getWatchdog()).thenReturn(new ExecuteWatchdog(10));

        if (service instanceof PrivilegedExecutorServiceImpl) {
            CommandLine privilegedCommandLine = new CommandLine("ls").addArgument("-all");
            when(mockExecutor
                    .execute(AdditionalMatchers.not(Matchers.argThat(new CommandLineMatcher(privilegedCommandLine)))))
                            .thenReturn(1);
        } else {
            CommandLine unprivilegedCommandLine = new CommandLine("sudo").addArgument("-u").addArgument("kura")
                    .addArgument("-s").addArgument(TIMEOUT).addArgument("-s").addArgument(SIGTERM).addArgument("10");
            unprivilegedCommandLine.addArgument("sh").addArgument("-c").addArgument(LIST_COMMAND, false);
            when(mockExecutor
                    .execute(AdditionalMatchers.not(Matchers.argThat(new CommandLineMatcher(unprivilegedCommandLine)))))
                            .thenReturn(1);
        }

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        Command command = new Command(new String[] { "ls", "-all" });
        command.setTimeout(10);
        command.setDirectory(TMP);
        CommandStatus status = service.execute(command);
        assertTrue(status.getExitStatus().isSuccessful());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeSyncCommandWithVariables() throws IOException {
        System.out
                .println(service.getClass().getSimpleName() + ": Test synchronous command execution with variables...");

        Executor mockExecutor = mock(DefaultExecutor.class);
        when(mockExecutor.getWatchdog()).thenReturn(new ExecuteWatchdog(10));

        if (service instanceof PrivilegedExecutorServiceImpl) {
            CommandLine privilegedCommandLine = new CommandLine("env");
            when(mockExecutor.execute(
                    AdditionalMatchers.not(Matchers.argThat(new CommandLineMatcher(privilegedCommandLine))), anyMap()))
                            .thenReturn(1);
        } else {
            CommandLine unprivilegedCommandLine = new CommandLine("sudo").addArgument("-u").addArgument("kura")
                    .addArgument("-s").addArgument("MYVAR=MYVALUE").addArgument(TIMEOUT).addArgument("-s")
                    .addArgument(SIGTERM).addArgument("10");
            unprivilegedCommandLine.addArgument("sh").addArgument("-c").addArgument("env", false);
            when(mockExecutor.execute(
                    AdditionalMatchers.not(Matchers.argThat(new CommandLineMatcher(unprivilegedCommandLine))),
                    anyMap())).thenReturn(1);
        }

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        Command command = new Command(new String[] { "env" });
        command.setTimeout(10);
        command.setDirectory(TMP);
        Map<String, String> env = new HashMap<>();
        env.put("MYVAR", "MYVALUE");
        command.setEnvironment(env);
        CommandStatus status = service.execute(command);
        assertTrue(status.getExitStatus().isSuccessful());
    }

    @Test
    public void executeSyncCommandWithTimeout() throws IOException {
        System.out.println(service.getClass().getSimpleName() + ": Test synchronous command execution with timeout...");

        Executor mockExecutor = mock(DefaultExecutor.class);
        ExecuteWatchdog mockExecuteWatchdog = mock(ExecuteWatchdog.class);
        when(mockExecutor.getWatchdog()).thenReturn(mockExecuteWatchdog);
        when(mockExecuteWatchdog.killedProcess()).thenReturn(true);

        if (service instanceof PrivilegedExecutorServiceImpl) {
            CommandLine privilegedCommandLine = new CommandLine("sleep").addArgument("20");
            when(mockExecutor.execute(Matchers.argThat(new CommandLineMatcher(privilegedCommandLine)))).thenReturn(1);
        } else {
            CommandLine unprivilegedCommandLine = new CommandLine("sudo").addArgument("-u").addArgument("kura")
                    .addArgument("-s").addArgument(TIMEOUT).addArgument("-s").addArgument(SIGTERM).addArgument("10");
            unprivilegedCommandLine.addArgument("sh").addArgument("-c").addArgument("sleep 20", false);
            when(mockExecutor.execute(Matchers.argThat(new CommandLineMatcher(unprivilegedCommandLine)))).thenReturn(1);
        }

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        Command command = new Command(new String[] { "sleep", "20" });
        command.setTimeout(10);
        command.setDirectory(TMP);
        CommandStatus status = service.execute(command);
        assertFalse(status.getExitStatus().isSuccessful());
        assertTrue(status.isTimedout());
    }

    @Test
    public void executeSyncCommandInAShell() throws IOException {
        System.out.println(service.getClass().getSimpleName() + ": Test synchronous shell command execution...");

        Executor mockExecutor = mock(DefaultExecutor.class);
        when(mockExecutor.getWatchdog()).thenReturn(new ExecuteWatchdog(10));

        if (service instanceof PrivilegedExecutorServiceImpl) {
            CommandLine privilegedCommandLine = new CommandLine("/bin/sh").addArgument("-c").addArgument(LIST_COMMAND,
                    false);
            when(mockExecutor
                    .execute(AdditionalMatchers.not(Matchers.argThat(new CommandLineMatcher(privilegedCommandLine)))))
                            .thenReturn(1);
        } else {
            CommandLine unprivilegedCommandLine = new CommandLine("sudo").addArgument("-u").addArgument("kura")
                    .addArgument("-s").addArgument(TIMEOUT).addArgument("-s").addArgument(SIGTERM).addArgument("10");
            unprivilegedCommandLine.addArgument("sh").addArgument("-c").addArgument(LIST_COMMAND, false);
            when(mockExecutor
                    .execute(AdditionalMatchers.not(Matchers.argThat(new CommandLineMatcher(unprivilegedCommandLine)))))
                            .thenReturn(1);
        }

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        Command command = new Command(new String[] { "ls", "-all" });
        command.setTimeout(10);
        command.setDirectory(TMP);
        command.setExecuteInAShell(true);
        CommandStatus status = service.execute(command);
        assertTrue(status.getExitStatus().isSuccessful());
    }

    @Test
    public void executeSyncCommandError() {
        System.out.println(service.getClass().getSimpleName() + ": Test synchronous command execution error...");
        Command command = new Command(new String[] {});
        command.setTimeout(10);
        command.setDirectory(TMP);
        CommandStatus status = service.execute(command);
        assertFalse(status.getExitStatus().isSuccessful());
        assertTrue(status.getExitStatus().getExitCode() == 1);
    }

    @Test
    public void executeAsyncCommand() throws IOException {
        System.out.println(service.getClass().getSimpleName() + ": Test asynchronous command execution...");
        CountDownLatch lock = new CountDownLatch(1);
        AtomicInteger exitStatus = new AtomicInteger(1);
        Consumer<CommandStatus> callback = status -> {
            exitStatus.set(status.getExitStatus().getExitCode());
            lock.countDown();
        };

        Executor mockExecutor = mock(DefaultExecutor.class);
        when(mockExecutor.getWatchdog()).thenReturn(new ExecuteWatchdog(10));

        if (service instanceof PrivilegedExecutorServiceImpl) {
            CommandLine privilegedCommandLine = new CommandLine("ls").addArgument("-all");
            doAnswer((Answer<InvocationOnMock>) invocation -> {
                callback.accept(new CommandStatus(new LinuxExitStatus(0)));
                return null;
            }).when(mockExecutor).execute(Matchers.argThat(new CommandLineMatcher(privilegedCommandLine)),
                    isA(LinuxResultHandler.class));
        } else {
            CommandLine unprivilegedCommandLine = new CommandLine("sudo").addArgument("-u").addArgument("kura")
                    .addArgument("-s").addArgument(TIMEOUT).addArgument("-s").addArgument(SIGTERM).addArgument("10");
            unprivilegedCommandLine.addArgument("sh").addArgument("-c").addArgument(LIST_COMMAND, false);
            doAnswer((Answer<InvocationOnMock>) invocation -> {
                callback.accept(new CommandStatus(new LinuxExitStatus(0)));
                return null;
            }).when(mockExecutor).execute(Matchers.argThat(new CommandLineMatcher(unprivilegedCommandLine)),
                    isA(LinuxResultHandler.class));
        }

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        Command command = new Command(new String[] { "ls", "-all" });
        command.setTimeout(10);
        command.setDirectory(TMP);
        service.execute(command, callback);

        boolean result = false;
        try {
            result = lock.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(result);
        assertTrue(exitStatus.get() == 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void executeAsyncCommandWithVariables() throws IOException {
        System.out.println(
                service.getClass().getSimpleName() + ": Test asynchronous command execution with variables...");
        CountDownLatch lock = new CountDownLatch(1);
        AtomicInteger exitStatus = new AtomicInteger(1);
        Consumer<CommandStatus> callback = status -> {
            exitStatus.set(status.getExitStatus().getExitCode());
            lock.countDown();
        };

        Executor mockExecutor = mock(DefaultExecutor.class);
        when(mockExecutor.getWatchdog()).thenReturn(new ExecuteWatchdog(10));

        if (service instanceof PrivilegedExecutorServiceImpl) {
            CommandLine privilegedCommandLine = new CommandLine("env");
            doAnswer((Answer<InvocationOnMock>) invocation -> {
                callback.accept(new CommandStatus(new LinuxExitStatus(0)));
                return null;
            }).when(mockExecutor).execute(Matchers.argThat(new CommandLineMatcher(privilegedCommandLine)), anyMap(),
                    isA(LinuxResultHandler.class));
        } else {
            CommandLine unprivilegedCommandLine = new CommandLine("sudo").addArgument("-u").addArgument("kura")
                    .addArgument("-s").addArgument("MYVAR=MYVALUE").addArgument(TIMEOUT).addArgument("-s")
                    .addArgument(SIGTERM).addArgument("10");
            unprivilegedCommandLine.addArgument("sh").addArgument("-c").addArgument("env", false);
            doAnswer((Answer<InvocationOnMock>) invocation -> {
                callback.accept(new CommandStatus(new LinuxExitStatus(0)));
                return null;
            }).when(mockExecutor).execute(Matchers.argThat(new CommandLineMatcher(unprivilegedCommandLine)), anyMap(),
                    isA(LinuxResultHandler.class));
        }

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        Command command = new Command(new String[] { "env" });
        command.setTimeout(10);
        command.setDirectory(TMP);
        Map<String, String> env = new HashMap<>();
        env.put("MYVAR", "MYVALUE");
        command.setEnvironment(env);
        service.execute(command, callback);

        boolean result = false;
        try {
            result = lock.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(result);
        assertTrue(exitStatus.get() == 0);
    }

    @Test
    public void executeAsyncCommandWithTimeout() throws IOException {
        System.out
                .println(service.getClass().getSimpleName() + ": Test asynchronous command execution with timeout...");
        CountDownLatch lock = new CountDownLatch(1);
        AtomicInteger exitStatus = new AtomicInteger(1);
        AtomicBoolean isTimedout = new AtomicBoolean(false);
        Consumer<CommandStatus> callback = status -> {
            exitStatus.set(status.getExitStatus().getExitCode());
            isTimedout.set(status.isTimedout());
            lock.countDown();
        };

        Executor mockExecutor = mock(DefaultExecutor.class);
        when(mockExecutor.getWatchdog()).thenReturn(new ExecuteWatchdog(10));

        if (service instanceof PrivilegedExecutorServiceImpl) {
            CommandLine privilegedCommandLine = new CommandLine("sleep").addArgument("30");
            doAnswer((Answer<InvocationOnMock>) invocation -> {
                CommandStatus status = new CommandStatus(new LinuxExitStatus(143));
                status.setTimedout(true);
                callback.accept(status);
                return null;
            }).when(mockExecutor).execute(Matchers.argThat(new CommandLineMatcher(privilegedCommandLine)),
                    isA(LinuxResultHandler.class));
        } else {
            CommandLine unprivilegedCommandLine = new CommandLine("sudo").addArgument("-u").addArgument("kura")
                    .addArgument("-s").addArgument(TIMEOUT).addArgument("-s").addArgument(SIGTERM).addArgument("10");
            unprivilegedCommandLine.addArgument("sh").addArgument("-c").addArgument("sleep 30", false);
            doAnswer((Answer<InvocationOnMock>) invocation -> {
                CommandStatus status = new CommandStatus(new LinuxExitStatus(124));
                status.setTimedout(true);
                callback.accept(status);
                return null;
            }).when(mockExecutor).execute(Matchers.argThat(new CommandLineMatcher(unprivilegedCommandLine)),
                    isA(LinuxResultHandler.class));
        }

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        Command command = new Command(new String[] { "sleep", "30" });
        command.setTimeout(10);
        command.setDirectory(TMP);
        service.execute(command, callback);

        boolean result = false;
        try {
            result = lock.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(result);
        assertTrue(exitStatus.get() == 143 || exitStatus.get() == 124);
        assertTrue(isTimedout.get());
    }

    @Test
    public void isRunningPid() throws IOException {
        System.out.println(service.getClass().getSimpleName() + ": Test isRunning with pid...");

        Executor mockExecutor = mock(DefaultExecutor.class);
        when(mockExecutor.getWatchdog()).thenReturn(new ExecuteWatchdog(10));

        CommandLine commandLine = CommandLine.parse("ps -p 100");
        when(mockExecutor.execute(AdditionalMatchers.not(Matchers.argThat(new CommandLineMatcher(commandLine)))))
                .thenReturn(1);

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        // Check if the command is correct. It returns false because the outputstream is empty.
        assertFalse(service.isRunning(new LinuxPid(100)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isRunningCommand() throws IOException {
        System.out.println(service.getClass().getSimpleName() + ": Test get command pids...");

        Executor mockExecutor = mock(DefaultExecutor.class);
        when(mockExecutor.getWatchdog()).thenReturn(new ExecuteWatchdog(10));

        CommandLine commandLine = new CommandLine("ps").addArgument("-ax");
        when(mockExecutor.execute((Matchers.argThat(new CommandLineMatcher(commandLine)))))
                .thenThrow(new IllegalArgumentException());

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        service.isRunning(new String[] { "ls" });
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPids() throws IOException {
        System.out.println(service.getClass().getSimpleName() + ": Test get command pids...");

        Executor mockExecutor = mock(DefaultExecutor.class);
        when(mockExecutor.getWatchdog()).thenReturn(new ExecuteWatchdog(10));

        CommandLine commandLine = new CommandLine("ps").addArgument("-ax");
        when(mockExecutor.execute((Matchers.argThat(new CommandLineMatcher(commandLine)))))
                .thenThrow(new IllegalArgumentException());

        ExecutorFactory mockExecutorFactory = () -> mockExecutor;
        service.setExecutorFactory(mockExecutorFactory);

        service.getPids(new String[] { "ls" });
    }

    private class CommandLineMatcher extends BaseMatcher<CommandLine> {

        private CommandLine commandLine;

        public CommandLineMatcher(CommandLine commandLine) {
            this.commandLine = commandLine;
        }

        @Override
        public boolean matches(Object arg0) {
            if (arg0 instanceof CommandLine) {
                return this.commandLine.toString().equals(((CommandLine) arg0).toString());
            } else {
                return false;
            }
        }

        @Override
        public void describeTo(Description arg0) {
            // Not needed
        }

    }

}
