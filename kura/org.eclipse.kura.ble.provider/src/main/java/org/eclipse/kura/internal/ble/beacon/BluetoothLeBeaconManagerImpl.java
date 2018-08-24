/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.beacon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.AdvertisingReportRecord;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconDecoder;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconEncoder;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconManager;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.eclipse.kura.internal.ble.util.BTSnoopListener;
import org.eclipse.kura.internal.ble.util.BluetoothLeUtil;
import org.eclipse.kura.internal.ble.util.BluetoothProcess;
import org.eclipse.kura.internal.ble.util.BluetoothProcessListener;
import org.osgi.service.component.ComponentContext;

public class BluetoothLeBeaconManagerImpl
        implements BluetoothLeBeaconManager<BluetoothLeBeacon>, BTSnoopListener, BluetoothProcessListener {

    private static final String SET_ADVERTISING_PARAMETERS_HCITOOL_MESSAGE = "Set Advertising Parameters : hcitool -i {} {}";

    private static final Logger logger = LogManager.getLogger(BluetoothLeBeaconManagerImpl.class);

    // See Bluetooth 4.0 Core specifications (https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=229737)
    private static final String OGF_CONTROLLER_CMD = "0x08";
    private static final String OCF_ADVERTISING_PARAM_CMD = "0x0006";
    private static final String OCF_ADVERTISING_DATA_CMD = "0x0008";
    private static final String OCF_ADVERTISING_ENABLE_CMD = "0x000a";
    private static final String CMD = "cmd";
    private static final String TWO_CHAR_REGEX = "(?<=\\G..)";

    private static Map<String, BluetoothLeBeaconAdvertiserImpl<BluetoothLeBeacon>> advertisers = new HashMap<>();
    private static Map<String, List<BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>>> scanners = new HashMap<>();

    private BluetoothProcess dumpProc;
    private BluetoothProcess hcitoolProc;
    private Map<BluetoothLeBeaconListener<BluetoothLeBeacon>, Class<?>> listeners;

    protected void activate(ComponentContext context) {
        logger.info("Activating Bluetooth Le Beacon Manager...");
        this.listeners = new HashMap<>();
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Bluetooth Le Beacon Manager...");
    }

    protected BluetoothProcess execBtdump(String interfaceName) throws IOException {
        return BluetoothLeUtil.btdumpCmd(interfaceName, this);
    }

    protected BluetoothProcess execHcitool(String interfaceName, String... cmd) throws IOException {
        return BluetoothLeUtil.hcitoolCmd(interfaceName, cmd, this);
    }

    @Override
    public BluetoothLeBeaconScanner<BluetoothLeBeacon> newBeaconScanner(BluetoothLeAdapter adapter,
            BluetoothLeBeaconDecoder<BluetoothLeBeacon> decoder) {
        BluetoothLeBeaconScannerImpl<BluetoothLeBeacon> scanner = new BluetoothLeBeaconScannerImpl<>(adapter, decoder,
                this);
        if (scanners.containsKey(adapter.getInterfaceName())) {
            scanners.get(adapter.getInterfaceName()).add(scanner);
        } else {
            List<BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>> scannerList = new ArrayList<>();
            scannerList.add(scanner);
            scanners.put(adapter.getInterfaceName(), scannerList);
        }
        return scanner;
    }

    @Override
    public BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> newBeaconAdvertiser(BluetoothLeAdapter adapter,
            BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder) throws KuraBluetoothBeaconAdvertiserNotAvailable {
        BluetoothLeBeaconAdvertiserImpl<BluetoothLeBeacon> advertiser;
        if (advertisers.containsKey(adapter.getInterfaceName())) {
            throw new KuraBluetoothBeaconAdvertiserNotAvailable(
                    "The Beacon Advertiser for " + adapter.getInterfaceName() + " has been already instanciated");
        } else {
            advertiser = new BluetoothLeBeaconAdvertiserImpl<>(adapter, encoder, this);
            advertisers.put(adapter.getInterfaceName(), advertiser);
        }
        return advertiser;
    }

    @Override
    public void deleteBeaconScanner(BluetoothLeBeaconScanner<BluetoothLeBeacon> scanner) {
        String interfaceName = scanner.getAdapter().getInterfaceName();
        if (scanners.containsKey(interfaceName)) {
            scanners.get(interfaceName).remove(scanner);
        }
    }

    @Override
    public void deleteBeaconAdvertiser(BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> advertiser) {
        advertisers.remove(advertiser.getAdapter().getInterfaceName());
    }

    public void startBeaconAdvertising(String interfaceName) throws KuraBluetoothCommandException {
        String[] cmd = { CMD, OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "01" };

        logger.debug(SET_ADVERTISING_PARAMETERS_HCITOOL_MESSAGE, () -> interfaceName, () -> String.join(" ", cmd));

        logger.info("Start Advertising on interface {}", interfaceName);

        try {
            execHcitool(interfaceName, cmd);
        } catch (IOException e) {
            throw new KuraBluetoothCommandException(e, "Start bluetooth beacon advertising failed");
        }
    }

    public void stopBeaconAdvertising(String interfaceName) throws KuraBluetoothCommandException {
        String[] cmd = { CMD, OGF_CONTROLLER_CMD, OCF_ADVERTISING_ENABLE_CMD, "00" };

        logger.debug(SET_ADVERTISING_PARAMETERS_HCITOOL_MESSAGE, () -> interfaceName, () -> String.join(" ", cmd));

        logger.info("Stop Advertising on interface {}", interfaceName);

        try {
            execHcitool(interfaceName, cmd);
        } catch (IOException e) {
            throw new KuraBluetoothCommandException(e, "Stop bluetooth beacon advertising failed");
        }
    }

    public void updateBeaconAdvertisingInterval(Integer min, Integer max, String interfaceName)
            throws KuraBluetoothCommandException {
        checkInterval(min, max);
        // See
        // http://stackoverflow.com/questions/21124993/is-there-a-way-to-increase-ble-advertisement-frequency-in-bluez
        String[] minHex = String.format("%04X", min).split(TWO_CHAR_REGEX);
        String[] maxHex = String.format("%04X", max).split(TWO_CHAR_REGEX);

        String[] cmd = { CMD, OGF_CONTROLLER_CMD, OCF_ADVERTISING_PARAM_CMD, minHex[1], minHex[0], maxHex[1], maxHex[0],
                "03", "00", "00", "00", "00", "00", "00", "00", "00", "07", "00" };

        logger.debug(SET_ADVERTISING_PARAMETERS_HCITOOL_MESSAGE, () -> interfaceName, () -> String.join(" ", cmd));

        logger.info("Set Advertising Parameters on interface {}", interfaceName);

        try {
            execHcitool(interfaceName, cmd);
        } catch (IOException e) {
            throw new KuraBluetoothCommandException(e, "Update bluetooth beacon advertising interval failed");
        }
    }

    private void checkInterval(Integer min, Integer max) {
        if (min > max) {
            throw new IllegalArgumentException("The minimum interval cannot be greater than the maximum.");
        }
        if (min < 14 || min > 65534) {
            throw new IllegalArgumentException("The minimum interval value must be between 14 and 65534.");
        }
        if (max < 14 || max > 65534) {
            throw new IllegalArgumentException("The maximum interval value must be between 14 and 65534.");
        }
    }

    public void updateBeaconAdvertisingData(BluetoothLeBeacon beacon,
            BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder, String interfaceName)
            throws KuraBluetoothCommandException {
        String[] data = toHexStringArray(encoder.encode(beacon));
        String[] cmd = new String[3 + data.length];
        cmd[0] = CMD;
        cmd[1] = OGF_CONTROLLER_CMD;
        cmd[2] = OCF_ADVERTISING_DATA_CMD;
        for (int i = 0; i < data.length; i++) {
            cmd[i + 3] = data[i];
        }

        logger.debug("Set Advertising Data : hcitool -i {} {}", () -> interfaceName, () -> String.join(" ", cmd));

        logger.info("Set Advertising Data on interface {}", interfaceName);
        try {
            execHcitool(interfaceName, cmd);
        } catch (IOException e) {
            throw new KuraBluetoothCommandException(e, "Update bluetooth beacon advertising data failed");
        }
    }

    @Override
    public void processInputStream(String string) throws KuraException {
        logger.debug("Command response : {}", string);
        String[] lines = string.split("\n");
        if (!string.isEmpty() && lines.length >= 1) {
            if (lines[0].toLowerCase().contains("unknown")
                    || lines.length >= 2 && lines[1].toLowerCase().contains("usage")) {
                throw new KuraBluetoothCommandException("Command failed. Error in command syntax.");
            } else if (lines[0].toLowerCase().contains("invalid") || lines[0].toLowerCase().contains("error")) {
                throw new KuraBluetoothCommandException("Command failed.");
            } else {
                parseReturnString(lines);
            }
        }
    }

    private void parseReturnString(String[] lines) throws KuraBluetoothCommandException {
        String lastLine = lines[lines.length - 1];

        String command = lines[0].substring(15, 35);

        // The last line of hcitool cmd return contains:
        // the numbers of packets sent (1 byte)
        // the opcode (2 bytes)
        // the exit code (1 byte)
        // the returned data if any
        String exitCode = lastLine.substring(11, 13);

        switch (exitCode.toLowerCase()) {
        case "00":
            logger.debug("Command {} Succeeded.", command);
            break;
        case "01":
            // The Unknown HCI Command error code indicates that the Controller does not understand the HCI
            // Command Packet OpCode that the Host sent.
            logger.debug("Command {} failed. Error: Unknown HCI Command (01)", command);
            throw new KuraBluetoothCommandException("Command " + command + " failed. Error: Unknown HCI Command (01)");
        case "03":
            // The Hardware Failure error code indicates to the Host that something in the Controller has failed
            // in a manner that cannot be described with any other error code.
            logger.debug("Command {} failed. Error: Hardware Failure (03)", command);
            throw new KuraBluetoothCommandException("Command " + command + " failed. Error: Hardware Failure (03)");
        case "0c":
            // The Command Disallowed error code indicates that the command requested cannot be executed because
            // the Controller is in a state where it cannot process this command at this time. This error code is
            // usually used when a command is run twice, so no exception is to be thrown, here.
            logger.debug("Command {} failed. Error: Command Disallowed (0C)", command);
            break;
        case "11":
            // The Unsupported Feature Or Parameter Value error code indicates that a feature or parameter value
            // in the HCI command is not supported.
            logger.debug("Command {} failed. Error: Unsupported Feature or Parameter Value (11)", command);
            throw new KuraBluetoothCommandException(
                    "Command " + command + " failed. Unsupported Feature or Parameter Value (11)");
        case "12":
            // The Invalid HCI Command Parameters error code indicates that at least one of the HCI command
            // parameters is invalid.
            logger.debug("Command {} failed. Error: Invalid HCI Command Parameters (12)", command);
            throw new KuraBluetoothCommandException(
                    "Command " + command + " failed. Error: Invalid HCI Command Parameters (12)");
        default:
            logger.debug("Command {} failed. Error {}", command, exitCode);
            throw new KuraBluetoothCommandException("Command " + command + " failed. Error " + exitCode);
        }
    }

    @Override
    public void processInputStream(int ch) throws KuraException {
        // Not used
    }

    @Override
    public void processErrorStream(String string) throws KuraException {
        // Not used
    }

    public static String[] toHexStringArray(byte[] in) {
        String[] out = new String[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = String.format("%02X", in[i]);
        }
        return out;
    }

    public void startBeaconScan(String interfaceName) throws KuraBluetoothCommandException {
        if (checkStartScanCondition(interfaceName)) {
            logger.info("Starting bluetooth beacon scan on {}", interfaceName);
            try {
                this.hcitoolProc = execHcitool(interfaceName, "lescan-passive", "--duplicates");
                this.dumpProc = execBtdump(interfaceName);
            } catch (IOException e) {
                throw new KuraBluetoothCommandException(e, "Start bluetooth beacon scan failed");
            }
        }
    }

    public void stopBeaconScan(String interfaceName) {
        // Stop scan on interface only if there is only one scanner that is scanning...
        if (checkStopScanCondition(interfaceName)) {
            logger.info("Stopping bluetooth beacon scan on {}", interfaceName);
            if (this.hcitoolProc != null) {
                this.hcitoolProc.destroy();
            }
            if (this.dumpProc != null) {
                this.dumpProc.destroyBTSnoop();
            }
        }
    }

    public boolean checkStopScanCondition(String interfaceName) {
        // Stop scanning on given interface if only one scanner is scanning
        boolean stopScan = false;
        if (!scanners.containsKey(interfaceName)) {
            stopScan = false;
        } else if (scanners.get(interfaceName).stream().mapToInt(e -> e.isScanning() ? 1 : 0).sum() == 1) {
            stopScan = true;
        }
        return stopScan;
    }

    public boolean checkStartScanCondition(String interfaceName) {
        // Start scanning on given interface if no one is already scanning
        boolean startScan = false;
        if (!scanners.containsKey(interfaceName)) {
            startScan = false;
        } else if (scanners.get(interfaceName).stream().mapToInt(e -> e.isScanning() ? 1 : 0).sum() == 0) {
            startScan = true;
        }
        return startScan;
    }

    public void addBeaconListener(BluetoothLeBeaconListener<BluetoothLeBeacon> listener, Class<?> clazz) {
        if (!this.listeners.containsKey(listener)) {
            this.listeners.put(listener, clazz);
        } else {
            logger.warn("The listener has been already registered");
        }
    }

    public void removeBeaconListener(BluetoothLeBeaconListener<BluetoothLeBeacon> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void processBTSnoopRecord(byte[] record) {
        // Extract raw advertisement data
        List<AdvertisingReportRecord> reportRecords = BluetoothLeUtil.parseLEAdvertisement(record);
        if (!reportRecords.isEmpty()) {

            // Get the active decoders
            List<BluetoothLeBeaconDecoder<BluetoothLeBeacon>> decoders = scanners.values().stream()
                    .flatMap(List::stream).filter(scanner -> scanner.isScanning()).map(scanner -> scanner.getDecoder())
                    .distinct().collect(Collectors.toList());

            List<BluetoothLeBeacon> beacons = new ArrayList<>();
            for (AdvertisingReportRecord report : reportRecords) {
                for (BluetoothLeBeaconDecoder<BluetoothLeBeacon> decoder : decoders) {
                    BluetoothLeBeacon beacon = decoder.decode(report.getReportData());
                    if (beacon != null) {
                        beacon.setAddress(report.getAddress());
                        beacon.setRssi(report.getRssi());
                        beacons.add(beacon);
                    }
                }
            }

            // Notify listeners
            if (!beacons.isEmpty() && !this.listeners.isEmpty()) {
                for (Entry<BluetoothLeBeaconListener<BluetoothLeBeacon>, Class<?>> entry : this.listeners.entrySet()) {
                    beacons.stream().filter(beacon -> entry.getValue() == beacon.getClass())
                            .collect(Collectors.toList()).forEach(entry.getKey()::onBeaconsReceived);
                }
            }
        }
    }

    @Override
    public void processBTSnoopErrorStream(String string) {
        // Not used
    }
}
