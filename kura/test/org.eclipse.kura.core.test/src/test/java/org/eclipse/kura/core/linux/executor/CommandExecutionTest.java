/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.linux.executor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.core.internal.linux.executor.ExecutorUtil;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.linux.executor.privileged.PrivilegedExecutorServiceImpl;
import org.eclipse.kura.core.linux.executor.unprivileged.UnprivilegedExecutorServiceImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CommandExecutionTest {

    // private static final String TMP = "/tmp";
    private static CommandExecutorService executor;

    public CommandExecutionTest(CommandExecutorService executor) {
        CommandExecutionTest.executor = executor;
    }

    //
    // // Don't run tests on MAC OS X
    // @ClassRule
    // public static final AssumingIsNotMac assumingIsNotMac = new AssumingIsNotMac();
    //
    // // Don't run tests on Jenkins
    // @ClassRule
    // public static final AssumingIsNotJenkins assumingIsNotJenkins = new AssumingIsNotJenkins();
    //
    @Parameterized.Parameters
    public static Collection<CommandExecutorService> getExecutors() {
        return Arrays.asList(new UnprivilegedExecutorServiceImpl(), new PrivilegedExecutorServiceImpl());
    }
    //
    // @BeforeClass
    // public static void setup() {
    // PrivilegedExecutorServiceImpl e = new PrivilegedExecutorServiceImpl();
    // ExecutorUtil util = new ExecutorUtil() {
    // @Override
    // public DefaultExecutor configureExecutor(Command command) {
    // DefaultExecutor executor = new DefaultExecutor();
    // int timeout = command.getTimeout();
    // ExecuteWatchdog watchdog = timeout <= 0 ? new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT)
    // : new ExecuteWatchdog(timeout * 1000L);
    // executor.setWatchdog(watchdog);
    //
    // OutputStream out = command.getOutputStream();
    // OutputStream err = command.getErrorStream();
    // InputStream in = command.getInputStream();
    // FlushPumpStreamHandler handler;
    // if (out != null && err != null) {
    // handler = new FlushPumpStreamHandler(out, err, in);
    // } else if (out != null) {
    // handler = new FlushPumpStreamHandler(out, new NullOutputStream(), in);
    // } else if (err != null) {
    // handler = new FlushPumpStreamHandler(new NullOutputStream(), err, in);
    // } else {
    // handler = new FlushPumpStreamHandler(new NullOutputStream(), new NullOutputStream(), in);
    // }
    // executor.setStreamHandler(handler);
    //
    // String directory = command.getDirectory();
    //
    // File workingDir = directory == null || directory.isEmpty() || !Files.isDirectory(Paths.get(directory))
    // ? TEMP_DIR
    // : new File(directory);
    // executor.setWorkingDirectory(workingDir);
    // return executor;
    // }
    //
    // }
    // TestUtil.setFieldValue(e, TMP, e);
    //
    //
    //
    // System.out.println("Check if kura user exists...");
    // CommandLine commandLine = new CommandLine("id");
    // commandLine.addArgument("-u");
    // commandLine.addArgument("kura");
    // DefaultExecutor executor = new DefaultExecutor();
    // ExecuteWatchdog watchdog = new ExecuteWatchdog(60000L);
    // executor.setWatchdog(watchdog);
    //
    // final ByteArrayOutputStream out = new ByteArrayOutputStream();
    // final ByteArrayOutputStream err = new ByteArrayOutputStream();
    // final PumpStreamHandler handler = new PumpStreamHandler(out, err);
    //
    // executor.setStreamHandler(handler);
    // executor.setWorkingDirectory(new File(TMP));
    //
    // int exitStatus = 1;
    // try {
    // exitStatus = executor.execute(commandLine);
    // } catch (IOException e) {
    // // Do nothing...
    // }
    //
    // if (exitStatus == 0) {
    // System.out.println("kura user exists");
    // } else {
    // System.out.println("kura user doesn't exists: let's try to create it...");
    // exitStatus = 1;
    // commandLine = new CommandLine("useradd");
    // commandLine.addArgument("kura");
    // try {
    // exitStatus = executor.execute(commandLine);
    // } catch (IOException e) {
    // System.out.println("Failed to create kura user " + e.getMessage());
    // }
    // if (exitStatus == 0) {
    // System.out.println("kura user created.");
    // } else {
    // System.out.println("kura user creation failed: some tests will fail");
    // }
    // }
    // }
    //
    // @AfterClass
    // public static void clean() {
    // System.out.println("Delete kura user...");
    // CommandLine commandLine = new CommandLine("userdel");
    // commandLine.addArgument("kura");
    // DefaultExecutor executor = new DefaultExecutor();
    // ExecuteWatchdog watchdog = new ExecuteWatchdog(60000L);
    // executor.setWatchdog(watchdog);
    //
    // final ByteArrayOutputStream out = new ByteArrayOutputStream();
    // final ByteArrayOutputStream err = new ByteArrayOutputStream();
    // final PumpStreamHandler handler = new PumpStreamHandler(out, err);
    //
    // executor.setStreamHandler(handler);
    // executor.setWorkingDirectory(new File(TMP));
    //
    // int exitStatus = 1;
    // try {
    // exitStatus = executor.execute(commandLine);
    // } catch (IOException e) {
    // // Do nothing...
    // }
    //
    // if (exitStatus == 0) {
    // System.out.println("kura user deleted.");
    // } else {
    // System.out.println("kura user not deleted.");
    // }
    // }

    // UNPRIVILEGED!!!!!!

    // private PrivilegedExecutorServiceImpl executor;
    private Command command;
    private CommandStatus status;
    private Consumer<CommandStatus> thenAcceptError = cs -> {
        this.status = cs;
        thenReturnExitValue(1);
        thenReturnErrorMessage("The commandLine cannot be empty or not defined");
    };
    private Consumer<CommandStatus> thenAcceptSuccess = cs -> {
        this.status = cs;
        thenReturnExitValue(0);
    };

    @Test
    public void shouldReturnErrorOnEmptyCommand() {
        givenCommand(" ");
        givenCommandExecutor();

        whenCommandSyncExecuted();

        thenReturnExitValue(1);
        thenReturnErrorMessage("The commandLine cannot be empty or not defined");
    }

    @Test
    public void shouldReturnSuccess() {
        givenCommand("ls -all");
        givenCommandExecutor();

        whenCommandSyncExecuted();

        thenReturnExitValue(0);
    }

    @Test
    public void shouldAcceptErrorOnEmptyCommand() {
        givenCommand(" ");
        givenCommandExecutor();

        whenCommandAsyncExecuted(this.thenAcceptError);

    }

    @Test
    public void shouldAcceptSuccess() {
        givenCommand("ls -all");
        givenCommandExecutor();

        whenCommandAsyncExecuted(this.thenAcceptSuccess);
    }

    private void givenCommandExecutor() {
        // this.executor = new PrivilegedExecutorServiceImpl();
        ExecutorUtil euMock = mock(ExecutorUtil.class);
        CommandStatus cs = new CommandStatus(this.command, new LinuxExitStatus(0));
        when(euMock.executePrivileged(this.command)).thenReturn(cs);
        when(euMock.executeUnprivileged(this.command)).thenReturn(cs);
        try {
            TestUtil.setFieldValue(executor, "executorUtil", euMock);
        } catch (NoSuchFieldException e) {
            // Do nothing...
        }
    }

    private void givenCommand(String commandLine) {
        this.command = new Command(commandLine.split(" "));
        this.command.setOutputStream(new ByteArrayOutputStream());
        this.command.setErrorStream(new ByteArrayOutputStream());
    }

    private void whenCommandSyncExecuted() {
        this.status = executor.execute(this.command);
    }

    private void whenCommandAsyncExecuted(Consumer<CommandStatus> callback) {
        executor.execute(this.command, callback);
    }

    private void thenReturnExitValue(int expectedExitValue) {
        assertEquals(expectedExitValue, this.status.getExitStatus().getExitCode());
    }

    private void thenReturnErrorMessage(String expectedErrorMessage) {
        assertEquals(expectedErrorMessage,
                new String(((ByteArrayOutputStream) this.status.getErrorStream()).toByteArray(), Charsets.UTF_8));
    }

    // @Test
    // public void executeSyncCommand() {
    // System.out.println(service.getClass().getSimpleName() + ": Test synchronous command execution...");
    // Command command = new Command(new String[] { "ls", "-all" });
    // command.setTimeout(10);
    // command.setDirectory(TMP);
    // CommandStatus status = service.execute(command);
    // assertTrue(status.getExitStatus().isSuccessful());
    // }
    //
    // @Test
    // public void executeSyncCommandWithFolder() {
    // System.out.println(service.getClass().getSimpleName() + ": Test synchronous command execution with folder...");
    // Command command = new Command(new String[] { "pwd" });
    // command.setTimeout(10);
    // command.setDirectory(TMP);
    // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // command.setOutputStream(outputStream);
    // CommandStatus status = service.execute(command);
    // assertTrue(status.getExitStatus().isSuccessful());
    // assertEquals(TMP + "\n", new String(outputStream.toByteArray(), Charsets.UTF_8));
    // }
    //
    // @Test
    // public void executeSyncCommandWithVariables() {
    // System.out
    // .println(service.getClass().getSimpleName() + ": Test synchronous command execution with variables...");
    // Command command = new Command(new String[] { "env" });
    // command.setTimeout(10);
    // command.setDirectory(TMP);
    // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // command.setOutputStream(outputStream);
    // Map<String, String> env = new HashMap<String, String>();
    // env.put("MYVAR", "MYVALUE");
    // command.setEnvironment(env);
    // CommandStatus status = service.execute(command);
    // assertTrue(status.getExitStatus().isSuccessful());
    // assertTrue(new String(outputStream.toByteArray(), Charsets.UTF_8).contains("MYVAR=MYVALUE"));
    // }
    //
    // @Test
    // public void executeSyncCommandWithTimeout() {
    // System.out.println(service.getClass().getSimpleName() + ": Test synchronous command execution with timeout...");
    // Command command = new Command(new String[] { "sleep", "20" });
    // command.setTimeout(10);
    // command.setDirectory(TMP);
    // long startTime = System.currentTimeMillis();
    // CommandStatus status = service.execute(command);
    // long stopTime = System.currentTimeMillis();
    // assertFalse(status.getExitStatus().isSuccessful());
    // assertTrue(status.isTimedout());
    // assertTrue(stopTime - startTime < 20000);
    // }
    //
    // @Test
    // public void executeAsyncCommand() {
    // System.out.println(service.getClass().getSimpleName() + ": Test asynchronous command execution...");
    // CountDownLatch lock = new CountDownLatch(1);
    // AtomicInteger exitStatus = new AtomicInteger(1);
    // Consumer<CommandStatus> callback = status -> {
    // exitStatus.set(status.getExitStatus().getExitCode());
    // lock.countDown();
    // };
    //
    // Command command = new Command(new String[] { "ls", "-all" });
    // command.setTimeout(10);
    // command.setDirectory(TMP);
    // service.execute(command, callback);
    //
    // boolean result = false;
    // try {
    // result = lock.await(20, TimeUnit.SECONDS);
    // } catch (InterruptedException e) {
    // Thread.currentThread().interrupt();
    // }
    //
    // assertTrue(result);
    // assertTrue(exitStatus.get() == 0);
    // }
    //
    // @Test
    // public void executeAsyncCommandWithFolder() {
    // System.out.println(service.getClass().getSimpleName() + ": Test asynchronous command execution with folder...");
    // CountDownLatch lock = new CountDownLatch(1);
    // AtomicInteger exitStatus = new AtomicInteger(1);
    // AtomicReference<String> stdout = new AtomicReference<String>("");
    // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // Consumer<CommandStatus> callback = status -> {
    // exitStatus.set(status.getExitStatus().getExitCode());
    // stdout.set(new String(outputStream.toByteArray(), Charsets.UTF_8));
    // lock.countDown();
    // };
    //
    // Command command = new Command(new String[] { "pwd" });
    // command.setTimeout(10);
    // command.setDirectory(TMP);
    // command.setOutputStream(outputStream);
    // service.execute(command, callback);
    //
    // boolean result = false;
    // try {
    // result = lock.await(20, TimeUnit.SECONDS);
    // } catch (InterruptedException e) {
    // Thread.currentThread().interrupt();
    // }
    //
    // assertTrue(result);
    // assertTrue(exitStatus.get() == 0);
    // assertEquals(TMP + "\n", stdout.get());
    // }
    //
    // @Test
    // public void executeAsyncCommandWithVariables() {
    // System.out.println(
    // service.getClass().getSimpleName() + ": Test asynchronous command execution with variables...");
    // CountDownLatch lock = new CountDownLatch(1);
    // AtomicInteger exitStatus = new AtomicInteger(1);
    // AtomicReference<String> stdout = new AtomicReference<String>("");
    // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // Consumer<CommandStatus> callback = status -> {
    // exitStatus.set(status.getExitStatus().getExitCode());
    // stdout.set(new String(outputStream.toByteArray(), Charsets.UTF_8));
    // lock.countDown();
    // };
    //
    // Command command = new Command(new String[] { "env" });
    // command.setTimeout(10);
    // command.setDirectory(TMP);
    // command.setOutputStream(outputStream);
    // Map<String, String> env = new HashMap<String, String>();
    // env.put("MYVAR", "MYVALUE");
    // command.setEnvironment(env);
    // service.execute(command, callback);
    //
    // boolean result = false;
    // try {
    // result = lock.await(20, TimeUnit.SECONDS);
    // } catch (InterruptedException e) {
    // Thread.currentThread().interrupt();
    // }
    //
    // assertTrue(result);
    // assertTrue(exitStatus.get() == 0);
    // assertTrue(stdout.get().contains("MYVAR=MYVALUE"));
    // }
    //
    // @Test
    // public void executeAsyncCommandWithTimeout() {
    // System.out
    // .println(service.getClass().getSimpleName() + ": Test asynchronous command execution with timeout...");
    // CountDownLatch lock = new CountDownLatch(1);
    // AtomicInteger exitStatus = new AtomicInteger(1);
    // AtomicBoolean isTimedout = new AtomicBoolean(false);
    // Consumer<CommandStatus> callback = status -> {
    // exitStatus.set(status.getExitStatus().getExitCode());
    // isTimedout.set(status.isTimedout());
    // lock.countDown();
    // };
    //
    // Command command = new Command(new String[] { "sleep", "30" });
    // command.setTimeout(10);
    // command.setDirectory(TMP);
    // long startTime = System.currentTimeMillis();
    // service.execute(command, callback);
    //
    // boolean result = false;
    // try {
    // result = lock.await(20, TimeUnit.SECONDS);
    // } catch (InterruptedException e) {
    // Thread.currentThread().interrupt();
    // }
    // long stopTime = System.currentTimeMillis();
    //
    // assertTrue(result);
    // assertFalse(exitStatus.get() == 0);
    // assertTrue(isTimedout.get());
    // assertTrue(stopTime - startTime < 20000);
    // }
    //
    // @Test
    // public void stopCommand() throws InterruptedException {
    // System.out.println(service.getClass().getSimpleName() + ": Test stop command execution...");
    // stopCommandInternal(null);
    // }
    //
    // @Test
    // public void stopCommandForce() throws InterruptedException {
    // System.out.println(service.getClass().getSimpleName() + ": Test force stop command execution...");
    // stopCommandInternal(LinuxSignal.SIGKILL);
    // }
    //
    // private void stopCommandInternal(Signal signal) throws InterruptedException {
    //
    // Consumer<CommandStatus> callback = status -> {
    // // Do nothing...
    // };
    // Command command = new Command(new String[] { "sleep", "40" });
    // command.setTimeout(60);
    // command.setDirectory(TMP);
    // command.setSignal(signal);
    // service.execute(command, callback);
    //
    // TimeUnit.SECONDS.sleep(2);
    // Map<String, Pid> pids = service.getPids(new String[] { "sleep", "40" });
    // assertFalse(pids.isEmpty());
    // for (Pid pid : pids.values()) {
    // service.stop(pid, signal);
    // }
    //
    // pids = service.getPids(new String[] { "sleep", "40" });
    // assertTrue(pids.isEmpty());
    // }
    //
    // @Test
    // public void killCommandWithDefaultSignal() throws InterruptedException {
    // System.out.println(service.getClass().getSimpleName() + ": Test kill command execution...");
    // killCommandInternal(null);
    // }
    //
    // @Test
    // public void killCommandWithSignal() throws InterruptedException {
    // System.out.println(service.getClass().getSimpleName() + ": Test force kill command execution...");
    // killCommandInternal(LinuxSignal.SIGKILL);
    // }
    //
    // private void killCommandInternal(Signal signal) throws InterruptedException {
    //
    // Consumer<CommandStatus> callback = status -> {
    // // Do nothing...
    // };
    // Command command = new Command(new String[] { "sleep", "50" });
    // command.setTimeout(10);
    // command.setDirectory(TMP);
    // command.setSignal(signal);
    // service.execute(command, callback);
    //
    // TimeUnit.SECONDS.sleep(2);
    // Map<String, Pid> pids = service.getPids(new String[] { "sleep", "50" });
    // assertFalse(pids.isEmpty());
    // service.kill(new String[] { "sleep", "50" }, signal);
    // pids = service.getPids(new String[] { "sleep", "50" });
    // assertTrue(pids.isEmpty());
    // }
    //
    // @Test
    // public void isRunningPid() throws InterruptedException {
    // System.out.println(service.getClass().getSimpleName() + ": Test isRunning with pid...");
    // Consumer<CommandStatus> callback = status -> {
    // // Do nothing...
    // };
    // Command command = new Command(new String[] { "sleep", "60" });
    // command.setTimeout(30);
    // command.setDirectory(TMP);
    // service.execute(command, callback);
    //
    // TimeUnit.SECONDS.sleep(2);
    // Map<String, Pid> pids = service.getPids(new String[] { "sleep", "60" });
    // assertFalse(pids.isEmpty());
    // for (Pid pid : pids.values()) {
    // assertTrue(service.isRunning(pid));
    // }
    // for (Pid pid : pids.values()) {
    // service.stop(pid, LinuxSignal.SIGKILL);
    // }
    // }
    //
    // @Test
    // public void isRunningCommand() throws InterruptedException {
    // System.out.println(service.getClass().getSimpleName() + ": Test isRunning with command...");
    // Consumer<CommandStatus> callback = status -> {
    // // Do nothing...
    // };
    // Command command = new Command(new String[] { "sleep", "70" });
    // command.setTimeout(30);
    // command.setDirectory(TMP);
    // service.execute(command, callback);
    //
    // TimeUnit.SECONDS.sleep(2);
    // Map<String, Pid> pids = service.getPids(new String[] { "sleep", "70" });
    // assertFalse(pids.isEmpty());
    // assertTrue(service.isRunning(new String[] { "sleep", "70" }));
    // for (Pid pid : pids.values()) {
    // service.stop(pid, LinuxSignal.SIGKILL);
    // }
    // }
    //
    // @Test
    // public void getPids() throws InterruptedException {
    // System.out.println(service.getClass().getSimpleName() + ": Test get command pids...");
    // Consumer<CommandStatus> callback = status -> {
    // // Do nothing...
    // };
    // Command command = new Command(new String[] { "sleep", "80" });
    // command.setTimeout(30);
    // command.setDirectory(TMP);
    // service.execute(command, callback);
    //
    // TimeUnit.SECONDS.sleep(2);
    // Map<String, Pid> pids = service.getPids(new String[] { "sleep", "80" });
    // // Check that the running processes are 4 (unprivileged service) or 1 (privileged service)
    // assertTrue(pids.size() == 1 || pids.size() == 4);
    // }

}
