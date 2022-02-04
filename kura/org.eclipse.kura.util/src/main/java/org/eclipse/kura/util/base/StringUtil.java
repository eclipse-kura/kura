/*******************************************************************************
 * Copyright (c) 2016, 2022 Eurotech and/or its affiliates and others
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
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.eclipse.kura.annotation.Nullable;

/**
 * The Class StringUtil contains all necessary static factory methods for
 * manipulating String instances
 */
public final class StringUtil {

    /** Constructor */
    private StringUtil() {
        // Static Factory Methods container. No need to instantiate.
    }

    /**
     * Returns {@code true} if the given string is null or is the empty string.
     *
     * @param string
     *            a string reference to check
     * @return {@code true} if the string is null or is the empty string
     */
    public static boolean isNullOrEmpty(@Nullable final String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param tokens
     *            an array objects to be joined. Strings will be formed from the
     *            objects by calling object.toString().
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public static String join(final CharSequence delimiter, final Iterable<?> tokens) {
        requireNonNull(delimiter, "Delimiter cannot be null.");
        requireNonNull(tokens, "Iterable elements cannot be null.");

        final StringBuilder sb = new StringBuilder();
        final Iterator<?> it = tokens.iterator();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(delimiter);
                sb.append(it.next());
            }
        }
        return sb.toString();
    }

    /**
     * Unescape an UTF-8 string.
     * 
     * @param string
     *            an UTF-8 escaped string.
     * @return string in UTF-8 with unescaped characters.
     */

    public static String unescapeUTF8String(final String string) {

        requireNonNull(string, "String cannot be null");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = 0;
        while (i < string.length()) {
            if (string.charAt(i) == '\\') {
                i += 2;
                String hexString = string.substring(i, i + 2);
                baos.write(Integer.parseInt(hexString, 16));
                i += 2;
            } else {
                baos.write(string.charAt(i));
                i += 1;
            }
        }

        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Covert a string in hexadecimal format.
     * 
     * @param string
     *            string to be converted in hex format.
     * @return string in hex format.
     */

    public static String toHex(String string) {
        StringBuilder sb = new StringBuilder();
        for (byte b : string.getBytes(StandardCharsets.UTF_8)) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

}
