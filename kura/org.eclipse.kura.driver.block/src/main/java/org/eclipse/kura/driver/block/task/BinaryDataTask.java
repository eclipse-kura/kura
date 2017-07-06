/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.driver.block.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.binary.BinaryData;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryDataTask<T> extends ChannelBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(BinaryDataTask.class);

    private BinaryData<T> dataType;

    private Function<T, TypedValue<?>> toTypedValue;
    private Function<TypedValue<?>, T> fromTypedValue;

    @SuppressWarnings("unchecked")
    public BinaryDataTask(ChannelRecord record, int offset, BinaryData<T> dataType, Mode mode) {
        this(record, offset, dataType, value -> TypedValues.newTypedValue(value),
                typedValue -> (T) typedValue.getValue(), mode);
    }

    public BinaryDataTask(ChannelRecord record, int offset, BinaryData<T> binaryDataType, DataType dataType,
            Mode mode) {
        this(record, offset, binaryDataType, createToTypedValueAdapter(binaryDataType.getValueType(), dataType),
                createFromTypedValueAdapter(binaryDataType.getValueType(), dataType), mode);
    }

    public BinaryDataTask(ChannelRecord record, int offset, BinaryData<T> dataType,
            Function<T, TypedValue<?>> toTypedValue, Function<TypedValue<?>, T> fromTypedValue, Mode mode) {
        super(record, offset, offset + dataType.getSize(), mode);
        this.dataType = dataType;
        this.toTypedValue = toTypedValue;
        this.fromTypedValue = fromTypedValue;
    }

    @Override
    public void run() throws IOException {
        final ToplevelBlockTask parent = getParent();
        Buffer buffer = parent.getBuffer();

        if (getMode() == Mode.READ) {
            logger.debug("Read {}: offset: {}", this.dataType.getClass().getSimpleName(), getStart());

            final T result = this.dataType.read(buffer, getStart() - parent.getStart());

            this.record.setValue(this.toTypedValue.apply(result));
            onSuccess();
        } else {
            logger.debug("Write {}: offset: {}", this.dataType.getClass().getSimpleName(), getStart());

            T value = this.fromTypedValue.apply(this.record.getValue());

            this.dataType.write(buffer, getStart() - parent.getStart(), value);
        }
    }

    private static <T> Function<T, TypedValue<?>> createToTypedValueAdapter(Class<T> sourceType, DataType targetType) {
        if (targetType == DataType.STRING) {
            if (sourceType == byte[].class) {
                return value -> new StringValue(Arrays.toString((byte[]) value));
            } else {
                return value -> new StringValue(value.toString());
            }
        }
        if (targetType == DataType.BOOLEAN) {
            if (sourceType == Boolean.class) {
                return value -> new BooleanValue((Boolean) value);
            } else if (sourceType == String.class) {
                return value -> new BooleanValue(Boolean.parseBoolean((String) value));
            }
        }
        if (Number.class.isAssignableFrom(sourceType)) {
            if (targetType == DataType.INTEGER) {
                return value -> new IntegerValue(((Number) value).intValue());
            } else if (targetType == DataType.LONG) {
                return value -> new LongValue(((Number) value).longValue());
            } else if (targetType == DataType.FLOAT) {
                return value -> new FloatValue(((Number) value).floatValue());
            } else if (targetType == DataType.DOUBLE) {
                return value -> new DoubleValue(((Number) value).doubleValue());
            }
        }
        if (sourceType == String.class) {
            if (targetType == DataType.INTEGER) {
                return value -> new IntegerValue(Integer.parseInt((String) value));
            } else if (targetType == DataType.LONG) {
                return value -> new LongValue(Long.parseLong((String) value));
            } else if (targetType == DataType.FLOAT) {
                return value -> new FloatValue(Float.parseFloat((String) value));
            } else if (targetType == DataType.DOUBLE) {
                return value -> new DoubleValue(Double.parseDouble((String) value));
            }
        }
        throw new IllegalArgumentException("Cannot convert from native type " + sourceType.getSimpleName()
                + " to Kura data type " + targetType.name());
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<TypedValue<?>, T> createFromTypedValueAdapter(Class<T> targetType,
            DataType sourceType) {
        if (targetType == String.class) {
            if (sourceType == DataType.BYTE_ARRAY) {
                return value -> (T) Arrays.toString((byte[]) value.getValue());
            } else {
                return value -> (T) value.toString();
            }
        }
        if (targetType == Boolean.class) {
            if (sourceType == DataType.BOOLEAN) {
                return value -> (T) value.getValue();
            } else if (sourceType == DataType.STRING) {
                return value -> (T) (Boolean) Boolean.parseBoolean((String) value.getValue());
            }
        }
        if (sourceType == DataType.STRING) {
            if (targetType == Integer.class) {
                return value -> (T) (Integer) Integer.parseInt((String) value.getValue());
            } else if (targetType == Long.class) {
                return value -> (T) (Long) Long.parseLong((String) value.getValue());
            } else if (targetType == Float.class) {
                return value -> (T) (Float) Float.parseFloat((String) value.getValue());
            } else if (targetType == Double.class) {
                return value -> (T) (Double) Double.parseDouble((String) value.getValue());
            }
        } else {
            if (targetType == Integer.class) {
                return value -> (T) (Integer) ((Number) value.getValue()).intValue();
            } else if (targetType == Long.class) {
                return value -> (T) (Long) ((Number) value.getValue()).longValue();
            } else if (targetType == Float.class) {
                return value -> (T) (Float) ((Number) value.getValue()).floatValue();
            } else if (targetType == Double.class) {
                return value -> (T) (Double) ((Number) value.getValue()).doubleValue();
            }
        }
        throw new IllegalArgumentException("Cannot convert from Kura data type " + sourceType.name()
                + " to native type " + targetType.getSimpleName());
    }
}
