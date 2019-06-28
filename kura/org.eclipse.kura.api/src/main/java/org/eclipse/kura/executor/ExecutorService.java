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

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface provides methods for running system processes or executing system commands.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ExecutorService {

    /**
     * Synchronously execute a system command.
     * 
     * @param command
     *            the command to be executed
     * @param timeout
     *            the timeout in seconds after which the command is terminated
     * @return a {@link CommandStats} object
     * @throws IOException
     */
    public CommandStats execute(int timeout, String... command) throws IOException;

    /**
     * Synchronously execute a system command.
     * 
     * @param command
     *            the command to be executed
     * @param directory
     *            the directory where the command will be executed
     * @param timeout
     *            the timeout in seconds after which the command is terminated
     * @return a {@link CommandStats} object
     * @throws IOException
     */
    public CommandStats execute(int timeout, String directory, String... command) throws IOException;

    /**
     * Synchronously execute a system command.
     * 
     * @param command
     *            the command to be executed
     * @param directory
     *            the directory where the command will be executed
     * @param environment
     *            set of key-value pairs representing environmental variables
     * @param timeout
     *            the timeout in seconds after which the command is terminated
     * @return a {@link CommandStats} object
     * @throws IOException
     */
    public CommandStats execute(int timeout, String directory, Map<String, String> environment, String... command)
            throws IOException;

    public void execute(int timeout, String directory, Map<String, String> environment, Consumer<CommandStats> callback,
            String... command) throws IOException;

    /**
     * Stop the system process identified by its Pid.
     * 
     * @param pid
     *            the Pid object of the process to be stopped
     * @param force
     *            force the stop of the process
     * @return true if the stop is succeeded
     * @throws IOException
     */
    public boolean stop(Pid pid, boolean force) throws IOException;

    /**
     * Kill a system command.
     * 
     * @param command
     *            the command to be killed
     * @param force
     *            force the stop of the process
     * @return true if the stop is succeeded
     * @throws IOException
     */
    public boolean kill(boolean force, String... command) throws IOException;

    /**
     * Return true if the process identified by the Pid is running
     * 
     * @param pid
     *            the Pid object of the process
     * @return true if the process is running, false otherwise
     */
    public boolean isRunning(Pid pid);

    /**
     * Return true if the command is running
     * 
     * @param command
     *            the command to be checked
     * @return true if the command is running, false otherwise
     */
    public boolean isRunning(String... command);

}
