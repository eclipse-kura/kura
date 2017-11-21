package org.eclipse.kura.web.server.ublox;

/** class name : BluetoothLeGattDescriptorImpl
 * implement the BluetoothLeGattDescriptor Interface
 *  @author NarcisseKAPDJOU
 *  @version 1.0.1
 */
import java.util.UUID;

public class BluetoothLeGattDescriptorImpl implements BluetoothLeGattDescriptor {

    private static int conn_handle;
    private static int char_handle;
    private static int desc_handle;
    private static String uuid;

    public BluetoothLeGattDescriptorImpl() {
        // TODO Auto-generated constructor stub
    }

    public BluetoothLeGattDescriptorImpl(int conn_handle, int char_handle, int desc_handle, String uuid) {
        this.conn_handle = conn_handle;
        this.char_handle = desc_handle;
        this.desc_handle = desc_handle;
        this.uuid = uuid;
    }

    public static int getConHandle() {
        return conn_handle;
    }

    public static int getCharHandle() {
        return char_handle;
    }

    public static int getDescHandle() {
        return desc_handle;
    }

    public static String getUuid() {
        return uuid;
    }

    @Override
    public UUID getUUID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BluetoothLeGattCharacteristic getCharacteristic() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * print characteristic of descriptor on the console
     */
    void descriptorOnResult() {
        System.out.println(
                UBloxCommand.getDescriptor().getConHandle() + "," + UBloxCommand.getDescriptor().getCharHandle() + ","
                        + UBloxCommand.getDescriptor().getDescHandle() + "," + UBloxCommand.getDescriptor().getUuid());

    }

    @Override
    public byte[] getValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] readValue() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeValue(byte[] value) throws Exception {
        // TODO Auto-generated method stub

    }

}
