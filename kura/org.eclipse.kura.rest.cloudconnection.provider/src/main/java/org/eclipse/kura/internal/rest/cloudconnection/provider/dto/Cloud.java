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

public class Cloud {

    private String pid;

    private String factoryPid;
    private String defaultFactoryPid;
    private String defaultFactoryPidRegex;

    public Cloud() {
    }

    public Cloud(String pid, String factoryPid) {
        this.pid = pid;
        this.factoryPid = factoryPid;
    }

    public Cloud(String pid) {
        this.pid = pid;
    }

    public String getPid() {
        return this.pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public void setFactoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
    }

    public String getDefaultFactoryPid() {
        return this.defaultFactoryPid;
    }

    public void setDefaultFactoryPid(String defaultFactoryPid) {
        this.defaultFactoryPid = defaultFactoryPid;
    }

    public String getDefaultFactoryPidRegex() {
        return this.defaultFactoryPidRegex;
    }

    public void setDefaultFactoryPidRegex(String defaultFactoryPidRegex) {
        this.defaultFactoryPidRegex = defaultFactoryPidRegex;
    }

}
