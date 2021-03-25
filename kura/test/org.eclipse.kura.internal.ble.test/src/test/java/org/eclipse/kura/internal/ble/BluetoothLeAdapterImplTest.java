/*******************************************************************************
 * Copyright (c) 2018, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.ble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.eclipse.kura.KuraBluetoothDiscoveryException;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothAdapter;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;

public class BluetoothLeAdapterImplTest {

    private static BluetoothLeAdapterImpl bluetoothLeAdapter;
    private static BluetoothAdapter adapterMock;
    private static DeviceManager deviceManagerMock;

    @BeforeClass
    public static void setup() throws DBusException {
        adapterMock = mock(BluetoothAdapter.class);
        when(adapterMock.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        when(adapterMock.getName()).thenReturn("AdapterName");
        when(adapterMock.getDeviceName()).thenReturn("DeviceName");
        when(adapterMock.getModAlias()).thenReturn("ModAlias");
        when(adapterMock.getAlias()).thenReturn("Alias");
        when(adapterMock.getDeviceClass()).thenReturn(1000);
        when(adapterMock.isPowered()).thenReturn(true);
        when(adapterMock.isDiscoverable()).thenReturn(null);
        when(adapterMock.getDiscoverableTimeout()).thenReturn(9000);
        when(adapterMock.isPairable()).thenReturn(false);
        when(adapterMock.getPairableTimeout()).thenReturn(3000);
        when(adapterMock.isDiscovering()).thenReturn(true);
        when(adapterMock.getUuids()).thenReturn(
                new String[] { "6a5a83dc-8ca7-11eb-8dcd-0242ac130003", "72579a7a-8ca7-11eb-8dcd-0242ac130003" });

        when(adapterMock.startDiscovery()).thenReturn(false);
        when(adapterMock.stopDiscovery()).thenReturn(false);

        BluetoothDevice device = mock(BluetoothDevice.class);
        when(device.getAddress()).thenReturn("11:22:33:44:55:66");
        when(device.getName()).thenReturn("DeviceName");
        List<BluetoothDevice> devices = new ArrayList<>();
        devices.add(device);
        deviceManagerMock = mock(DeviceManager.class);
        when(deviceManagerMock.getDevices("AA:BB:CC:DD:EE:FF", true)).thenReturn(devices);

        bluetoothLeAdapter = new BluetoothLeAdapterImpl(adapterMock) {

            @Override
            public DeviceManager getDeviceManager() {
                return deviceManagerMock;
            }
        };
    }

    @Test
    public void getterTest() {
        assertEquals("AA:BB:CC:DD:EE:FF", bluetoothLeAdapter.getAddress());
        assertEquals("AdapterName", bluetoothLeAdapter.getName());
        assertEquals("DeviceName", bluetoothLeAdapter.getInterfaceName());
        assertEquals("ModAlias", bluetoothLeAdapter.getModalias());
        assertEquals("Alias", bluetoothLeAdapter.getAlias());
        assertEquals(1000, bluetoothLeAdapter.getBluetoothClass());
        assertTrue(bluetoothLeAdapter.isPowered());
        assertFalse(bluetoothLeAdapter.isDiscoverable());
        assertEquals(9000, bluetoothLeAdapter.getDiscoverableTimeout());
        assertFalse(bluetoothLeAdapter.isPairable());
        assertEquals(3000, bluetoothLeAdapter.getPairableTimeout());
        assertTrue(bluetoothLeAdapter.isDiscovering());
        assertEquals(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003"), bluetoothLeAdapter.getUUIDs()[0]);
        assertEquals(UUID.fromString("72579a7a-8ca7-11eb-8dcd-0242ac130003"), bluetoothLeAdapter.getUUIDs()[1]);
    }

    @Test(expected = KuraBluetoothDiscoveryException.class)
    public void startDiscoveryTest() throws KuraBluetoothDiscoveryException {
        when(adapterMock.startDiscovery()).thenReturn(false);
        bluetoothLeAdapter.startDiscovery();
    }

    @Test(expected = KuraBluetoothDiscoveryException.class)
    public void stopDiscoveryTest() throws KuraBluetoothDiscoveryException {
        when(adapterMock.stopDiscovery()).thenReturn(false);
        bluetoothLeAdapter.stopDiscovery();
    }

    @Test(expected = ExecutionException.class)
    public void findDeviceByAddressFailedTest()
            throws KuraBluetoothDiscoveryException, InterruptedException, ExecutionException {
        when(adapterMock.isDiscovering()).thenReturn(true);
        bluetoothLeAdapter.findDeviceByAddress(1, "").get();
    }

    @Test
    public void findDeviceByAddressTest()
            throws KuraBluetoothDiscoveryException, InterruptedException, ExecutionException {
        when(adapterMock.isDiscovering()).thenReturn(false);
        when(adapterMock.startDiscovery()).thenReturn(true);
        when(adapterMock.stopDiscovery()).thenReturn(true);
        BluetoothLeDevice device = bluetoothLeAdapter.findDeviceByAddress(1, "11:22:33:44:55:66").get();

        assertEquals("11:22:33:44:55:66", device.getAddress());
    }

    @Test
    public void findDeviceByNameTest()
            throws KuraBluetoothDiscoveryException, InterruptedException, ExecutionException {
        when(adapterMock.isDiscovering()).thenReturn(false);
        when(adapterMock.startDiscovery()).thenReturn(true);
        when(adapterMock.stopDiscovery()).thenReturn(true);
        BluetoothLeDevice device = bluetoothLeAdapter.findDeviceByName(1, "DeviceName").get();

        assertEquals("DeviceName", device.getName());
    }

    @Test
    public void findDeviceByAddressWithCallbackTest()
            throws KuraBluetoothDiscoveryException, InterruptedException, ExecutionException {
        when(adapterMock.isDiscovering()).thenReturn(false);
        when(adapterMock.startDiscovery()).thenReturn(true);
        when(adapterMock.stopDiscovery()).thenReturn(true);

        bluetoothLeAdapter.findDeviceByAddress(1, "11:22:33:44:55:66",
                d -> assertEquals("11:22:33:44:55:66", d.getAddress()));
    }

    @Test
    public void findDeviceByNameWithCallbackTest()
            throws KuraBluetoothDiscoveryException, InterruptedException, ExecutionException {
        when(adapterMock.isDiscovering()).thenReturn(false);
        when(adapterMock.startDiscovery()).thenReturn(true);
        when(adapterMock.stopDiscovery()).thenReturn(true);

        bluetoothLeAdapter.findDeviceByName(1, "DeviceName", d -> assertEquals("DeviceName", d.getName()));
    }

    @Test
    public void setRssiDiscoveryFilterTest() throws BluezInvalidArgumentsException, BluezNotReadyException,
            BluezNotSupportedException, BluezFailedException {
        bluetoothLeAdapter.setRssiDiscoveryFilter(5);

        Map<String, Variant<?>> filter = new LinkedHashMap<>();
        filter.put("RSSI", new Variant<>((short) 5));
        filter.put("Transport", new Variant<>("auto"));
        filter.put("DuplicateData", new Variant<>(false));
        verify(adapterMock).setDiscoveryFilter(filter);
    }
}
