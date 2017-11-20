package org.eclipse.kura.web.server;

import java.util.HashSet;

import org.eclipse.kura.web.shared.model.GwtDeviceScannerModel;

import com.nokia.asgw.ublox.ble.impl.BluetoothAtCommand;
import com.nokia.asgw.ublox.ble.impl.BluetoothLeAdapterImpl;
import com.nokia.asgw.ublox.ble.impl.BluetoothLeDeviceImpl;
import com.nokia.asgw.ublox.ble.impl.BluetoothLeScanListenerImpl;

public class Ublox {

    private static BluetoothAtCommand at = new BluetoothAtCommand();
    private static BluetoothLeAdapterImpl adap = new BluetoothLeAdapterImpl();
    private static BluetoothLeDeviceImpl device = new BluetoothLeDeviceImpl();
    private static BluetoothLeScanListenerImpl bleDevice = new BluetoothLeScanListenerImpl();
    public HashSet<GwtDeviceScannerModel> listDevice = new HashSet<GwtDeviceScannerModel>();

    public Ublox(long time) {
        this.activate();
        this.getMapList(time);
    }

    public String activate() {
        at.connectSerialPort();
        return "connecting";
    }

    public HashSet<BluetoothLeDeviceImpl> getMapList(long time) {
        HashSet<BluetoothLeDeviceImpl> listDevice = new HashSet<BluetoothLeDeviceImpl>();
        for (BluetoothLeDeviceImpl device : adap.findDevice(time).values()) {
            this.listDevice.add(new GwtDeviceScannerModel(device.getAddress(), device.getName(), device.getTxPower(),
                    String.valueOf(device.getRSSI())));
        }
        return listDevice;
    }
}
