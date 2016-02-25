package org.eclipse.kura.bluetooth;

/**
 * BluetoothBeaconScanListener must be implemented by any class
 * wishing to receive BLE beacon data
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
