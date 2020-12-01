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

import org.apache.commons.io.output.NullOutputStream;

/**
 *
 * The CommandStatus object is returned by the {@link CommandExecutorService} after the execution of a command.
 * It contains all the relevant informations about the result of the command execution.
 * <p>
 * The parameters are the following:
 * <ul>
 * <li>command : the command run by the {@link CommandExecutorService}</li>
 * <li>exitStatus : the {@link ExitStatus} of the command. A value other than 0 means an error. When the command is
 * stopped by timeout the exit value is 124.</li>
 * <li>isTimedout : a flag that signals that the command was stopped by timeout</li>
 * <li>outputStream : the output of the command</li>
 * <li>errorStream : the error stream of the command</li>
 * <li>inputStream : the input stream used to send data to the process</li>
 * </ul>
 * 
 * @since 2.2
 *
 */
public class CommandStatus {

    private Command command;
    private ExitStatus exitStatus;
    private OutputStream outputStream;
    private OutputStream errorStream;
    private InputStream inputStream;
    private boolean isTimedout;

    public CommandStatus(Command command, ExitStatus exitStatus) {
        this.command = command;
        this.exitStatus = exitStatus;
        this.outputStream = new NullOutputStream();
        this.errorStream = new NullOutputStream();
        this.isTimedout = false;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public ExitStatus getExitStatus() {
        return this.exitStatus;
    }

    public void setExitStatus(ExitStatus exitStatus) {
        this.exitStatus = exitStatus;
    }

    public OutputStream getErrorStream() {
        return this.errorStream;
    }

    public void setErrorStream(OutputStream errorStream) {
        this.errorStream = errorStream;
    }

    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public boolean isTimedout() {
        return this.isTimedout;
    }

    public void setTimedout(boolean isTimedout) {
        this.isTimedout = isTimedout;
    }

}
