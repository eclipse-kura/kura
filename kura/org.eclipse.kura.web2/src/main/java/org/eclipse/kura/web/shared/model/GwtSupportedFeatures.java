/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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

public class GwtSupportedFeatures extends GwtBaseModel implements Serializable {

    private static final String ASSET_AVAILABLE = "assetAvailable";
    private static final String DRIVER_SERVICES_AVAILABLE = "driverServicesAvailable";
    private static final String WIRES_SERVICES_AVAILABLE = "wiresServicesAvailable";
    private static final String COMMAND_SERVICE_AVAILABLE = "commandServiceAvailable";

    private static final long serialVersionUID = 8446416262929849486L;

    public boolean areWiresServicesAvailable() {
        return getBooleanProperty(WIRES_SERVICES_AVAILABLE);
    }

    public boolean areDriverServicesAvailable() {
        return getBooleanProperty(DRIVER_SERVICES_AVAILABLE);
    }

    public boolean isAssetAvailable() {
        return getBooleanProperty(ASSET_AVAILABLE);
    }

    public boolean isCommandServiceAvailable() {
        return getBooleanProperty(COMMAND_SERVICE_AVAILABLE);
    }

    public void setDriverServicesAvailable(final boolean available) {
        set(DRIVER_SERVICES_AVAILABLE, available);
    }

    public void setWiresServicesAvailable(final boolean available) {
        set(WIRES_SERVICES_AVAILABLE, available);
    }

    public void setAssetAvailable(final boolean available) {
        set(ASSET_AVAILABLE, available);
    }

    public void setCommandServiceAvailable(final boolean available) {
        set(COMMAND_SERVICE_AVAILABLE, available);
    }

    private boolean getBooleanProperty(final String key) {
        final Object value = get(key);

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            return false;
        }
    }
}
