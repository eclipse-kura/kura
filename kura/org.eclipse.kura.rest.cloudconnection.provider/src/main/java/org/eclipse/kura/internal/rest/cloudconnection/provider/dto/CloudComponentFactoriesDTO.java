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

import java.util.List;

public class CloudComponentFactoriesDTO {

    private List<String> cloudConnectionFactoryPids;
    private List<CloudEntryDTO> pubSubFactories;

    public CloudComponentFactoriesDTO(List<String> cloudConnectionFactoryPids, List<CloudEntryDTO> pubSubFactories) {
        this.cloudConnectionFactoryPids = cloudConnectionFactoryPids;
        this.pubSubFactories = pubSubFactories;
    }

    public List<String> getCloudConnectionFactoryPids() {
        return cloudConnectionFactoryPids;
    }

    public List<CloudEntryDTO> getPubSubFactories() {
        return pubSubFactories;
    }

}
