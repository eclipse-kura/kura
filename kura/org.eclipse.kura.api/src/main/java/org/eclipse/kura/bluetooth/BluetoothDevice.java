package org.eclipse.kura.bluetooth;

/**
 * BluetoothDevice represents a Bluetooth device to which connections
 * may be made. The type of Bluetooth device will determine the 
 * communications mechanism. Standard Bluetooth devices will use
 * the {@link BluetoothConnector} and Bluetooth LE devices will use
 * {@link BluetoothGatt}.
 * <br>
 * When using {@link BluetoothConnector}, A default connector is not provided
 * and will need to be implemented.
 *
 */
public interface BluetoothDevice {

	/**
	 * Returns the the name of the Bluetooth device.
	 * 
	 * @return	The devices name
	 */
	public String getName();
	
	/**
	 * Returns the physical address of the device.
	 * 
	 * @return	The physical address of the device
	 */
	public String getAdress();
	
	/**
	 * The type of devices, name whether the device supports
	 * Bluetooth LE or not.
	 * 
	 * @return	The device type
	 */
	public int getType();
	
	/**
	 * Return a connector for communicating with a standard
	 * Bluetooth device. A default connector is not provided
	 * and will need to be implemented.
	 * @return	Standard Bluetooth connector
	 */
	public BluetoothConnector getBluetoothConnector();
	
	/**
	 * Return an instance of a Bluetooth GATT server to be
	 * used in communicating with Bluetooth LE devices.
	 * 
	 * @return BluetoothGatt
	 */
	public BluetoothGatt getBluetoothGatt();
	
}
