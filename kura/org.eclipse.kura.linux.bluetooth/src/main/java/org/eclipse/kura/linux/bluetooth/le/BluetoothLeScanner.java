/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - minor clean ups
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

    private static final Logger s_logger = LoggerFactory.getLogger(BluetoothLeScanner.class);
    private static final String MAC_REGEX = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";

    public static final int SCAN_FAILED_INTERNAL_ERROR = 0x0003;
    private static final String SIGINT = "2";

    private final Map<String, String> m_devices;
    private List<BluetoothDevice> m_scanResult;
    private BluetoothProcess m_proc = null;
    private BluetoothProcess m_dump_proc = null;
    private BluetoothLeScanListener m_listener = null;
    private BluetoothBeaconScanListener m_beacon_listener = null;
    private BluetoothAdvertisementScanListener m_advertisement_listener = null;
    private boolean m_scanRunning = false;
    private String m_companyName;

    public BluetoothLeScanner() {
        this.m_devices = new HashMap<String, String>();
    }

    public void startScan(String name, BluetoothLeScanListener listener) {
        this.m_listener = listener;

        s_logger.info("Starting bluetooth le scan...");

        // Start scan process
        this.m_proc = BluetoothUtil.hcitoolCmd(name, "lescan", this);

        setScanRunning(true);
    }

    public void startAdvertisementScan(String name, String companyName, BluetoothAdvertisementScanListener listener) {
        this.m_advertisement_listener = listener;
        this.m_companyName = companyName;

        s_logger.info("Starting bluetooth le advertisement scan...");

        // Start scan process
        this.m_proc = BluetoothUtil.hcitoolCmd(name, new String[] { "lescan-passive", "--duplicates" }, this);

        // Start dump process
        this.m_dump_proc = BluetoothUtil.btdumpCmd(name, this);

        setScanRunning(true);
    }

    public void startBeaconScan(String name, String companyName, BluetoothBeaconScanListener listener) {
        this.m_beacon_listener = listener;
        this.m_companyName = companyName;

        s_logger.info("Starting bluetooth le beacon scan...");

        // Start scan process
        this.m_proc = BluetoothUtil.hcitoolCmd(name, new String[] { "lescan-passive", "--duplicates" }, this);

        // Start dump process
        this.m_dump_proc = BluetoothUtil.btdumpCmd(name, this);

        setScanRunning(true);
    }

    public void killScan() {
        // SIGINT must be sent to the hcitool process. Otherwise the adapter must be toggled (down/up).
        if (this.m_proc != null) {
            s_logger.info("Killing hcitool...");
            BluetoothUtil.killCmd(BluetoothUtil.HCITOOL, SIGINT);
            this.m_proc = null;
        } else {
            s_logger.info("Cannot Kill hcitool, m_proc = null ...");
        }

        // Shut down btdump process
        if (this.m_dump_proc != null) {
            s_logger.info("Killing btdump...");
            this.m_dump_proc.destroyBTSnoop();
            this.m_dump_proc = null;
        } else {
            s_logger.info("Cannot Kill btdump, m_dump_proc = null ...");
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

        if (this.m_listener != null) {
            String[] lines = string.split("\n");
            for (String line : lines) {
                processLine(line);
            }

            this.m_scanResult = new ArrayList<BluetoothDevice>();
            for (Entry<String, String> device : this.m_devices.entrySet()) {
                this.m_scanResult.add(new BluetoothDeviceImpl(device.getKey(), device.getValue()));
                s_logger.info("m_scanResult.add {} - {}", device.getKey(), device.getValue());
            }

            // Alert listener that scan is complete
            this.m_listener.onScanResults(this.m_scanResult);
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
            if (bAdData != null && this.m_advertisement_listener != null) {
                try {
                    this.m_advertisement_listener.onAdvertisementDataReceived(bAdData);
                } catch (Exception e) {
                    s_logger.error("Scan listener threw exception", e);
                }
            }

            // Extract beacon advertisements
            List<BluetoothBeaconData> beaconDatas = BluetoothUtil.parseLEAdvertisingReport(record, this.m_companyName);

            // Extract beacon data
            for (BluetoothBeaconData beaconData : beaconDatas) {

                // Notify the listener
                try {

                    if (this.m_beacon_listener != null) {
                        this.m_beacon_listener.onBeaconDataReceived(beaconData);
                    }

                } catch (Exception e) {
                    s_logger.error("Scan listener threw exception", e);
                }
            }

        } catch (Exception e) {
            s_logger.error("Error processing advertising report", e);
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
        s_logger.info(line);
        if (line.contains("Set scan parameters failed:")) {
            s_logger.error("Error : " + line);
        } else {
            // Results from hcitool lescan should be in form:
            // <mac_address> <device_name>
            String[] results = line.split("\\s", 2);
            if (results.length == 2) {
                address = results[0].trim();
                name = results[1].trim();

                if (address.matches(MAC_REGEX)) {
                    if (this.m_devices.containsKey(address)) {
                        if (!name.equals("(unknown)") && !this.m_devices.get(address).equals(name)) {
                            s_logger.debug("Updating device: {} - {}", address, name);
                            this.m_devices.put(address, name);
                        }
                    } else {
                        s_logger.debug("Device found: {} - {}", address, name);
                        this.m_devices.put(address, name);
                    }
                }
            }
        }
    }

    public boolean isScanRunning() {
        return this.m_scanRunning;
    }

    private void setScanRunning(boolean scanRunning) {
        this.m_scanRunning = scanRunning;
    }

    @Deprecated
    public boolean is_scanRunning() {
        return this.m_scanRunning;
    }

    @Deprecated
    public void set_scanRunning(boolean m_scanRunning) {
        this.m_scanRunning = m_scanRunning;
    }

}
