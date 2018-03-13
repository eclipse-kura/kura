/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.driver.ble.sensortag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothDiscoveryException;
import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.TypedValue;
import org.junit.Test;

public class SensorTagDriverTest {

    @Test
    public void testActivateDeactivate()
            throws KuraBluetoothDiscoveryException, NoSuchFieldException, KuraBluetoothConnectionException {

        String interfaceName = "hci0";

        SensorTagDriver svc = new SensorTagDriver();

        // try without BLE service
        Map<String, Object> properties = new HashMap<>();
        properties.put("iname", interfaceName);
        try {
            svc.activate(properties);
            fail("Exception was expected");
        } catch (NullPointerException e) {
        }

        // init BLE service and activate
        BluetoothLeAdapter adapterMock = mock(BluetoothLeAdapter.class);
        when(adapterMock.isPowered()).thenReturn(false);
        when(adapterMock.isDiscovering()).thenReturn(true);
        when(adapterMock.getAddress()).thenReturn("12:34:56:78:90:AB");

        BluetoothLeService bleMock = mock(BluetoothLeService.class);
        when(bleMock.getAdapter(interfaceName)).thenReturn(adapterMock);
        svc.bindBluetoothLeService(bleMock);

        svc.activate(properties);

        verify(adapterMock, times(1)).isPowered();
        verify(adapterMock, times(1)).getAddress();
        verify(adapterMock, times(1)).setPowered(true);

        BluetoothLeDevice devMock = mock(BluetoothLeDevice.class);
        when(devMock.getName()).thenReturn("CC2451");
        when(devMock.isConnected()).thenReturn(true).thenReturn(false);

        Map<String, TiSensorTag> tiSensorTagMap = (Map<String, TiSensorTag>) TestUtil.getFieldValue(svc,
                "tiSensorTagMap");
        TiSensorTag tist = new TiSensorTag(devMock);
        tiSensorTagMap.put("12:34:56:78:90:AC", tist);

        // deactivate the service - disconnect and empty sensor tags
        svc.deactivate();

        verify(adapterMock, times(1)).isDiscovering();
        verify(adapterMock, times(1)).stopDiscovery();

        verify(devMock, times(2)).isConnected();
        verify(devMock, times(1)).disconnect();

        assertEquals(0, tiSensorTagMap.size());

        // unbind BLE service and try activation again
        svc.unbindBluetoothLeService(bleMock);

        try {
            svc.activate(properties);
            fail("Exception was expected");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testConnectNotConnected()
            throws NoSuchFieldException, ConnectionException, KuraBluetoothConnectionException {
        SensorTagDriver svc = new SensorTagDriver();

        Map<String, TiSensorTag> tiSensorTagMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "tiSensorTagMap", tiSensorTagMap);

        TiSensorTag tist = mock(TiSensorTag.class);
        when(tist.isConnected()).thenReturn(false);

        tiSensorTagMap.put("12:34:56:78:90:AC", tist);

        svc.connect();

        verify(tist, times(1)).connect();
        verify(tist, times(2)).isConnected();
        verify(tist, times(0)).enableTermometer();
    }

    @Test
    public void testConnect() throws NoSuchFieldException, ConnectionException, KuraBluetoothConnectionException {
        SensorTagDriver svc = new SensorTagDriver();

        Map<String, TiSensorTag> tiSensorTagMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "tiSensorTagMap", tiSensorTagMap);

        TiSensorTag tist = mock(TiSensorTag.class);
        when(tist.isConnected()).thenReturn(false).thenReturn(true);
        when(tist.enableIOService()).thenReturn(true);

        tiSensorTagMap.put("12:34:56:78:90:AC", tist);

        svc.connect();

        verify(tist, times(1)).connect();
        verify(tist, times(2)).isConnected();

        verify(tist, times(1)).enableTermometer();

        verify(tist, times(1)).setAccelerometerPeriod(50);
        verify(tist, times(1)).enableAccelerometer(new byte[] { 1 });

        verify(tist, times(1)).enableHygrometer();

        verify(tist, times(1)).setMagnetometerPeriod(50);
        verify(tist, times(1)).enableMagnetometer(new byte[] { 1 });

        verify(tist, times(1)).calibrateBarometer();
        verify(tist, times(1)).enableBarometer();
        verify(tist, times(1)).enableGyroscope(new byte[] { 7 });

        verify(tist, times(1)).enableLuxometer();

        verify(tist, times(1)).enableIOService();
        verify(tist, times(1)).switchOffBuzzer();
        verify(tist, times(1)).switchOffGreenLed();
        verify(tist, times(1)).switchOffRedLed();
    }

