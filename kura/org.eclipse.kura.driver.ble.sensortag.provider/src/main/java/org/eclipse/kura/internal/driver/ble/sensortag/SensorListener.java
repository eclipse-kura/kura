/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.driver.ble.sensortag;

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

    private final TiSensorTag sensorTag;
    private final String sensorType;
    private final int period;
    private final List<ChannelListener> listeners;
    private final List<String> channelNames;
    private final List<SensorName> sensorNames;
    private final List<DataType> dataTypes;

    public SensorListener(TiSensorTag sensorTag, String sensorType, int period) {
        this.sensorTag = sensorTag;
        this.sensorType = sensorType;
        this.period = period;
        this.channelNames = new ArrayList<>();
        this.sensorNames = new ArrayList<>();
        this.dataTypes = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public TiSensorTag getSensorTag() {
        return this.sensorTag;
    }

    public List<DataType> getDataTypes() {
        return this.dataTypes;
    }

    public List<ChannelListener> getListeners() {
        return listeners;
    }

    public List<String> getChannelNames() {
        return channelNames;
    }

    public List<SensorName> getSensorNames() {
        return sensorNames;
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

    public int getPeriod() {
        return period;
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
        case "TEMP":
            if (SensorName.TEMP_AMBIENT.equals(listener.getSensorNames().get(index))) {
                typedValue = SensorTagDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 0));
            } else {
                typedValue = SensorTagDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 1));
            }
            break;
        case "ACCELERATION":
        case "GYROSCOPE":
        case "MAGNETIC":
            SensorName sensorName = listener.getSensorNames().get(index);
            if (sensorName.toString().endsWith("_X")) {
                typedValue = SensorTagDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 0));
            } else if (sensorName.toString().endsWith("_Y")) {
                typedValue = SensorTagDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 1));
            } else if (sensorName.toString().endsWith("_Z")) {
                typedValue = SensorTagDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 2));
            } else {
                typedValue = SensorTagDriver.getTypedValue(listener.getDataTypes().get(index), Array.get(value, 0));
            }
            break;
        case "HUMIDITY":
        case "LIGHT":
        case "PRESSURE":
        case "KEYS":
            typedValue = SensorTagDriver.getTypedValue(listener.getDataTypes().get(index), value);
            break;
        default:
        }
        return typedValue;
    }
}
