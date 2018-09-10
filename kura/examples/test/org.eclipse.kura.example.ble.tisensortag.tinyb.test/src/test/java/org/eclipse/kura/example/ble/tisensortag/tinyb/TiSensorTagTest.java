/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.example.ble.tisensortag.tinyb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.eclipse.kura.example.testutil.TestUtil;
import org.junit.Assert;
import org.junit.Test;

public class TiSensorTagTest {

    private static final double EPS = 0.01;

    @Test
    public void testConnectionFailure()
            throws KuraBluetoothConnectionException, KuraBluetoothResourceNotFoundException {

        BluetoothLeGattService keysSvcMock = mock(BluetoothLeGattService.class);
        BluetoothLeGattCharacteristic keysChrMock = mock(BluetoothLeGattCharacteristic.class);

        TiSensorTagBuilder builder = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_KEYS_SERVICE, keysSvcMock)
                .addCharacteristic(TiSensorTagGatt.UUID_KEYS_SERVICE, TiSensorTagGatt.UUID_KEYS_STATUS, keysChrMock);
        TiSensorTag tag = builder.build(true);

        BluetoothLeDevice deviceMock = builder.getDevice();

        doThrow(new KuraBluetoothConnectionException(KuraErrorCode.BLE_IO_ERROR)).when(deviceMock).connect();

        verify(deviceMock, times(1)).connect();
        verify(deviceMock, times(2)).isConnected();

        when(deviceMock.isConnected()).thenReturn(false);
        tag.disconnect();

