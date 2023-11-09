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

public class CloudComponentFactories {

    private final List<CloudConnectionFactoryInfo> cloudConnectionFactories;
    private final List<PubSubFactoryInfo> pubSubFactories;

    public CloudComponentFactories(List<CloudConnectionFactoryInfo> cloudConnectionFactories,
            List<PubSubFactoryInfo> pubSubFactories) {

        this.cloudConnectionFactories = cloudConnectionFactories;
        this.pubSubFactories = pubSubFactories;
    }

    public List<CloudConnectionFactoryInfo> getCloudConnectionFactories() {
        return this.cloudConnectionFactories;
    }

    public List<PubSubFactoryInfo> getPubSubFactories() {
        return this.pubSubFactories;
    }

}
