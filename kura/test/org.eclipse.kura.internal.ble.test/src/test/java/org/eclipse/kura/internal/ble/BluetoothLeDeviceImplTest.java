package org.eclipse.kura.internal.ble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothPairException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt16;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;

public class BluetoothLeDeviceImplTest {

    private static BluetoothLeDeviceImpl bluetoothLeDevice;
    private static BluetoothDevice deviceMock;

    @BeforeClass
    public static void setup() throws DBusException {
        deviceMock = mock(BluetoothDevice.class);
        when(deviceMock.getAddress()).thenReturn("AA:BB:CC:DD:EE:FF");
        when(deviceMock.getName()).thenReturn("DeviceName");
        when(deviceMock.getAlias()).thenReturn("Alias");
        when(deviceMock.getModAlias()).thenReturn("ModAlias");
        when(deviceMock.getBluetoothClass()).thenReturn(5);
        when(deviceMock.getAppearance()).thenReturn(50);
        when(deviceMock.getIcon()).thenReturn("Icon");
        when(deviceMock.isPaired()).thenReturn(true);
        when(deviceMock.isTrusted()).thenReturn(false);
        when(deviceMock.isBlocked()).thenReturn(true);
        when(deviceMock.isLegacyPairing()).thenReturn(false);
        when(deviceMock.isConnected()).thenReturn(true);
        when(deviceMock.isServicesResolved()).thenReturn(true);
        when(deviceMock.getRssi()).thenReturn((short) 500);
        when(deviceMock.getUuids()).thenReturn(
                new String[] { "6a5a83dc-8ca7-11eb-8dcd-0242ac130003", "72579a7a-8ca7-11eb-8dcd-0242ac130003" });
        when(deviceMock.getTxPower()).thenReturn((short) 5000);
        Map<UInt16, byte[]> manufacturerData = new HashMap<>();
        manufacturerData.put(new UInt16(45), new byte[] { 0x00, 0x01 });
        when(deviceMock.getManufacturerData()).thenReturn(manufacturerData);
        Map<String, byte[]> serviceData = new HashMap<>();
        serviceData.put("6a5a83dc-8ca7-11eb-8dcd-0242ac130003", new byte[] { 0x02, 0x03 });
        when(deviceMock.getServiceData()).thenReturn(serviceData);

        BluetoothGattService service = new BluetoothGattService(null, deviceMock, null, null);
        when(deviceMock.getGattServiceByUuid("72579a7a-8ca7-11eb-8dcd-0242ac130003")).thenReturn(service);
        List<BluetoothGattService> serviceList = new ArrayList<>();
        serviceList.add(service);
        when(deviceMock.getGattServices()).thenReturn(serviceList);

        when(deviceMock.connect()).thenReturn(false);
        when(deviceMock.disconnect()).thenReturn(false);
        when(deviceMock.connectProfile("6a5a83dc-8ca7-11eb-8dcd-0242ac130003")).thenReturn(false);
        when(deviceMock.disconnectProfile("6a5a83dc-8ca7-11eb-8dcd-0242ac130003")).thenReturn(false);
        when(deviceMock.pair()).thenReturn(false);
        when(deviceMock.cancelPairing()).thenReturn(false);
        bluetoothLeDevice = new BluetoothLeDeviceImpl(deviceMock);
    }

    @Test
    public void getterTest() {
        assertEquals("AA:BB:CC:DD:EE:FF", bluetoothLeDevice.getAddress());
        assertEquals("DeviceName", bluetoothLeDevice.getName());
        assertEquals("Alias", bluetoothLeDevice.getAlias());
        assertEquals("ModAlias", bluetoothLeDevice.getModalias());
        assertEquals(5, bluetoothLeDevice.getBluetoothClass());
        assertEquals(50, bluetoothLeDevice.getAppearance());
        assertEquals("Icon", bluetoothLeDevice.getIcon());
        assertTrue(bluetoothLeDevice.isPaired());
        assertFalse(bluetoothLeDevice.isTrusted());
        assertTrue(bluetoothLeDevice.isBlocked());
        assertFalse(bluetoothLeDevice.isLegacyPairing());
        assertTrue(bluetoothLeDevice.isConnected());
        assertTrue(bluetoothLeDevice.isServicesResolved());
        assertEquals(500, bluetoothLeDevice.getRSSI());
        assertEquals(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003"), bluetoothLeDevice.getUUIDs()[0]);
        assertEquals(UUID.fromString("72579a7a-8ca7-11eb-8dcd-0242ac130003"), bluetoothLeDevice.getUUIDs()[1]);
        assertEquals(5000, bluetoothLeDevice.getTxPower());
        assertEquals(0x00, bluetoothLeDevice.getManufacturerData().get((short) 45)[0]);
        assertEquals(0x01, bluetoothLeDevice.getManufacturerData().get((short) 45)[1]);
        assertEquals(0x02,
                bluetoothLeDevice.getServiceData().get(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003"))[0]);
        assertEquals(0x03,
                bluetoothLeDevice.getServiceData().get(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003"))[1]);
    }

    @Test
    public void findServiceTest() throws KuraBluetoothResourceNotFoundException {
        assertEquals("AA:BB:CC:DD:EE:FF", bluetoothLeDevice
                .findService(UUID.fromString("72579a7a-8ca7-11eb-8dcd-0242ac130003")).getDevice().getAddress());
    }

    @Test
    public void findServiceWithTimeoutTest() throws KuraBluetoothResourceNotFoundException {
        assertEquals("AA:BB:CC:DD:EE:FF", bluetoothLeDevice
                .findService(UUID.fromString("72579a7a-8ca7-11eb-8dcd-0242ac130003"), 1).getDevice().getAddress());
    }

    @Test
    public void findServicesTest() throws KuraBluetoothResourceNotFoundException {
        assertEquals("AA:BB:CC:DD:EE:FF", bluetoothLeDevice.findServices().get(0).getDevice().getAddress());
    }

    @Test(expected = KuraBluetoothConnectionException.class)
    public void connectTest() throws KuraBluetoothConnectionException {
        bluetoothLeDevice.connect();
    }

    @Test(expected = KuraBluetoothConnectionException.class)
    public void disconnectTest() throws KuraBluetoothConnectionException {
        bluetoothLeDevice.disconnect();
    }

    @Test(expected = KuraBluetoothConnectionException.class)
    public void connectProfileTest() throws KuraBluetoothConnectionException {
        bluetoothLeDevice.connectProfile(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003"));
    }

    @Test(expected = KuraBluetoothConnectionException.class)
    public void disconnectProfileTest() throws KuraBluetoothConnectionException {
        bluetoothLeDevice.disconnectProfile(UUID.fromString("6a5a83dc-8ca7-11eb-8dcd-0242ac130003"));
    }

    @Test(expected = KuraBluetoothPairException.class)
    public void pairTest() throws KuraBluetoothPairException {
        bluetoothLeDevice.pair();
    }

    @Test(expected = KuraBluetoothPairException.class)
    public void cancelPairTest() throws KuraBluetoothPairException {
        bluetoothLeDevice.cancelPairing();
    }
}
