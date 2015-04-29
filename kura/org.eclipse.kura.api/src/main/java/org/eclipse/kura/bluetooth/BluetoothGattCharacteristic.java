package org.eclipse.kura.bluetooth;

import java.util.UUID;

public interface BluetoothGattCharacteristic {

	/*
	 * Get UUID of this characteristic
	 */
	public UUID getUuid();
	
	/*
	 * Get value of this characteristic
	 */
	public Object getValue();
	
	/*
	 * Set value of this characteristic
	 */
	public void setValue(Object value);
	
	/*
	 * Get permissions of this characteristic
	 */
	public int getPermissions();
	
	public String getHandle();
	public int getProperties();
	public String getValueHandle();
}
