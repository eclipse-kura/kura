package org.eclipse.kura.internal.driver.ble.xdk;

import java.util.UUID;

public class XdkGatt {
	
	//Accelerometer Sensor Service:
	public static final UUID UUID_XDK_ACCELEROMETER = UUID.fromString("5a211d40-7166-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_ACCELEROMETER_X_AXIS = UUID.fromString("5a211d41-7166-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_ACCELEROMETER_Y_AXIS = UUID.fromString("5a211d42-7166-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_ACCELEROMETER_Z_AXIS = UUID.fromString("5a211d43-7166-11e4-82f8-0800200c9a66");
	
	//Gyro Sensor Service:
	public static final UUID UUID_XDK_GYRO = UUID.fromString("aca96a40-74a4-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_GYRO_X_AXIS = UUID.fromString("aca96a41-74a4-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_GYRO_Y_AXIS = UUID.fromString("aca96a42-74a4-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_GYRO_Z_AXIS = UUID.fromString("aca96a43-74a4-11e4-82f8-0800200c9a66");
	
	//Light Sensor Service: 
	public static final UUID UUID_XDK_LIGHT = UUID.fromString("38eb02c0-7540-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_LIGHT_MILLILUX = UUID.fromString("38eb02c1-7540-11e4-82f8-0800200c9a66");
	
	//Noise Sensor Service:
	public static final UUID UUID_XDK_NOISE = UUID.fromString("01033830-754c-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_NOISE_DPSPL = UUID.fromString("01033831-754c-11e4-82f8-0800200c9a66");
	
	//Magnetometer Sensor Service:
	public static final UUID UUID_XDK_MAGNETOMETER = UUID.fromString("651f4c00-7579-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_MAGNETOMETER_X_AXIS = UUID.fromString("651f4c01-7579-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_MAGNETOMETER_Y_AXIS = UUID.fromString("651f4c02-7579-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_MAGNETOMETER_Z_AXIS = UUID.fromString("651f4c03-7579-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_MAGNETOMETER_RESISTENCE = UUID.fromString("651f4c04-7579-11e4-82f8-0800200c9a66");
	
	//Environment Sensor Service:
	public static final UUID UUID_XDK_ENVIRONMENT = UUID.fromString("92dab060-7634-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_ENVIRONMENT_PRESSURE = UUID.fromString("92dab061-7634-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_ENVIRONMENT_TEMPERATURE = UUID.fromString("92dab062-7634-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_ENVIRONMENT_HUMIDITY = UUID.fromString("92dab063-7634-11e4-82f8-0800200c9a66");
	
	//High Data Rata Service:
	public static final UUID UUID_XDK_HIGH_DATA_RATE = UUID.fromString("c2967210-7ba4-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY = UUID.fromString("c2967211-7ba4-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY = UUID.fromString("c2967212-7ba4-11e4-82f8-0800200c9a66");
	
	//Control XDK Service:
	public static final UUID UUID_XDK_CONTROL_SERVICE = UUID.fromString("55b741d0-7ada-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION = UUID.fromString("55b741d1-7ada-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_CONTROL_SERVICE_CHANGE_SENSOR_SAMPLING_RATA = UUID.fromString("55b741d2-7ada-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_CONTROL_SERVICE_REBOOT = UUID.fromString("55b741d3-7ada-11e4-82f8-0800200c9a66");
	public static final UUID UUID_XDK_CONTROL_SERVICE_GET_FIRMWARE_VERSION = UUID.fromString("55b741d4-7ada-11e4-82f8-0800200c9a66");
	
	
	
	private XdkGatt() {
		//Not used
	}
	
}
