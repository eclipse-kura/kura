package org.eclipse.kura.bluetooth;

import java.util.List;

/**
 * BluetoothLeScanListener must be implemented by any class
 * wishing to receive notifications on Bluetooth LE
 * scan events.
 *
 */
public interface BluetoothLeScanListener {

	/**
	 * Fired when an error in the scan has occurred.
	 * 
	 * @param errorCode
	 */
	public void onScanFailed(int errorCode);
	
	/**
	 * Fired when the Bluetooth LE scan is complete.
	 * 
	 * @param devices	A list of found devices
	 */
	public void onScanResults(List<BluetoothDevice> devices);
	
}
