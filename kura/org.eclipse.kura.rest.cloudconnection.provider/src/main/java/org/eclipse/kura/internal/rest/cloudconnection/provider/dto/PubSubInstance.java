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

public class PubSubInstance {

    private final String cloudEndpointPid;
    private final String pid;
    private final String factoryPid;
    private final CloudPubSubType type;

    public PubSubInstance(String cloudEndpointPid, String pid, String factoryPid, CloudPubSubType type) {
        this.cloudEndpointPid = cloudEndpointPid;
        this.pid = pid;
        this.factoryPid = factoryPid;
        this.type = type;
    }

    public String getCloudEndpointPid() {
        return this.cloudEndpointPid;
    }

    public String getPid() {
        return this.pid;
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public CloudPubSubType getType() {
        return this.type;
    }

}
