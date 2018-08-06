/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.example.eddystone.scanner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystoneService;
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

public class EddystoneScanner implements ConfigurableComponent, BluetoothLeBeaconListener<BluetoothLeEddystone> {

    private static final String ADDRESS_MESSAGE_PROP_KEY = "address";

    private static final Logger logger = LoggerFactory.getLogger(EddystoneScanner.class);

    private ExecutorService worker;
    private Future<?> handle;

    private BluetoothLeService bluetoothLeService;
    private BluetoothLeEddystoneService bluetoothLeEddystoneService;
    private BluetoothLeBeaconScanner<BluetoothLeEddystone> bluetoothLeEddystoneScanner;
    private Map<String, Long> publishTimes;
    private EddystoneScannerOptions options;

    private CloudPublisher cloudPublisher;

    public void setBluetoothLeService(BluetoothLeService bluetoothLeService) {
        this.bluetoothLeService = bluetoothLeService;
    }

    public void unsetBluetoothLeService(BluetoothLeService bluetoothLeService) {
        this.bluetoothLeService = null;
    }

    public void setBluetoothLeEddystoneService(BluetoothLeEddystoneService bluetoothLeEddystoneService) {
        this.bluetoothLeEddystoneService = bluetoothLeEddystoneService;
    }

    public void unsetBluetoothLeEddystoneService(BluetoothLeEddystoneService bluetoothLeEddystoneService) {
        this.bluetoothLeEddystoneService = null;
    }

    public void setCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
    }

    public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = null;
    }

    protected void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Activating Bluetooth Eddystone Scanner example...");

        this.publishTimes = new HashMap<>();
        doUpdate(properties);
        logger.info("Activating Bluetooth Eddystone Scanner example...Done");
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Eddystone Scanner Example...");

        releaseResources();

        if (this.handle != null) {
            this.handle.cancel(true);
        }

        if (this.worker != null) {
            this.worker.shutdown();
        }

        logger.debug("Deactivating Eddystone Scanner Example... Done.");
    }

    protected void updated(Map<String, Object> properties) {
        logger.debug("Updating Eddystone Scanner Example...");

        releaseResources();

        if (this.handle != null) {
            this.handle.cancel(true);
        }

        if (this.worker != null) {
            this.worker.shutdown();
        }

        doUpdate(properties);

        logger.debug("Updating Eddystone Scanner Example... Done");
    }

    private void doUpdate(Map<String, Object> properties) {
        this.options = new EddystoneScannerOptions(properties);

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
            this.bluetoothLeEddystoneScanner = this.bluetoothLeEddystoneService.newBeaconScanner(bluetoothLeAdapter);
            this.bluetoothLeEddystoneScanner.addBeaconListener(this);
            try {
                this.bluetoothLeEddystoneScanner.startBeaconScan(this.options.getScanDuration());
            } catch (KuraException e) {
                logger.error("iBeacon scanning failed", e);
            }
        } else {
            logger.warn("No Bluetooth adapter found ...");
        }
    }

    private void releaseResources() {
        if (this.bluetoothLeEddystoneScanner != null) {
            if (this.bluetoothLeEddystoneScanner.isScanning()) {
                this.bluetoothLeEddystoneScanner.stopBeaconScan();
            }
            this.bluetoothLeEddystoneScanner.removeBeaconListener(this);
            this.bluetoothLeEddystoneService.deleteBeaconScanner(this.bluetoothLeEddystoneScanner);
        }
    }

    private double calculateDistance(int rssi, int txpower) {

        int ratioDB = txpower - rssi;
        double ratioLinear = Math.pow(10, (double) ratioDB / 10);
        return Math.sqrt(ratioLinear);
    }

    @Override
    public void onBeaconsReceived(BluetoothLeEddystone eddystone) {
        logger.info("Eddystone {} received from {}", eddystone.getFrameType(), eddystone.getAddress());
        if ("UID".equals(eddystone.getFrameType())) {
            logger.info("Namespace : {}", bytesArrayToHexString(eddystone.getNamespace()));
            logger.info("Instance : {}", bytesArrayToHexString(eddystone.getInstance()));
        } else if ("URL".equals(eddystone.getFrameType())) {
            logger.info("URL : {}", eddystone.getUrlScheme() + eddystone.getUrl());
        }
        logger.info("TxPower : {}", eddystone.getTxPower());
        logger.info("RSSI : {}", eddystone.getRssi());
        long now = System.currentTimeMillis();

        Long lastPublishTime = this.publishTimes.get(eddystone.getAddress());

        // If this beacon is new, or it last published more than 'publish.period' seconds ago
        if (lastPublishTime == null || now - lastPublishTime > this.options.getPublishPeriod() * 1000L) {

            // Store the publish time against the address
            this.publishTimes.put(eddystone.getAddress(), now);

            if (this.cloudPublisher == null) {
                logger.info("No cloud publisher selected. Cannot publish!");
                return;
            }
            
            // Publish the beacon data to the beacon's topic
            KuraPayload kp = new KuraPayload();
            kp.setTimestamp(new Date());
            kp.addMetric("type", eddystone.getFrameType());
            if ("UID".equals(eddystone.getFrameType())) {
                kp.addMetric("namespace", bytesArrayToHexString(eddystone.getNamespace()));
                kp.addMetric("instance", bytesArrayToHexString(eddystone.getInstance()));
            } else if ("URL".equals(eddystone.getFrameType())) {
                kp.addMetric("URL", eddystone.getUrl());
            }
            kp.addMetric("txpower", (int) eddystone.getTxPower());
            kp.addMetric("rssi", eddystone.getRssi());
            kp.addMetric("distance", calculateDistance(eddystone.getRssi(), eddystone.getTxPower()));

            Map<String, Object> properties = new HashMap<>();
            properties.put(ADDRESS_MESSAGE_PROP_KEY, eddystone.getAddress());

            KuraMessage message = new KuraMessage(kp, properties);

            try {
                this.cloudPublisher.publish(message);
            } catch (KuraException e) {
                logger.error("Unable to publish", e);
            }
        }
    }

    private static String bytesArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
