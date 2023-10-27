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

import org.eclipse.kura.internal.rest.cloudconnection.provider.CloudPubSubType;

public class CloudPubSubEntryDTO extends CloudEntryDTO {

    private final String cloudConnectionPid;
    private final CloudPubSubType type;

    public CloudPubSubEntryDTO(String pid, String factoryPid, String cloudConnectionPid, CloudPubSubType type) {
        super(pid, factoryPid);
        this.cloudConnectionPid = cloudConnectionPid;
        this.type = type;
    }

    public String getCloudConnectionPid() {
        return this.cloudConnectionPid;
    }

    public CloudPubSubType getType() {
        return this.type;
    }

}
