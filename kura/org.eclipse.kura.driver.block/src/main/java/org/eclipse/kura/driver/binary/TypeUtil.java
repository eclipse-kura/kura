package org.eclipse.kura.driver.binary;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Function;

import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;

public final class TypeUtil {

    private TypeUtil() {
    }

    public static <T> Function<T, TypedValue<?>> toTypedValue(final Class<T> sourceType, final DataType targetType) {
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
            } else if (sourceType.isAssignableFrom(Number.class)) {
                return value -> new BooleanValue(((Number) value).doubleValue() != 0);
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
                return value -> new FloatValue(java.lang.Float.parseFloat((String) value));
            } else if (targetType == DataType.DOUBLE) {
                return value -> new DoubleValue(java.lang.Double.parseDouble((String) value));
            }
        }
        throw new IllegalArgumentException("Cannot convert from native type " + sourceType.getSimpleName()
                + " to Kura data type " + targetType.name());
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<TypedValue<?>, T> fromTypedValue(final Class<T> targetType, final DataType sourceType) {
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
            } else if (sourceType != DataType.BYTE_ARRAY) {
                return value -> (T) (Boolean) (((Number) value.getValue()).doubleValue() != 0);
            }
        }
        if (sourceType == DataType.STRING) {
            if (targetType == Integer.class) {
                return value -> (T) (Integer) Integer.parseInt((String) value.getValue());
            } else if (targetType == Long.class) {
                return value -> (T) (Long) Long.parseLong((String) value.getValue());
            } else if (targetType == java.lang.Float.class) {
                return value -> (T) (java.lang.Float) java.lang.Float.parseFloat((String) value.getValue());
            } else if (targetType == java.lang.Double.class) {
                return value -> (T) (java.lang.Double) java.lang.Double.parseDouble((String) value.getValue());
            }
        } else {
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
        }
        throw new IllegalArgumentException("Cannot convert from Kura data type " + sourceType.name()
                + " to native type " + targetType.getSimpleName());
    }
}
