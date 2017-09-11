/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtDriverAssetInfo extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = 1853917206097400018L;

    public String getInstancePid() {
        return super.get("instancePid");
    }

    public String getType() {
        return super.get("type");
    }

    public String getFactoryPid() {
        return super.get("factoryPid");
    }

    public void setInstancePid(String instancePid) {
        super.set("instancePid", instancePid);
    }

    public void setType(String type) {
        super.set("type", type);
    }

    public void setFactoryPid(String factoryPid) {
        super.set("factoryPid", factoryPid);
    }
}
