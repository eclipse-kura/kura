/*******************************************************************************
 * Copyright (c) 2020 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.trigonometric.functions.provider;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrigonometricComponentOptions {

    public enum OperatorOption {
        SIN,
        COS,
        TAN,
        ASIN,
        ACOS,
        ATAN
    }

    private static final String OPERAND_NAME_PROP_NAME = "operand.name";
    private static final String RESULT_NAME_PROP_NAME = "result.name";
    private static final String TRIGONOMETRIC_OPERATION = "trigonometric.operator";
    private static final String EMIT_RECEIVED_PROPERTIES_PROP_NAME = "emit.received.properties";

    private static final String OPERAND_NAME_DEFAULT = "operand";
    private static final String RESULT_NAME_DEFAULT = "result";
    private static final OperatorOption TRIGONOMETRIC_OPERATION_DEFAULT = OperatorOption.SIN;
    private static final boolean EMIT_RECEIVED_PROPERTIES_DEFAULT = false;

    private static final Logger logger = LoggerFactory.getLogger(TrigonometricComponentOptions.class);

    private final String operandName;
    private final String resultName;
    private final OperatorOption trigonometricOperation;
    private final boolean emitReceivedProperties;

    public TrigonometricComponentOptions(final Map<String, Object> properties) {
        this.operandName = getSafe(properties.get(OPERAND_NAME_PROP_NAME), OPERAND_NAME_DEFAULT);
        this.resultName = getSafe(properties.get(RESULT_NAME_PROP_NAME), RESULT_NAME_DEFAULT);
        this.trigonometricOperation = getLogicalOperator(
                getSafe(properties.get(TRIGONOMETRIC_OPERATION), TRIGONOMETRIC_OPERATION_DEFAULT.name()));
        this.emitReceivedProperties = getSafe(properties.get(EMIT_RECEIVED_PROPERTIES_PROP_NAME),
                EMIT_RECEIVED_PROPERTIES_DEFAULT);
    }

    private OperatorOption getLogicalOperator(String op) {
        try {
            return OperatorOption.valueOf(op);
        } catch (Exception e) {
            logger.warn("Unknown operator, falling back to default operator {}", TRIGONOMETRIC_OPERATION_DEFAULT);
            return TRIGONOMETRIC_OPERATION_DEFAULT;
        }
    }

    public String getOperandName() {
        return this.operandName;
    }

    public String getResultName() {
        return this.resultName;
    }

    public OperatorOption getTrigonometricOperation() {
        return this.trigonometricOperation;
    }

    public boolean shouldEmitReceivedProperties() {
        return emitReceivedProperties;
    }

    @SuppressWarnings("unchecked")
    private <T> T getSafe(Object o, T defaultValue) {
        return defaultValue.getClass().isInstance(o) ? (T) o : defaultValue;
    }
}
