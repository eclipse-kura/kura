package org.eclipse.kura.example.bluetooth;

import java.util.UUID;

public class TiSensorTagGatt {
	// these values are for TI CC2541
	public static final String HANDLE_TEMP_SENSOR_VALUE			= "0x0025";
	public static final String HANDLE_TEMP_SENSOR_NOTIFICATION	= "0x0026";
	public static final String HANDLE_TEMP_SENSOR_ENABLE		= "0x0029";
	
	public static final UUID UUID_TEMP_SENSOR_VALUE			= UUID.fromString("f000aa01-0451-4000-b000-000000000000");
	public static final UUID UUID_TEMP_SENSOR_ENABLE		= UUID.fromString("f000aa02-0451-4000-b000-000000000000");
	
}
