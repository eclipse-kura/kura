/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

public class RestCommandRequest {

    // Mandatory //
    private String command;
    private String password;

    // Optional //
    private byte[] zipBytes;
    private String[] arguments;
    private String[] enviromentPairs;
    private String workingDirectory;
    private boolean isRunAsync;

    public void setCommand(String command) {
        this.command = command;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

    public void setEnviromentPairs(String[] enviromentPairs) {
        this.arguments = enviromentPairs;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setIsRunAsync(boolean isRunAsync) {
        this.isRunAsync = isRunAsync;
    }

    public void setZipBytes(byte[] body) {
        this.zipBytes = body;
    }

    public String getCommand() {
        return this.command;
    }

    public String[] getArguments() {
        return this.arguments;
    }

    public String[] getEnviromentPairs() {
        return this.enviromentPairs;
    }

    public String getWorkingDirectory() {
        return this.workingDirectory;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean getIsRunAsync() {
        return this.isRunAsync;
    }

    public byte[] getZipBytes() {
        return this.zipBytes;
    }

}
