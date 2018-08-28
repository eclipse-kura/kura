/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.example.ibeacon.advertiser;

import java.util.Map;

import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeaconService;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IBeaconAdvertiser implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(IBeaconAdvertiser.class);

    private BluetoothLeService bluetoothLeService;
    private BluetoothLeAdapter bluetoothLeAdapter;
    private BluetoothLeIBeaconService bluetoothLeIBeaconService;
    private BluetoothLeBeaconAdvertiser<BluetoothLeIBeacon> advertising;
    private IBeaconAdvertiserOptions options;

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

    // --------------------------------------------------------------------
    //
    // Activation APIs
    //
    // --------------------------------------------------------------------
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Activating Bluetooth iBeacon example...");

        update(properties);

        logger.debug("Activating iBeacon Example... Done.");

    }

    protected void deactivate(ComponentContext context) {

        logger.debug("Deactivating iBeacon Example...");

        // Stop the advertising
        if (this.advertising != null) {
            try {
                this.advertising.stopBeaconAdvertising();
                this.bluetoothLeIBeaconService.deleteBeaconAdvertiser(this.advertising);
            } catch (KuraException e) {
                logger.error("Stop iBeacon advertising failed", e);
            }
        }

        // cancel bluetoothAdapter
        this.bluetoothLeAdapter = null;

        logger.debug("Deactivating iBeacon Example... Done.");
    }

    protected void updated(Map<String, Object> properties) {
        logger.info("Updating Bluetooth iBeacon example...");

        update(properties);

        logger.debug("Updating iBeacon Example... Done.");
    }

    // --------------------------------------------------------------------
    //
    // Private methods
    //
    // --------------------------------------------------------------------

    private void update(Map<String, Object> properties) {
        this.options = new IBeaconAdvertiserOptions(properties);

        // Stop the advertising
        if (this.advertising != null) {
            try {
                this.advertising.stopBeaconAdvertising();
                this.bluetoothLeIBeaconService.deleteBeaconAdvertiser(this.advertising);
            } catch (KuraException e) {
                logger.error("Stop iBeacon advertising failed", e);
            }
        }

        // cancel bluetoothAdapter
        this.bluetoothLeAdapter = null;

        // Get Bluetooth adapter with Beacon capabilities and ensure it is enabled
        if (this.options.isEnabled()) {
            this.bluetoothLeAdapter = this.bluetoothLeService.getAdapter(this.options.getIname());
            if (this.bluetoothLeAdapter != null) {
                logger.info("Bluetooth adapter interface => {}", this.options.getIname());
                logger.info("Bluetooth adapter address => {}", this.bluetoothLeAdapter.getAddress());

                if (!this.bluetoothLeAdapter.isPowered()) {
                    logger.info("Enabling bluetooth adapter...");
                    this.bluetoothLeAdapter.setPowered(true);
                }

                try {
                    this.advertising = this.bluetoothLeIBeaconService.newBeaconAdvertiser(this.bluetoothLeAdapter);
                    configureBeacon();
                } catch (KuraBluetoothBeaconAdvertiserNotAvailable e) {
                    logger.error("Beacon Advertiser not available on {}", this.bluetoothLeAdapter.getInterfaceName(),
                            e);
                }

            } else {
                logger.warn("No Bluetooth adapter found ...");
            }
        }
    }

    private void configureBeacon() {

        try {
            BluetoothLeIBeacon iBeacon = new BluetoothLeIBeacon(this.options.getUUID(),
                    this.options.getMajor().shortValue(), this.options.getMinor().shortValue(),
                    this.options.getTxPower().shortValue());
            this.advertising.updateBeaconAdvertisingData(iBeacon);
            this.advertising.updateBeaconAdvertisingInterval(this.options.getMinInterval(),
                    this.options.getMaxInterval());

            this.advertising.startBeaconAdvertising();
        } catch (KuraException e) {
            logger.error("IBeacon configuration failed", e);
        }
    }

}
