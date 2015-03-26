package org.eclipse.kura.bluetooth;

import java.util.List;

public interface BluetoothScanListener {

	public void onScanFailed(int errorCode);
	public void onScanResults(List<BluetoothDevice> devices);
}
