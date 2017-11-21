package org.eclipse.kura.web.server.ublox;

/** class name : BluetoothLeServiceImpl
 * implement the BluetoothLeService Interface
 *  @author NarcisseKAPDJOU
 *  @version 1.0.1
 */
import java.util.List;
import java.util.UUID;

public class BluetoothLeServiceImpl implements BluetoothLeService {

    private static int conn_handle; // connection handle of the connected device
    private static int start;       // start handle of the service
    private static int end;         // End handle of the service
    private static UUID uuid;        // UUID of the service. This can either be 16bit or 128bit

    public BluetoothLeServiceImpl() {

    }

    /**
     * initialize a service instance of this class. called in the GATT : +UBTGDP response of Discovers
     * all primary services on the remote device
     * 
     * @param conn_handle
     *            connexion handle
     * @param start
     *            start handle of the service
     * @param end
     *            end handle of the service
     * @param uuid
     *            uuid of the service. this can either be 16bit or 128bit
     * @return
     * 
     */
    public BluetoothLeServiceImpl(int conn_handle, int start, int end, UUID uuid) {
        this.conn_handle = conn_handle;
        this.start = start;
        this.end = end;
        this.uuid = uuid;
    }

    /**
     * initialize a service instance of this class. called in the BluetoothAtCommand Class GATT :+UBTGDU response of
     * Discovers all
     * primary services by UUID on the remote
     * 
     * @param conn_handle
     *            connexion handle
     * @param start
     *            start handle of the service
     * @param end
     *            end handle of the service
     * @return
     * 
     */
    public BluetoothLeServiceImpl(int conn_handle, int start, int end) {
        this.conn_handle = conn_handle;
        this.start = start;
        this.end = end;
    }

    public int getConnHandle() {
        return this.conn_handle;
    }

    public int getStart() {
        return this.start;
    }

    public int getEnd() {
        return this.end;

    }

    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public List<BluetoothLeAdapter> getAdapters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BluetoothLeAdapter getAdapter(String interfaceName) {
        // TODO Auto-generated method stub
        return null;
    }

}
