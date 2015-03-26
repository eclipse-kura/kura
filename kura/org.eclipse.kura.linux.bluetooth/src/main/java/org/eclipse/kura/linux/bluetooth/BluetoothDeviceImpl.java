package org.eclipse.kura.linux.bluetooth;

import org.eclipse.kura.bluetooth.BluetoothConnector;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.linux.bluetooth.le.BluetoothGattImpl;

public class BluetoothDeviceImpl implements BluetoothDevice {
	
	public static final int DEVICE_TYPE_DUAL = 0x003;
	public static final int DEVICE_TYPE_LE = 0x002;
	public static final int DEVICE_TYPE_UNKNOWN = 0x000;

	private String m_name;
	private String m_address;
	
	public BluetoothDeviceImpl(String name, String address) {
		m_name = name;
		m_address = address;
	}

	@Override
	public String getName() {
		return m_name;
	}
	
	@Override
	public String getAdress() {
		return m_address;
	}

	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return DEVICE_TYPE_UNKNOWN;
	}

	@Override
	public BluetoothConnector getBluetoothConnector() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BluetoothGatt getBluetoothGatt() {
		return new BluetoothGattImpl();
	}

}
