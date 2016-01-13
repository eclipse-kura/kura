package org.eclipse.kura.bluetooth;

public class BluetoothBeaconData {
	public String uuid;
	public String address;
	public int major, minor;
	public int rssi;
	
	@Override
	public String toString() {
		return "BluetoothBeaconData [uuid=" + uuid + ", address=" + address + ", major=" + major + ", minor=" + minor
				+ ", rssi=" + rssi + "]";
	}
}
