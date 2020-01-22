/**
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.internal.driver.ble.xdk;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

public class XdkOptions {

    private static final String INAME = "iname";
    private final Map<String, Object> properties;

    private static final String PROPERTY_QUATERNION = "enableRotationQuaternion";
    private static final boolean PROPERTY_QUATERNION_DEFAULT = false;

    private static final String PROPERTY_SAMPLE_RATE = "configureSampleRateHz";
    private static final int PROPERTY_SAMPLE_RATE_DEFAULT = 10;

    private final boolean enableQuaternion;
    private final int configSampleRate;

    /**
     * Instantiates a new BLE Xdk options.
     *
     * @param properties
     *            the properties
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    XdkOptions(final Map<String, Object> properties) {

        requireNonNull(properties, "Properties cannot be null");

        this.properties = properties;

        this.enableQuaternion = getProperty(properties, PROPERTY_QUATERNION, PROPERTY_QUATERNION_DEFAULT);
        this.configSampleRate = getProperty(properties, PROPERTY_SAMPLE_RATE, PROPERTY_SAMPLE_RATE_DEFAULT);
    }

    /**
     * Returns the Bluetooth Interface Name to be used
     *
     * @return the Bluetooth Adapter name (i.e. hci0)
     */
    String getBluetoothInterfaceName() {
        String interfaceName = null;
        final Object iname = this.properties.get(INAME);
        if (nonNull(iname) && iname instanceof String) {
            interfaceName = iname.toString();
        }
        return interfaceName;
    }

    public boolean isEnableRotationQuaternion() {
        return this.enableQuaternion;
    }

    public int isConfigSampleRate() {
        return this.configSampleRate;
    }

    @SuppressWarnings("unchecked")
    private <T> T getProperty(Map<String, Object> properties, String propertyName, T defaultValue) {
        Object prop = properties.getOrDefault(propertyName, defaultValue);
        if (prop != null && prop.getClass().isAssignableFrom(defaultValue.getClass())) {
            return (T) prop;
        } else {
            return defaultValue;
        }
    }

}