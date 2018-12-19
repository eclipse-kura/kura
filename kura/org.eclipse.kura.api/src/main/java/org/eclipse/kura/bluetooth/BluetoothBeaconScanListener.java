package org.eclipse.kura.bluetooth;

import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * BluetoothBeaconScanListener must be implemented by any class
 * wishing to receive BLE beacon data
 *
 * @deprecated This class is deprecated in favor of {@link BluetoothLeBeaconListener}
 * 
 */
@ConsumerType
@Deprecated
public interface BluetoothBeaconScanListener {

    /**
     * Fired when bluetooth beacon data is received
     *
     * @param beaconData
     */
    public void onBeaconDataReceived(BluetoothBeaconData beaconData);

}
