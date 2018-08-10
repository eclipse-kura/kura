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
package org.eclipse.kura.linux.bluetooth.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothBeaconData;
import org.eclipse.kura.bluetooth.listener.AdvertisingReportRecord;
import org.eclipse.kura.bluetooth.listener.BluetoothAdvertisementData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothUtil {

    private static final Logger s_logger = LoggerFactory.getLogger(BluetoothUtil.class);
    private static final ExecutorService s_processExecutor = Executors.newSingleThreadExecutor();

    public static final String HCITOOL = "hcitool";
    public static final String BTDUMP = "/tmp/BluetoothUtil.btsnoopdump.sh";
    private static final String BD_ADDRESS = "BD Address:";
    private static final String HCI_VERSION = "HCI Version:";
    private static final String HCICONFIG = "hciconfig";
    private static final String GATTTOOL = "gatttool";

    // Write bluetooth dumping script into /tmp
    static {
        try {
            File f = new File(BTDUMP);
            FileUtils.writeStringToFile(f, "#!/bin/bash\n" + "set -e\n" + "ADAPTER=$1\n"
                    + "{ hcidump -i $ADAPTER -R -w /dev/fd/3 >/dev/null; } 3>&1", false);

            f.setExecutable(true);
        } catch (IOException e) {
            s_logger.info("Unable to update", e);
        }

    }

    /*
     * Use hciconfig utility to return information about the bluetooth adapter
     */
    public static Map<String, String> getConfig(String name) throws KuraException {
        Map<String, String> props = new HashMap<String, String>();
        BluetoothSafeProcess proc = null;
        BufferedReader br = null;
        StringBuilder sb = null;
        String[] command = { HCICONFIG, name, "version" };
        try {
            proc = BluetoothProcessUtil.exec(command);
            // Check Error stream
            br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains("command not found")) {
                    throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
                } else if (line.toLowerCase().contains("no such device")) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
                }
            }
            if (br != null) {
                br.close();
            }

            // Check Input stream
            br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            sb = new StringBuilder();
            line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }

            // TODO: Pull more parameters from hciconfig?
            String[] results = sb.toString().split("\n");
            props.put("leReady", "false");
            for (String result : results) {
                if (result.indexOf(BD_ADDRESS) >= 0) {
                    // Address reported as:
                    // BD Address: xx:xx:xx:xx:xx:xx ACL MTU: xx:xx SCO MTU: xx:x
                    String[] ss = result.split(" ");
                    String address = "";
                    for (String sss : ss) {
                        if (sss.matches("^([0-9a-fA-F][0-9a-fA-F]:){5}([0-9a-fA-F][0-9a-fA-F])$")) {
                            address = sss;
                            break;
                        }
                    }
                    // String address = result.substring(index + BD_ADDRESS.length());
                    // String[] tmpAddress = address.split("\\s", 2);
                    // address = tmpAddress[0].trim();
                    props.put("address", address);
                    s_logger.trace("Bluetooth adapter address set to: {}", address);
                }
                if (result.indexOf(HCI_VERSION) >= 0) {
                    // HCI version : 4.0 (0x6) or HCI version : 4.1 (0x7)
                    if (result.indexOf("0x6") >= 0 || result.indexOf("0x7") >= 0) {
                        props.put("leReady", "true");
                        s_logger.trace("Bluetooth adapter is LE ready");
                    }
                }
            }

        } catch (Exception e) {
            s_logger.error("Failed to execute command: {}", command, e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (proc != null) {
                    proc.destroy();
                }
            } catch (IOException e) {
                s_logger.error("Error closing read buffer", e);
            }

        }

        return props;
    }

    /*
     * Use hciconfig utility to determine status of bluetooth adapter
     */
    public static boolean isEnabled(String name) {

        String[] command = { HCICONFIG, name };
        BluetoothSafeProcess proc = null;
        BufferedReader br = null;

        try {
            proc = BluetoothProcessUtil.exec(command);
            br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.contains("UP")) {
                    return true;
                }
                if (line.contains("DOWN")) {
                    return false;
                }
            }
        } catch (Exception e) {
            s_logger.error("Error executing command: {}", command, e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (proc != null) {
                    proc.destroy();
                }
            } catch (IOException e) {
                s_logger.error("Error closing read buffer", e);
            }
        }

        return false;
    }

    /*
     * Utility method that allows sending any hciconfig command. The buffered
     * response is returned in case results are needed.
     */
    public static BufferedReader hciconfigCmd(String name, String cmd) {
        String[] command = { HCICONFIG, name, cmd };
        BluetoothSafeProcess proc = null;
        BufferedReader br = null;
        try {
            proc = BluetoothProcessUtil.exec(command);
            br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        } catch (Exception e) {
            s_logger.error("Error executing command: {}", command, e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (proc != null) {
                    proc.destroy();
                }
            } catch (IOException e) {
                s_logger.error("Error closing read buffer", e);
            }
        }
        return br;
    }

    /*
     * Utility method to send specific kill commands to processes.
     */
    public static void killCmd(String cmd, String signal) {
        // String[] command = { "pkill", "-" + signal, cmd };
        String[] commandPidOf = { "pidof", cmd };
        BluetoothSafeProcess proc = null;
        BufferedReader br = null;
        try {
            proc = BluetoothProcessUtil.exec(commandPidOf);
            proc.waitFor();
            br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String pid = br.readLine();

            // Check if the pid is not empty
            if (pid != null) {
                String[] commandKill = { "kill", "-" + signal, pid };
                proc = BluetoothProcessUtil.exec(commandKill);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            s_logger.error("Error executing command: {}", commandPidOf, e);
        } catch (IOException e) {
            s_logger.error("Error executing command: {}", commandPidOf, e);
        } finally {
            if (proc != null) {
                proc.destroy();
            }
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                s_logger.warn("Error closing process for command: {}", commandPidOf, e);
            }
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
    public static BluetoothProcess btdumpCmd(String name, BTSnoopListener listener) {
        String[] command = { BTDUMP, name };

        BluetoothProcess proc = null;
        try {
            s_logger.debug("Command executed : {}", Arrays.toString(command));
            proc = execSnoop(command, listener);
        } catch (Exception e) {
            s_logger.error("Error executing command: {}", command, e);
        }

        return proc;
    }

    /*
     * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
     * BluetoothProcessListener is used to receive output from the process.
     */
    public static BluetoothProcess hcitoolCmd(String name, String cmd, BluetoothProcessListener listener) {
        String[] command = { HCITOOL, "-i", name, cmd };
        BluetoothProcess proc = null;
        try {
            s_logger.debug("Command executed : {}", Arrays.toString(command));
            proc = exec(command, listener);
        } catch (Exception e) {
            s_logger.error("Error executing command: {}", command, e);
        }

        return proc;
    }

    /*
     * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
     * BluetoothProcessListener is used to receive output from the process.
     */
    public static BluetoothProcess hcitoolCmd(String name, String[] cmd, BluetoothProcessListener listener) {
        String[] command = new String[3 + cmd.length];
        command[0] = HCITOOL;
        command[1] = "-i";
        command[2] = name;
        for (int i = 0; i < cmd.length; i++) {
            command[i + 3] = cmd[i];
        }
        BluetoothProcess proc = null;
        try {
            s_logger.debug("Command executed : {}", Arrays.toString(command));
            proc = exec(command, listener);
        } catch (Exception e) {
            s_logger.error("Error executing command: {}", command, e);
        }

        return proc;
    }

    /*
     * Method to start an interactive session with a remote Bluetooth LE device using the gatttool utility. The
     * listener is used to receive output from the process.
     */
    public static BluetoothProcess startSession(String adapterName, String address, BluetoothProcessListener listener) {
        String[] command = { GATTTOOL, "-i", adapterName, "-b", address, "-I" };
        BluetoothProcess proc = null;
        try {
            proc = exec(command, listener);
        } catch (Exception e) {
            s_logger.error("Error executing command: {}", command, e);
        }
        return proc;
    }

    /*
     * Method to create a separate thread for the BluetoothProcesses.
     */
    private static BluetoothProcess exec(final String[] cmdArray, final BluetoothProcessListener listener)
            throws IOException {

        // Serialize process executions. One at a time so we can consume all streams.
        Future<BluetoothProcess> futureSafeProcess = s_processExecutor.submit(new Callable<BluetoothProcess>() {

            @Override
            public BluetoothProcess call() throws Exception {
                Thread.currentThread().setName("BluetoothProcessExecutor");
                BluetoothProcess bluetoothProcess = new BluetoothProcess();
                bluetoothProcess.exec(cmdArray, listener);
                return bluetoothProcess;
            }
        });

        try {
            return futureSafeProcess.get();
        } catch (Exception e) {
            s_logger.error("Error waiting from SafeProcess output", e);
            throw new IOException(e);
        }
    }

    /*
     * Method to create a separate thread for the BluetoothProcesses.
     */
    private static BluetoothProcess execSnoop(final String[] cmdArray, final BTSnoopListener listener)
            throws IOException {

        // Serialize process executions. One at a time so we can consume all streams.
        Future<BluetoothProcess> futureSafeProcess = s_processExecutor.submit(new Callable<BluetoothProcess>() {

            @Override
            public BluetoothProcess call() throws Exception {
                Thread.currentThread().setName("BTSnoopProcessExecutor");
                BluetoothProcess bluetoothProcess = new BluetoothProcess();
                bluetoothProcess.execSnoop(cmdArray, listener);
                return bluetoothProcess;
            }
        });

        try {
            return futureSafeProcess.get();
        } catch (Exception e) {
            s_logger.error("Error waiting from SafeProcess output", e);
            throw new IOException(e);
        }
    }

    /**
     * Parse EIR data from a BLE advertising report,
     * extracting UUID, major and minor number.
     *
     * See Bluetooth Core 4.0; 8 EXTENDED INQUIRY RESPONSE DATA FORMAT
     *
     * @param b
     *            Array containing EIR data
     * @param i
     *            Index of first byte of EIR data
     * @return BeaconInfo or null if no beacon data present
     */
    private static BluetoothBeaconData parseEIRData(byte[] b, int payloadPtr, int len, String companyName) {

        for (int ptr = payloadPtr; ptr < payloadPtr + len;) {

            int structSize = b[ptr];
            if (structSize == 0) {
                break;
            }

            byte dataType = b[ptr + 1];

            if (dataType == (byte) 0xFF) { // Data-Type: Manufacturer-Specific

                int prefixPtr = ptr + 2;
                byte[] prefix = new byte[4];
                prefix[0] = (byte) Integer.parseInt(companyName.substring(2, 4), 16);
                prefix[1] = (byte) Integer.parseInt(companyName.substring(0, 2), 16);
                prefix[2] = 0x02;
                prefix[3] = 0x15;

                if (Arrays.equals(prefix, Arrays.copyOfRange(b, prefixPtr, prefixPtr + prefix.length))) {
                    BluetoothBeaconData bi = new BluetoothBeaconData();

                    int uuidPtr = ptr + 2 + prefix.length;
                    int majorPtr = uuidPtr + 16;
                    int minorPtr = uuidPtr + 18;

                    bi.uuid = "";
                    for (byte ub : Arrays.copyOfRange(b, uuidPtr, majorPtr)) {
                        bi.uuid += String.format("%02X", ub);
                    }

                    int majorl = b[majorPtr + 1] & 0xFF;
                    int majorh = b[majorPtr] & 0xFF;
                    int minorl = b[minorPtr + 1] & 0xFF;
                    int minorh = b[minorPtr] & 0xFF;
                    bi.major = majorh << 8 | majorl;
                    bi.minor = minorh << 8 | minorl;
                    bi.txpower = b[minorPtr + 2];
                    // Can't fill this in from here
                    bi.address = "";
                    return bi;
                }
            }

            ptr += structSize + 1;
        }

        return null;
    }

    /**
     * Check for advertisement out of an HCL LE Advertising Report Event
     *
     * See Bluetooth Core 4.0; 7.7.65.2 LE Advertising Report Event
     *
     * @param b
     * @return
     */
    public static BluetoothAdvertisementData parseLEAdvertisement(byte[] b) {

        BluetoothAdvertisementData btAdData = null;

        if (b[0] != 0x04 || b[1] != 0x3E) {
            // Not and Advertisement Packet
            return btAdData;
        }

        // LE Advertisement Subevent Code: 0x02
        if (b[3] != 0x02) {
            // Not a Advertisement Sub Event
            return btAdData;
        }

        // Start building Advertisement Data
        btAdData = new BluetoothAdvertisementData();
        btAdData.setRawData(b);

        btAdData.setPacketType(b[0]);
        btAdData.setEventType(b[1]);
        btAdData.setParameterLength(b[2]);
        btAdData.setSubEventCode(b[3]);

        // Number of reports in this advertisement
        btAdData.setNumberOfReports(b[4]);

        // Parse each report
        int ptr = 5;
        for (int nr = 0; nr < btAdData.getNumberOfReports(); nr++) {

            AdvertisingReportRecord arr = new AdvertisingReportRecord();
            arr.setEventType(b[ptr++]);
            arr.setAddressType(b[ptr++]);

            // Extract remote address
            String address = String.format("%02X:%02X:%02X:%02X:%02X:%02X", b[ptr + 5], b[ptr + 4], b[ptr + 3],
                    b[ptr + 2], b[ptr + 1], b[ptr + 0]);

            arr.setAddress(address);

            ptr += 6;

            int arrDataLength = b[ptr++];

            arr.setLength(b[ptr++]);
            byte[] arrData = new byte[arrDataLength];
            System.arraycopy(b, ptr, arrData, 0, arrDataLength);
            arr.setReportData(arrData);

            btAdData.addReportRecord(arr);

            ptr += arrDataLength;
        }

        return btAdData;
    }

    /**
     * Parse BLE beacons out of an HCL LE Advertising Report Event
     *
     * See Bluetooth Core 4.0; 7.7.65.2 LE Advertising Report Event
     *
     * @param b
     * @return
     */
    public static List<BluetoothBeaconData> parseLEAdvertisingReport(byte[] b, String companyName) {

        List<BluetoothBeaconData> results = new LinkedList<BluetoothBeaconData>();

        // Packet Type: Event OR Event Type: LE Advertisement Report
        if (b[0] != 0x04 || b[1] != 0x3E) {
            return results;
        }

        // LE Advertisement Subevent Code: 0x02
        if (b[3] != 0x02) {
            return results;
        }

        int numReports = b[4];

        int ptr = 5;
        for (int i = 0; i < numReports; i++) {
            ptr++;
            ptr++;

            // Extract remote address
            String address = String.format("%02X:%02X:%02X:%02X:%02X:%02X", b[ptr + 5], b[ptr + 4], b[ptr + 3],
                    b[ptr + 2], b[ptr + 1], b[ptr + 0]);
            ptr += 6;

            int len = b[ptr++];

            BluetoothBeaconData bi = parseEIRData(b, ptr, len, companyName);
            if (bi != null) {

                bi.address = address;
                bi.rssi = b[ptr + len];
                results.add(bi);
            }

            ptr += len;
        }

        return results;
    }
}
