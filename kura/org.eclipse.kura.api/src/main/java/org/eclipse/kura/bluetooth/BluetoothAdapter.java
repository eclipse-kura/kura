package org.eclipse.kura.bluetooth;

public interface BluetoothAdapter {

	public String getAddress();
	public boolean isEnabled();
	public boolean isLeReady();
	public void enable();
	public void disable();
	
	public void startLeScan(BluetoothScanListener listener);
	
}
