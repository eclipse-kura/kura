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

import java.util.ArrayList;
import java.util.List;

public class CloudComponentInstances {

    private List<CloudEndpointInstance> cloudEndpointInstances = new ArrayList<>();
    private List<PubSubInstance> pubsubInstances = new ArrayList<>();

    public CloudComponentInstances(List<CloudEndpointInstance> cloudEndpointInstances,
            List<PubSubInstance> pubsubInstances) {
        super();
        this.cloudEndpointInstances = cloudEndpointInstances;
        this.pubsubInstances = pubsubInstances;
    }

    public List<CloudEndpointInstance> getCloudEndpointInstances() {
        return this.cloudEndpointInstances;
    }

    public List<PubSubInstance> getPubsubInstances() {
        return this.pubsubInstances;
    }

}
