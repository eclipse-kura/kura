/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.ble.tisensortag.tinyb;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLe implements ConfigurableComponent {

    private static final String ADDRESS_MESSAGE_PROP_KEY = "address";

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLe.class);

    private static final String INTERRUPTED_EX = "Interrupted Exception";
    private static final String DISCOVERY_STOP_EX = "Failed to stop discovery";
    private static final String CONNECTION_ERROR_EX = "Cannot connect/disconnect to TI SensorTag {}.";

    private List<TiSensorTag> tiSensorTagList;
    private BluetoothLeService bluetoothLeService;
    private BluetoothLeAdapter bluetoothLeAdapter;
    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private BluetoothLeOptions options;

    private CloudPublisher cloudPublisher;

    public void setCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
    }

    public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = null;
    }

    public void setBluetoothLeService(BluetoothLeService bluetoothLeService) {
        this.bluetoothLeService = bluetoothLeService;
    }

    public void unsetBluetoothLeService(BluetoothLeService bluetoothLeService) {
        this.bluetoothLeService = null;
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
        if (this.bluetoothLeAdapter != null && this.bluetoothLeAdapter.isDiscovering()) {
            try {
                this.bluetoothLeAdapter.stopDiscovery();
            } catch (KuraException e) {
                logger.error(DISCOVERY_STOP_EX, e);
            }
        }

        // cancel a current worker handle if one is active
        if (this.handle != null) {
            this.handle.cancel(true);
        }

        // shutting down the worker and cleaning up the properties
        if (this.worker != null) {
            this.worker.shutdown();
        }

        // disconnect SensorTags
        for (TiSensorTag tiSensorTag : this.tiSensorTagList) {
            if (tiSensorTag != null && tiSensorTag.isConnected()) {
                try {
                    tiSensorTag.disconnect();
                } catch (KuraBluetoothConnectionException e) {
                    logger.error(CONNECTION_ERROR_EX, tiSensorTag.getBluetoothLeDevice().getAddress());
                }
            }
        }
        this.tiSensorTagList.clear();

        // cancel bluetoothAdapter
        this.bluetoothLeAdapter = null;
    }

    private void doUpdate(Map<String, Object> properties) {

        this.options = new BluetoothLeOptions(properties);
        if (this.options.isEnableScan()) {
            // re-create the worker
            this.worker = Executors.newSingleThreadScheduledExecutor();

            // Get Bluetooth adapter and ensure it is enabled
            this.bluetoothLeAdapter = this.bluetoothLeService.getAdapter(this.options.getIname());
            if (this.bluetoothLeAdapter != null) {
                logger.info("Bluetooth adapter interface => {}", this.options.getIname());
                if (!this.bluetoothLeAdapter.isPowered()) {
                    logger.info("Enabling bluetooth adapter...");
                    this.bluetoothLeAdapter.setPowered(true);
                    waitFor(1000);
                }
                logger.info("Bluetooth adapter address => {}", this.bluetoothLeAdapter.getAddress());

                this.handle = this.worker.scheduleAtFixedRate(this::performScan, 0, this.options.getPeriod(),
                        TimeUnit.SECONDS);
            } else {
                logger.info("Bluetooth adapter {} not found.", this.options.getIname());
            }
        }
    }

    void performScan() {
        // Scan for devices
        if (this.bluetoothLeAdapter.isDiscovering()) {
            try {
                this.bluetoothLeAdapter.stopDiscovery();
            } catch (KuraException e) {
                logger.error(DISCOVERY_STOP_EX, e);
            }
        }
        Future<List<BluetoothLeDevice>> future = this.bluetoothLeAdapter.findDevices(this.options.getScantime());
        try {
            filterDevices(future.get());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Scan for devices failed", e);
        }
        readSensorTags();
    }

    // --------------------------------------------------------------------
    //
    // Private Methods
    //
    // --------------------------------------------------------------------

    protected void doPublishKeys(String address, int key) {
        KuraPayload payload = new KuraPayload();
        payload.setTimestamp(new Date());
        payload.addMetric("key", key);

        Map<String, Object> properties = new HashMap<>();
        properties.put(ADDRESS_MESSAGE_PROP_KEY, address + "/keys");

        KuraMessage message = new KuraMessage(payload, properties);

        try {
            this.cloudPublisher.publish(message);
        } catch (Exception e) {
            logger.error("Can't publish message for buttons", e);
        }

    }

    private void doServicesDiscovery(TiSensorTag tiSensorTag) {
        logger.info("Starting services discovery...");
        for (Entry<String, BluetoothLeGattService> entry : tiSensorTag.discoverServices().entrySet()) {
            logger.info("Service {} {} ", entry.getKey(), entry.getValue().getUUID());
        }
    }

    private void doCharacteristicsDiscovery(TiSensorTag tiSensorTag) {
        for (BluetoothLeGattCharacteristic bgc : tiSensorTag.getCharacteristics()) {
            logger.info("Characteristics uuid : {}", bgc.getUUID());
        }
    }

    private boolean isSensorTagInList(String address) {
        boolean found = false;
        for (TiSensorTag tiSensorTag : this.tiSensorTagList) {
            if (tiSensorTag.getBluetoothLeDevice().getAddress().equals(address)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private void filterDevices(List<BluetoothLeDevice> devices) {
        // Scan for TI SensorTag
        for (BluetoothLeDevice bluetoothLeDevice : devices) {
            logger.info("Address {} Name {}", bluetoothLeDevice.getAddress(), bluetoothLeDevice.getName());

            if (bluetoothLeDevice.getName().replace(" ", "").contains("SensorTag")
                    && !isSensorTagInList(bluetoothLeDevice.getAddress())) {
                this.tiSensorTagList.add(new TiSensorTag(bluetoothLeDevice));
            }
        }
    }

    private void readSensorTags() {
        // connect to TiSensorTags
        for (TiSensorTag myTiSensorTag : this.tiSensorTagList) {
            try {
                if (!myTiSensorTag.isConnected()) {
                    logger.info("Connecting to TiSensorTag {}...", myTiSensorTag.getBluetoothLeDevice().getAddress());
                    myTiSensorTag.connect();
                }
                myTiSensorTag.init();

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

                if (this.options.isEnableButtons() && !myTiSensorTag.isKeysNotifying()) {
                    // For buttons only enable notifications
                    myTiSensorTag.enableKeysNotification(keys -> {
                        logger.info("Received key {}", keys);
                        doPublishKeys(myTiSensorTag.getBluetoothLeDevice().getAddress(), keys);
                    });
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
                if (!payload.metricNames().isEmpty()) {
                    publish(myTiSensorTag, payload);
                }

            } catch (KuraBluetoothConnectionException e) {
                logger.error(CONNECTION_ERROR_EX, myTiSensorTag.getBluetoothLeDevice().getAddress());
            }
        }
    }

    private void publish(TiSensorTag myTiSensorTag, KuraPayload payload) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ADDRESS_MESSAGE_PROP_KEY, myTiSensorTag.getBluetoothLeDevice().getAddress());

        KuraMessage message = new KuraMessage(payload, properties);
        try {
            this.cloudPublisher.publish(message);
        } catch (KuraException e) {
            logger.error("Publish message failed", e);
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
            myTiSensorTag.setGyroscopePeriod(50);
            byte[] config = { 0x07, 0x00 };
            myTiSensorTag.enableGyroscope(config);
        } else {
            byte[] config = { 0x07 };
            myTiSensorTag.enableGyroscope(config);
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
        myTiSensorTag.setMagnetometerPeriod(50);
        if (myTiSensorTag.isCC2650()) {
            byte[] config = { 0x40, 0x00 };
            myTiSensorTag.enableMagnetometer(config);
        } else {
            byte[] config = { 0x01 };
            myTiSensorTag.enableMagnetometer(config);
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
        myTiSensorTag.setAccelerometerPeriod(50);
        if (myTiSensorTag.isCC2650()) {
            byte[] config = { 0x38, 0x02 };
            myTiSensorTag.enableAccelerometer(config);
        } else {
            byte[] config = { 0x01 };
            myTiSensorTag.enableAccelerometer(config);
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

    protected static void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(INTERRUPTED_EX, e);
        }
    }

}
