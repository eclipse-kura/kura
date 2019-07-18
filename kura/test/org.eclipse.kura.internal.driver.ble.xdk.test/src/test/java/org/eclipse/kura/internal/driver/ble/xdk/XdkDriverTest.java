package org.eclipse.kura.internal.driver.ble.xdk;

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

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothDiscoveryException;
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
import org.eclipse.kura.type.DataType;
import org.junit.Test;

public class XdkDriverTest {

    @Test
    public void testActivateDeactivate()
            throws KuraBluetoothDiscoveryException, NoSuchFieldException, KuraBluetoothConnectionException {

        String interfaceName = "hci0";
        boolean enableRotationQuaternion = false;
        int configureSampleRateHz = 10;

        XdkDriver svc = new XdkDriver();

        // try without BLE service
        Map<String, Object> properties = new HashMap<>();
        properties.put("iname", interfaceName);
        properties.put("enableRotationQuaternion", enableRotationQuaternion);
        properties.put("configureSampleRateHz", configureSampleRateHz);
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
        when(devMock.getName()).thenReturn("Xdk");
        when(devMock.isConnected()).thenReturn(true).thenReturn(false);

        Map<String, Xdk> XdkMap = (Map<String, Xdk>) TestUtil.getFieldValue(svc, "xdkMap");
        Xdk tist = new Xdk(devMock);
        XdkMap.put("12:34:56:78:90:AC", tist);

        // deactivate the service - disconnect and empty xdk
        svc.deactivate();

        verify(adapterMock, times(1)).isDiscovering();
        verify(adapterMock, times(1)).stopDiscovery();

        verify(devMock, times(2)).isConnected();
        verify(devMock, times(1)).disconnect();

        assertEquals(0, XdkMap.size());

        svc.unbindBluetoothLeService(bleMock);

        try {
            svc.activate(properties);
            fail("Exception was expected");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testUpdated()
            throws KuraBluetoothDiscoveryException, NoSuchFieldException, KuraBluetoothConnectionException {

        String interfaceName = "hci0";
        boolean enableRotationQuaternion = false;
        int configureSampleRateHz = 10;

        XdkDriver svc = new XdkDriver();

        // try without BLE service
        Map<String, Object> properties = new HashMap<>();
        properties.put("iname", interfaceName);
        properties.put("enableRotationQuaternion", enableRotationQuaternion);
        properties.put("configureSampleRateHz", configureSampleRateHz);
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

        svc.updated(properties);

        verify(adapterMock, times(1)).isPowered();
        verify(adapterMock, times(1)).getAddress();
        verify(adapterMock, times(1)).setPowered(true);
    }

    @Test
    public void testConnectNotConnected()
            throws NoSuchFieldException, ConnectionException, KuraBluetoothConnectionException {
        XdkDriver svc = new XdkDriver();

        Map<String, Xdk> xdkMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "xdkMap", xdkMap);

        Xdk tist = mock(Xdk.class);
        when(tist.isConnected()).thenReturn(false);

        xdkMap.put("12:34:56:78:90:AC", tist);

        svc.connect();

        verify(tist, times(1)).connect();
        verify(tist, times(2)).isConnected();

    }

    @Test
    public void testConnect() throws NoSuchFieldException, ConnectionException, KuraBluetoothConnectionException {
        XdkDriver svc = new XdkDriver();

        Map<String, Xdk> xdkMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "xdkMap", xdkMap);

        Xdk tist = mock(Xdk.class);
        when(tist.isConnected()).thenReturn(false).thenReturn(true);

        xdkMap.put("12:34:56:78:90:AC", tist);

        svc.connect();

        verify(tist, times(1)).connect();
        verify(tist, times(2)).isConnected();
    }

    @Test
    public void testChannelListeners() throws ConnectionException, NoSuchFieldException, InterruptedException,
            ExecutionException, KuraBluetoothResourceNotFoundException {
        String xdkAddress = "12:34:56:78:90:AC";
        XdkDriver svc = new XdkDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        Map<String, Xdk> XdkMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "xdkMap", XdkMap);

