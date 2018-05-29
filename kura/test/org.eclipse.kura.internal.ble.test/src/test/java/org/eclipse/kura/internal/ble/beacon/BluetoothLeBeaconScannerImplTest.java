/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.ble.beacon;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconDecoder;
import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.junit.Test;

/**
 * Verifies that the calls to BluetoothLeBeaconScannerImpl get forwarded.
 */
public class BluetoothLeBeaconScannerImplTest {

    @Test
    public void testStartBeaconScan() throws KuraBluetoothCommandException, InterruptedException {
        BluetoothLeAdapter adapterMock = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconManagerImpl beaconManagerMock = mock(BluetoothLeBeaconManagerImpl.class);
        BluetoothLeBeaconScannerImpl<?> svc = new BluetoothLeBeaconScannerImpl<>(adapterMock, null, beaconManagerMock);

        String devName = "devname";
        when(adapterMock.getInterfaceName()).thenReturn(devName);

        Object lock = new Object();
        doAnswer(invocation -> {
            synchronized (lock) {
                lock.notifyAll();
            }
            return null;
        }).when(beaconManagerMock).stopBeaconScan(devName);

        new Thread(() -> {
            try {
                svc.startBeaconScan(1);
            } catch (KuraBluetoothCommandException e) {
            }
        }).start();

        Thread.sleep(100);

        assertTrue("Should be scanning", svc.isScanning());

        synchronized (lock) {
            lock.wait(1000);
        }

        Thread.sleep(100);
        assertFalse("Should not be scanning anymore", svc.isScanning());

        verify(beaconManagerMock, times(1)).startBeaconScan(devName);
        verify(beaconManagerMock, times(1)).stopBeaconScan(devName);
    }

    @Test
    public void testAddRemoveListener() throws KuraBluetoothCommandException {
        BluetoothLeAdapter adapterMock = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconManagerImpl beaconManagerMock = mock(BluetoothLeBeaconManagerImpl.class);
        BluetoothLeBeaconDecoder decoderMock = new BluetoothLeBeaconDecoder<BluetoothLeBeacon>() {

            @Override
            public BluetoothLeBeacon decode(byte[] data) {
                return null;
            }

            @Override
            public Class<BluetoothLeBeacon> getBeaconType() {
                return BluetoothLeBeacon.class;
            }
        };
        BluetoothLeBeaconScannerImpl<?> svc = new BluetoothLeBeaconScannerImpl<>(adapterMock, decoderMock,
                beaconManagerMock);

        BluetoothLeBeaconListener listener = new BluetoothLeBeaconListener() {

            @Override
            public void onBeaconsReceived(BluetoothLeBeacon beacon) {
            }
        };
        svc.addBeaconListener(listener);

        verify(beaconManagerMock, times(1)).addBeaconListener(listener, BluetoothLeBeacon.class);

        svc.removeBeaconListener(listener);

        verify(beaconManagerMock, times(1)).removeBeaconListener(listener);
    }
}