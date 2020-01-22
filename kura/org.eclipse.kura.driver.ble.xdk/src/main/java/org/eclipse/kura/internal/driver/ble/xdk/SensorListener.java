/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.driver.ble.xdk;

import static org.eclipse.kura.channel.ChannelFlag.FAILURE;
import static org.eclipse.kura.channel.ChannelFlag.SUCCESS;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;

public class SensorListener {

    private final Xdk xdk;
    private final String sensorType;
    private final List<ChannelListener> listeners;
    private final List<String> channelNames;
    private final List<SensorName> sensorNames;
    private final List<DataType> dataTypes;

    public SensorListener(Xdk xdk, String sensorType) {
        this.xdk = xdk;
        this.sensorType = sensorType;
        this.channelNames = new ArrayList<>();
        this.sensorNames = new ArrayList<>();
        this.dataTypes = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public Xdk getXdk() {
        return this.xdk;
    }

    public List<DataType> getDataTypes() {
        return this.dataTypes;
    }

    public List<ChannelListener> getListeners() {
        return this.listeners;
    }

    public List<String> getChannelNames() {
        return this.channelNames;
    }

    public List<SensorName> getSensorNames() {
        return this.sensorNames;
    }

    public void addListener(ChannelListener listener) {
        this.listeners.add(listener);
    }

    public void addChannelName(String name) {
        this.channelNames.add(name);
    }

    public void addSensorName(SensorName name) {
        this.sensorNames.add(name);
    }

    public void addDataType(DataType type) {
        this.dataTypes.add(type);
    }

    public String getSensorType() {
        return this.sensorType;
    }

    public void removeAll(int index) {
        this.channelNames.remove(index);
        this.sensorNames.remove(index);
        this.dataTypes.remove(index);
        this.listeners.remove(index);
    }

    public static <T> Consumer<T> getSensorConsumer(SensorListener listener) {
        return value -> {
            for (int index = 0; index < listener.getChannelNames().size(); index++) {
                ChannelRecord record = ChannelRecord.createReadRecord(listener.getChannelNames().get(index),
                        listener.getDataTypes().get(index));
                final Optional<TypedValue<?>> typedValue = getOptionalTypedValue(listener, index, value);
                if (!typedValue.isPresent()) {
                    record.setChannelStatus(new ChannelStatus(FAILURE,
                            "Error while converting the retrieved value to the defined typed", null));
                    record.setTimestamp(System.currentTimeMillis());
                    return;
                }

                record.setValue(typedValue.get());
                record.setChannelStatus(new ChannelStatus(SUCCESS));
                record.setTimestamp(System.currentTimeMillis());
                listener.getListeners().get(index).onChannelEvent(new ChannelEvent(record));
            }
        };
    }

    private static Optional<TypedValue<?>> getOptionalTypedValue(SensorListener listener, int index, Object value) {
        Optional<TypedValue<?>> typedValue = Optional.empty();
        switch (listener.getSensorType()) {
        case "ACCELERATION_X":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 0));
            break;
        case "ACCELERATION_Y":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 1));
            break;
        case "ACCELERATION_Z":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 2));
            break;
        case "GYROSCOPE_X":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 3));
            break;
        case "GYROSCOPE_Y":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 4));
            break;
        case "GYROSCOPE_Z":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 5));
            break;
        case "LIGHT":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 0));
            break;
        case "NOISE":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 1));
            break;
        case "PRESSURE":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 2));
            break;
        case "TEMPERATURE":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 3));
            break;
        case "HUMIDITY":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 4));
            break;
        case "SD_CARD_DETECT_STATUS":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 5));
            break;
        case "BUTTONS_STATUS":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 6));
            break;
        case "MAGNETIC_X":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 0));
            break;
        case "MAGNETIC_Y":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 1));
            break;
        case "MAGNETIC_Z":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 2));
            break;
        case "MAGNETOMETER_RESISTANCE":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 3));
            break;
        case "LED_STATUS":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 4));
            break;
        case "VOLTAGE_LEM":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 5));
            break;
        case "QUATERNION_M":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 6));
            break;
        case "QUATERNION_X":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 7));
            break;
        case "QUATERNION_Y":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 8));
            break;
        case "QUATERNION_Z":
            typedValue = XdkDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 9));
            break;
        default:
        }
        return typedValue;
    }
}