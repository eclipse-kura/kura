/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

public class GwtCloudPubSubEntry extends GwtCloudEntry {

    /**
     * 
     */
    private static final long serialVersionUID = -6307778385963106457L;

    public GwtCloudPubSubEntry() {
    }

    public String getCloudConnectionPid() {
        return get("cloudConnectionPid");
    }

    public void setCloudConnectionPid(String cloudConnectionPid) {
        set("cloudConnectionPid", cloudConnectionPid);
    }

    public Type getType() {
        return Type.valueOf(get("type"));
    }

    public void setType(final Type type) {
        set("type", type.name());
    }

    public enum Type {
        PUBLISHER,
        SUBSCRIBER
    }
}
