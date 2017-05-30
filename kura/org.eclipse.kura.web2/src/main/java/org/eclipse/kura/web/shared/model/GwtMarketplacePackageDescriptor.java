/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtMarketplacePackageDescriptor extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -1993222457417588101L;

    private static final String MIN_KURA_VERSION = "min_kura_version";
    private static final String MAX_KURA_VERSION = "max_kura_version";
    private static final String CURRENT_KURA_VERSION = "current_kura_version";
    private static final String IS_COMPATIBLE = "is_compatible";
    private static final String DP_URL = "dp_url";
    private static final String NODE_ID = "node_id";
    private static final String URL = "url";

    public void setNodeId(String nodeId) {
        set(NODE_ID, nodeId);
    }

    public String getNodeId() {
        return (String) get(NODE_ID);
    }

    public void setUrl(String url) {
        set(URL, url);
    }

    public String getUrl() {
        return (String) get(URL);
    }

    public void setDpUrl(String url) {
        set(DP_URL, url);
    }

    public String getDpUrl() {
        return (String) get(DP_URL);
    }

    public void setMinKuraVersion(String version) {
        set(MIN_KURA_VERSION, version);
    }

    public String getMinKuraVersion() {
        return (String) get(MIN_KURA_VERSION);
    }

    public void setMaxKuraVersion(String version) {
        set(MAX_KURA_VERSION, version);
    }

    public String getMaxKuraVersion() {
        return (String) get(MAX_KURA_VERSION);
    }

    public void setCurrentKuraVersion(String version) {
        set(CURRENT_KURA_VERSION, version);
    }

    public String getCurrentKuraVersion() {
        return (String) get(CURRENT_KURA_VERSION);
    }

    public void setCompatible(boolean isCompatible) {
        set(IS_COMPATIBLE, isCompatible);
    }

    public boolean isCompatible() {
        return (Boolean) get(IS_COMPATIBLE);
    }

}
