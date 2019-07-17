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
        ChannelRecord record = ChannelRecord.createReadRecord("PRESSURE", DataType.DOUBLE);
        Map<String, Object> config = new HashMap<>();
        config.put("xdk.address", xdkAddress);
        config.put("sensor.name", "PRESSURE");
        config.put("+name", "PRESSURE");
        config.put("+value.type", "DOUBLE");

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
    public void testReadNewXdk() throws ConnectionException, NoSuchFieldException, InterruptedException,
            ExecutionException, KuraBluetoothResourceNotFoundException, KuraBluetoothIOException {
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

        when(lowChrMock.readValue())
                .thenReturn(new byte[] { (byte) 0x01, (byte) 0x40, (byte) 0x24, (byte) 0x05, (byte) 0x00, (byte) 0x00,
                        (byte) 0xa8, (byte) 0x7f, (byte) 0x01, (byte) 0x00, (byte) 0xbd, (byte) 0x5c, (byte) 0x00,
                        (byte) 0x00, (byte) 0x2e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }); // 98216

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
        ChannelRecord record = ChannelRecord.createReadRecord("PRESSURE", DataType.DOUBLE);
        Map<String, Object> config = new HashMap<>();
        config.put("xdk.address", xdkAddress);
        config.put("sensor.name", "PRESSURE");
        config.put("+name", "PRESSURE");
        config.put("+value.type", "DOUBLE");

        record.setChannelConfig(config);
        records.add(record);

        svc.read(records);

        assertEquals(ChannelFlag.SUCCESS, record.getChannelStatus().getChannelFlag());
        assertEquals(98216.0, record.getValue().getValue());
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
