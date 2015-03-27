package org.eclipse.kura.bluetooth;

public interface BluetoothGatt {

	public boolean connect(String address);
	public void disconnect();
	
}
