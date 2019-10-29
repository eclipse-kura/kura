/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and others
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.bluetooth.BluetoothGatt;
import org.eclipse.kura.bluetooth.BluetoothGattCharacteristic;
import org.eclipse.kura.bluetooth.BluetoothGattSecurityLevel;
import org.eclipse.kura.bluetooth.BluetoothGattService;
import org.eclipse.kura.bluetooth.BluetoothLeNotificationListener;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcess;
import org.eclipse.kura.linux.bluetooth.util.BluetoothProcessListener;
import org.eclipse.kura.linux.bluetooth.util.BluetoothUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothGattImpl implements BluetoothGatt, BluetoothProcessListener {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothGattImpl.class);

    private static final long GATT_CONNECTION_TIMEOUT = 10000;
    private static final long GATT_SERVICE_TIMEOUT = 6000;
    private static final long GATT_COMMAND_TIMEOUT = 2000;

    private static final String ANSI_ESCAPE_SEQUENCES = "\\x1b\\[(K|[^m]+m)";
    private static final String[] NOT_CONNECTED = { "[   ]", "disconnected", "not connected", "error: connect" };
    private static final String[] CONNECTED = { "[con]", "connection successful", "usage: mtu <value>" };
    private static final String SERVICES = "attr handle:";
    private static final String CHARACTERISTICS = "handle:";
    private static final String READ_CHAR = "characteristic value/descriptor:";
    private static final String REGEX_READ_CHAR_UUID = "handle\\:.*value\\:[\\s|0-9|a-f|A-F]*";
    private static final String NOTIFICATION = "notification handle";
    private static final String ERROR_HANDLE = "invalid handle";
    private static final String[] ERROR_UUID = { "invalid uuid",
            "read characteristics by uuid failed: attribute can't be read" };
    private static final String ERROR_CHAR_MESSAGE = "Exception waiting for characteristics";
    private static final String ERROR_GATT_READ_MESSAGE = "Gatttool read timeout.";
    private static final String ERROR_GATT_TIMEOUT_MESSAGE = "Gatttool read error.";
    private static final String ERROR = "ERROR";

    private List<BluetoothGattService> bluetoothServices;
    private List<BluetoothGattCharacteristic> bluetoothGattCharacteristics;
    private BluetoothLeNotificationListener listener;
    private String charValue;
    private String charValueUuid;
    private String securityLevel;

    private BluetoothProcess proc;
    private BufferedWriter bufferedWriter;
    private boolean isConnected = false;
    private boolean ready = false;
    private StringBuilder stringBuilder = null;
    private final String address;
    private final CommandExecutorService executorService;

    public BluetoothGattImpl(String address, CommandExecutorService executorService) {
        this.address = address;
        this.executorService = executorService;
    }

    // --------------------------------------------------------------------
    //
    // BluetoothGatt API
    //
    // --------------------------------------------------------------------
    @Override
    public boolean connect() throws KuraException {
        return connect("hci0");
    }

    @Override
    public boolean connect(String adapterName) throws KuraException {
        try {
            this.proc = BluetoothUtil.startSession(adapterName, this.address, this, this.executorService);
        } catch (IOException e) {
            throw new KuraProcessExecutionErrorException(e, "Failed to start gatttool session");
        }
        if (this.proc != null) {
            this.bufferedWriter = this.proc.getWriter();
            logger.info("Sending connect message...");
            this.ready = false;
            String command = "connect\n";
            sendCmd(command);

            // Wait for connection or timeout
            long startTime = System.currentTimeMillis();
            while (!this.ready && System.currentTimeMillis() - startTime < GATT_CONNECTION_TIMEOUT) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.error("Exception waiting for connection", e);
                    Thread.currentThread().interrupt();
                }
            }
            if (!this.ready) {
                throw new KuraTimeoutException("Gatttool connection timeout.");
            }
        }

        return this.isConnected;
    }

    @Override
    public void disconnect() {
        if (this.proc != null) {
            String command = "exit\n";
            sendCmd(command);
            this.proc.destroy();
            this.proc = null;
            logger.info("Disconnected");
        }
    }

    @Override
    public boolean checkConnection() throws KuraException {
        if (this.proc != null) {
            this.bufferedWriter = this.proc.getWriter();
            logger.info("Check for connection...");
            this.ready = false;
            // Since in Bluez-5.x the connection status is shown by blue text, use mtu command to check connection :-)
            String command = "mtu\n";
            sendCmd(command);

            // Wait for connection or timeout
            long startTime = System.currentTimeMillis();
            while (!this.ready && System.currentTimeMillis() - startTime < GATT_CONNECTION_TIMEOUT) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.error("Exception waiting for connection", e);
                    Thread.currentThread().interrupt();
                }
            }
            if (!this.ready) {
                throw new KuraTimeoutException("Gatttool connection timeout.");
            }
        }

        return this.isConnected;
    }

    @Override
    public void setBluetoothLeNotificationListener(BluetoothLeNotificationListener listener) {
        this.listener = listener;
    }

    @Override
    public BluetoothGattService getService(UUID uuid) {
        return null;
    }

    @Override
    public List<BluetoothGattService> getServices() {
        if (this.proc != null) {
            this.bluetoothServices = new ArrayList<>();
            String command = "primary\n";
            sendCmd(command);
            try {
                Thread.sleep(GATT_SERVICE_TIMEOUT);
            } catch (InterruptedException e) {
                logger.error("Exception waiting for services", e);
                Thread.currentThread().interrupt();
            }
        }
        return this.bluetoothServices;
    }

    @Override
    public List<BluetoothGattCharacteristic> getCharacteristics(String startHandle, String endHandle) {
        logger.info("getCharacteristics {} : {}", startHandle, endHandle);
        if (this.proc != null) {
            this.bluetoothGattCharacteristics = new ArrayList<>();
            String command = "characteristics " + startHandle + " " + endHandle + "\n";
            sendCmd(command);
            try {
                Thread.sleep(GATT_SERVICE_TIMEOUT);
            } catch (InterruptedException e) {
                logger.error(ERROR_CHAR_MESSAGE, e);
                Thread.currentThread().interrupt();
            }
        }
        return this.bluetoothGattCharacteristics;
    }

    @Override
    public String readCharacteristicValue(String handle) throws KuraException {
        if (this.proc != null) {
            this.charValue = "";
            String command = "char-read-hnd " + handle + "\n";
            sendCmd(command);

            // Wait until read is complete, error is received or timeout
            long startTime = System.currentTimeMillis();
            while ("".equals(this.charValue) && !this.charValue.startsWith(ERROR)
                    && System.currentTimeMillis() - startTime < GATT_COMMAND_TIMEOUT) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.error(ERROR_CHAR_MESSAGE, e);
                    Thread.currentThread().interrupt();
                }
            }
            if ("".equals(this.charValue)) {
                throw new KuraTimeoutException(ERROR_GATT_READ_MESSAGE);
            }
            if (this.charValue.startsWith(ERROR)) {
                throw KuraException.internalError(ERROR_GATT_TIMEOUT_MESSAGE);
            }

        }

        return this.charValue;
    }

    @Override
    public String readCharacteristicValueByUuid(UUID uuid) throws KuraException {
        if (this.proc != null) {
            this.charValueUuid = "";
            String uuidString = uuid.toString();
            String command = "char-read-uuid " + uuidString + "\n";
            logger.info("send command : {}", command);
            sendCmd(command);

            // Wait until read is complete, error is received or timeout
            long startTime = System.currentTimeMillis();
            while ("".equals(this.charValueUuid) && !this.charValueUuid.startsWith(ERROR)
                    && System.currentTimeMillis() - startTime < GATT_COMMAND_TIMEOUT) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.error(ERROR_CHAR_MESSAGE, e);
                    Thread.currentThread().interrupt();
                }
            }
            if ("".equals(this.charValueUuid)) {
                throw new KuraTimeoutException(ERROR_GATT_READ_MESSAGE);
            }
            if (this.charValueUuid.startsWith(ERROR)) {
                throw KuraException.internalError(ERROR_GATT_TIMEOUT_MESSAGE);
            }
        }

        return this.charValueUuid;
    }

    @Override
    public void writeCharacteristicValue(String handle, String value) {
        if (this.proc != null) {
            this.charValueUuid = null;
            String command = "char-write-cmd " + handle + " " + value + "\n";
            sendCmd(command);
        }
    }

    @Override
    public void processInputStream(int ch) {
        if (this.stringBuilder == null) {
            this.stringBuilder = new StringBuilder();
        }

        // Process stream once newline, carriage return, or > char is received.
        // '>' indicates the gatttool prompt has returned.
        if (ch == 0xA || ch == 0xD || (char) ch == '>') {
            this.stringBuilder.append((char) ch);
            processLine(this.stringBuilder.toString().replaceAll(ANSI_ESCAPE_SEQUENCES, ""));
            this.stringBuilder.setLength(0);
        } else {
            this.stringBuilder.append((char) ch);
        }
    }

    @Override
    public void processInputStream(String string) {
        // Not implemented
    }

    @Override
    public void processErrorStream(String string) {
        // Not implemented
    }

    @Override
    public BluetoothGattSecurityLevel getSecurityLevel() throws KuraException {

        BluetoothGattSecurityLevel level = BluetoothGattSecurityLevel.UNKNOWN;
        if (this.isConnected && this.proc != null) {
            this.securityLevel = "";
            this.bufferedWriter = this.proc.getWriter();
            logger.info("Get security level...");
            String command = "sec-level\n";
            sendCmd(command);

            // Wait until read is complete, error is received or timeout
            long startTime = System.currentTimeMillis();
            while ("".equals(this.securityLevel) && !this.securityLevel.startsWith(ERROR)
                    && System.currentTimeMillis() - startTime < GATT_COMMAND_TIMEOUT) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.error(ERROR_CHAR_MESSAGE, e);
                    Thread.currentThread().interrupt();
                }
            }
            if ("".equals(this.securityLevel)) {
                throw new KuraTimeoutException(ERROR_GATT_READ_MESSAGE);
            } else if (this.securityLevel.startsWith(ERROR)) {
                throw KuraException.internalError(ERROR_GATT_TIMEOUT_MESSAGE);
            }

            level = BluetoothGattSecurityLevel.getBluetoothGattSecurityLevel(this.securityLevel);
        }

        return level;
    }

    @Override
    public void setSecurityLevel(BluetoothGattSecurityLevel level) {

        if (this.isConnected && this.proc != null) {
            this.bufferedWriter = this.proc.getWriter();
            logger.debug("Set security level to {}", level.toString());
            String command = "sec-level " + level.toString().toLowerCase() + "\n";
            sendCmd(command);
        }

    }

    // --------------------------------------------------------------------
    //
    // Private methods
    //
    // --------------------------------------------------------------------

    private void sendCmd(String command) {
        try {
            logger.debug("send command = {}", command);
            this.bufferedWriter.write(command);
            this.bufferedWriter.flush();
        } catch (IOException e) {
            logger.error("Error writing command: {}", command, e);
        }
    }

    private void processLine(String line) {

        logger.debug("Processing line : {}", line);

        // gatttool prompt indicates not connected, but session started
        if (checkString(line.toLowerCase(), NOT_CONNECTED)) {
            this.isConnected = false;
            this.ready = false;
        }
        // gatttool prompt indicates connected
        else if (checkString(line.toLowerCase(), CONNECTED)) {
            this.isConnected = true;
            this.ready = true;
        }
        // characteristic read by UUID returned
        else if (line.matches(REGEX_READ_CHAR_UUID)) {
            logger.debug("Characteristic value by UUID received: {}", line);
            // Parse the characteristic line, line is expected to be:
            // handle: 0xmmmm value: <value>
            String[] attr = line.split(":");
            this.charValueUuid = attr[2].trim();
            logger.info("m_charValueUuid: {}", this.charValueUuid);
        }
        // services are being returned
        else if (line.toLowerCase().startsWith(SERVICES)) {
            logger.debug("Service : {}", line);
            // Parse the services line, line is expected to be:
            // attr handle: 0xnnnn, end grp handle: 0xmmmm uuid: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
            String[] attr = line.split("\\s");
            String startHandle = attr[2].substring(0, attr[2].length() - 1);
            String endHandle = attr[6];
            String uuid = attr[8];

            if (this.bluetoothServices != null && isNewService(uuid)) {
                logger.debug("Adding new GATT service: {} : {} : {}", uuid, startHandle, endHandle);
                this.bluetoothServices.add(new BluetoothGattServiceImpl(uuid, startHandle, endHandle));
            }
        }
        // characteristics are being returned
        else if (line.toLowerCase().startsWith(CHARACTERISTICS)) {
            logger.debug("Characteristic : {}", line);
            // Parse the characteristic line, line is expected to be:
            // handle: 0xnnnn, char properties: 0xmm, char value handle: 0xpppp, uuid:
            // xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
            String[] attr = line.split(" ");
            String handle = attr[1].substring(0, attr[1].length() - 1);
            String properties = attr[4].substring(0, attr[4].length() - 1);
            String valueHandle = attr[8].substring(0, attr[8].length() - 1);
            String uuid = attr[10].substring(0, attr[10].length() - 1);
            if (this.bluetoothGattCharacteristics != null && isNewGattCharacteristic(uuid)) {
                logger.debug("Adding new GATT characteristic: {}", uuid);
                logger.debug("{} {} {}", handle, properties, valueHandle);
                this.bluetoothGattCharacteristics
                        .add(new BluetoothGattCharacteristicImpl(uuid, handle, properties, valueHandle));
            }
        }
        // characteristic read by handle returned
        else if (line.toLowerCase().contains(READ_CHAR)) {
            logger.debug("Characteristic value by handle received: {}", line);
            // Parse the characteristic line, line is expected to be:
            // Characteristic value/descriptor: <value>
            String[] attr = line.split(":");
            this.charValue = attr[1].trim();

        }
        // receiving notifications, need to notify listener
        else if (line.toLowerCase().contains(NOTIFICATION)) {
            logger.debug("Receiving notification: {}", line);
            // Parse the characteristic line, line is expected to be:
            // Notification handle = 0xmmmm value: <value>
            String sub = line.substring(line.toLowerCase().indexOf(NOTIFICATION) + NOTIFICATION.length() + 3).trim();
            String[] attr = sub.split(":");
            String handle = attr[0].split("\\s")[0];
            String value = attr[1].trim();
            this.listener.onDataReceived(handle, value);
        }
        // error reading handle
        else if (line.toLowerCase().contains(ERROR_HANDLE)) {
            logger.info("ERROR_HANDLE");
            this.charValue = "ERROR: Invalid handle!";
        }
        // error reading UUID
        else if (checkString(line.toLowerCase(), ERROR_UUID)) {
            logger.info("ERROR_UUID");
            this.charValueUuid = "ERROR: Invalid UUID!";
        }
        // get security level
        else if (line.toLowerCase().startsWith("sec-level:")) {
            logger.debug("Received security level : {}",
                    line.toLowerCase().substring("sec-level: ".length(), line.toLowerCase().length()));
            this.securityLevel = line.toLowerCase().substring("sec-level: ".length(), line.toLowerCase().length())
                    .replace("\n", "").replace("\r", "");
        }

    }

    private boolean checkString(String line, String[] lines) {

        for (String item : lines) {
            if (line.contains(item)) {
                return true;
            }
        }
        return false;

    }

    private boolean isNewService(String uuid) {

        for (BluetoothGattService service : this.bluetoothServices) {
            if (service.getUuid().toString().equals(uuid)) {
                return false;
            }
        }
        return true;
    }

    private boolean isNewGattCharacteristic(String uuid) {

        for (BluetoothGattCharacteristic characteristic : this.bluetoothGattCharacteristics) {
            if (characteristic.getUuid().toString().equals(uuid)) {
                return false;
            }
        }
        return true;

    }

}