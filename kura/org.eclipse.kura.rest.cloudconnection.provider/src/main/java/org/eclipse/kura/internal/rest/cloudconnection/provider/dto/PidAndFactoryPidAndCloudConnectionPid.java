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

public class PidAndFactoryPidAndCloudConnectionPid {

    private final String pid;
    private final String factoryPid;
    private final String cloudConnectionPid;

    public PidAndFactoryPidAndCloudConnectionPid(String pid, String factoryPid, String cloudConnectionPid) {
        this.pid = pid;
        this.factoryPid = factoryPid;
        this.cloudConnectionPid = cloudConnectionPid;
    }

    public String getPid() {
        return pid;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public String getCloudConnectionPid() {
        return cloudConnectionPid;
    }

}
