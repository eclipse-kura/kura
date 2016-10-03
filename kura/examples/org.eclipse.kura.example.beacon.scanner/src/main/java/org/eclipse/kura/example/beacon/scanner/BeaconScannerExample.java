package org.eclipse.kura.example.beacon.scanner;

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

public class BeaconScannerExample implements ConfigurableComponent, BluetoothBeaconScanListener {

    private static final Logger logger = LoggerFactory.getLogger(BeaconScannerExample.class);

    private static final String PROPERTY_ENABLE = "enableScanning";
    private static final String PROPERTY_TOPIC_PREFIX = "topicPrefix";
    private static final String PROPERTY_INAME = "iname";
    private static final String PROPERTY_RATE_LIMIT = "rate_limit";
    private static final String PROPERTY_COMPANY_CODE = "companyCode";

    // Configurable State
    private String adapterName;		// eg. hci0
    private String topicPrefix;		// eg. beacons
    private int rateLimit;			// eg. 5000ms
    private String companyCode;
    private Boolean enableScanning;

    // Internal State
    private BluetoothService bluetoothService;
    private BluetoothAdapter bluetoothAdapter;
    private CloudService cloudService;
    private CloudClient cloudClient;
    private Map<String, Long> publishTimes;

    // Services
    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    public void unsetBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = null;
    }

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.cloudService = null;
    }

    protected void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Activating Bluetooth Beacon Scanner example...");

        try {
            this.cloudClient = this.cloudService.newCloudClient("BeaconScannerExample");
        } catch (Exception e) {
            logger.error("Unable to get CloudClient", e);
            throw new ComponentException(e);
        }

        this.enableScanning = false;
        updated(properties);

        logger.info("Activating Bluetooth Beacon Scanner example...Done");

    }

    protected void deactivate(ComponentContext context) {

        logger.debug("Deactivating Beacon Scanner Example...");

        releaseResources();
        this.enableScanning = false;

        this.cloudClient.release();

        logger.debug("Deactivating Beacon Scanner Example... Done.");
    }

    protected void updated(Map<String, Object> properties) {

        releaseResources();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            try {

                if (key.equals(PROPERTY_INAME)) {
                    this.adapterName = (String) value;
                } else if (key.equals(PROPERTY_TOPIC_PREFIX)) {
                    this.topicPrefix = (String) value;
                } else if (key.equals(PROPERTY_RATE_LIMIT)) {
                    this.rateLimit = (Integer) value;
                } else if (key.equals(PROPERTY_COMPANY_CODE)) {
                    this.companyCode = (String) value;
                } else if (key.equals(PROPERTY_ENABLE)) {
                    this.enableScanning = (Boolean) value;
                }

            } catch (Exception e) {
                logger.error("Bad property type {}", key, e);
            }
        }

        if (this.enableScanning) {
            setup();
        }

    }

    private void setup() {

        this.publishTimes = new HashMap<String, Long>();

        this.bluetoothAdapter = this.bluetoothService.getBluetoothAdapter(this.adapterName);
        if (this.bluetoothAdapter != null) {
            this.bluetoothAdapter.startBeaconScan(this.companyCode, this);
        }

    }

    private void releaseResources() {
        if (this.bluetoothAdapter != null) {
            this.bluetoothAdapter.killLeScan();
            this.bluetoothAdapter = null;
        }

    }

    private double calculateDistance(int rssi, int txpower) {

        double distance;

        int ratioDB = txpower - rssi;
        double ratioLinear = Math.pow(10, (double) ratioDB / 10);
        distance = Math.sqrt(ratioLinear);

        // See http://stackoverflow.com/questions/20416218/understanding-ibeacon-distancing/20434019#20434019
        // double ratio = rssi*1.0/txpower;
        // if (ratio < 1.0) {
        // distance = Math.pow(ratio,10);
        // }
        // else {
        // distance = (0.89976)*Math.pow(ratio,7.7095) + 0.111;
        // }

        return distance;
    }

    @Override
    public void onBeaconDataReceived(BluetoothBeaconData beaconData) {

        logger.debug("Beacon from {} detected.", beaconData.address);
        long now = System.nanoTime();

        Long lastPublishTime = this.publishTimes.get(beaconData.address);

        // If this beacon is new, or it last published more than 'rateLimit' ms ago
        if (lastPublishTime == null || (now - lastPublishTime) / 1000000L > this.rateLimit) {

            // Store the publish time against the address
            this.publishTimes.put(beaconData.address, now);

            // Publish the beacon data to the beacon's topic
            KuraPayload kp = new KuraPayload();
            kp.addMetric("uuid", beaconData.uuid);
            kp.addMetric("txpower", beaconData.txpower);
            kp.addMetric("rssi", beaconData.rssi);
            kp.addMetric("major", beaconData.major);
            kp.addMetric("minor", beaconData.minor);
            kp.addMetric("distance", calculateDistance(beaconData.rssi, beaconData.txpower));
            try {
                this.cloudClient.publish(this.topicPrefix + "/" + beaconData.address, kp, 2, false);
            } catch (KuraException e) {
                logger.error("Unable to publish", e);
            }
        }
    }
}
