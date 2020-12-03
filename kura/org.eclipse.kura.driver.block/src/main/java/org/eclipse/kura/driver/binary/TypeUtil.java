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
package org.eclipse.kura.driver.binary;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Function;

import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;

public final class TypeUtil {

    private static final String TO_NATIVE_TYPE_MESSAGE = " to native type ";
    private static final String CANNOT_CONVERT_FROM_KURA_DATA_TYPE_MESSAGE = "Cannot convert from Kura data type ";
    private static final String TO_KURA_DATA_TYPE_MESSAGE = " to Kura data type ";
    private static final String CANNOT_CONVERT_FROM_NATIVE_TYPE_MESSAGE = "Cannot convert from native type ";

    private TypeUtil() {
    }

    public static <T> Function<T, TypedValue<?>> toTypedValue(final Class<T> sourceType, final DataType targetType) {
        if (targetType == DataType.STRING) {
            return toStringTypedValue(sourceType);
        } else if (targetType == DataType.BOOLEAN) {
            return toBooleanTypedValue(sourceType, targetType);
        } else if (Number.class.isAssignableFrom(sourceType)) {
            return toNumericalTypedValue(sourceType, targetType);
        } else if (targetType == DataType.BYTE_ARRAY) {
            return toByteArrayTypedValue(sourceType, targetType);
        } else if (sourceType == String.class) {
            return fromStringValue(targetType);
        }

        throw new IllegalArgumentException(CANNOT_CONVERT_FROM_NATIVE_TYPE_MESSAGE + sourceType.getSimpleName()
                + TO_KURA_DATA_TYPE_MESSAGE + targetType.name());
    }

    private static <T> Function<T, TypedValue<?>> fromStringValue(final DataType targetType) {
        if (targetType == DataType.INTEGER) {
            return value -> new IntegerValue(Integer.parseInt((String) value));
        } else if (targetType == DataType.LONG) {
            return value -> new LongValue(Long.parseLong((String) value));
        } else if (targetType == DataType.FLOAT) {
            return value -> new FloatValue(java.lang.Float.parseFloat((String) value));
        } else if (targetType == DataType.DOUBLE) {
            return value -> new DoubleValue(java.lang.Double.parseDouble((String) value));
        }

        throw new IllegalArgumentException(CANNOT_CONVERT_FROM_NATIVE_TYPE_MESSAGE + String.class.getSimpleName()
                + TO_KURA_DATA_TYPE_MESSAGE + targetType.name());
    }

    private static <T> Function<T, TypedValue<?>> toNumericalTypedValue(final Class<T> sourceType,
            final DataType targetType) {
        if (targetType == DataType.INTEGER) {
            return value -> new IntegerValue(((Number) value).intValue());
        } else if (targetType == DataType.LONG) {
            return value -> new LongValue(((Number) value).longValue());
        } else if (targetType == DataType.FLOAT) {
            return value -> new FloatValue(((Number) value).floatValue());
        } else if (targetType == DataType.DOUBLE) {
            return value -> new DoubleValue(((Number) value).doubleValue());
        }
        throw new IllegalArgumentException(CANNOT_CONVERT_FROM_NATIVE_TYPE_MESSAGE + sourceType.getSimpleName()
                + TO_KURA_DATA_TYPE_MESSAGE + targetType.name());
    }

    private static <T> Function<T, TypedValue<?>> toBooleanTypedValue(final Class<T> sourceType,
            final DataType targetType) {
        if (sourceType == Boolean.class) {
            return value -> new BooleanValue((Boolean) value);
        } else if (sourceType == String.class) {
            return value -> new BooleanValue(Boolean.parseBoolean((String) value));
        } else if (sourceType.isAssignableFrom(Number.class)) {
            return value -> new BooleanValue(((Number) value).doubleValue() != 0);
        }
        throw new IllegalArgumentException(CANNOT_CONVERT_FROM_NATIVE_TYPE_MESSAGE + sourceType.getSimpleName()
                + TO_KURA_DATA_TYPE_MESSAGE + targetType.name());
    }

    private static <T> Function<T, TypedValue<?>> toStringTypedValue(final Class<T> sourceType) {
        if (sourceType == byte[].class) {
            return value -> new StringValue(Arrays.toString((byte[]) value));
        } else {
            return value -> new StringValue(value.toString());
        }
    }

