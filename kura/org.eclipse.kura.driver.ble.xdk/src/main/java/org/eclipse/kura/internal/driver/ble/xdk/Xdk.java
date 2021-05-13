/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.driver.ble.xdk;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattCharacteristic;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Xdk {

    private static final Logger logger = LoggerFactory.getLogger(Xdk.class);

    private static final String SENSOR = "sensor";
    private static final String RATE = "rate";
    private static final String SENSOR_FUSION = "sensor fusion";
    private static final String HIGH_PRIORITY_ARRAY = "high priority array";
    private static final String LOW_PRIORITY_ARRAY = "low priority array";

    private static final int SERVICE_TIMEOUT = 10000;

    private final byte[] value = { 0x01 };
    private final byte[] sensorFusion = { 0x00 };

    private BluetoothLeDevice device;
    private final Map<String, XdkGattResources> gattResources;

    public Xdk(BluetoothLeDevice bluetoothLeDevice) {
        this.device = bluetoothLeDevice;
        this.gattResources = new HashMap<>();
    }

    public BluetoothLeDevice getBluetoothLeDevice() {
        return this.device;
    }

    public void setBluetoothLeDevice(BluetoothLeDevice device) {
        this.device = device;
    }

    public boolean isConnected() {
        return this.device.isConnected();
    }

    public void connect() throws ConnectionException {
        try {
            this.device.connect();
            Long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < SERVICE_TIMEOUT) {
                if (this.device.isServicesResolved()) {
                    break;
                }
                XdkDriver.waitFor(1000);
            }
            if (!isConnected() || !this.device.isServicesResolved()) {
                throw new ConnectionException("Connection failed");
            }
        } catch (KuraBluetoothConnectionException e) {
            throw new ConnectionException(e);
        }

    }

    public void init() throws ConnectionException {
        if (isConnected() && this.gattResources.size() != 8) {
            getGattResources();
        }
    }

    public void disconnect() throws ConnectionException {
        if (isHighNotifying()) {
            disableHighNotifications();
        }
        if (isLowNotifying()) {
            disableLowNotifications();
        }
        try {
            this.device.disconnect();
            if (isConnected()) {
                throw new ConnectionException("Disconnection failed");
            }
        } catch (KuraBluetoothConnectionException e) {
            throw new ConnectionException(e);
        }
        XdkDriver.waitFor(1000);
    }

    public List<BluetoothLeGattCharacteristic> getCharacteristics() {
        List<BluetoothLeGattCharacteristic> characteristics = new ArrayList<>();
        for (Entry<String, XdkGattResources> entry : this.gattResources.entrySet()) {
            try {
                characteristics.addAll(entry.getValue().getGattService().findCharacteristics());
            } catch (KuraException e) {
                logger.error("Failed to get characteristic", e);
            }
        }
        return characteristics;
    }

    public void startSensor(boolean enableQuaternion, int configSimpleRate) {

        if (enableQuaternion) {
            this.sensorFusion[0] = 0x01;
        } else {
            this.sensorFusion[0] = 0x00;
        }

        byte[] rateSamples = intToBytesArray(configSimpleRate);

        try {

            this.gattResources.get(RATE).getGattService()
                    .findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_CHANGE_SENSOR_SAMPLING_RATA)
                    .writeValue(rateSamples);

            this.gattResources.get(SENSOR).getGattService()
                    .findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION)
                    .writeValue(this.value);

            this.gattResources.get(SENSOR_FUSION).getGattService()
                    .findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_CONTROL_NODE_USE_SENSOR_FUSION)
                    .writeValue(this.sensorFusion);

        } catch (KuraException e) {
            logger.error("Sensor start failed", e);
        }
    }

    public float[] readHighData() {
        float[] hightData = new float[10];
        try {
            hightData = calculateHighData(
                    this.gattResources.get(HIGH_PRIORITY_ARRAY).getGattValueCharacteristic().readValue());
        } catch (KuraException e) {
            logger.error("High Data read failed", e);
        }
        return hightData;
    }

    public Integer[] readLowData(byte id) {

        Integer[] lowData = new Integer[7];
        byte[] data;

        try {
            data = this.gattResources.get(LOW_PRIORITY_ARRAY).getGattValueCharacteristic().readValue();
            while (data[0] != id) {
                data = this.gattResources.get(LOW_PRIORITY_ARRAY).getGattValueCharacteristic().readValue();
            }

            lowData = calculateLowData(data, id);
        } catch (KuraException e) {
            logger.error("Low Data read failed", e);
        }
        return lowData;
    }

    public void enableHighNotifications(Consumer<float[]> callback) {
        Consumer<byte[]> callbackHigh = valueBytes -> callback.accept(calculateHighData(valueBytes));
        try {
            this.gattResources.get(HIGH_PRIORITY_ARRAY).getGattService()
                    .findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY)
                    .enableValueNotifications(callbackHigh);
        } catch (KuraException e) {
            logger.error("Notification enable failed", e);
        }
    }

    public void enableLowNotifications(Consumer<Integer[]> callback, byte id) {
        Consumer<byte[]> callbackHigh = valueBytes -> {
            if (valueBytes[0] == id) {
                callback.accept(calculateLowData(valueBytes, id));
            }
        };
        try {
            this.gattResources.get(LOW_PRIORITY_ARRAY).getGattService()
                    .findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY)
                    .enableValueNotifications(callbackHigh);
        } catch (KuraException e) {
            logger.error("Notification enable failed", e);
        }
    }

    public void disableHighNotifications() {
        try {
            this.gattResources.get(HIGH_PRIORITY_ARRAY).getGattService()
                    .findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY)
                    .disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Notification disable failed", e);
        }
    }

    public void disableLowNotifications() {
        try {
            this.gattResources.get(LOW_PRIORITY_ARRAY).getGattService()
                    .findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY).disableValueNotifications();
        } catch (KuraException e) {
            logger.error("Notification disable failed", e);
        }
    }

    public boolean isHighNotifying() {
        return isNotifying(HIGH_PRIORITY_ARRAY);
    }

    public boolean isLowNotifying() {
        return isNotifying(LOW_PRIORITY_ARRAY);
    }

    private float[] calculateHighData(byte[] valueByte) {

        float[] highData = new float[10];

        if (this.sensorFusion[0] == 0x00) {
            int ax = shortSignedAtOffset(valueByte, 0);
            int ay = shortSignedAtOffset(valueByte, 2);
            int az = shortSignedAtOffset(valueByte, 4);

            int gx = shortSignedAtOffset(valueByte, 6);
            int gy = shortSignedAtOffset(valueByte, 8);
            int gz = shortSignedAtOffset(valueByte, 10);

            highData[0] = ax;
            highData[1] = ay;
            highData[2] = az;
            highData[3] = gx;
            highData[4] = gy;
            highData[5] = gz;
            highData[6] = 0;
            highData[7] = 0;
            highData[8] = 0;
            highData[9] = 0;

        } else {
            float dataM = fromByteArrayToFloat(splitBytesArray(valueByte, 0));
            float dataX = fromByteArrayToFloat(splitBytesArray(valueByte, 4));
            float dataY = fromByteArrayToFloat(splitBytesArray(valueByte, 8));
            float dataZ = fromByteArrayToFloat(splitBytesArray(valueByte, 12));

            highData[0] = 0;
            highData[1] = 0;
            highData[2] = 0;
            highData[3] = 0;
            highData[4] = 0;
            highData[5] = 0;
            highData[6] = dataM;
            highData[7] = dataX;
            highData[8] = dataY;
            highData[9] = dataZ;

        }

        return highData;
    }

    private Integer[] calculateLowData(byte[] valueByte, byte id) {
        Integer[] lowData = new Integer[7];

        if (id == 0x01) {

            Integer lux = thirtyTwoBitUnsignedAtOffset(valueByte, 1) / 1000;
            Integer noise = eightBitUnsignedAtOffset(valueByte, 5);
            Integer pressure = thirtyTwoBitUnsignedAtOffset(valueByte, 6);
            Integer temperature = thirtyTwoBitShortSignedAtOffset(valueByte, 10) / 1000;
            Integer humidity = thirtyTwoBitUnsignedAtOffset(valueByte, 14);
            Integer sdCard = valueByte[18] & 0xFF;
            Integer button = valueByte[19] & 0xFF;

            lowData[0] = lux;
            lowData[1] = noise;
            lowData[2] = pressure;
            lowData[3] = temperature;
            lowData[4] = humidity;
            lowData[5] = sdCard;
            lowData[6] = button;

        } else {

            Integer mx = shortSignedAtOffset(valueByte, 1);
            Integer my = shortSignedAtOffset(valueByte, 3);
            Integer mz = shortSignedAtOffset(valueByte, 5);
            Integer mRes = shortSignedAtOffset(valueByte, 7);
            Integer led = valueByte[9] & 0xFF;
            Integer voltage = shortSignedAtOffset(valueByte, 10) / 1000;
            Integer none = 0x00;

            lowData[0] = mx;
            lowData[1] = my;
            lowData[2] = mz;
            lowData[3] = mRes;
            lowData[4] = led;
            lowData[5] = voltage;
            lowData[6] = none;

        }

        return lowData;

    }

    private Integer shortSignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = c[offset] & 0xFF;
        Integer upperByte = (int) c[offset + 1];
        return (upperByte << 8) + lowerByte;
    }

    private int thirtyTwoBitShortSignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = c[offset] & 0xFF;
        Integer mediumByteA = c[offset + 1] & 0xFF;
        Integer mediumByteB = c[offset + 2] & 0xFF;
        Integer upperByte = (int) c[offset + 3];
        return (upperByte << 24) + (mediumByteB << 16) + (mediumByteA << 8) + lowerByte;
    }

    private Integer eightBitUnsignedAtOffset(byte[] c, int offset) {

        return c[offset] & 0xFF;
    }

    private Integer thirtyTwoBitUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = c[offset] & 0xFF;
        Integer mediumByteA = c[offset + 1] & 0xFF;
        Integer mediumByteB = c[offset + 2] & 0xFF;
        Integer upperByte = c[offset + 3] & 0xFF;
        return (upperByte << 24) + (mediumByteB << 16) + (mediumByteA << 8) + lowerByte;
    }

    private byte[] splitBytesArray(byte[] c, int offset) {
        byte[] split = new byte[4];
        split[3] = c[offset];
        split[2] = c[offset + 1];
        split[1] = c[offset + 2];
        split[0] = c[offset + 3];
        return split;
    }

    private byte[] intToBytesArray(int rate) {

        byte[] b = ByteBuffer.allocate(4).putInt(rate).array();
        byte[] rateBytes = new byte[4];
        rateBytes[0] = b[3];
        rateBytes[1] = b[2];
        rateBytes[2] = b[1];
        rateBytes[3] = b[1];

        return rateBytes;
    }

    private float fromByteArrayToFloat(byte[] c) {
        ByteBuffer buffer = ByteBuffer.wrap(c);
        return buffer.getFloat();
    }

    private void getGattResources() throws ConnectionException {
        try {
            BluetoothLeGattService controlService = this.device.findService(XdkGatt.UUID_XDK_CONTROL_SERVICE);
            BluetoothLeGattService dataService = this.device.findService(XdkGatt.UUID_XDK_HIGH_DATA_RATE);

            BluetoothLeGattCharacteristic sensorCharacteristic = controlService
                    .findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_START_SENSOR_SAMPLING_AND_NOTIFICATION);
            BluetoothLeGattCharacteristic rateCharacteristic = controlService
                    .findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_CHANGE_SENSOR_SAMPLING_RATA);
            BluetoothLeGattCharacteristic fusionCharacteristic = controlService
                    .findCharacteristic(XdkGatt.UUID_XDK_CONTROL_SERVICE_CONTROL_NODE_USE_SENSOR_FUSION);

            BluetoothLeGattCharacteristic highDataCharacteristic = dataService
                    .findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_HIGH_PRIORITY_ARREY);
            BluetoothLeGattCharacteristic lowDataCharacteristic = dataService
                    .findCharacteristic(XdkGatt.UUID_XDK_HIGH_DATA_RATE_LOW_PRIORITY_ARREY);

            XdkGattResources sensorGattResources = new XdkGattResources(SENSOR, controlService, sensorCharacteristic);
            XdkGattResources rateGattResources = new XdkGattResources(RATE, controlService, rateCharacteristic);
            XdkGattResources fusionGattResources = new XdkGattResources(RATE, controlService, fusionCharacteristic);
            //
            XdkGattResources highDataGattResources = new XdkGattResources(HIGH_PRIORITY_ARRAY, dataService,
                    highDataCharacteristic);
            XdkGattResources lowDataGattResources = new XdkGattResources(LOW_PRIORITY_ARRAY, dataService,
                    lowDataCharacteristic);

            this.gattResources.put(SENSOR, sensorGattResources);

            this.gattResources.put(RATE, rateGattResources);

            this.gattResources.put(SENSOR_FUSION, fusionGattResources);

            this.gattResources.put(HIGH_PRIORITY_ARRAY, highDataGattResources);

            this.gattResources.put(LOW_PRIORITY_ARRAY, lowDataGattResources);

        } catch (KuraBluetoothResourceNotFoundException e) {
            logger.error("Failed to get GATT service", e);
            disconnect();
        }
    }

    private boolean isNotifying(String resourceName) {
        XdkGattResources resource = this.gattResources.get(resourceName);
        if (resource != null) {
            return resource.getGattValueCharacteristic().isNotifying();
        } else {
            return false;
        }
    }

}
