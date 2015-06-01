package org.eclipse.kura.bluetooth;

/**
 * BluetoothAdapter represents the physical Bluetooth adapter on the host machine (ex: hci0).
 *
 */
public interface BluetoothAdapter {

	/**
	 * Get the MAC address of the Bluetooth adapter.
	 * 
	 * @return The MAC address of the adapter
	 */
	public String getAddress();
	
	/**
	 * Kill the process started by startLeScan.<br>
	 * SIGINT must be sent to the hcitool process. Otherwise the adapter must be toggled (down/up).
	 * 
	 */
	public void killLeScan();
	
	/**
	 * Return true if a lescan is running
	 * 
	 */
	public boolean isScanning();

	/**
	 * Return the status of the adapter
	 * 
	 * @return true if adapter is enabled, false otherwise
	 */
	public boolean isEnabled();
	
	/**
	 * Return true if the adapter supports Bluetooth LE.
	 * 
	 * @return	true if the adapter supports Bluetooth LE, false otherwise
	 */
	public boolean isLeReady();
	
	/**
	 * Enable the Bluetooth adapter
	 */
	public void enable();
	
	/**
	 * Disable the Bluetooth adapter
	 */
	public void disable();
	
	/**
	 * Starts an asynchronous scan for Bluetooth LE devices. Results are
	 * relayed through the {@link BluetoothLeScanListener} when the scan
	 * is complete.
	 * 
	 * @param listener	Interface for collecting scan results
	 */
	public void startLeScan(BluetoothLeScanListener listener);
	
	/**
	 * Get a remote Bluetooth device based on hardware adress
	 * 
	 * @param address	Hardware address of remote device
	 * @return BluetoothDevice
	 */
	public BluetoothDevice getRemoteDevice(String address);
	
}
