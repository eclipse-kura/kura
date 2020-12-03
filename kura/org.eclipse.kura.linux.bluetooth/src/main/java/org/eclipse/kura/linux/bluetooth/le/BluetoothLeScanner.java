/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.linux.bluetooth.le;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.bluetooth.BluetoothBeaconData;
import org.eclipse.kura.bluetooth.BluetoothBeaconScanListener;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothLeScanListener;
import org.eclipse.kura.bluetooth.listener.BluetoothAdvertisementData;
import org.eclipse.kura.bluetooth.listener.BluetoothAdvertisementScanListener;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.bluetooth.BluetoothDeviceImpl;
import org.eclipse.kura.linux.bluetooth.util.BTSnoopListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcess;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcessListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeScanner implements BluetoothProcessListener, BTSnoopListener {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLeScanner.class);
    private static final String MAC_REGEX = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";

    public static final int SCAN_FAILED_INTERNAL_ERROR = 0x0003;

    private final Map<String, String> devices;
    private List<BluetoothDevice> scanResult;
    private BluetoothProcess proc = null;
    private BluetoothProcess dumpProc = null;
    private BluetoothLeScanListener listener = null;
    private BluetoothBeaconScanListener beaconListener = null;
    private BluetoothAdvertisementScanListener advertisementListener = null;
    private boolean scanRunning = false;
    private String companyName;
    private final CommandExecutorService executorService;

    public BluetoothLeScanner(CommandExecutorService executorService) {
        this.devices = new HashMap<>();
        this.executorService = executorService;
    }

    public void startScan(String name, BluetoothLeScanListener listener) {
        this.listener = listener;

        logger.info("Starting bluetooth le scan...");

        // Start scan process
        try {
            this.proc = BluetoothUtil.hcitoolCmd(name, "lescan", this, this.executorService);
            setScanRunning(true);
        } catch (IOException e) {
            logger.error("Failed to start device scan", e);
        }
    }

    public void startAdvertisementScan(String name, String companyName, BluetoothAdvertisementScanListener listener) {
        this.advertisementListener = listener;
        this.companyName = companyName;

        logger.info("Starting bluetooth le advertisement scan...");

        try {
            // Start scan process
            this.proc = BluetoothUtil.hcitoolCmd(name, new String[] { "lescan-passive", "--duplicates" }, this,
                    this.executorService);
            // Start dump process
            this.dumpProc = BluetoothUtil.btdumpCmd(name, this, this.executorService);
            setScanRunning(true);
        } catch (IOException e) {
            logger.error("Failed to start advertisement scan", e);
        }
    }

    public void startBeaconScan(String name, String companyName, BluetoothBeaconScanListener listener) {
        this.beaconListener = listener;
        this.companyName = companyName;

        logger.info("Starting bluetooth le beacon scan...");

        // Start scan process
        try {
            this.proc = BluetoothUtil.hcitoolCmd(name, new String[] { "lescan-passive", "--duplicates" }, this,
                    this.executorService);
            // Start dump process
            this.dumpProc = BluetoothUtil.btdumpCmd(name, this, this.executorService);
            setScanRunning(true);
        } catch (IOException e) {
            logger.error("Failed to start beacon scan", e);
        }
    }

    public void killScan(String name) {
        // Shut down hcitool process
        if (this.proc != null) {
            logger.info("Killing hcitool...");
            if (!BluetoothUtil.stopHcitool(name, this.executorService, "")) {
                logger.info("Cannot kill hcitool process...");
            }
            this.proc.destroy();
        } else {
            logger.info("Cannot destroy hcitool process...");
        }

        // Shut down btdump process
        if (this.dumpProc != null) {
            logger.info("Killing btdump...");
            BluetoothUtil.stopBtdump(name, this.executorService);
            BluetoothUtil.killCmd(new String[] { "hcidump", "-i", name }, LinuxSignal.SIGINT, this.executorService);
            this.dumpProc.destroyBTSnoop();
        } else {
            logger.info("Cannot destroy btdump process...");
        }

        setScanRunning(false);
    }

    // --------------------------------------------------------------------
    //
    // BluetoothProcessListener API
    //
    // --------------------------------------------------------------------
    @Override
    public void processInputStream(String string) {

        if (this.listener != null) {
            String[] lines = string.split("\n");
            for (String line : lines) {
                processLine(line);
            }

            this.scanResult = new ArrayList<>();
            for (Entry<String, String> device : this.devices.entrySet()) {
                this.scanResult.add(new BluetoothDeviceImpl(device.getKey(), device.getValue(), this.executorService));
                logger.info("scanResult.add {} - {}", device.getKey(), device.getValue());
            }

            // Alert listener that scan is complete
            this.listener.onScanResults(this.scanResult);
        }
    }

    @Override
    public void processInputStream(int ch) {
    }

    @Override
    public void processBTSnoopRecord(byte[] record) {

        try {

            // Extract raw advertisement data
            BluetoothAdvertisementData bAdData = BluetoothUtil.parseLEAdvertisement(record);

            // Notify advertisement listeners
            if (bAdData != null && this.advertisementListener != null) {
                try {
                    this.advertisementListener.onAdvertisementDataReceived(bAdData);
                } catch (Exception e) {
                    logger.error("Scan listener threw exception", e);
                }
            }

            // Extract beacon advertisements
            List<BluetoothBeaconData> beaconDatas = BluetoothUtil.parseLEAdvertisingReport(record, this.companyName);

            // Extract beacon data
            for (BluetoothBeaconData beaconData : beaconDatas) {

                // Notify the listener
                try {

                    if (this.beaconListener != null) {
                        this.beaconListener.onBeaconDataReceived(beaconData);
                    }

                } catch (Exception e) {
                    logger.error("Scan listener threw exception", e);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing advertising report", e);
        }

    }

    @Override
    public void processErrorStream(String string) {
    }

    // --------------------------------------------------------------------
    //
    // Private methods
    //
    // --------------------------------------------------------------------
    private void processLine(String line) {
        String name;
        String address;
        logger.info(line);
        if (line.contains("Set scan parameters failed:")) {
            logger.error("Error : {}", line);
        } else {
            // Results from hcitool lescan should be in form:
            // <mac_address> <device_name>
            String[] results = line.split("\\s", 2);
            if (results.length == 2) {
                address = results[0].trim();
                name = results[1].trim();

                if (address.matches(MAC_REGEX)) {
                    if (this.devices.containsKey(address)) {
                        if (!name.equals("(unknown)") && !this.devices.get(address).equals(name)) {
                            logger.debug("Updating device: {} - {}", address, name);
                            this.devices.put(address, name);
                        }
                    } else {
                        logger.debug("Device found: {} - {}", address, name);
                        this.devices.put(address, name);
                    }
                }
            }
        }
    }

    public boolean isScanRunning() {
        return this.scanRunning;
    }

    private void setScanRunning(boolean scanRunning) {
        this.scanRunning = scanRunning;
    }

    @SuppressWarnings("checkstyle:methodName")
    @Deprecated
    public boolean is_scanRunning() {
        return this.scanRunning;
    }

    @SuppressWarnings("checkstyle:methodName")
    @Deprecated
    public void set_scanRunning(boolean scanRunning) {
        this.scanRunning = scanRunning;
    }

}
