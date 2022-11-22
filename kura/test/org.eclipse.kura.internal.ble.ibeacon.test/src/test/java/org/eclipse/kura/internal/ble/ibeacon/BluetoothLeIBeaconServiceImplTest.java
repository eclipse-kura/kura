/*******************************************************************************
 * Copyright (c) 2018, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.internal.ble.ibeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconManager;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.junit.Test;

public class BluetoothLeIBeaconServiceImplTest {

    @Test
    public void testSequence() throws KuraBluetoothBeaconAdvertiserNotAvailable {
        BluetoothLeBeaconManager mgrMock = mock(BluetoothLeBeaconManager.class);

        BluetoothLeAdapter adapter = mock(BluetoothLeAdapter.class);

        BluetoothLeBeaconAdvertiser advMock = mock(BluetoothLeBeaconAdvertiser.class);
        when(mgrMock.newBeaconAdvertiser(eq(adapter), any())).thenReturn(advMock);

        BluetoothLeBeaconScanner scMock = mock(BluetoothLeBeaconScanner.class);
        when(mgrMock.newBeaconScanner(eq(adapter), any())).thenReturn(scMock);

        BluetoothLeIBeaconServiceImpl svc = new BluetoothLeIBeaconServiceImpl();

        svc.activate(null);

        try {
            svc.newBeaconAdvertiser(adapter);
            fail("Exception was expected.");
        } catch (NullPointerException e) {
            // expected
        }

        svc.setBluetoothLeBeaconManager(mgrMock);

        BluetoothLeBeaconAdvertiser<BluetoothLeIBeacon> advertiser = svc.newBeaconAdvertiser(adapter);

        assertEquals(advMock, advertiser);
        verify(mgrMock, times(1)).newBeaconAdvertiser(eq(adapter), any());

        svc.deleteBeaconAdvertiser(advertiser);

        verify(mgrMock, times(1)).deleteBeaconAdvertiser(advertiser);

        BluetoothLeBeaconScanner<BluetoothLeIBeacon> scanner = svc.newBeaconScanner(adapter);

        assertEquals(scMock, scanner);
        verify(mgrMock, times(1)).newBeaconScanner(eq(adapter), any());

        svc.deleteBeaconScanner(scanner);

        verify(mgrMock, times(1)).deleteBeaconScanner(scanner);

        svc.unsetBluetoothLeBeaconManager(mgrMock);

        svc.deactivate(null);
    }

}
