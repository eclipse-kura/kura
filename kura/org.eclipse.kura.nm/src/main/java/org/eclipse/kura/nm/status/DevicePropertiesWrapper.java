/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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

import java.util.Optional;

import org.eclipse.kura.nm.enums.NMDeviceType;
import org.freedesktop.dbus.interfaces.Properties;

public class DevicePropertiesWrapper {

    private final Properties deviceProperties;
    private final Optional<Properties> deviceSpecificProperties;
    private final NMDeviceType deviceType;

    public DevicePropertiesWrapper(Properties deviceProps, Optional<Properties> specificProperties,
            NMDeviceType deviceType) {
        this.deviceProperties = deviceProps;
        this.deviceSpecificProperties = specificProperties;
        this.deviceType = deviceType;
    }

    public Properties getDeviceProperties() {
        return this.deviceProperties;
    }

    public Optional<Properties> getDeviceSpecificProperties() {
        return this.deviceSpecificProperties;
    }

    public NMDeviceType getDeviceType() {
        return this.deviceType;
    }

}
