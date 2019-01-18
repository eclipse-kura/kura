/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.beacon;

import java.util.Map;

import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothBeaconCommandListener;
import org.eclipse.kura.bluetooth.BluetoothService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeaconExample implements ConfigurableComponent, BluetoothBeaconCommandListener {

    private static final Logger logger = LoggerFactory.getLogger(BeaconExample.class);

    private static final String PROPERTY_ENABLE = "enableAdvertising";
    private static final String PROPERTY_MIN_INTERVAL = "minBeaconInterval";
    private static final String PROPERTY_MAX_INTERVAL = "maxBeaconInterval";
    private static final String PROPERTY_UUID = "uuid";
    private static final String PROPERTY_MAJOR = "major";
    private static final String PROPERTY_MINOR = "minor";
    private static final String PROPERTY_COMPANY = "companyCode";
    private static final String PROPERTY_TX_POWER = "txPower";
    private static final String PROPERTY_LIMITED = "LELimited";
    private static final String PROPERTY_BR_SUPPORTED = "BR_EDRSupported";
    private static final String PROPERTY_BR_CONTROLLER = "LE_BRController";
    private static final String PROPERTY_BR_HOST = "LE_BRHost";
    private static final String PROPERTY_INAME = "iname";

    private static final int PROPERTY_MAJOR_MAX = 65535;
    private static final int PROPERTY_MAJOR_MIN = 0;
    private static final int PROPERTY_MINOR_MAX = 65535;
    private static final int PROPERTY_MINOR_MIN = 0;
    private static final short PROPERTY_TX_POWER_MAX = 126;
    private static final short PROPERTY_TX_POWER_MIN = -127;

    private BluetoothService bluetoothService;
    private BluetoothAdapter bluetoothAdapter;

    private boolean enable;
    private Integer minInterval;
    private Integer maxInterval;
    private String uuid;
    private Integer major;
    private Integer minor;
    private String companyCode;
    private Integer txPower;
    private boolean leLimited;
    private boolean brSupported;
    private boolean brController;
    private boolean brHost;
    private String name = "hci0";

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
        logger.info("Activating Bluetooth Beacon example...");

        doUpdate(properties);

        // Get Bluetooth adapter with Beacon capabilities and ensure it is enabled
        this.bluetoothAdapter = this.bluetoothService.getBluetoothAdapter(this.name, this);
        if (this.bluetoothAdapter != null) {
            logger.info("Bluetooth adapter interface => {}", this.name);
            logger.info("Bluetooth adapter address => {}", this.bluetoothAdapter.getAddress());
            logger.info("Bluetooth adapter le enabled => {}", this.bluetoothAdapter.isLeReady());

            if (!this.bluetoothAdapter.isEnabled()) {
                logger.info("Enabling bluetooth adapter...");
                this.bluetoothAdapter.enable();
                logger.info("Bluetooth adapter address => {}", this.bluetoothAdapter.getAddress());
            }

            configureBeacon();

        } else {
            logger.warn("No Bluetooth adapter found ...");
        }

    }

    protected void deactivate(ComponentContext context) {

        logger.debug("Deactivating Beacon Example...");

        // Stop the advertising
        this.bluetoothAdapter.stopBeaconAdvertising();

        // cancel bluetoothAdapter
        this.bluetoothAdapter = null;

        logger.debug("Deactivating Beacon Example... Done.");
    }

    protected void updated(Map<String, Object> properties) {

        doUpdate(properties);

        // Stop the advertising
        this.bluetoothAdapter.stopBeaconAdvertising();

        // cancel bluetoothAdapter
        this.bluetoothAdapter = null;

        // Get Bluetooth adapter and ensure it is enabled
        this.bluetoothAdapter = this.bluetoothService.getBluetoothAdapter(this.name, this);
        if (this.bluetoothAdapter != null) {
            logger.info("Bluetooth adapter interface => {}", this.name);
            logger.info("Bluetooth adapter address => {}", this.bluetoothAdapter.getAddress());
            logger.info("Bluetooth adapter le enabled => {}", this.bluetoothAdapter.isLeReady());

            if (!this.bluetoothAdapter.isEnabled()) {
                logger.info("Enabling bluetooth adapter...");
                this.bluetoothAdapter.enable();
                logger.info("Bluetooth adapter address => {}", this.bluetoothAdapter.getAddress());
            }

            configureBeacon();

        } else {
            logger.warn("No Bluetooth adapter found ...");
        }

        logger.debug("Updating Beacon Example... Done.");
    }

    private void doUpdate(Map<String, Object> properties) {
        if (properties != null) {
            if (properties.get(PROPERTY_ENABLE) != null) {
                this.enable = (Boolean) properties.get(PROPERTY_ENABLE);
            }
            if (properties.get(PROPERTY_MIN_INTERVAL) != null) {
                this.minInterval = (int) ((Integer) properties.get(PROPERTY_MIN_INTERVAL) / 0.625);
            }
            if (properties.get(PROPERTY_MAX_INTERVAL) != null) {
                this.maxInterval = (int) ((Integer) properties.get(PROPERTY_MAX_INTERVAL) / 0.625);
            }
            if (properties.get(PROPERTY_UUID) != null) {
                if (((String) properties.get(PROPERTY_UUID)).trim().replace("-", "").length() == 32) {
                    this.uuid = ((String) properties.get(PROPERTY_UUID)).replace("-", "");
                    if (!this.uuid.matches("^[0-9a-fA-F]+$")) {
                        logger.warn("UUID contains invalid value!");
                        this.uuid = null;
                    }
                } else {
                    logger.warn("UUID is too short or too long!");
                }
            }
            if (properties.get(PROPERTY_MAJOR) != null) {
                this.major = setInRange((int) properties.get(PROPERTY_MAJOR), PROPERTY_MAJOR_MAX, PROPERTY_MAJOR_MIN);
            }
            if (properties.get(PROPERTY_MINOR) != null) {
                this.minor = setInRange((int) properties.get(PROPERTY_MINOR), PROPERTY_MINOR_MAX, PROPERTY_MINOR_MIN);
            }
            if (properties.get(PROPERTY_COMPANY) != null) {
                this.companyCode = (String) properties.get(PROPERTY_COMPANY);
            }
            if (properties.get(PROPERTY_TX_POWER) != null) {
                this.txPower = setInRange((int) properties.get(PROPERTY_TX_POWER), PROPERTY_TX_POWER_MAX,
                        PROPERTY_TX_POWER_MIN);
            }
            if (properties.get(PROPERTY_LIMITED) != null) {
                this.leLimited = (Boolean) properties.get(PROPERTY_LIMITED);
            }
            if (properties.get(PROPERTY_BR_SUPPORTED) != null) {
                this.brSupported = (Boolean) properties.get(PROPERTY_BR_SUPPORTED);
            }
            if (properties.get(PROPERTY_BR_CONTROLLER) != null) {
                this.brController = (Boolean) properties.get(PROPERTY_BR_CONTROLLER);
            }
            if (properties.get(PROPERTY_BR_HOST) != null) {
                this.brHost = (Boolean) properties.get(PROPERTY_BR_HOST);
            }
            if (properties.get(PROPERTY_INAME) != null) {
                this.name = (String) properties.get(PROPERTY_INAME);
            }
        }
    }

    // --------------------------------------------------------------------
    //
    // Private methods
    //
    // --------------------------------------------------------------------

    private void configureBeacon() {

        if (this.enable) {

            if (this.minInterval != null && this.maxInterval != null) {
                this.bluetoothAdapter.setBeaconAdvertisingInterval(this.minInterval, this.maxInterval);
            }

            this.bluetoothAdapter.startBeaconAdvertising();

            if (this.uuid != null && this.major != null && this.minor != null && this.companyCode != null
                    && this.txPower != null) {
                this.bluetoothAdapter.setBeaconAdvertisingData(this.uuid, this.major, this.minor, this.companyCode,
                        this.txPower, this.leLimited, this.leLimited ? false : true, this.brSupported,
                        this.brController, this.brHost);
            }

        } else {
            this.bluetoothAdapter.stopBeaconAdvertising();
        }
    }

    private Integer setInRange(int value, int max, int min) {
        if (value <= max && value >= min) {
            return value;
        } else {
            return (value > max) ? max : min;
        }
    }

    // --------------------------------------------------------------------
    //
    // BluetoothBeaconCommandListener APIs
    //
    // --------------------------------------------------------------------
    @Override
    public void onCommandFailed(String errorCode) {
        logger.warn("Error in executing command. Error Code: {}", errorCode);
    }

    @Override
    public void onCommandResults(String results) {
        logger.info("Command results : {}", results);
    }

}
