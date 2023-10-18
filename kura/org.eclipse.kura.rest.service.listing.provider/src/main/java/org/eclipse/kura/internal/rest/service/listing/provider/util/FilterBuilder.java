/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.service.listing.provider.util;

import java.util.function.Consumer;

public class FilterBuilder {

    private final StringBuilder stringBuilder = new StringBuilder();

    public FilterBuilder property(final String name, final String value) {
        final String escapedValue = value != null ? escapeValue(value) : "*";

        stringBuilder.append("(").append(name).append("=").append(escapedValue).append(")");

        return this;
    }

    public FilterBuilder and(final Consumer<FilterBuilder> consumer) {
        return op("&", consumer);
    }

    public FilterBuilder or(final Consumer<FilterBuilder> consumer) {
        return op("|", consumer);
    }

    public FilterBuilder not(final Consumer<FilterBuilder> consumer) {
        return op("!", consumer);
    }

    private FilterBuilder op(final String operator, final Consumer<FilterBuilder> consumer) {
        stringBuilder.append("(");
        stringBuilder.append(operator);

        consumer.accept(this);

        stringBuilder.append(")");

        return this;
    }

    private String escapeValue(final String value) {
        return value.replace("(", "\\(").replace(")", "\\)").replace("*", "\\*");
    }

    public String build() {
        return stringBuilder.toString();
    }

}