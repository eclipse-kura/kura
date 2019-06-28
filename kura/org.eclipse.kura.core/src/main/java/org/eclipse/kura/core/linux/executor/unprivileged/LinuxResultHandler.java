package org.eclipse.kura.core.linux.executor.unprivileged;

import java.util.function.Consumer;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.eclipse.kura.executor.CommandStats;

public class LinuxResultHandler implements ExecuteResultHandler {

    private final Consumer<CommandStats> callback;
    private CommandStats commandStats;

    public LinuxResultHandler(Consumer<CommandStats> callback) {
        this.callback = callback;
    }

    public CommandStats getStats() {
        return this.commandStats;
    }

    public void setStats(CommandStats stats) {
        this.commandStats = stats;
    }

    @Override
    public void onProcessComplete(int exitValue) {
        this.commandStats.setExitStatus(new LinuxExitStatus(exitValue));
        this.callback.accept(this.commandStats);
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        this.commandStats.setExitStatus(new LinuxExitStatus(0));
        this.callback.accept(this.commandStats);
    }

}
