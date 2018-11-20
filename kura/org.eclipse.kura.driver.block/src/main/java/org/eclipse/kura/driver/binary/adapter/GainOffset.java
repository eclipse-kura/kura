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

public class GainOffset implements BinaryData<Double> {

    private final double gain;
    private final double off;

    private final Adapter<? extends Number> adapter;

    public GainOffset(final BinaryData<? extends Number> wrapped, final double gain, final double offset) {
        this.gain = gain;
        this.off = offset;
        this.adapter = new Adapter<>(wrapped);
    }

    @Override
    public void write(final Buffer buf, final int offset, final Double value) {
        final double d = value * gain + off;
        adapter.write(buf, offset, d);
    }

    @Override
    public Double read(Buffer buf, int offset) {
        return adapter.read(buf, offset) * gain + off;
    }

    @Override
    public Class<Double> getValueType() {
        return Double.class;
    }

    @Override
    public Endianness getEndianness() {
        return adapter.getWrapped().getEndianness();
    }

    @Override
    public int getSize() {
        return adapter.getWrapped().getSize();
    }

    private static class Adapter<T extends Number> {

        private final BinaryData<T> wrapped;
        private final Function<java.lang.Double, T> fromDouble;

        public Adapter(final BinaryData<T> wrapped) {
            this.wrapped = wrapped;
            this.fromDouble = fromDouble(wrapped.getValueType());
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

        public void write(final Buffer buf, final int offset, final Double value) {
            wrapped.write(buf, offset, fromDouble.apply(value));
        }

        public Double read(final Buffer buf, final int offset) {
            return wrapped.read(buf, offset).doubleValue();
        }

        public BinaryData<T> getWrapped() {
            return wrapped;
        }
    }
}
