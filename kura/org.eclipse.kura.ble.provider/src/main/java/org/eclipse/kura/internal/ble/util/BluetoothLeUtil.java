/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.bluetooth.le.beacon.AdvertisingReportRecord;

public class BluetoothLeUtil {

    private static final Logger logger = LogManager.getLogger(BluetoothLeUtil.class);
    private static final ExecutorService processExecutor = Executors.newSingleThreadExecutor();

    public static final String HCITOOL = "hcitool";
    public static final String BTDUMP = "/tmp/BluetoothUtil.btsnoopdump.sh";

    public static final String COMMAND_ERROR = "Error executing command: {}";
    public static final String COMMAND_EXEC = "Command executed : {}";

    // Write bluetooth dumping script into /tmp
    static {
        try {
            File f = new File(BTDUMP);
            FileUtils.writeStringToFile(f, "#!/bin/bash\n" + "set -e\n" + "ADAPTER=$1\n"
                    + "{ exec hcidump -i $ADAPTER -R -w /dev/fd/3 >/dev/null; } 3>&1", false);

            if (!f.setExecutable(true)) {
                logger.warn("Unable to set as executable");
            }
        } catch (IOException e) {
            logger.info("Unable to update", e);
        }

    }

    private BluetoothLeUtil() {

    }

    /*
     * Utility method to send specific kill commands to processes.
     */
    public static void killCmd(String cmd, String signal) {
        String[] commandPidOf = { "pidof", cmd };
        BluetoothSafeProcess proc = null;
        try {
            proc = BluetoothProcessUtil.exec(commandPidOf);
            proc.waitFor();
        } catch (IOException e) {
            logger.error(COMMAND_ERROR, commandPidOf, e);
            return;
        } catch (InterruptedException e) {
            logger.error(COMMAND_ERROR, commandPidOf, e);
            Thread.currentThread().interrupt();
        }
        
        try (InputStreamReader is = new InputStreamReader(proc.getInputStream());
                BufferedReader br = new BufferedReader(is);) {
            String pid = br.readLine();

            // Check if the pid is not empty
            if (pid != null) {
                String[] commandKill = { "kill", "-" + signal, pid };
                proc = BluetoothProcessUtil.exec(commandKill);
            }

        } catch (IOException e) {
            logger.error(COMMAND_ERROR, commandPidOf, e);
        } finally {
            proc.destroy();
        }
    }

    /**
     * Start an hci dump process for the examination of BLE advertisement packets
     *
     * @param name
     *            Name of HCI device (hci0, for example)
     * @param listener
     *            Listener for receiving btsnoop records
     * @return BluetoothProcess created
     */
    public static BluetoothProcess btdumpCmd(String name, BTSnoopListener listener) throws IOException {
        String[] command = { BTDUMP, name };
        return execSnoop(command, listener);
    }

    /*
     * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
     * BluetoothProcessListener is used to receive output from the process.
     */
    public static BluetoothProcess hcitoolCmd(String name, String cmd, BluetoothProcessListener listener)
            throws IOException {
        String[] command = { HCITOOL, "-i", name, cmd };
        return exec(command, listener);
    }

    /*
     * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
     * BluetoothProcessListener is used to receive output from the process.
     */
    public static BluetoothProcess hcitoolCmd(String name, String[] cmd, BluetoothProcessListener listener)
            throws IOException {
        String[] command = new String[3 + cmd.length];
        command[0] = HCITOOL;
        command[1] = "-i";
        command[2] = name;
        for (int i = 0; i < cmd.length; i++) {
            command[i + 3] = cmd[i];
        }
        return exec(command, listener);
    }

    /*
     * Method to create a separate thread for the BluetoothProcesses.
     */
    private static BluetoothProcess exec(final String[] cmdArray, final BluetoothProcessListener listener)
            throws IOException {

        // Serialize process executions. One at a time so we can consume all streams.
        Future<BluetoothProcess> futureSafeProcess = processExecutor.submit(() -> {
            Thread.currentThread().setName("BluetoothProcessExecutor");
            BluetoothProcess bluetoothProcess = new BluetoothProcess();
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
    private static BluetoothProcess execSnoop(final String[] cmdArray, final BTSnoopListener listener)
            throws IOException {

        // Serialize process executions. One at a time so we can consume all streams.
        Future<BluetoothProcess> futureSafeProcess = processExecutor.submit(() -> {
            Thread.currentThread().setName("BTSnoopProcessExecutor");
            BluetoothProcess bluetoothProcess = new BluetoothProcess();
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
     * See Bluetooth Core 4.0; 7.7.65.2 LE Advertising Report Event
     *
     * @param b
     *            the byte stream
     * @return BluetoothAdvertisementData
     */
    public static List<AdvertisingReportRecord> parseLEAdvertisement(byte[] b) {

        List<AdvertisingReportRecord> reportRecords = new ArrayList<>();

        // HCI Packet Type : HCI Event (0x04)
        // Event Code : LE Advertising Report (0x3E)
        if (b[0] != 0x04 || b[1] != 0x3E) {
            // Not an Advertisement Packet
            return reportRecords;
        }

        // Subevent Code : LE Advertisement Subevent (0x02)
        if (b[3] != 0x02) {
            // Not an Advertisement Sub Event
            return reportRecords;
        }

        // Number of reports in this advertisement
        int reportRecordsNumber = b[4];

        // Parse each report
        int ptr = 5;
        for (int nr = 0; nr < reportRecordsNumber; nr++) {

            AdvertisingReportRecord arr = new AdvertisingReportRecord();
            arr.setEventType(b[ptr++]);
            arr.setAddressType(b[ptr++]);

            // Extract remote address
            String address = String.format("%02X:%02X:%02X:%02X:%02X:%02X", b[ptr + 5], b[ptr + 4], b[ptr + 3],
                    b[ptr + 2], b[ptr + 1], b[ptr + 0]);

            arr.setAddress(address);

            ptr += 6;

            int arrDataLength = b[ptr++];

            if (arrDataLength > 0) {
                arr.setLength(b[ptr + 1]);
            }
            byte[] arrData = new byte[arrDataLength];
            System.arraycopy(b, ptr, arrData, 0, arrDataLength);
            arr.setReportData(arrData);
            arr.setRssi(b[ptr + arrDataLength]);

            reportRecords.add(arr);

            ptr += arrDataLength;
        }

        return reportRecords;
    }

}
