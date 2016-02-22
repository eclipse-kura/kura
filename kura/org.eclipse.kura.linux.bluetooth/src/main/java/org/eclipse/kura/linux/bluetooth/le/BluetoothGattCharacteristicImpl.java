/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
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
		setHandle(handle);
		setProperties(Integer.parseInt(properties.substring(2, properties.length()), 16));
		setValueHandle(valueHandle);
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

	public void setHandle(String m_handle) {
		this.m_handle = m_handle;
	}

	public void setProperties(int m_properties) {
		this.m_properties = m_properties;
	}

	public void setValueHandle(String m_valueHandle) {
		this.m_valueHandle = m_valueHandle;
	}

	@Override
	public String getHandle() {
		return m_handle;
	}

	@Override
	public int getProperties() {
		return m_properties;
	}

	@Override
	public String getValueHandle() {
		return m_valueHandle;
	}

}
