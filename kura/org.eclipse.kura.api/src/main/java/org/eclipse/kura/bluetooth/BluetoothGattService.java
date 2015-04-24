package org.eclipse.kura.bluetooth;

import java.util.List;
import java.util.UUID;

public interface BluetoothGattService {

	/*
	 * Get characteristic based on UUID
	 */
	public BluetoothGattCharacteristic getCharacteristic(UUID uuid);
	
	/*
	 * Get list of characteristics of the service
	 */
	public List<BluetoothGattCharacteristic> getCharacterisitcs();
	
	/*
	 * Return the UUID of this service
	 */
	public UUID getUuid();
}
