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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

public class CommandStats {

    private Optional<Pid> pid;
    private ExitStatus exitStatus;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private boolean isTimedout;

    public CommandStats(ExitStatus exitStatus) {
        this.pid = Optional.empty();
        this.exitStatus = exitStatus;
        this.outputStream = new ByteArrayOutputStream();
        this.errorStream = new ByteArrayOutputStream();
        this.isTimedout = false;
    }

    public Optional<Pid> getPid() {
        return pid;
    }

    public void setPid(Optional<Pid> pid) {
        this.pid = pid;
    }

    public ExitStatus getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(ExitStatus exitStatus) {
        this.exitStatus = exitStatus;
    }

    public String getErrorString() {
        return new String(errorStream.toByteArray(), UTF_8);
    }

    public ByteArrayOutputStream getErrorStream() {
        return this.errorStream;
    }

    public void setErrorStream(ByteArrayOutputStream errorStream) {
        this.errorStream = errorStream;
    }

    public String getOutputString() {
        return new String(outputStream.toByteArray(), UTF_8);
    }

    public ByteArrayOutputStream getOutputStream() {
        return this.outputStream;
    }

    public void setOutputStream(ByteArrayOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public boolean isTimedout() {
        return isTimedout;
    }

    public void setTimedout(boolean isTimedout) {
        this.isTimedout = isTimedout;
    }

}
