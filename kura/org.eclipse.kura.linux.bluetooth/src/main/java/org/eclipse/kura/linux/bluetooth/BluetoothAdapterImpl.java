/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and others
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
package org.eclipse.kura.linux.bluetooth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothBeaconCommandListener;
import org.eclipse.kura.bluetooth.BluetoothBeaconScanListener;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothLeScanListener;
import org.eclipse.kura.bluetooth.listener.BluetoothAdvertisementScanListener;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.bluetooth.le.BluetoothLeScanner;
import org.eclipse.kura.linux.bluetooth.le.beacon.BluetoothAdvertisingData;
import org.eclipse.kura.linux.bluetooth.le.beacon.BluetoothConfigurationProcessListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothAdapterImpl implements BluetoothAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothAdapterImpl.class);

    private static List<BluetoothDevice> connectedDevices;

    private final String name;
    private String address;
    private boolean leReady;
    private BluetoothLeScanner bls = null;
    private BluetoothBeaconCommandListener bbcl;
    private final CommandExecutorService executorService;

    // See Bluetooth 4.0 Core specifications (https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=229737)
    private static final String OGF_CONTROLLER_CMD = "0x08";
    private static final String OCF_ADVERTISING_PARAM_CMD = "0x0006";
    private static final String OCF_ADVERTISING_DATA_CMD = "0x0008";
    private static final String OCF_ADVERTISING_ENABLE_CMD = "0x000a";

    public BluetoothAdapterImpl(String name, CommandExecutorService executorService) throws KuraException {
        this.name = name;
        this.bbcl = null;
        this.executorService = executorService;
        buildAdapter(name);
    }

    public BluetoothAdapterImpl(String name, BluetoothBeaconCommandListener bbcl,
            CommandExecutorService executorService) throws KuraException {
        this.name = name;
        this.bbcl = bbcl;
        this.executorService = executorService;
        buildAdapter(name);
    }

    public void setBluetoothBeaconCommandListener(BluetoothBeaconCommandListener bbcl) {
        this.bbcl = bbcl;
    }

    // --------------------------------------------------------------------
    //
    // Private methods
    //
    // --------------------------------------------------------------------
    private void buildAdapter(String name) throws KuraException {
        logger.debug("Creating new Bluetooth adapter: {}", name);
        Map<String, String> props = BluetoothUtil.getConfig(name, this.executorService);
        this.address = props.get("address");
        this.leReady = Boolean.parseBoolean(props.get("leReady"));
    }

    private String[] toStringArray(String string) {

        // Regex to split a string every 2 characters
        return string.split("(?<=\\G..)");

    }

    // --------------------------------------------------------------------
    //
    // Static methods
    //
    // --------------------------------------------------------------------
    public static void addConnectedDevice(BluetoothDevice bd) {
        if (connectedDevices == null) {
            connectedDevices = new ArrayList<>();
        }
        connectedDevices.add(bd);
    }

    public static void removeConnectedDevice(BluetoothDevice bd) {
        if (connectedDevices == null) {
            return;
        }
        connectedDevices.remove(bd);
    }

    // --------------------------------------------------------------------
    //
    // BluetoothAdapter API
    //
    // --------------------------------------------------------------------

    @Override
    public String getAddress() {
        return this.address;
    }

    @Override
    public boolean isEnabled() {
        return BluetoothUtil.isEnabled(this.name, this.executorService);
    }

    @Override
    public void startLeScan(BluetoothLeScanListener listener) {
        killLeScan();
        this.bls = new BluetoothLeScanner(this.executorService);
        this.bls.startScan(this.name, listener);
    }

    @Override
    public void startAdvertisementScan(String companyName, BluetoothAdvertisementScanListener listener) {
        killLeScan();
        this.bls = new BluetoothLeScanner(this.executorService);
        this.bls.startAdvertisementScan(this.name, companyName, listener);
    }

    @Override
    public void startBeaconScan(String companyName, BluetoothBeaconScanListener listener) {
        killLeScan();
        this.bls = new BluetoothLeScanner(this.executorService);
        this.bls.startBeaconScan(this.name, companyName, listener);
    }

    @Override
    public void killLeScan() {
        if (this.bls != null) {
            this.bls.killScan(this.name);
            this.bls = null;
        }
    }

    @Override
    public boolean isScanning() {
        if (this.bls != null) {
            return this.bls.isScanRunning();
        } else {
            return false;
        }
    }

    @Override
    public boolean isLeReady() {
        return this.leReady;
    }

    @Override
    public void enable() {
        BluetoothUtil.hciconfigCmd(this.name, "up", this.executorService);
    }

    @Override
    public void disable() {
        BluetoothUtil.hciconfigCmd(this.name, "down", this.executorService);
    }

    @Override
    public BluetoothDevice getRemoteDevice(String address) {
        return new BluetoothDeviceImpl(address, "", this.executorService);
    }

    @Override
    public void startBeaconAdvertising() {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.bbcl);

        logger.debug("Start Advertising : hcitool -i {} cmd {} {} 01", this.name, OGF_CONTROLLER_CMD,
                OCF_ADVERTISING_ENABLE_CMD);
        logger.info("Start Advertising on interface {}", this.name);
        String[] cmd = { "cmd", OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "01" };
        runHcitoolCmd(bbl, cmd);

    }

    @Override
    public void stopBeaconAdvertising() {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.bbcl);

        logger.debug("Stop Advertising : hcitool -i {} cmd {} {} 00", this.name, OGF_CONTROLLER_CMD,
                OCF_ADVERTISING_ENABLE_CMD);
        logger.info("Stop Advertising on interface {}", this.name);
        String[] cmd = { "cmd", OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "00" };
        runHcitoolCmd(bbl, cmd);
    }

    @Override
    public void setBeaconAdvertisingInterval(Integer min, Integer max) {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.bbcl);

        // See
        // http://stackoverflow.com/questions/21124993/is-there-a-way-to-increase-ble-advertisement-frequency-in-bluez
        String[] minHex = toStringArray(BluetoothAdvertisingData.to2BytesHex(min));
        String[] maxHex = toStringArray(BluetoothAdvertisingData.to2BytesHex(max));

        logger.debug(
                "Set Advertising Parameters : hcitool -i {} cmd {} {} {} {} {} {} 03 00 00 00 00 00 00 00 00 07 00",
                this.name, OGF_CONTROLLER_CMD, OCF_ADVERTISING_PARAM_CMD, minHex[1], minHex[0], maxHex[1], maxHex[0]);
        logger.info("Set Advertising Parameters on interface {}", this.name);
        String[] cmd = { "cmd", OGF_CONTROLLER_CMD, OCF_ADVERTISING_PARAM_CMD, minHex[1], minHex[0], maxHex[1],
                maxHex[0], "03", "00", "00", "00", "00", "00", "00", "00", "00", "07", "00" };
        runHcitoolCmd(bbl, cmd);

    }

    @Override
    public void setBeaconAdvertisingData(String uuid, Integer major, Integer minor, String companyCode, Integer txPower,
            boolean LELimited, boolean LEGeneral, boolean BR_EDRSupported, boolean LE_BRController, boolean LE_BRHost) {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.bbcl);

        String[] dataHex = toStringArray(BluetoothAdvertisingData.getData(uuid, major, minor, companyCode, txPower,
                LELimited, LEGeneral, BR_EDRSupported, LE_BRController, LE_BRHost));
        String[] cmd = new String[3 + dataHex.length];
        cmd[0] = "cmd";
        cmd[1] = OGF_CONTROLLER_CMD;
        cmd[2] = OCF_ADVERTISING_DATA_CMD;
        for (int i = 0; i < dataHex.length; i++) {
            cmd[i + 3] = dataHex[i];
        }

        logger.debug("Set Advertising Data : hcitool -i {} cmd {} {} {}", this.name, OGF_CONTROLLER_CMD,
                OCF_ADVERTISING_DATA_CMD, Arrays.toString(dataHex));
        logger.info("Set Advertising Data on interface {}", this.name);
        runHcitoolCmd(bbl, cmd);

    }

    @Override
    public void ExecuteCmd(String ogf, String ocf, String parameter) {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.bbcl);

        String[] paramArray = toStringArray(parameter);
        logger.info("Execute custom command : hcitool -i {} cmd {} {} {}", this.name, ogf, ocf,
                Arrays.toString(paramArray));
        String[] cmd = new String[3 + paramArray.length];
        cmd[0] = "cmd";
        cmd[1] = ogf;
        cmd[2] = ocf;
        for (int i = 0; i < paramArray.length; i++) {
            cmd[i + 3] = paramArray[i];
        }

        runHcitoolCmd(bbl, cmd);
    }

    private void runHcitoolCmd(BluetoothConfigurationProcessListener bbl, String[] cmd) {
        try {
            BluetoothUtil.hcitoolCmd(this.name, cmd, bbl, this.executorService);
        } catch (IOException e) {
            logger.error("Failed to set beacon advertising data", e);
        }
    }

}
