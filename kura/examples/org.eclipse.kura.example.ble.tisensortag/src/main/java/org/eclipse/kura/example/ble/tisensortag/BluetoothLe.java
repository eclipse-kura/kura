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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
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

	private final String APP_ID      = "BLE_APP_V1";
	private String PROPERTY_SCANTIME = "scan_time";
	private String PROPERTY_PERIOD   = "period";
	private String PROPERTY_TEMP     = "enableTermometer";
	private String PROPERTY_ACC      = "enableAccelerometer";
	private String PROPERTY_HUM      = "enableHygrometer";
	private String PROPERTY_MAG      = "enableMagnetometer";
	private String PROPERTY_PRES     = "enableBarometer";
	private String PROPERTY_GYRO     = "enableGyroscope";
	private String PROPERTY_OPTO     = "enableLuxometer";
	private String PROPERTY_BUTTONS  = "enableButtons";
	private String PROPERTY_TOPIC    = "publishTopic";
	private String PROPERTY_INAME    = "iname";

	private CloudService                m_cloudService;
	private static CloudClient          m_cloudClient;
	private List<TiSensorTag>           m_tiSensorTagList;
	private BluetoothService            m_bluetoothService;
	private BluetoothAdapter            m_bluetoothAdapter;
	private List<BluetoothGattService>  m_bluetoothGattServices;
	private ScheduledExecutorService    m_worker;
	private ScheduledFuture<?>          m_handle;
	
	private int     m_period       = 10;
	private int     m_scantime     = 5;
	private static String  m_topic = "data";
	private long    m_startTime;
	private boolean m_connected    = false;
	private String  iname          = "hci0";
	private boolean enableTemp     = false;
	private boolean enableAcc      = false;
	private boolean enableHum      = false;
	private boolean enableMag      = false;
	private boolean enablePres     = false;
	private boolean enableGyro     = false;
	private boolean enableOpto     = false;
	private boolean enableButtons  = false;

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
		
		if(properties != null){
			if (properties.get(PROPERTY_SCANTIME) != null)
				m_scantime = (Integer) properties.get(PROPERTY_SCANTIME);
			if (properties.get(PROPERTY_PERIOD) != null)
				m_period = (Integer) properties.get(PROPERTY_PERIOD);
			if (properties.get(PROPERTY_TEMP) != null)
				enableTemp = (Boolean) properties.get(PROPERTY_TEMP);
			if (properties.get(PROPERTY_ACC) != null)
				enableAcc = (Boolean) properties.get(PROPERTY_ACC);
			if (properties.get(PROPERTY_HUM) != null)
				enableHum = (Boolean) properties.get(PROPERTY_HUM);
			if (properties.get(PROPERTY_MAG) != null)
				enableMag = (Boolean) properties.get(PROPERTY_MAG);
			if (properties.get(PROPERTY_PRES) != null)
				enablePres = (Boolean) properties.get(PROPERTY_PRES);
			if (properties.get(PROPERTY_GYRO) != null)
				enableGyro = (Boolean) properties.get(PROPERTY_GYRO);
			if (properties.get(PROPERTY_OPTO) != null)
				enableOpto = (Boolean) properties.get(PROPERTY_OPTO);
			if (properties.get(PROPERTY_BUTTONS) != null)
				enableButtons = (Boolean) properties.get(PROPERTY_BUTTONS);
			if (properties.get(PROPERTY_TOPIC) != null)
				m_topic = (String) properties.get(PROPERTY_TOPIC);
			if (properties.get(PROPERTY_INAME) != null)
				iname = (String) properties.get(PROPERTY_INAME);
		}
		
		m_tiSensorTagList = new ArrayList<TiSensorTag>();
		m_worker = Executors.newSingleThreadScheduledExecutor();

		try {
			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
			m_cloudClient.addCloudClientListener(this);

			// Get Bluetooth adapter and ensure it is enabled
			m_bluetoothAdapter = m_bluetoothService.getBluetoothAdapter(iname);
			if (m_bluetoothAdapter != null) {
				s_logger.info("Bluetooth adapter interface => " + iname);
				s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
				s_logger.info("Bluetooth adapter le enabled => " + m_bluetoothAdapter.isLeReady());

				if (!m_bluetoothAdapter.isEnabled()) {
					s_logger.info("Enabling bluetooth adapter...");
					m_bluetoothAdapter.enable();
					s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
				}
				m_startTime = 0;
				m_connected = false;
				m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
					@Override
					public void run() {
						checkScan();
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

		// cancel bluetoothAdapter
		m_bluetoothAdapter = null;
		
		// Releasing the CloudApplicationClient
		s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
		m_cloudClient.release();

		s_logger.debug("Deactivating BluetoothLe... Done.");
	}

	protected void updated(Map<String,Object> properties) {

		if(properties != null){
			if (properties.get(PROPERTY_SCANTIME) != null)
				m_scantime = (Integer) properties.get(PROPERTY_SCANTIME);
			if (properties.get(PROPERTY_PERIOD) != null)
				m_period = (Integer) properties.get(PROPERTY_PERIOD);
			if (properties.get(PROPERTY_TEMP) != null)
				enableTemp = (Boolean) properties.get(PROPERTY_TEMP);
			if (properties.get(PROPERTY_ACC) != null)
				enableAcc = (Boolean) properties.get(PROPERTY_ACC);
			if (properties.get(PROPERTY_HUM) != null)
				enableHum = (Boolean) properties.get(PROPERTY_HUM);
			if (properties.get(PROPERTY_MAG) != null)
				enableMag = (Boolean) properties.get(PROPERTY_MAG);
			if (properties.get(PROPERTY_PRES) != null)
				enablePres = (Boolean) properties.get(PROPERTY_PRES);
			if (properties.get(PROPERTY_GYRO) != null)
				enableGyro = (Boolean) properties.get(PROPERTY_GYRO);
			if (properties.get(PROPERTY_OPTO) != null)
				enableOpto = (Boolean) properties.get(PROPERTY_OPTO);
			if (properties.get(PROPERTY_BUTTONS) != null)
				enableButtons = (Boolean) properties.get(PROPERTY_BUTTONS);
			if (properties.get(PROPERTY_TOPIC) != null)
				m_topic = (String) properties.get(PROPERTY_TOPIC);
			if (properties.get(PROPERTY_INAME) != null)
				iname = (String) properties.get(PROPERTY_INAME);
		}
		
		try {
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
			
			// cancel a current worker handle if one is active
			if (m_handle != null) {
				m_handle.cancel(true);
			}

			// shutting down the worker and cleaning up the properties
			m_worker.shutdown();
			
			// cancel bluetoothAdapter
			m_bluetoothAdapter = null;
			
			// re-create the worker
			m_worker = Executors.newSingleThreadScheduledExecutor();
			
			// Get Bluetooth adapter and ensure it is enabled
			m_bluetoothAdapter = m_bluetoothService.getBluetoothAdapter(iname);
			if (m_bluetoothAdapter != null) {
				s_logger.info("Bluetooth adapter interface => " + iname);
				s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
				s_logger.info("Bluetooth adapter le enabled => " + m_bluetoothAdapter.isLeReady());

				if (!m_bluetoothAdapter.isEnabled()) {
					s_logger.info("Enabling bluetooth adapter...");
					m_bluetoothAdapter.enable();
					s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
				}
				m_startTime = 0;
				m_connected = false;
				m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
					@Override
					public void run() {
						checkScan();
					}
				}, 0, 1, TimeUnit.SECONDS);
			}
			else s_logger.warn("No Bluetooth adapter found ...");
		} catch (Exception e) {
			s_logger.error("Error starting component", e);
			throw new ComponentException(e);
		}

		s_logger.debug("Updating Bluetooth Service... Done.");
	}

	// --------------------------------------------------------------------
	//
	//  Main task executed every second
	//
	// --------------------------------------------------------------------

	void checkScan() {

		// Scan for devices
		if(m_bluetoothAdapter.isScanning()) {
			s_logger.info("m_bluetoothAdapter.isScanning");
			if((System.currentTimeMillis() - m_startTime) >= (m_scantime * 1000)) {
				m_bluetoothAdapter.killLeScan();
			}
		}
		else {
			if((System.currentTimeMillis() - m_startTime) >= (m_period * 1000)) {
				s_logger.info("startLeScan");
				m_bluetoothAdapter.startLeScan(this);
				m_startTime = System.currentTimeMillis();
			}
		}

	}

	// --------------------------------------------------------------------
	//
	//  Private Methods
	//
	// --------------------------------------------------------------------

	protected static void doPublishKeys(String address, Object key) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric("key", key);
		try {
			m_cloudClient.publish(m_topic + "/" + address + "/keys", payload, 0, false);
		} catch (Exception e) {
			s_logger.error("Can't publish message, " + "keys", e);
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
		}
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
					TiSensorTag tiSensorTag = new TiSensorTag(bluetoothDevice);
					m_tiSensorTagList.add(tiSensorTag);
				}
			}
			else { 
				s_logger.info("Found device = " + bluetoothDevice.getAdress());
			}
		}
		
		// connect to TiSensorTags
		for (TiSensorTag myTiSensorTag : m_tiSensorTagList) {
			
			if (!myTiSensorTag.isConnected()) {
				s_logger.info("Connecting to TiSensorTag...");
				m_connected = myTiSensorTag.connect();
			}
			else {
				s_logger.info("TiSensorTag already connected!");
				m_connected = true;
			}
			
			KuraPayload payload = new KuraPayload();
			payload.setTimestamp(new Date());
			if (myTiSensorTag.getCC2650())
				payload.addMetric("Type", "CC2650");
			else
				payload.addMetric("Type", "CC2541");
			
			if (m_connected) {
				
				myTiSensorTag.setFirmwareRevision(myTiSensorTag.firmwareRevision());
				
				if (enableTemp) {
					myTiSensorTag.enableTermometer();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					double[] temperatures = myTiSensorTag.readTemperature();
					
					s_logger.info("Ambient: " + temperatures[0] + " Target: " + temperatures[1]);
					
					payload.addMetric("Ambient", temperatures[0]);
					payload.addMetric("Target", temperatures[1]);
				}
				
				if (enableAcc) {
					if (myTiSensorTag.getCC2650()) {
						// Reduce period to 500ms (for a bug on SensorTag firmware :-)) and enable accelerometer with range 8g
						myTiSensorTag.setAccelerometerPeriod("32");
						myTiSensorTag.enableAccelerometer("3802");
					}
					else
						myTiSensorTag.enableAccelerometer("01");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					double[] acceleration = myTiSensorTag.readAcceleration();
					
					s_logger.info("Acc X: " + acceleration[0] + " Acc Y: " + acceleration[1] + " Acc Z: " + acceleration[2]);
					
					payload.addMetric("Acceleration X", acceleration[0]);
					payload.addMetric("Acceleration Y", acceleration[1]);
					payload.addMetric("Acceleration Z", acceleration[2]);
				}
				
				if (enableHum) {
					myTiSensorTag.enableHygrometer();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					float humidity = myTiSensorTag.readHumidity();
					s_logger.info("Humidity: " + humidity);
					
					payload.addMetric("Humidity", humidity);
				}	
				
				if (enableMag) {
					// Reduce period to 500ms (for a bug on SensorTag firmware :-)) and enable magnetometer
					myTiSensorTag.setMagnetometerPeriod("32");
					if (myTiSensorTag.getCC2650())
						myTiSensorTag.enableMagnetometer("4000");
					else 
						myTiSensorTag.enableMagnetometer("");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					float[] magneticField = myTiSensorTag.readMagneticField();
					
					s_logger.info("Mag X: " + magneticField[0] + " Mag Y: " + magneticField[1] + " Mag Z: " + magneticField[2]);
					
					payload.addMetric("Magnetic X", magneticField[0]);
					payload.addMetric("Magnetic Y", magneticField[1]);
					payload.addMetric("Magnetic Z", magneticField[2]);
									
				}
				
				if (enablePres) {
					// Calibrate pressure sensor
					myTiSensorTag.calibrateBarometer();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					myTiSensorTag.readCalibrationBarometer();
					
					// Read pressure
					myTiSensorTag.enableBarometer();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					double pressure = myTiSensorTag.readPressure();
					
					s_logger.info("Pre : " + pressure);
					
					payload.addMetric("Pressure", pressure);
				}

				if (enableGyro) {
					if (myTiSensorTag.getCC2650()) {
						// Reduce period to 500ms (for a bug on SensorTag firmware :-)) and enable gyroscope
						myTiSensorTag.setGyroscopePeriod("32");
						myTiSensorTag.enableGyroscope("0700");
					}
					else 
						myTiSensorTag.enableGyroscope("07");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					float[] gyroscope = myTiSensorTag.readGyroscope();
					
					s_logger.info("Gyro X: " + gyroscope[0] + " Gyro Y: " + gyroscope[1] + " Gyro Z: " + gyroscope[2]);
					
					payload.addMetric("Gyro X", gyroscope[0]);
					payload.addMetric("Gyro Y", gyroscope[1]);
					payload.addMetric("Gyro Z", gyroscope[2]);
									
				}
				
				if (enableOpto) {
					myTiSensorTag.enableLuxometer();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					double light = myTiSensorTag.readLight();
					s_logger.info("Light: " + light);
					
					payload.addMetric("Light", light);
				}
				
				if (enableButtons) {
					// For buttons only enable notifications
					myTiSensorTag.enableKeysNotification();
				}
				
				try {
					// Publish only if there are metrics to be published!
					if (!payload.metricNames().isEmpty())
						m_cloudClient.publish(m_topic + "/" + myTiSensorTag.getBluetoothDevice().getAdress() , payload, 0, false);
				} catch (KuraException e) {
					e.printStackTrace();
				}
			}
			else {
				s_logger.info("Cannot connect to TI SensorTag " + myTiSensorTag.getBluetoothDevice().getAdress() + ".");
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
