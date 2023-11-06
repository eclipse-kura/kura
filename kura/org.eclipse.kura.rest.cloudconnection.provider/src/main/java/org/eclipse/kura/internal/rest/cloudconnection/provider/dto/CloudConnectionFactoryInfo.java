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

public class CloudConnectionFactoryInfo {

    private final String pid;
    private final String defaultPid;
    private final String pidRegex;

    public CloudConnectionFactoryInfo(String pid, String defaultPid, String pidRegex) {
        super();
        this.pid = pid;
        this.defaultPid = defaultPid;
        this.pidRegex = pidRegex;
    }

    public String getPid() {
        return pid;
    }

    public String getDefaultPid() {
        return defaultPid;
    }

    public String getPidRegex() {
        return pidRegex;
    }

}
