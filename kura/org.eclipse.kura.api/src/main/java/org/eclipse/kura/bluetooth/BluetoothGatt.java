/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.bluetooth;

import java.util.List;
import java.util.UUID;

/**
 * The BluetoothGatt service is the main communication interface with the Bluettoth LE device. The service
 * will provide information about available services and mechanisms for reading and writing to
 * available characteristics.
 *
 */
public interface BluetoothGatt {

	/**
	 * Connect to devices GATT server.
	 * 
	 * @return If connection was successful
	 */
	public boolean connect();
	
	/**
	 * Disconnect from devices GATT server.
	 */
	public void disconnect();
	
	/**
	 * Check if the device is connected.
	 */
	public boolean checkConnection();
	
	/**
	 * Sets the listener by which asynchronous actions of the GATT
	 * server will be communicated.
	 * 
	 * @param listener	BluetoothLeListener
	 */
	public void setBluetoothLeNotificationListener(BluetoothLeNotificationListener listener);
	
	/**
	 * Return a GATT service based on a UUID.
	 * 
	 * @param uuid	UUID of service
	 * @return	BluetoothGattService
	 */
	public BluetoothGattService getService(UUID uuid);
	
	/**
	 * Get a list of GATT services offered by the device.
	 * 
	 * @return	List of services
	 */
	public List<BluetoothGattService> getServices();
	
	/**
	 * Get a list of GATT characteristics based on start and end handles. Handle boundaries
	 * can be obtained from the {@link #getServices() getServices} method.
	 * 
	 * @param startHandle	Start handle
	 * @param endHandle		End handle
	 * @return	List of GATT characteristics
	 */
	public List<BluetoothGattCharacteristic> getCharacteristics(String startHandle, String endHandle);
	
	/**
	 * Read characteristic value from handle.
	 * 
	 * @param handle	Characteristic handle
	 * @return	Characteristic value
	 */
	public String readCharacteristicValue(String handle);
	
	/**
	 * Read value from characteristic by UUID.
	 * 
	 * @param uuid	UUID of Characteristic
	 * @return	Characteristic value
	 */
	public String readCharacteristicValueByUuid(UUID uuid);
	
	/**
	 * Write value to characteristic.
	 * 
	 * @param handle Handle of Characteristic
	 * @param value	 Value to write to Characteristic
	 */
	public void writeCharacteristicValue(String handle, String value);
}
