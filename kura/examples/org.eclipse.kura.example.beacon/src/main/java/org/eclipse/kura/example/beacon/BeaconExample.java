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

    private static final Logger s_logger = LoggerFactory.getLogger(BeaconExample.class);

    private final String PROPERTY_ENABLE = "enableAdvertising";
    private final String PROPERTY_MIN_INTERVAL = "minBeaconInterval";
    private final String PROPERTY_MAX_INTERVAL = "maxBeaconInterval";
    private final String PROPERTY_UUID = "uuid";
    private final String PROPERTY_MAJOR = "major";
    private final String PROPERTY_MINOR = "minor";
    private final String PROPERTY_COMPANY = "companyCode";
    private final String PROPERTY_TX_POWER = "txPower";
    private final String PROPERTY_LIMITED = "LELimited";
    private final String PROPERTY_BR_SUPPORTED = "BR_EDRSupported";
    private final String PROPERTY_BR_CONTROLLER = "LE_BRController";
    private final String PROPERTY_BR_HOST = "LE_BRHost";
    private final String PROPERTY_INAME = "iname";

    private BluetoothService m_bluetoothService;
    private BluetoothAdapter m_bluetoothAdapter;

    private boolean m_enable;
    private Integer m_minInterval;
    private Integer m_maxInterval;
    private String m_uuid;
    private Integer m_major;
    private Integer m_minor;
    private String m_companyCode;
    private Integer m_txPower;
    private boolean m_LELimited;
    private boolean m_BRSupported;
    private boolean m_BRController;
    private boolean m_BRHost;
    private String m_iname = "hci0";

    public void setBluetoothService(BluetoothService bluetoothService) {
        this.m_bluetoothService = bluetoothService;
    }

    public void unsetBluetoothService(BluetoothService bluetoothService) {
        this.m_bluetoothService = null;
    }

    // --------------------------------------------------------------------
    //
    // Activation APIs
    //
    // --------------------------------------------------------------------
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        s_logger.info("Activating Bluetooth Beacon example...");

        if (properties != null) {
            if (properties.get(this.PROPERTY_ENABLE) != null) {
                this.m_enable = (Boolean) properties.get(this.PROPERTY_ENABLE);
            }
            if (properties.get(this.PROPERTY_MIN_INTERVAL) != null) {
                this.m_minInterval = (int) ((Integer) properties.get(this.PROPERTY_MIN_INTERVAL) / 0.625);
            }
            if (properties.get(this.PROPERTY_MAX_INTERVAL) != null) {
                this.m_maxInterval = (int) ((Integer) properties.get(this.PROPERTY_MAX_INTERVAL) / 0.625);
            }
            if (properties.get(this.PROPERTY_UUID) != null) {
                if (((String) properties.get(this.PROPERTY_UUID)).trim().replace("-", "").length() == 32) {
                    this.m_uuid = ((String) properties.get(this.PROPERTY_UUID)).replace("-", "");
                } else {
                    s_logger.warn("UUID is too short!");
                }
            }
            if (properties.get(this.PROPERTY_MAJOR) != null) {
                this.m_major = (Integer) properties.get(this.PROPERTY_MAJOR);
            }
            if (properties.get(this.PROPERTY_MINOR) != null) {
                this.m_minor = (Integer) properties.get(this.PROPERTY_MINOR);
            }
            if (properties.get(this.PROPERTY_COMPANY) != null) {
                this.m_companyCode = (String) properties.get(this.PROPERTY_COMPANY);
            }
            if (properties.get(this.PROPERTY_TX_POWER) != null) {
                this.m_txPower = (Integer) properties.get(this.PROPERTY_TX_POWER);
            }
            if (properties.get(this.PROPERTY_LIMITED) != null) {
                this.m_LELimited = (Boolean) properties.get(this.PROPERTY_LIMITED);
            }
            if (properties.get(this.PROPERTY_BR_SUPPORTED) != null) {
                this.m_BRSupported = (Boolean) properties.get(this.PROPERTY_BR_SUPPORTED);
            }
            if (properties.get(this.PROPERTY_BR_CONTROLLER) != null) {
                this.m_BRController = (Boolean) properties.get(this.PROPERTY_BR_CONTROLLER);
            }
            if (properties.get(this.PROPERTY_BR_HOST) != null) {
                this.m_BRHost = (Boolean) properties.get(this.PROPERTY_BR_HOST);
            }
            if (properties.get(this.PROPERTY_INAME) != null) {
                this.m_iname = (String) properties.get(this.PROPERTY_INAME);
            }
        }

        // Get Bluetooth adapter with Beacon capabilities and ensure it is enabled
        this.m_bluetoothAdapter = this.m_bluetoothService.getBluetoothAdapter(this.m_iname, this);
        if (this.m_bluetoothAdapter != null) {
            s_logger.info("Bluetooth adapter interface => " + this.m_iname);
            s_logger.info("Bluetooth adapter address => " + this.m_bluetoothAdapter.getAddress());
            s_logger.info("Bluetooth adapter le enabled => " + this.m_bluetoothAdapter.isLeReady());

            if (!this.m_bluetoothAdapter.isEnabled()) {
                s_logger.info("Enabling bluetooth adapter...");
                this.m_bluetoothAdapter.enable();
                s_logger.info("Bluetooth adapter address => " + this.m_bluetoothAdapter.getAddress());
            }

            configureBeacon();

        } else {
            s_logger.warn("No Bluetooth adapter found ...");
        }

    }

    protected void deactivate(ComponentContext context) {

        s_logger.debug("Deactivating Beacon Example...");

        // Stop the advertising
        this.m_bluetoothAdapter.stopBeaconAdvertising();

        // cancel bluetoothAdapter
        this.m_bluetoothAdapter = null;

        s_logger.debug("Deactivating Beacon Example... Done.");
    }

    protected void updated(Map<String, Object> properties) {

        if (properties != null) {
            if (properties.get(this.PROPERTY_ENABLE) != null) {
                this.m_enable = (Boolean) properties.get(this.PROPERTY_ENABLE);
            }
            if (properties.get(this.PROPERTY_MIN_INTERVAL) != null) {
                this.m_minInterval = (int) ((Integer) properties.get(this.PROPERTY_MIN_INTERVAL) / 0.625);
            }
            if (properties.get(this.PROPERTY_MAX_INTERVAL) != null) {
                this.m_maxInterval = (int) ((Integer) properties.get(this.PROPERTY_MAX_INTERVAL) / 0.625);
            }
            if (properties.get(this.PROPERTY_UUID) != null) {
                if (((String) properties.get(this.PROPERTY_UUID)).trim().replace("-", "").length() == 32) {
                    this.m_uuid = ((String) properties.get(this.PROPERTY_UUID)).replace("-", "");
                } else {
                    s_logger.warn("UUID is too short!");
                }
            }
            if (properties.get(this.PROPERTY_MAJOR) != null) {
                this.m_major = (Integer) properties.get(this.PROPERTY_MAJOR);
            }
            if (properties.get(this.PROPERTY_MINOR) != null) {
                this.m_minor = (Integer) properties.get(this.PROPERTY_MINOR);
            }
            if (properties.get(this.PROPERTY_COMPANY) != null) {
                this.m_companyCode = (String) properties.get(this.PROPERTY_COMPANY);
            }
            if (properties.get(this.PROPERTY_TX_POWER) != null) {
                this.m_txPower = (Integer) properties.get(this.PROPERTY_TX_POWER);
            }
            if (properties.get(this.PROPERTY_LIMITED) != null) {
                this.m_LELimited = (Boolean) properties.get(this.PROPERTY_LIMITED);
            }
            if (properties.get(this.PROPERTY_BR_SUPPORTED) != null) {
                this.m_BRSupported = (Boolean) properties.get(this.PROPERTY_BR_SUPPORTED);
            }
            if (properties.get(this.PROPERTY_BR_CONTROLLER) != null) {
                this.m_BRController = (Boolean) properties.get(this.PROPERTY_BR_CONTROLLER);
            }
            if (properties.get(this.PROPERTY_BR_HOST) != null) {
                this.m_BRHost = (Boolean) properties.get(this.PROPERTY_BR_HOST);
            }
            if (properties.get(this.PROPERTY_INAME) != null) {
                this.m_iname = (String) properties.get(this.PROPERTY_INAME);
            }
        }

        // Stop the advertising
        this.m_bluetoothAdapter.stopBeaconAdvertising();

        // cancel bluetoothAdapter
        this.m_bluetoothAdapter = null;

        // Get Bluetooth adapter and ensure it is enabled
        this.m_bluetoothAdapter = this.m_bluetoothService.getBluetoothAdapter(this.m_iname, this);
        if (this.m_bluetoothAdapter != null) {
            s_logger.info("Bluetooth adapter interface => " + this.m_iname);
            s_logger.info("Bluetooth adapter address => " + this.m_bluetoothAdapter.getAddress());
            s_logger.info("Bluetooth adapter le enabled => " + this.m_bluetoothAdapter.isLeReady());

            if (!this.m_bluetoothAdapter.isEnabled()) {
                s_logger.info("Enabling bluetooth adapter...");
                this.m_bluetoothAdapter.enable();
                s_logger.info("Bluetooth adapter address => " + this.m_bluetoothAdapter.getAddress());
            }

            configureBeacon();

        } else {
            s_logger.warn("No Bluetooth adapter found ...");
        }

        s_logger.debug("Updating Beacon Example... Done.");
    }

    // --------------------------------------------------------------------
    //
    // Private methods
    //
    // --------------------------------------------------------------------

    private void configureBeacon() {

        if (this.m_enable) {

            if (this.m_minInterval != null && this.m_maxInterval != null) {
                this.m_bluetoothAdapter.setBeaconAdvertisingInterval(this.m_minInterval, this.m_maxInterval);
            }

            this.m_bluetoothAdapter.startBeaconAdvertising();

            if (this.m_uuid != null && this.m_major != null && this.m_minor != null && this.m_companyCode != null
                    && this.m_txPower != null) {
                this.m_bluetoothAdapter.setBeaconAdvertisingData(this.m_uuid, this.m_major, this.m_minor,
                        this.m_companyCode, this.m_txPower, this.m_LELimited, this.m_LELimited ? false : true,
                        this.m_BRSupported, this.m_BRController, this.m_BRHost);
            }

        } else {
            this.m_bluetoothAdapter.stopBeaconAdvertising();
        }
    }

    // --------------------------------------------------------------------
    //
    // BluetoothBeaconCommandListener APIs
    //
    // --------------------------------------------------------------------
    @Override
    public void onCommandFailed(String errorCode) {
        s_logger.warn("Error in executing command. Error Code: " + errorCode);
    }

    @Override
    public void onCommandResults(String results) {
        s_logger.info("Command results : " + results);
    }

}
