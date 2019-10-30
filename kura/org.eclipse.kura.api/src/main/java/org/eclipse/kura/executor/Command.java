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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.output.NullOutputStream;

/**
 * 
 * The Command class includes the informations needed by the {@link CommandExecutorService} to run a system command.
 * The only mandatory parameter is the commandLine that represents the command to be run:
 * </br>
 * </br>
 * &nbsp&nbsp Command command = new Command("ls -all");
 * </br>
 * </br>
 * Optional parameters are:
 * <ul>
 * <li>directory : the directory where the command is run</li>
 * <li>environment : a map containing the environment variables needed by the command</li>
 * <li>out : the output stream representing the output of the command</li>
 * <li>err : the error stream representing the errors of the command</li>
 * <li>in : the input stream representing the input of the command</li>
 * <li>timeout : the timeout in seconds after that the command is stopped. -1 means infinite timeout.</li>
 * <li>signal : the {@link Signal} sent to the command to stop it after timeout</li>
 * <li>executeInAShell : a flag that indicates if the command should be executed in a shell/terminal. Default is
 * false.</li>
 * </ul>
 * 
 */
public class Command {

    private final String[] commandLine;
    private String directory;
    private Map<String, String> environment;
    private int timeout = -1;
    private Signal signal;
    private boolean executeInAShell;
    private OutputStream out;
    private OutputStream err;
    private InputStream in;

    public Command(String[] commandLine) {
        this.commandLine = commandLine;
        this.out = new NullOutputStream();
        this.err = new NullOutputStream();
    }

    public String[] getCommandLine() {
        return commandLine;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Signal getSignal() {
        return signal;
    }

    public void setSignal(Signal signal) {
        this.signal = signal;
    }

    public boolean isExecutedInAShell() {
        return this.executeInAShell;
    }

    public void setExecuteInAShell(boolean executeInAShell) {
        this.executeInAShell = executeInAShell;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public OutputStream getErrorStream() {
        return err;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    public InputStream getInputStream() {
        return in;
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public String toString() {
        return String.join(" ", this.commandLine);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(commandLine);
        result = prime * result + ((directory == null) ? 0 : directory.hashCode());
        result = prime * result + ((environment == null) ? 0 : environment.hashCode());
        result = prime * result + (executeInAShell ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Command other = (Command) obj;
        if (!Arrays.equals(commandLine, other.commandLine))
            return false;
        if (directory == null) {
            if (other.directory != null)
                return false;
        } else if (!directory.equals(other.directory))
            return false;
        if (environment == null) {
            if (other.environment != null)
                return false;
        } else if (!environment.equals(other.environment))
            return false;
        if (executeInAShell != other.executeInAShell)
            return false;
        return true;
    }

}
