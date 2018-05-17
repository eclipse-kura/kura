/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.driver.eddystone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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

import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystoneService;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.StringValue;
import org.junit.Test;

public class EddystoneDriverTest {

    @Test
    public void testActivateDeactivate() {

        String interfaceName = "hci0";

        EddystoneDriver svc = new EddystoneDriver();

        // Try without BLEService
        Map<String, Object> properties = new HashMap<>();
        properties.put("iname", interfaceName);
        try {
            svc.activate(properties);
            fail("Exception was expected");
        } catch (NullPointerException e) {
        }

        BluetoothLeAdapter adapterMock = mock(BluetoothLeAdapter.class);
        when(adapterMock.isPowered()).thenReturn(false).thenReturn(true);

        BluetoothLeService bleMock = mock(BluetoothLeService.class);
        svc.bindBluetoothLeService(bleMock);

        // Try without Adapter
        svc.activate(properties);

        when(bleMock.getAdapter(interfaceName)).thenReturn(adapterMock);

        // Try without EddystoneService
        try {
            svc.activate(properties);
            fail("Exception was expected");
        } catch (NullPointerException e) {
        }

        @SuppressWarnings("unchecked")
        BluetoothLeBeaconScanner<BluetoothLeEddystone> eddystoneScannerMock = mock(BluetoothLeBeaconScanner.class);
        when(eddystoneScannerMock.isScanning()).thenReturn(true).thenReturn(false).thenReturn(true);

        BluetoothLeEddystoneService eddystoneServiceMock = mock(BluetoothLeEddystoneService.class);
        when(eddystoneServiceMock.newBeaconScanner(adapterMock)).thenReturn(eddystoneScannerMock);
        svc.bindBluetoothLeEddystoneService(eddystoneServiceMock);

        // Test activate
        svc.activate(properties);

        // Test update
        svc.updated(properties);

        // Test deactivate
        svc.deactivate();

        verify(adapterMock, times(3)).isPowered();
        verify(adapterMock, times(1)).setPowered(true);

        // Unbind BLEService and EddystoneService try activation again
        svc.unbindBluetoothLeService(bleMock);
        svc.unbindBluetoothLeEddystoneService(eddystoneServiceMock);

        try {
            svc.activate(properties);
            fail("Exception was expected");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testReadAndPreparedRead() throws ConnectionException {
        EddystoneDriver svc = new EddystoneDriver();

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("eddystone", DataType.INTEGER);
        Map<String, Object> config = new HashMap<>();
        config.put("eddystone.type", EddystoneFrameType.URL);

        record.setChannelConfig(config);
        records.add(record);
        svc.read(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
        assertTrue(record.getChannelStatus().getExceptionMessage().contains("Read operation not supported"));

        PreparedRead pr = svc.prepareRead(records);

        assertTrue(pr == null);
    }

    @Test
    public void testWrite() {
        EddystoneDriver svc = new EddystoneDriver();

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("eddystone", DataType.INTEGER);
        Map<String, Object> config = new HashMap<>();
        config.put("eddystone.type", EddystoneFrameType.URL.toString());

        record.setChannelConfig(config);
        records.add(record);
        try {
            svc.write(records);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("OPERATION_NOT_SUPPORTED"));
        }
    }

    @Test
    public void testChannelListeners() throws NoSuchFieldException, ConnectionException {
        EddystoneDriver svc = new EddystoneDriver();

        Set<EddystoneListener> eddystoneListeners = new HashSet<>();
        TestUtil.setFieldValue(svc, "eddystoneListeners", eddystoneListeners);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord urlRecord = ChannelRecord.createReadRecord("eddystoneURL", DataType.INTEGER);
        Map<String, Object> urlConfig = new HashMap<>();
        urlConfig.put("eddystone.type", EddystoneFrameType.URL.toString());
        urlConfig.put("+name", "eddystoneURL");
        urlConfig.put("+value.type", "integer");
        urlRecord.setChannelConfig(urlConfig);
        records.add(urlRecord);

        ChannelListener urlListener = event -> {
            assertTrue("URL;www.eclipse.org/kura;-10;0;0.31622776601683794"
                    .equals(((StringValue) event.getChannelRecord().getValue()).getValue()));
        };

        ChannelRecord uidRecord = ChannelRecord.createReadRecord("eddystoneUID", DataType.INTEGER);
        Map<String, Object> uidConfig = new HashMap<>();
        uidConfig.put("eddystone.type", EddystoneFrameType.UID.toString());
        uidConfig.put("+name", "eddystoneUID");
        uidConfig.put("+value.type", "integer");
        uidRecord.setChannelConfig(uidConfig);
        records.add(uidRecord);

        ChannelListener uidListener = event -> {
            assertTrue("UID;0001020304050607090A;000102030405;-10;0;0.31622776601683794"
                    .equals(((StringValue) event.getChannelRecord().getValue()).getValue()));
        };

        svc.registerChannelListener(urlConfig, urlListener);
        svc.registerChannelListener(uidConfig, uidListener);
        assertEquals(2, eddystoneListeners.size());

        BluetoothLeEddystone eddystoneURL = new BluetoothLeEddystone();
        eddystoneURL.configureEddystoneURLFrame("www.eclipse.org/kura", (short) -10);
        svc.onBeaconsReceived(eddystoneURL);

        BluetoothLeEddystone eddystoneUID = new BluetoothLeEddystone();
        byte[] namespace = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x09, 0x0A };
        byte[] instance = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };
        eddystoneUID.configureEddystoneUIDFrame(namespace, instance, (short) -10);
        svc.onBeaconsReceived(eddystoneUID);

        svc.unregisterChannelListener(urlListener);
        svc.unregisterChannelListener(uidListener);
        assertEquals(0, eddystoneListeners.size());
    }

    @Test
    public void testChannelDescriptor() {
        EddystoneDriver svc = new EddystoneDriver();
        @SuppressWarnings("unchecked")
        List<Tad> elements = (List<Tad>) svc.getChannelDescriptor().getDescriptor();

        assertEquals(1, elements.size());
    }

    @Test
    public void testEddystoneListenerEquality() {

        ChannelListener urlListener = event -> {
        };
        ChannelListener uidListener = event -> {
        };

        EddystoneListener listenerURL = new EddystoneListener(null, urlListener, EddystoneFrameType.URL);

        assertTrue(listenerURL.equals(listenerURL));
        assertFalse(listenerURL.equals(null));
        assertFalse(listenerURL.equals(urlListener));

        EddystoneListener listenerUID = new EddystoneListener("eddystoneUID", uidListener, EddystoneFrameType.UID);
        assertFalse(listenerURL.equals(listenerUID));

        listenerURL = new EddystoneListener("eddystoneURL", urlListener, EddystoneFrameType.URL);
        assertFalse(listenerURL.equals(listenerUID));

        listenerUID = new EddystoneListener("eddystoneURL", uidListener, EddystoneFrameType.UID);
        assertFalse(listenerURL.equals(listenerUID));

        listenerUID = new EddystoneListener("eddystoneURL", uidListener, EddystoneFrameType.URL);
        listenerURL = new EddystoneListener("eddystoneURL", null, EddystoneFrameType.URL);
        assertFalse(listenerURL.equals(listenerUID));

        listenerURL = new EddystoneListener("eddystoneURL", urlListener, EddystoneFrameType.URL);
        assertFalse(listenerURL.equals(listenerUID));

        listenerUID = new EddystoneListener("eddystoneURL", urlListener, EddystoneFrameType.URL);
        assertTrue(listenerUID.equals(listenerURL));
    }
}
