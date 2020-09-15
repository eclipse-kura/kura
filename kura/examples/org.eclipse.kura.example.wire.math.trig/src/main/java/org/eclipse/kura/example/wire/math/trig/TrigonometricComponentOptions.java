/*******************************************************************************
 * Copyright (c) 2020 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.trig;

import java.util.Map;
import java.util.function.Function;

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

    private static final String PARAMETER_NAME_PROP_NAME = "parameter.name";
    private static final String RESULT_NAME_PROP_NAME = "result.name";
    private static final String TRIGONOMETRIC_FUNCTION = "trigonometric.function";
    private static final String EMIT_RECEIVED_PROPERTIES_PROP_NAME = "emit.received.properties";

    private static final String PARAMETER_NAME_DEFAULT = "parameter";
    private static final String RESULT_NAME_DEFAULT = "result";
    private static final OperatorOption TRIGONOMETRIC_FUNCTION_DEFAULT = OperatorOption.SIN;
    private static final boolean EMIT_RECEIVED_PROPERTIES_DEFAULT = false;

    private static final Logger logger = LoggerFactory.getLogger(TrigonometricComponentOptions.class);

    private final String operandName;
    private final String resultName;
    private final OperatorOption operatorOption;
    private final boolean emitReceivedProperties;
    private final Function<Double, Double> trigonometricFunction;

    public TrigonometricComponentOptions(final Map<String, Object> properties) {
        this.operandName = getSafe(properties.get(PARAMETER_NAME_PROP_NAME), PARAMETER_NAME_DEFAULT);
        this.resultName = getSafe(properties.get(RESULT_NAME_PROP_NAME), RESULT_NAME_DEFAULT);
        this.operatorOption = getLogicalOperator(
                getSafe(properties.get(TRIGONOMETRIC_FUNCTION), TRIGONOMETRIC_FUNCTION_DEFAULT.name()));
        this.trigonometricFunction = getTrigonometricFunction(this.operatorOption);
        this.emitReceivedProperties = getSafe(properties.get(EMIT_RECEIVED_PROPERTIES_PROP_NAME),
                EMIT_RECEIVED_PROPERTIES_DEFAULT);
    }

    private OperatorOption getLogicalOperator(String op) {
        try {
            return OperatorOption.valueOf(op);
        } catch (Exception e) {
            logger.warn("Unknown operator, falling back to default operator {}", TRIGONOMETRIC_FUNCTION_DEFAULT);
            return TRIGONOMETRIC_FUNCTION_DEFAULT;
        }
    }

    public String getParameterName() {
        return this.operandName;
    }

    public String getResultName() {
        return this.resultName;
    }

    public OperatorOption getTrigonometricOperation() {
        return this.operatorOption;
    }

    public boolean shouldEmitReceivedProperties() {
        return emitReceivedProperties;
    }

    public Function<Double, Double> getTrigonometricFunction() {
        return this.trigonometricFunction;
    }

    private Function<Double, Double> getTrigonometricFunction(OperatorOption o) {
        switch (o) {
        case COS:
            return parameter -> Math.cos(parameter.doubleValue());
        case TAN:
            return parameter -> Math.tan(parameter.doubleValue());
        case ASIN:
            return parameter -> Math.asin(parameter.doubleValue());
        case ACOS:
            return parameter -> Math.acos(parameter.doubleValue());
        case ATAN:
            return parameter -> Math.atan(parameter.doubleValue());
        default:
        case SIN:
            return parameter -> Math.sin(parameter.doubleValue());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getSafe(Object o, T defaultValue) {
        return defaultValue.getClass().isInstance(o) ? (T) o : defaultValue;
    }
}
