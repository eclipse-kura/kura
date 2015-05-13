package org.eclipse.kura.linux.bluetooth.util;

public interface BluetoothProcessListener {
	
	public void processInputStream(String string);
	
	public void processInputStream(int ch);
	
	public void processErrorStream(String string);
	
}
