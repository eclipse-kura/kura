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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class GwtClientExtensionBundle extends KuraBaseModel implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 7106592629491998207L;

    public GwtClientExtensionBundle() {
    }

    public GwtClientExtensionBundle(Map<String, String> properties, String entryPointUrl) {
        setExtensionBundleProperties(new HashMap<>(properties));
        setEntryPointUrl(entryPointUrl);
    }

    public void setExtensionBundleProperties(final Map<String, String> properties) {
        set("properties", properties);
    }

    public void setEntryPointUrl(final String entryPointUrl) {
        set("entryPointUrl", entryPointUrl);
    }

    public Map<String, String> getExtensionBundleProperties() {
        return get("properties");
    }

    public String getEntryPointUrl() {
        return get("entryPointUrl");
    }

}
