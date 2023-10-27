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

import org.eclipse.kura.internal.rest.cloudconnection.provider.CloudConnectionState;
import org.eclipse.kura.internal.rest.cloudconnection.provider.CloudConnectionType;

public class CloudConnectionEntryDTO extends CloudEntryDTO {

    private final String cloudConnectionFactoryPid;
    private CloudConnectionState state;
    private CloudConnectionType connectionType;

    public CloudConnectionEntryDTO(String pid, String cloudConnectionFactoryPid) {
        super(pid);
        this.cloudConnectionFactoryPid = cloudConnectionFactoryPid;
    }

    public String getCloudConnectionFactoryPid() {
        return this.cloudConnectionFactoryPid;
    }

    public CloudConnectionState getState() {
        return this.state;
    }

    public void setState(CloudConnectionState state) {
        this.state = state;
    }

    public CloudConnectionType getConnectionType() {
        return this.connectionType;
    }

    public void setConnectionType(CloudConnectionType connectionType) {
        this.connectionType = connectionType;
    }

}
