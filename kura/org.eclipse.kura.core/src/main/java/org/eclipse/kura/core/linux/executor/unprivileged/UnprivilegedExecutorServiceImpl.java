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
package org.eclipse.kura.core.linux.executor.unprivileged;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.internal.linux.executor.ExecutorUtil;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.Signal;
import org.eclipse.kura.executor.UnprivilegedExecutorService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnprivilegedExecutorServiceImpl implements UnprivilegedExecutorService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(UnprivilegedExecutorServiceImpl.class);
    private static final LinuxSignal DEFAULT_SIGNAL = LinuxSignal.SIGTERM;

    @SuppressWarnings("unused")
    private ComponentContext ctx;
    private UnprivilegedExecutorServiceOptions options;

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("activate...");

        this.ctx = componentContext;
        this.options = new UnprivilegedExecutorServiceOptions(properties);
        ExecutorUtil.setCommandUsername(this.options.getCommandUsername());
    }

    public void update(Map<String, Object> properties) {
        logger.info("updated...");

        this.options = new UnprivilegedExecutorServiceOptions(properties);
        ExecutorUtil.setCommandUsername(this.options.getCommandUsername());
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("deactivate...");
        this.ctx = null;
    }

    @Override
    public CommandStatus execute(Command command) {
        if (command.getSignal() == null) {
            command.setSignal(DEFAULT_SIGNAL);
        }
        return ExecutorUtil.executeUnprivileged(command);
    }

    @Override
    public void execute(Command command, Consumer<CommandStatus> callback) {
        if (command.getSignal() == null) {
            command.setSignal(DEFAULT_SIGNAL);
        }
        ExecutorUtil.executeUnprivileged(command, callback);
    }

    @Override
    public boolean stop(Pid pid, Signal signal) {
        boolean isStopped = false;
        if (signal == null) {
            isStopped = ExecutorUtil.stopUnprivileged(pid, DEFAULT_SIGNAL);
        } else {
            isStopped = ExecutorUtil.stopUnprivileged(pid, signal);
        }
        return isStopped;
    }

    @Override
    public boolean kill(String commandLine, Signal signal) {
        boolean isKilled = false;
        if (signal == null) {
            isKilled = ExecutorUtil.killUnprivileged(commandLine, DEFAULT_SIGNAL);
        } else {
            isKilled = ExecutorUtil.killUnprivileged(commandLine, signal);
        }
        return isKilled;
    }

    @Override
    public boolean isRunning(Pid pid) {
        return ExecutorUtil.isRunning(pid);
    }

    @Override
    public boolean isRunning(String commandLine) {
        return ExecutorUtil.isRunning(commandLine);
    }

    @Override
    public List<Pid> getPids(String commandLine, boolean exact) {
        return ExecutorUtil.getPids(commandLine, exact);
    }

}
