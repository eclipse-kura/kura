package org.eclipse.kura.bluetooth;


/**
 * BluetoothService provides a mechanism for interfacing with Standard
 * Bluetooth and Bluetooth LE devices.
 * 
 */
public interface BluetoothService {

	/**
	 * Get the default Bluetooth adapter for the host machine.
	 * 
	 * @return	Default Bluetooth adapter
	 */
	public BluetoothAdapter getBluetoothAdapter();
	
	/**
	 * Get the Bluetooth adapter specified by name.
	 * 
	 * @param name	Name of the Bluetooth Adapter
	 * @return	Bluetooth Adapter
	 */
	public BluetoothAdapter getBluetoothAdapter(String name);

	/**
	 * Get the Bluetooth adapter specified by name.
	 * 
	 * @param name	Name of the Bluetooth Adapter
	 * @param bbcl	Bluetooth Beacon Listener for commands
	 * @return	Bluetooth Adapter
	 */
	public BluetoothAdapter getBluetoothAdapter(String name, BluetoothBeaconCommandListener bbcl);
}
