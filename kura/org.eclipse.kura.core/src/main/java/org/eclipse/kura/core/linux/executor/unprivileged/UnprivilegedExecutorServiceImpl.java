package org.eclipse.kura.core.linux.executor.unprivileged;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.kura.core.internal.linux.executor.ExecutorUtil;
import org.eclipse.kura.executor.CommandStats;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.UnprivilegedExecutorService;
import org.osgi.service.component.ComponentContext;

public class UnprivilegedExecutorServiceImpl implements UnprivilegedExecutorService {

    @SuppressWarnings("unused")
    private ComponentContext ctx;

    protected void activate(ComponentContext componentContext) {
        this.ctx = componentContext;
    }

    protected void deactivate(ComponentContext componentContext) {
        this.ctx = null;
    }

    @Override
    public CommandStats execute(int timeout, String... command) throws IOException {
        return execute(timeout, null, null, command);
    }

    @Override
    public CommandStats execute(int timeout, String directory, String... command) throws IOException {
        return execute(timeout, directory, null, command);
    }

    @Override
    public CommandStats execute(int timeout, String directory, Map<String, String> environment, String... command)
            throws IOException {
        CommandStats stats = new CommandStats(new LinuxExitStatus(0));
        String commandString = Arrays.asList(command).stream().collect(Collectors.joining(" "));
        if (commandString.contains("sudo")) {
            stats.setExitStatus(new LinuxExitStatus(1));
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            errorStream.write(ExecutorUtil.PRIVILEGED_OPERATIONS_NOT_ALLOWED.getBytes(UTF_8));
            stats.setErrorStream(errorStream);
        } else {
            // Checks also scripts and directory?
            stats = ExecutorUtil.execute(timeout, directory, environment, commandString);
        }
        return stats;
    }

    @Override
    public void execute(int timeout, String directory, Map<String, String> environment, Consumer<CommandStats> callback,
            String... command) throws IOException {
        String commandString = Arrays.asList(command).stream().collect(Collectors.joining(" "));
        if (commandString.contains("sudo")) {
            CommandStats stats = new CommandStats(new LinuxExitStatus(1));
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            errorStream.write(ExecutorUtil.PRIVILEGED_OPERATIONS_NOT_ALLOWED.getBytes(UTF_8));
            stats.setErrorStream(errorStream);
            callback.accept(stats);
        } else {
            // Checks also scripts and directory?
            ExecutorUtil.execute(timeout, directory, environment, commandString, callback);
        }
    }

    @Override
    public boolean stop(Pid pid, boolean force) throws IOException {
        // Filter root processes
        return ExecutorUtil.stop(pid, force);
    }

    @Override
    public boolean kill(boolean force, String... command) throws IOException {
        String commandString = Arrays.asList(command).stream().collect(Collectors.joining(" "));
        if (commandString.contains("sudo")) {
            throw new IOException(ExecutorUtil.PRIVILEGED_OPERATIONS_NOT_ALLOWED);
        }
        return ExecutorUtil.kill(commandString, force);
    }

    @Override
    public boolean isRunning(Pid pid) {
        return ExecutorUtil.isRunning(pid);
    }

    @Override
    public boolean isRunning(String... command) {
        String commandString = Arrays.asList(command).stream().collect(Collectors.joining(" "));
        return ExecutorUtil.isRunning(commandString);
    }

}
