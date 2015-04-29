package org.eclipse.kura.example.bluetooth;

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
	
	public static final String ADDRESS = "68:C9:0B:06:54:82"; // TI SensorTag
//	public static final String ADDRESS = "00:22:D0:4F:4E:1C"; // Polar Loop
	
	private BluetoothGatt m_bluetoothGatt;
	private BluetoothDevice m_device;
	
	public TiSensorTag(BluetoothDevice bluetoothDevice) {
		m_device = bluetoothDevice;
	}
	
	public boolean connect() {
		m_bluetoothGatt = m_device.getBluetoothGatt();
		boolean connected = m_bluetoothGatt.connect();
		if(connected) {
			m_bluetoothGatt.setBluetoothLeNotificationListener(this);
			return true;
		}
		else {
			return false;
		}
	}
	
	public void disconnect() {
		if (m_bluetoothGatt != null)
			m_bluetoothGatt.disconnect();
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

	// --------------------------------------------------------------------
	//
	//  Temperature Sensor
	//
	// --------------------------------------------------------------------
	/*
	 * Enable temperature sensor
	 */
	public void enableTempSensor() {
		// Write "01" to 0x29 to enable temperature sensor
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE, "01");
	}
	
	/*
	 * Disable temperature sensor
	 */
	public void disableTempSensor() {
		// Write "00" to 0x29 to enable temperature sensor
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE, "00");
	}
	
	/*
	 * Read temperature sensor
	 */
	public String readTemp(String handleValue) {
		// Read value from handle 0x25
		//return m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE);
		return m_bluetoothGatt.readCharacteristicValue(handleValue);
	}
	
	/*
	 * Read temperature sensor by UUID
	 */
	public String readTempByUuid(UUID uuid) {
		return m_bluetoothGatt.readCharacteristicValueByUuid(uuid);
	}
	
	/*
	 * Enable temperature notifications
	 */
	public void enableTempNotifications() {
		//Write "01:00 to 0x26 to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION, "01:00");
	}
	/*
	 * Disable temperature notifications
	 */
	public void disableTempNotifications() {
		//Write "00:00 to 0x26 to enable notifications
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION, "00:00");
	}
	
	/*
	 *Calculate temperature, reference: http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
	 */
	private int unsignedToSigned(int unsigned) {
		if ((unsigned & (1 << 8-1)) != 0) {
            unsigned = -1 * ((1 << 8-1) - (unsigned & ((1 << 8-1) - 1)));
        }
        return unsigned;
	}
	
	private double calculateTemp(double obj, double amb) {
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

	@Override
	public void onDataReceived(String handle, String value) {
		s_logger.info("handle: " + handle + " value: " + value);
		if (handle.equals(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE)) {
			s_logger.info("Received temp value: " + value);
			String[] tmp = value.split("\\s");
			int lsbObj = Integer.parseInt(tmp[0], 16);
			int msbObj = Integer.parseInt(tmp[1], 16);
			int lsbAmb = Integer.parseInt(tmp[2], 16);
			int msbAmb = Integer.parseInt(tmp[3], 16);
			
			int objT = (unsignedToSigned(msbObj) << 8) + lsbObj;
			int ambT = (msbAmb << 8) + lsbAmb;
			
			double ambient = ambT / 128.0;
			double target = calculateTemp((double)objT, (double)ambT);
			
			s_logger.info("Ambient: " + ambient + " Target: " + target);
			BluetoothLe.doPublishTemp(ambient, target);
		}
		
	}














}
