/*******************************************************************************
 * Copyright (c) 2017, 2026 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Scott Ware
 *******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.bluetooth.le.beacon.AdvertisingReportRecord;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.CommandExecutorService;

public class BluetoothLeUtil {

    private static final Logger logger = LogManager.getLogger(BluetoothLeUtil.class);

    @SuppressWarnings("checkstyle:constantName")
    private static final ExecutorService processExecutor = Executors.newSingleThreadExecutor();

    public static final String HCITOOL = "hcitool";

    public static final String COMMAND_ERROR = "Error executing command: {}";
    public static final String COMMAND_EXEC = "Command executed : {}";

    // eventType(1) + addressType(1) + address(6) + dataLength(1)
    private static final int NON_EXTENDED_HEADER_LENGTH = 9;
    // eventType(2) + addressType(1) + address(6) + primaryPhy(1) + secondaryPhy(1) + sid(1) + txPower(1)
    // + rssi(1) + periodicAdvertisingInterval(2) + directAddressType(1) + directAddress(6) + dataLength(1)
    private static final int EXTENDED_HEADER_LENGTH = 24;

    private BluetoothLeUtil() {
        // Empty constructor
    }

    /**
     * Start a bluetooth monitor process for the examination of BLE advertisement packets
     *
     * @param name
     *            Name of HCI device (hci0, for example)
     * @param listener
     *            Listener for receiving btsnoop records
     * @return BluetoothProcess created
     */
    public static BluetoothProcess btdumpCmd(String name, CommandExecutorService executorService,
            BTSnoopListener listener) throws IOException {
        String[] command = { "btmon", "-i", name, "-w", "/dev/fd/3" };
        return execSnoop(command, executorService, listener);
    }

    /*
     * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
     * BluetoothProcessListener is used to receive output from the process.
     */
    public static BluetoothProcess hcitoolCmd(String name, String cmd, CommandExecutorService executorService,
            BluetoothProcessListener listener) throws IOException {
        String[] command = { HCITOOL, "-i", name, cmd };
        return exec(command, executorService, listener);
    }

    /*
     * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
     * BluetoothProcessListener is used to receive output from the process.
     */
    public static BluetoothProcess hcitoolCmd(String name, String[] cmd, CommandExecutorService executorService,
            BluetoothProcessListener listener) throws IOException {
        String[] command = new String[3 + cmd.length];
        command[0] = HCITOOL;
        command[1] = "-i";
        command[2] = name;
        for (int i = 0; i < cmd.length; i++) {
            command[i + 3] = cmd[i];
        }
        return exec(command, executorService, listener);
    }

    /*
     * Method to create a separate thread for the BluetoothProcesses.
     */
    private static BluetoothProcess exec(final String[] cmdArray, CommandExecutorService executorService,
            final BluetoothProcessListener listener) throws IOException {

        // Serialize process executions. One at a time so we can consume all streams.
        Future<BluetoothProcess> futureSafeProcess = processExecutor.submit(() -> {
            Thread.currentThread().setName("BluetoothProcessExecutor");
            BluetoothProcess bluetoothProcess = new BluetoothProcess(executorService);
            bluetoothProcess.exec(cmdArray, listener);
            return bluetoothProcess;
        });

        try {
            return futureSafeProcess.get();
        } catch (Exception e) {
            logger.error("Error waiting from SafeProcess output", e);
            throw new IOException(e);
        }
    }

    /*
     * Method to create a separate thread for the BluetoothProcesses.
     */
    private static BluetoothProcess execSnoop(final String[] cmdArray, CommandExecutorService executorService,
            final BTSnoopListener listener) throws IOException {

        // Serialize process executions. One at a time so we can consume all streams.
        Future<BluetoothProcess> futureSafeProcess = processExecutor.submit(() -> {
            Thread.currentThread().setName("BTSnoopProcessExecutor");
            BluetoothProcess bluetoothProcess = new BluetoothProcess(executorService);
            bluetoothProcess.execSnoop(cmdArray, listener);
            return bluetoothProcess;
        });

        try {
            return futureSafeProcess.get();
        } catch (Exception e) {
            logger.error("Error waiting from SafeProcess output", e);
            throw new IOException(e);
        }
    }

    /**
     * Check for advertisement out of an HCL LE Advertising Report Event
     *
     * See Bluetooth Core 5.2; 7.7.65.2 LE Advertising Report Event
     * See Bluetooth Core 5.2; 7.7.65.13 LE Advertising Report Event
     *
     * @param b
     *            the byte stream
     * @return BluetoothAdvertisementData
     */
    public static List<AdvertisingReportRecord> parseLEAdvertisement(byte[] b) {

        List<AdvertisingReportRecord> reportRecords = new ArrayList<>();

        // Event Code : LE Meta (0x3E)
        if (b[0] != 0x3E) {
            // Not an Advertising Packet
            return reportRecords;
        }

        // Sub Event Code : LE Advertising Report (0x02)
        // Sub Event Code : LE Extended Advertising Report (0x0D)
        if (b[2] != 0x02 && b[2] != 0x0D) {
            // Not an Advertisement Event
            return reportRecords;
        }

        // Determine if this is an extended advertising report
        boolean extendedAdvertisingReport = false;

        if (b[2] == 0x0D) {
            extendedAdvertisingReport = true;
        }

        // Number of reports in this advertisement
        int reportRecordsNumber = b[3] & 0xFF;

        // Parse each report; ptrHolder[0] carries the cursor position across calls
        int[] ptrHolder = { 4 };
        for (int nr = 0; nr < reportRecordsNumber; nr++) {
            AdvertisingReportRecord arr = parseReportRecord(b, ptrHolder, extendedAdvertisingReport);
            if (arr == null) {
                break;
            }
            reportRecords.add(arr);
        }

        return reportRecords;
    }

    private static AdvertisingReportRecord parseReportRecord(byte[] b, int[] ptrHolder,
            boolean extendedAdvertisingReport) {

        int ptr = ptrHolder[0];

        // Return if there isn't even enough buffer left for this report's fixed-size fields
        int minHeaderLength = extendedAdvertisingReport ? EXTENDED_HEADER_LENGTH : NON_EXTENDED_HEADER_LENGTH;
        if (ptr + minHeaderLength > b.length) {
            return null;
        }

        AdvertisingReportRecord arr = new AdvertisingReportRecord();
        arr.setExtendedReport(extendedAdvertisingReport);

        if (extendedAdvertisingReport) {
            arr.setEventType(((b[ptr++] & 0xFF) | (b[ptr++] & 0xFF) << 8));
        } else {
            arr.setEventType(b[ptr++]);
        }

        arr.setAddressType(b[ptr++]);

        // Extract remote address
        String address = String.format("%02X:%02X:%02X:%02X:%02X:%02X", b[ptr + 5], b[ptr + 4], b[ptr + 3],
                b[ptr + 2], b[ptr + 1], b[ptr + 0]);

        arr.setAddress(address);

        // Skip past address bytes
        ptr += 6;

        // Additional Extended Advertising Report attributes
        if (extendedAdvertisingReport) {
            // Primary & Secondary Phy
            arr.setPrimaryPhy(b[ptr++]);
            arr.setSecondaryPhy(b[ptr++]);

            // SID
            arr.setSid(b[ptr++]);

            // Tx Power
            arr.setTxPower(b[ptr++]);

            // RSSI
            arr.setRssi(b[ptr++]);

            // Periodic Advertising Interval
            arr.setPeriodicAdvertisingInterval(((b[ptr++] & 0xFF) | (b[ptr++] & 0xFF) << 8));

            // Direct Address
            arr.setDirectAddressType(b[ptr++]);

            String directAddress = String.format("%02X:%02X:%02X:%02X:%02X:%02X", b[ptr + 5], b[ptr + 4],
                    b[ptr + 3], b[ptr + 2], b[ptr + 1], b[ptr + 0]);

            arr.setDirectAddress(directAddress);

            // Skip past direct address bytes
            ptr += 6;
        }

        int arrDataLength = b[ptr++] & 0xFF;

        // Bail out if the declared length runs past the buffer: a legitimate report always
        // has its data (and, for non-extended reports, a trailing RSSI byte) present.
        int trailingBytesNeeded = extendedAdvertisingReport ? 0 : 1;
        if (ptr + arrDataLength + trailingBytesNeeded > b.length) {
            return null;
        }

        if (arrDataLength > 0) {
            arr.setLength(arrDataLength);
        }
        byte[] arrData = new byte[arrDataLength];
        System.arraycopy(b, ptr, arrData, 0, arrDataLength);
        arr.setReportData(arrData);

        // RSSI
        if (!extendedAdvertisingReport) {
            arr.setRssi(b[ptr + arrDataLength]);
        }

        ptr += arrDataLength;
        ptrHolder[0] = ptr;

        return arr;
    }

    public static boolean stopHcitool(String interfaceName, CommandExecutorService executorService, String... params) {
        List<String> killCommand = new ArrayList<>();
        killCommand.add(HCITOOL);
        killCommand.add("-i");
        killCommand.add(interfaceName);
        Arrays.asList(params).forEach(killCommand::add);
        return executorService.kill(killCommand.toArray(new String[0]), LinuxSignal.SIGINT);
    }

    public static boolean stopBtdump(String interfaceName, CommandExecutorService executorService) {
        String[] command = { "btmon", "-i", interfaceName, "-w", "/dev/fd/3" };
        return executorService.kill(command, LinuxSignal.SIGTERM);
    }
}
