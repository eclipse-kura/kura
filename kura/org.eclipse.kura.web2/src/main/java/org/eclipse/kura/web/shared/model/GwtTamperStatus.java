/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtTamperStatus implements IsSerializable {

    private boolean isTampered;
    private String displayName;
    private Map<String, String> properties;

    public GwtTamperStatus() {
        super();
    }

    public GwtTamperStatus(final String displayName, final boolean isTampered, final Map<String, String> properties) {
        this.displayName = displayName;
        this.isTampered = isTampered;
        this.properties = properties;

    }

    public boolean isTampered() {
        return isTampered;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getDisplayName() {
        return displayName;
    }
}