        verify(deviceMock, times(1)).disconnect();
    }

    @Test
    public void testConnectNotCC2650() throws NoSuchFieldException {

        TiSensorTagBuilder builder = new TiSensorTagBuilder(false, true);
        TiSensorTag tag = builder.build(true);

        assertFalse(tag.isCC2650());

        Map<String, TiSensorTagGattResources> gattResources = (Map<String, TiSensorTagGattResources>) TestUtil
                .getFieldValue(tag, "gattResources");

        assertNotNull(gattResources);
        assertEquals(1, gattResources.size());
        assertTrue(gattResources.containsKey("devinfo"));
        assertFalse(gattResources.containsKey("opto"));
    }

    @Test
    public void testConnectDisconnect()
            throws KuraBluetoothConnectionException, KuraBluetoothResourceNotFoundException, NoSuchFieldException {

        BluetoothLeGattService infoSvcMock = mock(BluetoothLeGattService.class);
        BluetoothLeGattService optoSvcMock = mock(BluetoothLeGattService.class);
        BluetoothLeGattService keysSvcMock = mock(BluetoothLeGattService.class);
        BluetoothLeGattCharacteristic keysChrMock = mock(BluetoothLeGattCharacteristic.class);
        BluetoothLeGattCharacteristic optoChrMock = mock(BluetoothLeGattCharacteristic.class);

        TiSensorTagBuilder builder = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_DEVINFO_SERVICE, infoSvcMock)
                .addService(TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE, optoSvcMock)
                .addService(TiSensorTagGatt.UUID_KEYS_SERVICE, keysSvcMock)
                .addCharacteristic(TiSensorTagGatt.UUID_KEYS_SERVICE, TiSensorTagGatt.UUID_KEYS_STATUS, keysChrMock)
                .addCharacteristic(TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE, TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE,
                        optoChrMock);
        TiSensorTag tag = builder.build(true);

        assertTrue(tag.isCC2650());

        BluetoothLeDevice deviceMock = tag.getBluetoothLeDevice();
        verify(deviceMock, times(1)).connect();

        Map<String, TiSensorTagGattResources> gattResources = (Map<String, TiSensorTagGattResources>) TestUtil
                .getFieldValue(tag, "gattResources");

        assertNotNull(gattResources);
        assertEquals(3, gattResources.size());
        assertEquals(infoSvcMock, gattResources.get("devinfo").getGattService());
        assertEquals(optoSvcMock, gattResources.get("opto").getGattService());
        assertEquals(keysSvcMock, gattResources.get("keys").getGattService());

        when(deviceMock.isConnected()).thenReturn(false);
        tag.disconnect();

        verify(deviceMock, times(1)).disconnect();
    }

    @Test
    public void testGetFWRevisionFail() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        BluetoothLeGattService infoSvcMock = mock(BluetoothLeGattService.class);
        doThrow(new KuraBluetoothResourceNotFoundException("test")).when(infoSvcMock)
                .findCharacteristic(TiSensorTagGatt.UUID_DEVINFO_FIRMWARE_REVISION);

        TiSensorTagBuilder builder = new TiSensorTagBuilder(true, true).addService(TiSensorTagGatt.UUID_DEVINFO_SERVICE,
                infoSvcMock);
        TiSensorTag tag = builder.build(true);

        assertEquals("", tag.getFirmareRevision());
    }

    @Test
    public void testGetFWRevision() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        String fw = "1.40";
        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        when(bch.readValue()).thenReturn(fw.getBytes());

        BluetoothLeGattService infoSvcMock = mock(BluetoothLeGattService.class);
        when(infoSvcMock.findCharacteristic(TiSensorTagGatt.UUID_DEVINFO_FIRMWARE_REVISION)).thenReturn(bch);

        TiSensorTagBuilder builder = new TiSensorTagBuilder(true, true).addService(TiSensorTagGatt.UUID_DEVINFO_SERVICE,
                infoSvcMock);
        TiSensorTag tag = builder.build(true);

        assertEquals(fw, tag.getFirmareRevision());
    }

    @Test
    public void testGetCharacteristicsEmpty() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        TiSensorTagBuilder builder = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_DEVINFO_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_KEYS_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_IO_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE, mock(BluetoothLeGattService.class));

        TiSensorTag tag = builder.build(true);

        List<BluetoothLeGattCharacteristic> characteristics = tag.getCharacteristics();
        assertEquals(0, characteristics.size());
    }

    @Test
    public void testGetCharacteristics() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        BluetoothLeGattService infoSvcMock = mock(BluetoothLeGattService.class);
        List<BluetoothLeGattCharacteristic> isChs = new ArrayList<>();
        isChs.add(mock(BluetoothLeGattCharacteristic.class));
        isChs.add(mock(BluetoothLeGattCharacteristic.class));
        when(infoSvcMock.findCharacteristics()).thenReturn(isChs);

        BluetoothLeGattService optoSvcMock = mock(BluetoothLeGattService.class);
        List<BluetoothLeGattCharacteristic> osChs = new ArrayList<>();
        osChs.add(mock(BluetoothLeGattCharacteristic.class));
        when(optoSvcMock.findCharacteristics()).thenReturn(osChs);

        TiSensorTagBuilder builder = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_KEYS_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_IO_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_DEVINFO_SERVICE, infoSvcMock)
                .addService(TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE, optoSvcMock);
        TiSensorTag tag = builder.build(true);

        List<BluetoothLeGattCharacteristic> characteristics = tag.getCharacteristics();
        assertEquals(3, characteristics.size());
    }

    @Test
    public void testDiscoverServices() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        TiSensorTagBuilder builder = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_KEYS_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_IO_SENSOR_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_DEVINFO_SERVICE, mock(BluetoothLeGattService.class))
                .addService(TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE, mock(BluetoothLeGattService.class));
        TiSensorTag tag = builder.build(true);

        Map<String, BluetoothLeGattService> services = tag.discoverServices();
        assertEquals(8, services.size());
    }

    @Test
    public void testEnableTermometer() throws Throwable {
        testEnable(true, TiSensorTagGatt.UUID_TEMP_SENSOR_ENABLE, TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE,
                "enableTermometer");
    }

    @Test
    public void testEnableTermometerFail() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        doThrow(new KuraBluetoothIOException("test")).when(bch).writeValue(anyObject());

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_ENABLE)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE, svcMock).build(true);

        tag.enableTermometer();

        verify(bch, times(1)).writeValue(new byte[] { 1 });
    }

    @Test
    public void testDisableTermometer() throws Throwable {
        testDisable(true, TiSensorTagGatt.UUID_TEMP_SENSOR_ENABLE, TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE,
                "disableTermometer");
    }

    @Test
    public void testDisableTermometerFail() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        doThrow(new KuraBluetoothIOException("test")).when(bch).writeValue(anyObject());

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_ENABLE)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE, svcMock).build(true);

        tag.disableTermometer();

        verify(bch, times(1)).writeValue(new byte[] { 0 });
    }

    @Test
    public void testReadTemperatureFail() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        BluetoothLeGattService tempSvcMock = mock(BluetoothLeGattService.class);
        BluetoothLeGattCharacteristic tempChrMock = mock(BluetoothLeGattCharacteristic.class);
        doThrow(new KuraBluetoothIOException("test")).when(tempChrMock).readValue();

        TiSensorTagBuilder builder = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE, tempSvcMock).addCharacteristic(
                        TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE, TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE, tempChrMock);
        TiSensorTag tag = builder.build(true);

        assertArrayEquals(new double[2], tag.readTemperature(), EPS);
    }

    @Test
    public void testReadTemperatureCc2650() throws Throwable {
        byte[] val = { 1, 2, 2, 4 };
        double[] expected = new double[] { 8, 4 };
        testRead(val, expected, true, TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE, TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE,
                "readTemperature");
    }

    @Test
    public void testReadTemperature() throws Throwable {
        byte[] val = { 1, 2, 2, 4 };
        double[] expected = new double[] { 8.02, 26.64 };
        testRead(val, expected, false, TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE, TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE,
                "readTemperature");
    }

    @Test
    public void testSetTermometerPeriod() throws Throwable {
        testSetPeriod(true, TiSensorTagGatt.UUID_TEMP_SENSOR_PERIOD, TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE,
                "setTermometerPeriod");
    }

    @Test
    public void testSetTermometerPeriodFail() throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        doThrow(new KuraBluetoothIOException("test")).when(bch).writeValue(anyObject());

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(TiSensorTagGatt.UUID_TEMP_SENSOR_PERIOD)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE, svcMock).build(true);

        tag.setTermometerPeriod(1000);

        verify(bch, times(1)).writeValue(new byte[] { -24 });
    }

    @Test
    public void testEnableTemperatureNotifications() throws Throwable {
        byte[] val = { 1, 2, 2, 4 };
        double[] expected = { 8, 4 };
        testEnableNotifications(val, expected, true, TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE,
                TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE, "enableTemperatureNotifications");
    }

    @Test
    public void testDisableTemperatureNotifications() throws Throwable {
        testDisableNotifications(true, TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE, TiSensorTagGatt.UUID_TEMP_SENSOR_SERVICE,
                "disableTemperatureNotifications");
    }

    @Test
    public void testEnableAccelerometer() throws Throwable {
        byte[] config = { 2 };

        testEnable(config, false, TiSensorTagGatt.UUID_ACC_SENSOR_ENABLE, TiSensorTagGatt.UUID_ACC_SENSOR_SERVICE,
                "enableAccelerometer");
    }

    @Test
    public void testEnableAccelerometerCc2650() throws Throwable {
        byte[] oldVal = { 0, 0, 0, 0, 1, 1, 1, 0, 0, 1 };
        byte[] config = { 1, 0, 1, 0, 1, 1, 1, 0, 0, 1 };
        byte[] writeVal = new byte[] { 1, 0 };

        testEnable(oldVal, config, writeVal, true, TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE,
                TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, "enableAccelerometer");
    }

    @Test
    public void testDisableAccelerometer() throws Throwable {
        testDisable(false, TiSensorTagGatt.UUID_ACC_SENSOR_ENABLE, TiSensorTagGatt.UUID_ACC_SENSOR_SERVICE,
                "disableAccelerometer");
    }

    @Test
    public void testDisableAccelerometerCc2650() throws Throwable {
        byte[] oldVal = { 0, 0, 0, 0, 1, 1, 1, 0, 0, 1 };
        byte[] writeVal = new byte[] { 0, 0 };

        testDisable(oldVal, writeVal, true, TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE,
                TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, "disableAccelerometer");
    }

    @Test
    public void testDisableAccelerometerCc2650Fail()
            throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        doThrow(new KuraBluetoothIOException("test")).when(bch).readValue();

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, svcMock).build(true);

        tag.disableAccelerometer();

        verify(bch, times(0)).writeValue(anyObject());
    }

    @Test
    public void testReadAccelerationCc2650() throws Throwable {
        byte[] val = { 0, 1, 2, 3, 4, 5, 0, 4, 0, 8, 0, 16 };
        double[] expected = new double[] { -0.25, 0.5, -1 };

        testRead(val, expected, true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE, TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE,
                "readAcceleration");
    }

    @Test
    public void testReadAcceleration() throws Throwable {
        byte[] val = { (byte) 129, (byte) 132, (byte) 255 };
        double[] expected = new double[] { -1.98, -1.94, 0.02 };

        testRead(val, expected, false, TiSensorTagGatt.UUID_ACC_SENSOR_VALUE, TiSensorTagGatt.UUID_ACC_SENSOR_SERVICE,
                "readAcceleration");
    }

    @Test
    public void testEnableAccelerationNotificationsCc2650() throws Throwable {
        byte[] val = { 0, 1, 2, 3, 4, 5, 0, 4, 0, 8, 0, 16 };
        double[] expected = { -0.25, 0.5, -1 };
        testEnableNotifications(val, expected, true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE,
                TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, "enableAccelerationNotifications");
    }

    @Test
    public void testEnableAccelerationNotifications() throws Throwable {
        byte[] val = { (byte) 129, (byte) 132, (byte) 255 };
        double[] expected = { -1.98, -1.94, 0.02 };
        testEnableNotifications(val, expected, false, TiSensorTagGatt.UUID_ACC_SENSOR_VALUE,
                TiSensorTagGatt.UUID_ACC_SENSOR_SERVICE, "enableAccelerationNotifications");
    }

    @Test
    public void testDisableAccelerationNotificationsCc2650() throws Throwable {
        testDisableNotifications(true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE, TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE,
                "disableAccelerationNotifications");
    }

    @Test
    public void testDisableAccelerationNotifications() throws Throwable {
        testDisableNotifications(false, TiSensorTagGatt.UUID_ACC_SENSOR_VALUE, TiSensorTagGatt.UUID_ACC_SENSOR_SERVICE,
                "disableAccelerationNotifications");
    }

    @Test
    public void testSetAccelerometerPeriodCc2650() throws Throwable {
        testSetPeriod(true, TiSensorTagGatt.UUID_MOV_SENSOR_PERIOD, TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE,
                "setAccelerometerPeriod");
    }

    @Test
    public void testSetAccelerometerPeriod() throws Throwable {
        testSetPeriod(false, TiSensorTagGatt.UUID_ACC_SENSOR_PERIOD, TiSensorTagGatt.UUID_ACC_SENSOR_SERVICE,
                "setAccelerometerPeriod");
    }

    @Test
    public void testEnableHygrometer() throws Throwable {
        testEnable(true, TiSensorTagGatt.UUID_HUM_SENSOR_ENABLE, TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE,
                "enableHygrometer");
    }

    @Test
    public void testDisableHygrometer() throws Throwable {
        testDisable(true, TiSensorTagGatt.UUID_HUM_SENSOR_ENABLE, TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE,
                "disableHygrometer");
    }

    @Test
    public void testReadHumidityCc2650() throws Throwable {
        byte[] val = { 0, 1, 0, (byte) 128 };
        float expected = 50;

        testRead(val, expected, true, TiSensorTagGatt.UUID_HUM_SENSOR_VALUE, TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE,
                "readHumidity");
    }

    @Test
    public void testReadHumidity() throws Throwable {
        byte[] val = { 0, 1, (byte) 0xB0, (byte) 0x72 };
        float expected = 50;

        testRead(val, expected, false, TiSensorTagGatt.UUID_HUM_SENSOR_VALUE, TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE,
                "readHumidity");
    }

    @Test
    public void testEnableHumidityNotifications() throws Throwable {
        byte[] val = { 0, 1, 0, (byte) 128 };
        float expected = 50;
        testEnableNotifications(val, expected, true, TiSensorTagGatt.UUID_HUM_SENSOR_VALUE,
                TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE, "enableHumidityNotifications");
    }

    @Test
    public void testDisableHumidityNotifications() throws Throwable {
        testDisableNotifications(false, TiSensorTagGatt.UUID_HUM_SENSOR_VALUE, TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE,
                "disableHumidityNotifications");
    }

    @Test
    public void testSetHumidityPeriod() throws Throwable {
        testSetPeriod(false, TiSensorTagGatt.UUID_HUM_SENSOR_PERIOD, TiSensorTagGatt.UUID_HUM_SENSOR_SERVICE,
                "setHygrometerPeriod");
    }

    @Test
    public void testEnableMagnetometer() throws Throwable {
        byte[] config = { 1 };

        testEnable(config, false, TiSensorTagGatt.UUID_MAG_SENSOR_ENABLE, TiSensorTagGatt.UUID_MAG_SENSOR_SERVICE,
                "enableMagnetometer");
    }

    @Test
    public void testEnableMagnetometerCc2650() throws Throwable {
        byte[] oldVal = { 0, 0, 0, 0, 1, 1, 1, 0, 0, 1 };
        byte[] config = { 1, 0, 1, 0, 1, 1, 1, 0, 0, 1 };
        byte[] writeVal = new byte[] { 1, 0 };

        testEnable(oldVal, config, writeVal, true, TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE,
                TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, "enableMagnetometer");
    }

    @Test
    public void testDisableMagnetometer() throws Throwable {
        testDisable(false, TiSensorTagGatt.UUID_MAG_SENSOR_ENABLE, TiSensorTagGatt.UUID_MAG_SENSOR_SERVICE,
                "disableMagnetometer");
    }

    @Test
    public void testDisableMagnetometerCc2650() throws Throwable {
        byte[] oldVal = { 0, 0, 0, 0, 1, 1, 1, 0, 0, 1 };
        byte[] writeVal = new byte[] { 0, 0 };

        testDisable(oldVal, writeVal, true, TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE,
                TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, "disableMagnetometer");
    }

    @Test
    public void testDisableMagnetometerCc2650Fail()
            throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        doThrow(new KuraBluetoothIOException("test")).when(bch).readValue();

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, svcMock).build(true);

        tag.disableMagnetometer();

        verify(bch, times(0)).writeValue(anyObject());
    }

    @Test
    public void testReadMagneticFieldCc2650() throws Throwable {
        byte[] val = { 0, 1, 2, 3, 4, 5, 0, 4, 0, 8, 0, 16, 0xF, 0x1A, 0x1F, 0x34, 0x3C, 0x68 };
        float[] expected = new float[] { 1000, 2000.15f, 4000 };

        testRead(val, expected, true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE, TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE,
                "readMagneticField");
    }

    @Test
    public void testReadMagneticField() throws Throwable {
        byte[] val = { 0, (byte) 0x80, 0, (byte) 0x80, 0, (byte) 0xC0 };
        float[] expected = new float[] { 1000, 1000, -500 };

        testRead(val, expected, false, TiSensorTagGatt.UUID_MAG_SENSOR_VALUE, TiSensorTagGatt.UUID_MAG_SENSOR_SERVICE,
                "readMagneticField");
    }

    @Test
    public void testEnableMagneticFieldNotificationsCc2650() throws Throwable {
        byte[] val = { 0, 1, 2, 3, 4, 5, 0, 4, 0, 8, 0, 16, 0xF, 0x1A, 0x1F, 0x34, 0x3C, 0x68 };
        float[] expected = { 1000, 2000.15f, 4000 };
        testEnableNotifications(val, expected, true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE,
                TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, "enableMagneticFieldNotifications");
    }

    @Test
    public void testEnableMagneticFieldNotifications() throws Throwable {
        byte[] val = { 0, (byte) 0x80, 0, (byte) 0x80, 0, (byte) 0xC0 };
        float[] expected = { 1000, 1000, -500 };
        testEnableNotifications(val, expected, false, TiSensorTagGatt.UUID_MAG_SENSOR_VALUE,
                TiSensorTagGatt.UUID_MAG_SENSOR_SERVICE, "enableMagneticFieldNotifications");
    }

    @Test
    public void testDisableMagneticFieldNotificationsCc2650() throws Throwable {
        testDisableNotifications(true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE, TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE,
                "disableMagneticFieldNotifications");
    }

    @Test
    public void testDisableMagneticFieldNotifications() throws Throwable {
        testDisableNotifications(false, TiSensorTagGatt.UUID_MAG_SENSOR_VALUE, TiSensorTagGatt.UUID_MAG_SENSOR_SERVICE,
                "disableMagneticFieldNotifications");
    }

    @Test
    public void testSetMagnetometerPeriodCc2650() throws Throwable {
        testSetPeriod(true, TiSensorTagGatt.UUID_MOV_SENSOR_PERIOD, TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE,
                "setMagnetometerPeriod");
    }

    @Test
    public void testSetMagnetometerPeriod() throws Throwable {
        testSetPeriod(false, TiSensorTagGatt.UUID_MAG_SENSOR_PERIOD, TiSensorTagGatt.UUID_MAG_SENSOR_SERVICE,
                "setMagnetometerPeriod");
    }

    @Test
    public void testEnableBarometer() throws Throwable {
        testEnable(true, TiSensorTagGatt.UUID_PRE_SENSOR_ENABLE, TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE,
                "enableBarometer");
    }

    @Test
    public void testDisableBarometer() throws Throwable {
        testDisable(true, TiSensorTagGatt.UUID_PRE_SENSOR_ENABLE, TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE,
                "disableBarometer");
    }

    @Test
    public void testCalibrateBarometerNonCc2650() throws Throwable {
        byte[] val = { 123 };

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        when(bch.readValue()).thenReturn(val);

        BluetoothLeGattCharacteristic bch2 = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_CALIBRATION)).thenReturn(bch);
        when(svcMock.findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_ENABLE)).thenReturn(bch2);

        TiSensorTag tag = new TiSensorTagBuilder(false, true)
                .addService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, svcMock).build(true);

        tag.calibrateBarometer();

        verify(bch2, times(1)).writeValue(new byte[] { 2 });
        verify(bch, times(1)).readValue();

        byte[] cal = (byte[]) TestUtil.getFieldValue(tag, "pressureCalibration");
        assertArrayEquals(val, cal);
    }

    @Test
    public void testReadPressureCalibration() throws Throwable {
        byte[] val = { 123 };

        testRead(val, val, false, TiSensorTagGatt.UUID_PRE_SENSOR_CALIBRATION, TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE,
                "readCalibrationPressure");
    }

    @Test
    public void testReadPressureCc2650Short() throws Throwable {
        byte[] val = { 0, 1, (byte) 0x63, (byte) 0xA0 };
        double expected = 1013.76;

        testRead(val, expected, true, TiSensorTagGatt.UUID_PRE_SENSOR_VALUE, TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE,
                "readPressure");
    }

    @Test
    public void testReadPressureCc2650Long() throws Throwable {
        byte[] val = { 0, 1, 2, (byte) 0xB4, (byte) 0x8B, 0x01 };
        double expected = 1013;

        testRead(val, expected, true, TiSensorTagGatt.UUID_PRE_SENSOR_VALUE, TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE,
                "readPressure");
    }

    @Test
    public void testReadPressure()
            throws KuraBluetoothResourceNotFoundException, KuraBluetoothIOException, NoSuchFieldException {

        byte[] val = { 0, 1, 2, 3 };
        byte[] cal = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5 };
        double expected = 3.18;

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        when(bch.readValue()).thenReturn(val);

        BluetoothLeGattService infoSvcMock = mock(BluetoothLeGattService.class);
        when(infoSvcMock.findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(false, true)
                .addService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, infoSvcMock).build(true);

        TestUtil.setFieldValue(tag, "pressureCalibration", cal);

        assertEquals(expected, tag.readPressure(), EPS);
    }

    @Test
    public void testEnablePressureNotifications() throws Throwable {
        byte[] val = { 0, 1, 2, (byte) 0xB4, (byte) 0x8B, 0x01 };
        double expected = 1013;
        testEnableNotifications(val, expected, true, TiSensorTagGatt.UUID_PRE_SENSOR_VALUE,
                TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, "enablePressureNotifications");
    }

    @Test
    public void testDisablePressureNotifications() throws Throwable {
        testDisableNotifications(false, TiSensorTagGatt.UUID_PRE_SENSOR_VALUE, TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE,
                "disablePressureNotifications");
    }

    @Test
    public void testSetPressurePeriod() throws Throwable {
        testSetPeriod(false, TiSensorTagGatt.UUID_PRE_SENSOR_PERIOD, TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE,
                "setBarometerPeriod");
    }

    @Test
    public void testEnableGyroscope() throws Throwable {
        byte[] config = { 1 };

        testEnable(config, false, TiSensorTagGatt.UUID_GYR_SENSOR_ENABLE, TiSensorTagGatt.UUID_GYR_SENSOR_SERVICE,
                "enableGyroscope");
    }

    @Test
    public void testEnableGyroscopeCc2650() throws Throwable {
        byte[] oldVal = { 0, 0, 0, 0, 1, 1, 1, 0, 0, 1 };
        byte[] config = { 1, 0, 1, 0, 1, 1, 1, 0, 0, 1 };
        byte[] writeVal = new byte[] { 1, 0 };

        testEnable(oldVal, config, writeVal, true, TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE,
                TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, "enableGyroscope");
    }

    @Test
    public void testDisableGyroscope() throws Throwable {
        testDisable(false, TiSensorTagGatt.UUID_GYR_SENSOR_ENABLE, TiSensorTagGatt.UUID_GYR_SENSOR_SERVICE,
                "disableGyroscope");
    }

    @Test
    public void testDisableGyroscopeCc2650() throws Throwable {
        byte[] oldVal = { 0, 0, 0, 0, 1, 1, 1, 0, 0, 1 };
        byte[] writeVal = new byte[] { 0, 0 };

        testDisable(oldVal, writeVal, true, TiSensorTagGatt.UUID_MOV_SENSOR_ENABLE,
                TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, "disableGyroscope");
    }

    @Test
    public void testReadGyroscopeCc2650() throws Throwable {
        byte[] val = { 0x0A, (byte) 0x17, 0x14, (byte) 0x2E, 0x5C, 0x0F };
        float[] expected = new float[] { 90, 45, 30 };

        testRead(val, expected, true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE, TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE,
                "readGyroscope");
    }

    @Test
    public void testReadGyroscope() throws Throwable {
        byte[] val = { 0x0A, (byte) 0x17, 0x14, (byte) 0x2E, 0x5C, 0x0F };
        float[] expected = new float[] { 90, -45, 30 };

        testRead(val, expected, false, TiSensorTagGatt.UUID_GYR_SENSOR_VALUE, TiSensorTagGatt.UUID_GYR_SENSOR_SERVICE,
                "readGyroscope");
    }

    @Test
    public void testEnableGyroscopeNotificationsCc2650() throws Throwable {
        byte[] val = { 0x0A, (byte) 0x17, 0x14, (byte) 0x2E, 0x5C, 0x0F };
        float[] expected = new float[] { 90, 45, 30 };

        testEnableNotifications(val, expected, true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE,
                TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE, "enableGyroscopeNotifications");
    }

    @Test
    public void testEnableGyroscopeNotifications() throws Throwable {
        byte[] val = { 0x0A, (byte) 0x17, 0x14, (byte) 0x2E, 0x5C, 0x0F };
        float[] expected = new float[] { 90, -45, 30 };

        testEnableNotifications(val, expected, false, TiSensorTagGatt.UUID_GYR_SENSOR_VALUE,
                TiSensorTagGatt.UUID_GYR_SENSOR_SERVICE, "enableGyroscopeNotifications");
    }

    @Test
    public void testDisableGyroscopeNotificationsCc2650() throws Throwable {
        testDisableNotifications(true, TiSensorTagGatt.UUID_MOV_SENSOR_VALUE, TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE,
                "disableGyroscopeNotifications");
    }

    @Test
    public void testDisableGyroscopeNotifications() throws Throwable {
        testDisableNotifications(false, TiSensorTagGatt.UUID_GYR_SENSOR_VALUE, TiSensorTagGatt.UUID_GYR_SENSOR_SERVICE,
                "disableGyroscopeNotifications");
    }

    @Test
    public void testSetGyroscopePeriodCc2650() throws Throwable {
        testSetPeriod(true, TiSensorTagGatt.UUID_MOV_SENSOR_PERIOD, TiSensorTagGatt.UUID_MOV_SENSOR_SERVICE,
                "setGyroscopePeriod");
    }

    @Test
    public void testEnableLuxometer() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.enableLuxometer();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testEnableLuxometerCc2650() throws Throwable {
        testEnable(true, TiSensorTagGatt.UUID_OPTO_SENSOR_ENABLE, TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE,
                "enableLuxometer");
    }

    @Test
    public void testDisableLuxometer() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.disableLuxometer();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testDisableLuxometerCc2650() throws Throwable {
        testDisable(true, TiSensorTagGatt.UUID_OPTO_SENSOR_ENABLE, TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE,
                "disableLuxometer");
    }

    @Test
    public void testReadLightCc2650() throws Throwable {
        byte[] val = { (byte) 0xC4, 0x29 };
        double expected = 100;

        testRead(val, expected, true, TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE, TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE,
                "readLight");
    }

    @Test
    public void testReadLight() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.readLight();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testEnableLightNotificationsCc2650() throws Throwable {
        byte[] val = { (byte) 0xC4, 0x29 };
        double expected = 100;

        testEnableNotifications(val, expected, true, TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE,
                TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE, "enableLightNotifications");
    }

    @Test
    public void testEnableLightNotifications() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.enableLightNotifications(null);
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testDisableLightNotificationsCc2650() throws Throwable {
        testDisableNotifications(true, TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE, TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE,
                "disableLightNotifications");
    }

    @Test
    public void testDisableLightNotifications() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.disableLightNotifications();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testSetLuxometerPeriodCc2650() throws Throwable {
        testSetPeriod(true, TiSensorTagGatt.UUID_OPTO_SENSOR_PERIOD, TiSensorTagGatt.UUID_OPTO_SENSOR_SERVICE,
                "setLuxometerPeriod");
    }

    @Test
    public void testEnableKeysNotifications() throws Throwable {
        byte[] val = { 123 };
        int expected = 123;

        Consumer<Integer> callback = t -> assertEquals(expected, (int) t);
        testEnableNotifications(val, expected, true, TiSensorTagGatt.UUID_KEYS_STATUS,
                TiSensorTagGatt.UUID_KEYS_SERVICE, "enableKeysNotification", callback);
    }

    @Test
    public void testDisableKeysNotifications() throws Throwable {
        testDisableNotifications(true, TiSensorTagGatt.UUID_KEYS_STATUS, TiSensorTagGatt.UUID_KEYS_SERVICE,
                "disableKeysNotifications");
    }

    @Test
    public void testEnableIOService() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.enableIOService();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testEnableIOServiceCc2650() throws Throwable {
        testEnable(true, TiSensorTagGatt.UUID_IO_SENSOR_ENABLE, TiSensorTagGatt.UUID_IO_SENSOR_SERVICE,
                "enableIOService");
    }

    @Test
    public void testDisableIOService() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.disableIOService();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testDisableIOServiceCc2650() throws Throwable {
        testDisable(true, TiSensorTagGatt.UUID_IO_SENSOR_ENABLE, TiSensorTagGatt.UUID_IO_SENSOR_SERVICE,
                "disableIOService");
    }

    @Test
    public void testSwitchOnRedLedCc2650() throws Throwable {
        byte[] oldVal = { (byte) 0xF0 };
        byte[] writeVal = { (byte) 0xF1 };
        testDisable(oldVal, writeVal, true, TiSensorTagGatt.UUID_IO_SENSOR_VALUE,
                TiSensorTagGatt.UUID_IO_SENSOR_SERVICE, "switchOnRedLed");
    }

    @Test
    public void testSwitchOnRedLed() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.switchOnRedLed();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testSwitchOffRedLedCc2650() throws Throwable {
        byte[] oldVal = { (byte) 0xFF };
        byte[] writeVal = { (byte) 0xFE };
        testDisable(oldVal, writeVal, true, TiSensorTagGatt.UUID_IO_SENSOR_VALUE,
                TiSensorTagGatt.UUID_IO_SENSOR_SERVICE, "switchOffRedLed");
    }

    @Test
    public void testSwitchOffRedLed() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.switchOffRedLed();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testSwitchOnGreenLedCc2650() throws Throwable {
        byte[] oldVal = { (byte) 0xF0 };
        byte[] writeVal = { (byte) 0xF2 };
        testDisable(oldVal, writeVal, true, TiSensorTagGatt.UUID_IO_SENSOR_VALUE,
                TiSensorTagGatt.UUID_IO_SENSOR_SERVICE, "switchOnGreenLed");
    }

    @Test
    public void testSwitchOnGreenLed() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.switchOnGreenLed();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testSwitchOffGreenLedCc2650() throws Throwable {
        byte[] oldVal = { (byte) 0xFF };
        byte[] writeVal = { (byte) 0xFD };
        testDisable(oldVal, writeVal, true, TiSensorTagGatt.UUID_IO_SENSOR_VALUE,
                TiSensorTagGatt.UUID_IO_SENSOR_SERVICE, "switchOffGreenLed");
    }

    @Test
    public void testSwitchOffGreenLed() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.switchOffGreenLed();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testSwitchOnBuzzerCc2650() throws Throwable {
        byte[] oldVal = { (byte) 0xF0 };
        byte[] writeVal = { (byte) 0xF4 };
        testDisable(oldVal, writeVal, true, TiSensorTagGatt.UUID_IO_SENSOR_VALUE,
                TiSensorTagGatt.UUID_IO_SENSOR_SERVICE, "switchOnBuzzer");
    }

    @Test
    public void testSwitchOnBuzzer() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.switchOnBuzzer();
        // nothing to check - OK if it didn't fail
    }

    @Test
    public void testSwitchOffBuzzerCc2650() throws Throwable {
        byte[] oldVal = { (byte) 0xFF };
        byte[] writeVal = { (byte) 0xFB };
        testDisable(oldVal, writeVal, true, TiSensorTagGatt.UUID_IO_SENSOR_VALUE,
                TiSensorTagGatt.UUID_IO_SENSOR_SERVICE, "switchOffBuzzer");
    }

    @Test
    public void testSwitchOffBuzzer() {
        TiSensorTag tag = new TiSensorTagBuilder(false, true).build(true);
        tag.switchOffBuzzer();
        // nothing to check - OK if it didn't fail
    }

    private void testDisable(boolean cc2650, UUID characteristic, UUID service, String method) throws Throwable {
        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(characteristic)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(cc2650, true).addService(service, svcMock).build(true);

        TestUtil.invokePrivate(tag, method);

        verify(bch, times(1)).writeValue(new byte[] { 0 });
    }

    private void testDisable(byte[] oldVal, byte[] writeVal, boolean cc2650, UUID characteristic, UUID service,
            String method) throws Throwable {

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        when(bch.readValue()).thenReturn(oldVal);

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(characteristic)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(cc2650, true).addService(service, svcMock).build(true);

        TestUtil.invokePrivate(tag, method);

        verify(bch, times(1)).writeValue(writeVal);
    }

    private void testDisableNotifications(boolean cc2650, UUID characteristic, UUID service, String method)
            throws Throwable {

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(characteristic)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(cc2650, true).addService(service, svcMock).build(true);

        TestUtil.invokePrivate(tag, method);

        verify(bch, times(1)).disableValueNotifications();
    }

    private void testEnable(byte[] oldVal, byte[] config, byte[] writeVal, boolean cc2650, UUID characteristic,
            UUID service, String method) throws Throwable {

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        when(bch.readValue()).thenReturn(oldVal);

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(characteristic)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(cc2650, true).addService(service, svcMock).build(true);

        TestUtil.invokePrivate(tag, method, config);

        verify(bch, times(1)).writeValue(writeVal);
    }

    private void testEnable(byte[] config, boolean cc2650, UUID characteristic, UUID service, String method)
            throws Throwable {

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(characteristic)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(cc2650, true).addService(service, svcMock).build(true);

        TestUtil.invokePrivate(tag, method, config);

        verify(bch, times(1)).writeValue(config);
    }

    private void testEnable(boolean cc2650, UUID characteristic, UUID service, String method) throws Throwable {
        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(characteristic)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(cc2650, true).addService(service, svcMock).build(true);

        TestUtil.invokePrivate(tag, method);

        byte[] config = new byte[] { 1 };

        verify(bch, times(1)).writeValue(config);
    }

    private void testEnableNotifications(byte[] val, double[] expected, boolean cc2650, UUID characteristic,
            UUID service, String method) throws Throwable {

        Consumer<double[]> callback = t -> assertArrayEquals(expected, t, EPS);

        testEnableNotifications(val, expected, cc2650, characteristic, service, method, callback);
    }

    private void testEnableNotifications(byte[] val, double expected, boolean cc2650, UUID characteristic, UUID service,
            String method) throws Throwable {

        Consumer<Double> callback = t -> assertEquals(expected, t, EPS);

        testEnableNotifications(val, expected, cc2650, characteristic, service, method, callback);
    }

    private void testEnableNotifications(byte[] val, float expected, boolean cc2650, UUID characteristic, UUID service,
            String method) throws Throwable {

        Consumer<Float> callback = t -> assertEquals(expected, t, EPS);

        testEnableNotifications(val, expected, cc2650, characteristic, service, method, callback);
    }

    private void testEnableNotifications(byte[] val, float[] expected, boolean cc2650, UUID characteristic,
            UUID service, String method) throws Throwable {

        Consumer<float[]> callback = t -> assertArrayEquals(expected, t, (float) EPS);

        testEnableNotifications(val, expected, cc2650, characteristic, service, method, callback);
    }

    private <T> void testEnableNotifications(byte[] val, T expected, boolean cc2650, UUID characteristic, UUID service,
            String method, Consumer<T> callback) throws Throwable {

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        doAnswer(invocation -> {
            Consumer<byte[]> consumer = invocation.getArgumentAt(0, Consumer.class);
            consumer.accept(val);
            return null;
        }).when(bch).enableValueNotifications(anyObject());

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(characteristic)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(cc2650, true).addService(service, svcMock).build(true);

        TestUtil.invokePrivate(tag, method, callback);
    }

    private void testRead(byte[] val, byte[] expected, boolean cc2650, UUID characteristic, UUID service, String method)
            throws Throwable {

        testRead(val, expected, cc2650, characteristic, service, method, Assert::assertArrayEquals);
    }

    private void testRead(byte[] val, float[] expected, boolean cc2650, UUID characteristic, UUID service,
            String method) throws Throwable {

        testRead(val, expected, cc2650, characteristic, service, method,
                (e, a) -> assertArrayEquals(e, a, (float) EPS));
    }

    private void testRead(byte[] val, double[] expected, boolean cc2650, UUID characteristic, UUID service,
            String method) throws Throwable {

        testRead(val, expected, cc2650, characteristic, service, method, (e, a) -> assertArrayEquals(e, a, EPS));
    }

    private void testRead(byte[] val, double expected, boolean cc2650, UUID characteristic, UUID service, String method)
            throws Throwable {

        testRead(val, expected, cc2650, characteristic, service, method, (e, a) -> assertEquals(e, a, EPS));
    }

    private void testRead(byte[] val, float expected, boolean cc2650, UUID characteristic, UUID service, String method)
            throws Throwable {

        testRead(val, expected, cc2650, characteristic, service, method, (e, a) -> assertEquals(e, a, EPS));
    }

    private <T> void testRead(byte[] val, T expected, boolean cc2650, UUID characteristic, UUID service, String method,
            Assertion<T> assertion) throws Throwable {

        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);
        when(bch.readValue()).thenReturn(val);

        BluetoothLeGattService infoSvcMock = mock(BluetoothLeGattService.class);
        when(infoSvcMock.findCharacteristic(characteristic)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(cc2650, true).addService(service, infoSvcMock).build(true);

        assertion.assertEq(expected, (T) TestUtil.invokePrivate(tag, method));
    }

    private void testSetPeriod(boolean cc2650, UUID characteristic, UUID service, String method) throws Throwable {
        BluetoothLeGattCharacteristic bch = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattService svcMock = mock(BluetoothLeGattService.class);
        when(svcMock.findCharacteristic(characteristic)).thenReturn(bch);

        TiSensorTag tag = new TiSensorTagBuilder(cc2650, true).addService(service, svcMock).build(true);

        TestUtil.invokePrivate(tag, method, 1000);

        verify(bch, times(1)).writeValue(new byte[] { -24 });
    }

}

