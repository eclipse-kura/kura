/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.protocol.modbus;

/**
 * This class is used to enumerate the Java primitive data types.
 *
 * @author matt.demaree
 *
 */
public class ProtocolPrimitiveDataTypes {

    /**
     *
     */
    private ProtocolPrimitiveDataTypes() {
    };

    /**
     * defined for completeness, typically never used
     */
    public static final String TYPE_VOID = "VOID";
    /**
     * a primitive data type of boolean (1 bit)
     */
    public static final String TYPE_BOOLEAN = "BOOL";
    /**
     * a primitive data type of character (8 bits)
     */
    public static final String TYPE_CHAR = "CHAR";
    /**
     * a primitive data type of byte (8 bits)
     */
    public static final String TYPE_BYTE = "BYTE";
    /**
     * a primitive data type of short int (16 bits)
     */
    public static final String TYPE_SHORT = "SHORT";
    /**
     * a primitive data type of int, limited to the range 0 to 65535 inclusive
     */
    public static final String TYPE_UNSIGNED_SHORT = "USHORT";
    /**
     * a primitive data type of int (32 bits)
     */
    public static final String TYPE_INT = "INT";
    /**
     * a primitive data type of long, limited to the range 0 to 4294967295 inclusive
     */
    public static final String TYPE_UNSIGNED_INT = "UINT";
    /**
     * a primitive data type of long (64 bits)
     */
    public static final String TYPE_LONG = "LONG";
    /**
     * a primitive data type of 32 bit floating point
     */
    public static final String TYPE_FLOAT = "FLOAT";
    /**
     * a primitive data type of 64 bit floating point
     */
    public static final String TYPE_DOUBLE = "DOUBLE";
    /**
     * something other than one of the primitive data types
     */
    public static final String TYPE_CLASS = "CLASS";
}
