package org.eclipse.kura.core.internal.linux.executor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.eclipse.kura.core.linux.executor.unprivileged.LinuxExitStatus;
import org.eclipse.kura.core.linux.executor.unprivileged.LinuxPid;
import org.eclipse.kura.core.linux.executor.unprivileged.LinuxResultHandler;
import org.eclipse.kura.executor.CommandStats;
import org.eclipse.kura.executor.Pid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorUtil {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorUtil.class);

    public static final String PRIVILEGED_OPERATIONS_NOT_ALLOWED = "Privileged operations not allowed";
    public static final String EXECUTING = "executing: {}";
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    private ExecutorUtil() {
        // Empty private constructor
    }

    public static CommandStats execute(int timeout, String directory, Map<String, String> environment, String command)
            throws IOException {
        CommandLine commandLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout * 1000L);
        executor.setWatchdog(watchdog);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final PumpStreamHandler handler = new PumpStreamHandler(out, err);

        executor.setStreamHandler(handler);
        File workingDir = directory == null || directory.isEmpty() ? TEMP_DIR : new File(directory);
        executor.setWorkingDirectory(workingDir);

        int exitStatus = 0;
        CommandStats commandStats;
        logger.info(EXECUTING, command);
        try {
            if (environment != null && !environment.isEmpty()) {
                // Map<String, String> currentEnvironment = mergeEnvironments(environment);
                exitStatus = executor.execute(commandLine, environment);
            } else {
                exitStatus = executor.execute(commandLine);
            }
        } catch (IOException e) {
            exitStatus = 1;
            logger.warn("Command " + command + " failed", e);
        } finally {
            commandStats = new CommandStats(new LinuxExitStatus(exitStatus));
            try {
                Optional<Integer> pid = getPid(command);
                if (pid.isPresent()) {
                    commandStats.setPid(Optional.of(new LinuxPid(pid.get())));
                }
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }
            commandStats.setOutputStream(out);
            commandStats.setErrorStream(err);
            commandStats.setTimedout(watchdog.killedProcess());
        }

        return commandStats;
    }

    public static void execute(int timeout, String directory, Map<String, String> environment, String command,
            Consumer<CommandStats> callback) throws IOException {
        CommandLine commandLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout * 1000L);
        executor.setWatchdog(watchdog);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final PumpStreamHandler handler = new PumpStreamHandler(out, err);

        executor.setStreamHandler(handler);
        File workingDir = directory == null || directory.isEmpty() ? TEMP_DIR : new File(directory);
        executor.setWorkingDirectory(workingDir);

        CommandStats commandStats = new CommandStats(new LinuxExitStatus(0));
        commandStats.setOutputStream(out);
        commandStats.setErrorStream(err);

        LinuxResultHandler resultHandler = new LinuxResultHandler(callback);
        resultHandler.setStats(commandStats);
        // Don't get the pid in handlers...

        if (environment != null && !environment.isEmpty()) {
            // Map<String, String> currentEnvironment = mergeEnvironments(environment);
            executor.execute(commandLine, environment, resultHandler);
        } else {
            executor.execute(commandLine, resultHandler);
        }

    }

    public static boolean stop(Pid pid, boolean force) throws IOException {
        boolean isStopped = true;
        if (isRunning(pid)) {
            StringBuilder killCommand = new StringBuilder("kill ");
            if (force) {
                killCommand.append("-9 ");
            }
            killCommand.append(pid);

            if (force) {
                logger.info("Attempting to kill -9 pid {}", pid);
            } else {
                logger.info("Attempting to kill pid {}", pid);
            }

            CommandStats commandStats = execute(60, null, null, killCommand.toString());
            isStopped = (Integer) commandStats.getExitStatus().getExitValue() == 0;
        }
        return isStopped;
    }

    public static boolean kill(String command, boolean force) throws IOException {
        boolean isKilled = true;
        Optional<Integer> pid = getPid(command);
        if (pid.isPresent()) {
            isKilled = stop(new LinuxPid(pid.get()), force);
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
            logger.error("Failed to check if process with pid {} is running", pidString);
        }
        return isRunning;
    }

    public static boolean isRunning(String command) {
        boolean isRunning = false;
        try {
            Optional<Integer> pid = getPid(command);
            if (pid.isPresent()) {
                isRunning = isRunning(new LinuxPid(pid.get()));
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return isRunning;
    }

    private static Optional<Integer> getPid(String command) throws IOException {
        // Remove sudo if needed
        String filteredCommand = command.startsWith("sudo") ? command.substring(4) : command;
        String pgrepCommand = "pgrep -f '" + filteredCommand + "'"; // Not the right way... use the old one.
        CommandLine commandLine = CommandLine.parse(pgrepCommand);
        DefaultExecutor executor = new DefaultExecutor();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final PumpStreamHandler handler = new PumpStreamHandler(out, err);

        executor.setStreamHandler(handler);
        executor.setWorkingDirectory(TEMP_DIR);
        executor.setExitValue(0);
        int exitValue = executor.execute(commandLine);
        String pid = new String(out.toByteArray(), UTF_8);

        if (exitValue == 0 && !pid.isEmpty()) {
            return Optional.of(Integer.parseInt(pid));
        } else {
            return Optional.empty();
        }
    }

    // private static Map<String, String> mergeEnvironments(Map<String, String> environment) throws IOException {
    // Map<String, String> currentEnvironment = EnvironmentUtils.getProcEnvironment();
    // environment.put("PATH", environment.get("PATH") + ":" + new File(frontendDirectory, "node").getAbsolutePath());
    // return currentEnvironment;
    // }

}
