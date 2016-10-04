/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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

import java.util.UUID;

public class TiSensorTagGatt {

    // These values are for TI CC2541 and TI CC2650
    // Refer to http://processors.wiki.ti.com/images/archive/a/a8/20130111154127!BLE_SensorTag_GATT_Server.pdf for the
    // CC2541
    // and http://www.ti.com/ww/en/wireless_connectivity/sensortag2015/tearDown.html#main for the CC2560

    // Firmware revision
    public static final String HANDLE_FIRMWARE_REVISION_2541 = "0x0018";
    public static final String HANDLE_FIRMWARE_REVISION_2650 = "0x0014";

    // Temperature sensor
    // CC2541
    public static final String HANDLE_TEMP_SENSOR_VALUE_2541 = "0x0025";
    public static final String HANDLE_TEMP_SENSOR_NOTIFICATION_2541 = "0x0026";
    public static final String HANDLE_TEMP_SENSOR_ENABLE_2541 = "0x0029";
    // CC 2650
    public static final String HANDLE_TEMP_SENSOR_VALUE_2650 = "0x0021";
    public static final String HANDLE_TEMP_SENSOR_NOTIFICATION_2650 = "0x0022";
    public static final String HANDLE_TEMP_SENSOR_ENABLE_2650 = "0x0024";
    public static final String HANDLE_TEMP_SENSOR_PERIOD_2650 = "0x0026";

    public static final UUID UUID_TEMP_SENSOR_VALUE = UUID.fromString("f000aa01-0451-4000-b000-000000000000");
    public static final UUID UUID_TEMP_SENSOR_ENABLE = UUID.fromString("f000aa02-0451-4000-b000-000000000000");
    public static final UUID UUID_TEMP_SENSOR_PERIOD = UUID.fromString("f000aa03-0451-4000-b000-000000000000");

    // Accelerometer sensor
    // CC2541
    public static final String HANDLE_ACC_SENSOR_VALUE_2541 = "0x002d";
    public static final String HANDLE_ACC_SENSOR_NOTIFICATION_2541 = "0x002e";
    public static final String HANDLE_ACC_SENSOR_ENABLE_2541 = "0x0031";
    public static final String HANDLE_ACC_SENSOR_PERIOD_2541 = "0x0034";

    public static final UUID UUID_ACC_SENSOR_VALUE = UUID.fromString("f000aa11-0451-4000-b000-000000000000");
    public static final UUID UUID_ACC_SENSOR_ENABLE = UUID.fromString("f000aa12-0451-4000-b000-000000000000");
    public static final UUID UUID_ACC_SENSOR_PERIOD = UUID.fromString("f000aa13-0451-4000-b000-000000000000");

    // Humidity sensor
    // CC2541
    public static final String HANDLE_HUM_SENSOR_VALUE_2541 = "0x0038";
    public static final String HANDLE_HUM_SENSOR_NOTIFICATION_2541 = "0x0039";
    public static final String HANDLE_HUM_SENSOR_ENABLE_2541 = "0x003c";
    // CC2650
    public static final String HANDLE_HUM_SENSOR_VALUE_2650 = "0x0029";
    public static final String HANDLE_HUM_SENSOR_NOTIFICATION_2650 = "0x002a";
    public static final String HANDLE_HUM_SENSOR_ENABLE_2650 = "0x002c";
    public static final String HANDLE_HUM_SENSOR_PERIOD_2650 = "0x002E";

    public static final UUID UUID_HUM_SENSOR_VALUE = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    public static final UUID UUID_HUM_SENSOR_ENABLE = UUID.fromString("f000aa22-0451-4000-b000-000000000000");
    public static final UUID UUID_HUM_SENSOR_PERIOD = UUID.fromString("f000aa23-0451-4000-b000-000000000000");

    // Magnetometer sensor
    // CC2541
    public static final String HANDLE_MAG_SENSOR_VALUE_2541 = "0x0040";
    public static final String HANDLE_MAG_SENSOR_NOTIFICATION_2541 = "0x0041";
    public static final String HANDLE_MAG_SENSOR_ENABLE_2541 = "0x0044";
    public static final String HANDLE_MAG_SENSOR_PERIOD_2541 = "0x0047";

    public static final UUID UUID_MAG_SENSOR_VALUE = UUID.fromString("f000aa31-0451-4000-b000-000000000000");
    public static final UUID UUID_MAG_SENSOR_ENABLE = UUID.fromString("f000aa32-0451-4000-b000-000000000000");
    public static final UUID UUID_MAG_SENSOR_PERIOD = UUID.fromString("f000aa33-0451-4000-b000-000000000000");

