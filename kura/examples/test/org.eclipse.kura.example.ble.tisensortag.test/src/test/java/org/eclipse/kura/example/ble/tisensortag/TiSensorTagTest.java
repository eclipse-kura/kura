/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.example.ble.tisensortag;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;
import org.eclipse.kura.bluetooth.BluetoothGattSecurityLevel;
import org.eclipse.kura.bluetooth.BluetoothGattService;
import org.eclipse.kura.example.testutil.TestUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TiSensorTagTest {

    private static final String V_1_4 = "312E34xxx";
    private static final String V_1_5 = "312E35xxx";
    private static final double EPS = 0.01;
    public static final String ADDRESS = "12:34:56:78:90:AB";

    @Test
    public void testConnectionException() throws KuraException {
        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, true, false);
        BluetoothGatt gattMock = builder.getGattMock();
        TiSensorTag tag = builder.build(false);

        doThrow(new KuraException(KuraErrorCode.BLE_IO_ERROR)).when(gattMock).connect(adapter);

        boolean connected = tag.connect(adapter);

        assertFalse(connected);

        verify(gattMock, times(1)).connect(adapter);
        verify(gattMock, times(1)).disconnect();

        tag.disconnect();

        verify(gattMock, times(2)).disconnect();
    }

    @Test
    public void testConnectionFailure() throws KuraException {
        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, true, false);
        BluetoothGatt gattMock = builder.getGattMock();
        TiSensorTag tag = builder.build(false);

        boolean connected = tag.connect(adapter);

        assertFalse(connected);

        verify(gattMock, times(1)).connect(adapter);
        verify(gattMock, times(1)).disconnect();
    }

    @Test
    public void testConnect() throws KuraException {
        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, false, true);
        String val = V_1_4;
        builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, val);
        TiSensorTag tag = builder.build(true);

        assertFalse(tag.isCC2650());
        assertEquals("1.4", tag.getFirmareRevision());
    }

    @Test
    public void testConnectCC2650() throws KuraException {
        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, true, true);
        String val = "312E3430";
        builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2650, val);
        TiSensorTag tag = builder.build(true);

        assertTrue(tag.isCC2650());
        assertEquals("1.40", tag.getFirmareRevision());
        assertTrue(tag.isConnected());
    }

    @Test
    public void testSetFirmwareRevisionFailure() throws KuraException {
        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, true, false);
        BluetoothGatt gattMock = builder.getGattMock();

        doThrow(new KuraException(KuraErrorCode.BLE_CONNECTION_ERROR)).when(gattMock)
                .readCharacteristicValue(anyString());

        TiSensorTag tag = builder.build(true);

        tag.setFirmwareRevision();

        assertNull(tag.getFirmareRevision());
    }

    @Test
    public void testDiscoverServices() {
        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, true, false);
        BluetoothGatt gattMock = builder.getGattMock();

        List<BluetoothGattService> services = new ArrayList<>();
        when(gattMock.getServices()).thenReturn(services);

        TiSensorTag tag = builder.build(true);

        List<BluetoothGattService> list = tag.discoverServices();

        assertEquals(services, list);

        verify(gattMock, times(1)).getServices();
    }

    @Test
    public void testGetCharacteristics() {
        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, true, false);
        BluetoothGatt gattMock = builder.getGattMock();

        List<BluetoothGattCharacteristic> services = new ArrayList<>();
        when(gattMock.getCharacteristics("1", "2")).thenReturn(services);

        TiSensorTag tag = builder.build(true);

        List<BluetoothGattCharacteristic> list = tag.getCharacteristics("1", "2");

        assertEquals(services, list);

        verify(gattMock, times(1)).getCharacteristics("1", "2");
    }

    @Test
    public void testSetGetSecurityLevel() throws KuraException {
        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, true, false);
        BluetoothGatt gattMock = builder.getGattMock();

        TiSensorTag tag = builder.build(true);

        BluetoothGattSecurityLevel level = BluetoothGattSecurityLevel.HIGH;
        tag.setSecurityLevel(level);

        verify(gattMock, times(1)).setSecurityLevel(level);

        when(gattMock.getSecurityLevel()).thenReturn(level);

        BluetoothGattSecurityLevel result = tag.getSecurityLevel();

        assertEquals(level, result);
    }

    @Test
    public void testEnableTermometer() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "01", "enableTermometer");
    }

    @Test
    public void testEnableTermometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "01", "enableTermometer");
    }

    @Test
    public void testDisableTermometer() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "00", "disableTermometer");
    }

    @Test
    public void testDisableTermometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "00", "disableTermometer");
    }

    @Test
    public void testReadTemperatureCc2650() throws Throwable {
        double[] expected = new double[] { 8, 4 };
        testRead(true, TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2650, "01020204", expected, "readTemperature",
                (e, a) -> assertArrayEquals(e, a, EPS));
    }

    @Test
    public void testReadTemperature() throws Throwable {
        double[] expected = new double[] { 8.02, 26.64 };
        testRead(false, TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2541, "01020204", expected, "readTemperature",
                (e, a) -> assertArrayEquals(e, a, EPS));
    }

    @Test
    public void testReadTemperatureUUID() throws Throwable {
        double[] expected = new double[] { 8.02, 26.64 };
        testRead(false, TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE, "01020204", expected, "readTemperatureByUuid",
                (e, a) -> assertArrayEquals(e, a, EPS));
    }

    @Test
    public void testEnableTemperatureNotifications() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(false, TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541, "01:00",
                "enableTemperatureNotifications", listener);
    }

    @Test
    public void testEnableTemperatureNotificationsCc2650() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);

        TiSensorTagBuilder builder = testWrite(true, TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650, "01:00",
                "enableTemperatureNotifications",
                listener);

        TiSensorTag tag = builder.getTag();

        tag.onDataReceived(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2650, "01020204");

        ArgumentCaptor<Map> payloadArg = ArgumentCaptor.forClass(Map.class);
        verify(listener, times(1)).notify(eq(TiSensorTagTest.ADDRESS), payloadArg.capture());

        Map<String, Object> payload = payloadArg.getValue();

        assertEquals(2, payload.size());
        assertEquals(8.0, payload.get("Ambient"));
        assertEquals(4.0, payload.get("Target"));

    }

    @Test
    public void testDisableTemperatureNotifications() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541, "00:00",
                "disableTemperatureNotifications");
    }

    @Test
    public void testDisableTemperatureNotificationsCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650, "00:00",
                "disableTemperatureNotifications");
    }

    @Test
    public void testSetTermometerPeriod() throws Throwable {
        String period = "1000";
        testWrite(true, TiSensorTagGatt.HANDLE_TEMP_SENSOR_PERIOD_2650, period, "setTermometerPeriod", period);
    }

    @Test
    public void testEnableAccelerometer() throws Throwable {
        String config = "cfg";
        testWrite(false, TiSensorTagGatt.HANDLE_ACC_SENSOR_ENABLE_2541, config, "enableAccelerometer", config);
    }

    @Test
    public void testEnableAccelerometerCc2650() throws Throwable {
        String config = "cfg";
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, config, "enableAccelerometer", config);
    }

    @Test
    public void testDisableAccelerometer() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_ACC_SENSOR_ENABLE_2541, "00", "disableAccelerometer");
    }

    @Test
    public void testDisableAccelerometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, "0000", "disableAccelerometer");
    }

    @Test
    public void testReadAccelerationCc2650() throws Throwable {
        double[] expected = new double[] { -0.25, 0.5, -1 };
        testRead(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_VALUE_2650, "000102030405000400080010", expected,
                "readAcceleration", (e, a) -> assertArrayEquals(e, a, EPS));
    }

    @Test
    public void testReadAcceleration() throws Throwable {
        double[] expected = new double[] { -1.98, -1.94, 0.02 };
        testRead(false, TiSensorTagGatt.HANDLE_ACC_SENSOR_VALUE_2541, "81 84 FF", expected, "readAcceleration",
                (e, a) -> assertArrayEquals(e, a, EPS));
    }

    @Test
    public void testReadAccelerationUUIDCc2650() throws Throwable {
        double[] expected = new double[] { -0.25, 0.5, -1 };
        testRead(true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE, "000102030405000400080010", expected,
                "readAccelerationByUuid", (e, a) -> assertArrayEquals(e, a, EPS));
    }

    @Test
    public void testReadAccelerationUUID() throws Throwable {
        double[] expected = new double[] { -1.98, -1.94, 0.02 };
        testRead(false, TiSensorTagGatt.UUID_ACC_SENSOR_VALUE, "81 84 FF", expected, "readAccelerationByUuid",
                (e, a) -> assertArrayEquals(e, a, EPS));
    }

    @Test
    public void testEnableAccelerationNotifications() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(false, TiSensorTagGatt.HANDLE_ACC_SENSOR_NOTIFICATION_2541, "01:00",
                "enableAccelerationNotifications", listener);
    }

    @Test
    public void testEnableAccelerationNotificationsCc2650() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "01:00", "enableAccelerationNotifications",
                listener);
    }

    @Test
    public void testDisableAccelerationNotifications() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_ACC_SENSOR_NOTIFICATION_2541, "00:00",
                "disableAccelerationNotifications");
    }

    @Test
    public void testDisableAccelerationNotificationsCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "00:00",
                "disableAccelerationNotifications");
    }

    @Test
    public void testSetAccelerometerPeriod() throws Throwable {
        String period = "1000";
        testWrite(false, TiSensorTagGatt.HANDLE_ACC_SENSOR_PERIOD_2541, period, "setAccelerometerPeriod", period);
    }

    @Test
    public void testSetAccelerometerPeriodCc2650() throws Throwable {
        String period = "1000";
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_PERIOD_2650, period, "setAccelerometerPeriod", period);
    }

    @Test
    public void testEnableHygrometer() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2541, "01", "enableHygrometer");
    }

    @Test
    public void testEnableHygrometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2650, "01", "enableHygrometer");
    }

    @Test
    public void testDisableHygrometer() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2541, "00", "disableHygrometer");
    }

    @Test
    public void testDisableHygrometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2650, "00", "disableHygrometer");
    }

    @Test
    public void testReadHumidityCc2650() throws Throwable {
        float expected = 50;
        testRead(true, TiSensorTagGatt.HANDLE_HUM_SENSOR_VALUE_2650, "00010080", expected, "readHumidity",
                (e, a) -> assertEquals(e, a, EPS));
    }

    @Test
    public void testReadHumidity() throws Throwable {
        float expected = 50;
        testRead(false, TiSensorTagGatt.HANDLE_HUM_SENSOR_VALUE_2541, "00018072", expected, "readHumidity",
                (e, a) -> assertEquals(e, a, 0.1));
    }

    @Test
    public void testReadHumidityUUID() throws Throwable {
        float expected = 50;
        testRead(false, TiSensorTagGatt.UUID_HUM_SENSOR_VALUE, "00018072", expected, "readHumidityByUuid",
                (e, a) -> assertEquals(e, a, 0.1));
    }

    @Test
    public void testEnableHumidityNotifications() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(false, TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2541, "01:00", "enableHumidityNotifications",
                listener);
    }

    @Test
    public void testEnableHumidityNotificationsCc2650() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(true, TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2650, "01:00", "enableHumidityNotifications",
                listener);
    }

    @Test
    public void testDisableHumidityNotifications() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2541, "00:00", "disableHumidityNotifications");
    }

    @Test
    public void testDisableHumidityNotificationsCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2650, "00:00", "disableHumidityNotifications");
    }

    @Test
    public void testSetHygrometerPeriod() throws Throwable {
        String period = "1000";
        testWrite(true, TiSensorTagGatt.HANDLE_HUM_SENSOR_PERIOD_2650, period, "setHygrometerPeriod", period);
    }

    @Test
    public void testEnableMagnetometer() throws Throwable {
        String config = "cfg";
        testWrite(false, TiSensorTagGatt.HANDLE_MAG_SENSOR_ENABLE_2541, "01", "enableMagnetometer", config);
    }

    @Test
    public void testEnableMagnetometerCc2650() throws Throwable {
        String config = "cfg";
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, config, "enableMagnetometer", config);
    }

    @Test
    public void testDisableMagnetometer() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_MAG_SENSOR_ENABLE_2541, "00", "disableMagnetometer");
    }

    @Test
    public void testDisableMagnetometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, "0000", "disableMagnetometer");
    }

    @Test
    public void testReadMagneticFieldCc2650() throws Throwable {
        float[] expected = new float[] { 1000, 2000.15f, 4000 };
        testRead(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_VALUE_2650, "0001020304050004000800100F1A1F343C68", expected,
                "readMagneticField", (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    @Test
    public void testReadMagneticField() throws Throwable {
        float[] expected = new float[] { 1000, 1000, -500 };
        testRead(false, TiSensorTagGatt.HANDLE_MAG_SENSOR_VALUE_2541, "0080008000C0", expected, "readMagneticField",
                (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    @Test
    public void testReadMagneticFieldUUIDCc2650() throws Throwable {
        float[] expected = new float[] { 1000, 2000.15f, 4000 };
        testRead(true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE, "0001020304050004000800100F1A1F343C68", expected,
                "readMagneticFieldByUuid", (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    @Test
    public void testReadMagneticFieldUUID() throws Throwable {
        float[] expected = new float[] { 1000, 1000, -500 };
        testRead(false, TiSensorTagGatt.UUID_MAG_SENSOR_VALUE, "0080008000C0", expected, "readMagneticFieldByUuid",
                (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    @Test
    public void testEnableMagneticFieldNotifications() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(false, TiSensorTagGatt.HANDLE_MAG_SENSOR_NOTIFICATION_2541, "01:00",
                "enableMagneticFieldNotifications", listener);
    }

    @Test
    public void testEnableMagneticFieldNotificationsCc2650() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "01:00",
                "enableMagneticFieldNotifications", listener);
    }

    @Test
    public void testDisableMagneticFieldNotifications() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_MAG_SENSOR_NOTIFICATION_2541, "00:00",
                "disableMagneticFieldNotifications");
    }

    @Test
    public void testDisableMagneticFieldNotificationsCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "00:00",
                "disableMagneticFieldNotifications");
    }

    @Test
    public void testSetMagnetometerPeriod() throws Throwable {
        String period = "1000";
        testWrite(false, TiSensorTagGatt.HANDLE_MAG_SENSOR_PERIOD_2541, period, "setMagnetometerPeriod", period);
    }

    @Test
    public void testSetMagnetometerPeriodCc2650() throws Throwable {
        String period = "1000";
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_PERIOD_2650, period, "setMagnetometerPeriod", period);
    }

    @Test
    public void testEnableBarometer14() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_4, "01", "enableBarometer");
    }

    @Test
    public void testEnableBarometer15() throws Throwable {
        testWrite(false, false, TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_5, "01", "enableBarometer", null);
    }

    @Test
    public void testEnableBarometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2650, "01", "enableBarometer");
    }

    @Test
    public void testDisableBarometer14() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_4, "00", "disableBarometer");
    }

    @Test
    public void testDisableBarometer15() throws Throwable {
        testWrite(false, false, TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_5, "00", "disableBarometer", null);
    }

    @Test
    public void testDisableBarometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2650, "00", "disableBarometer");
    }

    @Test
    public void testCalibrateBarometer14() throws Throwable {
        TiSensorTagBuilder builder = testWrite(false, TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_4, "02",
                "calibrateBarometer");

        verify(builder.getGattMock()).readCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_CALIBRATION_2541_1_4);
    }

    @Test
    public void testCalibrateBarometer15() throws Throwable {
        TiSensorTagBuilder builder = testWrite(false, false, TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_5, "02",
                "calibrateBarometer", null);

        verify(builder.getGattMock()).readCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_CALIBRATION_2541_1_5);
    }

    @Test
    public void testSetCalibrationBarometer14() throws Throwable {
        String expected = "1013";
        TiSensorTagBuilder builder = testRead(false, TiSensorTagGatt.HANDLE_PRE_CALIBRATION_2541_1_4, expected, null,
                "setCalibrationBarometer", (e, a) -> {
                    // do nothing
                });

        TiSensorTag tag = builder.getTag();

        assertEquals(expected, TestUtil.getFieldValue(tag, "pressureCalibration"));
    }

    @Test
    public void testSetCalibrationBarometer15() throws Throwable {
        String expected = "1013";

        TiSensorTagBuilder builder = testRead(false, false, TiSensorTagGatt.HANDLE_PRE_CALIBRATION_2541_1_5, expected,
                null, "setCalibrationBarometer", (e, a) -> {
                    // do nothing
                });

        TiSensorTag tag = builder.getTag();

        assertEquals(expected, TestUtil.getFieldValue(tag, "pressureCalibration"));
    }

    @Test
    public void testReadPressureCc2650Short() throws Throwable {
        double expected = 1013.76;
        testRead(true, TiSensorTagGatt.HANDLE_PRE_SENSOR_VALUE_2650, "000163A0", expected, "readPressure",
                (e, a) -> assertEquals(e, a, EPS));
    }

    @Test
    public void testReadPressureCc2650Long() throws Throwable {
        double expected = 1013;
        testRead(true, TiSensorTagGatt.HANDLE_PRE_SENSOR_VALUE_2650, "000102B48B01", expected, "readPressure",
                (e, a) -> assertEquals(e, a, EPS));
    }

    @Test
    public void testReadPressure14() throws KuraException, NoSuchFieldException {
        double expected = 3.18;

        TiSensorTagBuilder builder = new TiSensorTagBuilder("hci0", false, true);
        builder.readCharacteristic(TiSensorTagGatt.HANDLE_PRE_SENSOR_VALUE_2541_1_4, "00010203");
        builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_4);

        TiSensorTag tag = builder.build(true);
        TestUtil.setFieldValue(tag, "pressureCalibration", "00010203040506070809000102030405");

        double pressure = tag.readPressure();

        assertEquals(expected, pressure, EPS);
    }

    @Test
    public void testReadPressure15() throws KuraException, NoSuchFieldException {
        double expected = 3.18;

        TiSensorTagBuilder builder = new TiSensorTagBuilder("hci0", false, true);
        builder.readCharacteristic(TiSensorTagGatt.HANDLE_PRE_SENSOR_VALUE_2541_1_5, "00010203");
        builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_5);

        TiSensorTag tag = builder.build(true);
        TestUtil.setFieldValue(tag, "pressureCalibration", "00010203040506070809000102030405");

        double pressure = tag.readPressure();

        assertEquals(expected, pressure, EPS);
    }

    @Test
    public void testReadPressureUUIDCc2650() throws Throwable {
        double expected = 1013.76;
        testRead(true, TiSensorTagGatt.UUID_PRE_SENSOR_VALUE, "000163A0", expected, "readPressureByUuid",
                (e, a) -> assertEquals(e, a, EPS));
    }

    @Test
    public void testEnablePressureNotifications14() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(false, TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_4, "01:00",
                "enablePressureNotifications", listener);
    }

    @Test
    public void testEnablePressureNotifications15() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(false, false, TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_5, "01:00",
                "enablePressureNotifications", listener);
    }

    @Test
    public void testEnablePressureNotificationsCc2650() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(true, TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2650, "01:00", "enablePressureNotifications",
                listener);
    }

    @Test
    public void testDisablePressureNotifications14() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_4, "00:00",
                "disablePressureNotifications");
    }

    @Test
    public void testDisablePressureNotifications15() throws Throwable {
        testWrite(false, false, TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_5, "00:00",
                "disablePressureNotifications", null);
    }

    @Test
    public void testDisablePressureNotificationsCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2650, "00:00", "disablePressureNotifications");
    }

    @Test
    public void testSetBarometerPeriodCc2650() throws Throwable {
        String period = "1000";
        testWrite(true, TiSensorTagGatt.HANDLE_PRE_SENSOR_PERIOD_2650, period, "setBarometerPeriod", period);
    }

    @Test
    public void testEnableGyroscope() throws Throwable {
        String config = "cfg";
        testWrite(false, TiSensorTagGatt.HANDLE_GYR_SENSOR_ENABLE_2541, config, "enableGyroscope", config);
    }

    @Test
    public void testEnableGyroscopeCc2650() throws Throwable {
        String config = "cfg";
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, config, "enableGyroscope", config);
    }

    @Test
    public void testDisableGyroscope() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_GYR_SENSOR_ENABLE_2541, "00", "disableGyroscope");
    }

    @Test
    public void testDisableGyroscopeCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, "0000", "disableGyroscope");
    }

    @Test
    public void testReadGyroscopeCc2650() throws Throwable {
        float[] expected = new float[] { 90, 45, 30 };
        testRead(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_VALUE_2650, "0A17142E5C0F", expected, "readGyroscope",
                (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    @Test
    public void testReadGyroscope() throws Throwable {
        float[] expected = new float[] { 90, -45, 30 };
        testRead(false, TiSensorTagGatt.HANDLE_GYR_SENSOR_VALUE_2541, "0A17142E5C0F", expected, "readGyroscope",
                (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    @Test
    public void testReadGyroscopeUUIDCc2650() throws Throwable {
        float[] expected = new float[] { 90, 45, 30 };
        testRead(true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE, "0A17142E5C0F", expected, "readGyroscopeByUuid",
                (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    @Test
    public void testReadGyroscopeUUID() throws Throwable {
        float[] expected = new float[] { 90, -45, 30 };
        testRead(false, TiSensorTagGatt.UUID_GYR_SENSOR_VALUE, "0A17142E5C0F", expected, "readGyroscopeByUuid",
                (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    @Test
    public void testEnableGyroscopeNotifications() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(false, TiSensorTagGatt.HANDLE_GYR_SENSOR_NOTIFICATION_2541, "01:00", "enableGyroscopeNotifications",
                listener);
    }

    @Test
    public void testEnableGyroscopeNotificationsCc2650() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "01:00", "enableGyroscopeNotifications",
                listener);
    }

    @Test
    public void testDisableGyroscopeNotifications() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_GYR_SENSOR_NOTIFICATION_2541, "00:00", "disableGyroscopeNotifications");
    }

    @Test
    public void testDisableGyroscopeNotificationsCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "00:00", "disableGyroscopeNotifications");
    }

    @Test
    public void testSetGyroscopePeriodCc2650() throws Throwable {
        String period = "1000";
        testWrite(true, TiSensorTagGatt.HANDLE_MOV_SENSOR_PERIOD_2650, period, "setGyroscopePeriod", period);
    }

    @Test
    public void testEnableLuxometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_OPTO_SENSOR_ENABLE_2650, "01", "enableLuxometer");
    }

    @Test
    public void testEnableLuxometer() throws KuraException {
        TiSensorTag tag = new TiSensorTagBuilder("hci0", false, true)
                .readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_4).build(true);
        tag.enableLuxometer();
    }

    @Test
    public void testDisableLuxometerCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_OPTO_SENSOR_ENABLE_2650, "00", "disableLuxometer");
    }

    @Test
    public void testReadLightCc2650() throws Throwable {
        double expected = 100;
        testRead(true, TiSensorTagGatt.HANDLE_OPTO_SENSOR_VALUE_2650, "C429", expected, "readLight",
                (e, a) -> assertEquals(e, a, EPS));
    }

    @Test
    public void testReadLight() throws KuraException {
        double expected = 0.0;
        TiSensorTag tag = new TiSensorTagBuilder("hci0", false, true)
                .readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_4).build(true);
        assertEquals(expected, tag.readLight(), EPS);
    }

    @Test
    public void testReadLightUUIDCc2650() throws Throwable {
        double expected = 100;
        testRead(true, TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE, "C429", expected, "readLightByUuid",
                (e, a) -> assertEquals(e, a, EPS));
    }

    @Test
    public void testReadLightUUID() throws KuraException {
        double expected = 0.0;
        TiSensorTag tag = new TiSensorTagBuilder("hci0", false, true)
                .readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_4).build(true);
        assertEquals(expected, tag.readLightByUuid(), EPS);
    }

    @Test
    public void testEnableLightNotificationsCc2650() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(true, TiSensorTagGatt.HANDLE_OPTO_SENSOR_NOTIFICATION_2650, "01:00", "enableLightNotifications",
                listener);
    }

    @Test
    public void testDisableLightNotificationsCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_OPTO_SENSOR_NOTIFICATION_2650, "00:00", "disableLightNotifications");
    }

    @Test
    public void testSetLuxometerPeriodCc2650() throws Throwable {
        String period = "1000";
        testWrite(true, TiSensorTagGatt.HANDLE_OPTO_SENSOR_PERIOD_2650, period, "setLuxometerPeriod", period);
    }

    @Test
    public void testReadKeysStatusCc2650() throws Throwable {
        testRead(true, TiSensorTagGatt.HANDLE_KEYS_STATUS_2650, "123", "123", "readKeysStatus",
                (e, a) -> assertEquals(e, a));
    }

    @Test
    public void testReadKeysStatus() throws Throwable {
        testRead(false, TiSensorTagGatt.HANDLE_KEYS_STATUS_2541, "123", "123", "readKeysStatus",
                (e, a) -> assertEquals(e, a));
    }

    @Test
    public void testReadKeysStatusUUIDCc2650() throws Throwable {
        testRead(true, TiSensorTagGatt.UUID_KEYS_STATUS, "123", "123", "readKeysStatusByUuid",
                (e, a) -> assertEquals(e, a));
    }

    @Test
    public void testReadKeysStatusUUID() throws Throwable {
        testRead(false, TiSensorTagGatt.UUID_KEYS_STATUS, "123", "123", "readKeysStatusByUuid",
                (e, a) -> assertEquals(e, a));
    }

    @Test
    public void testEnableKeysNotifications() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);
        testWrite(false, TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION_2541, "01:00", "enableKeysNotifications", listener);
    }

    @Test
    public void testEnableKeysNotificationsCc2650() throws Throwable {
        TiSensorTagNotificationListener listener = mock(TiSensorTagNotificationListener.class);

        TiSensorTagBuilder builder = testWrite(true, TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION_2650, "01:00",
                "enableKeysNotifications", listener);

        TiSensorTag tag = builder.getTag();

        tag.onDataReceived(TiSensorTagGatt.HANDLE_KEYS_STATUS_2650, "123");

        ArgumentCaptor<Map> payloadArg = ArgumentCaptor.forClass(Map.class);
        verify(listener, times(1)).notify(eq(TiSensorTagTest.ADDRESS), payloadArg.capture());

        Map<String, Object> payload = payloadArg.getValue();

        assertEquals(1, payload.size());
        assertEquals(123, payload.get("Keys"));
    }

    @Test
    public void testDisableKeysNotifications() throws Throwable {
        testWrite(false, TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION_2541, "00:00", "disableKeysNotifications");
    }

    @Test
    public void testDisableKeysNotificationsCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION_2650, "00:00", "disableKeysNotifications");
    }

    @Test
    public void testEnableIOServiceCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_IO_SENSOR_ENABLE_2650, "01", "enableIOService");
    }

    @Test
    public void testEnableIOService() throws Throwable {
        TiSensorTag tag = new TiSensorTagBuilder("hci0", false, true)
                .readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_4).build(true);
        tag.enableIOService();
    }

    @Test
    public void testDisableIOServiceCc2650() throws Throwable {
        testWrite(true, TiSensorTagGatt.HANDLE_IO_SENSOR_ENABLE_2650, "00", "disableIOService");
    }

    @Test
    public void testSwitchOnRedLed() throws Throwable {
        testReadWrite(true, TiSensorTagGatt.HANDLE_IO_SENSOR_VALUE_2650, "F0", "f1", "switchOnRedLed");
    }

    @Test
    public void testSwitchOffRedLed() throws Throwable {
        testReadWrite(true, TiSensorTagGatt.HANDLE_IO_SENSOR_VALUE_2650, "FF", "fe", "switchOffRedLed");
    }

    @Test
    public void testSwitchOnGreenLed() throws Throwable {
        testReadWrite(true, TiSensorTagGatt.HANDLE_IO_SENSOR_VALUE_2650, "F0", "f2", "switchOnGreenLed");
    }

    @Test
    public void testSwitchOffGreenLed() throws Throwable {
        testReadWrite(true, TiSensorTagGatt.HANDLE_IO_SENSOR_VALUE_2650, "FF", "fd", "switchOffGreenLed");
    }

    @Test
    public void testSwitchOnBuzzer() throws Throwable {
        testReadWrite(true, TiSensorTagGatt.HANDLE_IO_SENSOR_VALUE_2650, "F0", "f4", "switchOnBuzzer");
    }

    @Test
    public void testSwitchOffBuzzer() throws Throwable {
        testReadWrite(true, TiSensorTagGatt.HANDLE_IO_SENSOR_VALUE_2650, "FF", "fb", "switchOffBuzzer");
    }

    public <T> TiSensorTagBuilder testRead(boolean cc2650, UUID id, String rawValue, T expected, String method,
            Assertion<T> assertion) throws Throwable {

        return testRead(cc2650, true, null, id, rawValue, expected, method, assertion);
    }

    public <T> TiSensorTagBuilder testRead(boolean cc2650, String id, String rawValue, T expected, String method,
            Assertion<T> assertion) throws Throwable {

        return testRead(cc2650, true, id, null, rawValue, expected, method, assertion);
    }

    public <T> TiSensorTagBuilder testRead(boolean cc2650, boolean v14, String id, String rawValue, T expected,
            String method, Assertion<T> assertion) throws Throwable {

        return testRead(cc2650, v14, id, null, rawValue, expected, method, assertion);
    }

    public <T> TiSensorTagBuilder testRead(boolean cc2650, boolean v14, String id, UUID uuid, String rawValue,
            T expected, String method, Assertion<T> assertion) throws Throwable {

        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, cc2650, true);
        if (id != null) {
            builder.readCharacteristic(id, rawValue);
        }
        if (uuid != null) {
            builder.readCharacteristic(uuid, rawValue);
        }
        if (cc2650) {
            builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2650, "312E3430");
        } else if (v14) {
            builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_4);
        } else {
            builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_5);
        }

        TiSensorTag tag = builder.build(true);

        Object o = TestUtil.invokePrivate(tag, method);

        assertion.assertEq(expected, (T) o);

        return builder;
    }

    public TiSensorTagBuilder testReadWrite(boolean cc2650, String id, String readValue, String writeValue,
            String method) throws Throwable {

        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, cc2650, true);
        builder.readCharacteristic(id, readValue);
        builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2650, "312E3430");

        TiSensorTag tag = builder.build(true);

        TestUtil.invokePrivate(tag, method);

        builder.verifyWrite(id, writeValue);

        return builder;
    }

    public TiSensorTagBuilder testWrite(boolean cc2650, String id, String value, String method) throws Throwable {
        return testWrite(cc2650, id, value, method, null);
    }

    public TiSensorTagBuilder testWrite(boolean cc2650, String id, String value, String method, Object param)
            throws Throwable {
        return testWrite(cc2650, true, id, value, method, param);
    }

    public TiSensorTagBuilder testWrite(boolean cc2650, boolean v14, String id, String value, String method,
            Object param) throws Throwable {

        String adapter = "hci0";

        TiSensorTagBuilder builder = new TiSensorTagBuilder(adapter, cc2650, true);
        if (cc2650) {
            builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2650, "312E3430");
        } else if (v14) {
            builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_4);
        } else {
            builder.readCharacteristic(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541, V_1_5);
        }
        TiSensorTag tag = builder.build(true);

        if (param != null) {
            TestUtil.invokePrivate(tag, method, param);
        } else {
            TestUtil.invokePrivate(tag, method);
        }

        builder.verifyWrite(id, value);

        return builder;
    }

}

