package org.eclipse.kura.bluetooth;

public interface BluetoothAdapter {

	public String getAddress();
	public int getScanTime();
	public void setScanTime(int scanTime);
	public boolean isEnabled();
	public boolean isLeReady();
	public void enable();
	public void disable();
	
	public void startLeScan(BluetoothScanListener listener);
	
}
