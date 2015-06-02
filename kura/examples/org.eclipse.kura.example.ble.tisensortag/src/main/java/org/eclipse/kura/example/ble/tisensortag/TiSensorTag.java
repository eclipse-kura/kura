package org.eclipse.kura.example.ble.tisensortag;

import java.util.List;
import java.util.UUID;

import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;
import org.eclipse.kura.bluetooth.BluetoothGattService;
import org.eclipse.kura.bluetooth.BluetoothLeNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TiSensorTag implements BluetoothLeNotificationListener {

	private static final Logger s_logger = LoggerFactory.getLogger(TiSensorTag.class);
	
	private BluetoothGatt m_bluetoothGatt;
	private BluetoothDevice m_device;
	private boolean m_connected;
	private String pressureCalibration;
	
	private boolean temperatureReceived =false;
	private double  tempAmbient = 0.0;
	private double  tempTarget = 0.0;
	
	public TiSensorTag(BluetoothDevice bluetoothDevice) {
		m_device = bluetoothDevice;
		m_connected = false;
	}
	
	public BluetoothDevice getBluetoothDevice() {
		return m_device;
	}

	public void setBluetoothDevice(BluetoothDevice device) {
		m_device = device;
	}
	
	public boolean isConnected() {
		return m_connected;
	}
	
    public boolean connect() {
        m_bluetoothGatt = m_device.getBluetoothGatt();
        boolean connected = m_bluetoothGatt.connect();
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

	// ---------------------------------------------------------------------------------------------
	//
	//  Temperature Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
	//
	// ---------------------------------------------------------------------------------------------
	/*
	 * Enable temperature sensor
	 */
	public void enableTemperatureSensor(boolean cc2650) {
		// Write "01" to 0x29 to enable temperature sensor
		if(cc2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "01");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "01");
	}
	
	/*
	 * Disable temperature sensor
	 */
	public void disableTemperatureSensor(boolean cc2650) {
		// Write "00" to 0x29 to enable temperature sensor
		if(cc2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "00");
	}
	
	/*
	 * Read temperature sensor
	 */
	public String readTemperature(String handleValue) {
		// Read value from handle 0x25
		return m_bluetoothGatt.readCharacteristicValue(handleValue);
	}
	
	/*
	 * Read temperature sensor by UUID
	 */
	public String readTemperatureByUuid(UUID uuid) {
		return m_bluetoothGatt.readCharacteristicValueByUuid(uuid);
	}
	
	/*
	 * Enable temperature notifications
	 */
	public void enableTemperatureNotifications(boolean cc2650) {
		//Write "01:00 to 0x26 to enable notifications
		if(cc2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650, "01:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541, "01:00");
	}
	/*
	 * Disable temperature notifications
	 */
	public void disableTemperatureNotifications(boolean cc2650) {
		//Write "00:00 to 0x26 to enable notifications
		if(cc2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650, "00:00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541, "00:00");
	}
	
	/*
	 * Calculate temperature
	 */
	private double calculateTemperature(double obj, double amb) {
		double Vobj2 = obj;
	    Vobj2 *= 0.00000015625;

	    double Tdie = (amb / 128.0) + 273.15;

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

	    return tObj - 273.15;
	}

	// -----------------------------------------------------------------------------------------------
	//
	//  Accelerometer Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
	//
	// -----------------------------------------------------------------------------------------------
	/*
	 * Enable accelerometer sensor
	 */
	public void enableAccelerometerSensor(String range) {
		// Write "01" to 0x31 in order to select 2g range, "02" for 4g, "03" for 8g
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_ENABLE, range);
	}
	
	/*
	 * Disable accelerometer sensor
	 */
	public void disableAccelerometerSensor() {
		// Write "00" to 0x31 to disable accelerometer sensor
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_ENABLE, "00");
	}
	
	/*
	 * Read accelerometer sensor
	 */
	public String readAccelerometer(String handleValue) {
		// Read value from handle 0x2D
		return m_bluetoothGatt.readCharacteristicValue(handleValue);
	}
	
	/*
	 * Read accelerometer sensor by UUID
	 */
	public String readAccelerometerByUuid(UUID uuid) {
		return m_bluetoothGatt.readCharacteristicValueByUuid(uuid);
	}
	
	/*
	 * Enable accelerometer notifications
	 */
	public void enableAccelerometerNotifications() {
		//Write "01:00 to 0x2E to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_NOTIFICATION, "01:00");
	}
	/*
	 * Disable accelerometer notifications
	 */
	public void disableAccelerometerNotifications() {
		//Write "00:00 to 0x2E to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_ACC_SENSOR_NOTIFICATION, "00:00");
	}
	
	// ------------------------------------------------------------------------------------------
	//
	//  Humidity Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
	//
	// ------------------------------------------------------------------------------------------
	/*
	 * Enable humidity sensor
	 */
	public void enableHumiditySensor(boolean cc2650) {
		// Write "01" to 0x3c enable humidity sensor
		if(cc2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2650, "01");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2541, "01");
	}
	
	/*
	 * Disable humidity sensor
	 */
	public void disableHumiditySensor(boolean cc2650) {
		// Write "00" to 0x3c to disable humidity sensor
		if(cc2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2650, "00");
		else
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_ENABLE_2541, "00");
	}
	
	/*
	 * Read humidity sensor
	 */
	public String readHumidity(String handleValue) {
		// Read value from handle 0x38
		return m_bluetoothGatt.readCharacteristicValue(handleValue);
	}
	
	/*
	 * Read humidity sensor by UUID
	 */
	public String readHumidityByUuid(UUID uuid) {
		return m_bluetoothGatt.readCharacteristicValueByUuid(uuid);
	}
	
	/*
	 * Enable humidity notifications
	 */
	public void enableHumidityNotifications(boolean cc2650) {
		//Write "01:00 to 0x39 to enable notifications
		if(cc2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2650, "01:00");
		else 
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2541, "01:00");
	}
	/*
	 * Disable humidity notifications
	 */
	public void disableHumidityNotifications(boolean cc2650) {
		//Write "00:00 to 0x39 to enable notifications
		if(cc2650)
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2650, "00:00");
		else 
			m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_HUM_SENSOR_NOTIFICATION_2541, "00:00");
	}
	
	// ------------------------------------------------------------------------------------------
	//
	//  Magnetometer Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
	//
	// ------------------------------------------------------------------------------------------
	/*
	 * Enable magnetometer sensor
	 */
	public void enableMagnetometerSensor() {
		// Write "01" to 0x44 enable magnetometer sensor
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_ENABLE, "01");
	}
	
	/*
	 * Disable magnetometer sensor
	 */
	public void disableMagnetometerSensor() {
		// Write "00" to 0x44 to disable magnetometer sensor
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_ENABLE, "00");
	}
	
	/*
	 * Read magnetometer sensor
	 */
	public String readMagnetometer(String handleValue) {
		// Read value from handle 0x40
		return m_bluetoothGatt.readCharacteristicValue(handleValue);
	}
	
	/*
	 * Read magnetometer sensor by UUID
	 */
	public String readMagnetometerByUuid(UUID uuid) {
		return m_bluetoothGatt.readCharacteristicValueByUuid(uuid);
	}
	
	/*
	 * Enable magnetometer notifications
	 */
	public void enableMagnetometerNotifications() {
		//Write "01:00 to 0x41 to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_NOTIFICATION, "01:00");
	}
	/*
	 * Disable magnetometer notifications
	 */
	public void disableMagnetometerNotifications() {
		//Write "00:00 to 0x41 to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_MAG_SENSOR_NOTIFICATION, "00:00");
	}
	
	// ------------------------------------------------------------------------------------------
	//
	//  Barometric Pressure Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
	//
	// ------------------------------------------------------------------------------------------
	/*
	 * Enable pressure sensor
	 */
	public void enablePressureSensor() {
		// Write "01" to 0x4f enable pressure sensor
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE, "01");
	}
	
	/*
	 * Disable pressure sensor
	 */
	public void disablePressureSensor() {
		// Write "00" to 0x4f to disable pressure sensor
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE, "00");
	}
	
	/*
	 * Calibrate pressure sensor
	 */
	public void calibratePressureSensor() {
		// Write "02" to 0x4f to calibrate pressure sensor
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_ENABLE, "02");
	}
	
	/*
	 * Read calibration pressure sensor
	 */
	public String readCalibrationPressureSensor(String handleValue) {
		// Read value from 0x52
		pressureCalibration = m_bluetoothGatt.readCharacteristicValue(handleValue); 
		return pressureCalibration;
	}
	
	/*
	 * Read pressure sensor
	 */
	public String readPressure(String handleValue) {
		// Read value from handle 0x40
		return m_bluetoothGatt.readCharacteristicValue(handleValue);
	}
	
	/*
	 * Read pressure sensor by UUID
	 */
	public String readPressureByUuid(UUID uuid) {
		return m_bluetoothGatt.readCharacteristicValueByUuid(uuid);
	}
	
	/*
	 * Enable pressure notifications
	 */
	public void enablePressureNotifications() {
		//Write "01:00 to 0x4c to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION, "01:00");
	}
	/*
	 * Disable pressure notifications
	 */
	public void disablePressureNotifications() {
		//Write "00:00 to 0x4c to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_PRE_SENSOR_NOTIFICATION, "00:00");
	}
	
	/*
	 * Calculate pressure
	 */
	private double calculatePre(int t_r, int p_r, int[] c) {

		// Ignore temperature from pressure sensor
		// double t_a = (100 * (c[0] * t_r / Math.pow(2,8) + c[1] * Math.pow(2,6))) / Math.pow(2,16);
	    double S = c[2] + c[3] * t_r / Math.pow(2,17) + ((c[4] * t_r / Math.pow(2,15)) * t_r) / Math.pow(2,19);
	    double O = c[5] * Math.pow(2,14) + c[6] * t_r / Math.pow(2,3) + ((c[7] * t_r / Math.pow(2,15)) * t_r) / Math.pow(2,4);
	    double p_a = (S * p_r + O) / Math.pow(2,14);
		
	    return p_a;
	}

	// ------------------------------------------------------------------------------------------
	//
	//  Gyroscope Sensor, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
	//
	// ------------------------------------------------------------------------------------------
	/*
	 * Enable gyroscope sensor
	 */
	public void enableGyroscopeSensor(String enable) {
		// Write "00" to turn off gyroscope, "01" to enable X axis only, "02" to enable Y axis only, 
		// "03" = X and Y, "04" = Z only, "05" = X and Z, "06" = Y and Z and "07" = X, Y and Z.  
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_GYR_SENSOR_ENABLE, enable);
	}
	
	/*
	 * Disable gyroscope sensor
	 */
	public void disableGyroscopeSensor() {
		// Write "00" to 0x5b to disable gyroscope sensor
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_GYR_SENSOR_ENABLE, "00");
	}
	
	/*
	 * Read gyroscope sensor
	 */
	public String readGyroscope(String handleValue) {
		// Read value from handle 0x57
		return m_bluetoothGatt.readCharacteristicValue(handleValue);
	}
	
	/*
	 * Read gyroscope sensor by UUID
	 */
	public String readGyroscopeByUuid(UUID uuid) {
		return m_bluetoothGatt.readCharacteristicValueByUuid(uuid);
	}
	
	/*
	 * Enable gyroscope notifications
	 */
	public void enableGyroscopeNotifications() {
		//Write "01:00 to 0x58 to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_GYR_SENSOR_NOTIFICATION, "01:00");
	}
	/*
	 * Disable gyroscope notifications
	 */
	public void disableGyroscopeNotifications() {
		//Write "00:00 to 0x58 to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_GYR_SENSOR_NOTIFICATION, "00:00");
	}

	// ------------------------------------------------------------------------------------------
	//
	//  Keys, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
	//
	// ------------------------------------------------------------------------------------------
	/*
	 * Read keys status
	 */
	public String readKeysStatus(String handleValue) {
		// Read value from handle 0x5f
		return m_bluetoothGatt.readCharacteristicValue(handleValue);
	}
	
	/*
	 * Read keys status by UUID
	 */
	public String readKeysStatusByUuid(UUID uuid) {
		return m_bluetoothGatt.readCharacteristicValueByUuid(uuid);
	}
	
	/*
	 * Enable keys notification
	 */
	public void enableKeysNotification() {
		//Write "01:00 to 0x60 to enable keys
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION, "01:00");
	}
	/*
	 * Disable gyroscope notifications
	 */
	public void disableKeysNotifications() {
		//Write "00:00 to 0x60 to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_KEYS_NOTIFICATION, "00:00");
	}
	
	// ---------------------------------------------------------------------------------------------
	//
	//  BluetoothLeNotificationListener API
	//
	// ---------------------------------------------------------------------------------------------
	@Override
	public void onDataReceived(String handle, String value) {
		
		String[] tmp = null;
		
		//s_logger.info("handle: " + handle + " value: " + value);
		if (handle.equals(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2650) || handle.equals(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2541)) {
			//s_logger.info("Received temp value: " + value);
			tmp = value.split("\\s");
			int lsbObj = Integer.parseInt(tmp[0], 16);
			int msbObj = Integer.parseInt(tmp[1], 16);
			int lsbAmb = Integer.parseInt(tmp[2], 16);
			int msbAmb = Integer.parseInt(tmp[3], 16);
			
			int objT = (unsignedToSigned(msbObj) << 8) + lsbObj;
			int ambT = (msbAmb << 8) + lsbAmb;
			
			double ambient = ambT / 128.0;
			double target = calculateTemperature((double)objT, (double)ambT);
			
			s_logger.info("Received temp value: Ambient: " + ambient + " Target: " + target);
			temperatureReceived = true;
			tempAmbient = ambient;
			tempTarget = target;
		} 
		else if (handle.equals(TiSensorTagGatt.HANDLE_ACC_SENSOR_VALUE)) {
			s_logger.info("Received acc value: " + value);
			tmp = value.split("\\s");
			int x = Integer.parseInt(tmp[0], 16);
			int y = Integer.parseInt(tmp[1], 16);
			int z = Integer.parseInt(tmp[2], 16) * -1;
			
			double xd = x / 64.0;
			double yd = y / 64.0;
			double zd = z / 64.0;
			
			s_logger.info("X acc: " + xd + " Y acc: " + yd + " Z acc: " + zd);
			//BluetoothLe.doPublishAcc(m_device.getAdress(), xd, yd, zd);
		}
		else if (handle.equals(TiSensorTagGatt.HANDLE_HUM_SENSOR_VALUE_2650)||handle.equals(TiSensorTagGatt.HANDLE_HUM_SENSOR_VALUE_2541)) {
			s_logger.info("Received hum value: " + value);
			tmp = value.split("\\s");
			// Ignore temperature value from humidity sensor
			int lsbHum = Integer.parseInt(tmp[2], 16);
			int msbHum = Integer.parseInt(tmp[3], 16);
			
			int hum = (msbHum << 8) + lsbHum;
			hum = hum - (hum % 4);
			float humf = (-6f) + 125f * (hum / 65535f);
			
			s_logger.info("Hum: " + humf);
		}
		else if (handle.equals(TiSensorTagGatt.HANDLE_MAG_SENSOR_VALUE)) {
			s_logger.info("Received mag value: " + value);
			tmp = value.split("\\s");
			int msbX = Integer.parseInt(tmp[0], 16);
			int lsbX = Integer.parseInt(tmp[1], 16);
			int msbY = Integer.parseInt(tmp[2], 16);
			int lsbY = Integer.parseInt(tmp[3], 16);
			int msbZ = Integer.parseInt(tmp[4], 16);
			int lsbZ = Integer.parseInt(tmp[5], 16);
			
			int x = (unsignedToSigned(msbX) << 8) + lsbX;
			int y = (unsignedToSigned(msbY) << 8) + lsbY;
			int z = (unsignedToSigned(msbZ) << 8) + lsbZ;
			
			float xf = x * (2000f / 65536f) * -1;
			float yf = y * (2000f / 65536f) * -1;
			float zf = z * (2000f / 65536f);
			
			s_logger.info("X mag: " + xf + " Y mag: " + yf + " Z mag: " + zf);
			//BluetoothLe.doPublishMag(m_device.getAdress(), xf, yf, zf);
		}
		else if (handle.equals(TiSensorTagGatt.HANDLE_PRE_SENSOR_VALUE)) {
			s_logger.info("Received pre value: " + value);
			tmp = value.split("\\s");
			int lsbTemp = Integer.parseInt(tmp[0], 16);
			int msbTemp = Integer.parseInt(tmp[1], 16);
			int lsbPre = Integer.parseInt(tmp[2], 16);
			int msbPre = Integer.parseInt(tmp[3], 16);

			int t_r = (unsignedToSigned(msbTemp) << 8) + lsbTemp;
			int p_r = (msbPre << 8) + lsbPre;
			
			tmp = pressureCalibration.split("\\s");
			int lsbc1 = Integer.parseInt(tmp[0], 16);
			int msbc1 = Integer.parseInt(tmp[1], 16);
			int lsbc2 = Integer.parseInt(tmp[2], 16);
			int msbc2 = Integer.parseInt(tmp[3], 16);
			int lsbc3 = Integer.parseInt(tmp[4], 16);
			int msbc3 = Integer.parseInt(tmp[5], 16);
			int lsbc4 = Integer.parseInt(tmp[6], 16);
			int msbc4 = Integer.parseInt(tmp[7], 16);
			int lsbc5 = Integer.parseInt(tmp[8], 16);
			int msbc5 = Integer.parseInt(tmp[9], 16);
			int lsbc6 = Integer.parseInt(tmp[10], 16);
			int msbc6 = Integer.parseInt(tmp[11], 16);
			int lsbc7 = Integer.parseInt(tmp[12], 16);
			int msbc7 = Integer.parseInt(tmp[13], 16);
			int lsbc8 = Integer.parseInt(tmp[14], 16);
			int msbc8 = Integer.parseInt(tmp[15], 16);
			
			int c[] = new int[8];
			c[0] = (msbc1 << 8) + lsbc1;
			c[1] = (msbc2 << 8) + lsbc2;
			c[2] = (msbc3 << 8) + lsbc3;
			c[3] = (msbc4 << 8) + lsbc4;
			c[4] = (unsignedToSigned(msbc5) << 8) + lsbc5;
			c[5] = (unsignedToSigned(msbc6) << 8) + lsbc6;
			c[6] = (unsignedToSigned(msbc7) << 8) + lsbc7;
			c[7] = (unsignedToSigned(msbc8) << 8) + lsbc8;
			
			double p_a = calculatePre(t_r, p_r, c);
			s_logger.info("Pre: " + p_a);
			//BluetoothLe.doPublishPre(m_device.getAdress(), p_a);
		}
		else if (handle.equals(TiSensorTagGatt.HANDLE_GYR_SENSOR_VALUE)) {
			s_logger.info("Received gyr value: " + value);
			tmp = value.split("\\s");
			int msbX = Integer.parseInt(tmp[0], 16);
			int lsbX = Integer.parseInt(tmp[1], 16);
			int msbY = Integer.parseInt(tmp[2], 16);
			int lsbY = Integer.parseInt(tmp[3], 16);
			int msbZ = Integer.parseInt(tmp[4], 16);
			int lsbZ = Integer.parseInt(tmp[5], 16);
			
			int x = (unsignedToSigned(msbX) << 8) + lsbX;
			int y = (unsignedToSigned(msbY) << 8) + lsbY;
			int z = (unsignedToSigned(msbZ) << 8) + lsbZ;
			
			float xf = x * (500f / 65536f);
			float yf = y * (500f / 65536f) * -1;
			float zf = z * (500f / 65536f);
			
			s_logger.info("X gyr: " + xf + " Y gyr: " + yf + " Z gyr: " + zf);
			//BluetoothLe.doPublishGyr(m_device.getAdress(), xf, yf, zf);
		}
		else if (handle.equals(TiSensorTagGatt.HANDLE_KEYS_STATUS)) {
			s_logger.info("Received keys value: " + value);
//			if (!value.equals("00"))
//				BluetoothLe.doPublishKeys(m_device.getAdress(), Integer.parseInt(value) );
		}
	}
	
	// ---------------------------------------------------------------------------------------------
	//
	//  Auxiliary methods
	//
	// ---------------------------------------------------------------------------------------------
	private int unsignedToSigned(int unsigned) {
		if ((unsigned & (1 << 8-1)) != 0) {
            unsigned = -1 * ((1 << 8-1) - (unsigned & ((1 << 8-1) - 1)));
        }
        return unsigned;
	}

	public boolean isTemperatureReceived() {
		return temperatureReceived;
	}

	public void setTemperatureReceived(boolean temperatureReceived) {
		this.temperatureReceived = temperatureReceived;
	}

	public double getTempAmbient() {
		return tempAmbient;
	}

	public double getTempTarget() {
		return tempTarget;
	}
}