    @Test
    public void testConnectCc2650() throws NoSuchFieldException, ConnectionException, KuraBluetoothConnectionException {
        SensorTagDriver svc = new SensorTagDriver();

        Map<String, TiSensorTag> tiSensorTagMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "tiSensorTagMap", tiSensorTagMap);

        TiSensorTag tist = mock(TiSensorTag.class);
        when(tist.isCC2650()).thenReturn(true);
        when(tist.isConnected()).thenReturn(false).thenReturn(true);
        when(tist.enableIOService()).thenReturn(true);

        tiSensorTagMap.put("12:34:56:78:90:AC", tist);

        svc.connect();

        verify(tist, times(1)).connect();
        verify(tist, times(2)).isConnected();

        verify(tist, times(1)).enableAccelerometer(new byte[] { 0x38, 2 });

        verify(tist, times(1)).enableMagnetometer(new byte[] { 0x40, 0 });

        verify(tist, times(1)).setGyroscopePeriod(50);
        verify(tist, times(1)).enableGyroscope(new byte[] { 7, 0 });
    }

    @Test
    public void testChannelListeners() throws ConnectionException, NoSuchFieldException, InterruptedException,
            ExecutionException, KuraBluetoothResourceNotFoundException {
        String tagAddress = "12:34:56:78:90:AC";
        SensorTagDriver svc = new SensorTagDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        Map<String, TiSensorTag> tiSensorTagMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "tiSensorTagMap", tiSensorTagMap);

        Set<SensorListener> sensorListeners = new HashSet<>();
        TestUtil.setFieldValue(svc, "sensorListeners", sensorListeners);

        BluetoothLeGattService preSvcMock = mock(BluetoothLeGattService.class);
        BluetoothLeGattCharacteristic preChrMock = mock(BluetoothLeGattCharacteristic.class);
        BluetoothLeGattCharacteristic prePeriodChrMock = mock(BluetoothLeGattCharacteristic.class);
        when(preSvcMock.findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE)).thenReturn(preChrMock);
        when(preChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        TiSensorTag tag = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, preSvcMock)
                .addCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, TiSensorTagGatt.UUID_PRE_SENSOR_VALUE,
                        preChrMock)
                .addCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, TiSensorTagGatt.UUID_PRE_SENSOR_PERIOD,
                        prePeriodChrMock)
                .build(true);
        tiSensorTagMap.put(tagAddress, tag);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("pressure", DataType.DOUBLE);
        Map<String, Object> config = new HashMap<>();
        config.put("sensortag.address", tagAddress);
        config.put("sensor.name", "PRESSURE");
        config.put("notification.period", "1000");
        config.put("+name", "pressure");
        config.put("+value.type", "double");

        record.setChannelConfig(config);
        records.add(record);
        ChannelListener listener = event -> {
        };

        svc.registerChannelListener(config, listener);
        assertEquals(1, sensorListeners.size());

        svc.unregisterChannelListener(listener);
        assertEquals(0, sensorListeners.size());
    }

    @Test
    public void testReadFailSensorName() throws ConnectionException {
        SensorTagDriver svc = new SensorTagDriver();

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("pressure", DataType.DOUBLE);
        Map<String, Object> config = new HashMap<>();
        config.put("sensortag.address", "12:34:56:78:90:AC");
        config.put("sensor.name", "barometer");

        record.setChannelConfig(config);
        records.add(record);
        svc.read(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
        assertTrue(record.getChannelStatus().getExceptionMessage().contains("sensor name"));
    }

    @Test
    public void testReadNewSensorTag() throws ConnectionException, NoSuchFieldException, InterruptedException,
            ExecutionException, KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
        String address = "12:34:56:78:90:AC";

        SensorTagDriver svc = new SensorTagDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        // for getSensorTag
        Future<BluetoothLeDevice> bldFuture = mock(Future.class);
        when(bluetoothLeAdapter.findDeviceByAddress(5, address)).thenReturn(bldFuture);
        BluetoothLeDevice bldMock = mock(BluetoothLeDevice.class);
        when(bldFuture.get()).thenReturn(bldMock);
        when(bldMock.getName()).thenReturn("CC2650 SensorTag");

        when(bldMock.isConnected()).thenReturn(true);

        // getGattServices
        BluetoothLeGattService gattSvcMock = mock(BluetoothLeGattService.class);
        BluetoothLeGattCharacteristic blgcMock = mock(BluetoothLeGattCharacteristic.class);
        when(blgcMock.readValue()).thenReturn(new byte[] { 0, 1, 2, (byte) 0xB4, (byte) 0x8B, 0x01 }); // 1013
        when(gattSvcMock.findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE)).thenReturn(blgcMock);
        when(bldMock.findService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE)).thenReturn(gattSvcMock);
        // end for getSensorTag

        Map<String, TiSensorTag> tiSensorTagMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "tiSensorTagMap", tiSensorTagMap);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("pressure", DataType.DOUBLE);
        Map<String, Object> config = new HashMap<>();
        config.put("sensortag.address", address);
        config.put("sensor.name", "PRESSURE");

        record.setChannelConfig(config);
        records.add(record);
        svc.read(records);

        assertEquals(ChannelFlag.SUCCESS, record.getChannelStatus().getChannelFlag());
        assertEquals(1013.0, record.getValue().getValue());
    }

    @Test
    public void testReadFailReadException() throws ConnectionException, NoSuchFieldException, KuraException {
        String address = "12:34:56:78:90:AC";

        SensorTagDriver svc = new SensorTagDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        Map<String, TiSensorTag> tiSensorTagMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "tiSensorTagMap", tiSensorTagMap);

        BluetoothLeGattService preSvcMock = mock(BluetoothLeGattService.class);
        doThrow(new KuraBluetoothResourceNotFoundException("test")).when(preSvcMock)
                .findCharacteristic(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE);

        TiSensorTag tag = new TiSensorTagBuilder(true, true)
                .addService(TiSensorTagGatt.UUID_PRE_SENSOR_SERVICE, preSvcMock).build(true);
        tiSensorTagMap.put(address, tag);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("pressure", DataType.DOUBLE);
        Map<String, Object> config = new HashMap<>();
        config.put("sensortag.address", address);
        config.put("sensor.name", "PRESSURE");
        record.setChannelConfig(config);
        records.add(record);

        svc.read(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
        assertTrue(record.getChannelStatus().getExceptionMessage().contains("SensortTag Read Operation Failed"));
    }

    @Test
    public void testReadResult() throws Throwable {
        SensorTagDriver svc = new SensorTagDriver();
        TiSensorTag tist = mock(TiSensorTag.class);

        when(tist.readAcceleration()).thenReturn(new double[] { 1, 2, 3 });
        when(tist.readGyroscope()).thenReturn(new float[] { 1, 2, 3 });
        when(tist.readMagneticField()).thenReturn(new float[] { 1, 2, 3 });
        when(tist.readTemperature()).thenReturn(new double[] { 1, 2 });

        try {
            TestUtil.invokePrivate(svc, "getReadResult", SensorName.BUZZER, tist);
            fail("Exception was expected");
        } catch (KuraBluetoothIOException e) {
        }

        try {
            TestUtil.invokePrivate(svc, "getReadResult", SensorName.GREEN_LED, tist);
            fail("Exception was expected");
        } catch (KuraBluetoothIOException e) {
        }

        try {
            TestUtil.invokePrivate(svc, "getReadResult", SensorName.RED_LED, tist);
            fail("Exception was expected");
        } catch (KuraBluetoothIOException e) {
        }

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.TEMP_AMBIENT, tist);

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.TEMP_TARGET, tist);
        verify(tist, times(2)).readTemperature();

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.HUMIDITY, tist);
        verify(tist, times(1)).readHumidity();

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.ACCELERATION_X, tist);

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.ACCELERATION_Y, tist);

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.ACCELERATION_Z, tist);
        verify(tist, times(3)).readAcceleration();

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.MAGNETIC_X, tist);

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.MAGNETIC_Y, tist);

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.MAGNETIC_Z, tist);
        verify(tist, times(3)).readMagneticField();

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.GYROSCOPE_X, tist);

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.GYROSCOPE_Y, tist);

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.GYROSCOPE_Z, tist);
        verify(tist, times(3)).readGyroscope();

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.LIGHT, tist);
        verify(tist, times(1)).readLight();

        TestUtil.invokePrivate(svc, "getReadResult", SensorName.PRESSURE, tist);
        verify(tist, times(1)).readPressure();
    }

    @Test
    public void testWriteDoubleFailure()
            throws NoSuchFieldException, InterruptedException, ExecutionException, ConnectionException {

        String address = "12:34:56:78:90:AC";

        SensorTagDriver svc = new SensorTagDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        // for getSensorTag
        Future<BluetoothLeDevice> bldFuture = mock(Future.class);
        when(bluetoothLeAdapter.findDeviceByAddress(5, address)).thenReturn(bldFuture);
        BluetoothLeDevice bldMock = mock(BluetoothLeDevice.class);
        when(bldFuture.get()).thenReturn(bldMock);
        when(bldMock.getName()).thenReturn("CC2650 SensorTag");

        when(bldMock.isConnected()).thenReturn(false).thenReturn(true).thenReturn(false);
        // end for getSensorTag

        Map<String, TiSensorTag> tiSensorTagMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "tiSensorTagMap", tiSensorTagMap);

        List<ChannelRecord> records = new ArrayList<>();
        TypedValue<Double> val = new DoubleValue(101.3);
        ChannelRecord record = ChannelRecord.createWriteRecord("pressure", val);
        Map<String, Object> config = new HashMap<>();
        config.put("sensortag.address", address);
        config.put("sensor.name", "PRESSURE");
        record.setChannelConfig(config);
        records.add(record);

        svc.write(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
        assertTrue(record.getChannelStatus().getExceptionMessage().contains("SensortTag Write Operation Failed"));
    }

    @Test
    public void testWriteGreen() throws NoSuchFieldException, InterruptedException, ExecutionException,
            KuraBluetoothIOException, KuraBluetoothResourceNotFoundException, ConnectionException {

        String sensorName = "GREEN_LED";
        byte[] writeOn = new byte[] { 2 };
        byte[] writeOff = new byte[] { (byte) 0xFD };

        testWrite(sensorName, writeOn, writeOff);
    }

    @Test
    public void testWriteRed() throws NoSuchFieldException, InterruptedException, ExecutionException,
            KuraBluetoothIOException, KuraBluetoothResourceNotFoundException, ConnectionException {

        String sensorName = "RED_LED";
        byte[] writeOn = new byte[] { 1 };
        byte[] writeOff = new byte[] { (byte) 0xFE };

        testWrite(sensorName, writeOn, writeOff);
    }

    @Test
    public void testWriteBuzzer() throws NoSuchFieldException, InterruptedException, ExecutionException,
            KuraBluetoothIOException, KuraBluetoothResourceNotFoundException, ConnectionException {

        String sensorName = "BUZZER";
        byte[] writeOn = new byte[] { 4 };
        byte[] writeOff = new byte[] { (byte) 0xFB };

        testWrite(sensorName, writeOn, writeOff);
    }

    public void testWrite(String sensorName, byte[] writeOn, byte[] writeOff)
            throws NoSuchFieldException, InterruptedException, ExecutionException, KuraBluetoothIOException,
            KuraBluetoothResourceNotFoundException, ConnectionException {

        String address = "12:34:56:78:90:AC";

        SensorTagDriver svc = new SensorTagDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        // for getSensorTag
        Future<BluetoothLeDevice> bldFuture = mock(Future.class);
        when(bluetoothLeAdapter.findDeviceByAddress(5, address)).thenReturn(bldFuture);
        BluetoothLeDevice bldMock = mock(BluetoothLeDevice.class);
        when(bldFuture.get()).thenReturn(bldMock);
        when(bldMock.getName()).thenReturn("CC2650 SensorTag");

        when(bldMock.isConnected()).thenReturn(true);

        // getGattServices
        BluetoothLeGattService gattSvcMock = mock(BluetoothLeGattService.class);
        BluetoothLeGattCharacteristic blgcMock = mock(BluetoothLeGattCharacteristic.class);
        when(gattSvcMock.findCharacteristic(TiSensorTagGatt.UUID_IO_SENSOR_VALUE)).thenReturn(blgcMock);
        // all off, all on
        when(blgcMock.readValue()).thenReturn(new byte[] { 0 }).thenReturn(new byte[] { (byte) 0xFF });
        when(bldMock.findService(TiSensorTagGatt.UUID_IO_SENSOR_SERVICE)).thenReturn(gattSvcMock);
        // end for getSensorTag

        Map<String, TiSensorTag> tiSensorTagMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "tiSensorTagMap", tiSensorTagMap);

        List<ChannelRecord> records = new ArrayList<>();
        TypedValue<Boolean> val = new BooleanValue(true);
        ChannelRecord record = ChannelRecord.createWriteRecord(sensorName, val);
        Map<String, Object> config = new HashMap<>();
        config.put("sensortag.address", address);
        config.put("sensor.name", sensorName);
        record.setChannelConfig(config);
        records.add(record);

        val = new BooleanValue(false);
        record = ChannelRecord.createWriteRecord(sensorName, val);
        config = new HashMap<>();
        config.put("sensortag.address", address);
        config.put("sensor.name", sensorName);
        record.setChannelConfig(config);
        records.add(record);

        svc.write(records);

        assertEquals(ChannelFlag.SUCCESS, record.getChannelStatus().getChannelFlag());
        verify(blgcMock, times(1)).writeValue(writeOn);
        verify(blgcMock, times(1)).writeValue(writeOff);
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
            } catch (ConnectionException e) {
                // Do nothing
            }

            return tag;
        }

    }
}
