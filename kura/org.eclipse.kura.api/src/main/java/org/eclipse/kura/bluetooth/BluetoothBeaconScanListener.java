package org.eclipse.kura.bluetooth;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * BluetoothBeaconScanListener must be implemented by any class
 * wishing to receive BLE beacon data
 *
 */
@ConsumerType
public interface BluetoothBeaconScanListener {

    /**
     * Fired when bluetooth beacon data is received
     *
     * @param beaconData
     */
    public void onBeaconDataReceived(BluetoothBeaconData beaconData);

}
