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
import org.eclipse.kura.bluetooth.BluetoothGattSecurityLevel;
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

public class BluetoothLe implements ConfigurableComponent, CloudClientListener, BluetoothLeScanListener {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLe.class);

    private final String APP_ID = "BLE_APP_V1";
    private final String PROPERTY_SCAN = "scan_enable";
    private final String PROPERTY_SCANTIME = "scan_time";
    private final String PROPERTY_PERIOD = "period";
    private final String PROPERTY_TEMP = "enableTermometer";
    private final String PROPERTY_ACC = "enableAccelerometer";
    private final String PROPERTY_HUM = "enableHygrometer";
    private final String PROPERTY_MAG = "enableMagnetometer";
    private final String PROPERTY_PRES = "enableBarometer";
    private final String PROPERTY_GYRO = "enableGyroscope";
    private final String PROPERTY_OPTO = "enableLuxometer";
    private final String PROPERTY_BUTTONS = "enableButtons";
    private final String PROPERTY_REDLED = "switchOnRedLed";
    private final String PROPERTY_GREENLED = "switchOnGreenLed";
    private final String PROPERTY_BUZZER = "switchOnBuzzer";
    private final String PROPERTY_TOPIC = "publishTopic";
    private final String PROPERTY_INAME = "iname";

    private CloudService cloudService;
    private static CloudClient cloudClient;
    private List<TiSensorTag> tiSensorTagList;
    private BluetoothService bluetoothService;
    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothGattService> bluetoothGattServices;
    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private int period = 10;
    private int scantime = 5;
    private static String topic = "data";
    private long startTime;
    private boolean connected = false;
    private String iname = "hci0";
    private boolean enableScan = false;
    private boolean enableTemp = false;
    private boolean enableAcc = false;
    private boolean enableHum = false;
    private boolean enableMag = false;
    private boolean enablePres = false;
    private boolean enableGyro = false;
    private boolean enableOpto = false;
    private boolean enableButtons = false;
    private boolean enableRedLed = false;
    private boolean enableGreenLed = false;
    private boolean enableBuzzer = false;

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.cloudService = null;
    }

    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    public void unsetBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = null;
    }

    // --------------------------------------------------------------------
    //
    // Activation APIs
    //
    // --------------------------------------------------------------------
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Activating BluetoothLe example...");

        readProperties(properties);

        this.tiSensorTagList = new ArrayList<TiSensorTag>();

        try {
            cloudClient = this.cloudService.newCloudClient(this.APP_ID);
            cloudClient.addCloudClientListener(this);
        } catch (KuraException e1) {
            logger.error("Error starting component", e1);
            throw new ComponentException(e1);
        }

        if (this.enableScan) {

            this.worker = Executors.newSingleThreadScheduledExecutor();

            try {

                // Get Bluetooth adapter and ensure it is enabled
                this.bluetoothAdapter = this.bluetoothService.getBluetoothAdapter(this.iname);
                if (this.bluetoothAdapter != null) {
                    logger.info("Bluetooth adapter interface => " + this.iname);
                    logger.info("Bluetooth adapter address => " + this.bluetoothAdapter.getAddress());
                    logger.info("Bluetooth adapter le enabled => " + this.bluetoothAdapter.isLeReady());

                    if (!this.bluetoothAdapter.isEnabled()) {
                        logger.info("Enabling bluetooth adapter...");
                        this.bluetoothAdapter.enable();
                        logger.info("Bluetooth adapter address => " + this.bluetoothAdapter.getAddress());
                    }
                    this.startTime = 0;
                    this.connected = false;
                    this.handle = this.worker.scheduleAtFixedRate(new Runnable() {

                        @Override
                        public void run() {
                            checkScan();
                        }
                    }, 0, 1, TimeUnit.SECONDS);
                } else {
                    logger.warn("No Bluetooth adapter found ...");
                }
            } catch (Exception e) {
                logger.error("Error starting component", e);
                throw new ComponentException(e);
            }
        }
    }

    protected void deactivate(ComponentContext context) {

        logger.debug("Deactivating BluetoothLe...");
        if (this.bluetoothAdapter != null && this.bluetoothAdapter.isScanning()) {
            logger.debug("m_bluetoothAdapter.isScanning");
            this.bluetoothAdapter.killLeScan();
        }

        // disconnect SensorTags
        for (TiSensorTag tiSensorTag : this.tiSensorTagList) {
            if (tiSensorTag != null) {
                tiSensorTag.disconnect();
            }
        }
        this.tiSensorTagList.clear();

        // cancel a current worker handle if one if active
        if (this.handle != null) {
            this.handle.cancel(true);
        }

        // shutting down the worker and cleaning up the properties
        if (this.worker != null) {
            this.worker.shutdown();
        }

        // cancel bluetoothAdapter
        this.bluetoothAdapter = null;

        // Releasing the CloudApplicationClient
        logger.info("Releasing CloudApplicationClient for {}...", this.APP_ID);
        if (cloudClient != null) {
            cloudClient.release();
        }

        logger.debug("Deactivating BluetoothLe... Done.");
    }

    protected void updated(Map<String, Object> properties) {

        readProperties(properties);

        try {
            logger.debug("Deactivating BluetoothLe...");
            if (this.bluetoothAdapter != null && this.bluetoothAdapter.isScanning()) {
                logger.debug("m_bluetoothAdapter.isScanning");
                this.bluetoothAdapter.killLeScan();
            }

            // disconnect SensorTags
            for (TiSensorTag tiSensorTag : this.tiSensorTagList) {
                if (tiSensorTag != null) {
                    tiSensorTag.disconnect();
                }
            }
            this.tiSensorTagList.clear();

            // cancel a current worker handle if one is active
            if (this.handle != null) {
                this.handle.cancel(true);
            }

            // shutting down the worker and cleaning up the properties
            if (this.worker != null) {
                this.worker.shutdown();
            }

            // cancel bluetoothAdapter
            this.bluetoothAdapter = null;

            if (this.enableScan) {
                // re-create the worker
                this.worker = Executors.newSingleThreadScheduledExecutor();

                // Get Bluetooth adapter and ensure it is enabled
                this.bluetoothAdapter = this.bluetoothService.getBluetoothAdapter(this.iname);
                if (this.bluetoothAdapter != null) {
                    logger.info("Bluetooth adapter interface => " + this.iname);
                    logger.info("Bluetooth adapter address => " + this.bluetoothAdapter.getAddress());
                    logger.info("Bluetooth adapter le enabled => " + this.bluetoothAdapter.isLeReady());

                    if (!this.bluetoothAdapter.isEnabled()) {
                        logger.info("Enabling bluetooth adapter...");
                        this.bluetoothAdapter.enable();
                        logger.info("Bluetooth adapter address => " + this.bluetoothAdapter.getAddress());
                    }
                    this.startTime = 0;
                    this.connected = false;
                    this.handle = this.worker.scheduleAtFixedRate(new Runnable() {

                        @Override
                        public void run() {
                            checkScan();
                        }
                    }, 0, 1, TimeUnit.SECONDS);
                } else {
                    logger.warn("No Bluetooth adapter found ...");
                }
            }
        } catch (Exception e) {
            logger.error("Error starting component", e);
            throw new ComponentException(e);
        }

        logger.debug("Updating Bluetooth Service... Done.");
    }

    // --------------------------------------------------------------------
    //
    // Main task executed every second
    //
    // --------------------------------------------------------------------

    void checkScan() {

        // Scan for devices
        if (this.bluetoothAdapter.isScanning()) {
            logger.info("m_bluetoothAdapter.isScanning");
            if (System.currentTimeMillis() - this.startTime >= this.scantime * 1000) {
                this.bluetoothAdapter.killLeScan();
            }
        } else {
            if (System.currentTimeMillis() - this.startTime >= this.period * 1000) {
                logger.info("startLeScan");
                this.bluetoothAdapter.startLeScan(this);
                this.startTime = System.currentTimeMillis();
            }
        }

    }

    // --------------------------------------------------------------------
    //
    // Private Methods
    //
    // --------------------------------------------------------------------

    protected static void doPublishKeys(String address, Object key) {
        KuraPayload payload = new KuraPayload();
        payload.setTimestamp(new Date());
        payload.addMetric("key", key);
        try {
            cloudClient.publish(topic + "/" + address + "/keys", payload, 0, false);
        } catch (Exception e) {
            logger.error("Can't publish message, " + "keys", e);
        }

    }

    @SuppressWarnings("unused")
    private void doServicesDiscovery(TiSensorTag tiSensorTag) {
        logger.info("Starting services discovery...");
        this.bluetoothGattServices = tiSensorTag.discoverServices();
        for (BluetoothGattService bgs : this.bluetoothGattServices) {
            logger.info(
                    "Service UUID: " + bgs.getUuid() + "  :  " + bgs.getStartHandle() + "  :  " + bgs.getEndHandle());
        }
    }

    @SuppressWarnings("unused")
    private void doCharacteristicsDiscovery(TiSensorTag tiSensorTag) {
        List<BluetoothGattCharacteristic> lbgc = tiSensorTag.getCharacteristics("0x0001", "0x0100");
        for (BluetoothGattCharacteristic bgc : lbgc) {
            logger.info(
                    "Characteristics uuid : " + bgc.getUuid() + " : " + bgc.getHandle() + " : " + bgc.getValueHandle());
        }
    }

    private boolean searchSensorTagList(String address) {

        for (TiSensorTag tiSensorTag : this.tiSensorTagList) {
            if (tiSensorTag.getBluetoothDevice().getAdress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    private void readProperties(Map<String, Object> properties) {
        if (properties != null) {
            if (properties.get(this.PROPERTY_SCAN) != null) {
                this.enableScan = (Boolean) properties.get(this.PROPERTY_SCAN);
            }
            if (properties.get(this.PROPERTY_SCANTIME) != null) {
                this.scantime = (Integer) properties.get(this.PROPERTY_SCANTIME);
            }
            if (properties.get(this.PROPERTY_PERIOD) != null) {
                this.period = (Integer) properties.get(this.PROPERTY_PERIOD);
            }
            if (properties.get(this.PROPERTY_TEMP) != null) {
                this.enableTemp = (Boolean) properties.get(this.PROPERTY_TEMP);
            }
            if (properties.get(this.PROPERTY_ACC) != null) {
                this.enableAcc = (Boolean) properties.get(this.PROPERTY_ACC);
            }
            if (properties.get(this.PROPERTY_HUM) != null) {
                this.enableHum = (Boolean) properties.get(this.PROPERTY_HUM);
            }
            if (properties.get(this.PROPERTY_MAG) != null) {
                this.enableMag = (Boolean) properties.get(this.PROPERTY_MAG);
            }
            if (properties.get(this.PROPERTY_PRES) != null) {
                this.enablePres = (Boolean) properties.get(this.PROPERTY_PRES);
            }
            if (properties.get(this.PROPERTY_GYRO) != null) {
                this.enableGyro = (Boolean) properties.get(this.PROPERTY_GYRO);
            }
            if (properties.get(this.PROPERTY_OPTO) != null) {
                this.enableOpto = (Boolean) properties.get(this.PROPERTY_OPTO);
            }
            if (properties.get(this.PROPERTY_BUTTONS) != null) {
                this.enableButtons = (Boolean) properties.get(this.PROPERTY_BUTTONS);
            }
            if (properties.get(this.PROPERTY_REDLED) != null) {
                this.enableRedLed = (Boolean) properties.get(this.PROPERTY_REDLED);
            }
            if (properties.get(this.PROPERTY_GREENLED) != null) {
                this.enableGreenLed = (Boolean) properties.get(this.PROPERTY_GREENLED);
            }
            if (properties.get(this.PROPERTY_BUZZER) != null) {
                this.enableBuzzer = (Boolean) properties.get(this.PROPERTY_BUZZER);
            }
            if (properties.get(this.PROPERTY_TOPIC) != null) {
                topic = (String) properties.get(this.PROPERTY_TOPIC);
            }
            if (properties.get(this.PROPERTY_INAME) != null) {
                this.iname = (String) properties.get(this.PROPERTY_INAME);
            }
        }
    }

    // --------------------------------------------------------------------
    //
    // BluetoothLeScanListener APIs
    //
    // --------------------------------------------------------------------
    @Override
    public void onScanFailed(int errorCode) {
        logger.error("Error during scan");

    }

    @Override
    public void onScanResults(List<BluetoothDevice> scanResults) {

        // Scan for TI SensorTag
        for (BluetoothDevice bluetoothDevice : scanResults) {
            logger.info("Address " + bluetoothDevice.getAdress() + " Name " + bluetoothDevice.getName());

            if (bluetoothDevice.getName().contains("SensorTag")) {
                logger.info("TI SensorTag " + bluetoothDevice.getAdress() + " found.");
                if (!searchSensorTagList(bluetoothDevice.getAdress())) {
                    TiSensorTag tiSensorTag = new TiSensorTag(bluetoothDevice);
                    this.tiSensorTagList.add(tiSensorTag);
                }
            } else {
                logger.info("Found device = " + bluetoothDevice.getAdress());
            }
        }

        logger.debug("Found " + this.tiSensorTagList.size() + " SensorTags");

        // connect to TiSensorTags
        for (TiSensorTag myTiSensorTag : this.tiSensorTagList) {

            if (!myTiSensorTag.isConnected()) {
                logger.info("Connecting to TiSensorTag...");
                this.connected = myTiSensorTag.connect(this.iname);
                if (this.connected) {
                    logger.info("Set security level to high.");
                    myTiSensorTag.setSecurityLevel(BluetoothGattSecurityLevel.HIGH);
                    logger.info("Security Level : " + myTiSensorTag.getSecurityLevel().toString());
                }
            } else {
                logger.info("TiSensorTag already connected!");
                this.connected = true;
            }

            if (this.connected) {

                KuraPayload payload = new KuraPayload();
                payload.setTimestamp(new Date());
                if (myTiSensorTag.getCC2650()) {
                    payload.addMetric("Type", "CC2650");
                } else {
                    payload.addMetric("Type", "CC2541");
                }

                // Test
                // doServicesDiscovery(myTiSensorTag);
                // doCharacteristicsDiscovery(myTiSensorTag);

                myTiSensorTag.setFirmwareRevision(myTiSensorTag.firmwareRevision());

                if (this.enableTemp) {
                    myTiSensorTag.enableTermometer();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    double[] temperatures = myTiSensorTag.readTemperature();

                    logger.info("Ambient: " + temperatures[0] + " Target: " + temperatures[1]);

                    payload.addMetric("Ambient", temperatures[0]);
                    payload.addMetric("Target", temperatures[1]);
                }

                if (this.enableAcc) {
                    if (myTiSensorTag.getCC2650()) {
                        // Reduce period to 500ms (for a bug on SensorTag firmware :-)) and enable accelerometer with
                        // range 8g
                        myTiSensorTag.setAccelerometerPeriod("32");
                        myTiSensorTag.enableAccelerometer("3802");
                    } else {
                        myTiSensorTag.enableAccelerometer("01");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted Exception", e);
                    }
                    double[] acceleration = myTiSensorTag.readAcceleration();

                    logger.info(
                            "Acc X: " + acceleration[0] + " Acc Y: " + acceleration[1] + " Acc Z: " + acceleration[2]);

                    payload.addMetric("Acceleration X", acceleration[0]);
                    payload.addMetric("Acceleration Y", acceleration[1]);
                    payload.addMetric("Acceleration Z", acceleration[2]);
                }

                if (this.enableHum) {
                    myTiSensorTag.enableHygrometer();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted Exception", e);
                    }

                    float humidity = myTiSensorTag.readHumidity();
                    logger.info("Humidity: " + humidity);

                    payload.addMetric("Humidity", humidity);
                }

                if (this.enableMag) {
                    // Reduce period to 500ms (for a bug on SensorTag firmware :-)) and enable magnetometer
                    myTiSensorTag.setMagnetometerPeriod("32");
                    if (myTiSensorTag.getCC2650()) {
                        myTiSensorTag.enableMagnetometer("4000");
                    } else {
                        myTiSensorTag.enableMagnetometer("");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted Exception", e);
                    }
                    float[] magneticField = myTiSensorTag.readMagneticField();

                    logger.info("Mag X: " + magneticField[0] + " Mag Y: " + magneticField[1] + " Mag Z: "
                            + magneticField[2]);

                    payload.addMetric("Magnetic X", magneticField[0]);
                    payload.addMetric("Magnetic Y", magneticField[1]);
                    payload.addMetric("Magnetic Z", magneticField[2]);

                }

                if (this.enablePres) {
                    // Calibrate pressure sensor
                    myTiSensorTag.calibrateBarometer();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted Exception", e);
                    }
                    myTiSensorTag.readCalibrationBarometer();

                    // Read pressure
                    myTiSensorTag.enableBarometer();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted Exception", e);
                    }
                    double pressure = myTiSensorTag.readPressure();

                    logger.info("Pre : " + pressure);

                    payload.addMetric("Pressure", pressure);
                }

                if (this.enableGyro) {
                    if (myTiSensorTag.getCC2650()) {
                        // Reduce period to 500ms (for a bug on SensorTag firmware :-)) and enable gyroscope
                        myTiSensorTag.setGyroscopePeriod("32");
                        myTiSensorTag.enableGyroscope("0700");
                    } else {
                        myTiSensorTag.enableGyroscope("07");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted Exception", e);
                    }
                    float[] gyroscope = myTiSensorTag.readGyroscope();

                    logger.info("Gyro X: " + gyroscope[0] + " Gyro Y: " + gyroscope[1] + " Gyro Z: " + gyroscope[2]);

                    payload.addMetric("Gyro X", gyroscope[0]);
                    payload.addMetric("Gyro Y", gyroscope[1]);
                    payload.addMetric("Gyro Z", gyroscope[2]);

                }

                if (this.enableOpto) {
                    myTiSensorTag.enableLuxometer();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted Exception", e);
                    }

                    double light = myTiSensorTag.readLight();
                    logger.info("Light: " + light);

                    payload.addMetric("Light", light);
                }

                if (this.enableButtons) {
                    // For buttons only enable notifications
                    myTiSensorTag.enableKeysNotification();
                }

                if (this.enableRedLed) {
                    myTiSensorTag.switchOnRedLed();
                } else {
                    myTiSensorTag.switchOffRedLed();
                }

                if (this.enableGreenLed) {
                    myTiSensorTag.switchOnGreenLed();
                } else {
                    myTiSensorTag.switchOffGreenLed();
                }

                if (this.enableBuzzer) {
                    myTiSensorTag.switchOnBuzzer();
                } else {
                    myTiSensorTag.switchOffBuzzer();
                }

                myTiSensorTag.enableIOService();

                try {
                    // Publish only if there are metrics to be published!
                    if (!payload.metricNames().isEmpty()) {
                        cloudClient.publish(topic + "/" + myTiSensorTag.getBluetoothDevice().getAdress(), payload, 0,
                                false);
                    }
                } catch (Exception e) {
                    logger.error("Interrupted Exception", e);
                }
            } else {
                logger.info("Cannot connect to TI SensorTag " + myTiSensorTag.getBluetoothDevice().getAdress() + ".");
            }

        }

    }

    // --------------------------------------------------------------------
    //
    // CloudClientListener APIs
    //
    // --------------------------------------------------------------------
    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {

    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {

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
