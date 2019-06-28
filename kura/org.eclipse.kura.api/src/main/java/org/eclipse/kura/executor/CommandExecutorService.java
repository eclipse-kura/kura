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
package org.eclipse.kura.executor;

import java.util.List;
import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface provides methods for running system processes or executing system commands.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CommandExecutorService {

    /**
     * Synchronously execute a system command.
     * 
     * @param command
     *            the {@link Command} to be executed
     * @return a {@link CommandStatus} object
     */
    public CommandStatus execute(Command command);

    /**
     * Asynchronously execute a system command.
     * 
     * @param command
     *            the {@link Command} to be executed
     * @param callback
     *            the consumer executed when the command returns
     */
    public void execute(Command command, Consumer<CommandStatus> callback);

    /**
     * Stop the system process identified by its Pid.
     * 
     * @param pid
     *            the {@link Pid} of the process to be stopped
     * @param signal
     *            the {@link Signal} send to the process to stop it. If null, a default signal will be send
     * @return true if the stop was succeeded
     */
    public boolean stop(Pid pid, Signal signal);

    /**
     * Kill a system command. If more instances of the same processes are running, all will be killed.
     * 
     * @param commandLine
     *            the command to be killed
     * @param signal
     *            the {@link Signal} send to the command to kill it. If null, a default signal will be send
     * @return true if the kill was succeeded
     */
    public boolean kill(String commandLine, Signal signal);

    /**
     * Return true if the process identified by the Pid is running
     * 
     * @param pid
     *            the {@link Pid} object of the process
     * @return true if the process is running, false otherwise
     */
    public boolean isRunning(Pid pid);

    /**
     * Return true if the command is running.
     * It is equivalent to !getPids(commandLine, true).isEmpty().
     * 
     * @param commandLine
     *            the command to be checked
     * @return true if the command is running, false otherwise
     */
    public boolean isRunning(String commandLine);

    /**
     * Return the list of {@link Pid} of the given command or process
     * 
     * @param commandLine
     *            the command line
     * @param exact
     *            if set to true an exact match of the command line will be checked, otherwise the pids of the processes
     *            that
     *            at least contain the given command will be returned
     * @return
     */
    public List<Pid> getPids(String commandLine, boolean exact);

}
