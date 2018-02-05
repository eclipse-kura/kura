/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport;

import java.util.Map;

public class AbstractSingleportMathComponentOptions {

    private static final String OPERAND_NAME_PROP_NAME = "operand.name";
    private static final String RESULT_NAME_PROP_NAME = "result.name";
    private static final String WINDOW_SIZE_PROPERTY_NAME = "window.size";
    private static final String EMIT_RECEIVED_PROPERTIES_PROP_NAME = "emit.received.properties";

    private static final String OPERAND_NAME_DEFAULT = "operand";
    private static final String RESULT_NAME_DEFAULT = "result";
    private static final int WINDOW_SIZE_DEFAULT = 10;
    private static final boolean EMIT_RECEIVED_PROPERTIES_DEFAULT = false;

    private final String operandName;
    private final String resultName;
    private final int windowSize;
    private final boolean emitReceivedProperties;

    public AbstractSingleportMathComponentOptions(final Map<String, Object> properties) {
        this.operandName = getSafe(properties.get(OPERAND_NAME_PROP_NAME), OPERAND_NAME_DEFAULT);
        this.resultName = getSafe(properties.get(RESULT_NAME_PROP_NAME), RESULT_NAME_DEFAULT);
        this.windowSize = getSafe(properties.get(WINDOW_SIZE_PROPERTY_NAME), WINDOW_SIZE_DEFAULT);
        this.emitReceivedProperties = getSafe(properties.get(EMIT_RECEIVED_PROPERTIES_PROP_NAME),
                EMIT_RECEIVED_PROPERTIES_DEFAULT);
    }

    public String getOperandName() {
        return operandName;
    }

    public String getResultName() {
        return resultName;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public boolean shouldEmitReceivedProperties() {
        return emitReceivedProperties;
    }

    @SuppressWarnings("unchecked")
    private <T> T getSafe(Object o, T defaultValue) {
        if (defaultValue.getClass().isInstance(o)) {
            return (T) o;
        }
        return defaultValue;
    }
}
