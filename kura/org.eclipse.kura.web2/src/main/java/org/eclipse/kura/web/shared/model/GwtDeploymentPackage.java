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
import java.util.List;

public class GwtDeploymentPackage extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -7648638193931336835L;

    // Needed to prevent serialization errors
    @SuppressWarnings("unused")
    private GwtBundleInfo unused;

    public void setName(String name) {
        set("name", name);
    }

    public String getName() {
        return (String) get("name");
    }

    public void setVersion(String version) {
        set("version", version);
    }

    public String getVersion() {
        return (String) get("version");
    }

    public List<GwtBundleInfo> getBundleInfos() {
        return get("bundles");
    }

    public void setBundleInfos(List<GwtBundleInfo> bundleInfos) {
        set("bundles", bundleInfos);
    }
}
