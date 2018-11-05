/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.driver.binary.adapter;

import java.math.BigInteger;
import java.util.function.Function;

import org.eclipse.kura.driver.binary.BinaryData;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.driver.binary.Endianness;

public class GainOffset<T extends Number> implements BinaryData<T> {

    private final BinaryData<T> wrapped;
    private final double gain;
    private final double off;

    private Function<java.lang.Double, T> fromDouble;

    public GainOffset(final BinaryData<T> wrapped, final double gain, final double offset) {
        this.wrapped = wrapped;
        this.gain = gain;
        this.off = offset;
        this.fromDouble = fromDouble(wrapped.getValueType());
    }

    @Override
    public void write(Buffer buf, int offset, T value) {
        final double d = value.doubleValue() * gain + off;
        wrapped.write(buf, offset, this.fromDouble.apply(d));
    }

    @Override
    public T read(Buffer buf, int offset) {
        final T value = wrapped.read(buf, offset);
        return fromDouble.apply(value.doubleValue() * gain + off);
    }

    @Override
    public Class<T> getValueType() {
        return wrapped.getValueType();
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<java.lang.Double, T> fromDouble(final Class<T> targetType) {
        if (targetType == Byte.class) {
            return value -> (T) (Byte) value.byteValue();
        } else if (targetType == Short.class) {
            return value -> (T) (Short) value.shortValue();
        } else if (targetType == Integer.class) {
            return value -> (T) (Integer) value.intValue();
        } else if (targetType == Long.class) {
            return value -> (T) (Long) value.longValue();
        } else if (targetType == java.lang.Float.class) {
            return value -> (T) (Float) value.floatValue();
        } else if (targetType == java.lang.Double.class) {
            return value -> (T) (Double) value.doubleValue();
        } else if (targetType == BigInteger.class) {
            return value -> (T) BigInteger.valueOf(value.longValue());
        }

        throw new IllegalArgumentException("cannot convert from double to " + targetType.getSimpleName());
    }

    @Override
    public Endianness getEndianness() {
        return wrapped.getEndianness();
    }

    @Override
    public int getSize() {
        return wrapped.getSize();
    }

}
