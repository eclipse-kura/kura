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