    // Pressure sensor
    // CC2541 (Firm. 1.4)
    public static final String HANDLE_PRE_SENSOR_VALUE_2541_1_4 = "0x004b";
    public static final String HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_4 = "0x004c";
    public static final String HANDLE_PRE_SENSOR_ENABLE_2541_1_4 = "0x004f";
    public static final String HANDLE_PRE_CALIBRATION_2541_1_4 = "0x0052";
    // CC2541 (Firm. 1.5)
    public static final String HANDLE_PRE_SENSOR_VALUE_2541_1_5 = "0x0051";
    public static final String HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_5 = "0x0052";
    public static final String HANDLE_PRE_SENSOR_ENABLE_2541_1_5 = "0x0055";
    public static final String HANDLE_PRE_CALIBRATION_2541_1_5 = "0x005b";
    // CC2650
    public static final String HANDLE_PRE_SENSOR_VALUE_2650 = "0x0031";
    public static final String HANDLE_PRE_SENSOR_NOTIFICATION_2650 = "0x0032";
    public static final String HANDLE_PRE_SENSOR_ENABLE_2650 = "0x0034";
    public static final String HANDLE_PRE_SENSOR_PERIOD_2650 = "0x0036";

    public static final UUID UUID_PRE_SENSOR_VALUE = UUID.fromString("f000aa41-0451-4000-b000-000000000000");
    public static final UUID UUID_PRE_SENSOR_ENABLE = UUID.fromString("f000aa42-0451-4000-b000-000000000000");
    public static final UUID UUID_PRE_SENSOR_CALIBRATION = UUID.fromString("f000aa43-0451-4000-b000-000000000000");
    public static final UUID UUID_PRE_SENSOR_PERIOD = UUID.fromString("f000aa44-0451-4000-b000-000000000000");

    // Gyroscope sensor
    // CC2541
    public static final String HANDLE_GYR_SENSOR_VALUE_2541 = "0x0057";
    public static final String HANDLE_GYR_SENSOR_NOTIFICATION_2541 = "0x0058";
    public static final String HANDLE_GYR_SENSOR_ENABLE_2541 = "0x005b";

    public static final UUID UUID_GYR_SENSOR_VALUE = UUID.fromString("f000aa51-0451-4000-b000-000000000000");
    public static final UUID UUID_GYR_SENSOR_ENABLE = UUID.fromString("f000aa52-0451-4000-b000-000000000000");

    // Keys
    // CC2541
    public static final String HANDLE_KEYS_STATUS_2541 = "0x005f";
    public static final String HANDLE_KEYS_NOTIFICATION_2541 = "0x0060";
    // CC2650
    public static final String HANDLE_KEYS_STATUS_2650 = "0x0049";
    public static final String HANDLE_KEYS_NOTIFICATION_2650 = "0x004A";

    public static final UUID UUID_KEYS_STATUS = UUID.fromString("f000ffe1-0451-4000-b000-000000000000");

    // Ambient Light sensor
    // CC2650
    public static final String HANDLE_OPTO_SENSOR_VALUE_2650 = "0x0041";
    public static final String HANDLE_OPTO_SENSOR_NOTIFICATION_2650 = "0x0042";
    public static final String HANDLE_OPTO_SENSOR_ENABLE_2650 = "0x0044";
    public static final String HANDLE_OPTO_SENSOR_PERIOD_2650 = "0x0046";

    public static final UUID UUID_OPTO_SENSOR_VALUE = UUID.fromString("f000aa71-0451-4000-b000-000000000000");
    public static final UUID UUID_OPTO_SENSOR_ENABLE = UUID.fromString("f000aa72-0451-4000-b000-000000000000");
    public static final UUID UUID_OPTO_SENSOR_PERIOD = UUID.fromString("f000aa73-0451-4000-b000-000000000000");

    // Movement sensor (accelerometer, gyroscope and magnetometer)
    // CC2560
    public static final String HANDLE_MOV_SENSOR_VALUE_2650 = "0x0039";
    public static final String HANDLE_MOV_SENSOR_NOTIFICATION_2650 = "0x003A";
    public static final String HANDLE_MOV_SENSOR_ENABLE_2650 = "0x003C";
    public static final String HANDLE_MOV_SENSOR_PERIOD_2650 = "0x003E";

    public static final UUID UUID_MOV_SENSOR_VALUE = UUID.fromString("f000aa81-0451-4000-b000-000000000000");
    public static final UUID UUID_MOV_SENSOR_ENABLE = UUID.fromString("f000aa82-0451-4000-b000-000000000000");
    public static final UUID UUID_MOV_SENSOR_PERIOD = UUID.fromString("f000aa83-0451-4000-b000-000000000000");

    // IO Service (leds and buzzer)
    // CC2560
    public static final String HANDLE_IO_SENSOR_VALUE_2650 = "0x004E";
    public static final String HANDLE_IO_SENSOR_ENABLE_2650 = "0x0050";

    public static final UUID UUID_IO_SENSOR_VALUE = UUID.fromString("f000aa65-0451-4000-b000-000000000000");
    public static final UUID UUID_IO_SENSOR_ENABLE = UUID.fromString("f000aa66-0451-4000-b000-000000000000");
}
