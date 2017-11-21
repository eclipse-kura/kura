package org.eclipse.kura.web.server.ublox;

/** class name : BluetoothLeGattServiceImpl
 * implement the BluetoothLeGattService Interface
 * BluetoothLeScanListener is implemented wishing to receive notifications on Bluetooth LE scan events.
 *  @author NarcisseKAPDJOU
 *  @version 1.0.1
 */
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BluetoothLeScanListenerImpl implements BluetoothLeScanListener {

    private BluetoothAtCommand at = new BluetoothAtCommand();
    private BluetoothLeAdapterImpl adap = new BluetoothLeAdapterImpl();
    private final Map<String, BluetoothLeDeviceImpl> m_devices;
    private long scan_timeout = 60 * 1000; // 60 seconds
    private String scan_end = "";

    // private List<BluetoothLeDevice> m_scanResult;
    private boolean m_scanRunning = false;

    public BluetoothLeScanListenerImpl() {
        this.m_devices = new HashMap<String, BluetoothLeDeviceImpl>();
    }

    /*
     * public void startLeScan(){
     * String adapterName = adap.getInterfaceName();
     * setScanRunning(true);
     * at.discoveryBle(adapterName);
     * if(BluetoothAtCommand.Atscan_timeout > scan_timeout)
     * {
     * setScanRunning(false);
     * }
     * }
     * 
     * public static long getScanTimeout()
     * {
     * return scan_timeout;
     * }
     * 
     * 
     * public boolean isScanRunning() {
     * return this.m_scanRunning;
     * }
     * 
     * private void setScanRunning(boolean scanRunning) {
     * this.m_scanRunning = scanRunning;
     * }
     */

    public void addDevice(BluetoothLeDeviceImpl device) {
        if (!m_devices.containsKey(device.getAddress())) {
            m_devices.put(device.getAddress(), device);
        } else if (device.getName().length() != 0) {
            m_devices.put(device.getAddress(), device);
        }

    }

    public Map<String, BluetoothLeDeviceImpl> getMapScan() {
        return m_devices;
    }

    @Override
    public void onScanFailed(int errorCode) {
        // TODO Auto-generated method stub

    }

    /*
     * print list of devices discovery on the console
     * 
     */
    public void onScanResults() {
        System.out.println("call onScan");
        m_scanRunning = false;
        for (BluetoothLeDeviceImpl device : m_devices.values()) {
            System.out.println(device.getAddress() + "," + device.getName() + "," + device.getRSSI() + ","
                    + device.getTimeStamp());
        }
    }

    @Override
    public void onScanResults(List<BluetoothLeDevice> devices) {

    }

}
