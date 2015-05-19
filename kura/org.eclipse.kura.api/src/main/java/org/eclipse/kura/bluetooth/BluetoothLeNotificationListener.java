package org.eclipse.kura.bluetooth;

/**
 * BluetoothLeNotificationListener must be implemented by any class
 * wishing to receive notifications on Bluetooth LE
 * notification events.
 *
 */
public interface BluetoothLeNotificationListener {

	/**
	 * Fired when notification data is received from the
	 * Bluetooth LE device.
	 * 
	 * @param handle Handle of Characteristic
	 * @param value	 Value received from the device
	 */
	public void onDataReceived(String handle, String value);
}
