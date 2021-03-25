package org.eclipse.kura.internal.ble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraBluetoothNotificationException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristicProperties;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattDescriptor;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;

public class BluetoothLeGattCharacteristicImplTest {

    private static BluetoothLeGattCharacteristicImpl bluetoothLeGattCharacteristic;
    private static BluetoothGattCharacteristic charMock;
    private static byte[] value = { 0x04, 0x05 };

    @BeforeClass
    public static void setup() throws DBusException {
        charMock = mock(BluetoothGattCharacteristic.class);
        when(charMock.getUuid()).thenReturn("6a5a83dc-8ca7-11eb-8dcd-0242ac130003");
        BluetoothDevice deviceMock = mock(BluetoothDevice.class);
        when(deviceMock.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        BluetoothGattService service = new BluetoothGattService(null, deviceMock, null, null);
        when(charMock.getService()).thenReturn(service);
        when(charMock.getValue()).thenReturn(new byte[] { 0x00, 0x01 });
        when(charMock.isNotifying()).thenReturn(false);
        List<String> properties = new ArrayList<>();
        properties.add("broadcast");
        when(charMock.getFlags()).thenReturn(properties);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        when(descriptor.getUuid()).thenReturn("6a5a83dc-8ca7-11eb-8dcd-0242ac130003");
        when(charMock.getGattDescriptorByUuid("6a5a83dc-8ca7-11eb-8dcd-0242ac130003")).thenReturn(descriptor);
        List<BluetoothGattDescriptor> descriptors = new ArrayList<>();
        descriptors.add(descriptor);
        when(charMock.getGattDescriptors()).thenReturn(descriptors);
        when(charMock.readValue(null)).thenReturn(new byte[] { 0x02, 0x03 });
        DeviceManager deviceManagerMock = mock(DeviceManager.class);
        bluetoothLeGattCharacteristic = new BluetoothLeGattCharacteristicImpl(charMock) {

            @Override
            public DeviceManager getDeviceManager() {
                return deviceManagerMock;
            }
        };
    }

    @Test
    public void getterTest() {
        assertEquals(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003"), bluetoothLeGattCharacteristic.getUUID());
        assertEquals("AA:BB:CC:DD:EE:FF", bluetoothLeGattCharacteristic.getService().getDevice().getAddress());
        assertEquals(0x00, bluetoothLeGattCharacteristic.getValue()[0]);
        assertEquals(0x01, bluetoothLeGattCharacteristic.getValue()[1]);
        assertFalse(bluetoothLeGattCharacteristic.isNotifying());
        assertEquals(BluetoothLeGattCharacteristicProperties.BROADCAST,
                bluetoothLeGattCharacteristic.getProperties().get(0));
    }

    @Test
    public void findDescriptorByUuidTest() throws KuraBluetoothResourceNotFoundException {
        assertEquals("6a5a83dc-8ca7-11eb-8dcd-0242ac130003", bluetoothLeGattCharacteristic
                .findDescriptor(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003")).getUUID().toString());
    }

    @Test
    public void findDescriptorByUuidWithTimeoutTest() throws KuraBluetoothResourceNotFoundException {
        assertEquals("6a5a83dc-8ca7-11eb-8dcd-0242ac130003", bluetoothLeGattCharacteristic
                .findDescriptor(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003"), 1).getUUID().toString());
    }

    @Test
    public void findDescriptorsTest() throws KuraBluetoothResourceNotFoundException {
        assertEquals("6a5a83dc-8ca7-11eb-8dcd-0242ac130003",
                bluetoothLeGattCharacteristic.findDescriptors().get(0).getUUID().toString());
    }

    @Test
    public void readValueTest() throws KuraBluetoothIOException {
        assertEquals(0x02, bluetoothLeGattCharacteristic.readValue()[0]);
        assertEquals(0x03, bluetoothLeGattCharacteristic.readValue()[1]);
    }

    @Test
    public void enableValueNotificationsTest() throws DBusException, KuraBluetoothNotificationException {
        bluetoothLeGattCharacteristic.enableValueNotifications(null);
        verify(charMock, times(1)).startNotify();
    }

    @Test
    public void disableValueNotificationsTest() throws DBusException, KuraBluetoothNotificationException {
        bluetoothLeGattCharacteristic.disableValueNotifications();
        verify(charMock, times(1)).stopNotify();
    }

    @Test
    public void writeValueTest() throws DBusException, KuraBluetoothIOException {
        bluetoothLeGattCharacteristic.writeValue(value);
        verify(charMock, times(1)).writeValue(value, null);
    }

}