interface Assertion<T> {

    void assertEq(T expected, T actual);
}

class TiSensorTagBuilder {

    private String adapter;
    private BluetoothDevice deviceMock;
    private BluetoothGatt gattMock;

    private TiSensorTag tag;

    public TiSensorTagBuilder(String adapter) {
        this(adapter, true, false);
    }

    public TiSensorTagBuilder(String adapter, boolean cc2650, boolean connected) {
        this.adapter = adapter;

        deviceMock = mock(BluetoothDevice.class);

        when(deviceMock.getAdress()).thenReturn(TiSensorTagTest.ADDRESS);

        gattMock = mock(BluetoothGatt.class);
        when(deviceMock.getBluetoothGatt()).thenReturn(gattMock);

        if (cc2650) {
            when(deviceMock.getName()).thenReturn("CC2650 SensorTag");
        } else {
            when(deviceMock.getName()).thenReturn("CC2541");
        }

        if (connected) {
            try {
                when(gattMock.connect(adapter)).thenReturn(true);
                when(gattMock.checkConnection()).thenReturn(true);
            } catch (KuraException e) {
                // OK
            }
        }
    }

    public BluetoothGatt getGattMock() {
        return gattMock;
    }

    public TiSensorTag getTag() {
        return tag;
    }

    public TiSensorTagBuilder readCharacteristic(String id, String value) throws KuraException {
        when(gattMock.readCharacteristicValue(id)).thenReturn(value);

        return this;
    }

    public TiSensorTagBuilder readCharacteristic(UUID id, String value) throws KuraException {
        when(gattMock.readCharacteristicValueByUuid(id)).thenReturn(value);

        return this;
    }

    public TiSensorTag build(boolean connect) {
        tag = new TiSensorTag(deviceMock);

        if (connect) {
            tag.connect(adapter);
        }

        return tag;
    }

    public void verifyWrite(String id, String value) {
        verify(gattMock, times(1)).writeCharacteristicValue(id, value);
    }
}
