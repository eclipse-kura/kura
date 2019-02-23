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
package org.eclipse.kura.linux.bluetooth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothBeaconCommandListener;
import org.eclipse.kura.bluetooth.BluetoothBeaconScanListener;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothLeScanListener;
import org.eclipse.kura.bluetooth.listener.BluetoothAdvertisementScanListener;
import org.eclipse.kura.linux.bluetooth.le.BluetoothLeScanner;
import org.eclipse.kura.linux.bluetooth.le.beacon.BluetoothAdvertisingData;
import org.eclipse.kura.linux.bluetooth.le.beacon.BluetoothConfigurationProcessListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothAdapterImpl implements BluetoothAdapter {

    private static final Logger s_logger = LoggerFactory.getLogger(BluetoothAdapterImpl.class);

    private static List<BluetoothDevice> s_connectedDevices;

    private final String m_name;
    private String m_address;
    private boolean m_leReady;
    private BluetoothLeScanner m_bls = null;
    private BluetoothBeaconCommandListener m_bbcl;

    // See Bluetooth 4.0 Core specifications (https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=229737)
    private static final String OGF_CONTROLLER_CMD = "0x08";
    private static final String OCF_ADVERTISING_PARAM_CMD = "0x0006";
    private static final String OCF_ADVERTISING_DATA_CMD = "0x0008";
    private static final String OCF_ADVERTISING_ENABLE_CMD = "0x000a";

    public BluetoothAdapterImpl(String name) throws KuraException {
        this.m_name = name;
        this.m_bbcl = null;
        buildAdapter(name);
    }

    public BluetoothAdapterImpl(String name, BluetoothBeaconCommandListener bbcl) throws KuraException {
        this.m_name = name;
        this.m_bbcl = bbcl;
        buildAdapter(name);
    }

    public void setBluetoothBeaconCommandListener(BluetoothBeaconCommandListener bbcl) {
        this.m_bbcl = bbcl;
    }

    // --------------------------------------------------------------------
    //
    // Private methods
    //
    // --------------------------------------------------------------------
    private void buildAdapter(String name) throws KuraException {
        s_logger.debug("Creating new Bluetooth adapter: {}", name);
        Map<String, String> props = new HashMap<String, String>();
        props = BluetoothUtil.getConfig(name);
        this.m_address = props.get("address");
        this.m_leReady = Boolean.parseBoolean(props.get("leReady"));
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
        if (s_connectedDevices == null) {
            s_connectedDevices = new ArrayList<BluetoothDevice>();
        }
        s_connectedDevices.add(bd);
    }

    public static void removeConnectedDevice(BluetoothDevice bd) {
        if (s_connectedDevices == null) {
            return;
        }
        s_connectedDevices.remove(bd);
    }

    // --------------------------------------------------------------------
    //
    // BluetoothAdapter API
    //
    // --------------------------------------------------------------------

    @Override
    public String getAddress() {
        return this.m_address;
    }

    @Override
    public boolean isEnabled() {
        return BluetoothUtil.isEnabled(this.m_name);
    }

    @Override
    public void startLeScan(BluetoothLeScanListener listener) {
        killLeScan();
        this.m_bls = new BluetoothLeScanner();
        this.m_bls.startScan(this.m_name, listener);
    }

    @Override
    public void startAdvertisementScan(String companyName, BluetoothAdvertisementScanListener listener) {
        killLeScan();
        this.m_bls = new BluetoothLeScanner();
        this.m_bls.startAdvertisementScan(this.m_name, companyName, listener);
    }

    @Override
    public void startBeaconScan(String companyName, BluetoothBeaconScanListener listener) {
        killLeScan();
        this.m_bls = new BluetoothLeScanner();
        this.m_bls.startBeaconScan(this.m_name, companyName, listener);
    }

    @Override
    public void killLeScan() {
        if (this.m_bls != null) {
            this.m_bls.killScan();
            this.m_bls = null;
        }
    }

    @Override
    public boolean isScanning() {
        if (this.m_bls != null) {
            return this.m_bls.isScanRunning();
        } else {
            return false;
        }
    }

    @Override
    public boolean isLeReady() {
        return this.m_leReady;
    }

    @Override
    public void enable() {
        BluetoothUtil.hciconfigCmd(this.m_name, "up");
    }

    @Override
    public void disable() {
        BluetoothUtil.hciconfigCmd(this.m_name, "down");
    }

    @Override
    public BluetoothDevice getRemoteDevice(String address) {
        return new BluetoothDeviceImpl(address, "");
    }

    @Override
    public void startBeaconAdvertising() {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.m_bbcl);

        s_logger.debug("Start Advertising : hcitool -i " + this.m_name + " cmd " + OGF_CONTROLLER_CMD + " "
                + OCF_ADVERTISING_ENABLE_CMD + " 01");
        s_logger.info("Start Advertising on interface " + this.m_name);
        String[] cmd = { "cmd", OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "01" };
        BluetoothUtil.hcitoolCmd(this.m_name, cmd, bbl);

    }

    @Override
    public void stopBeaconAdvertising() {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.m_bbcl);

        s_logger.debug("Stop Advertising : hcitool -i " + this.m_name + " cmd " + OGF_CONTROLLER_CMD + " "
                + OCF_ADVERTISING_ENABLE_CMD + " 00");
        s_logger.info("Stop Advertising on interface " + this.m_name);
        String[] cmd = { "cmd", OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "00" };
        BluetoothUtil.hcitoolCmd(this.m_name, cmd, bbl);
    }

    @Override
    public void setBeaconAdvertisingInterval(Integer min, Integer max) {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.m_bbcl);

        // See
        // http://stackoverflow.com/questions/21124993/is-there-a-way-to-increase-ble-advertisement-frequency-in-bluez
        String[] minHex = toStringArray(BluetoothAdvertisingData.to2BytesHex(min));
        String[] maxHex = toStringArray(BluetoothAdvertisingData.to2BytesHex(max));

        s_logger.debug("Set Advertising Parameters : hcitool -i " + this.m_name + " cmd " + OGF_CONTROLLER_CMD + " "
                + OCF_ADVERTISING_PARAM_CMD + " " + minHex[1] + " " + minHex[0] + " " + maxHex[1] + " " + maxHex[0]
                + " 03 00 00 00 00 00 00 00 00 07 00");
        s_logger.info("Set Advertising Parameters on interface " + this.m_name);
        String[] cmd = { "cmd", OGF_CONTROLLER_CMD, OCF_ADVERTISING_PARAM_CMD, minHex[1], minHex[0], maxHex[1],
                maxHex[0], "03", "00", "00", "00", "00", "00", "00", "00", "00", "07", "00" };
        BluetoothUtil.hcitoolCmd(this.m_name, cmd, bbl);

    }

    @Override
    public void setBeaconAdvertisingData(String uuid, Integer major, Integer minor, String companyCode, Integer txPower,
            boolean LELimited, boolean LEGeneral, boolean BR_EDRSupported, boolean LE_BRController, boolean LE_BRHost) {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.m_bbcl);

        String[] dataHex = toStringArray(BluetoothAdvertisingData.getData(uuid, major, minor, companyCode, txPower,
                LELimited, LEGeneral, BR_EDRSupported, LE_BRController, LE_BRHost));
        String[] cmd = new String[3 + dataHex.length];
        cmd[0] = "cmd";
        cmd[1] = OGF_CONTROLLER_CMD;
        cmd[2] = OCF_ADVERTISING_DATA_CMD;
        for (int i = 0; i < dataHex.length; i++) {
            cmd[i + 3] = dataHex[i];
        }

        s_logger.debug("Set Advertising Data : hcitool -i " + this.m_name + "cmd " + OGF_CONTROLLER_CMD + " "
                + OCF_ADVERTISING_DATA_CMD + " " + Arrays.toString(dataHex));
        s_logger.info("Set Advertising Data on interface " + this.m_name);
        BluetoothUtil.hcitoolCmd(this.m_name, cmd, bbl);

    }

    @Override
    public void ExecuteCmd(String ogf, String ocf, String parameter) {

        BluetoothConfigurationProcessListener bbl = new BluetoothConfigurationProcessListener(this.m_bbcl);

        String[] paramArray = toStringArray(parameter);
        s_logger.info("Execute custom command : hcitool -i " + this.m_name + "cmd " + ogf + " " + ocf + " "
                + Arrays.toString(paramArray));
        String[] cmd = new String[3 + paramArray.length];
        cmd[0] = "cmd";
        cmd[1] = ogf;
        cmd[2] = ocf;
        for (int i = 0; i < paramArray.length; i++) {
            cmd[i + 3] = paramArray[i];
        }

        BluetoothUtil.hcitoolCmd(this.m_name, cmd, bbl);
    }

}
