/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.executor;

import java.util.Map;
import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface provides methods for running system processes or executing system commands.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.2
 */
@ProviderType
public interface CommandExecutorService {

    /**
     * Synchronously executes a system command.
     *
     * @param command
     *            the {@link Command} to be executed
     * @return a {@link CommandStatus} object
     */
    public CommandStatus execute(Command command);

    /**
     * Asynchronously executes a system command.
     *
     * @param command
     *            the {@link Command} to be executed
     * @param callback
     *            the consumer called when the command returns
     */
    public void execute(Command command, Consumer<CommandStatus> callback);

    /**
     * Stops the system process identified by the given {@link Pid}.
     *
     * @param pid
     *            the {@link Pid} of the process to be stopped
     * @param signal
     *            the {@link Signal} sent to the process to stop it. If null, a default signal will be sent.
     *            The type of the default signal is implementation specific
     * @return a boolean value that is true if the stop operation succeeded
     */
    public boolean stop(Pid pid, Signal signal);

    /**
     * Kills the system commands containing all the tokens in the given command line.
     * If more processes are found, all of them will be killed.
     *
     * @param commandLine
     *            the command to be killed
     * @param signal
     *            the {@link Signal} sent to the command to kill it. If null, a default signal will be sent.
     *            The type of the default signal is implementation specific
     * @return a boolean value that is true if the kill operation succeeded
     */
    public boolean kill(String[] commandLine, Signal signal);

    /**
     * Returns true if the process identified by the given Pid is running.
     *
     * @param pid
     *            the {@link Pid} object of the process
     * @return a boolean value that is true if the process is running
     */
    public boolean isRunning(Pid pid);

    /**
     * Returns true if at least one process containing all the tokens in the given command line is found.
     * It is equivalent to !getPids(commandLine).isEmpty().
     *
     * @param commandLine
     *            the command to be checked
     * @return a boolean value that is true if the command is running
     */
    public boolean isRunning(String[] commandLine);

    /**
     * This method searches for running processes containing all the tokens in the command line.
     * It returns a map whose keys are the commands found and the values are the associated {@link Pid}s.
     *
     * @param commandLine
     *            the command line
     * @return a map of commands and associated {@link Pid}
     */
    public Map<String, Pid> getPids(String[] commandLine);

}
