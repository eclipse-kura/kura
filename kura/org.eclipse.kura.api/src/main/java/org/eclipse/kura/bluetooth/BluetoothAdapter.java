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
	 * Return the amount of time used when scanning for Bluetooth devices.
	 * 
	 * @return The scan time in seconds
	 */
	public int getScanTime();
	
	/**
	 * Set the amount of time used when scanning for Bluetooth devices.
	 * 
	 * @param scanTime	The amount of time in seconds to scan
	 */
	public void setScanTime(int scanTime);
	
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
