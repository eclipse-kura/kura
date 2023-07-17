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

import java.util.Base64;
import java.util.Map;

public class RestCommandRequest {

    private String command;
    private String password;
    private String zipBytes;
    private String[] arguments;
    private Map<String, String> environmentPairs;
    private String workingDirectory;

    public void setCommand(String command) {
        this.command = command;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

    public void setEnvironmentPairs(Map<String, String> environmentPairs) {
        this.environmentPairs = environmentPairs;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setZipBytes(String zipBytes) {
        this.zipBytes = zipBytes;
    }

    public String getCommand() {
        return this.command;
    }

    public String[] getArguments() {
        return this.arguments;
    }

    public Map<String, String> getEnvironmentPairs() {
        return this.environmentPairs;
    }

    public String[] getEnvironmentPairsAsStringArray() {
        if (this.environmentPairs == null) {
            return new String[0];
        }
        return this.environmentPairs.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
    }

    public String getWorkingDirectory() {
        return this.workingDirectory;
    }

    public String getPassword() {
        return this.password;
    }

    public String getZipBytes() {
        return this.zipBytes;
    }

    public byte[] getZipBytesAsByteArray() {
        if (this.zipBytes == null) {
            return new byte[0];
        }

        return Base64.getDecoder().decode(this.zipBytes);
    }

}
