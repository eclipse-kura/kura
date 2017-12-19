/**
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.driver.opcua.localization.OpcUaMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

public final class DataTypeMapper {

    private static final OpcUaMessages message = LocalizationAdapter.adapt(OpcUaMessages.class);

    private DataTypeMapper() {
    }

    private static Variant mapByteArray(byte[] javaValue, VariableType opcuaType) {
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
        throw new IllegalArgumentException(
                message.errorValueTypeConversion() + javaValue.getClass() + " " + opcuaType.name());
    }

    public static Variant map(Object javaValue, VariableType opcuaType) {

        if (opcuaType == VariableType.DEFINED_BY_JAVA_TYPE
                || (javaValue instanceof Boolean && opcuaType == VariableType.BOOLEAN)) {
            return new Variant(javaValue);
        }
        if (javaValue instanceof byte[]) {
            return mapByteArray((byte[]) javaValue, opcuaType);
        }
        if (javaValue instanceof Number) {
            final Number numberValue = (Number) javaValue;
            if (opcuaType == VariableType.SBYTE) {
                return new Variant(numberValue.byteValue());
            } else if (opcuaType == VariableType.INT16) {
                return new Variant(numberValue.shortValue());
            } else if (opcuaType == VariableType.INT32) {
                return new Variant(numberValue.intValue());
            } else if (opcuaType == VariableType.INT64) {
                return new Variant(numberValue.longValue());
            } else if (opcuaType == VariableType.BYTE) {
                return new Variant(UByte.valueOf(numberValue.longValue()));
            } else if (opcuaType == VariableType.UINT16) {
                return new Variant(UShort.valueOf(numberValue.intValue()));
            } else if (opcuaType == VariableType.UINT32) {
                return new Variant(UInteger.valueOf(numberValue.longValue()));
            } else if (opcuaType == VariableType.UINT64) {
                return new Variant(ULong.valueOf(numberValue.longValue()));
            } else if (opcuaType == VariableType.FLOAT) {
                return new Variant(numberValue.floatValue());
            } else if (opcuaType == VariableType.DOUBLE) {
                return new Variant(numberValue.doubleValue());
            } else if (opcuaType == VariableType.STRING) {
                return new Variant(numberValue.toString());
            }
        }
        if (javaValue instanceof String) {
            final String stringValue = (String) javaValue;
            if (opcuaType == VariableType.SBYTE) {
                return new Variant(Byte.parseByte(stringValue));
            } else if (opcuaType == VariableType.INT16) {
                return new Variant(Short.parseShort(stringValue));
            } else if (opcuaType == VariableType.INT32) {
                return new Variant(Integer.parseInt(stringValue));
            } else if (opcuaType == VariableType.INT64) {
                return new Variant(Long.parseLong(stringValue));
            } else if (opcuaType == VariableType.BYTE) {
                return new Variant(UByte.valueOf(stringValue));
            } else if (opcuaType == VariableType.UINT16) {
                return new Variant(UShort.valueOf(stringValue));
            } else if (opcuaType == VariableType.UINT32) {
                return new Variant(UInteger.valueOf(stringValue));
            } else if (opcuaType == VariableType.UINT64) {
                return new Variant(ULong.valueOf(stringValue));
            } else if (opcuaType == VariableType.FLOAT) {
                return new Variant(Float.parseFloat(stringValue));
            } else if (opcuaType == VariableType.DOUBLE) {
                return new Variant(Double.parseDouble(stringValue));
            } else if (opcuaType == VariableType.STRING) {
                return new Variant(stringValue);
            }
        }
        throw new IllegalArgumentException(
                message.errorValueTypeConversion() + javaValue.getClass() + " " + opcuaType.name());
    }
}
