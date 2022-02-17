/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
    private String[] lastCommand;

    CommandExecutorServiceStub(CommandStatus returnedStatus) {
        this.returnedStatus = returnedStatus;
    }

    @Override
    public CommandStatus execute(Command command) {
        this.lastCommand = command.getCommandLine();
        return returnedStatus;
    }

    @Override
    public void execute(Command command, Consumer<CommandStatus> callback) {
        this.lastCommand = command.getCommandLine();
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

    public String[] getLastCommand() {
        return this.lastCommand;
    }
}
