/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothBeaconData;
import org.eclipse.kura.bluetooth.listener.AdvertisingReportRecord;
import org.eclipse.kura.bluetooth.listener.BluetoothAdvertisementData;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothUtil {

    private static final String ERROR_EXECUTING_COMMAND_MESSAGE = "Error executing command: {}";
    private static final Logger logger = LoggerFactory.getLogger(BluetoothUtil.class);
    private static final ExecutorService processExecutor = Executors.newSingleThreadExecutor();

    public static final String HCITOOL = "hcitool";
    public static final String BTDUMP = "/tmp/BluetoothUtil.btsnoopdump.sh";
    private static final String BD_ADDRESS = "BD Address:";
    private static final String HCI_VERSION = "HCI Version:";
    private static final String HCICONFIG = "hciconfig";
    private static final String GATTTOOL = "gatttool";

    private static final String DEFAULT_COMPANY_CODE = "004c";

    // Write bluetooth dumping script into /tmp
    static {
        try {
            File f = new File(BTDUMP);
            FileUtils.writeStringToFile(f, "#!/bin/bash\n" + "set -e\n" + "ADAPTER=$1\n"
                    + "{ hcidump -i $ADAPTER -R -w /dev/fd/3 >/dev/null; } 3>&1", false);

            if (!f.setExecutable(true)) {
                logger.warn("Unable to set as executable");
            }
        } catch (IOException e) {
            logger.info("Unable to update", e);
        }

    }

    private BluetoothUtil() {
        // Empty constructor
    }

    /*
     * Use hciconfig utility to return information about the bluetooth adapter
     */
    public static Map<String, String> getConfig(String name, CommandExecutorService executorService)
            throws KuraException {
        Map<String, String> props = new HashMap<>();
        String[] commandLine = { HCICONFIG, name, "version" };
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        Command command = new Command(commandLine);
        command.setTimeout(60);
        command.setOutputStream(outputStream);
        command.setErrorStream(errorStream);
        CommandStatus status = executorService.execute(command);
        if (status.getExitStatus().isSuccessful()) {
            // Check Input stream
            String[] outputLines = new String(outputStream.toByteArray(), Charsets.UTF_8).split("\n");
            // TODO: Pull more parameters from hciconfig?
            props.put("leReady", "false");
            for (String result : outputLines) {
                parseCommandResult(props, result);
            }
        } else {
            // Check Enput stream
            String[] errorLines = new String(errorStream.toByteArray(), Charsets.UTF_8).split("\n");
            for (String line : errorLines) {
                if (line.toLowerCase().contains("command not found")) {
                    throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
                } else if (line.toLowerCase().contains("no such device")) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
                }
            }
        }

        return props;
    }

    private static void parseCommandResult(Map<String, String> props, String result) {
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
            logger.trace("Bluetooth adapter address set to: {}", address);
        }
        if (result.indexOf(HCI_VERSION) >= 0 && (result.indexOf("0x6") >= 0 || result.indexOf("0x7") >= 0)) {
            // HCI version : 4.0 (0x6) or HCI version : 4.1 (0x7)
            props.put("leReady", "true");
            logger.trace("Bluetooth adapter is LE ready");
        }
    }

    /*
     * Use hciconfig utility to determine status of bluetooth adapter
     */
    public static boolean isEnabled(String name, CommandExecutorService executorService) {

        boolean isEnabled = false;
        String[] commandLine = { HCICONFIG, name };
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        Command command = new Command(commandLine);
        command.setTimeout(60);
        command.setOutputStream(outputStream);
        command.setErrorStream(errorStream);
        CommandStatus status = executorService.execute(command);
        if (status.getExitStatus().isSuccessful()) {
            String[] outputLines = new String(outputStream.toByteArray(), Charsets.UTF_8).split("\n");
            for (String line : outputLines) {
                if (line.contains("UP")) {
                    isEnabled = true;
                    break;
                }
            }
        } else {
            if (logger.isErrorEnabled()) {
                logger.error(ERROR_EXECUTING_COMMAND_MESSAGE, String.join(" ", commandLine));
            }
        }

        return isEnabled;
    }

    /*
     * Utility method that allows sending any hciconfig command. The buffered
     * response is returned in case results are needed.
     */
    public static String hciconfigCmd(String name, String cmd, CommandExecutorService executorService) {
        String outputString = "";
        String[] commandLine = { HCICONFIG, name, cmd };
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        Command command = new Command(commandLine);
        command.setTimeout(60);
        command.setOutputStream(outputStream);
        command.setErrorStream(errorStream);
        CommandStatus status = executorService.execute(command);
        if (status.getExitStatus().isSuccessful()) {
            outputString = new String(outputStream.toByteArray(), Charsets.UTF_8);
        } else {
            if (logger.isErrorEnabled()) {
                logger.error(ERROR_EXECUTING_COMMAND_MESSAGE, String.join(" ", commandLine));
            }
        }
        return outputString;
    }

    /*
     * Utility method to send specific kill commands to processes.
     */
    public static void killCmd(String[] cmd, Signal signal, CommandExecutorService executorService) {
        // Get the pids and filter the ones that exactly match the command
        Map<String, Pid> pids = executorService.getPids(cmd).entrySet().stream()
                .filter(entry -> entry.getKey().equals(String.join(" ", cmd)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (Pid pid : pids.values()) {
            if (!executorService.stop(pid, signal)) {
                logger.warn("Failed to stop command with pid {}", pid.getPid());
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
    public static BluetoothProcess btdumpCmd(String name, BTSnoopListener listener,
            CommandExecutorService executorService) throws IOException {
        String[] command = { BTDUMP, name };
        return execSnoop(command, listener, executorService);
    }

    /*
     * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
     * BluetoothProcessListener is used to receive output from the process.
     */
    public static BluetoothProcess hcitoolCmd(String name, String cmd, BluetoothProcessListener listener,
            CommandExecutorService executorService) throws IOException {
        String[] command = { HCITOOL, "-i", name, cmd };
        return exec(command, listener, executorService);
    }

    /*
     * Method to utilize BluetoothProcess and the hcitool utility. These processes run indefinitely, so the
     * BluetoothProcessListener is used to receive output from the process.
     */
    public static BluetoothProcess hcitoolCmd(String name, String[] cmd, BluetoothProcessListener listener,
            CommandExecutorService executorService) throws IOException {
        String[] command = new String[3 + cmd.length];
        command[0] = HCITOOL;
        command[1] = "-i";
        command[2] = name;
        for (int i = 0; i < cmd.length; i++) {
            command[i + 3] = cmd[i];
        }
        return exec(command, listener, executorService);
    }

    /*
     * Method to start an interactive session with a remote Bluetooth LE device using the gatttool utility. The
     * listener is used to receive output from the process.
     */
    public static BluetoothProcess startSession(String adapterName, String address, BluetoothProcessListener listener,
            CommandExecutorService executorService) throws IOException {
        String[] command = { GATTTOOL, "-i", adapterName, "-b", address, "-I" };
        return exec(command, listener, executorService);
    }

    /*
     * Method to create a separate thread for the BluetoothProcesses.
     */
    private static BluetoothProcess exec(final String[] cmdArray, final BluetoothProcessListener listener,
            CommandExecutorService executorService) throws IOException {

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
    private static BluetoothProcess execSnoop(final String[] cmdArray, final BTSnoopListener listener,
            CommandExecutorService executorService) throws IOException {

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
                companyName = inSetHex(inSetRange(companyName, 4, DEFAULT_COMPANY_CODE), DEFAULT_COMPANY_CODE);
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

    private static String inSetRange(String value, int range, String defaultValue) {
        if (value.length() != range) {
            return defaultValue;
        } else {
            return value;
        }
    }

    private static String inSetHex(String value, String defaultValue) {
        if (!value.matches("^[0-9a-fA-F]+$")) {
            return defaultValue;
        } else {
            return value;
        }
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

        List<BluetoothBeaconData> results = new LinkedList<>();

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

    public static boolean stopHcitool(String interfaceName, CommandExecutorService executorService, String... params) {
        List<String> killCommand = new ArrayList<>();
        killCommand.add(HCITOOL);
        killCommand.add("-i");
        killCommand.add(interfaceName);
        Arrays.asList(params).stream().forEach(s -> killCommand.add(s));
        return executorService.kill(killCommand.toArray(new String[0]), LinuxSignal.SIGINT);
    }

    public static boolean stopBtdump(String interfaceName, CommandExecutorService executorService) {
        String[] killCommand = { BTDUMP, interfaceName };
        return executorService.kill(killCommand, null);
    }

}
