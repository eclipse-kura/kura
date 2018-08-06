/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.example.beacon.scanner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothBeaconData;
import org.eclipse.kura.bluetooth.BluetoothBeaconScanListener;
import org.eclipse.kura.bluetooth.BluetoothService;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeaconScannerExample implements ConfigurableComponent, BluetoothBeaconScanListener {

    private static final Logger logger = LoggerFactory.getLogger(BeaconScannerExample.class);

    private static final String PROPERTY_ENABLE = "enableScanning";
    private static final String PROPERTY_INAME = "iname";
    private static final String PROPERTY_RATE_LIMIT = "rate_limit";
    private static final String PROPERTY_COMPANY_CODE = "companyCode";

    // Configurable State
    private String adapterName;		// eg. hci0
    private int rateLimit;			// eg. 5000ms
    private String companyCode;
    private Boolean enableScanning;

    // Internal State
    private BluetoothService bluetoothService;
    private BluetoothAdapter bluetoothAdapter;
    private Map<String, Long> publishTimes;

    private CloudPublisher cloudPublisher;

    // Services
    public void setBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    public void unsetBluetoothService(BluetoothService bluetoothService) {
        this.bluetoothService = null;
    }

    public void setCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = cloudPublisher;
    }

    public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
        this.cloudPublisher = null;
    }

    protected void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Activating Bluetooth Beacon Scanner example...");

        this.enableScanning = false;
        updated(properties);

        logger.info("Activating Bluetooth Beacon Scanner example...Done");

    }

    protected void deactivate(ComponentContext context) {

        logger.debug("Deactivating Beacon Scanner Example...");

        releaseResources();
        this.enableScanning = false;

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
            
            if (this.cloudPublisher == null) {
                logger.info("No cloud publisher selected. Cannot publish!");
                return;
            }

            // Publish the beacon data to the beacon's topic
            KuraPayload kp = new KuraPayload();
            kp.setTimestamp(new Date());
            kp.addMetric("uuid", beaconData.uuid);
            kp.addMetric("txpower", beaconData.txpower);
            kp.addMetric("rssi", beaconData.rssi);
            kp.addMetric("major", beaconData.major);
            kp.addMetric("minor", beaconData.minor);
            kp.addMetric("distance", calculateDistance(beaconData.rssi, beaconData.txpower));

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("address", beaconData.address);
            
            KuraMessage message = new KuraMessage(kp, properties);
            try {
                this.cloudPublisher.publish(message);
            } catch (KuraException e) {
                logger.error("Unable to publish", e);
            }
        }
    }
}
