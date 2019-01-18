/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.example.eddystone.advertiser;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public class EddystoneAdvertiserOptions {

    private static final String PROPERTY_ENABLE = "enable.advertising";
    private static final String PROPERTY_MIN_INTERVAL = "min.beacon.interval";
    private static final String PROPERTY_MAX_INTERVAL = "max.beacon.interval";
    private static final String PROPERTY_TYPE = "eddystone.type";
    private static final String PROPERTY_NAMESPACE = "eddystone.uid.namespace";
    private static final String PROPERTY_INSTANCE = "eddystone.uid.instance";
    private static final String PROPERTY_URL = "eddystone.url";
    private static final String PROPERTY_TX_POWER = "tx.power";
    private static final String PROPERTY_INAME = "iname";

    private static final boolean PROPERTY_ENABLE_DEFAULT = false;
    private static final int PROPERTY_MIN_INTERVAL_DEFAULT = 1000;
    private static final int PROPERTY_MAX_INTERVAL_DEFAULT = 1000;
    private static final String PROPERTY_TYPE_DEFAULT = "UID";
    private static final String PROPERTY_NAMESPACE_DEFAULT = "00112233445566778899";
    private static final String PROPERTY_INSTANCE_DEFAULT = "001122334455";
    private static final String PROPERTY_URL_DEFAULT = "http://www.eclipse.org/kura";
    private static final int PROPERTY_TX_POWER_DEFAULT = 0;
    private static final String PROPERTY_INAME_DEFAULT = "hci0";

    private static final int PROPERTY_TX_POWER_MAX = 126;
    private static final int PROPERTY_TX_POWER_MIN = -127;

    private final boolean enable;
    private final Integer minInterval;
    private final Integer maxInterval;
    private final String eddystoneFrametype;
    private final String uidNamespace;
    private final String uidInstance;
    private final String urlUrl;
    private final Integer txPower;
    private final String iname;

    public EddystoneAdvertiserOptions(Map<String, Object> properties) {
        requireNonNull(properties, "Required not null");
        this.enable = getProperty(properties, PROPERTY_ENABLE, PROPERTY_ENABLE_DEFAULT);
        this.minInterval = (int) (getProperty(properties, PROPERTY_MIN_INTERVAL, PROPERTY_MIN_INTERVAL_DEFAULT)
                / 0.625);
        this.maxInterval = (int) (getProperty(properties, PROPERTY_MAX_INTERVAL, PROPERTY_MAX_INTERVAL_DEFAULT)
                / 0.625);
        this.eddystoneFrametype = getProperty(properties, PROPERTY_TYPE, PROPERTY_TYPE_DEFAULT);
        this.urlUrl = getProperty(properties, PROPERTY_URL, PROPERTY_URL_DEFAULT);

        int txPowerInt = getProperty(properties, PROPERTY_TX_POWER, PROPERTY_TX_POWER_DEFAULT);
        if (txPowerInt <= PROPERTY_TX_POWER_MAX && txPowerInt >= PROPERTY_TX_POWER_MIN) {
            this.txPower = getProperty(properties, PROPERTY_TX_POWER, PROPERTY_TX_POWER_DEFAULT);
        } else {
            if (txPowerInt > PROPERTY_TX_POWER_MAX) {
                this.txPower = PROPERTY_TX_POWER_MAX;
            } else {
                this.txPower = PROPERTY_TX_POWER_MIN;
            }
        }

        this.iname = getProperty(properties, PROPERTY_INAME, PROPERTY_INAME_DEFAULT);
        this.uidNamespace = setInPropertyLimit(properties, PROPERTY_NAMESPACE, PROPERTY_NAMESPACE_DEFAULT, 20);
        this.uidInstance = setInPropertyLimit(properties, PROPERTY_INSTANCE, PROPERTY_INSTANCE_DEFAULT, 12);
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

    public String getEddystoneFrametype() {
        return this.eddystoneFrametype;
    }

    public String getUidNamespace() {
        return this.uidNamespace;
    }

    public String getUidInstance() {
        return this.uidInstance;
    }

    public String getUrlUrl() {
        return this.urlUrl;
    }

    public Integer getTxPower() {
        return this.txPower;
    }

    public String getIname() {
        return this.iname;
    }

    private String setInPropertyLimit(Map<String, Object> properties, String propertyName, String propertyDefault,
            int lengthLimit) {
        String property = getProperty(properties, propertyName, propertyDefault);
        if (property.length() == lengthLimit) {
            return setInHex(property, propertyDefault);
        } else {
            return propertyDefault;
        }
    }

    private String setInHex(String value, String defaultValue) {
        if (!value.matches("^[0-9a-fA-F]+$")) {
            return defaultValue;
        } else {
            return value;
        }
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
