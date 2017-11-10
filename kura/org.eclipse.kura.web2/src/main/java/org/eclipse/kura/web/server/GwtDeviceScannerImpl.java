package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtDeviceScannerModel;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceScanner;

public class GwtDeviceScannerImpl implements GwtDeviceScanner {

    static ArrayList<GwtDeviceScannerModel> listDeavice = new ArrayList<GwtDeviceScannerModel>();
    private Map<Integer, GwtDeviceScannerModel> mapTest = new HashMap<Integer, GwtDeviceScannerModel>();
    private BluetoothLeOptions options;
    private BluetoothLeAdapter bluetoothLeAdapter;
    static final String TEST = "Test";

    private void filterDevices(List<BluetoothLeDevice> devices) {
        // Scan for TI SensorTag
        int i = 0;
        for (BluetoothLeDevice bluetoothLeDevice : devices) {
            this.mapTest.put(i + 1, new GwtDeviceScannerModel(bluetoothLeDevice.getAddress(),
                    bluetoothLeDevice.getName(), bluetoothLeDevice.getRSSI(), bluetoothLeDevice.getTxPower()));
        }
        List<GwtDeviceScannerModel> listDeviceScanner = new ArrayList<GwtDeviceScannerModel>();
        for (Map.Entry<Integer, GwtDeviceScannerModel> e : mapTest.entrySet()) {
            listDeviceScanner.add(e.getValue());
        }
    }

    void performScan() {
        // Scan for devices
        if (this.bluetoothLeAdapter.isDiscovering()) {
            try {
                this.bluetoothLeAdapter.stopDiscovery();
            } catch (KuraException e) {
                // logger.error(DISCOVERY_STOP_EX, e);
            }
        }
        Future<List<BluetoothLeDevice>> future = this.bluetoothLeAdapter.findDevices(this.options.getScantime());
        try {
            filterDevices(future.get());
        } catch (InterruptedException | ExecutionException e) {
            // logger.error("Scan for devices failed", e);
        }
    }

    @Override
    public ArrayList<GwtDeviceScannerModel> findDeviceScanner(GwtXSRFToken xsrfToken, boolean hasNetAdmin)
            throws GwtKuraException {

        List<GwtDeviceScannerModel> pairs = new ArrayList<GwtDeviceScannerModel>();

        pairs.add(new GwtDeviceScannerModel("1", "2", (short) 1, (short) 3));
        pairs.add(new GwtDeviceScannerModel("3", "4", (short) 1, (short) 3));
        pairs.add(new GwtDeviceScannerModel("9", "5", (short) 1, (short) 3));
        pairs.add(new GwtDeviceScannerModel("6", "9", (short) 1, (short) 3));

        return new ArrayList<GwtDeviceScannerModel>(pairs);
    }
}
