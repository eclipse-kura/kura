/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.internal.linux.executor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.linux.executor.LinuxPid;
import org.eclipse.kura.core.linux.executor.LinuxResultHandler;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorUtil {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorUtil.class);
    private static final String COMMAND_MESSAGE = "Command ";
    private static final String FAILED_TO_GET_PID_MESSAGE = "Failed to get pid for command '{}'";
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final String DEFAULT_COMMAND_USERNAME = "kura";

    private String commandUsername;

    public ExecutorUtil() {
        this.commandUsername = DEFAULT_COMMAND_USERNAME;
    }

    public ExecutorUtil(String commandUsername) {
        this.commandUsername = commandUsername;
    }

    public String getCommandUsername() {
        return commandUsername;
    }

    public void setCommandUsername(String commandUsername) {
        this.commandUsername = commandUsername;
    }

    public CommandStatus executeUnprivileged(Command command) {
        CommandLine commandLine = buildUnprivilegedCommand(command);
        return executeSync(command, commandLine);
    }

    public void executeUnprivileged(Command command, Consumer<CommandStatus> callback) {
        CommandLine commandLine = buildUnprivilegedCommand(command);
        executeAsync(command, commandLine, callback);
    }

    public CommandStatus executePrivileged(Command command) {
        CommandLine commandLine = buildPrivilegedCommand(command);
        return executeSync(command, commandLine);
    }

    public void executePrivileged(Command command, Consumer<CommandStatus> callback) {
        CommandLine commandLine = buildPrivilegedCommand(command);
        executeAsync(command, commandLine, callback);
    }

    public boolean stopUnprivileged(Pid pid, Signal signal) {
        boolean isStopped = true;
        if (isRunning(pid)) {
            Command killCommand = new Command(buildKillCommand(pid, signal));
            killCommand.setTimeout(60);
            killCommand.setSignal(signal);
            CommandStatus commandStatus = executeUnprivileged(killCommand);
            isStopped = commandStatus.getExitStatus().isSuccessful();
        }
        return isStopped;
    }

    public boolean killUnprivileged(String[] commandLine, Signal signal) {
        boolean isKilled = true;
        Map<String, Pid> pids = getPids(commandLine);
        for (Pid pid : pids.values()) {
            isKilled &= stopUnprivileged(pid, signal);
        }
        return isKilled;
    }

    public boolean stopPrivileged(Pid pid, Signal signal) {
        boolean isStopped = true;
        if (isRunning(pid)) {
            Command killCommand = new Command(buildKillCommand(pid, signal));
            killCommand.setTimeout(60);
            killCommand.setSignal(signal);
            CommandStatus commandStatus = executePrivileged(killCommand);
            isStopped = commandStatus.getExitStatus().isSuccessful();
        }
        return isStopped;
    }

    public boolean killPrivileged(String[] commandLine, Signal signal) {
        boolean isKilled = true;
        Map<String, Pid> pids = getPids(commandLine);
        for (Pid pid : pids.values()) {
            isKilled &= stopPrivileged(pid, signal);
        }
        return isKilled;
    }

    public boolean isRunning(Pid pid) {
        boolean isRunning = false;
        String pidString = ((Integer) pid.getPid()).toString();
        String psCommand = "ps -p " + pidString;
        CommandLine commandLine = CommandLine.parse(psCommand);
        Executor executor = getExecutor();

        final ByteArrayOutputStream out = createStream();
        final ByteArrayOutputStream err = createStream();
        final PumpStreamHandler handler = new PumpStreamHandler(out, err);

        executor.setStreamHandler(handler);
        executor.setWorkingDirectory(TEMP_DIR);
        executor.setExitValue(0);
        int exitValue;
        try {
            exitValue = executor.execute(commandLine);
            if (exitValue == 0 && new String(out.toByteArray(), UTF_8).contains(pidString)) {
                isRunning = true;
            }
        } catch (IOException e) {
            logger.warn("Failed to check if process with pid {} is running", pidString);
        }
        return isRunning;
    }

    public boolean isRunning(String[] commandLine) {
        return !getPids(commandLine).isEmpty();
    }

    public Map<String, Pid> getPids(String[] commandLine) {
        Map<String, Pid> pids = new HashMap<>();
        CommandLine psCommandLine = new CommandLine("ps");
        psCommandLine.addArgument("-ax");
        Executor executor = getExecutor();

        final ByteArrayOutputStream out = createStream();
        final ByteArrayOutputStream err = createStream();
        final PumpStreamHandler handler = new PumpStreamHandler(out, err);

        executor.setStreamHandler(handler);
        executor.setWorkingDirectory(TEMP_DIR);
        executor.setExitValue(0);
        int exitValue = 1;
        try {
            exitValue = executor.execute(psCommandLine);
        } catch (IOException e) {
            logger.debug(FAILED_TO_GET_PID_MESSAGE, commandLine, e);
        }
        if (exitValue == 0) {
            pids = parsePids(out, commandLine);
        }
        return pids;
    }

    public static void stopStreamHandler(Executor executor) {
        try {
            if (executor.getStreamHandler() != null) {
                executor.getStreamHandler().stop();
            }
        } catch (IOException e) {
            logger.warn("Failed to stop stream handlers", e);
        }
    }

    private Map<String, Pid> parsePids(ByteArrayOutputStream out, String[] commandLine) {
        Map<String, Integer> pids = new HashMap<>();
        String pid;
        String[] output = new String(out.toByteArray(), UTF_8).split("\n");
        for (String line : output) {
            StringTokenizer st = new StringTokenizer(line);
            pid = st.nextToken();
            st.nextElement();
            st.nextElement();
            st.nextElement();

            // get the remainder of the line showing the command that was issued
            line = line.substring(line.indexOf(st.nextToken()));
            if (checkLine(line, commandLine)) {
                pids.put(line, Integer.parseInt(pid));
            }
        }
        // Sort pids in reverse order (useful when stop processes...)
        return pids.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new LinuxPid(e.getValue()), (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    private boolean checkLine(String line, String[] tokens) {
        return Arrays.stream(tokens).parallel().allMatch(line::contains);
    }

    private CommandStatus executeSync(Command command, CommandLine commandLine) {
        CommandStatus commandStatus = new CommandStatus(command, new LinuxExitStatus(0));
        commandStatus.setOutputStream(command.getOutputStream());
        commandStatus.setErrorStream(command.getErrorStream());
        commandStatus.setInputStream(command.getInputStream());

        Executor executor = configureExecutor(command);

        int exitStatus = 0;
        logger.debug("Executing: {}", commandLine);
        try {
            Map<String, String> environment = command.getEnvironment();
            if (environment != null && !environment.isEmpty()) {
                Map<String, String> currentEnv = EnvironmentUtils.getProcEnvironment();
                currentEnv.putAll(environment);
                exitStatus = executor.execute(commandLine, currentEnv);
            } else {
                exitStatus = executor.execute(commandLine);
            }
        } catch (ExecuteException e) {
            exitStatus = e.getExitValue();
            logger.debug(COMMAND_MESSAGE + " {} returned error code {}", commandLine, exitStatus, e);
        } catch (IOException e) {
            exitStatus = 1;
            logger.error(COMMAND_MESSAGE + " {} failed", commandLine, e);
        } finally {
            stopStreamHandler(executor);
            commandStatus.setExitStatus(new LinuxExitStatus(exitStatus));
            commandStatus.setTimedout(executor.getWatchdog().killedProcess());
        }

        return commandStatus;
    }

    private Executor configureExecutor(Command command) {
        Executor executor = getExecutor();
        int timeout = command.getTimeout();
        ExecuteWatchdog watchdog = timeout <= 0 ? new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT)
                : new ExecuteWatchdog(timeout * 1000L);
        executor.setWatchdog(watchdog);

        OutputStream out = command.getOutputStream();
        OutputStream err = command.getErrorStream();
        InputStream in = command.getInputStream();
        FlushPumpStreamHandler handler;
        if (out != null && err != null) {
            handler = new FlushPumpStreamHandler(out, err, in);
        } else if (out != null) {
            handler = new FlushPumpStreamHandler(out, new NullOutputStream(), in);
        } else if (err != null) {
            handler = new FlushPumpStreamHandler(new NullOutputStream(), err, in);
        } else {
            handler = new FlushPumpStreamHandler(new NullOutputStream(), new NullOutputStream(), in);
        }
        executor.setStreamHandler(handler);

        String directory = command.getDirectory();

        File workingDir = directory == null || directory.isEmpty() || !Files.isDirectory(Paths.get(directory))
                ? TEMP_DIR
                : new File(directory);
        executor.setWorkingDirectory(workingDir);
        return executor;
    }

    protected Executor getExecutor() {
        return new DefaultExecutor();
    }

    private void executeAsync(Command command, CommandLine commandLine, Consumer<CommandStatus> callback) {
        CommandStatus commandStatus = new CommandStatus(command, new LinuxExitStatus(0));
        commandStatus.setOutputStream(command.getOutputStream());
        commandStatus.setErrorStream(command.getErrorStream());
        commandStatus.setInputStream(command.getInputStream());

        Executor executor = configureExecutor(command);

        LinuxResultHandler resultHandler = new LinuxResultHandler(callback, executor);
        resultHandler.setStatus(commandStatus);

        logger.debug("Executing: {}", commandLine);
        try {
            Map<String, String> environment = command.getEnvironment();
            if (environment != null && !environment.isEmpty()) {
                Map<String, String> currentEnv = EnvironmentUtils.getProcEnvironment();
                currentEnv.putAll(environment);
                executor.execute(commandLine, currentEnv, resultHandler);
            } else {
                executor.execute(commandLine, resultHandler);
            }
        } catch (IOException e) {
            stopStreamHandler(executor);
            commandStatus.setExitStatus(new LinuxExitStatus(1));
            logger.error(COMMAND_MESSAGE + commandLine + " failed", e);
        }
    }

    private String[] buildKillCommand(Pid pid, Signal signal) {
        Integer pidNumber = pid.getPid();
        if (logger.isInfoEnabled()) {
            logger.info("Attempting to send {} to process with pid {}", ((LinuxSignal) signal).name(), pidNumber);
        }
        return new String[] { "kill", "-" + signal.getSignalNumber(), String.valueOf(pidNumber) };
    }

    private CommandLine buildUnprivilegedCommand(Command command) {
        // Build the command as follows:
        // su <command_user> -c "VARS... timeout -s <signal> <timeout> <command>"
        // or su <command_user> c "VARS... sh -c <command>"
        // The timeout command is added because the commons-exec doesn't allow to set the signal to send for killing a
        // process after a timeout
        CommandLine commandLine = new CommandLine("su");
        commandLine.addArgument(this.commandUsername);
        commandLine.addArgument("-c");

        List<String> c = new ArrayList<>();
        Map<String, String> env = command.getEnvironment();
        if (env != null && !env.isEmpty()) {
            env.entrySet().stream().forEach(entry -> c.add(entry.getKey() + "=" + entry.getValue()));
        }

        int timeout = command.getTimeout();
        if (timeout != -1) {
            c.add("timeout");
            c.add("-s");
            c.add(((LinuxSignal) command.getSignal()).name());
            c.add(Integer.toString(timeout));
        }

        Arrays.asList(command.getCommandLine()).stream().forEach(c::add);
        commandLine.addArgument(String.join(" ", c), false);

        return commandLine;
    }

    private CommandLine buildPrivilegedCommand(Command command) {
        CommandLine commandLine;
        if (command.isExecutedInAShell()) {
            commandLine = new CommandLine("/bin/sh");
            commandLine.addArgument("-c");
            commandLine.addArgument(command.toString(), false);
        } else {
            String[] tokens = command.getCommandLine();
            commandLine = new CommandLine(tokens[0]);
            for (int i = 1; i < tokens.length; i++) {
                commandLine.addArgument(tokens[i], false);
            }
        }
        return commandLine;
    }

    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream();
    }
}
