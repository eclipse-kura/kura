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

public class CloudConnectionFactoryPidAndCloudEndpointPid {

    private final String cloudConnectionFactoryPid;
    private final String cloudEndpointPid;

    public CloudConnectionFactoryPidAndCloudEndpointPid(String cloudConnectionFactoryPid, String cloudEndpointPid) {
        super();
        this.cloudConnectionFactoryPid = cloudConnectionFactoryPid;
        this.cloudEndpointPid = cloudEndpointPid;
    }

    public String getCloudConnectionFactoryPid() {
        return cloudConnectionFactoryPid;
    }

    public String getCloudEndpointPid() {
        return cloudEndpointPid;
    }

}
