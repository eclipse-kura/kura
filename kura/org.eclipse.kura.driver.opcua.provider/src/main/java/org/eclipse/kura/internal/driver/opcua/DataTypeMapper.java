/**
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.internal.driver.opcua;

import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

public final class DataTypeMapper {

    private DataTypeMapper() {
    }

    private static Variant mapByteArray(final byte[] javaValue, final VariableType opcuaType) {
        if (opcuaType == VariableType.BYTE_STRING) {
            return new Variant(ByteString.of(javaValue));
        } else if (opcuaType == VariableType.BYTE_ARRAY) {
            final UByte[] array = new UByte[javaValue.length];
            for (int i = 0; i < javaValue.length; i++) {
                array[i] = UByte.valueOf(javaValue[i]);
            }
            return new Variant(array);
        } else if (opcuaType == VariableType.SBYTE_ARRAY) {
            final Byte[] array = new Byte[javaValue.length];
            for (int i = 0; i < javaValue.length; i++) {
                array[i] = javaValue[i];
            }
            return new Variant(array);
        }
        throw new IllegalArgumentException("Error while converting the retrieved value to the defined typed "
                + javaValue.getClass() + " " + opcuaType.name());
    }

    public static Variant map(final Object value, final VariableType targetType) {

        if (targetType == VariableType.DEFINED_BY_JAVA_TYPE
                || (value instanceof Boolean && targetType == VariableType.BOOLEAN)) {
            return new Variant(value);
        }
        if (value instanceof byte[]) {
            return mapByteArray((byte[]) value, targetType);
        }
        if (value instanceof Number) {
            final Number numberValue = (Number) value;
            if (targetType == VariableType.SBYTE) {
                return new Variant(numberValue.byteValue());
            } else if (targetType == VariableType.INT16) {
                return new Variant(numberValue.shortValue());
            } else if (targetType == VariableType.INT32) {
                return new Variant(numberValue.intValue());
            } else if (targetType == VariableType.INT64) {
                return new Variant(numberValue.longValue());
            } else if (targetType == VariableType.BYTE) {
                return new Variant(UByte.valueOf(numberValue.longValue()));
            } else if (targetType == VariableType.UINT16) {
                return new Variant(UShort.valueOf(numberValue.intValue()));
            } else if (targetType == VariableType.UINT32) {
                return new Variant(UInteger.valueOf(numberValue.longValue()));
            } else if (targetType == VariableType.UINT64) {
                return new Variant(ULong.valueOf(numberValue.longValue()));
            } else if (targetType == VariableType.FLOAT) {
                return new Variant(numberValue.floatValue());
            } else if (targetType == VariableType.DOUBLE) {
                return new Variant(numberValue.doubleValue());
            } else if (targetType == VariableType.STRING) {
                return new Variant(numberValue.toString());
            }
        }
        if (value instanceof String) {
            final String stringValue = (String) value;
            if (targetType == VariableType.SBYTE) {
                return new Variant(Byte.parseByte(stringValue));
            } else if (targetType == VariableType.INT16) {
                return new Variant(Short.parseShort(stringValue));
            } else if (targetType == VariableType.INT32) {
                return new Variant(Integer.parseInt(stringValue));
            } else if (targetType == VariableType.INT64) {
                return new Variant(Long.parseLong(stringValue));
            } else if (targetType == VariableType.BYTE) {
                return new Variant(UByte.valueOf(stringValue));
            } else if (targetType == VariableType.UINT16) {
                return new Variant(UShort.valueOf(stringValue));
            } else if (targetType == VariableType.UINT32) {
                return new Variant(UInteger.valueOf(stringValue));
            } else if (targetType == VariableType.UINT64) {
                return new Variant(ULong.valueOf(stringValue));
            } else if (targetType == VariableType.FLOAT) {
                return new Variant(Float.parseFloat(stringValue));
            } else if (targetType == VariableType.DOUBLE) {
                return new Variant(Double.parseDouble(stringValue));
            } else if (targetType == VariableType.STRING) {
                return new Variant(stringValue);
            }
        }
        throw new IllegalArgumentException("Error while converting the retrieved value to the defined typed "
                + value.getClass() + " " + targetType.name());
    }

    private static byte[] toByteArray(Object objectValue) {
        if (objectValue instanceof byte[]) {
            return (byte[]) objectValue;
        } else if (objectValue instanceof ByteString) {
            return ((ByteString) objectValue).bytesOrEmpty();
        } else if (objectValue instanceof Byte[]) {
            final Byte[] value = (Byte[]) objectValue;
            final byte[] result = new byte[value.length];
            for (int i = 0; i < value.length; i++) {
                result[i] = value[i];
            }
            return result;
        } else if (objectValue instanceof UByte[]) {
            final UByte[] value = (UByte[]) objectValue;
            final byte[] result = new byte[value.length];
            for (int i = 0; i < value.length; i++) {
                result[i] = (byte) (value[i].intValue() & 0xff);
            }
            return result;
        }
        throw new IllegalArgumentException();
    }

    public static TypedValue<?> map(final Object value, final DataType targetType) {
        switch (targetType) {
        case LONG:
            return TypedValues.newLongValue(Long.parseLong(value.toString()));
        case FLOAT:
            return TypedValues.newFloatValue(Float.parseFloat(value.toString()));
        case DOUBLE:
            return TypedValues.newDoubleValue(Double.parseDouble(value.toString()));
        case INTEGER:
            return TypedValues.newIntegerValue(Integer.parseInt(value.toString()));
        case BOOLEAN:
            return TypedValues.newBooleanValue(Boolean.parseBoolean(value.toString()));
        case STRING:
            if (value instanceof LocalizedText) {
                final LocalizedText text = (LocalizedText) value;
                return TypedValues.newStringValue(text.getText());
            } else {
                return TypedValues.newStringValue(value.toString());
            }
        case BYTE_ARRAY:
            return TypedValues.newByteArrayValue(toByteArray(value));
        default:
            throw new IllegalArgumentException();
        }
    }
}
