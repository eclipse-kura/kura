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
package org.eclipse.kura.example.ble.tisensortag;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;
import org.eclipse.kura.bluetooth.BluetoothGattService;
import org.eclipse.kura.bluetooth.BluetoothLeNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// More documentation can be found in http://processors.wiki.ti.com/index.php/SensorTag_User_Guide for the CC2541
// and http://processors.wiki.ti.com/index.php/CC2650_SensorTag_User's_Guide for the CC2650

public class TiSensorTag implements BluetoothLeNotificationListener {

	private static final Logger s_logger = LoggerFactory.getLogger(TiSensorTag.class);
	
	private BluetoothGatt   m_bluetoothGatt;
	private BluetoothDevice m_device;
	private boolean         m_connected;
	private String          pressureCalibration;
	private boolean         CC2650;
	private String          firmwareRevision;
	
	public TiSensorTag(BluetoothDevice bluetoothDevice) {
		m_device = bluetoothDevice;
		m_connected = false;
		if (m_device.getName().contains("CC2650 SensorTag"))
			CC2650 = true;
		else
			CC2650 = false;
				
	}
	
	public BluetoothDevice getBluetoothDevice() {
		return m_device;
	}

	public void setBluetoothDevice(BluetoothDevice device) {
		m_device = device;
	}
	
	public boolean isConnected() {
		m_connected = checkConnection();
		return m_connected;
	}
	
    public boolean connect() {
        m_bluetoothGatt = m_device.getBluetoothGatt();
        boolean connected = false;
		try {
			connected = m_bluetoothGatt.connect();
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
        if(connected) {
            m_bluetoothGatt.setBluetoothLeNotificationListener(this);
            m_connected = true;
            return true;
        }
        else {
        	// If connect command is not executed, close gatttool
        	m_bluetoothGatt.disconnect();
        	m_connected = false;
            return false;
        }
    }
    
	public void disconnect() {
		if (m_bluetoothGatt != null) {
			m_bluetoothGatt.disconnect();
			m_connected = false;
		}
	}
	
    public boolean checkConnection() {
    	if (m_bluetoothGatt != null) {
    		boolean connected = false;
			try {
				connected = m_bluetoothGatt.checkConnection();
			} catch (KuraException e) {
				s_logger.error(e.toString());
			}
    		if(connected) {
    			m_connected = true;
    			return true;
    		}
    		else {
    			// If connect command is not executed, close gatttool
    			m_bluetoothGatt.disconnect();
    			m_connected = false;
    			return false;
    		}
    	}
    	else {
			m_connected = false;
			return false;
    	}
    }
	
	public boolean getCC2650() {
		return CC2650;
	}
	
	public void setCC2650(boolean cc2650) {
		this.CC2650 = cc2650;
	}
	
	public String getFirmareRevision() {
		return firmwareRevision;
	}
	
	public void setFirmwareRevision(String firmwareRevision) {
		this.firmwareRevision = firmwareRevision;
	}
	
	/*
	 * Discover services
	 */
	public List<BluetoothGattService> discoverServices() {
		return m_bluetoothGatt.getServices();
	}
	
	public List<BluetoothGattCharacteristic> getCharacteristics(String startHandle, String endHandle) {
		s_logger.info("List<BluetoothGattCharacteristic> getCharacteristics");
		return m_bluetoothGatt.getCharacteristics(startHandle, endHandle);
	}

	public String firmwareRevision() {
		String firmwareVersion = "";
		try {
			if (CC2650)
				firmwareVersion = hexAsciiToString(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2650));
			else {
				String firmware = m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_FIRMWARE_REVISION_2541);
				firmwareVersion = hexAsciiToString(firmware.substring(0,firmware.length()-3));
			}
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return firmwareVersion;
	}
	
