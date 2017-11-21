package org.eclipse.kura.web.server.ublox;
/** class name : BluetoothLeAdapterImpl
 * implement the BluetoothLeAdapter Interface
 *  @author NarcisseKAPDJOU
 *  @version 1.0.1
 */

import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

public class BluetoothLeAdapterImpl implements BluetoothLeAdapter {

    private final Logger logger = Logger.getLogger(BluetoothLeAdapterImpl.class);
    private BluetoothAtCommand at = new BluetoothAtCommand();
    private String bd_addr;     // mac address of device
    private String name_adapter; // name of the adapter
    private String interface_name = new String("/dev/ttymxc2");
    private boolean scanRunning = false;
    private static long scan_timeout = 60 * 1000; // 60 seconds
    private boolean discoverable = false;

    public BluetoothLeAdapterImpl() {
        // TODO Auto-generated constructor stub
    }

    /*
     * implement a constructor who to initialize a adapter object by his name, mac address and interface name
     * 
     * @param bd_addr
     * mac address of device
     * 
     * @param name_adapter
     * name of the adapter
     * 
     * @param interface_name
     * 
     * @return
     */
    public BluetoothLeAdapterImpl(String bd_addr, String name_adapter, String interface_name) {
        this.bd_addr = bd_addr;
        this.name_adapter = name_adapter;
        this.interface_name = interface_name;
    }

    /*
     * stop discovery services
     * 
     * @param
     * 
     * @return
     */
    @Override
    public void stopDiscovery() throws Exception {

    }

    @Override
    public String getAddress() {
        at.getAdapterMacAddress();
        return bd_addr;
    }

    @Override
    public String getName() {
        at.getAdapterName();
        return name_adapter;
    }

    /*
     * using by the class UBloxCommand to send the adapter name received on the serial port after
     * getName() function is called
     * 
     * @param name_adapterSet
     * adapter name
     * 
     * @return
     * 
     */
    public void setName(String name_adapterSet) {
        name_adapter = name_adapterSet;
    }

    /*
     * using by the class UBloxCommand to send the adapter mac address received on the serial port after
     * getAddress() function is called
     * 
     * @param bd_addrSet
     * adapter mac address
     * 
     * @return
     * 
     */
    public void setAddress(String bd_addrSet) {
        bd_addr = bd_addrSet;
    }

    /*
     * 
     * 
     * 
     */
    public void setInterfaceName(String interface_nameSet) {
        interface_name = interface_nameSet;
    }

    /*
     * called to start scan device
     * 
     * @param timeout
     * scan device time out
     * 
     * @return UBloxCommand.gestListScan().getMapScan()
     * Map list
     */
    public Map<String, BluetoothLeDeviceImpl> findDevice(long timeout) {
        logger.info("En cours de scan...");
        setScanTimeOut(timeout);
        scanRunning = true;
        setDiscoverable(true);
        at.scanBle(getInterfaceName());
        scanRunning = false;

        // UBloxCommand.getListScan().getScanningEnd();

        for (BluetoothLeDeviceImpl device : UBloxCommand.getListScan().getMapScan().values()) {
            System.out.println(device.getAddress() + "," + device.getName() + "," + device.getRSSI() + ","
                    + device.getTimeStamp());
            logger.info(device.getAddress());
        }

        return UBloxCommand.getListScan().getMapScan();
    }

    public static long getScanTimeout() {
        return scan_timeout;
    }

    public static void setScanTimeOut(long time) {
        scan_timeout = time;
    }

    @Override
    public String getModalias() {
        // TODO Auto-generated method stub
        return null;
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
    public long getBluetoothClass() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isPowered() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setPowered(boolean value) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDiscoverable() {
        return discoverable;
    }

    @Override
    public void setDiscoverable(boolean value) {
        this.discoverable = value;
    }

    @Override
    public long getDiscoverableTimeout() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setDiscoverableTimout(long value) {

        // TODO Auto-generated method stub

    }

    @Override
    public boolean isPairable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setPairable(boolean value) {
        // TODO Auto-generated method stub

    }

    @Override
    public long getPairableTimeout() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setPairableTimeout(long value) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDiscovering() {
        // TODO Auto-generated method stub
        return scanRunning;
    }

    @Override
    public UUID[] getUUIDs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInterfaceName() {
        return interface_name;
    }

}
