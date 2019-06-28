/*******************************************************************************
 * Copyright (c) 2019 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.linux.executor.privileged;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.kura.core.internal.linux.executor.ExecutorUtil;
import org.eclipse.kura.executor.CommandStats;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.PrivilegedExecutorService;
import org.osgi.service.component.ComponentContext;

public class PrivilegedExecutorServiceImpl implements PrivilegedExecutorService {

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
        String commandString = Arrays.asList(command).stream().collect(Collectors.joining(" "));
        if (!commandString.startsWith("sudo")) {
            commandString = "sudo " + commandString;
        }
        return ExecutorUtil.execute(timeout, directory, environment, commandString);
    }

    @Override
    public void execute(int timeout, String directory, Map<String, String> environment, Consumer<CommandStats> callback,
            String... command) throws IOException {
        String commandString = Arrays.asList(command).stream().collect(Collectors.joining(" "));
        if (!commandString.startsWith("sudo")) {
            commandString = "sudo " + commandString;
        }
        ExecutorUtil.execute(timeout, directory, environment, commandString, callback);
    }

    @Override
    public boolean stop(Pid pid, boolean force) throws IOException {
        return ExecutorUtil.stop(pid, force);
    }

    @Override
    public boolean kill(boolean force, String... command) throws IOException {
        String commandString = Arrays.asList(command).stream().collect(Collectors.joining(" "));
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