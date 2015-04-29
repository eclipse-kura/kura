package org.eclipse.kura.example.bluetooth;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
	private final int WAIT_TIME = 5000;
	
	private final String PROPERTY_DEVICEID = "deviceId";
	
	private CloudService m_cloudService;
	private static CloudClient  m_cloudClient;
	
	private TiSensorTag m_tiSensorTag;
	private BluetoothService m_bluetoothService;
	private BluetoothAdapter m_bluetoothAdapter;
	
	private List<BluetoothGattService> m_bluetoothGattServices;
	private boolean m_found = false;
	private LocalThread m_thread;
	private boolean endTest =false;
	
	private String m_deviceId;
	
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
		if (m_tiSensorTag != null) {
			m_tiSensorTag.disconnect();
		}
		endTest = true;
		m_thread.stopThread();
	}
	
	protected void updated(Map<String,Object> properties) {
		s_logger.debug("Updating Bluetooth Service...");
		doUpdate(properties);
	}

	private void doUpdate(Map<String,Object> properties) {
		
		if(properties.get(PROPERTY_DEVICEID) != null){
			m_deviceId = (String) properties.get(PROPERTY_DEVICEID);
			s_logger.info("Device ID from properties = "+m_deviceId);
		}
		else{ 
			m_deviceId = TiSensorTag.ADDRESS;
			s_logger.info("Device ID from properties = NULL -> hardcoded = "+m_deviceId);
		}

		if (m_tiSensorTag != null) {
			m_tiSensorTag.disconnect();
		}
		if(m_thread!=null)
			m_thread.stopThread();
		
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
		while (!m_found && !endTest){
			startScan();
			s_logger.info("Looking for device...");
			long startTime = System.currentTimeMillis();
			while (!m_found && (System.currentTimeMillis() - startTime) < WAIT_TIME) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(endTest)
				return;
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
				doServicesDiscovery();
				doCharacteristicsDiscovery();
				//testReadWrite();
				//testNotifications();
				//runDemo();
				
				m_tiSensorTag.disconnect();
			}
			else 
				s_logger.info("Device could not connect");
		}
		else {
			s_logger.info("Device not found");
		}
	}
	
	private void doServicesDiscovery() {
		s_logger.info("Starting services discovery...");
		m_bluetoothGattServices = m_tiSensorTag.discoverServices();
		for (BluetoothGattService bgs : m_bluetoothGattServices) {	
			s_logger.info("Service UUID: " + bgs.getUuid()+"  :  "+bgs.getStartHandle()+"  :  "+bgs.getEndHandle());
		}
	}

	private void doCharacteristicsDiscovery() {
		List<BluetoothGattCharacteristic> lbgc = m_tiSensorTag.getCharacteristics("0x0001", "0x0100"); 
		for(BluetoothGattCharacteristic bgc:lbgc){
			s_logger.info("Characteristics uuid : "+bgc.getUuid()+" : "+bgc.getHandle()+" : "+bgc.getValueHandle());
			String ls = bgc.getUuid().toString();
			if(ls.startsWith("00002a00")){
				String value = m_tiSensorTag.readTemp(bgc.getValueHandle());
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
		
 		// Try to read All Values ?		
		
//		String [] uu = new String[100];
//		String [] vv = new String[100];
//		int index=0;
//		for(BluetoothGattCharacteristic bgc:lbgc){
//			String ls = bgc.getUuid().toString();
//			if(ls.startsWith("f000aa")){
//				String value = m_tiSensorTag.readTempByUuid(bgc.getUuid());
//				//s_logger.info("Read from UUID is: " + value);
//				uu[index]=ls;
//				vv[index]=value;
//				index++;
//			}
//		}
//		for(int i=0; i<index; i++){
//			s_logger.info(uu[i]+"  ->  "+vv[i]);
//		}
		
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
			value = m_tiSensorTag.readTemp(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE);
			s_logger.info("Temperatue read from handle is: " + value);
			Thread.sleep(5000);
			//Read value from UUID
			value = m_tiSensorTag.readTempByUuid(TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE);
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
			// Data should show in listener
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
			if (bluetoothDevice.getAdress().equals(m_deviceId)) {
				s_logger.info("Smart Sensor found ");
				m_tiSensorTag = new TiSensorTag(bluetoothDevice);
				m_found = true;
			}
			else s_logger.info("Found device = "+bluetoothDevice.getAdress());
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
