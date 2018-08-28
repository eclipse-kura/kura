/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.example.ble.tisensortag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothService;
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
        BluetoothAdapter bluetoothAdapter = mock(BluetoothAdapter.class);
        when(bluetoothAdapter.isScanning()).thenReturn(true);
        TestUtil.setFieldValue(svc, "bluetoothAdapter", bluetoothAdapter);

        List<TiSensorTag> tiSensorTagList = new ArrayList<>();
        TiSensorTag tistMock = mock(TiSensorTag.class);
        when(tistMock.isConnected()).thenReturn(true);
        tiSensorTagList.add(tistMock);
        TestUtil.setFieldValue(svc, "tiSensorTagList", tiSensorTagList);

        svc.deactivate(null);

        verify(bluetoothAdapter, times(1)).killLeScan();
        verify(tistMock, times(1)).disableKeysNotifications();
        verify(tistMock, times(1)).disconnect();
        assertNull(TestUtil.getFieldValue(svc, "bluetoothAdapter"));

        svc.unsetCloudPublisher(cpMock);

        assertNull(TestUtil.getFieldValue(svc, "cloudPublisher"));
    }

    @Test
    public void testUpdate() throws NoSuchFieldException, KuraException, InterruptedException, ExecutionException {
        String interfaceName = "hci0";

        BluetoothLe svc = new BluetoothLe();

        CloudPublisher cpMock = mock(CloudPublisher.class);
        svc.setCloudPublisher(cpMock);

        BluetoothService bleMock = mock(BluetoothService.class);
        BluetoothAdapter adapterMock = mock(BluetoothAdapter.class);
        when(adapterMock.getAddress()).thenReturn("12:34:56:78:90:AB");
        doAnswer(inv -> {
            synchronized (adapterMock) {
                adapterMock.notifyAll(); // stop waiting after connect is called in readSensorTags
            }

            return null;
        }).when(adapterMock).startLeScan(svc);
        when(bleMock.getBluetoothAdapter(interfaceName)).thenReturn(adapterMock);
        svc.setBluetoothService(bleMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put("iname", interfaceName);
        properties.put("scan_enable", true);
        properties.put("enableButtons", true);
        properties.put("period", 20);
        properties.put("scan_time", 2);

        svc.activate(null, properties);

        synchronized (adapterMock) {
            adapterMock.wait(900); // wait < 1 s
        }

        assertNotNull(TestUtil.getFieldValue(svc, "worker"));

        List<TiSensorTag> tiSensorTagList = (List<TiSensorTag>) TestUtil.getFieldValue(svc, "tiSensorTagList");
        assertNotNull(tiSensorTagList);
        assertEquals(0, tiSensorTagList.size());

        verify(adapterMock, times(1)).enable();
    }

    @Test
    public void testOnScanResults() throws Throwable {
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

        BluetoothDevice bldMock = mock(BluetoothDevice.class);
        when(bldMock.getAdress()).thenReturn("12:34:56:78:90:AC");
        when(bldMock.getName()).thenReturn("SensorTag CC2541");
        when(tistMock.getBluetoothDevice()).thenReturn(bldMock);

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

        List<BluetoothDevice> scanResults = new ArrayList<>();
        scanResults.add(bldMock);
        svc.onScanResults(scanResults);

        verify(tistMock, times(1)).enableTermometer();
        verify(tistMock, times(1)).enableAccelerometer("01");
        verify(tistMock, times(1)).enableLuxometer();
        verify(tistMock, times(1)).enableHygrometer();
        verify(tistMock, times(1)).enableMagnetometer("");
        verify(tistMock, times(1)).enableBarometer();
        verify(tistMock, times(1)).enableGyroscope("07");
        verify(tistMock, times(1)).enableLuxometer();
        verify(tistMock, times(1)).enableKeysNotifications(svc);
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
