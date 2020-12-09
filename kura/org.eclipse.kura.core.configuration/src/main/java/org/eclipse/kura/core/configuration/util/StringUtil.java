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
package org.eclipse.kura.core.configuration.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.configuration.Password;

public class StringUtil {

    private static final char DELIMITER = ',';
    private static final char ESCAPE = '\\';

    public static String[] splitValues(String strValues) {
        if (strValues == null) {
            return null;
        }

        // The trick is to strip out unescaped whitespace characters before and
        // after the input string as well as before and after each
        // individual token within the input string without losing any escaped
        // whitespace characters. Whitespace between two non-whitespace
        // characters may or may not be escaped. Also, any character may be
        // escaped. The escape character is '\'. The delimiter is ','.
        List<String> values = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        // Loop over the characters within the input string and extract each
        // value token.
        for (int i = 0; i < strValues.length(); i++) {
            char c1 = strValues.charAt(i);
            switch (c1) {
            case DELIMITER:
                // When the delimiter is encountered, add the extracted
                // token to the result and prepare the buffer to receive the
                // next token.
                values.add(buffer.toString());
                buffer.delete(0, buffer.length());
                break;
            case ESCAPE:
                // When the escape is encountered, add the immediately
                // following character to the token, unless the end of the
                // input has been reached. Note this will result in loop
                // counter 'i' being incremented twice, once here and once
                // at the end of the loop.
                if (i + 1 < strValues.length()) {
                    buffer.append(strValues.charAt(++i));
                }
                // If the ESCAPE character occurs as the last character
                // of the string, ignore it.
                break;
            default:
                // For all other characters, add them to the current token
                // unless dealing with unescaped whitespace at the beginning
                // or end. We know the whitespace is unescaped because it
                // would have been handled in the ESCAPE case otherwise.
                if (Character.isWhitespace(c1)) {
                    // Ignore unescaped whitespace at the beginning of the
                    // token.
                    if (buffer.length() == 0) {
                        continue;
                    }
                    // If the whitespace is not at the beginning, look
                    // forward, starting with the next character, to see if
                    // it's in the middle or at the end. Unescaped
                    // whitespace in the middle is okay.
                    for (int j = i + 1; j < strValues.length(); j++) {
                        // Keep looping until the end of the string is
                        // reached or a non-whitespace character other than
                        // the escape is seen.
                        char c2 = strValues.charAt(j);
                        if (!Character.isWhitespace(c2)) {
                            // If the current character is not the DELIMITER, all whitespace
                            // characters are significant and should be added to the token.
                            // Otherwise, they're at the end and should be ignored. But watch
                            // out for an escape character at the end of the input. Ignore it
                            // and any previous insignificant whitespace if it exists.
                            if (c2 == ESCAPE && j + 1 >= strValues.length()) {
                                continue;
                            }
                            if (c2 != DELIMITER) {
                                buffer.append(strValues.substring(i, j));
                            }
                            // Let loop counter i catch up with the inner loop but keep in
                            // mind it will still be incremented at the end of the outer loop.
                            i = j - 1;
                            break;
                        }
                    }
                } else {
                    // For non-whitespace characters.
                    buffer.append(c1);
                }
            }
        }
        // Don't forget to add the last token.
        values.add(buffer.toString());
        return values.toArray(new String[] {});
    }

    public static String valueToString(Object value) {
        String result = null;
        if (value == null) {
            result = null;
        } else if (value instanceof String) {
            result = value.toString();
        } else if (value instanceof Long) {
            result = value.toString();
        } else if (value instanceof Double) {
            result = value.toString();
        } else if (value instanceof Float) {
            result = value.toString();
        } else if (value instanceof Integer) {
            result = value.toString();
        } else if (value instanceof Byte) {
            result = value.toString();
        } else if (value instanceof Character) {
            result = value.toString();
        } else if (value instanceof Boolean) {
            result = value.toString();
        } else if (value instanceof Short) {
            result = value.toString();
        } else if (value instanceof Password) {
            result = value.toString();
        } else if (value instanceof String[]) {
            StringBuilder sb = new StringBuilder();
            String[] ss = (String[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        } else if (value instanceof Long[]) {
            StringBuilder sb = new StringBuilder();
            Long[] ss = (Long[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        } else if (value instanceof Double[]) {
            StringBuilder sb = new StringBuilder();
            Double[] ss = (Double[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        } else if (value instanceof Float[]) {
            StringBuilder sb = new StringBuilder();
            Float[] ss = (Float[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        } else if (value instanceof Integer[]) {
            StringBuilder sb = new StringBuilder();
            Integer[] ss = (Integer[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        } else if (value instanceof Byte[]) {
            StringBuilder sb = new StringBuilder();
            Byte[] ss = (Byte[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        } else if (value instanceof Character[]) {
            StringBuilder sb = new StringBuilder();
            Character[] ss = (Character[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        } else if (value instanceof Boolean[]) {
            StringBuilder sb = new StringBuilder();
            Boolean[] ss = (Boolean[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        } else if (value instanceof Short[]) {
            StringBuilder sb = new StringBuilder();
            Short[] ss = (Short[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        } else if (value instanceof Password[]) {
            StringBuilder sb = new StringBuilder();
            String[] ss = (String[]) value;
            for (int i = 0; i < ss.length; i++) {
                if (ss[i] != null) {
                    sb.append(escapeString(ss[i].toString()));
                    if (i != ss.length - 1) {
                        sb.append(",");
                    }
                }
            }
            result = sb.toString();
        }
        return result;
    }

    public static String escapeString(String s) {
        String escaped = s;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace(",", "\\,");
        escaped = escaped.replace(" ", "\\ ");
        return escaped;
    }

    public static String unescapeString(String s) {
        String value = s;

        // remove all space at the beginning of the string which are not escaped
        value = value.replaceAll("^((?<!\\\\) )*", "");

        // remove all space at the end of the string which are not escaped
        value = value.replaceAll("((?<!\\\\) )*$", "");

        // replace all escaped spaces with just space
        // The pattern covers for any even number of backslashes before the space char
        value = value.replaceAll("\\\\(\\\\\\\\)* ", " ");

        // replace all escaped comma with just comma
        // The pattern covers for any even number of backslashes before the comma char
        value = value.replaceAll("\\\\(\\\\\\\\)*,", ",");

        return value;
    }
}
