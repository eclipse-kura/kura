package org.eclipse.kura.bluetooth;

/**
 * BluetoothLeScanListener must be implemented by any class
 * wishing to receive notifications on Bluetooth LE
 * scan events.
 *
 */
public interface BluetoothBeaconScanListener {
	
	/**
	 * Fired when bluetooth beacon data is received
	 * 
	 * @param beaconData
	 */
	public void onBeaconDataReceived(BluetoothBeaconData beaconData);
	
}
