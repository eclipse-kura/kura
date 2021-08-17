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
 *******************************************************************************/
package org.eclipse.kura.core.linux.executor;

import java.util.function.Consumer;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.Executor;
import org.eclipse.kura.core.internal.linux.executor.ExecutorUtil;
import org.eclipse.kura.executor.CommandStatus;

public class LinuxResultHandler implements ExecuteResultHandler {

    private static final int TIMEOUT_EXIT_VALUE = 124;
    private static final int SIGTERM_EXIT_VALUE = 143;
    private final Consumer<CommandStatus> callback;
    private CommandStatus commandStatus;
    private Executor executor;

    public LinuxResultHandler(Consumer<CommandStatus> callback, Executor executor) {
        this.callback = callback;
        this.executor = executor;
    }

    public CommandStatus getStatus() {
        return this.commandStatus;
    }

    public void setStatus(CommandStatus status) {
        this.commandStatus = status;
    }

    @Override
    public void onProcessComplete(int exitValue) {
        ExecutorUtil.stopStreamHandler(this.executor);
        this.commandStatus.setExitStatus(new LinuxExitStatus(exitValue));
        this.callback.accept(this.commandStatus);
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        ExecutorUtil.stopStreamHandler(this.executor);
        this.commandStatus.setExitStatus(new LinuxExitStatus(e.getExitValue()));
        // The PrivilegedExecutorService kills a command with SIGTERM and exits with 143 when timedout; the
        // UnprivilegedExecutorService uses timeout command that exits with 124.
        if (e.getExitValue() == TIMEOUT_EXIT_VALUE || e.getExitValue() == SIGTERM_EXIT_VALUE) {
            this.commandStatus.setTimedout(true);
        }
        this.callback.accept(this.commandStatus);
    }

}
