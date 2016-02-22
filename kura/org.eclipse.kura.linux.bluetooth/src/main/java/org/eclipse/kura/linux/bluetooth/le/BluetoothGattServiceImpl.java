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

	@Override
	public String getStartHandle() {
		return m_startHandle;
	}

	@Override
	public String getEndHandle() {
		return m_endHandle;
	}
}
