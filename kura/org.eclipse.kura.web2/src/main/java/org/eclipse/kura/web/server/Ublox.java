package org.eclipse.kura.web.server;

import java.util.HashSet;

import org.eclipse.kura.web.server.ublox.BluetoothAtCommand;
import org.eclipse.kura.web.server.ublox.BluetoothLeAdapterImpl;
import org.eclipse.kura.web.server.ublox.BluetoothLeDeviceImpl;
import org.eclipse.kura.web.server.ublox.BluetoothLeScanListenerImpl;
import org.eclipse.kura.web.shared.model.GwtDeviceScannerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ublox {

    private static final Logger logger = LoggerFactory.getLogger(Ublox.class);

    private BluetoothAtCommand at = new BluetoothAtCommand();
    private BluetoothLeAdapterImpl adap = new BluetoothLeAdapterImpl();
    private BluetoothLeDeviceImpl device = new BluetoothLeDeviceImpl();
    private BluetoothLeScanListenerImpl bleDevice = new BluetoothLeScanListenerImpl();
    public HashSet<GwtDeviceScannerModel> listDevice = new HashSet<GwtDeviceScannerModel>();

    public Ublox(long time) {
        logger.info("new instance...");
        this.activate();
        this.getMapList(time);
    }

    public String activate() {
        logger.info("ublox activate..");
        this.at.connectSerialPort();
        return "connecting";
    }

    public void getMapList(long time) {
        logger.info("scan..");
        for (BluetoothLeDeviceImpl device : this.adap.findDevice(time).values()) {
            this.listDevice.add(new GwtDeviceScannerModel(device.getAddress(), device.getName(), device.getTxPower(),
                    String.valueOf(device.getRSSI())));
            logger.info("address {} ", device.getAddress());
        }
    }
}
