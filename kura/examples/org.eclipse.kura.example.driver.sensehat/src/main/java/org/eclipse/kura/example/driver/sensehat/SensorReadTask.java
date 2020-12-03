/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.example.driver.sensehat;

import java.util.function.Function;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class SensorReadTask extends ReadTask {

    public SensorReadTask(final ChannelRecord record, final SensorReader reader) {
        super(record, adapt(record.getValueType(), reader));
    }

    private static Function<SenseHatInterface, TypedValue<?>> adapt(final DataType targetType, SensorReader reader) {
        if (targetType == DataType.STRING) {
            return sensehat -> TypedValues.newStringValue(Float.toString(reader.read(sensehat)));
        } else if (targetType == DataType.BOOLEAN) {
            return sensehat -> TypedValues.newBooleanValue(reader.read(sensehat) != 0);
        } else if (targetType == DataType.INTEGER) {
            return sensehat -> TypedValues.newIntegerValue((int) reader.read(sensehat));
        } else if (targetType == DataType.LONG) {
            return sensehat -> TypedValues.newLongValue((long) reader.read(sensehat));
        } else if (targetType == DataType.FLOAT) {
            return sensehat -> TypedValues.newFloatValue((float) reader.read(sensehat));
        } else if (targetType == DataType.DOUBLE) {
            return sensehat -> TypedValues.newDoubleValue((double) reader.read(sensehat));
        }

        throw new IllegalArgumentException(
                "Cannot convert from native type float to Kura data type " + targetType.name());
    }

    public interface SensorReader {

        public float read(SenseHatInterface sensehat);
    }
}