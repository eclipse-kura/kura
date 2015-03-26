package org.eclipse.kura.bluetooth;

public interface BluetoothDevice {

	public String getName();
	public String getAdress();
	public int getType();
	public BluetoothConnector getBluetoothConnector();
	public BluetoothGatt getBluetoothGatt();
	
}
