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
package org.eclipse.kura.core.linux.executor;

import java.util.function.Consumer;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.eclipse.kura.executor.CommandStatus;

public class LinuxResultHandler implements ExecuteResultHandler {

    private static final int TIMEOUT_EXIT_VALUE = 124;
    private static final int SIGTERM_EXIT_VALUE = 143;
    private final Consumer<CommandStatus> callback;
    private CommandStatus commandStatus;

    public LinuxResultHandler(Consumer<CommandStatus> callback) {
        this.callback = callback;
    }

    public CommandStatus getStatus() {
        return this.commandStatus;
    }

    public void setStatus(CommandStatus status) {
        this.commandStatus = status;
    }

    @Override
    public void onProcessComplete(int exitValue) {
        this.commandStatus.setExitStatus(new LinuxExitStatus(exitValue));
        this.callback.accept(this.commandStatus);
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        this.commandStatus.setExitStatus(new LinuxExitStatus(e.getExitValue()));
        // The PrivilegedExecutorService kills a command with SIGTERM and exits with 143 when timedout; the
        // UnprivilegedExecutorService uses timeout command that exits with 124.
        if (e.getExitValue() == TIMEOUT_EXIT_VALUE || e.getExitValue() == SIGTERM_EXIT_VALUE) {
            this.commandStatus.setTimedout(true);
        }
        this.callback.accept(this.commandStatus);
    }

}
