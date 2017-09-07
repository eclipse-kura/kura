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
package org.eclipse.kura.example.ble.tisensortag.tinyb;

import java.util.UUID;

public class TiSensorTagGatt {

    // These values are for TI CC2541 and TI CC2650
    // Refer to http://processors.wiki.ti.com/images/archive/a/a8/20130111154127!BLE_SensorTag_GATT_Server.pdf for the
    // CC2541
    // and http://www.ti.com/ww/en/wireless_connectivity/sensortag2015/tearDown.html#main for the CC2560

    // Firmware revision
    public static final UUID UUID_DEVINFO_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DEVINFO_FIRMWARE_REVISION = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");

    // Temperature sensor
    public static final UUID UUID_TEMP_SENSOR_SERVICE = UUID.fromString("f000aa00-0451-4000-b000-000000000000");
    public static final UUID UUID_TEMP_SENSOR_VALUE = UUID.fromString("f000aa01-0451-4000-b000-000000000000");
    public static final UUID UUID_TEMP_SENSOR_ENABLE = UUID.fromString("f000aa02-0451-4000-b000-000000000000");
    public static final UUID UUID_TEMP_SENSOR_PERIOD = UUID.fromString("f000aa03-0451-4000-b000-000000000000");

    // Accelerometer sensor
    // CC2541
    public static final UUID UUID_ACC_SENSOR_SERVICE = UUID.fromString("f000aa10-0451-4000-b000-000000000000");
    public static final UUID UUID_ACC_SENSOR_VALUE = UUID.fromString("f000aa11-0451-4000-b000-000000000000");
    public static final UUID UUID_ACC_SENSOR_ENABLE = UUID.fromString("f000aa12-0451-4000-b000-000000000000");
    public static final UUID UUID_ACC_SENSOR_PERIOD = UUID.fromString("f000aa13-0451-4000-b000-000000000000");

    // Humidity sensor
    public static final UUID UUID_HUM_SENSOR_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    public static final UUID UUID_HUM_SENSOR_VALUE = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    public static final UUID UUID_HUM_SENSOR_ENABLE = UUID.fromString("f000aa22-0451-4000-b000-000000000000");
    public static final UUID UUID_HUM_SENSOR_PERIOD = UUID.fromString("f000aa23-0451-4000-b000-000000000000");

    // Magnetometer sensor
    // CC2541
    public static final UUID UUID_MAG_SENSOR_SERVICE = UUID.fromString("f000aa30-0451-4000-b000-000000000000");
    public static final UUID UUID_MAG_SENSOR_VALUE = UUID.fromString("f000aa31-0451-4000-b000-000000000000");
    public static final UUID UUID_MAG_SENSOR_ENABLE = UUID.fromString("f000aa32-0451-4000-b000-000000000000");
    public static final UUID UUID_MAG_SENSOR_PERIOD = UUID.fromString("f000aa33-0451-4000-b000-000000000000");

    // Pressure sensor
    public static final UUID UUID_PRE_SENSOR_SERVICE = UUID.fromString("f000aa40-0451-4000-b000-000000000000");
    public static final UUID UUID_PRE_SENSOR_VALUE = UUID.fromString("f000aa41-0451-4000-b000-000000000000");
    public static final UUID UUID_PRE_SENSOR_ENABLE = UUID.fromString("f000aa42-0451-4000-b000-000000000000");
    public static final UUID UUID_PRE_SENSOR_CALIBRATION = UUID.fromString("f000aa43-0451-4000-b000-000000000000");
    public static final UUID UUID_PRE_SENSOR_PERIOD = UUID.fromString("f000aa44-0451-4000-b000-000000000000");

    // Gyroscope sensor
    // CC2541
    public static final UUID UUID_GYR_SENSOR_SERVICE = UUID.fromString("f000aa50-0451-4000-b000-000000000000");
    public static final UUID UUID_GYR_SENSOR_VALUE = UUID.fromString("f000aa51-0451-4000-b000-000000000000");
    public static final UUID UUID_GYR_SENSOR_ENABLE = UUID.fromString("f000aa52-0451-4000-b000-000000000000");

    // Keys
    public static final UUID UUID_KEYS_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_KEYS_STATUS = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    // Ambient Light sensor
    // CC2650
    public static final UUID UUID_OPTO_SENSOR_SERVICE = UUID.fromString("f000aa70-0451-4000-b000-000000000000");
    public static final UUID UUID_OPTO_SENSOR_VALUE = UUID.fromString("f000aa71-0451-4000-b000-000000000000");
    public static final UUID UUID_OPTO_SENSOR_ENABLE = UUID.fromString("f000aa72-0451-4000-b000-000000000000");
    public static final UUID UUID_OPTO_SENSOR_PERIOD = UUID.fromString("f000aa73-0451-4000-b000-000000000000");

    // Movement sensor (accelerometer, gyroscope and magnetometer)
    // CC2560
    public static final UUID UUID_MOV_SENSOR_SERVICE = UUID.fromString("f000aa80-0451-4000-b000-000000000000");
    public static final UUID UUID_MOV_SENSOR_VALUE = UUID.fromString("f000aa81-0451-4000-b000-000000000000");
    public static final UUID UUID_MOV_SENSOR_ENABLE = UUID.fromString("f000aa82-0451-4000-b000-000000000000");
    public static final UUID UUID_MOV_SENSOR_PERIOD = UUID.fromString("f000aa83-0451-4000-b000-000000000000");

    // IO Service (leds and buzzer)
    // CC2560
    public static final UUID UUID_IO_SENSOR_SERVICE = UUID.fromString("f000aa64-0451-4000-b000-000000000000");
    public static final UUID UUID_IO_SENSOR_VALUE = UUID.fromString("f000aa65-0451-4000-b000-000000000000");
    public static final UUID UUID_IO_SENSOR_ENABLE = UUID.fromString("f000aa66-0451-4000-b000-000000000000");

}
