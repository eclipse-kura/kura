/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.util.osgi;

import org.eclipse.kura.util.base.StringUtil;
import org.osgi.framework.Constants;

public final class FilterUtil {

    private FilterUtil() {
    }

    public static String equal(final String property, final String value) {
        if (property == null || property.isEmpty()) {
            return "";
        }

        return "(" + quote(property) + "=" + quote(value) + ")";
    }

    public static String objectClass(final Class<?> clazz) {
        return equal(Constants.OBJECTCLASS, clazz.getName());
    }

    public static String expressions(final String operation, final String... expressions) {

        if (StringUtil.isNullOrEmpty(operation)) {
            throw new IllegalArgumentException("'operation' must not be null or empty");
        }

        if (expressions == null || expressions.length == 0) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();

        int remaining = 0;

        for (final String expression : expressions) {

            if (expression == null || expression.isEmpty()) {
                continue;
            }

            builder.append(expression);
            remaining++;
        }

        if (remaining > 1) {
            return "(" + operation + builder.toString() + ")";
        } else {
            return builder.toString();
        }
    }

    public static String and(final String... expressions) {
        return expressions("&", expressions);
    }

    public static String or(final String... expressions) {
        return expressions("|", expressions);
    }

    public static String not(final String expression) {
        if (expression == null || expression.isEmpty()) {
            return "";
        }

        return "(!" + expression + ")";
    }

    public static String simpleFilter(final Class<?> objectClass, final String property, final String value) {
        return and(objectClass(objectClass), equal(property, value));
    }

    /**
     * Quote a value for being used in LDAP style OSGi filter.
     * 
     * @param value
     *            The value to quote, may be {@code null}.
     * @return The quoted value, is {@code null} if the input was {@code null}.
     */
    public static String quote(final String value) {

        if (value == null) {
            return null;
        }

        final int len = value.length();
        final StringBuilder sb = new StringBuilder(value.length()); // ideally we have the same length

        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);

            switch (c) {
            case '*': //$FALL-THROUGH$
            case '(': //$FALL-THROUGH$
            case ')': //$FALL-THROUGH$
                sb.append('\\');
                break;
            default:
                break;
            }

            sb.append(c);
        }
        return sb.toString();
    }

}
