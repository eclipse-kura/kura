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
 * @since 2.2
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
        return this.commandLine;
    }

    public String getDirectory() {
        return this.directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Map<String, String> getEnvironment() {
        return this.environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Signal getSignal() {
        return this.signal;
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
        return this.out;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public OutputStream getErrorStream() {
        return this.err;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    public InputStream getInputStream() {
        return this.in;
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
        result = prime * result + Arrays.hashCode(this.commandLine);
        result = prime * result + (this.directory == null ? 0 : this.directory.hashCode());
        result = prime * result + (this.environment == null ? 0 : this.environment.hashCode());
        result = prime * result + (this.executeInAShell ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Command other = (Command) obj;
        if (!Arrays.equals(this.commandLine, other.commandLine)) {
            return false;
        }
        if (this.directory == null) {
            if (other.directory != null) {
                return false;
            }
        } else if (!this.directory.equals(other.directory)) {
            return false;
        }
        if (this.environment == null) {
            if (other.environment != null) {
                return false;
            }
        } else if (!this.environment.equals(other.environment)) {
            return false;
        }
        if (this.executeInAShell != other.executeInAShell) {
            return false;
        }
        return true;
    }

}