	// ----------------------------------------------------------------------------------------------------------
	//
	//  Temperature Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide (for CC2541)
	//
	// ----------------------------------------------------------------------------------------------------------
	/*
	 * Enable temperature sensor
	 */
	public void enableTermometer() {
		// Write "01" to enable temperature sensor
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "01");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "01");
	}
	
	/*
	 * Disable temperature sensor
	 */
	public void disableTermometer() {
		// Write "00" disable temperature sensor
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "00");
	}
	
	/*
	 * Read temperature sensor
	 */
	public double[] readTemperature() {
		double[] temperatures = new double[2];
		// Read value
		try {
			if (CC2650)
				temperatures = calculateTemperature(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2650));
			else
				temperatures = calculateTemperature(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2541));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return temperatures;
	}
	
	/*
	 * Read temperature sensor by UUID
	 */
	public double[] readTemperatureByUuid() {
		double[] temperatures = new double[2];
		try {
			temperatures = calculateTemperature(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return temperatures;
	}
	
	/*
	 * Enable temperature notifications
	 */
	public void enableTemperatureNotifications() {
		// Write "01:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650, "01:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541, "01:00");
	}
	
	/*
	 * Disable temperature notifications
	 */
	public void disableTemperatureNotifications() {
		// Write "00:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650, "00:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541, "00:00");
	}
	
	/*
	 * Set sampling period (only for CC2650)
	 */
	public void setTermometerPeriod(String period) {
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_PERIOD_2650, period);
	}
	
	/*
	 * Calculate temperature
	 */
	private double[] calculateTemperature(String value) {
		
		s_logger.info("Received temperature value: " + value);
		
		double[] temperatures = new double[2];

		byte[] valueByte = hexStringToByteArray(value.replace(" ", ""));
				
		if (CC2650) {
			int ambT = shortUnsignedAtOffset(valueByte, 2);
			int objT = shortUnsignedAtOffset(valueByte, 0);
			temperatures[0] = (double) ((ambT >> 2) * 0.03125);
			temperatures[1] = (double) ((objT >> 2) * 0.03125);
		} else {
			
			int ambT = shortUnsignedAtOffset(valueByte, 2);
			int objT = shortSignedAtOffset(valueByte, 0);
			temperatures[0] = ambT / 128.0;
			
			double Vobj2 = objT;
			Vobj2 *= 0.00000015625;

			double Tdie = (ambT / 128.0) + 273.15;

			double S0 = 5.593E-14;	// Calibration factor
			double a1 = 1.75E-3;
			double a2 = -1.678E-5;
			double b0 = -2.94E-5;
			double b1 = -5.7E-7;
			double b2 = 4.63E-9;
			double c2 = 13.4;
			double Tref = 298.15;
			double S = S0*(1+a1*(Tdie - Tref)+a2*Math.pow((Tdie - Tref),2));
			double Vos = b0 + b1*(Tdie - Tref) + b2*Math.pow((Tdie - Tref),2);
			double fObj = (Vobj2 - Vos) + c2*Math.pow((Vobj2 - Vos),2);
			double tObj = Math.pow(Math.pow(Tdie,4) + (fObj/S),.25);

			temperatures[1] = tObj - 273.15;
		}
	    
	    return temperatures;
	}

	// ------------------------------------------------------------------------------------------------------------
	//
	//  Accelerometer Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide (for CC2541)
	//
	// ------------------------------------------------------------------------------------------------------------
	/*
	 * Enable accelerometer sensor
	 */
	public void enableAccelerometer(String config) {
		
		if (CC2650)
			// 0: gyro X, 1: gyro Y, 2: gyro Z
			// 3: acc X, 4: acc Y, 5: acc Z
			// 6: mag
			// 7: wake-on-motion
			// 8-9: acc range (0 : 2g, 1 : 4g, 2 : 8g, 3 : 16g)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, config);
		else {
			// Write "01" in order to enable the sensor in 2g range
			// Write "01" in order to select 2g range, "02" for 4g, "03" for 8g (only for firmware > 1.5)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_ENABLE_2541, config);
		}
	}
	
	/*
	 * Disable accelerometer sensor
	 */
	public void disableAccelerometer() {
		if (CC2650)
			// Write "0000" to disable accelerometer sensor
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, "0000");
		else
			// Write "00" to disable accelerometer sensor
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_ENABLE_2541, "00");
	}
	
	/*
	 * Read accelerometer sensor
	 */
	public double[] readAcceleration() {
		double[] acceleration = new double[3];
		// Read value 
		try {
			if (CC2650)
				acceleration = calculateAcceleration(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_VALUE_2650));
			else
				acceleration = calculateAcceleration(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_VALUE_2541));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return acceleration;
	}
	
	/*
	 * Read accelerometer sensor by UUID
	 */
	public double[] readAccelerationByUuid() {
		double[] acceleration = new double[3];
		try {
			if (CC2650)
				return calculateAcceleration(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE));
			else
				return calculateAcceleration(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_ACC_SENSOR_VALUE));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return acceleration;
	}
	
	/*
	 * Enable accelerometer notifications
	 */
	public void enableAccelerationNotifications() {
		// Write "01:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "01:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_NOTIFICATION_2541, "01:00");
	}
	
	/*
	 * Disable accelerometer notifications
	 */
	public void disableAccelerationNotifications() {
		// Write "00:00 to disable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "00:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_NOTIFICATION_2541, "00:00");
	}
	
	/*
	 * Set sampling period
	 */
	public void setAccelerometerPeriod(String period) {
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_PERIOD_2650, period);
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_PERIOD_2541, period);
	}
	
	/*
	 * Calculate acceleration
	 */
	private double[] calculateAcceleration(String value) {
		
		s_logger.info("Received accelerometer value: " + value);
		
		double[] acceleration = new double[3];
		byte[] valueByte = hexStringToByteArray(value.replace(" ", ""));
		
		if (CC2650) {
			final float SCALE = (float) 4096.0;
		
			int x = shortSignedAtOffset(valueByte, 6);
			int y = shortSignedAtOffset(valueByte, 8);
			int z = shortSignedAtOffset(valueByte, 10);
			
			acceleration[0] = (x / SCALE) * -1;
			acceleration[1] = (y / SCALE);
			acceleration[2] = (z / SCALE) * -1;
		}
		else {
			String[] tmp = value.split("\\s");
			int x = unsignedToSigned(Integer.parseInt(tmp[0], 16), 8);
			int y = unsignedToSigned(Integer.parseInt(tmp[1], 16), 8);
			int z = unsignedToSigned(Integer.parseInt(tmp[2], 16), 8) * -1;
	
			acceleration[0] = x / 64.0;
			acceleration[1] = y / 64.0;
			acceleration[2] = z / 64.0;
		}
		
		return acceleration;
	}
	
	// -------------------------------------------------------------------------------------------------------
	//
	//  Humidity Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide (for CC2541)
	//
	// -------------------------------------------------------------------------------------------------------
	/*
	 * Enable humidity sensor
	 */
	public void enableHygrometer() {
		// Write "01" to enable humidity sensor
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2650, "01");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2541, "01");
	}
	
	/*
	 * Disable humidity sensor
	 */
	public void disableHygrometer() {
		// Write "00" to disable humidity sensor
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2650, "00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2541, "00");
	}
	
	/*
	 * Read humidity sensor
	 */
	public float readHumidity() {
		float humidity = 0F;
		// Read value
		try {
			if (CC2650)
				humidity = calculateHumidity(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_VALUE_2650));
			else
				humidity = calculateHumidity(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_VALUE_2541));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return humidity;
	}
	
	/*
	 * Read humidity sensor by UUID
	 */
	public float readHumidityByUuid() {
		float humidity = 0F;
		try {
			humidity = calculateHumidity(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_HUM_SENSOR_VALUE));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return humidity;
	}
	
	/*
	 * Enable humidity notifications
	 */
	public void enableHumidityNotifications() {
		// Write "01:00 to 0x39 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2650, "01:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2541, "01:00");
	}
	
	/*
	 * Disable humidity notifications
	 */
	public void disableHumidityNotifications() {
		// Write "00:00 to 0x39 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2650, "00:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2541, "00:00");
	}
	
	/*
	 * Set sampling period (for CC2650 only)
	 */
	public void setHygrometerPeriod(String period) {
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_PERIOD_2650, period);
	}
	
	/*
	 * Calculate Humidity
	 */
	private float calculateHumidity(String value) {
		
		s_logger.info("Received barometer value: " + value);
		
		byte[] valueByte = hexStringToByteArray(value.replace(" ", ""));
		
		int hum = shortUnsignedAtOffset(valueByte, 2);
		
		float humf = 0f;
		
		if (CC2650) {
			humf = (hum / 65536f) * 100f;
		}
		else {
			hum = hum - (hum % 4);
			humf = (-6f) + 125f * (hum / 65535f);
		}
		return humf;
	}
	
	// -----------------------------------------------------------------------------------------------------------
	//
	//  Magnetometer Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide (for CC2541)
	//
	// -----------------------------------------------------------------------------------------------------------
	/*
	 * Enable magnetometer sensor
	 */
	public void enableMagnetometer(String config) {
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, config);
		else
			// Write "01" enable magnetometer sensor
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_ENABLE_2541, "01");
	}
	
	/*
	 * Disable magnetometer sensor
	 */
	public void disableMagnetometer() {
		if (CC2650)
			// Write "0000" to disable magnetometer sensor
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, "0000");
		else
			// Write "00" to disable magnetometer sensor
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_ENABLE_2541, "00");
	}
	
	/*
	 * Read magnetometer sensor
	 */
	public float[] readMagneticField() {
		float[] magnetic = new float[3];
		// Read value
		try {
			if (CC2650)
				magnetic = calculateMagneticField(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_VALUE_2650));
			else
				magnetic = calculateMagneticField(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_VALUE_2541));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return magnetic;
	}
	
	/*
	 * Read magnetometer sensor by UUID
	 */
	public float[] readMagneticFieldByUuid() {
		float[] magnetic = new float[3];
		try {
			if (CC2650)
				magnetic = calculateMagneticField(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE));
			else
				magnetic = calculateMagneticField(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_MAG_SENSOR_VALUE));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return magnetic;
	}
	
	/*
	 * Enable magnetometer notifications
	 */
	public void enableMagneticFieldNotifications() {
		// Write "01:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "01:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_NOTIFICATION_2541, "01:00");
	}
	
	/*
	 * Disable magnetometer notifications
	 */
	public void disableMagneticFieldNotifications() {
		// Write "00:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "00:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_NOTIFICATION_2541, "00:00");
	}
	
	/*
	 * Set sampling period
	 */
	public void setMagnetometerPeriod(String period) {
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_PERIOD_2650, period);
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_PERIOD_2541, period);
	}
	
	/*
	 * Calculate Magnetic Field
	 */
	private float[] calculateMagneticField(String value) {
		
		s_logger.info("Received magnetometer value: " + value);
		
		float[] magneticField = new float[3];
		
		byte[] valueByte = hexStringToByteArray(value.replace(" ", ""));
		
		if (CC2650) {
			
			final float SCALE = (float) (32768 / 4912);
			
			int x = shortSignedAtOffset(valueByte, 12);
			int y = shortSignedAtOffset(valueByte, 14);
			int z = shortSignedAtOffset(valueByte, 16);
			
			magneticField[0] = x / SCALE;
			magneticField[1] = y / SCALE; 
			magneticField[2] = z / SCALE;
		}
		else {

			int x = shortSignedAtOffset(valueByte, 0);
			int y = shortSignedAtOffset(valueByte, 2);
			int z = shortSignedAtOffset(valueByte, 4);
			
			magneticField[0] = x * (2000f / 65536f) * -1;
			magneticField[1] = y * (2000f / 65536f) * -1;
			magneticField[2] = z * (2000f / 65536f);
		}
		
		return magneticField;
	}
	
	// ------------------------------------------------------------------------------------------------------------------
	//
	//  Barometric Pressure Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide (for CC2541)
	//
	// ------------------------------------------------------------------------------------------------------------------
	/*
	 * Enable pressure sensor
	 */
	public void enableBarometer() {
		// Write "01" enable pressure sensor
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2650, "01");
		else {
			if (firmwareRevision.contains("1.4"))
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_4, "01");
			else
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_5, "01");
		}
	}
	
	/*
	 * Disable pressure sensor
	 */
	public void disableBarometer() {
		// Write "00" to disable pressure sensor
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2650, "00");
		else {
			if (firmwareRevision.contains("1.4"))
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_4, "00");
			else
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_5, "00");
		}
	}
	
	/*
	 * Calibrate pressure sensor
	 */
	public void calibrateBarometer() {
		// Write "02" to calibrate pressure sensor
		if (!CC2650) {
			if (firmwareRevision.contains("1.4")) 
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_4, "02");
			else
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE_2541_1_5, "02");
		}
	}
	
	/*
	 * Read calibration pressure sensor
	 */
	public String readCalibrationBarometer() {
		pressureCalibration = "";
		// Read value
		try {
			if (!CC2650) {
				if (firmwareRevision.contains("1.4")) 
					pressureCalibration = m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_CALIBRATION_2541_1_4);
				else
					pressureCalibration = m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_CALIBRATION_2541_1_5);
			}
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return pressureCalibration;
	}
	
	/*
	 * Read pressure sensor
	 */
	public double readPressure() {
		double pressure = 0.0;
		// Read value
		try {
			if (CC2650)
				pressure = calculatePressure(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_VALUE_2650));
			else
				if (firmwareRevision.contains("1.4")) 
					pressure = calculatePressure(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_VALUE_2541_1_4));
				else
					pressure = calculatePressure(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_VALUE_2541_1_5));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return pressure;
	}
	
	/*
	 * Read pressure sensor by UUID
	 */
	public double readPressureByUuid() {
		double pressure = 0.0;
		try {
			pressure = calculatePressure(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_PRE_SENSOR_VALUE));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return pressure;		
	}
	
	/*
	 * Enable pressure notifications
	 */
	public void enablePressureNotifications() {
		// Write "01:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2650, "01:00");
		else
			if (firmwareRevision.contains("1.4")) 
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_4, "01:00");
			else
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_5, "01:00");
	}
	
	/*
	 * Disable pressure notifications
	 */
	public void disablePressureNotifications() {
		// Write "00:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2650, "00:00");
		else
			if (firmwareRevision.contains("1.4")) 
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_4, "00:00");
			else
				m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION_2541_1_5, "00:00");
	}
	
	/*
	 * Set sampling period (only for CC2650)
	 */
	public void setBarometerPeriod(String period) {
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_PERIOD_2650, period);
	}
	
	/*
	 * Calculate pressure
	 */
	private double calculatePressure(String value) {

		s_logger.info("Received pressure value: " + value);
		
		double p_a = 0.0;
		byte[] valueByte = hexStringToByteArray(value.replace(" ", ""));
		
		if (CC2650) {
            
            if (valueByte.length > 4) {
            	Integer val = twentyFourBitUnsignedAtOffset(valueByte, 3);
                p_a = val / 100.0;
            }
			else {
                int mantissa;
                int exponent;
                Integer pre = shortUnsignedAtOffset(valueByte, 2);

                mantissa = pre & 0x0FFF;
                exponent = (pre >> 12) & 0xFF;

                double output;
                double magnitude = Math.pow(2.0, (double) exponent);
                output = (mantissa * magnitude);
                p_a = output / 100.0;
            }
            
		}
		else {
			
			int t_r = shortSignedAtOffset(valueByte, 0);
			int p_r = shortUnsignedAtOffset(valueByte, 2);
			
			byte[] pressureCalibrationByte = hexStringToByteArray(pressureCalibration.replace(" ", ""));
			int c[] = new int[8];
			c[0] = shortUnsignedAtOffset(pressureCalibrationByte, 0);
			c[1] = shortUnsignedAtOffset(pressureCalibrationByte, 2);
			c[2] = shortUnsignedAtOffset(pressureCalibrationByte, 4);
			c[3] = shortUnsignedAtOffset(pressureCalibrationByte, 6);
			c[4] = shortSignedAtOffset(pressureCalibrationByte, 8);
			c[5] = shortSignedAtOffset(pressureCalibrationByte, 10);
			c[6] = shortSignedAtOffset(pressureCalibrationByte, 12);
			c[7] = shortSignedAtOffset(pressureCalibrationByte, 14);
			
			// Ignore temperature from pressure sensor
			// double t_a = (100 * (c[0] * t_r / Math.pow(2,8) + c[1] * Math.pow(2,6))) / Math.pow(2,16);
		    double S = c[2] + c[3] * t_r / Math.pow(2,17) + ((c[4] * t_r / Math.pow(2,15)) * t_r) / Math.pow(2,19);
		    double O = c[5] * Math.pow(2,14) + c[6] * t_r / Math.pow(2,3) + ((c[7] * t_r / Math.pow(2,15)) * t_r) / Math.pow(2,4);
		    p_a = (S * p_r + O) / Math.pow(2,14) / 100.0;
		    
		}
		
	    return p_a;
	}

	// --------------------------------------------------------------------------------------------------------
	//
	//  Gyroscope Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide (for CC2541)
	//
	// --------------------------------------------------------------------------------------------------------
	/*
	 * Enable gyroscope sensor
	 */
	public void enableGyroscope(String enable) {
		if (CC2650) {
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, enable);
		}
		else {
			// Write "00" to turn off gyroscope, "01" to enable X axis only, "02" to enable Y axis only, 
			// "03" = X and Y, "04" = Z only, "05" = X and Z, "06" = Y and Z and "07" = X, Y and Z.  
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_GYR_SENSOR_ENABLE_2541, enable);
		}
	}
	
	/*
	 * Disable gyroscope sensor
	 */
	public void disableGyroscope() {
		// Write "00" to disable gyroscope sensor
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_ENABLE_2650, "0000");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_GYR_SENSOR_ENABLE_2541, "00");
	}
	
	/*
	 * Read gyroscope sensor
	 */
	public float[] readGyroscope() {
		float[] gyroscope = new float[3];
		// Read value
		try {
			if (CC2650)
				gyroscope = calculateGyroscope(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_VALUE_2650));
			else
				gyroscope = calculateGyroscope(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_GYR_SENSOR_VALUE_2541));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return gyroscope;
	}
	
	/*
	 * Read gyroscope sensor by UUID
	 */
	public float[] readGyroscopeByUuid() {
		float[] gyroscope = new float[3];
		try {
			if (CC2650)
				gyroscope = calculateGyroscope(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_MOV_SENSOR_VALUE));
			else
				gyroscope = calculateGyroscope(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_GYR_SENSOR_VALUE));
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return gyroscope;			
	}
	
	/*
	 * Enable gyroscope notifications
	 */
	public void enableGyroscopeNotifications() {
		// Write "01:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "01:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_GYR_SENSOR_NOTIFICATION_2541, "01:00");
	}
	
	/*
	 * Disable gyroscope notifications
	 */
	public void disableGyroscopeNotifications() {
		// Write "00:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_NOTIFICATION_2650, "00:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_GYR_SENSOR_NOTIFICATION_2541, "00:00");
	}

	/*
	 * Set sampling period (only for CC2650)
	 */
	public void setGyroscopePeriod(String period) {
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MOV_SENSOR_PERIOD_2650, period);
	}	
	
	/*
	 * Calculate gyroscope
	 */
	private float[] calculateGyroscope(String value) {
		
		s_logger.info("Received gyro value: " + value);
		
		float[] gyroscope = new float[3];
		byte[] valueByte = hexStringToByteArray(value.replace(" ", ""));
		
		int y = shortSignedAtOffset(valueByte, 0);
		int x = shortSignedAtOffset(valueByte, 2); 
		int z = shortSignedAtOffset(valueByte, 4);
		
		if (CC2650) {
			
			final float SCALE = (float) (65535 / 500);
			
			gyroscope[0] = x / SCALE;
			gyroscope[1] = y / SCALE;
			gyroscope[2] = z / SCALE;
		}
		else {
			gyroscope[0] = x * (500f / 65536f);
			gyroscope[1] = y * (500f / 65536f) * -1;
			gyroscope[2] = z * (500f / 65536f);
		}

		return gyroscope;
	}

	// -------------------------------------------------------------------------------------------------------
	//
	//  Optical Sensor
	//
	// -------------------------------------------------------------------------------------------------------
	/*
	 * Enable optical sensor
	 */
	public void enableLuxometer() {
		// Write "01" to enable light sensor
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_OPTO_SENSOR_ENABLE_2650, "01");
		else
			s_logger.info("Not optical sensor on CC2541.");
			
	}
	
	/*
	 * Disable optical sensor
	 */
	public void disableLuxometer() {
		// Write "00" to disable light sensor
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_OPTO_SENSOR_ENABLE_2650, "00");
		else
			s_logger.info("Not optical sensor on CC2541.");
	}
	
	/*
	 * Read optical sensor
	 */
	public double readLight() {
		double light = 0.0;
		// Read value
		try {
			if (CC2650)
				light = calculateLight(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_OPTO_SENSOR_VALUE_2650));
			else {
				s_logger.info("Not optical sensor on CC2541.");
				light = 0.0;
			}
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return light;
	}
	
	/*
	 * Read optical sensor by UUID
	 */
	public double readLightByUuid() {
		double light = 0.0;
		try {
			if (CC2650)
				light = calculateLight(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_OPTO_SENSOR_VALUE));
			else {
				s_logger.info("Not optical sensor on CC2541.");
				light = 0.0;
			}
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return light;
	}
	
	/*
	 * Enable optical notifications
	 */
	public void enableLightNotifications() {
		// Write "01:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_OPTO_SENSOR_NOTIFICATION_2650, "01:00");
		else
			s_logger.info("Not optical sensor on CC2541.");
	}
	
	/*
	 * Disable optical notifications
	 */
	public void disableLightNotifications() {
		// Write "00:00 to enable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_OPTO_SENSOR_NOTIFICATION_2650, "00:00");
		else
			s_logger.info("Not optical sensor on CC2541.");
	}
	
	/*
	 * Set sampling period (only for CC2650)
	 */
	public void setLuxometerPeriod(String period) {
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_OPTO_SENSOR_PERIOD_2650, period);
	}
	
	/*
	 * Calculate light
	 */
	private double calculateLight(String value) {
		
		s_logger.info("Received luxometer value: " + value);
		
		byte[] valueByte = hexStringToByteArray(value.replace(" ", ""));
		int sfloat = shortUnsignedAtOffset(valueByte, 0);
		
		int mantissa;
		int exponent;

		mantissa = sfloat & 0x0FFF;
		exponent = (sfloat & 0xF000) >> 12;

		return (double) mantissa * (0.01 * Math.pow(2.0, (double) exponent)); 
		
	}

	// --------------------------------------------------------------------------------------------
	//
	//  Keys, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide (for CC2541)
	//
	// --------------------------------------------------------------------------------------------
	/*
	 * Read keys status
	 */
	public String readKeysStatus() {
		String key = "";
		// Read value
		try {
			if (CC2650)
				key = m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_KEYS_STATUS_2650);
			else
				key = m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_KEYS_STATUS_2541);
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return key;
	}
	
	/*
	 * Read keys status by UUID
	 */
	public String readKeysStatusByUuid() {
		String key = "";
		try {
			key = m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_KEYS_STATUS);
		} catch (KuraException e) {
			s_logger.error(e.toString());
		}
		return key;			
	}
	
	/*
	 * Enable keys notification
	 */
	public void enableKeysNotification() {
		//Write "01:00 to enable keys
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION_2650, "01:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION_2541, "01:00");
	}
	/*
	 * Disable keys notifications
	 */
	public void disableKeysNotifications() {
		//Write "00:00 to disable notifications
		if (CC2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION_2650, "00:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION_2541, "00:00");
	}
	
	// ---------------------------------------------------------------------------------------------
	//
	//  BluetoothLeNotificationListener API
	//
	// ---------------------------------------------------------------------------------------------
	@Override
	public void onDataReceived(String handle, String value) {
		
		if (handle.equals(TiSensorTagGatt.HANDLE_KEYS_STATUS_2541) || handle.equals(TiSensorTagGatt.HANDLE_KEYS_STATUS_2650)) {
			s_logger.info("Received keys value: " + value);
			if (!value.equals("00"))
				BluetoothLe.doPublishKeys(m_device.getAdress(), Integer.parseInt(value) );
		}
		
	}
	
	// ---------------------------------------------------------------------------------------------
	//
	//  Auxiliary methods
	//
	// ---------------------------------------------------------------------------------------------
	private int unsignedToSigned(int unsigned, int bitLength) {
		if ((unsigned & (1 << bitLength-1)) != 0) {
            unsigned = -1 * ((1 << bitLength-1) - (unsigned & ((1 << bitLength-1) - 1)));
        }
        return unsigned;
	}

	private String hexAsciiToString(String hex) {
		hex = hex.replaceAll(" ", "");
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < hex.length(); i+=2) {
			String str = hex.substring(i, i+2);
			output.append((char)Integer.parseInt(str, 16));
		}
		return output.toString();
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
    private static Integer shortSignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset+1]; // Interpret MSB as signedan
        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset+1] & 0xFF;
        return (upperByte << 8) + lowerByte;
    }
    private static Integer twentyFourBitUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer mediumByte = (int) c[offset+1] & 0xFF;
        Integer upperByte = (int) c[offset + 2] & 0xFF;
        return (upperByte << 16) + (mediumByte << 8) + lowerByte;
    }
}
