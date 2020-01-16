/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and others
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.core.internal.linux.executor.ExecutorUtil;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.ExecutorFactory;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.PrivilegedExecutorService;
import org.eclipse.kura.executor.Signal;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivilegedExecutorServiceImpl implements PrivilegedExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(PrivilegedExecutorServiceImpl.class);
    private static final LinuxSignal DEFAULT_SIGNAL = LinuxSignal.SIGTERM;
    private ExecutorUtil executorUtil = new ExecutorUtil();

    @SuppressWarnings("unused")
    private ComponentContext ctx;

    public void activate(ComponentContext componentContext) {
        logger.info("activate...");
        this.ctx = componentContext;
    }

    public void deactivate(ComponentContext componentContext) {
        logger.info("deactivate...");
        this.ctx = null;
    }

    @Override
    public void setExecutorFactory(ExecutorFactory executorFactory) {
        if (this.executorUtil != null) {
            this.executorUtil.setExecutorFactory(executorFactory);
        }
    }

    @Override
    public CommandStatus execute(Command command) {
        if (command.getCommandLine() == null || command.getCommandLine().length == 0) {
            return buildErrorStatus();
        }
        if (command.getSignal() == null) {
            command.setSignal(DEFAULT_SIGNAL);
        }
        return this.executorUtil.executePrivileged(command);
    }

    @Override
    public void execute(Command command, Consumer<CommandStatus> callback) {
        if (command.getCommandLine() == null || command.getCommandLine().length == 0) {
            callback.accept(buildErrorStatus());
        }
        if (command.getSignal() == null) {
            command.setSignal(DEFAULT_SIGNAL);
        }
        this.executorUtil.executePrivileged(command, callback);
    }

    @Override
    public boolean stop(Pid pid, Signal signal) {
        boolean isStopped = false;
        if (signal == null) {
            isStopped = this.executorUtil.stopPrivileged(pid, DEFAULT_SIGNAL);
        } else {
            isStopped = this.executorUtil.stopPrivileged(pid, signal);
        }
        return isStopped;
    }

    @Override
    public boolean kill(String[] commandLine, Signal signal) {
        boolean isKilled = false;
        if (signal == null) {
            isKilled = this.executorUtil.killPrivileged(commandLine, DEFAULT_SIGNAL);
        } else {
            isKilled = this.executorUtil.killPrivileged(commandLine, signal);
        }
        return isKilled;
    }

    @Override
    public boolean isRunning(Pid pid) {
        return this.executorUtil.isRunning(pid);
    }

    @Override
    public boolean isRunning(String[] commandLine) {
        return this.executorUtil.isRunning(commandLine);
    }

    @Override
    public Map<String, Pid> getPids(String[] commandLine) {
        return this.executorUtil.getPids(commandLine);
    }

    private CommandStatus buildErrorStatus() {
        CommandStatus status = new CommandStatus(new LinuxExitStatus(1));
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            err.write("The commandLine cannot be empty or not defined".getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            logger.error("Cannot write to error stream", e);
        }
        status.setErrorStream(err);
        return status;
    }
}