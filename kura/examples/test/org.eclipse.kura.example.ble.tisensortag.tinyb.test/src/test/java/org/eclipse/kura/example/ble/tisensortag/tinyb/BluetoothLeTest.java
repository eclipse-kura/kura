/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.example.ble.tisensortag.tinyb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.example.testutil.TestUtil;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class BluetoothLeTest {

    @Test
    public void testActivateDeactivate() throws NoSuchFieldException, KuraException {
        BluetoothLe svc = new BluetoothLe();

        CloudPublisher cpMock = mock(CloudPublisher.class);
        svc.setCloudPublisher(cpMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("iname", "hci0");
        properties.put("scan_enable", false); // stop fast in doUpdate
        properties.put("enableButtons", true); // will cause disable key notifications to be called

        svc.activate(null, properties);

        assertNull(TestUtil.getFieldValue(svc, "worker"));
        assertNotNull(TestUtil.getFieldValue(svc, "tiSensorTagList"));

        // prepare for deactivation
        BluetoothLeAdapter bluetoothLeAdapter = mock(BluetoothLeAdapter.class);
        when(bluetoothLeAdapter.isDiscovering()).thenReturn(true);
        TestUtil.setFieldValue(svc, "bluetoothLeAdapter", bluetoothLeAdapter);

        List<TiSensorTag> tiSensorTagList = new ArrayList<>();
        TiSensorTag tistMock = mock(TiSensorTag.class);
        when(tistMock.isConnected()).thenReturn(true);
        tiSensorTagList.add(tistMock);
        TestUtil.setFieldValue(svc, "tiSensorTagList", tiSensorTagList);

        svc.deactivate(null);

        verify(bluetoothLeAdapter, times(1)).stopDiscovery();
        verify(tistMock, times(1)).disconnect();
        assertNull(TestUtil.getFieldValue(svc, "bluetoothLeAdapter"));

        svc.unsetCloudPublisher(cpMock);

        assertNull(TestUtil.getFieldValue(svc, "cloudPublisher"));
    }

    @Test
    public void testUpdate() throws NoSuchFieldException, KuraException, InterruptedException, ExecutionException {
        String interfaceName = "hci0";

        BluetoothLe svc = new BluetoothLe();

        CloudPublisher cpMock = mock(CloudPublisher.class);
        svc.setCloudPublisher(cpMock);

        BluetoothLeService bleMock = mock(BluetoothLeService.class);
        BluetoothLeAdapter adapterMock = mock(BluetoothLeAdapter.class);
        when(adapterMock.getAddress()).thenReturn("12:34:56:78:90:AB");
        when(bleMock.getAdapter(interfaceName)).thenReturn(adapterMock);
        svc.setBluetoothLeService(bleMock);

        // for performScan
        when(adapterMock.isDiscovering()).thenReturn(true); // so it can be made to stop
        Future<List<BluetoothLeDevice>> devFuture = mock(Future.class);
        List<BluetoothLeDevice> devices = new ArrayList<>();
        // for performScan->filterDevices
        BluetoothLeDevice devMock = mock(BluetoothLeDevice.class);
        when(devMock.getName()).thenReturn("CC2650 SensorTag");
        when(devMock.getAddress()).thenReturn("12:34:56:78:90:AC");
        when(devMock.isConnected()).thenReturn(false);
        doAnswer(inv -> {
            synchronized (devMock) {
                devMock.notifyAll(); // stop waiting after connect is called in readSensorTags
            }
            throw new KuraBluetoothConnectionException("test"); // for testing logging
        }).when(devMock).connect();
        devices.add(devMock);
        BluetoothLeDevice devMock2 = mock(BluetoothLeDevice.class);
        when(devMock2.getName()).thenReturn("CC2451 SensorTag");
        // add the same address, to check that duplicate address is not added
        when(devMock2.getAddress()).thenReturn("12:34:56:78:90:AC");
        devices.add(devMock2);
        // end for performScan->filterDevices
        when(devFuture.get()).thenReturn(devices);
        when(adapterMock.findDevices(2)).thenReturn(devFuture);
        // end for performScan

        Map<String, Object> properties = new HashMap<>();
        properties.put("iname", interfaceName);
        properties.put("scan_enable", true);
        properties.put("enableButtons", true);
        properties.put("period", 20);
        properties.put("scan_time", 2);

        svc.activate(null, properties);

        synchronized (devMock) {
            devMock.wait(2000); // wait < period
        }

        assertNotNull(TestUtil.getFieldValue(svc, "worker"));

        List<TiSensorTag> tiSensorTagList = (List<TiSensorTag>) TestUtil.getFieldValue(svc, "tiSensorTagList");
        assertNotNull(tiSensorTagList);
        assertEquals(1, tiSensorTagList.size());

        verify(devMock, times(1)).connect();
    }

    @Test
    public void testReadSensorTags() throws Throwable {
        BluetoothLe svc = new BluetoothLe();

        Map<String, Object> properties = new HashMap<>();
        properties.put("publishTopic", "testTopic");
        properties.put("discoverServicesAndCharacteristics", true);
        properties.put("enableTermometer", true);
        properties.put("enableAccelerometer", true);
        properties.put("enableHygrometer", true);
        properties.put("enableMagnetometer", true);
        properties.put("enableBarometer", true);
        properties.put("enableGyroscope", true);
        properties.put("enableLuxometer", true);
        properties.put("enableButtons", true);
        properties.put("switchOnRedLed", true);
        properties.put("switchOnGreenLed", true);
        properties.put("switchOnBuzzer", true);

        BluetoothLeOptions options = new BluetoothLeOptions(properties);
        TestUtil.setFieldValue(svc, "options", options);

        CloudPublisher cpMock = mock(CloudPublisher.class);
        svc.setCloudPublisher(cpMock);

        List<TiSensorTag> tiSensorTagList = new ArrayList<>();
        TestUtil.setFieldValue(svc, "tiSensorTagList", tiSensorTagList);

        TiSensorTag tistMock = mock(TiSensorTag.class);
        tiSensorTagList.add(tistMock);
        when(tistMock.isConnected()).thenReturn(true);

        BluetoothLeDevice bldMock = mock(BluetoothLeDevice.class);
        when(bldMock.getAddress()).thenReturn("12:34:56:78:90:AC");
        when(tistMock.getBluetoothLeDevice()).thenReturn(bldMock);

        // for discovery
        Map<String, BluetoothLeGattService> services = new HashMap<>();
        BluetoothLeGattService service = mock(BluetoothLeGattService.class);
        when(service.getUUID()).thenReturn(TiSensorTagGatt.UUID_MAG_SENSOR_SERVICE);
        services.put("pressure", service);
        when(tistMock.discoverServices()).thenReturn(services);

        List<BluetoothLeGattCharacteristic> chars = new ArrayList<>();
        BluetoothLeGattCharacteristic blgc = mock(BluetoothLeGattCharacteristic.class);
        when(blgc.getUUID()).thenReturn(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE);
        chars.add(blgc);
        when(tistMock.getCharacteristics()).thenReturn(chars);
        // end for discovery

        // for sensor reading
        double[] acc = { 1, 1.5, -1 };
        when(tistMock.readAcceleration()).thenReturn(acc);
        float[] gyro = { 4, 5, 6 };
        when(tistMock.readGyroscope()).thenReturn(gyro);
        Float hum = 50f;
        when(tistMock.readHumidity()).thenReturn(hum);
        Double light = 100.1;
        when(tistMock.readLight()).thenReturn(light);
        float[] mag = { 1000, 2000, 3000 };
        when(tistMock.readMagneticField()).thenReturn(mag);
        Double pres = 101.3;
        when(tistMock.readPressure()).thenReturn(pres);
        double[] temp = { 20, 25 };
        when(tistMock.readTemperature()).thenReturn(temp);
        // end for sensor reading

        TestUtil.invokePrivate(svc, "readSensorTags");

        verify(tistMock, times(1)).enableTermometer();
        verify(tistMock, times(1)).enableAccelerometer(new byte[] { 1 });
        verify(tistMock, times(1)).enableLuxometer();
        verify(tistMock, times(1)).enableHygrometer();
        verify(tistMock, times(1)).enableMagnetometer(new byte[] { 1 });
        verify(tistMock, times(1)).enableBarometer();
        verify(tistMock, times(1)).enableGyroscope(new byte[] { 7 });
        verify(tistMock, times(1)).enableLuxometer();
        verify(tistMock, times(1)).enableKeysNotification(anyObject());
        verify(tistMock, times(1)).switchOnRedLed();
        verify(tistMock, times(1)).switchOnGreenLed();
        verify(tistMock, times(1)).switchOnBuzzer();
        verify(tistMock, times(1)).enableIOService();

        ArgumentCaptor<KuraMessage> messageArg = ArgumentCaptor.forClass(KuraMessage.class);
        verify(cpMock).publish(messageArg.capture());

        KuraMessage message = messageArg.getValue();
        KuraPayload payload = message.getPayload();
        assertEquals(temp[0], payload.getMetric("Ambient"));
        assertEquals(temp[1], payload.getMetric("Target"));
        assertEquals(acc[0], payload.getMetric("Acceleration X"));
        assertEquals(acc[1], payload.getMetric("Acceleration Y"));
        assertEquals(acc[2], payload.getMetric("Acceleration Z"));
        assertEquals(hum, payload.getMetric("Humidity"));
        assertEquals(mag[0], payload.getMetric("Magnetic X"));
        assertEquals(mag[1], payload.getMetric("Magnetic Y"));
        assertEquals(mag[2], payload.getMetric("Magnetic Z"));
        assertEquals(pres, payload.getMetric("Pressure"));
        assertEquals(gyro[0], payload.getMetric("Gyro X"));
        assertEquals(gyro[1], payload.getMetric("Gyro Y"));
        assertEquals(gyro[2], payload.getMetric("Gyro Z"));
        assertEquals(light, payload.getMetric("Light"));
    }

}
