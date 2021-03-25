package org.eclipse.kura.internal.ble;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.kura.KuraBluetoothIOException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattDescriptor;

public class BluetoothLeGattDescriptorImplTest {

    private static BluetoothLeGattDescriptorImpl bluetoothLeGattDescriptor;
    private static BluetoothGattDescriptor descMock;

    @BeforeClass
    public static void setup() throws DBusException {
        descMock = mock(BluetoothGattDescriptor.class);
        when(descMock.readValue(null)).thenReturn(new byte[] { 0x00, 0x01 });
        when(descMock.getUuid()).thenReturn("6a5a83dc-8ca7-11eb-8dcd-0242ac130003");
        BluetoothGattCharacteristic charMock = mock(BluetoothGattCharacteristic.class);
        when(charMock.getUuid()).thenReturn("6a5a83dc-aaaa-11eb-8dcd-0242ac130003");
        when(descMock.getCharacteristic()).thenReturn(charMock);
        when(descMock.getValue()).thenReturn(new byte[] { 0x02, 0x03 });
        bluetoothLeGattDescriptor = new BluetoothLeGattDescriptorImpl(descMock);
    }

    @Test
    public void readValueTest() throws KuraBluetoothIOException {
        assertEquals(0x00, bluetoothLeGattDescriptor.readValue()[0]);
        assertEquals(0x01, bluetoothLeGattDescriptor.readValue()[1]);
    }

    @Test
    public void writeValueTest() throws DBusException, KuraBluetoothIOException {
        bluetoothLeGattDescriptor.writeValue(null);
        verify(descMock, times(1)).writeValue(null, null);
    }

    @Test
    public void getUUIDTest() {
        assertEquals("6a5a83dc-8ca7-11eb-8dcd-0242ac130003", bluetoothLeGattDescriptor.getUUID().toString());
    }

    @Test
    public void getCharacteristicTest() {
        assertEquals("6a5a83dc-aaaa-11eb-8dcd-0242ac130003",
                bluetoothLeGattDescriptor.getCharacteristic().getUUID().toString());
    }

    @Test
    public void getValueTest() {
        assertEquals(0x02, bluetoothLeGattDescriptor.getValue()[0]);
        assertEquals(0x03, bluetoothLeGattDescriptor.getValue()[1]);
    }

}
