/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.example.ibeacon.advertiser;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.UUID;

public class IBeaconAdvertiserOptions {

    private static final String PROPERTY_ENABLE = "enable.advertising";
    private static final String PROPERTY_MIN_INTERVAL = "min.beacon.interval";
    private static final String PROPERTY_MAX_INTERVAL = "max.beacon.interval";
    private static final String PROPERTY_UUID = "uuid";
    private static final String PROPERTY_MAJOR = "major";
    private static final String PROPERTY_MINOR = "minor";
    private static final String PROPERTY_TX_POWER = "tx.power";
    private static final String PROPERTY_INAME = "iname";

    private static final boolean PROPERTY_ENABLE_DEFAULT = false;
    private static final int PROPERTY_MIN_INTERVAL_DEFAULT = 1000;
    private static final int PROPERTY_MAX_INTERVAL_DEFAULT = 1000;
    private static final String PROPERTY_UUID_DEFAULT = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final int PROPERTY_MAJOR_DEFAULT = 0;
    private static final int PROPERTY_MINOR_DEFAULT = 0;
    private static final int PROPERTY_TX_POWER_DEFAULT = 0;
    private static final String PROPERTY_INAME_DEFAULT = "hci0";

    private final boolean enable;
    private final Integer minInterval;
    private final Integer maxInterval;
    private UUID uuid;
    private final int major;
    private final int minor;
    private final Integer txPower;
    private final String iname;

    public IBeaconAdvertiserOptions(Map<String, Object> properties) {
        requireNonNull(properties, "Required not null");
        this.enable = getProperty(properties, PROPERTY_ENABLE, PROPERTY_ENABLE_DEFAULT);
        this.minInterval = (int) (getProperty(properties, PROPERTY_MIN_INTERVAL, PROPERTY_MIN_INTERVAL_DEFAULT)
                / 0.625);
        this.maxInterval = (int) (getProperty(properties, PROPERTY_MAX_INTERVAL, PROPERTY_MAX_INTERVAL_DEFAULT)
                / 0.625);
        this.major = getProperty(properties, PROPERTY_MAJOR, PROPERTY_MAJOR_DEFAULT);
        this.minor = getProperty(properties, PROPERTY_MINOR, PROPERTY_MINOR_DEFAULT);
        this.txPower = getProperty(properties, PROPERTY_TX_POWER, PROPERTY_TX_POWER_DEFAULT);
        this.iname = getProperty(properties, PROPERTY_INAME, PROPERTY_INAME_DEFAULT);

        String uuidString = getProperty(properties, PROPERTY_UUID, PROPERTY_UUID_DEFAULT);
        if (uuidString.trim().replace("-", "").length() != 32) {
            this.uuid = UUID.fromString(PROPERTY_UUID_DEFAULT);
        } else {
            this.uuid = UUID.fromString(uuidString);
        }
    }

    public boolean isEnabled() {
        return this.enable;
    }

    public Integer getMinInterval() {
        return this.minInterval;
    }

    public Integer getMaxInterval() {
        return this.maxInterval;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public Integer getMajor() {
        return this.major;
    }

    public Integer getMinor() {
        return this.minor;
    }

    public Integer getTxPower() {
        return this.txPower;
    }

    public String getIname() {
        return this.iname;
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
