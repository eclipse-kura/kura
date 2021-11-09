package org.eclipse.kura.linux.net.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.Signal;

class CommandExecutorServiceStub implements CommandExecutorService {

    CommandStatus returnedStatus;

    CommandExecutorServiceStub(CommandStatus returnedStatus) {
        this.returnedStatus = returnedStatus;
    }

    @Override
    public CommandStatus execute(Command command) {
        return returnedStatus;
    }

    @Override
    public void execute(Command command, Consumer<CommandStatus> callback) {
    }

    @Override
    public boolean stop(Pid pid, Signal signal) {
        return true;
    }

    @Override
    public boolean kill(String[] commandLine, Signal signal) {
        return true;
    }

    @Override
    public boolean isRunning(Pid pid) {
        return true;
    }

    @Override
    public boolean isRunning(String[] commandLine) {
        return true;
    }

    @Override
    public Map<String, Pid> getPids(String[] commandLine) {
        return null;
    }

    public void writeOutput(String commandOutput) {
        OutputStream out = new ByteArrayOutputStream();
        try (Writer w = new OutputStreamWriter(out, "UTF-8")) {
            w.write(commandOutput);
        } catch (Exception e) {
        }
        returnedStatus.setOutputStream(out);
    }
};