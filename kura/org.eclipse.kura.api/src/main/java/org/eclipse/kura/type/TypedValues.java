/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.type;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Nullable;

/**
 * The Class TypedValues is an utility class to quickly create different
 * {@link TypedValue}
 * 
 * @noextend This class is not intended to be extended by clients.
 */
public final class TypedValues {

    private TypedValues() {
        // Static Factory Methods container. No need to instantiate.
    }

    /**
     * Creates new boolean value.
     *
     * @param value
     *            the primitive boolean value
     * @return the boolean value represented as {@link TypedValue}
     */
    public static BooleanValue newBooleanValue(final boolean value) {
        return new BooleanValue(value);
    }

    /**
     * Creates new byte array value.
     *
     * @param value
     *            the primitive byte array value
     * @return the byte array value represented as {@link TypedValue}
     * @throws KuraRuntimeException
     *             if the argument is null
     */
    public static ByteArrayValue newByteArrayValue(final byte[] value) {
        return new ByteArrayValue(value);
    }

    /**
     * Creates new byte value.
     *
     * @param value
     *            the primitive byte value
     * @return the byte value represented as {@link TypedValue}
     */
    public static ByteValue newByteValue(final byte value) {
        return new ByteValue(value);
    }

    /**
     * Creates new double value.
     *
     * @param value
     *            the primitive double value
     * @return the double value represented as {@link TypedValue}
     */
    public static DoubleValue newDoubleValue(final double value) {
        return new DoubleValue(value);
    }

    /**
     * Creates new integer value.
     *
     * @param value
     *            the primitive integer value
     * @return the integer value represented as {@link TypedValue}
     */
    public static IntegerValue newIntegerValue(final int value) {
        return new IntegerValue(value);
    }

    /**
     * Creates new long value.
     *
     * @param value
     *            the primitive long value
     * @return the long value represented as {@link TypedValue}
     */
    public static LongValue newLongValue(final long value) {
        return new LongValue(value);
    }

    /**
     * Creates new short value.
     *
     * @param value
     *            the primitive short value
     * @return the short value represented as {@link TypedValue}
     */
    public static ShortValue newShortValue(final short value) {
        return new ShortValue(value);
    }

    /**
     * Creates new string value.
     *
     * @param value
     *            the string value to be represented as {@link TypedValue}
     * @return the string value represented as {@link TypedValue}
     */
    public static StringValue newStringValue(@Nullable final String value) {
        return new StringValue(value);
    }

    /**
     * Creates new TypedValue inferring the type from the argument.
     * 
     * @param value
     *            an object that needs to be represented as {@link TypedValue}
     * @return a {@link TypedValue} that represents the conversion of {@code value}
     * @throws IllegalArgumentException
     *             if {@code value} cannot be represented as {@link TypedValue}
     */
    public static TypedValue<?> newTypedValue(final Object value) {
        if (value instanceof Boolean) {
            return newBooleanValue((Boolean) value);
        } else if (value instanceof byte[]) {
            return newByteArrayValue((byte[]) value);
        } else if (value instanceof Byte) {
            return newByteValue((Byte) value);
        } else if (value instanceof Double) {
            return newDoubleValue((Double) value);
        } else if (value instanceof Integer) {
            return newIntegerValue((Integer) value);
        } else if (value instanceof Long) {
            return newLongValue((Long) value);
        } else if (value instanceof Short) {
            return newShortValue((Short) value);
        } else if (value instanceof String) {
            return newStringValue((String) value);
        }

        throw new IllegalArgumentException("Cannot convert to TypedValue");
    }
}
