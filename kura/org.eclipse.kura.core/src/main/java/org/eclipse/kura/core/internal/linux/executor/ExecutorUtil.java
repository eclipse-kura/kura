/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.internal.linux.executor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.eclipse.kura.core.linux.executor.LinuxExitValue;
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

    private static final String COMMAND = "Command ";

    private static final String FAILED_TO_GET_PID_MESSAGE = "Failed to get pid for command '{}'";

    private static final Logger logger = LoggerFactory.getLogger(ExecutorUtil.class);

    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final int GET_PID_ATTEMPTS = 5;

    private static String commandUsername = "kura";

    private ExecutorUtil() {
        // Empty private constructor
    }

    public static String getCommandUsername() {
        return commandUsername;
    }

    public static void setCommandUsername(String commandUsername) {
        ExecutorUtil.commandUsername = commandUsername;
    }

    public static CommandStatus executeUnprivileged(Command command) {
        CommandLine commandLine = buildUnprivilegedCommand(command);
        CommandStatus status = executeSync(command, commandLine);
        // Set exact parameter to true to get the process forked by the sh command
        List<Pid> pids = getPids(command.getCommandLine());
        if (!pids.isEmpty()) {
            status.setPid(pids.get(0));
        }
        return status;
    }

    public static void executeUnprivileged(Command command, Consumer<CommandStatus> callback) {
        CommandLine commandLine = buildUnprivilegedCommand(command);
        CommandStatus status = executeAsync(command, commandLine, callback);
        // Set exact parameter to true to get the process forked by the sh command
        List<Pid> pids = getPids(command.getCommandLine());
        if (!pids.isEmpty()) {
            status.setPid(pids.get(0));
        }
    }

    public static CommandStatus executePrivileged(Command command) {
        CommandLine commandLine = buildPrivilegedCommand(command);
        CommandStatus status = executeSync(command, commandLine);
        // Set exact parameter to true to get the process forked by the sh command
        List<Pid> pids = getPids(command.getCommandLine());
        if (!pids.isEmpty()) {
            status.setPid(pids.get(0));
        }
        return status;
    }

    public static void executePrivileged(Command command, Consumer<CommandStatus> callback) {
        CommandLine commandLine = buildPrivilegedCommand(command);
        CommandStatus status = executeAsync(command, commandLine, callback);
        // Set exact parameter to true to get the process forked by the sh command
        List<Pid> pids = getPids(command.getCommandLine());
        if (!pids.isEmpty()) {
            status.setPid(pids.get(0));
        }
    }

    public static boolean stopUnprivileged(Pid pid, Signal signal) {
        boolean isStopped = true;
        if (isRunning(pid)) {
            Command killCommand = new Command(buildKillCommand(pid, signal));
            killCommand.setTimeout(60);
            killCommand.setSignal(signal);
            CommandStatus commandStatus = executeUnprivileged(killCommand);
            isStopped = (Integer) commandStatus.getExitStatus().getExitValue() == 0;
        }
        return isStopped;
    }

    public static boolean killUnprivileged(String commandLine, Signal signal) {
        List<Boolean> areAllKilled = new ArrayList<>();
        List<Pid> pids = getPids(commandLine);
        for (Pid pid : pids) {
            areAllKilled.add(stopUnprivileged(pid, signal));
        }
        return areAllKilled.stream().allMatch(b -> b);
    }

    public static boolean stopPrivileged(Pid pid, Signal signal) {
        boolean isStopped = true;
        if (isRunning(pid)) {
            Command killCommand = new Command(buildKillCommand(pid, signal));
            killCommand.setTimeout(60);
            killCommand.setSignal(signal);
            CommandStatus commandStatus = executePrivileged(killCommand);
            isStopped = (Integer) commandStatus.getExitStatus().getExitValue() == 0;
        }
        return isStopped;
    }

    public static boolean killPrivileged(String commandLine, Signal signal) {
        boolean isKilled = true;
        List<Pid> pids = getPids(commandLine);
        for (Pid pid : pids) {
            isKilled &= stopPrivileged(pid, signal);
        }
        return isKilled;
    }

    public static boolean isRunning(Pid pid) {
        boolean isRunning = false;
        String pidString = ((Integer) pid.getPid()).toString();
        String psCommand = "ps -p " + pidString;
        CommandLine commandLine = CommandLine.parse(psCommand);
        DefaultExecutor executor = new DefaultExecutor();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
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

    public static boolean isRunning(String commandLine) {
        return !getPids(commandLine).isEmpty();
    }

    public static List<Pid> getPids(String commandLine) {
        // Perform the search for the pid of a command/process for few times, since it could not be available just after
        // a command is run.
        int attempts = GET_PID_ATTEMPTS;
        List<Integer> pids = new ArrayList<>();
        while (attempts > 0 && pids.isEmpty()) {
            pids = getPidsInternal(commandLine);
            attempts--;
        }
        if (pids.isEmpty()) {
            return new ArrayList<>();
        } else {
            return convertToPid(pids);
        }
    }

    public static List<Pid> convertToPid(List<Integer> pids) {
        return pids.stream().map(LinuxPid::new).collect(Collectors.toList());
    }

    private static List<Integer> getPidsInternal(String commandLine) {
        List<Integer> pids = new ArrayList<>();
        CommandLine pidofCommandLine = new CommandLine("pidof");
        for (String token : commandLine.split("\\s+")) {
            pidofCommandLine.addArgument(token.replace("-", ""), false); // Remove '-' from commandLine to avoid
                                                                         // errors
        }
        DefaultExecutor executor = new DefaultExecutor();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final PumpStreamHandler handler = new PumpStreamHandler(out, err);

        executor.setStreamHandler(handler);
        executor.setWorkingDirectory(TEMP_DIR);
        executor.setExitValue(0);
        int exitValue = 1;
        try {
            exitValue = executor.execute(pidofCommandLine);
        } catch (IOException e) {
            logger.debug(FAILED_TO_GET_PID_MESSAGE, commandLine, e);
        }
        if (exitValue == 0) {
            pids = Arrays.stream(new String(out.toByteArray(), UTF_8).split("\\s+")).map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
        return pids;
    }

    private static CommandStatus executeSync(Command command, CommandLine commandLine) {
        CommandStatus commandStatus = new CommandStatus(new LinuxExitValue(0));
        commandStatus.setOutputStream(command.getOutputStream());
        commandStatus.setErrorStream(command.getErrorStream());
        commandStatus.setInputStream(command.getInputStream());

        DefaultExecutor executor = configureExecutor(command);

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
            logger.debug(COMMAND + commandLine + " returned error code " + exitStatus, e);
        } catch (IOException e) {
            exitStatus = 1;
            logger.error(COMMAND + commandLine + " failed", e);
        } finally {
            commandStatus.setExitStatus(new LinuxExitValue(exitStatus));
            commandStatus.setTimedout(executor.getWatchdog().killedProcess());
        }

        return commandStatus;
    }

    private static DefaultExecutor configureExecutor(Command command) {
        DefaultExecutor executor = new DefaultExecutor();
        int timeout = command.getTimeout();
        ExecuteWatchdog watchdog = timeout == -1 ? new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT)
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
        File workingDir = directory == null || directory.isEmpty() ? TEMP_DIR : new File(directory);
        executor.setWorkingDirectory(workingDir);
        return executor;
    }

    private static CommandStatus executeAsync(Command command, CommandLine commandLine,
            Consumer<CommandStatus> callback) {
        CommandStatus commandStatus = new CommandStatus(new LinuxExitValue(0));
        commandStatus.setOutputStream(command.getOutputStream());
        commandStatus.setErrorStream(command.getErrorStream());
        commandStatus.setInputStream(command.getInputStream());

        DefaultExecutor executor = configureExecutor(command);

        LinuxResultHandler resultHandler = new LinuxResultHandler(callback);
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
            commandStatus.setExitStatus(new LinuxExitValue(1));
            logger.error(COMMAND + commandLine + " failed", e);
        }

        return commandStatus;
    }

    private static String buildKillCommand(Pid pid, Signal signal) {
        Integer pidNumber = (Integer) pid.getPid();
        StringBuilder killCommand = new StringBuilder("kill ");
        killCommand.append("-" + signal.getSignalNumber() + " ");
        killCommand.append(pidNumber);

        logger.info("Attempting to send {} to process with pid {}", ((LinuxSignal) signal).name(), pidNumber);
        return killCommand.toString();
    }

    private static CommandLine buildUnprivilegedCommand(Command command) {
        // Build the command as follows:
        // sudo -u <command_user> -s VARS... timeout -s <signal> <timeout> sh -c "cd <directory>; <command>"
        // or sudo -u <command_user> -s VARS... sh -c "cd <directory>; <command>"
        // The timeout command is added because the commons-exec fails to destroy a process started with sudo
        CommandLine commandLine = new CommandLine("sudo");
        commandLine.addArgument("-u");
        commandLine.addArgument(ExecutorUtil.commandUsername);
        commandLine.addArgument("-s");

        Map<String, String> env = command.getEnvironment();
        if (env != null && !env.isEmpty()) {
            env.entrySet().stream().forEach(entry -> commandLine.addArgument(entry.getKey() + "=" + entry.getValue()));
        }

        int timeout = command.getTimeout();
        if (timeout != -1) {
            commandLine.addArgument("timeout");
            commandLine.addArgument("-s");
            commandLine.addArgument(((LinuxSignal) command.getSignal()).name());
            commandLine.addArgument(Integer.toString(timeout));
        }

        commandLine.addArgument("sh");
        commandLine.addArgument("-c");

        StringBuilder sb = new StringBuilder();
        sb.append("cd ");
        String directory = command.getDirectory();
        String workingDirectory = directory == null || directory.isEmpty() ? TEMP_DIR.getAbsolutePath() : directory;
        sb.append(workingDirectory);
        sb.append("; ");
        sb.append(command.getCommandLine());
        commandLine.addArgument(sb.toString(), false);

        return commandLine;
    }

    private static CommandLine buildPrivilegedCommand(Command command) {
        // Build the command passing single arguments to commandLine
        String[] tokens = command.getCommandLine().split("\\s+");
        CommandLine commandLine = new CommandLine(tokens[0]);
        for (int i = 1; i < tokens.length; i++) {
            commandLine.addArgument(tokens[i], false);
        }
        return commandLine;
    }

}