interface Assertion<T> {

    void assertEq(T expected, T actual);
}

class TiSensorTagBuilder {

    private BluetoothLeDevice deviceMock;

    public TiSensorTagBuilder() {
        this(true, false);
    }

    public TiSensorTagBuilder(boolean cc2650, boolean connected) {
        deviceMock = mock(BluetoothLeDevice.class);

        if (cc2650) {
            when(deviceMock.getName()).thenReturn("CC2650 SensorTag");
        } else {
            when(deviceMock.getName()).thenReturn("CC2541");
        }

        if (connected) {
            when(deviceMock.isConnected()).thenReturn(true);
            when(deviceMock.isServicesResolved()).thenReturn(true);
        }

    }

    public TiSensorTagBuilder addService(UUID id, BluetoothLeGattService service)
            throws KuraBluetoothResourceNotFoundException {

        when(deviceMock.findService(id)).thenReturn(service);

        return this;
    }

    public TiSensorTagBuilder addCharacteristic(UUID serviceId, UUID CharacteristicId,
            BluetoothLeGattCharacteristic characteristic) throws KuraBluetoothResourceNotFoundException {

        when(deviceMock.findService(serviceId).findCharacteristic(CharacteristicId)).thenReturn(characteristic);

        return this;
    }

    public BluetoothLeDevice getDevice() {
        return this.deviceMock;
    }

    public TiSensorTag build(boolean connect) {
        TiSensorTag tag = new TiSensorTag(deviceMock);

        try {
            if (connect) {
                tag.connect();
            }
            tag.init();
        } catch (KuraBluetoothConnectionException e) {
            // Do nothing
        }

        return tag;
    }

}
