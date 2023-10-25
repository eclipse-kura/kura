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
    private final String factoryPid;
    private final String defaultFactoryPid;
    private final String defaultFactoryPidRegex;

    public CloudEntryDTO(String pid, String factoryPid, String defaultFactoryPid, String defaultFactoryPidRegex) {
        this.pid = pid;
        this.factoryPid = factoryPid;
        this.defaultFactoryPid = defaultFactoryPid;
        this.defaultFactoryPidRegex = defaultFactoryPidRegex;
    }

    public String getPid() {
        return pid;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public String getDefaultFactoryPid() {
        return defaultFactoryPid;
    }

    public String getDefaultFactoryPidRegex() {
        return defaultFactoryPidRegex;
    }

}
