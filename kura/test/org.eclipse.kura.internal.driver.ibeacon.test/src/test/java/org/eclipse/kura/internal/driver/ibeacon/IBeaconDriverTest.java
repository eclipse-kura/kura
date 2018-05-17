/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.driver.ibeacon;

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
import java.util.UUID;

import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeaconService;
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

public class IBeaconDriverTest {

    @Test
    public void testActivateDeactivate() {

        String interfaceName = "hci0";

        IBeaconDriver svc = new IBeaconDriver();

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

        // Try without IBeaconService
        try {
            svc.activate(properties);
            fail("Exception was expected");
        } catch (NullPointerException e) {
        }

        @SuppressWarnings("unchecked")
        BluetoothLeBeaconScanner<BluetoothLeIBeacon> ibeaconScannerMock = mock(BluetoothLeBeaconScanner.class);
        when(ibeaconScannerMock.isScanning()).thenReturn(true).thenReturn(false).thenReturn(true);

        BluetoothLeIBeaconService iBeaconServiceMock = mock(BluetoothLeIBeaconService.class);
        when(iBeaconServiceMock.newBeaconScanner(adapterMock)).thenReturn(ibeaconScannerMock);
        svc.bindBluetoothLeIBeaconService(iBeaconServiceMock);

        // Test activate
        svc.activate(properties);

        // Test update
        svc.updated(properties);

        // Test deactivate
        svc.deactivate();

        verify(adapterMock, times(3)).isPowered();
        verify(adapterMock, times(1)).setPowered(true);

        // Unbind BLEService and iBeaconService try activation again
        svc.unbindBluetoothLeService(bleMock);
        svc.unbindBluetoothLeIBeaconService(iBeaconServiceMock);

        try {
            svc.activate(properties);
            fail("Exception was expected");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testReadAndPreparedRead() throws ConnectionException {
        IBeaconDriver svc = new IBeaconDriver();

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("ibeacon", DataType.INTEGER);
        Map<String, Object> config = new HashMap<>();

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
        IBeaconDriver svc = new IBeaconDriver();

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("ibeacon", DataType.INTEGER);
        Map<String, Object> config = new HashMap<>();

        record.setChannelConfig(config);
        records.add(record);
        try {
            svc.write(records);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("OPERATION_NOT_SUPPORTED"));
        }
    }

    @Test
    public void testChannelListeners() throws ConnectionException, NoSuchFieldException {
        IBeaconDriver svc = new IBeaconDriver();

        Set<IBeaconListener> ibeaconListeners = new HashSet<>();
        TestUtil.setFieldValue(svc, "iBeaconListeners", ibeaconListeners);

        List<ChannelRecord> records = new ArrayList<>();
        ChannelRecord record = ChannelRecord.createReadRecord("ibeacon", DataType.INTEGER);
        Map<String, Object> config = new HashMap<>();
        config.put("+name", "ibeacon");
        config.put("+value.type", "integer");
        record.setChannelConfig(config);
        records.add(record);

        ChannelListener listener = event -> {
            assertTrue("00000000-1111-2222-3333-444444444444;-10;0;1;1;0.31622776601683794"
                    .equals(((StringValue) event.getChannelRecord().getValue()).getValue()));
        };

        svc.registerChannelListener(config, listener);
        assertEquals(1, ibeaconListeners.size());

        BluetoothLeIBeacon iBeacon = new BluetoothLeIBeacon(UUID.fromString("00000000-1111-2222-3333-444444444444"),
                (short) 1, (short) 1, (short) -10);
        svc.onBeaconsReceived(iBeacon);

        svc.unregisterChannelListener(listener);
        assertEquals(0, ibeaconListeners.size());
    }

    @Test
    public void testChannelDescriptor() {
        IBeaconDriver svc = new IBeaconDriver();
        @SuppressWarnings("unchecked")
        List<Tad> elements = (List<Tad>) svc.getChannelDescriptor().getDescriptor();

        assertEquals(0, elements.size());
    }

    @Test
    public void testIBeaconListenerEquality() {

        ChannelListener channelListener1 = event -> {
        };
        IBeaconListener iBeaconListener1 = new IBeaconListener("ibeacon1", channelListener1);

        assertTrue(iBeaconListener1.equals(iBeaconListener1));
        assertFalse(iBeaconListener1.equals(null));
        assertFalse(iBeaconListener1.equals(channelListener1));

        ChannelListener channelListener2 = event -> {
        };
        IBeaconListener iBeaconListener2 = new IBeaconListener(null, channelListener2);
        assertFalse(iBeaconListener2.equals(iBeaconListener1));

        iBeaconListener2 = new IBeaconListener("ibeacon2", channelListener2);
        assertFalse(iBeaconListener2.equals(iBeaconListener1));

        iBeaconListener2 = new IBeaconListener("ibeacon1", null);
        assertFalse(iBeaconListener2.equals(iBeaconListener1));

        iBeaconListener2 = new IBeaconListener("ibeacon1", channelListener2);
        assertFalse(iBeaconListener2.equals(iBeaconListener1));

        iBeaconListener2 = new IBeaconListener("ibeacon1", channelListener1);
        assertTrue(iBeaconListener2.equals(iBeaconListener1));
    }
}
