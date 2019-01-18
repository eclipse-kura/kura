/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.ble.ibeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
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
        when(mgrMock.newBeaconAdvertiser(eq(adapter), anyObject())).thenReturn(advMock);

        BluetoothLeBeaconScanner scMock = mock(BluetoothLeBeaconScanner.class);
        when(mgrMock.newBeaconScanner(eq(adapter), anyObject())).thenReturn(scMock);

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
        verify(mgrMock, times(1)).newBeaconAdvertiser(eq(adapter), anyObject());

        svc.deleteBeaconAdvertiser(advertiser);

        verify(mgrMock, times(1)).deleteBeaconAdvertiser(advertiser);

        BluetoothLeBeaconScanner<BluetoothLeIBeacon> scanner = svc.newBeaconScanner(adapter);

        assertEquals(scMock, scanner);
        verify(mgrMock, times(1)).newBeaconScanner(eq(adapter), anyObject());

        svc.deleteBeaconScanner(scanner);

        verify(mgrMock, times(1)).deleteBeaconScanner(scanner);

        svc.unsetBluetoothLeBeaconManager(mgrMock);

        svc.deactivate(null);
    }

}
