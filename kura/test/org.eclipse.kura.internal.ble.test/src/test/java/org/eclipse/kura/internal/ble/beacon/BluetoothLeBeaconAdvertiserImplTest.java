/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.ble.beacon;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconEncoder;
import org.junit.Test;

/**
 * Verifies that the calls to BluetoothLeBeaconAdvertiserImpl get forwarded.
 */
public class BluetoothLeBeaconAdvertiserImplTest {

    @Test
    public void testStartBeaconAdvertising() throws KuraBluetoothCommandException {
        BluetoothLeAdapter adapterMock = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconManagerImpl beaconManagerMock = mock(BluetoothLeBeaconManagerImpl.class);
        BluetoothLeBeaconAdvertiserImpl<?> svc = new BluetoothLeBeaconAdvertiserImpl<>(adapterMock, null,
                beaconManagerMock);

        String devName = "devname";
        when(adapterMock.getInterfaceName()).thenReturn(devName);

        svc.startBeaconAdvertising();

        verify(beaconManagerMock, times(1)).startBeaconAdvertising(devName);
    }

    @Test
    public void testStopBeaconAdvertising() throws KuraBluetoothCommandException {
        BluetoothLeAdapter adapterMock = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconManagerImpl beaconManagerMock = mock(BluetoothLeBeaconManagerImpl.class);
        BluetoothLeBeaconAdvertiserImpl<?> svc = new BluetoothLeBeaconAdvertiserImpl<>(adapterMock, null,
                beaconManagerMock);

        String devName = "devname";
        when(adapterMock.getInterfaceName()).thenReturn(devName);

        svc.stopBeaconAdvertising();

        verify(beaconManagerMock, times(1)).stopBeaconAdvertising(devName);
    }

    @Test
    public void testUpdateBeaconAdvertisingInterval() throws KuraBluetoothCommandException {
        BluetoothLeAdapter adapterMock = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconManagerImpl beaconManagerMock = mock(BluetoothLeBeaconManagerImpl.class);
        BluetoothLeBeaconAdvertiserImpl<?> svc = new BluetoothLeBeaconAdvertiserImpl<>(adapterMock, null,
                beaconManagerMock);

        String devName = "devname";
        when(adapterMock.getInterfaceName()).thenReturn(devName);

        Integer min = 0;
        Integer max = 10;
        svc.updateBeaconAdvertisingInterval(min, max);

        verify(beaconManagerMock, times(1)).updateBeaconAdvertisingInterval(min, max, devName);
    }

    @Test
    public void testUpdateBeaconAdvertisingData() throws KuraBluetoothCommandException {
        BluetoothLeAdapter adapterMock = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconEncoder encoder = null;
        BluetoothLeBeaconManagerImpl beaconManagerMock = mock(BluetoothLeBeaconManagerImpl.class);
        BluetoothLeBeaconAdvertiserImpl<TestBeacon> svc = new BluetoothLeBeaconAdvertiserImpl<TestBeacon>(adapterMock,
                encoder, beaconManagerMock);

        String devName = "devname";
        when(adapterMock.getInterfaceName()).thenReturn(devName);

        TestBeacon beacon = new TestBeacon();
        svc.updateBeaconAdvertisingData(beacon);

        verify(beaconManagerMock, times(1)).updateBeaconAdvertisingData(beacon, encoder, devName);
    }
}

class TestBeacon extends BluetoothLeBeacon {
}
