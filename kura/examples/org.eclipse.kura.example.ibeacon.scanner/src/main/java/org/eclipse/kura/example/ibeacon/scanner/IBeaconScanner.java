/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.example.ibeacon.scanner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeaconService;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IBeaconScanner implements ConfigurableComponent, BluetoothLeBeaconListener<BluetoothLeIBeacon> {

    private static final String ADDRESS_MESSAGE_PROP_KEY = "address";

    private static final Logger logger = LoggerFactory.getLogger(IBeaconScanner.class);

    private ExecutorService worker;
    private Future<?> handle;

    private BluetoothLeService bluetoothLeService;
    private BluetoothLeIBeaconService bluetoothLeIBeaconService;
    private BluetoothLeBeaconScanner<BluetoothLeIBeacon> bluetoothLeIBeaconScanner;
    private Map<String, Long> publishTimes;
    private IBeaconScannerOptions options;

    private CloudPublisher cloudPublisher;

    public void setBluetoothLeService(BluetoothLeService bluetoothLeService) {
        this.bluetoothLeService = bluetoothLeService;
    }

    public void unsetBluetoothLeService(BluetoothLeService bluetoothLeService) {
        this.bluetoothLeService = null;
    }

    public void setBluetoothLeIBeaconService(BluetoothLeIBeaconService bluetoothLeIBeaconService) {
        this.bluetoothLeIBeaconService = bluetoothLeIBeaconService;
    }

    public void unsetBluetoothLeIBeaconService(BluetoothLeIBeaconService bluetoothLeIBeaconService) {
        this.bluetoothLeIBeaconService = null;
    }

    public void setCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
    }

    public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = null;
    }

    protected void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Activating Bluetooth iBeacon Scanner example...");

        this.publishTimes = new HashMap<>();
        doUpdate(properties);
        logger.info("Activating Bluetooth iBeacon Scanner example...Done");
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating iBeacon Scanner Example...");

        releaseResources();

        if (this.handle != null) {
            this.handle.cancel(true);
        }

        if (this.worker != null) {
            this.worker.shutdown();
        }

        logger.debug("Deactivating iBeacon Scanner Example... Done.");
    }

    protected void updated(Map<String, Object> properties) {
        logger.debug("Updating iBeacon Scanner Example...");

        releaseResources();

        if (this.handle != null) {
            this.handle.cancel(true);
        }

        if (this.worker != null) {
            this.worker.shutdown();
        }

        doUpdate(properties);

        logger.debug("Updating iBeacon Scanner Example... Done");
    }

    private void doUpdate(Map<String, Object> properties) {
        this.options = new IBeaconScannerOptions(properties);

        if (this.options.isEnabled()) {
            this.worker = Executors.newSingleThreadExecutor();
            this.handle = this.worker.submit(this::setup);
        }
    }

    private void setup() {
        BluetoothLeAdapter bluetoothLeAdapter = this.bluetoothLeService.getAdapter(this.options.getAdapterName());
        if (bluetoothLeAdapter != null) {
            if (!bluetoothLeAdapter.isPowered()) {
                bluetoothLeAdapter.setPowered(true);
            }
            this.bluetoothLeIBeaconScanner = this.bluetoothLeIBeaconService.newBeaconScanner(bluetoothLeAdapter);
            this.bluetoothLeIBeaconScanner.addBeaconListener(this);
            try {
                this.bluetoothLeIBeaconScanner.startBeaconScan(this.options.getScanDuration());
            } catch (KuraException e) {
                logger.error("iBeacon scanning failed", e);
            }
        } else {
            logger.warn("No Bluetooth adapter found ...");
        }
    }

    private void releaseResources() {
        if (this.bluetoothLeIBeaconScanner != null) {
            if (this.bluetoothLeIBeaconScanner.isScanning()) {
                this.bluetoothLeIBeaconScanner.stopBeaconScan();
            }
            this.bluetoothLeIBeaconScanner.removeBeaconListener(this);
            this.bluetoothLeIBeaconService.deleteBeaconScanner(this.bluetoothLeIBeaconScanner);
        }
    }

    private double calculateDistance(int rssi, int txpower) {

        int ratioDB = txpower - rssi;
        double ratioLinear = Math.pow(10, (double) ratioDB / 10);
        return Math.sqrt(ratioLinear);
    }

    @Override
    public void onBeaconsReceived(BluetoothLeIBeacon iBeacon) {
        logger.info("iBeacon received from {}", iBeacon.getAddress());
        logger.info("UUID : {}", iBeacon.getUuid());
        logger.info("Major : {}", iBeacon.getMajor());
        logger.info("Minor : {}", iBeacon.getMinor());
        logger.info("TxPower : {}", iBeacon.getTxPower());
        logger.info("RSSI : {}", iBeacon.getRssi());
        long now = System.currentTimeMillis();

        Long lastPublishTime = this.publishTimes.get(iBeacon.getAddress());

        // If this beacon is new, or it last published more than 'rateLimit' seconds ago
        if (lastPublishTime == null || now - lastPublishTime > this.options.getPublishPeriod() * 1000L) {

            // Store the publish time against the address
            this.publishTimes.put(iBeacon.getAddress(), now);

            if (this.cloudPublisher == null) {
                logger.info("No cloud publisher selected. Cannot publish!");
                return;
            }

            // Publish the beacon data to the beacon's topic
            KuraPayload kp = new KuraPayload();
            kp.setTimestamp(new Date());
            kp.addMetric("uuid", iBeacon.getUuid().toString());
            kp.addMetric("txpower", (int) iBeacon.getTxPower());
            kp.addMetric("rssi", iBeacon.getRssi());
            kp.addMetric("major", (int) iBeacon.getMajor());
            kp.addMetric("minor", (int) iBeacon.getMinor());
            kp.addMetric("distance", calculateDistance(iBeacon.getRssi(), iBeacon.getTxPower()));

            Map<String, Object> properties = new HashMap<>();
            properties.put(ADDRESS_MESSAGE_PROP_KEY, iBeacon.getAddress());

            KuraMessage message = new KuraMessage(kp, properties);
            try {
                this.cloudPublisher.publish(message);
            } catch (KuraException e) {
                logger.error("Unable to publish", e);
            }
        }
    }
}
