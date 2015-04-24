package org.eclipse.kura.linux.bluetooth.le;

import java.util.UUID;

import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;

public class BluetoothGattCharacteristicImpl implements BluetoothGattCharacteristic {

	private UUID   m_uuid;
	private String m_handle;
	private int    m_properties;
	private String m_valueHandle;
	
	public BluetoothGattCharacteristicImpl(String uuid, String handle, String properties, String valueHandle) {
		m_uuid = UUID.fromString(uuid);
		m_handle = handle;
		m_properties = Integer.parseInt(properties.substring(2, properties.length()), 16);
		m_valueHandle = valueHandle;
	}
	
	// --------------------------------------------------------------------
	//
	//  BluetoothGattCharacteristic API
	//
	// --------------------------------------------------------------------
	@Override
	public UUID getUuid() {
		return m_uuid;
	}

	@Override
	public Object getValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setValue(Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getPermissions() {
		// TODO Auto-generated method stub
		return 0;
	}

}
