package org.eclipse.kura.example.beacon_scanner;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothBeaconData;
import org.eclipse.kura.bluetooth.BluetoothBeaconScanListener;
import org.eclipse.kura.bluetooth.BluetoothService;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeaconExample implements ConfigurableComponent, BluetoothBeaconScanListener {

	private static final Logger log = LoggerFactory.getLogger(BeaconExample.class);

	private static final String PROPERTY_TOPIC_PREFIX	= "topicPrefix";
	private static final String PROPERTY_INAME			= "iname";
	private static final String PROPERTY_RATE_LIMIT		= "rate_limit";

	// Configurable State
	private String adapterName;		// eg. hci0
	private String topicPrefix;		// eg. beacons
	private int rateLimit;			// eg. 5000ms
	
	// Internal State
	private BluetoothService	bluetoothService;
	private BluetoothAdapter	bluetoothAdapter;
	private CloudService		cloudService;
	private CloudClient			cloudClient;
	private Map<String, Long>	publishTimes;

	// Services
	public void setBluetoothService(BluetoothService bluetoothService) {
		this.bluetoothService = bluetoothService;
	}

	public void unsetBluetoothService(BluetoothService bluetoothService) {
		bluetoothService = null;
	}
	
	public void setCloudService(CloudService cloudService) {
		this.cloudService = cloudService;
	}

	public void unsetCloudService(CloudService bluetoothService) {
		cloudService = null;
	}

	protected void activate(ComponentContext context, Map<String,Object> properties) {
		log.info("Activating Bluetooth Beacon example...");
		

		try {
			cloudClient = cloudService.newCloudClient("BeaconExample");
		} catch(Exception e) {
			log.error("Unable to get CloudClient", e);
			throw new ComponentException(e);
		}
		
		updated(properties);
		
		log.info("Activating Bluetooth Beacon example...Done");

	}

	protected void deactivate(ComponentContext context) {

		log.debug("Deactivating Beacon Example...");
		
		releaseResources();
		
		cloudClient.release();
		
		log.debug("Deactivating Beacon Example... Done.");
	}

	protected void updated(Map<String,Object> properties) {
		
		releaseResources();
		
		for(Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			
			try {
				
				if(key.equals(PROPERTY_INAME)) {
					adapterName = (String)value;
					
				} else if(key.equals(PROPERTY_TOPIC_PREFIX)) {
					topicPrefix = (String)value;
					
				} else if(key.equals(PROPERTY_RATE_LIMIT)) {
					rateLimit = (Integer)value;
				}

			} catch(Exception e) {
				log.error("Bad property type {} ({})", key, value.getClass().getSimpleName());
			}
		}
		
		setup();
		
	}

	
	private void setup() {
		
		publishTimes = new HashMap<String, Long>();
		
		bluetoothAdapter = bluetoothService.getBluetoothAdapter(adapterName);
		if(bluetoothAdapter != null) {
			bluetoothAdapter.startBeaconScan(this);
		}
		
	}
	
	
	private void releaseResources() {
		if(bluetoothAdapter != null) {
			bluetoothAdapter.killLeScan();
			bluetoothAdapter = null;
		}
		
	}

	@Override
	public void onBeaconDataReceived(BluetoothBeaconData beaconData) {
		
		long now = System.nanoTime();
		
		Long lastPublishTime = publishTimes.get(beaconData.address);
		
		// If this beacon is new, or it last published more than 'rateLimit' ms ago
		if(lastPublishTime == null || (now - lastPublishTime) / 1000000L > rateLimit) {
			
			// Store the publish time against the address
			publishTimes.put(beaconData.address, now);
			
			// Publish the beacon data to the beacon's topic
			KuraPayload kp = new KuraPayload();
			kp.addMetric("uuid", beaconData.uuid);
			kp.addMetric("rssi",beaconData.rssi);
			kp.addMetric("major",beaconData.major);
			kp.addMetric("minor",beaconData.minor);
			try {
				cloudClient.publish(topicPrefix + "/" + beaconData.address, kp, 2, false);
			} catch (KuraException e) {
				log.error("Unable to publish", e);
			}
		}
	}
}
