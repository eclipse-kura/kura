package org.eclipse.kura.web.server.ublox;
/** class name : BluetoothLeDeviceImpl
 * implement the BluetoothLeDevice Interface
 *  @author NarcisseKAPDJOU
 *  @version 1.0.1
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BluetoothLeDeviceImpl implements BluetoothLeDevice {

    private short rssi;       // power receive of device scanner
    private short rssi_con;   // rssi of receiver while to connect of device
    private String bd_addr;     // mac address of device
    private String name_device; // name of the adapter
    private String timeStamp;    // time and date for each discovery device
    private boolean connect = false;
    private boolean getListServices = false;
    private static UUID s_uuid;
    private static BluetoothAtCommand at = new BluetoothAtCommand();

    public BluetoothLeDeviceImpl() {

    }

    /**
     * initialize a device instance of this class. called while the discovery devices in the UBloxCommand class
     *
     * @param bd_addr
     *            address of the remote device
     * @param name_device
     *            name of the device
     * @param rssi
     *            power receiver of the remote device
     * @return
     */
    public BluetoothLeDeviceImpl(String bd_addr, String name_device, short rssi) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
        String string = dateFormat.format(new Date());
        this.rssi = rssi;
        this.bd_addr = bd_addr;
        this.name_device = name_device;
        this.timeStamp = string;
    }

    /**
     * disconnect from Ble device
     * 
     * @return
     */
    @Override
    public void disconnect() throws Exception {     // to put in the adapter class
        at.disconnectToBleDevice();
        connect = false;
    }

    /**
     * connect to Ble device
     * 
     * @return
     */
    @Override
    public void connect(String bd_addr) throws Exception {    // to put in the adapter class
        at.connectToBleDevice(bd_addr);
        // findServices();
        connect = true;
    }

    /*
     * @Override
     * public void disconnectProfile(UUID uuid) throws Exception {
     * // TODO Auto-generated method stub
     * 
     * }
     */
    @Override
    public void pair() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void cancelPairing() throws Exception {
        // TODO Auto-generated method stub

    }

    /**
     * return the mac address of the Device
     * 
     * @return bd_addr
     */
    @Override
    public String getAddress() {
        // TODO Auto-generated method stub
        return this.bd_addr;
    }

    /**
     * return the name of the Device
     * 
     * @return name_device
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return this.name_device;
    }

    /**
     * return the date and time
     * 
     * @return timeStamp
     */
    @Override
    public String getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public String getAlias() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAlias(String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getBluetoothClass() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public short getAppearance() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPaired() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isTrusted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setTrusted(boolean value) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isBlocked() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setBlocked(boolean value) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isLegacyPairing() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * return the rssi of a device
     * 
     * @return rssi
     */
    @Override
    public short getRSSI() {
        return rssi;
    }

    /**
     * return boolean who allow to know the state of the connection to device
     * 
     * @return connect
     *         true of false
     */
    @Override
    public boolean isConnected() {
        return connect;
    }

    @Override
    public UUID[] getUUIDs() {
        // at.getListServices();
        return null;
    }

    public static UUID getUUID() {
        return s_uuid;
    }

    @Override
    public String getModalias() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BluetoothLeAdapter getAdapter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Short, byte[]> getManufacturerData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<UUID, byte[]> getServiceData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public short getTxPower() {
        rssi_con = at.getRssiConnection(getAddress());
        return rssi_con;
    }

    @Override
    public List<BluetoothLeServiceImpl> findServiceByUuid(UUID uuid) throws Exception {
        if (getListServices = true) {
            s_uuid = uuid;
            at.getListServicesByUuid(uuid);

        }
        return UBloxCommand.getListServicesByUuid().getListServicesByUuid();
    }

    @Override
    public Map<UUID, BluetoothLeServiceImpl> findServices() throws Exception {
        if (isConnected()) {
            at.getListServices();
            getListServices = true;
        } else {
            disconnect();
            connect(getAddress());
            at.getListServices();
            getListServices = true;
        }
        return UBloxCommand.getListServices().getMapServices();
    }

    /*
     * 
     */
    public BluetoothLeGattCharacteristicImpl getCharacteristicOfServices(int start, int end) {

        at.getCaracteristiquesServices(start, end);
        return UBloxCommand.getCharacteristic();
    }

    public BluetoothLeGattDescriptorImpl getCharacteristicDescriptor(int value_handle, int service_end_handle) {

        at.getCharacteristicsDescriptor(value_handle, service_end_handle);
        return UBloxCommand.getDescriptor();
    }

    @Override
    public void connectProfile(UUID uuid) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void disconnectProfile(UUID uuid) throws Exception {
        // TODO Auto-generated method stub

    }

}
