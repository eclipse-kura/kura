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

public class PidAndFactoryPidAndCloudEndpointPid {

    private final String pid;
    private final String factoryPid;
    private final String cloudEndpointPid;

    public PidAndFactoryPidAndCloudEndpointPid(String pid, String factoryPid, String cloudEndpointPid) {

        this.pid = pid;
        this.factoryPid = factoryPid;
        this.cloudEndpointPid = cloudEndpointPid;
    }

    public String getPid() {
        return this.pid;
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public String getCloudEndpointPid() {
        return this.cloudEndpointPid;
    }

}
