package org.eclipse.kura.example.bluetooth;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothDevice;
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
	private final int WAIT_TIME = 20000;
	
	private CloudService m_cloudService;
	private static CloudClient  m_cloudClient;
	
	private TiSensorTag m_tiSensorTag;
	private BluetoothService m_bluetoothService;
	private BluetoothAdapter m_bluetoothAdapter;
	
	private List<BluetoothGattService> m_bluetoothGattServices;
	private boolean m_found = false;
	private LocalThread m_thread;
	
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
			
			doUpdate();
		} catch (Exception e) {
			s_logger.error("Error starting component", e);
			throw new ComponentException(e);
		}
		
	}
	
	protected void deactivate(ComponentContext context) {
		if (m_tiSensorTag != null) {
			m_tiSensorTag.disconnect();
		}
		m_thread.stopThread();
	}
	
	private void doUpdate() {
		
		// Get Bluetooth adapter and ensure it is enabled
		m_bluetoothAdapter = m_bluetoothService.getBluetoothAdapter();
		if (m_bluetoothAdapter != null) {
			s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
			s_logger.info("Bluetooth adapter le enabled => " + m_bluetoothAdapter.isLeReady());
			
			if (!m_bluetoothAdapter.isEnabled()) {
				s_logger.info("Enabling bluetooth adapter...");
				m_bluetoothAdapter.enable();
			}
			
			// Start scanning in separate thread to allow bundle to finish activation
			m_thread = new LocalThread();
			m_thread.start();
			
		}
		
	}
	
	// --------------------------------------------------------------------
	//
	//  Static Methods
	//
	// --------------------------------------------------------------------
	
	protected static void doPublishTemp(Object ambValue, Object targetValue) {
		KuraPayload payload = new KuraPayload();
		payload.setTimestamp(new Date());
		payload.addMetric("Ambient", ambValue);
		payload.addMetric("Target", targetValue);
		try {
			int messageId = m_cloudClient.publish("temperature", payload, 0, false);
		} catch (Exception e) {
			s_logger.error("Can't publish message, " + "temperature", e);
		}
		
	}
	
	// --------------------------------------------------------------------
	//
	//  Private Methods
	//
	// --------------------------------------------------------------------
	
	private void begin() {
		/*
		 *Scan for Bluetooth LE devices. This will block until the the desired device is found or
		 *the time limit is exceeded.
		*/
		startScan();
		s_logger.info("Looking for device...");
		long startTime = System.currentTimeMillis();
		while (!m_found && (System.currentTimeMillis() - startTime) < WAIT_TIME) {
			// do nothing
		}
		
		/*
		 * Device was found, connect to device. To connect, you must
	  	 * first get the GATT server of the device, then connect.
	  	*/
		if (m_found) {
			s_logger.info("Found, connecting...");
			boolean connected = m_tiSensorTag.connect();
			if (connected) {
				// Once connected, run through all diagnostics or comment
				// out and run demo
				s_logger.info("Connected!");
				//doServicesDiscovery();
				//testReadWrite();
				//testNotifications();
				runDemo();
				
			}
			else 
				s_logger.info("Device could not connect");
		}
		else {
			s_logger.info("Device not found");
		}
	}
	
	
	
	private void runDemo() {
		try {
			m_tiSensorTag.enableTempNotifications();
			Thread.sleep(1000);
			m_tiSensorTag.enableTempSensor();
		} catch (InterruptedException ie) {
			s_logger.error(ie.getLocalizedMessage());
		}
	}
	
	private void testReadWrite() {
		String value;
		
		s_logger.info("Starting read/write test...");
		// Enable temperature sensor
		m_tiSensorTag.enableTempSensor();
		try {
			Thread.sleep(5000);
			// Read value from temp sensor
			value = m_tiSensorTag.readTemp();
			s_logger.info("Temperatue read from handle is: " + value);
			Thread.sleep(5000);
			//Read value from UUID
			value = m_tiSensorTag.readTempByUuid();
			s_logger.info("Temperature read from UUID is: " + value);
		} catch (InterruptedException e) {
			s_logger.error("Error in testReadWrite: " + e.getLocalizedMessage());
		}
	}
	
	private void testNotifications() {
		s_logger.info("Starting notifications test...");
		try {
			
			// Enable notifications
			m_tiSensorTag.enableTempNotifications();
			// Enable temperature sensor
			m_tiSensorTag.enableTempSensor();
			// Data should show in lisenter
			// Delay, then disable notifications
			Thread.sleep(5000);
			m_tiSensorTag.disableTempNotifications();
		} catch (InterruptedException e) {
			s_logger.error("Error in testNotifications: " + e.getLocalizedMessage());
		}
		
		
	}
	
	private void startScan() {
		s_logger.info("Starting LE scan...");
		m_bluetoothAdapter.startLeScan(this);
	}
	
	private void doServicesDiscovery() {
		s_logger.info("Starting services discovery...");
		m_bluetoothGattServices = m_tiSensorTag.discoverServices();
		for (BluetoothGattService bgs : m_bluetoothGattServices) {
			s_logger.info("Service UUID: " + bgs.getUuid());
		}
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
			if (bluetoothDevice.getAdress().equals(TiSensorTag.ADDRESS)) {
				m_tiSensorTag = new TiSensorTag(bluetoothDevice);
				m_found = true;
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionLost() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionEstablished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * Local Thread
	 */
	private class LocalThread extends Thread {
		private volatile boolean stopThread = false;
		private volatile boolean done = false;
				
		public void run() {
			int x = 0;
			while (!stopThread && !done) {
				begin();
				done = true;
			}
			
		}
		
		public void stopThread() {
			stopThread = true;
		}
	}

}
