/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.List;

public class GwtCloudComponentFactories implements Serializable {

    private List<String> cloudConnectionFactoryPids;

    private List<GwtCloudEntry> pubSubFactories;

    /**
     * 
     */
    private static final long serialVersionUID = -1968801905394521346L;

    public GwtCloudComponentFactories() {
    }

    public List<String> getCloudConnectionFactoryPids() {
        return cloudConnectionFactoryPids;
    }

    public void setCloudConnectionFactoryPids(final List<String> cloudConnectionFactoryPids) {
        this.cloudConnectionFactoryPids = cloudConnectionFactoryPids;
    }

    public List<GwtCloudEntry> getPubSubFactories() {
        return pubSubFactories;
    }

    public void setPubSubFactories(final List<GwtCloudEntry> pubSubFactories) {
        this.pubSubFactories = pubSubFactories;
    }

}
