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
package org.eclipse.kura.example.ble.tisensortag;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public class BluetoothLeOptions {

    private static final String PROPERTY_SCAN_NAME = "scan_enable";
    private static final String PROPERTY_SCANTIME_NAME = "scan_time";
    private static final String PROPERTY_PERIOD_NAME = "period";
    private static final String PROPERTY_TEMP_NAME = "enableTermometer";
    private static final String PROPERTY_ACC_NAME = "enableAccelerometer";
    private static final String PROPERTY_HUM_NAME = "enableHygrometer";
    private static final String PROPERTY_MAG_NAME = "enableMagnetometer";
    private static final String PROPERTY_PRES_NAME = "enableBarometer";
    private static final String PROPERTY_GYRO_NAME = "enableGyroscope";
    private static final String PROPERTY_OPTO_NAME = "enableLuxometer";
    private static final String PROPERTY_BUTTONS_NAME = "enableButtons";
    private static final String PROPERTY_REDLED_NAME = "switchOnRedLed";
    private static final String PROPERTY_GREENLED_NAME = "switchOnGreenLed";
    private static final String PROPERTY_BUZZER_NAME = "switchOnBuzzer";
    private static final String PROPERTY_DISCOVERY_NAME = "discoverServicesAndCharacteristics";
    private static final String PROPERTY_TOPIC_NAME = "publishTopic";
    private static final String PROPERTY_INAME_NAME = "iname";

    private static final boolean PROPERTY_SCAN_DEFAULT = false;
    private static final int PROPERTY_SCANTIME_DEFAULT = 5;
    private static final int PROPERTY_PERIOD_DEFAULT = 120;
    private static final boolean PROPERTY_TEMP_DEFAULT = false;
    private static final boolean PROPERTY_ACC_DEFAULT = false;
    private static final boolean PROPERTY_HUM_DEFAULT = false;
    private static final boolean PROPERTY_MAG_DEFAULT = false;
    private static final boolean PROPERTY_PRES_DEFAULT = false;
    private static final boolean PROPERTY_GYRO_DEFAULT = false;
    private static final boolean PROPERTY_OPTO_DEFAULT = false;
    private static final boolean PROPERTY_BUTTONS_DEFAULT = false;
    private static final boolean PROPERTY_REDLED_DEFAULT = false;
    private static final boolean PROPERTY_GREENLED_DEFAULT = false;
    private static final boolean PROPERTY_BUZZER_DEFAULT = false;
    private static final boolean PROPERTY_DISCOVERY_DEFAULT = false;
    private static final String PROPERTY_TOPIC_DEFAULT = "data";
    private static final String PROPERTY_INAME_DEFAULT = "hci0";

    private int scantime;
    private int period;
    private String topic;
    private String iname;
    private boolean enableScan;
    private boolean enableTemp;
    private boolean enableAcc;
    private boolean enableHum;
    private boolean enableMag;
    private boolean enablePres;
    private boolean enableGyro;
    private boolean enableOpto;
    private boolean enableButtons;
    private boolean enableRedLed;
    private boolean enableGreenLed;
    private boolean enableBuzzer;
    private boolean enableServicesDiscovery;

    public BluetoothLeOptions(Map<String, Object> properties) {
        requireNonNull(properties, "Required not null");
        this.scantime = getProperty(properties, PROPERTY_SCANTIME_NAME, PROPERTY_SCANTIME_DEFAULT);
        this.period = getProperty(properties, PROPERTY_PERIOD_NAME, PROPERTY_PERIOD_DEFAULT);
        this.topic = getProperty(properties, PROPERTY_TOPIC_NAME, PROPERTY_TOPIC_DEFAULT);
        this.iname = getProperty(properties, PROPERTY_INAME_NAME, PROPERTY_INAME_DEFAULT);
        this.enableScan = getProperty(properties, PROPERTY_SCAN_NAME, PROPERTY_SCAN_DEFAULT);
        this.enableTemp = getProperty(properties, PROPERTY_TEMP_NAME, PROPERTY_TEMP_DEFAULT);
        this.enableAcc = getProperty(properties, PROPERTY_ACC_NAME, PROPERTY_ACC_DEFAULT);
        this.enableHum = getProperty(properties, PROPERTY_HUM_NAME, PROPERTY_HUM_DEFAULT);
        this.enableMag = getProperty(properties, PROPERTY_MAG_NAME, PROPERTY_MAG_DEFAULT);
        this.enablePres = getProperty(properties, PROPERTY_PRES_NAME, PROPERTY_PRES_DEFAULT);
        this.enableGyro = getProperty(properties, PROPERTY_GYRO_NAME, PROPERTY_GYRO_DEFAULT);
        this.enableOpto = getProperty(properties, PROPERTY_OPTO_NAME, PROPERTY_OPTO_DEFAULT);
        this.enableButtons = getProperty(properties, PROPERTY_BUTTONS_NAME, PROPERTY_BUTTONS_DEFAULT);
        this.enableRedLed = getProperty(properties, PROPERTY_REDLED_NAME, PROPERTY_REDLED_DEFAULT);
        this.enableGreenLed = getProperty(properties, PROPERTY_GREENLED_NAME, PROPERTY_GREENLED_DEFAULT);
        this.enableBuzzer = getProperty(properties, PROPERTY_BUZZER_NAME, PROPERTY_BUZZER_DEFAULT);
        this.enableServicesDiscovery = getProperty(properties, PROPERTY_DISCOVERY_NAME, PROPERTY_DISCOVERY_DEFAULT);
    }

    public int getScantime() {
        return this.scantime;
    }

    public int getPeriod() {
        return this.period;
    }

    public String getTopic() {
        return this.topic;
    }

    public String getIname() {
        return this.iname;
    }

    public boolean isEnableScan() {
        return this.enableScan;
    }

    public boolean isEnableTemp() {
        return this.enableTemp;
    }

    public boolean isEnableAcc() {
        return this.enableAcc;
    }

    public boolean isEnableHum() {
        return this.enableHum;
    }

    public boolean isEnableMag() {
        return this.enableMag;
    }

    public boolean isEnablePres() {
        return this.enablePres;
    }

    public boolean isEnableGyro() {
        return this.enableGyro;
    }

    public boolean isEnableOpto() {
        return this.enableOpto;
    }

    public boolean isEnableButtons() {
        return this.enableButtons;
    }

    public boolean isEnableRedLed() {
        return this.enableRedLed;
    }

    public boolean isEnableGreenLed() {
        return this.enableGreenLed;
    }

    public boolean isEnableBuzzer() {
        return this.enableBuzzer;
    }

    public boolean isEnableServicesDiscovery() {
        return this.enableServicesDiscovery;
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