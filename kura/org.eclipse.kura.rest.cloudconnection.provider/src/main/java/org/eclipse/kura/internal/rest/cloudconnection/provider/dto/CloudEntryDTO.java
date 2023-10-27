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
package org.eclipse.kura.internal.rest.cloudconnection.provider.dto;

public class CloudEntryDTO {

    private final String pid;
    private String factoryPid;
    private String defaultFactoryPid;
    private String defaultFactoryPidRegex;

    public CloudEntryDTO(String pid, String factoryPid) {
        this.pid = pid;
        this.factoryPid = factoryPid;
    }

    public CloudEntryDTO(String pid) {
        this.pid = pid;
    }

    public String getPid() {
        return pid;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public void setFactoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
    }

    public String getDefaultFactoryPid() {
        return defaultFactoryPid;
    }

    public void setDefaultFactoryPid(String defaultFactoryPid) {
        this.defaultFactoryPid = defaultFactoryPid;
    }

    public String getDefaultFactoryPidRegex() {
        return defaultFactoryPidRegex;
    }

    public void setDefaultFactoryPidRegex(String defaultFactoryPidRegex) {
        this.defaultFactoryPidRegex = defaultFactoryPidRegex;
    }

}
