/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.linux.bluetooth.le;

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
    private static final String SIGINT = "2";

    private final Map<String, String> devices;
    private List<BluetoothDevice> scanResult;
    private BluetoothProcess proc = null;
    private BluetoothProcess dumpProc = null;
    private BluetoothLeScanListener listener = null;
    private BluetoothBeaconScanListener beaconListener = null;
    private BluetoothAdvertisementScanListener advertisementListener = null;
    private boolean scanRunning = false;
    private String companyName;

    public BluetoothLeScanner() {
        this.devices = new HashMap<>();
    }

    public void startScan(String name, BluetoothLeScanListener listener) {
        this.listener = listener;

        logger.info("Starting bluetooth le scan...");

        // Start scan process
        this.proc = BluetoothUtil.hcitoolCmd(name, "lescan", this);

        setScanRunning(true);
    }

    public void startAdvertisementScan(String name, String companyName, BluetoothAdvertisementScanListener listener) {
        this.advertisementListener = listener;
        this.companyName = companyName;

        logger.info("Starting bluetooth le advertisement scan...");

        // Start scan process
        this.proc = BluetoothUtil.hcitoolCmd(name, new String[] { "lescan-passive", "--duplicates" }, this);

        // Start dump process
        this.dumpProc = BluetoothUtil.btdumpCmd(name, this);

        setScanRunning(true);
    }

    public void startBeaconScan(String name, String companyName, BluetoothBeaconScanListener listener) {
        this.beaconListener = listener;
        this.companyName = companyName;

        logger.info("Starting bluetooth le beacon scan...");

        // Start scan process
        this.proc = BluetoothUtil.hcitoolCmd(name, new String[] { "lescan-passive", "--duplicates" }, this);

        // Start dump process
        this.dumpProc = BluetoothUtil.btdumpCmd(name, this);

        setScanRunning(true);
    }

    public void killScan() {
        // SIGINT must be sent to the hcitool process. Otherwise the adapter must be toggled (down/up).
        if (this.proc != null) {
            logger.info("Killing hcitool...");
            BluetoothUtil.killCmd(BluetoothUtil.HCITOOL, SIGINT);
            this.proc = null;
        } else {
            logger.info("Cannot Kill hcitool, m_proc = null ...");
        }

        // Shut down btdump process
        if (this.dumpProc != null) {
            logger.info("Killing btdump...");
            this.dumpProc.destroyBTSnoop();
            this.dumpProc = null;
        } else {
            logger.info("Cannot Kill btdump, m_dump_proc = null ...");
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
                this.scanResult.add(new BluetoothDeviceImpl(device.getKey(), device.getValue()));
                logger.info("m_scanResult.add {} - {}", device.getKey(), device.getValue());
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

    @Deprecated
    public boolean is_scanRunning() {
        return this.scanRunning;
    }

    @Deprecated
    public void set_scanRunning(boolean m_scanRunning) {
        this.scanRunning = m_scanRunning;
    }

}
