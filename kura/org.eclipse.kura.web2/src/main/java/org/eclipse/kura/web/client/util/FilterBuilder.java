/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.util;

public class FilterBuilder {

    public static String of(String s) {
        StringBuilder builder = new StringBuilder();
        builder.append('(').append(s).append(')');
        return builder.toString();
    }

    private static StringBuilder chain(StringBuilder builder, String... filters) {
        for (String f : filters) {
            builder.append('(').append(f).append(')');
        }
        return builder;
    }

    public static String and(String... filters) {
        StringBuilder builder = new StringBuilder();
        builder.append('&');
        return chain(builder, filters).toString();
    }

    public static String or(String... filters) {
        StringBuilder builder = new StringBuilder();
        builder.append('|');
        return chain(builder, filters).toString();
    }

    public static String not(String filter) {
        StringBuilder builder = new StringBuilder();
        builder.append('!');
        return chain(builder, filter).toString();
    }

}