/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLe implements ConfigurableComponent, BluetoothLeScanListener, TiSensorTagNotificationListener {

    private static final String ADDRESS_MESSAGE_PROP_KEY = "address";

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLe.class);

    private static final String INTERRUPTED_EX = "Interrupted Exception";

    private List<TiSensorTag> tiSensorTagList;
    private BluetoothService bluetoothService;
    private BluetoothAdapter bluetoothAdapter;
    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;
    private long startTime;

    private BluetoothLeOptions options;

    private CloudPublisher cloudPublisher;

    public void setCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
    }

    public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = null;
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

        this.tiSensorTagList = new CopyOnWriteArrayList<>(new ArrayList<>());
        doUpdate(properties);
        logger.debug("Updating Bluetooth Service... Done.");
    }

    protected void deactivate(ComponentContext context) {
        doDeactivate();

        logger.debug("Deactivating BluetoothLe... Done.");
    }

    protected void updated(Map<String, Object> properties) {

        doDeactivate();
        doUpdate(properties);
        logger.debug("Updating Bluetooth Service... Done.");
    }

    private void doDeactivate() {
        logger.debug("Deactivating BluetoothLe...");
        if (this.bluetoothAdapter != null && this.bluetoothAdapter.isScanning()) {
            this.bluetoothAdapter.killLeScan();
        }

        // disconnect SensorTags
        for (TiSensorTag tiSensorTag : this.tiSensorTagList) {
            if (tiSensorTag != null && tiSensorTag.isConnected()) {
                if (this.options.isEnableButtons()) {
                    tiSensorTag.disableKeysNotifications();
                }
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
    }

    private void doUpdate(Map<String, Object> properties) {

        this.options = new BluetoothLeOptions(properties);
        this.startTime = 0;
        if (this.options.isEnableScan()) {
            // re-create the worker
            this.worker = Executors.newSingleThreadScheduledExecutor();

            // Get Bluetooth adapter and ensure it is enabled
            this.bluetoothAdapter = this.bluetoothService.getBluetoothAdapter(this.options.getIname());
            if (this.bluetoothAdapter != null) {
                logger.info("Bluetooth adapter interface => {}", this.options.getIname());
                if (!this.bluetoothAdapter.isEnabled()) {
                    logger.info("Enabling bluetooth adapter...");
                    this.bluetoothAdapter.enable();
                }
                logger.info("Bluetooth adapter address => {}", this.bluetoothAdapter.getAddress());

                this.handle = this.worker.scheduleAtFixedRate(this::performScan, 0, 1, TimeUnit.SECONDS);
            } else {
                logger.info("Bluetooth adapter {} not found.", this.options.getIname());
            }
        }
    }

    // --------------------------------------------------------------------
    //
    // Main task executed every second
    //
    // --------------------------------------------------------------------

    void performScan() {

        // Scan for devices
        if (this.bluetoothAdapter.isScanning()) {
            logger.info("m_bluetoothAdapter.isScanning");
            if (System.currentTimeMillis() - this.startTime >= this.options.getScantime() * 1000) {
                this.bluetoothAdapter.killLeScan();
            }
        } else {
            if (System.currentTimeMillis() - this.startTime >= this.options.getPeriod() * 1000) {
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

    @Override
    public void notify(String address, Map<String, Object> values) {
        if (this.cloudPublisher == null) {
            logger.info("No cloud publisher selected. Cannot publish!");
            return;
        }

        KuraPayload payload = new KuraPayload();
        payload.setTimestamp(new Date());
        for (Entry<String, Object> entry : values.entrySet()) {
            payload.addMetric(entry.getKey(), entry.getValue());
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(ADDRESS_MESSAGE_PROP_KEY, address);

        KuraMessage message = new KuraMessage(payload, properties);

        try {
            this.cloudPublisher.publish(message);
        } catch (KuraException e) {
            logger.error("Can't publish message", e);
        }

    }

    private void doServicesDiscovery(TiSensorTag tiSensorTag) {
        logger.info("Starting services discovery...");
        for (BluetoothGattService bgs : tiSensorTag.discoverServices()) {
            logger.info("Service UUID: {}  :  {}  :  {}", bgs.getUuid(), bgs.getStartHandle(), bgs.getEndHandle());
        }
    }

    private void doCharacteristicsDiscovery(TiSensorTag tiSensorTag) {
        List<BluetoothGattCharacteristic> lbgc = tiSensorTag.getCharacteristics("0x0001", "0x0100");
        for (BluetoothGattCharacteristic bgc : lbgc) {
            logger.info("Characteristics uuid : {} : {} : {}", bgc.getUuid(), bgc.getHandle(), bgc.getValueHandle());
        }
    }

    private boolean isSensorTagInList(String address) {
        boolean found = false;
        for (TiSensorTag tiSensorTag : this.tiSensorTagList) {
            if (tiSensorTag.getBluetoothDevice().getAdress().equals(address)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private void filterDevices(List<BluetoothDevice> devices) {
        // Scan for TI SensorTag
        for (BluetoothDevice bluetoothDevice : devices) {
            logger.info("Address {} Name {}", bluetoothDevice.getAdress(), bluetoothDevice.getName());

            if (bluetoothDevice.getName().contains("SensorTag") && !isSensorTagInList(bluetoothDevice.getAdress())) {
                this.tiSensorTagList.add(new TiSensorTag(bluetoothDevice));
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

        filterDevices(scanResults);

        // connect to TiSensorTags
        for (TiSensorTag myTiSensorTag : this.tiSensorTagList) {
            if (!myTiSensorTag.isConnected()) {
                logger.info("Connecting to TiSensorTag {}...", myTiSensorTag.getBluetoothDevice().getAdress());
                myTiSensorTag.connect(this.options.getIname());
            }

            if (myTiSensorTag.isConnected()) {
                KuraPayload payload = new KuraPayload();
                payload.setTimestamp(new Date());
                if (myTiSensorTag.isCC2650()) {
                    payload.addMetric("Type", "CC2650");
                } else {
                    payload.addMetric("Type", "CC2541");
                }

                if (this.options.isEnableServicesDiscovery()) {
                    doServicesDiscovery(myTiSensorTag);
                    doCharacteristicsDiscovery(myTiSensorTag);
                }

                payload.addMetric("Firmware", myTiSensorTag.getFirmareRevision());

                if (this.options.isEnableTemp()) {
                    readTemperature(myTiSensorTag, payload);
                }

                if (this.options.isEnableAcc()) {
                    readAcceleration(myTiSensorTag, payload);
                }

                if (this.options.isEnableHum()) {
                    readHumidity(myTiSensorTag, payload);
                }

                if (this.options.isEnableMag()) {
                    readMagneticField(myTiSensorTag, payload);
                }

                if (this.options.isEnablePres()) {
                    readPressure(myTiSensorTag, payload);
                }

                if (this.options.isEnableGyro()) {
                    readOrientation(myTiSensorTag, payload);
                }

                if (this.options.isEnableOpto()) {
                    readLight(myTiSensorTag, payload);
                }

                if (this.options.isEnableButtons()) {
                    // For buttons only enable notifications
                    myTiSensorTag.enableKeysNotifications(this);
                } else {
                    myTiSensorTag.disableKeysNotifications();
                }

                if (this.options.isEnableRedLed()) {
                    myTiSensorTag.switchOnRedLed();
                } else {
                    myTiSensorTag.switchOffRedLed();
                }

                if (this.options.isEnableGreenLed()) {
                    myTiSensorTag.switchOnGreenLed();
                } else {
                    myTiSensorTag.switchOffGreenLed();
                }

                if (this.options.isEnableBuzzer()) {
                    myTiSensorTag.switchOnBuzzer();
                } else {
                    myTiSensorTag.switchOffBuzzer();
                }

                myTiSensorTag.enableIOService();

                // Publish only if there are metrics to be published!
                if (!payload.metricNames().isEmpty() && this.cloudPublisher != null) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put(ADDRESS_MESSAGE_PROP_KEY, myTiSensorTag.getBluetoothDevice().getAdress());

                    KuraMessage message = new KuraMessage(payload, properties);

                    try {
                        this.cloudPublisher.publish(message);
                    } catch (KuraException e) {
                        logger.error("Publish message failed", e);
                    }
                }

            } else {
                logger.warn("Cannot connect to TI SensorTag {}.", myTiSensorTag.getBluetoothDevice().getAdress());
            }

        }
    }

    private void readLight(TiSensorTag myTiSensorTag, KuraPayload payload) {
        myTiSensorTag.enableLuxometer();
        waitFor(1000);

        double light = myTiSensorTag.readLight();
        logger.info("Light: {}", light);

        payload.addMetric("Light", light);
    }

    private void readOrientation(TiSensorTag myTiSensorTag, KuraPayload payload) {
        if (myTiSensorTag.isCC2650()) {
            // Reduce period to 500ms (for a bug on SensorTag firmware :-)) and enable gyroscope
            myTiSensorTag.setGyroscopePeriod("32");
            myTiSensorTag.enableGyroscope("0700");
        } else {
            myTiSensorTag.enableGyroscope("07");
        }
        waitFor(1000);
        float[] gyroscope = myTiSensorTag.readGyroscope();

        logger.info("Gyro X: {} Gyro Y: {} Gyro Z: {}", gyroscope[0], gyroscope[1], gyroscope[2]);

        payload.addMetric("Gyro X", gyroscope[0]);
        payload.addMetric("Gyro Y", gyroscope[1]);
        payload.addMetric("Gyro Z", gyroscope[2]);
    }

    private void readPressure(TiSensorTag myTiSensorTag, KuraPayload payload) {
        // Calibrate pressure sensor
        myTiSensorTag.calibrateBarometer();
        waitFor(1000);

        // Read pressure
        myTiSensorTag.enableBarometer();
        waitFor(1000);
        double pressure = myTiSensorTag.readPressure();

        logger.info("Pre: {}", pressure);

        payload.addMetric("Pressure", pressure);
    }

    private void readMagneticField(TiSensorTag myTiSensorTag, KuraPayload payload) {
        // Reduce period to 500ms (for a bug on SensorTag firmware :-)) and enable magnetometer
        myTiSensorTag.setMagnetometerPeriod("32");
        if (myTiSensorTag.isCC2650()) {
            myTiSensorTag.enableMagnetometer("4000");
        } else {
            myTiSensorTag.enableMagnetometer("");
        }
        waitFor(1000);
        float[] magneticField = myTiSensorTag.readMagneticField();

        logger.info("Mag X: {} Mag Y: {} Mag Z: {}", magneticField[0], magneticField[1], magneticField[2]);

        payload.addMetric("Magnetic X", magneticField[0]);
        payload.addMetric("Magnetic Y", magneticField[1]);
        payload.addMetric("Magnetic Z", magneticField[2]);
    }

    private void readHumidity(TiSensorTag myTiSensorTag, KuraPayload payload) {
        myTiSensorTag.enableHygrometer();
        waitFor(1000);

        float humidity = myTiSensorTag.readHumidity();
        logger.info("Humidity: {}", humidity);

        payload.addMetric("Humidity", humidity);
    }

    private void readAcceleration(TiSensorTag myTiSensorTag, KuraPayload payload) {
        // Reduce period to 500ms (for a bug on SensorTag firmware :-)) and enable accelerometer with
        // range 8g
        myTiSensorTag.setAccelerometerPeriod("32");
        if (myTiSensorTag.isCC2650()) {
            myTiSensorTag.enableAccelerometer("3802");
        } else {
            myTiSensorTag.enableAccelerometer("01");
        }
        waitFor(1000);
        double[] acceleration = myTiSensorTag.readAcceleration();

        logger.info("Acc X: {} Acc Y: {} Acc Z: {}", acceleration[0], acceleration[1], acceleration[2]);

        payload.addMetric("Acceleration X", acceleration[0]);
        payload.addMetric("Acceleration Y", acceleration[1]);
        payload.addMetric("Acceleration Z", acceleration[2]);
    }

    private void readTemperature(TiSensorTag myTiSensorTag, KuraPayload payload) {
        myTiSensorTag.enableTermometer();
        waitFor(1000);
        double[] temperatures = myTiSensorTag.readTemperature();

        logger.info("Ambient: {} Target: {}", temperatures[0], temperatures[1]);

        payload.addMetric("Ambient", temperatures[0]);
        payload.addMetric("Target", temperatures[1]);
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(INTERRUPTED_EX, e);
        }
    }
}
