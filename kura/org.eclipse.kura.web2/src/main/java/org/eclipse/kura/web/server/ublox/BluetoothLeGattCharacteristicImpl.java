package org.eclipse.kura.web.server.ublox;

/** class name : BluetoothLeGattCharacteristicImpl
 * implement the BluetoothLeGattCharacteristic Interface
 *  @author NarcisseKAPDJOU
 *  @version 1.0.1
 */
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class BluetoothLeGattCharacteristicImpl implements BluetoothLeGattCharacteristic {

    // private static BluetoothAtCommand at = new BluetoothAtCommand();
    // private static BluetoothLeServiceImpl service_gatt = new BluetoothLeServiceImpl();
    private static int conn_handle;
    private static int attr_handle;
    private static byte[] propreties;
    private static int value_handle;
    private static String uuid;

    /*
     * GATT : response of discover all characteristics of services
     */
    public BluetoothLeGattCharacteristicImpl(int conn_handle, int attr_handle, byte[] propreties, int value_handle,
            String uuid) {
        this.conn_handle = conn_handle;
        this.attr_handle = attr_handle;
        this.propreties = propreties;
        this.uuid = uuid;
    }

    public BluetoothLeGattCharacteristicImpl() {

    }

    public static int getConnHandle() {
        return conn_handle;
    }

    public static int getAttHandle() {
        return attr_handle;
    }

    public static byte[] getPropreties() {
        return propreties;
    }

    public static int valueHandle() {
        return value_handle;
    }

    public static String getUuid() {
        return uuid;
    }

    @Override
    public BluetoothLeGattDescriptor findDescriptor(UUID uuid) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<BluetoothLeGattDescriptor> findDescriptors() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] readValue() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void enableValueNotifications(Consumer<byte[]> callback) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void disableValueNotifications() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeValue(byte[] value) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public UUID getUUID() {

        return null;
    }

    @Override
    public BluetoothLeServiceImpl getService() {

        return null;
    }

    @Override
    public byte[] getValue() {
        // at.getCharacteristicsOfvalue(value_handle, service_end_handle);
        return null;
    }

    @Override
    public boolean isNotifying() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<BluetoothLeGattCharacteristicProperties> getProperties() {
        // at.getCaracteristiquesByStartEnd(start, end);
        return null;
    }

}