        Set<SensorListener> sensorListeners = new HashSet<>();
        TestUtil.setFieldValue(svc, "sensorListeners", sensorListeners);

        BluetoothLeGattService controlSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattService dataSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattCharacteristic rateChrMock = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattCharacteristic highChrMock = mock(BluetoothLeGattCharacteristic.class);
        BluetoothLeGattCharacteristic lowChrMock = mock(BluetoothLeGattCharacteristic.class);

        when(controlSvcMock.findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION))
                .thenReturn(rateChrMock);
        when(rateChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY))
                .thenReturn(highChrMock);
        when(highChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY)).thenReturn(lowChrMock);
        when(lowChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        Xdk xdk = new XdkBuilder(true).addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY,
                        highChrMock)
                .addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY,
                        lowChrMock)
                .addService(XdkGatt.UUID_XDK_CONTROL_SERVICE, controlSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION, rateChrMock)
                .build(true);
        XdkMap.put(xdkAddress, xdk);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord recordPressure = ChannelRecord.createReadRecord("PRESSURE", DataType.DOUBLE);
        ChannelRecord recordAccX = ChannelRecord.createReadRecord("ACCELERATION_X", DataType.INTEGER);
        ChannelRecord recordMX = ChannelRecord.createReadRecord("MAGNETIC_X", DataType.INTEGER);

        Map<String, Object> config = new HashMap<>();
        config.put("xdk.address", xdkAddress);
        config.put("sensor.name", "PRESSURE");
        config.put("+name", "PRESSURE");
        config.put("+value.type", "DOUBLE");

        recordPressure.setChannelConfig(config);
        records.add(recordPressure);

        Map<String, Object> configAccX = new HashMap<>();
        configAccX.put("xdk.address", xdkAddress);
        configAccX.put("sensor.name", "ACCELERATION_X");
        configAccX.put("+name", "ACCELERATION_X");
        configAccX.put("+value.type", "INTEGER");

        recordAccX.setChannelConfig(configAccX);
        records.add(recordAccX);

        Map<String, Object> configMX = new HashMap<>();
        configMX.put("xdk.address", xdkAddress);
        configMX.put("sensor.name", "MAGNETIC_X");
        configMX.put("+name", "MAGNETIC_X");
        configMX.put("+value.type", "INTEGER");

        recordMX.setChannelConfig(configMX);
        records.add(recordMX);

        ChannelListener listener = event -> {
        };

        svc.registerChannelListener(config, listener);
        assertEquals(1, sensorListeners.size());

        svc.unregisterChannelListener(listener);
        assertEquals(0, sensorListeners.size());
    }

    @Test
    public void testReadFailSensorName() throws ConnectionException {
        XdkDriver svc = new XdkDriver();

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("PRESSURE", DataType.DOUBLE);
        Map<String, Object> config = new HashMap<>();
        config.put("xdk.address", "12:34:56:78:90:AC");
        config.put("sensor.name", "BAROMETER");

        record.setChannelConfig(config);
        records.add(record);
        svc.read(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
        assertTrue(record.getChannelStatus().getExceptionMessage().contains("sensor name"));
    }

    @Test
    public void testReadNewXdk() throws Throwable {
        String xdkAddress = "12:34:56:78:90:AC";
        XdkDriver svc = new XdkDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        Map<String, Xdk> XdkMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "xdkMap", XdkMap);

        Set<SensorListener> sensorListeners = new HashSet<>();
        TestUtil.setFieldValue(svc, "sensorListeners", sensorListeners);

        BluetoothLeGattService controlSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattService dataSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattCharacteristic rateChrMock = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattCharacteristic highChrMock = mock(BluetoothLeGattCharacteristic.class);
        BluetoothLeGattCharacteristic lowChrMock = mock(BluetoothLeGattCharacteristic.class);

        when(controlSvcMock.findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION))
                .thenReturn(rateChrMock);
        when(rateChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY))
                .thenReturn(highChrMock);
        when(highChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY)).thenReturn(lowChrMock);
        when(lowChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(highChrMock.readValue())
                .thenReturn(new byte[] { (byte) 0x0B, (byte) 0x00, (byte) 0x16, (byte) 0x00, (byte) 0x09, (byte) 0x04,
                        (byte) 0x2C, (byte) 0x00, (byte) 0x37, (byte) 0x00, (byte) 0x42, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

        when(lowChrMock.readValue())
                .thenReturn(new byte[] { (byte) 0x01, (byte) 0x40, (byte) 0x24, (byte) 0x05, (byte) 0x00, (byte) 0x00,
                        (byte) 0xa8, (byte) 0x7f, (byte) 0x01, (byte) 0x00, (byte) 0xbd, (byte) 0x5c, (byte) 0x00,
                        (byte) 0x00, (byte) 0x2e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

        Xdk xdk = new XdkBuilder(true).addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY,
                        highChrMock)
                .addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY,
                        lowChrMock)
                .addService(XdkGatt.UUID_XDK_CONTROL_SERVICE, controlSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION, rateChrMock)
                .build(true);

        XdkMap.put(xdkAddress, xdk);

        List<ChannelRecord> records = new ArrayList<>();

        ChannelRecord recordHumidity = ChannelRecord.createReadRecord("HUMIDITY", DataType.LONG);
        ChannelRecord recordLight = ChannelRecord.createReadRecord("LIGHT", DataType.FLOAT);
        ChannelRecord recordPressure = ChannelRecord.createReadRecord("PRESSURE", DataType.DOUBLE);
        ChannelRecord recordTemperature = ChannelRecord.createReadRecord("TEMPERATURE", DataType.INTEGER);
        ChannelRecord recordButton = ChannelRecord.createReadRecord("BUTTON_STATUS", DataType.BOOLEAN);
        ChannelRecord recordNoise = ChannelRecord.createReadRecord("NOISE", DataType.STRING);

        Map<String, Object> configHumidity = new HashMap<>();
        configHumidity.put("xdk.address", xdkAddress);
        configHumidity.put("sensor.name", "HUMIDITY");
        configHumidity.put("+name", "HUMIDITY");
        configHumidity.put("+value.type", "LONG");

        recordHumidity.setChannelConfig(configHumidity);
        records.add(recordHumidity);

        Map<String, Object> configLight = new HashMap<>();
        configLight.put("xdk.address", xdkAddress);
        configLight.put("sensor.name", "LIGHT");
        configLight.put("+name", "LIGHT");
        configLight.put("+value.type", "FLOAT");

        recordLight.setChannelConfig(configLight);
        records.add(recordLight);

        Map<String, Object> configPressure = new HashMap<>();
        configPressure.put("xdk.address", xdkAddress);
        configPressure.put("sensor.name", "PRESSURE");
        configPressure.put("+name", "PRESSURE");
        configPressure.put("+value.type", "DOUBLE");

        recordPressure.setChannelConfig(configPressure);
        records.add(recordPressure);

        Map<String, Object> configTemperature = new HashMap<>();
        configTemperature.put("xdk.address", xdkAddress);
        configTemperature.put("sensor.name", "TEMPERATURE");
        configTemperature.put("+name", "TEMPERATURE");
        configTemperature.put("+value.type", "INTEGER");

        recordTemperature.setChannelConfig(configTemperature);
        records.add(recordTemperature);

        Map<String, Object> configButton = new HashMap<>();
        configButton.put("xdk.address", xdkAddress);
        configButton.put("sensor.name", "BUTTON_STATUS");
        configButton.put("+name", "BUTTON_STATUS");
        configButton.put("+value.type", "BOOLEAN");

        recordButton.setChannelConfig(configButton);
        records.add(recordButton);

        Map<String, Object> configNoise = new HashMap<>();
        configNoise.put("xdk.address", xdkAddress);
        configNoise.put("sensor.name", "NOISE");
        configNoise.put("+name", "NOISE");
        configNoise.put("+value.type", "STRING");

        recordNoise.setChannelConfig(configNoise);
        records.add(recordNoise);

        svc.read(records);

        assertEquals(ChannelFlag.SUCCESS, recordHumidity.getChannelStatus().getChannelFlag());
        assertEquals((long) 46, recordHumidity.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordLight.getChannelStatus().getChannelFlag());
        assertEquals((float) 336.0, recordLight.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordPressure.getChannelStatus().getChannelFlag());
        assertEquals((double) 98216.0, recordPressure.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordTemperature.getChannelStatus().getChannelFlag());
        assertEquals((int) 23, recordTemperature.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordButton.getChannelStatus().getChannelFlag());
        assertEquals((boolean) false, recordButton.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordNoise.getChannelStatus().getChannelFlag());
        assertEquals("0", recordNoise.getValue().getValue());
    }

    @Test
    public void testReadHighData() throws Throwable {
        String xdkAddress = "12:34:56:78:90:AC";
        XdkDriver svc = new XdkDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        Map<String, Xdk> XdkMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "xdkMap", XdkMap);

        Set<SensorListener> sensorListeners = new HashSet<>();
        TestUtil.setFieldValue(svc, "sensorListeners", sensorListeners);

        BluetoothLeGattService controlSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattService dataSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattCharacteristic rateChrMock = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattCharacteristic highChrMock = mock(BluetoothLeGattCharacteristic.class);
        BluetoothLeGattCharacteristic lowChrMock = mock(BluetoothLeGattCharacteristic.class);

        when(controlSvcMock.findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION))
                .thenReturn(rateChrMock);
        when(rateChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY))
                .thenReturn(highChrMock);
        when(highChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY)).thenReturn(lowChrMock);
        when(lowChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(highChrMock.readValue())
                .thenReturn(new byte[] { (byte) 0x0B, (byte) 0x00, (byte) 0x16, (byte) 0x00, (byte) 0x09, (byte) 0x04,
                        (byte) 0x2C, (byte) 0x00, (byte) 0x37, (byte) 0x00, (byte) 0x42, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

        when(lowChrMock.readValue())
                .thenReturn(new byte[] { (byte) 0x01, (byte) 0x40, (byte) 0x24, (byte) 0x05, (byte) 0x00, (byte) 0x00,
                        (byte) 0xa8, (byte) 0x7f, (byte) 0x01, (byte) 0x00, (byte) 0xbd, (byte) 0x5c, (byte) 0x00,
                        (byte) 0x00, (byte) 0x2e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

        Xdk xdk = new XdkBuilder(true).addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY,
                        highChrMock)
                .addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY,
                        lowChrMock)
                .addService(XdkGatt.UUID_XDK_CONTROL_SERVICE, controlSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION, rateChrMock)
                .build(true);

        XdkMap.put(xdkAddress, xdk);

        List<ChannelRecord> records = new ArrayList<>();

        ChannelRecord recordAccX = ChannelRecord.createReadRecord("ACCELERATION_X", DataType.INTEGER);
        ChannelRecord recordAccY = ChannelRecord.createReadRecord("ACCELERATION_Y", DataType.INTEGER);
        ChannelRecord recordAccZ = ChannelRecord.createReadRecord("ACCELERATION_Z", DataType.INTEGER);
        ChannelRecord recordGyroX = ChannelRecord.createReadRecord("GYROSCOPE_X", DataType.INTEGER);
        ChannelRecord recordGyroY = ChannelRecord.createReadRecord("GYROSCOPE_Y", DataType.INTEGER);
        ChannelRecord recordGyroZ = ChannelRecord.createReadRecord("GYROSCOPE_Z", DataType.INTEGER);

        Map<String, Object> configAccX = new HashMap<>();
        configAccX.put("xdk.address", xdkAddress);
        configAccX.put("sensor.name", "ACCELERATION_X");
        configAccX.put("+name", "ACCELERATION_X");
        configAccX.put("+value.type", "INTEGER");

        recordAccX.setChannelConfig(configAccX);
        records.add(recordAccX);

        Map<String, Object> configAccY = new HashMap<>();
        configAccY.put("xdk.address", xdkAddress);
        configAccY.put("sensor.name", "ACCELERATION_Y");
        configAccY.put("+name", "ACCELERATION_Y");
        configAccY.put("+value.type", "INTEGER");

        recordAccY.setChannelConfig(configAccY);
        records.add(recordAccY);

        Map<String, Object> configAccZ = new HashMap<>();
        configAccZ.put("xdk.address", xdkAddress);
        configAccZ.put("sensor.name", "ACCELERATION_Z");
        configAccZ.put("+name", "ACCELERATION_Z");
        configAccZ.put("+value.type", "INTEGER");

        recordAccZ.setChannelConfig(configAccZ);
        records.add(recordAccZ);

        Map<String, Object> configGyroX = new HashMap<>();
        configGyroX.put("xdk.address", xdkAddress);
        configGyroX.put("sensor.name", "GYROSCOPE_X");
        configGyroX.put("+name", "GYROSCOPE_X");
        configGyroX.put("+value.type", "INTEGER");

        recordGyroX.setChannelConfig(configGyroX);
        records.add(recordGyroX);

        Map<String, Object> configGyroY = new HashMap<>();
        configGyroY.put("xdk.address", xdkAddress);
        configGyroY.put("sensor.name", "GYROSCOPE_Y");
        configGyroY.put("+name", "GYROSCOPE_Y");
        configGyroY.put("+value.type", "INTEGER");

        recordGyroY.setChannelConfig(configGyroY);
        records.add(recordGyroY);

        Map<String, Object> configGyroZ = new HashMap<>();
        configGyroZ.put("xdk.address", xdkAddress);
        configGyroZ.put("sensor.name", "GYROSCOPE_Z");
        configGyroZ.put("+name", "GYROSCOPE_Z");
        configGyroZ.put("+value.type", "INTEGER");

        recordGyroZ.setChannelConfig(configGyroZ);
        records.add(recordGyroZ);

        svc.read(records);

        assertEquals(ChannelFlag.SUCCESS, recordAccX.getChannelStatus().getChannelFlag());
        assertEquals((int) 11, recordAccX.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordAccY.getChannelStatus().getChannelFlag());
        assertEquals((int) 22, recordAccY.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordAccZ.getChannelStatus().getChannelFlag());
        assertEquals((int) 1033, recordAccZ.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordGyroX.getChannelStatus().getChannelFlag());
        assertEquals((int) 44, recordGyroX.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordGyroY.getChannelStatus().getChannelFlag());
        assertEquals((int) 55, recordGyroY.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordGyroZ.getChannelStatus().getChannelFlag());
        assertEquals((int) 66, recordGyroZ.getValue().getValue());
    }

    @Test
    public void testReadLowDataMessageTwo() throws Throwable {
        String xdkAddress = "12:34:56:78:90:AC";
        XdkDriver svc = new XdkDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        Map<String, Xdk> XdkMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "xdkMap", XdkMap);

        Set<SensorListener> sensorListeners = new HashSet<>();
        TestUtil.setFieldValue(svc, "sensorListeners", sensorListeners);

        BluetoothLeGattService controlSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattService dataSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattCharacteristic rateChrMock = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattCharacteristic highChrMock = mock(BluetoothLeGattCharacteristic.class);
        BluetoothLeGattCharacteristic lowChrMock = mock(BluetoothLeGattCharacteristic.class);

        when(controlSvcMock.findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION))
                .thenReturn(rateChrMock);
        when(rateChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY))
                .thenReturn(highChrMock);
        when(highChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY)).thenReturn(lowChrMock);
        when(lowChrMock.isNotifying()).thenReturn(false).thenReturn(true);

        when(highChrMock.readValue())
                .thenReturn(new byte[] { (byte) 0x0B, (byte) 0x00, (byte) 0x16, (byte) 0x00, (byte) 0x09, (byte) 0x04,
                        (byte) 0x2C, (byte) 0x00, (byte) 0x37, (byte) 0x00, (byte) 0x42, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

        when(lowChrMock.readValue())
                .thenReturn(new byte[] { (byte) 0x02, (byte) 0xea, (byte) 0xff, (byte) 0xf3, (byte) 0xff, (byte) 0xc2,
                        (byte) 0xff, (byte) 0xa1, (byte) 0x18, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

        Xdk xdk = new XdkBuilder(true).addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY,
                        highChrMock)
                .addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY,
                        lowChrMock)
                .addService(XdkGatt.UUID_XDK_CONTROL_SERVICE, controlSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION, rateChrMock)
                .build(true);

        XdkMap.put(xdkAddress, xdk);

        List<ChannelRecord> records = new ArrayList<>();

        ChannelRecord recordMX = ChannelRecord.createReadRecord("MAGNETIC_X", DataType.INTEGER);
        ChannelRecord recordMY = ChannelRecord.createReadRecord("MAGNETIC__Y", DataType.INTEGER);
        ChannelRecord recordMZ = ChannelRecord.createReadRecord("MAGNETIC__Z", DataType.INTEGER);
        ChannelRecord recordMR = ChannelRecord.createReadRecord("MAGNETOMETER_RESISTANCE", DataType.INTEGER);
        ChannelRecord recordLed = ChannelRecord.createReadRecord("LED_STATUS", DataType.INTEGER);
        ChannelRecord recordVoltage = ChannelRecord.createReadRecord("VOLTAGE_LEM", DataType.INTEGER);

        Map<String, Object> configMX = new HashMap<>();
        configMX.put("xdk.address", xdkAddress);
        configMX.put("sensor.name", "MAGNETIC_X");
        configMX.put("+name", "MAGNETIC_X");
        configMX.put("+value.type", "INTEGER");

        recordMX.setChannelConfig(configMX);
        records.add(recordMX);

        Map<String, Object> configMY = new HashMap<>();
        configMY.put("xdk.address", xdkAddress);
        configMY.put("sensor.name", "MAGNETIC_Y");
        configMY.put("+name", "MAGNETIC_Y");
        configMY.put("+value.type", "INTEGER");

        recordMY.setChannelConfig(configMY);
        records.add(recordMY);

        Map<String, Object> configMZ = new HashMap<>();
        configMZ.put("xdk.address", xdkAddress);
        configMZ.put("sensor.name", "MAGNETIC_Z");
        configMZ.put("+name", "MAGNETIC_Z");
        configMZ.put("+value.type", "INTEGER");

        recordMZ.setChannelConfig(configMZ);
        records.add(recordMZ);

        Map<String, Object> configMR = new HashMap<>();
        configMR.put("xdk.address", xdkAddress);
        configMR.put("sensor.name", "MAGNETOMETER_RESISTANCE");
        configMR.put("+name", "MAGNETOMETER_RESISTANCE");
        configMR.put("+value.type", "INTEGER");

        recordMR.setChannelConfig(configMR);
        records.add(recordMR);

        Map<String, Object> configLed = new HashMap<>();
        configLed.put("xdk.address", xdkAddress);
        configLed.put("sensor.name", "LED_STATUS");
        configLed.put("+name", "LED_STATUS");
        configLed.put("+value.type", "INTEGER");

        recordLed.setChannelConfig(configLed);
        records.add(recordLed);

        Map<String, Object> configVoltage = new HashMap<>();
        configVoltage.put("xdk.address", xdkAddress);
        configVoltage.put("sensor.name", "VOLTAGE_LEM");
        configVoltage.put("+name", "VOLTAGE_LEM");
        configVoltage.put("+value.type", "INTEGER");

        recordVoltage.setChannelConfig(configVoltage);
        records.add(recordVoltage);

        svc.read(records);

        assertEquals(ChannelFlag.SUCCESS, recordMX.getChannelStatus().getChannelFlag());
        assertEquals((int) -22, recordMX.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordMY.getChannelStatus().getChannelFlag());
        assertEquals((int) -13, recordMY.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordMZ.getChannelStatus().getChannelFlag());
        assertEquals((int) -62, recordMZ.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordMR.getChannelStatus().getChannelFlag());
        assertEquals((int) 6305, recordMR.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordLed.getChannelStatus().getChannelFlag());
        assertEquals((int) 2, recordLed.getValue().getValue());

        assertEquals(ChannelFlag.SUCCESS, recordVoltage.getChannelStatus().getChannelFlag());
        assertEquals((int) 0, recordVoltage.getValue().getValue());
    }

    @Test
    public void testReadFailReadException() throws ConnectionException, NoSuchFieldException, KuraException {

        String xdkAddress = "12:34:56:78:90:AC";
        XdkDriver svc = new XdkDriver();

        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        Map<String, Xdk> XdkMap = new HashMap<>();
        TestUtil.setFieldValue(svc, "xdkMap", XdkMap);

        Set<SensorListener> sensorListeners = new HashSet<>();
        TestUtil.setFieldValue(svc, "sensorListeners", sensorListeners);

        BluetoothLeGattService controlSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattService dataSvcMock = mock(BluetoothLeGattService.class);

        BluetoothLeGattCharacteristic rateChrMock = mock(BluetoothLeGattCharacteristic.class);

        BluetoothLeGattCharacteristic highChrMock = mock(BluetoothLeGattCharacteristic.class);
        BluetoothLeGattCharacteristic lowChrMock = mock(BluetoothLeGattCharacteristic.class);

        when(controlSvcMock.findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION))
                .thenReturn(rateChrMock);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY))
                .thenReturn(highChrMock);

        when(dataSvcMock.findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY)).thenReturn(lowChrMock);

        Xdk xdk = new XdkBuilder(true).addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY,
                        highChrMock)
                .addService(XdkGatt.UUID_XDK_HIGH_DATA_RATE, dataSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE, XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY,
                        lowChrMock)
                .addService(XdkGatt.UUID_XDK_CONTROL_SERVICE, controlSvcMock)
                .addCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE,
                        XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION, rateChrMock)
                .build(true);

        XdkMap.put(xdkAddress, xdk);

        doThrow(new KuraBluetoothResourceNotFoundException("test")).when(dataSvcMock)
                .findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("PRESSURE", DataType.DOUBLE);
        Map<String, Object> config = new HashMap<>();
        config.put("xdk.address", xdkAddress);
        config.put("sensor.name", "PRESSURE");
        config.put("+name", "PRESSURE");
        config.put("+value.type", "DOUBLE");

        record.setChannelConfig(config);
        records.add(record);

        svc.read(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
        assertTrue(record.getChannelStatus().getExceptionMessage().contains("Xdk Read Operation Failed"));
    }

    class XdkBuilder {

        private BluetoothLeDevice deviceMock;

        public XdkBuilder() {
            this(false);
        }

        public XdkBuilder(boolean connected) {
            deviceMock = mock(BluetoothLeDevice.class);

            when(deviceMock.getName()).thenReturn("Xdk");

            if (connected) {
                when(deviceMock.isConnected()).thenReturn(true);
            }
        }

        public XdkBuilder addService(UUID id, BluetoothLeGattService service)
                throws KuraBluetoothResourceNotFoundException {

            when(deviceMock.findService(id)).thenReturn(service);

            return this;
        }

        public XdkBuilder addCharacteristic(UUID serviceId, UUID CharacteristicId,
                BluetoothLeGattCharacteristic characteristic) throws KuraBluetoothResourceNotFoundException {

            when(deviceMock.findService(serviceId).findCharacteristic(CharacteristicId)).thenReturn(characteristic);

            return this;
        }

        public BluetoothLeDevice getDevice() {
            return this.deviceMock;
        }

        public Xdk build(boolean connect) {
            Xdk xdk = new Xdk(deviceMock);

            try {
                if (connect) {
                    xdk.connect();
                }
                xdk.init();
            } catch (ConnectionException e) {
                // Do nothing
            }

            return xdk;
        }

    }

}
