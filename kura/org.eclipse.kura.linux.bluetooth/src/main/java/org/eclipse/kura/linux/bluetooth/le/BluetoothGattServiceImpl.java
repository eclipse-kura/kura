package org.eclipse.kura.linux.bluetooth.le;

import java.util.List;
import java.util.UUID;

import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;
import org.eclipse.kura.bluetooth.BluetoothGattService;

public class BluetoothGattServiceImpl implements BluetoothGattService {

	private UUID m_uuid;
	private String m_startHandle;
	private String m_endHandle;
	
	public BluetoothGattServiceImpl(String uuid, String startHandle, String endHandle) {
		m_uuid = UUID.fromString(uuid);
		m_startHandle = startHandle;
		m_endHandle = endHandle;
	}
	
	// --------------------------------------------------------------------
	//
	//  BluetoothGattService API
	//
	// --------------------------------------------------------------------

	@Override
	public BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BluetoothGattCharacteristic> getCharacterisitcs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UUID getUuid() {
		return m_uuid;
	}

}
