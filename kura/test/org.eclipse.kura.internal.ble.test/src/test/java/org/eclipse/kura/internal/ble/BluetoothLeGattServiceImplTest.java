package org.eclipse.kura.internal.ble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;

public class BluetoothLeGattServiceImplTest {

    private static BluetoothLeGattServiceImpl bluetoothLeGattService;
    private static BluetoothGattService serviceMock;

    @BeforeClass
    public static void setup() throws DBusException {
        serviceMock = mock(BluetoothGattService.class);
        when(serviceMock.getUuid()).thenReturn("6a5a83dc-8ca7-11eb-8dcd-0242ac130003");
        BluetoothDevice deviceMock = mock(BluetoothDevice.class);
        when(deviceMock.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        when(serviceMock.getDevice()).thenReturn(deviceMock);
        when(serviceMock.isPrimary()).thenReturn(false);
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getUuid()).thenReturn("6a5a83dc-bbbb-11eb-8dcd-0242ac130003");
        when(serviceMock.getGattCharacteristicByUuid("6a5a83dc-bbbb-11eb-8dcd-0242ac130003"))
                .thenReturn(characteristic);
        List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
        characteristics.add(characteristic);
        when(serviceMock.getGattCharacteristics()).thenReturn(characteristics);
        bluetoothLeGattService = new BluetoothLeGattServiceImpl(serviceMock);
    }

    @Test
    public void getterTest() {
        assertEquals(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003"), bluetoothLeGattService.getUUID());
        assertFalse(bluetoothLeGattService.isPrimary());
    }

    @Test
    public void findCharacteristicByUuidTest() throws KuraBluetoothResourceNotFoundException {
        assertEquals("6a5a83dc-bbbb-11eb-8dcd-0242ac130003", bluetoothLeGattService
                .findCharacteristic(UUID.fromString("6a5a83dc-bbbb-11eb-8dcd-0242ac130003")).getUUID().toString());
    }

    @Test
    public void findDescriptorByUuidWithTimeoutTest() throws KuraBluetoothResourceNotFoundException {
        assertEquals("6a5a83dc-bbbb-11eb-8dcd-0242ac130003", bluetoothLeGattService
                .findCharacteristic(UUID.fromString("6a5a83dc-bbbb-11eb-8dcd-0242ac130003"), 1).getUUID().toString());
    }

    @Test
    public void findCharacteristicsTest() throws KuraBluetoothResourceNotFoundException {
        assertEquals("6a5a83dc-bbbb-11eb-8dcd-0242ac130003",
                bluetoothLeGattService.findCharacteristics().get(0).getUUID().toString());
    }

    @Test
    public void getDeviceTest() {
        assertEquals("AA:BB:CC:DD:EE:FF", bluetoothLeGattService.getDevice().getAddress());
    }

}
