package org.eclipse.kura.example.bluetooth;

import java.util.UUID;

public class TiSensorTagGatt {
	
	// These values are for TI CC2541
	// Refer to http://processors.wiki.ti.com/images/archive/a/a8/20130111154127!BLE_SensorTag_GATT_Server.pdf
	
	// Temperature sensor
	public static final String HANDLE_TEMP_SENSOR_VALUE			= "0x0025";
	public static final String HANDLE_TEMP_SENSOR_NOTIFICATION	= "0x0026";
	public static final String HANDLE_TEMP_SENSOR_ENABLE		= "0x0029";
	
	public static final UUID UUID_TEMP_SENSOR_VALUE			    = UUID.fromString("f000aa01-0451-4000-b000-000000000000");
	public static final UUID UUID_TEMP_SENSOR_ENABLE		    = UUID.fromString("f000aa02-0451-4000-b000-000000000000");
	
	// Accelerometer sensor
	public static final String HANDLE_ACC_SENSOR_VALUE			= "0x002d";
	public static final String HANDLE_ACC_SENSOR_NOTIFICATION	= "0x002e";
	public static final String HANDLE_ACC_SENSOR_ENABLE		    = "0x0031";

	public static final UUID UUID_ACC_SENSOR_VALUE			    = UUID.fromString("f000aa11-0451-4000-b000-000000000000");
	public static final UUID UUID_ACC_SENSOR_ENABLE		        = UUID.fromString("f000aa12-0451-4000-b000-000000000000");
	
	// Humidity sensor
	public static final String HANDLE_HUM_SENSOR_VALUE			= "0x0038";
	public static final String HANDLE_HUM_SENSOR_NOTIFICATION	= "0x0039";
	public static final String HANDLE_HUM_SENSOR_ENABLE		    = "0x003c";

	public static final UUID UUID_HUM_SENSOR_VALUE			    = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
	public static final UUID UUID_HUM_SENSOR_ENABLE		        = UUID.fromString("f000aa22-0451-4000-b000-000000000000");
	
	// Magnetometer sensor
	public static final String HANDLE_MAG_SENSOR_VALUE			= "0x0040";
	public static final String HANDLE_MAG_SENSOR_NOTIFICATION	= "0x0041";
	public static final String HANDLE_MAG_SENSOR_ENABLE		    = "0x0044";

	public static final UUID UUID_MAG_SENSOR_VALUE			    = UUID.fromString("f000aa31-0451-4000-b000-000000000000");
	public static final UUID UUID_MAG_SENSOR_ENABLE		        = UUID.fromString("f000aa32-0451-4000-b000-000000000000");
	
	// Pressure sensor
	public static final String HANDLE_PRE_SENSOR_VALUE			= "0x004b";
	public static final String HANDLE_PRE_SENSOR_NOTIFICATION	= "0x004c";
	public static final String HANDLE_PRE_SENSOR_ENABLE		    = "0x004f";
	public static final String HANDLE_PRE_CALIBRATION		    = "0x0052";

	public static final UUID UUID_PRE_SENSOR_VALUE			    = UUID.fromString("f000aa41-0451-4000-b000-000000000000");
	public static final UUID UUID_PRE_SENSOR_ENABLE		        = UUID.fromString("f000aa42-0451-4000-b000-000000000000");
	
	// Gyroscope sensor
	public static final String HANDLE_GYR_SENSOR_VALUE			= "0x0057";
	public static final String HANDLE_GYR_SENSOR_NOTIFICATION	= "0x0058";
	public static final String HANDLE_GYR_SENSOR_ENABLE		    = "0x005b";

	public static final UUID UUID_GYR_SENSOR_VALUE			    = UUID.fromString("f000aa51-0451-4000-b000-000000000000");
	public static final UUID UUID_GYR_SENSOR_ENABLE		        = UUID.fromString("f000aa52-0451-4000-b000-000000000000");
	
	// Keys
	public static final String HANDLE_KEYS_STATUS    			= "0x005f";
	public static final String HANDLE_KEYS_NOTIFICATION			= "0x0060";
	
	public static final UUID UUID_KEYS_STATUS   			    = UUID.fromString("f000ffe1-0451-4000-b000-000000000000");

}
