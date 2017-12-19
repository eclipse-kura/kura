/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.conditional;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class ConditionalOptions {

    private static final String CONDITION_PROPERTY_KEY = "condition";

    private static final String DEFAULT_CONDITION_PROPERTY_KEY = "wire.getInputRecord(0, \"TIMER\") > 10;";

    private final Map<String, Object> properties;

    ConditionalOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties must be not null");
        this.properties = properties;
    }

    CompiledScript getCompiledBooleanExpression(ScriptEngine scriptEngine) throws ScriptException {
        String booleanExpression = (String) this.properties.getOrDefault(CONDITION_PROPERTY_KEY,
                DEFAULT_CONDITION_PROPERTY_KEY);

        return ((Compilable) scriptEngine).compile(booleanExpression);
    }

}
