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

package org.eclipse.kura.nm.status;

import org.freedesktop.dbus.interfaces.Properties;

public class SimProperties {

    private final Properties properties;
    private final boolean isActive;
    private final boolean isPrimary;

    public SimProperties(Properties properties, boolean isActive, boolean isPrimary) {
        this.properties = properties;
        this.isActive = isActive;
        this.isPrimary = isPrimary;
    }

    public Properties getProperties() {
        return this.properties;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public boolean isPrimary() {
        return this.isPrimary;
    }

}
