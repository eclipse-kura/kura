/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtGroupedNVPair extends GwtBaseModel implements IsSerializable, Serializable {

    private static final long serialVersionUID = 6017065568183482351L;

    public GwtGroupedNVPair() {
    }

    public GwtGroupedNVPair(String group, String name, String value) {
        setGroup(group);
        setName(name);
        setValue(value);
    }

    public void setGroup(String group) {
        set("group", group);
    }

    public String getGroup() {
        return get("group");
    }

    public String getGroupLoc() {
        return get("groupLoc");
    }

    public void setId(String id) {
        set("id", id);
    }

    public String getId() {
        return get("id");
    }

    public void setName(String name) {
        set("name", name);
    }

    public String getName() {
        return get("name");
    }

    public String getNameLoc() {
        return get("nameLoc");
    }

    public void setValue(String value) {
        set("value", value);
    }

    public String getValue() {
        return get("value");
    }

    public void setStatus(String status) {
        set("status", status);
    }

    public String getStatus() {
        return get("status");
    }

    public String getStatusLoc() {
        return get("statusLoc");
    }

    public void setVersion(String version) {
        set("version", version);
    }

    public String getVersion() {
        return get("version");
    }

    public void setType(String type) {
        set("type", type);
    }

    public String getType() {
        return get("type");
    }
}
