/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.protocol.modbus;

/**
 * This static class defines the possible byte organization in
 * the data stream sent and received. The Modbus protocol only
 * defines one analog data type (16 bit) and only one format
 * for that data in the stream (big endian). Manufactures of
 * various "Modbus compatible" field devices have extend and
 * modified the size and arrangementof the data in the stream. Data units may now be 16 or
 * 32 bit sized and may be in a variety of byte arrangements.
 * <p>
 * In the definitions below, the character arrangement making up the definition
 * field value actually represent the ordering of the bytes in the
 * stream. For a hexadecimal number 0x01020304, the '1' is the 0x01
 * byte, '2' is the 0x02 byte, '3' is the 0x03 byte and '4' is the 0x04.
 * <p>
 * The Field values, not the field names should be used in configuration files.
 *
 * @author matt.demaree
 *
 */
public class ModbusDataOrder {

    /**
     *
     */
    private ModbusDataOrder() {
    }

    /**
     * booleans do not have a specified data order
     */
    public static final String MODBUS_BOOLEAN_ORDER = "none";
    /**
     * this is the Modbus default (note only 16 bit or 2 byte data)
     */
    public static final String MODBUS_WORD_ORDER_BIG_ENDIAN = "12";
    public static final String MODBUS_WORD_ORDER_LITTLE_ENDIAN = "21";
    /**
     * this is the most common 32 bit arrangement used by many devices
     */
    public static final String MODBUS_LONG_ORDER_BIG_BIG_ENDIAN = "1234";
    public static final String MODBUS_LONG_ORDER_BIG_LITTLE_ENDIAN = "2143";
    public static final String MODBUS_LONG_ORDER_LITTLE_BIG_ENDIAN = "3412";
    public static final String MODBUS_LONG_ORDER_LITTLE_LITTLE_ENDIAN = "4321";
}
