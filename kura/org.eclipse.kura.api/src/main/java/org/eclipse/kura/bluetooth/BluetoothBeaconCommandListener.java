package org.eclipse.kura.bluetooth;


/**
 * BluetoothBeaconCommandListener must be implemented by any class
 * wishing to receive notifications on Bluetooth Beacon
 * command results.
 *
 */
public interface BluetoothBeaconCommandListener {

	/**
	 * Fired when an error in the command execution has occurred.
	 * 
	 * @param errorCode
	 */
	public void onCommandFailed(String errorCode);
	
	/**
	 * Fired when the command succeeded.
	 * 
	 */
	public void onCommandResults(String results);
}
