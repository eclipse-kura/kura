package org.eclipse.kura.example.bluetooth;

import java.util.ArrayList;
import java.util.Date;
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

	private CloudService                m_cloudService;
	private static CloudClient          m_cloudClient;
	private List<TiSensorTag>           m_tiSensorTagList;
	private BluetoothService            m_bluetoothService;
	private BluetoothAdapter            m_bluetoothAdapter;
	private List<BluetoothGattService>  m_bluetoothGattServices;
	private ScheduledExecutorService    m_worker;
	private ScheduledFuture<?>          m_handle;

	private String PROPERTY_PERIOD = "period";

	public BluetoothLe() {
		super();
		m_tiSensorTagList = new ArrayList<TiSensorTag>();
		m_worker = Executors.newSingleThreadScheduledExecutor();
	}

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

		try {
			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
			m_cloudClient.addCloudClientListener(this);

			doUpdate(properties);
		} catch (Exception e) {
			s_logger.error("Error starting component", e);
			throw new ComponentException(e);
		}

	}

	protected void deactivate(ComponentContext context) {

		s_logger.debug("Deactivating BluetoothLe...");

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

		s_logger.debug("Updating Bluetooth Service...");
		doUpdate(properties);
		s_logger.debug("Updating Bluetooth Service... Done.");
	}

	// --------------------------------------------------------------------
	//
	//  Static Methods
	//
	// --------------------------------------------------------------------

	protected static void doPublishTemp(String address, Object ambValue, Object targetValue) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric("Ambient", ambValue);
		payload.addMetric("Target", targetValue);
		try {
			m_cloudClient.publish(address + "/temperature", payload, 0, false);
		} catch (Exception e) {
			s_logger.error("Can't publish message, " + "temperature", e);
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


	private void doUpdate(Map<String,Object> properties) {

		// disconnect SensorTags
		for (TiSensorTag tiSensorTag : m_tiSensorTagList) {
			if (tiSensorTag != null) {
				tiSensorTag.disconnect();
			}
		}
		// cancel a current worker handle if one if active
		if (m_handle != null) {
			m_handle.cancel(true);
		}

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

			// schedule a new worker based on the properties of the service
			int pubrate = (Integer) properties.get(PROPERTY_PERIOD);
			m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
				@Override
				public void run() {

					startScan();
				}
			}, 0, pubrate, TimeUnit.SECONDS);

		}

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

	private void scheduleNotifications(TiSensorTag tiSensorTag) {
		s_logger.info("Starting sensor notifications...");
		try {

			// Calibrate pressure sensor
			tiSensorTag.calibratePressureSensor();
			String cal = tiSensorTag.readCalibrationPressureSensor(TiSensorTagGatt.HANDLE_PRE_CALIBRATION);

			// Enable notifications
			tiSensorTag.enableTemperatureNotifications();
			tiSensorTag.enableAccelerometerNotifications();
			tiSensorTag.enableHumidityNotifications();
			tiSensorTag.enableMagnetometerNotifications();
			tiSensorTag.enablePressureNotifications();
			tiSensorTag.enableGyroscopeNotifications();
			tiSensorTag.enableKeysNotification();
			// Enable sensors
			tiSensorTag.enableTemperatureSensor();
			tiSensorTag.enableAccelerometerSensor("01");
			tiSensorTag.enableHumiditySensor();
			tiSensorTag.enableMagnetometerSensor();
			tiSensorTag.enablePressureSensor();
			tiSensorTag.enableGyroscopeSensor("07");
			// Wait for 5 second
			Thread.sleep(5000);
			// Disable notifications
			tiSensorTag.disableTemperatureNotifications();
			tiSensorTag.disableAccelerometerNotifications();
			tiSensorTag.disableHumidityNotifications();
			tiSensorTag.disableMagnetometerNotifications();
			tiSensorTag.disablePressureNotifications();
			tiSensorTag.disableGyroscopeNotifications();
			tiSensorTag.disableKeysNotifications();
		} catch (InterruptedException e) {
			s_logger.error("Error during sensor notifications: " + e.getLocalizedMessage());
		}


	}

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
			if (bluetoothDevice.getName().equals("SensorTag")) {
				s_logger.info("TI SensorTag " + bluetoothDevice.getAdress() + " found.");
				if (!searchSensorTagList(bluetoothDevice.getAdress()))
					m_tiSensorTagList.add(new TiSensorTag(bluetoothDevice));
			}
			else { 
				s_logger.info("Found device = " + bluetoothDevice.getAdress());
			}
		}
		if (m_tiSensorTagList.size() > 0) {
			for (TiSensorTag tiSensorTag : m_tiSensorTagList) {
				boolean connected = tiSensorTag.connect();
				if (connected) {
					s_logger.info("Connected to TI SensorTag " + tiSensorTag.getBluetoothDevice().getAdress() + ".");
					// Once connected, run through all diagnostics or comment
					doServicesDiscovery(tiSensorTag);
//					doCharacteristicsDiscovery(tiSensorTag);
					scheduleNotifications(tiSensorTag);

					tiSensorTag.disconnect();
				}
				else {
					s_logger.info("Cannot connect to TI SensorTag " + tiSensorTag.getBluetoothDevice().getAdress() + ".");
				}
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
