/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.conditional;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import javax.script.ScriptException;

public class ConditionalOptions {

    private static final String CONDITION_PROPERTY_KEY = "condition";

    private static final String DEFAULT_CONDITION = "records[0].TIMER !== null && "
            + "records[0].TIMER.getValue() > 10 && records[0]['TIMER'].getValue() < 30;";

    private final Map<String, Object> properties;

    ConditionalOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties must be not null");
        this.properties = properties;
    }

    String getBooleanExpression() throws ScriptException {
        final Object booleanExpression = this.properties.get(CONDITION_PROPERTY_KEY);

        if (!(booleanExpression instanceof String)) {
            return DEFAULT_CONDITION;
        }

        return (String) booleanExpression;
    }

}
