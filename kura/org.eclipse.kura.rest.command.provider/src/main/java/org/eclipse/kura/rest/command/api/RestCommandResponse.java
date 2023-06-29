/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.command.api;

public class RestCommandResponse {

    private String stdout;
    private String stderr;
    private int exitCode;
    private Boolean isTimeOut;

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public void setIsTimeOut(Boolean isTimeOut) {
        this.isTimeOut = isTimeOut;
    }

    public String getStdout() {
        return this.stdout;
    }

    public String getStderr() {
        return this.stderr;
    }

    public int getExitCode() {
        return this.exitCode;
    }

    public Boolean getIsTimeOut() {
        return this.isTimeOut;
    }
}
