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

    private final String cloudConnectionFactoryPid;
    private final String defaultCloudEndpointPid;
    private final String cloudEndpointPidRegex;

    public CloudConnectionFactoryInfo(String cloudConnectionFactoryPid, String defaultCloudEndpointPid,
            String cloudEndpointPidRegex) {

        this.cloudConnectionFactoryPid = cloudConnectionFactoryPid;
        this.defaultCloudEndpointPid = defaultCloudEndpointPid;
        this.cloudEndpointPidRegex = cloudEndpointPidRegex;
    }

    public String getCloudConnectionFactoryPid() {
        return this.cloudConnectionFactoryPid;
    }

    public String getDefaultCloudEndpointPid() {
        return this.defaultCloudEndpointPid;
    }

    public String getCloudEndpointPidRegex() {
        return this.cloudEndpointPidRegex;
    }

}
