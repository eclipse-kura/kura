package org.eclipse.kura.example.ble.tisensortag;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;
import org.eclipse.kura.bluetooth.BluetoothGattService;
import org.eclipse.kura.bluetooth.BluetoothLeScanListener;
import org.eclipse.kura.bluetooth.BluetoothService;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLe implements ConfigurableComponent, CloudClientListener, BluetoothLeScanListener{

	private static final Logger s_logger = LoggerFactory.getLogger(BluetoothLe.class);

	private final String APP_ID = "BLE_APP_V1";
	private String PROPERTY_SCANTIME = "scan_time";
	private String PROPERTY_CC2650 = "cc2650";
	private String PROPERTY_PERIOD = "period";
	private String PROPERTY_TOPIC = "publishTopic";

	private CloudService                m_cloudService;
	private static CloudClient          m_cloudClient;
	private List<TiSensorTag>           m_tiSensorTagList;
	private BluetoothService            m_bluetoothService;
	private BluetoothAdapter            m_bluetoothAdapter;
	private List<BluetoothGattService>  m_bluetoothGattServices;
	private ScheduledExecutorService    m_worker;
	private ScheduledFuture<?>          m_handle;
	private boolean m_found = false;
	private TiSensorTag myTiSensorTag = null;
	
	private int m_pubrate = 10;
	private int m_scantime = 5;
	private String m_topic = null;
	private int m_workerCount = 0;
	private long m_startTime = 0;
	private boolean m_connected = false;
	private boolean m_cc2650 = true;


	public void setCloudService(CloudService cloudService) {
		m_cloudService = cloudService;
	}

	public void unsetCloudService(CloudService cloudService) {
		m_cloudService = null;
	}

	public void setBluetoothService(BluetoothService bluetoothService) {
		m_bluetoothService = bluetoothService;
	}

	public void unsetBluetoothService(BluetoothService bluetoothService) {
		m_bluetoothService = null;
	}

	// --------------------------------------------------------------------
	//
	//  Activation APIs
	//
	// --------------------------------------------------------------------
	protected void activate(ComponentContext context, Map<String,Object> properties) {
		s_logger.info("Activating BluetoothLe example...");
		
		if(properties!=null){
			if(properties.get(PROPERTY_CC2650)!=null)
				m_cc2650 = (Boolean) properties.get(PROPERTY_CC2650);
			if(properties.get(PROPERTY_SCANTIME)!=null)
				m_scantime = (Integer) properties.get(PROPERTY_SCANTIME);
			if(properties.get(PROPERTY_PERIOD)!=null)
				m_pubrate = (Integer) properties.get(PROPERTY_PERIOD);
			if(properties.get(PROPERTY_TOPIC)!=null)
				m_topic = (String) properties.get(PROPERTY_TOPIC);
		}
		
		m_tiSensorTagList = new ArrayList<TiSensorTag>();
		m_worker = Executors.newSingleThreadScheduledExecutor();

		try {
			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
			m_cloudClient.addCloudClientListener(this);

			// Get Bluetooth adapter and ensure it is enabled
			m_bluetoothAdapter = m_bluetoothService.getBluetoothAdapter();
			if (m_bluetoothAdapter != null) {
				s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
				s_logger.info("Bluetooth adapter le enabled => " + m_bluetoothAdapter.isLeReady());

				if (!m_bluetoothAdapter.isEnabled()) {
					s_logger.info("Enabling bluetooth adapter...");
					m_bluetoothAdapter.enable();
					s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
				}
				m_workerCount = 0;
				m_found = false;
				m_connected = false;
				m_startTime = 0;
				m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
					@Override
					public void run() {
						updateSensors();
					}
				}, 0, 1, TimeUnit.SECONDS);
			}
			else s_logger.warn("No Bluetooth adapter found ...");
		} catch (Exception e) {
			s_logger.error("Error starting component", e);
			throw new ComponentException(e);
		}

	}

	protected void deactivate(ComponentContext context) {

		s_logger.debug("Deactivating BluetoothLe...");
		if(m_bluetoothAdapter.isScanning()){
			s_logger.debug("m_bluetoothAdapter.isScanning");
			m_bluetoothAdapter.killLeScan();
		}

		// disconnect SensorTags
		for (TiSensorTag tiSensorTag : m_tiSensorTagList) {
			if (tiSensorTag != null) {
				tiSensorTag.disconnect();
			}
		}
		m_tiSensorTagList.clear();

		// cancel a current worker handle if one if active
		if (m_handle != null) {
			m_handle.cancel(true);
		}

		// shutting down the worker and cleaning up the properties
		m_worker.shutdown();

		// Releasing the CloudApplicationClient
		s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
		m_cloudClient.release();

		s_logger.debug("Deactivating BluetoothLe... Done.");
	}

	protected void updated(Map<String,Object> properties) {

		if(properties!=null){
			if(properties.get(PROPERTY_CC2650)!=null)
				m_cc2650 = (Boolean) properties.get(PROPERTY_CC2650);
			if(properties.get(PROPERTY_SCANTIME)!=null)
				m_scantime = (Integer) properties.get(PROPERTY_SCANTIME);
			if(properties.get(PROPERTY_PERIOD)!=null)
				m_pubrate = (Integer) properties.get(PROPERTY_PERIOD);
			if(properties.get(PROPERTY_TOPIC)!=null)
				m_topic = (String) properties.get(PROPERTY_TOPIC);
		}

		s_logger.debug("Updating Bluetooth Service... Done.");
	}

	// --------------------------------------------------------------------
	//
	//  Main task executed every second
	//
	// --------------------------------------------------------------------

	void updateSensors() {

		// Scan
		if(!m_found){
			if(m_bluetoothAdapter.isScanning()){
				s_logger.info("m_bluetoothAdapter.isScanning");
				if((System.currentTimeMillis() - m_startTime) >= (m_scantime*1000)){
					m_bluetoothAdapter.killLeScan();
				}
			}
			else{
				s_logger.info("startLeScan");
				m_bluetoothAdapter.startLeScan(this);
				m_startTime = System.currentTimeMillis();
			}
		}
		else if(m_bluetoothAdapter.isScanning()){
			m_bluetoothAdapter.killLeScan();			
		}
		
		// connect SensorTag
		if(m_found){
			if(!m_connected){
				s_logger.info("Found, connecting...");
				m_connected = myTiSensorTag.connect();
				if(m_connected){
					doServicesDiscovery(myTiSensorTag);
					doCharacteristicsDiscovery(myTiSensorTag);
					myTiSensorTag.enableTemperatureSensor(m_cc2650);
					myTiSensorTag.enableHumiditySensor(m_cc2650);
					myTiSensorTag.enableTemperatureNotifications(m_cc2650);
					myTiSensorTag.enableHumidityNotifications(m_cc2650);
				}
				else {
					s_logger.info("Cannot connect to TI SensorTag " + myTiSensorTag.getBluetoothDevice().getAdress() + ".");
				}
			}

			// Temperature
			if(myTiSensorTag.isTemperatureReceived()){
				if(m_workerCount==m_pubrate)
					doPublishTemp(myTiSensorTag.getBluetoothDevice().getAdress(), myTiSensorTag.getTempAmbient(), myTiSensorTag.getTempTarget());
				myTiSensorTag.setTemperatureReceived(false);
//				myTiSensorTag.disableTemperatureNotifications(m_cc2650);
//				myTiSensorTag.disconnect();
			}
		}

		m_workerCount++;
		if(m_workerCount>m_pubrate){
			m_workerCount=0;
		}
	}
	
	private void doPublishTemp(String address, Object ambValue, Object targetValue) {
		if(m_topic!=null){
			KuraPayload payload = new KuraPayload();
			payload.setTimestamp(new Date());
			payload.addMetric("Ambient", ambValue);
			payload.addMetric("Target", targetValue);
			try {
				m_cloudClient.publish(m_topic+"/"+address + "/temperature", payload, 0, false);
			} catch (Exception e) {
				s_logger.error("Can't publish message, " + "temperature", e);
			}
		}
	}

	protected static void doPublishAcc(String address, Object x, Object y, Object z) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric("accX", x);
		payload.addMetric("accY", y);
		payload.addMetric("accZ", z);
		try {
			m_cloudClient.publish(address + "/accelerometer", payload, 0, false);
		} catch (Exception e) {
			s_logger.error("Can't publish message, " + "accelerometer", e);
		}

	}

	protected static void doPublishHum(String address, Object hum) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric("Humidity", hum);
		try {
			m_cloudClient.publish(address + "/humidity", payload, 0, false);
		} catch (Exception e) {
			s_logger.error("Can't publish message, " + "humidity", e);
		}

	}

	protected static void doPublishMag(String address, Object x, Object y, Object z) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric("magX", x);
		payload.addMetric("magY", y);
		payload.addMetric("magZ", z);
		try {
			m_cloudClient.publish(address + "/magnetometer", payload, 0, false);
		} catch (Exception e) {
			s_logger.error("Can't publish message, " + "magnetometer", e);
		}

	}

	protected static void doPublishPre(String address, Object pre) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric("pressure", pre);
		try {
			m_cloudClient.publish(address + "/pressure", payload, 0, false);
		} catch (Exception e) {
			s_logger.error("Can't publish message, " + "pressure", e);
		}

	}

	protected static void doPublishGyr(String address, Object x, Object y, Object z) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric("gyrX", x);
		payload.addMetric("gyrY", y);
		payload.addMetric("gyrZ", z);
		try {
			m_cloudClient.publish(address + "/gyroscope", payload, 0, false);
		} catch (Exception e) {
			s_logger.error("Can't publish message, " + "gyroscope", e);
		}

	}

	protected static void doPublishKeys(String address, Object key) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric("key", key);
		try {
			m_cloudClient.publish(address + "/keys", payload, 0, false);
		} catch (Exception e) {
			s_logger.error("Can't publish message, " + "keys", e);
		}

	}

	// --------------------------------------------------------------------
	//
	//  Private Methods
	//
	// --------------------------------------------------------------------



	private void readTemperature() {
		String value;
		
		s_logger.info("Read temperature...");
		// Enable temperature sensor
		myTiSensorTag.enableTemperatureSensor(m_cc2650);
		s_logger.info("enableTemperatureSensor ok");
		try {
			Thread.sleep(2000);
			// Read value from temp sensor
			s_logger.info("try to read ----------- ");
			if(m_cc2650)
				value = myTiSensorTag.readTemperature(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2650);
			else
				value = myTiSensorTag.readTemperature(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2541);
			s_logger.info("Temperature read from handle is: " + value);
			double temp = convTemp(value);
		} catch (InterruptedException e) {
			s_logger.error("Error in testReadWrite: " + e.getLocalizedMessage());
		}
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

	private double convTemp(String value) {
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

		return ambient;
	}

	private void doServicesDiscovery(TiSensorTag tiSensorTag) {
		s_logger.info("Starting services discovery...");
		m_bluetoothGattServices = tiSensorTag.discoverServices();
		for (BluetoothGattService bgs : m_bluetoothGattServices) {	
			s_logger.info("Service UUID: " + bgs.getUuid()+"  :  "+bgs.getStartHandle()+"  :  "+bgs.getEndHandle());
		}
	}

	private void doCharacteristicsDiscovery(TiSensorTag tiSensorTag) {
		List<BluetoothGattCharacteristic> lbgc = tiSensorTag.getCharacteristics("0x0001", "0x0100"); 
		for(BluetoothGattCharacteristic bgc:lbgc){
			s_logger.info("Characteristics uuid : "+bgc.getUuid()+" : "+bgc.getHandle()+" : "+bgc.getValueHandle());
			String ls = bgc.getUuid().toString();
			if(ls.startsWith("00002a00")){
				String value = tiSensorTag.readTemperature(bgc.getValueHandle());
				s_logger.info("rec  = "+value);
				String[] ts = value.split(" ");
				String fin = "";
				for(String lls:ts){
					int ic = Integer.parseInt(lls, 16);
					fin+= (char)ic;
				}
				s_logger.info("Device name: " + fin);
			}
		}

	}

//	private void scheduleNotifications(TiSensorTag tiSensorTag) {
//		s_logger.info("Starting sensor notifications...");
//		try {
//
//			// Calibrate pressure sensor
//			tiSensorTag.calibratePressureSensor();
//			String cal = tiSensorTag.readCalibrationPressureSensor(TiSensorTagGatt.HANDLE_PRE_CALIBRATION);
//
//			// Enable notifications
//			tiSensorTag.enableTemperatureNotifications();
//			tiSensorTag.enableAccelerometerNotifications();
//			tiSensorTag.enableHumidityNotifications();
//			tiSensorTag.enableMagnetometerNotifications();
//			tiSensorTag.enablePressureNotifications();
//			tiSensorTag.enableGyroscopeNotifications();
//			tiSensorTag.enableKeysNotification();
//			// Enable sensors
//			tiSensorTag.enableTemperatureSensor();
//			tiSensorTag.enableAccelerometerSensor("01");
//			tiSensorTag.enableHumiditySensor();
//			tiSensorTag.enableMagnetometerSensor();
//			tiSensorTag.enablePressureSensor();
//			tiSensorTag.enableGyroscopeSensor("07");
//			// Wait for 5 second
//			Thread.sleep(5000);
//			// Disable notifications
//			tiSensorTag.disableTemperatureNotifications();
//			tiSensorTag.disableAccelerometerNotifications();
//			tiSensorTag.disableHumidityNotifications();
//			tiSensorTag.disableMagnetometerNotifications();
//			tiSensorTag.disablePressureNotifications();
//			tiSensorTag.disableGyroscopeNotifications();
//			tiSensorTag.disableKeysNotifications();
//		} catch (InterruptedException e) {
//			s_logger.error("Error during sensor notifications: " + e.getLocalizedMessage());
//		}
//
//
//	}

	private void startScan() {
		s_logger.info("Starting LE scan...");
		m_bluetoothAdapter.startLeScan(this);
	}

	private boolean searchSensorTagList(String address) {

		for (TiSensorTag tiSensorTag : m_tiSensorTagList) {
			if (tiSensorTag.getBluetoothDevice().getAdress().equals(address))
				return true;
		}
		return false;
	}

	// --------------------------------------------------------------------
	//
	//  BluetoothLeScanListener APIs
	//
	// --------------------------------------------------------------------
	@Override
	public void onScanFailed(int errorCode) {
		s_logger.error("Error during scan");

	}

	@Override
	public void onScanResults(List<BluetoothDevice> scanResults) {
		// Scan for TI SensorTag
		for (BluetoothDevice bluetoothDevice : scanResults) {
			s_logger.info("Address " + bluetoothDevice.getAdress() + " Name " + bluetoothDevice.getName());
			if (bluetoothDevice.getName().contains("SensorTag")) {
				s_logger.info("TI SensorTag " + bluetoothDevice.getAdress() + " found.");
				if (!searchSensorTagList(bluetoothDevice.getAdress())){
					myTiSensorTag = new TiSensorTag(bluetoothDevice);
					m_tiSensorTagList.add(myTiSensorTag);
					m_found = true;
				}
			}
			else { 
				s_logger.info("Found device = " + bluetoothDevice.getAdress());
			}
		}
	}

	// --------------------------------------------------------------------
	//
	//  CloudClientListener APIs
	//
	// --------------------------------------------------------------------
	@Override
	public void onControlMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {

	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {

	}

	@Override
	public void onConnectionLost() {

	}

	@Override
	public void onConnectionEstablished() {

	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {

	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {

	}

}