    private static <T> Function<T, TypedValue<?>> toByteArrayTypedValue(final Class<T> sourceType,
            final DataType targetType) {
        if (sourceType == byte[].class) {
            return value -> new ByteArrayValue((byte[]) value);
        }
        throw new IllegalArgumentException(CANNOT_CONVERT_FROM_NATIVE_TYPE_MESSAGE + sourceType.getSimpleName()
                + TO_KURA_DATA_TYPE_MESSAGE + targetType.name());
    }

    public static <T> Function<TypedValue<?>, T> fromTypedValue(final Class<T> targetType, final DataType sourceType) {
        if (targetType == String.class) {
            return toStringValue(sourceType);
        } else if (targetType == Boolean.class) {
            return toBooleanValue(targetType, sourceType);
        } else if (targetType == byte[].class) {
            return toByteArrayValue(targetType, sourceType);
        } else if (sourceType == DataType.STRING) {
            return fromStringTypedValue(targetType);
        } else {
            return fromNumericTypedValue(targetType, sourceType);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<TypedValue<?>, T> fromNumericTypedValue(final Class<T> targetType,
            final DataType sourceType) {
        if (targetType == Integer.class) {
            return value -> (T) (Integer) ((Number) value.getValue()).intValue();
        } else if (targetType == Long.class) {
            return value -> (T) (Long) ((Number) value.getValue()).longValue();
        } else if (targetType == java.lang.Float.class) {
            return value -> (T) (java.lang.Float) ((Number) value.getValue()).floatValue();
        } else if (targetType == java.lang.Double.class) {
            return value -> (T) (java.lang.Double) ((Number) value.getValue()).doubleValue();
        } else if (targetType == BigInteger.class) {
            return value -> (T) BigInteger.valueOf(((Number) value.getValue()).longValue());
        }

        throw new IllegalArgumentException(CANNOT_CONVERT_FROM_KURA_DATA_TYPE_MESSAGE + sourceType.name()
                + TO_NATIVE_TYPE_MESSAGE + targetType.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<TypedValue<?>, T> fromStringTypedValue(final Class<T> targetType) {
        if (targetType == Integer.class) {
            return value -> (T) (Integer) Integer.parseInt((String) value.getValue());
        } else if (targetType == Long.class) {
            return value -> (T) (Long) Long.parseLong((String) value.getValue());
        } else if (targetType == java.lang.Float.class) {
            return value -> (T) (java.lang.Float) java.lang.Float.parseFloat((String) value.getValue());
        } else if (targetType == java.lang.Double.class) {
            return value -> (T) (java.lang.Double) java.lang.Double.parseDouble((String) value.getValue());
        }

        throw new IllegalArgumentException(CANNOT_CONVERT_FROM_KURA_DATA_TYPE_MESSAGE + DataType.STRING.name()
                + TO_NATIVE_TYPE_MESSAGE + targetType.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<TypedValue<?>, T> toBooleanValue(final Class<T> targetType, final DataType sourceType) {
        if (sourceType == DataType.BOOLEAN) {
            return value -> (T) value.getValue();
        } else if (sourceType == DataType.STRING) {
            return value -> (T) (Boolean) Boolean.parseBoolean((String) value.getValue());
        } else if (sourceType != DataType.BYTE_ARRAY) {
            return value -> (T) (Boolean) (((Number) value.getValue()).doubleValue() != 0);
        }
        throw new IllegalArgumentException(CANNOT_CONVERT_FROM_KURA_DATA_TYPE_MESSAGE + sourceType.name()
                + TO_NATIVE_TYPE_MESSAGE + targetType.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<TypedValue<?>, T> toStringValue(final DataType sourceType) {
        if (sourceType == DataType.BYTE_ARRAY) {
            return value -> (T) Arrays.toString((byte[]) value.getValue());
        } else {
            return value -> (T) value.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<TypedValue<?>, T> toByteArrayValue(final Class<T> targetType,
            final DataType sourceType) {
        if (sourceType == DataType.BYTE_ARRAY) {
            return value -> (T) (byte[]) value.getValue();
        }
        throw new IllegalArgumentException(CANNOT_CONVERT_FROM_KURA_DATA_TYPE_MESSAGE + sourceType.name()
                + TO_NATIVE_TYPE_MESSAGE + targetType.getSimpleName());
    }
}
