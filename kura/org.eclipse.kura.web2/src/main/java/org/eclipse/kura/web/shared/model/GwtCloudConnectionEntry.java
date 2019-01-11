/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

public class GwtCloudConnectionEntry extends GwtCloudEntry {

    private static final long serialVersionUID = 3373858744219238675L;

    public GwtCloudConnectionEntry() {
    }

    public GwtCloudConnectionState getState() {
        return GwtCloudConnectionState.valueOf(get("state"));
    }

    public void setState(GwtCloudConnectionState state) {
        set("state", state.name());
    }
    
    public GwtCloudConnectionType getConnectionType() {
        return GwtCloudConnectionType.valueOf(get("type"));
    }
    
    public void setConnectionType(GwtCloudConnectionType type) {
        set("type", type.name());
    }

    public String getCloudConnectionFactoryPid() {
        return get("cloudConnectionFactoryPid");
    }

    public void setCloudConnectionFactoryPid(String cloudConnectionFactoryPid) {
        set("cloudConnectionFactoryPid", cloudConnectionFactoryPid);
    }

    public enum GwtCloudConnectionState {
        UNREGISTERED,
        DISCONNECTED,
        CONNECTED;
    }
    
    public enum GwtCloudConnectionType {
        ENDPOINT,
        CONNECTION;
    }
}
