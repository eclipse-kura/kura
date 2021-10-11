/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.util.base;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * The Class TypeUtil contains all necessary static factory methods for
 * manipulating java type instances
 */
public final class TypeUtil {

    /** Constructor */
    private TypeUtil() {
        // Static Factory Methods container. No need to instantiate.
    }

    /**
     * Returns a byte array representation of the provided integer value.
     *
     * @param value
     *            The provided integer value
     * @return the byte array instance
     */
    public static byte[] intToBytes(final int value) {
        final byte[] result = new byte[4];
        result[0] = (byte) (value >> 24);
        result[1] = (byte) (value >> 16);
        result[2] = (byte) (value >> 8);
        result[3] = (byte) value;
        return result;
    }

    /**
     * Converts to the byte array from the provided object instance
     *
     * @param value
     * @return the byte array
     * @throws IOException
     *             if the access to byte stream fails
     * @throws NullPointerException
     *             if the argument is null
     */
    public static byte[] objectToByteArray(final Object value) throws IOException {
        requireNonNull(value, "Value cannot be null.");
        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        final ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(value);
        return b.toByteArray();
    }

    /**
     * 
     * Convert an hex string to a byte array. Simple copy-paste of the method in standard JRE 8
     * {@link javax.xml.bind.DatatypeConverter#parseHexBinary(String)}
     * 
     * @param string
     * @return the byte array
     * @throws IllegalArgumentException
     *             if the hex string is invalid
     */

    public static byte[] parseHexBinary(String string) {
        final int len = string.length();

        // "111" is not a valid hex encoding.
        if (len % 2 != 0) {
            throw new IllegalArgumentException("hexBinary needs to be even-length: " + string);
        }

        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int h = hexToBin(string.charAt(i));
            int l = hexToBin(string.charAt(i + 1));
            if (h == -1 || l == -1) {
                throw new IllegalArgumentException("contains illegal character for hexBinary: " + string);
            }

            out[i / 2] = (byte) (h * 16 + l);
        }

        return out;
    }

    private static int hexToBin(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        }
        return -1;
    }

}
