/*******************************************************************************
 * Copyright (c) 2020 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.logic.multiport.provider;

import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.kura.wire.graph.BarrierAggregatorFactory;
import org.eclipse.kura.wire.graph.CachingAggregatorFactory;
import org.eclipse.kura.wire.graph.PortAggregatorFactory;
import org.osgi.framework.BundleContext;

public class LogicalComponentOptions {

    public enum OperatorOption {
        AND,
        OR,
        XOR,
        NOR,
        NAND,
        NOT
    }

    private static final String FIRST_OPERAND_NAME_PROP_NAME = "operand.name.1";
    private static final String SECOND_OPERAND_NAME_PROP_NAME = "operand.name.2";
    private static final String RESULT_NAME_PROP_NAME = "result.name";
    private static final String BARRIER_MODALITY_PROPERTY_KEY = "barrier";
    private static final String BOOLEAN_OPERATION = "logical.operator";

    private static final String OPERAND_NAME_DEFAULT = "operand";
    private static final String RESULT_NAME_DEFAULT = "result";
    private static final boolean BARRIER_MODALITY_PROPERTY_DEFAULT = false;
    private static final OperatorOption BOOLEAN_OPERATION_DEFAULT = OperatorOption.AND;

    private final String firstOperandName;
    private final String secondOperandName;
    private final String resultName;
    private final BiFunction<Boolean, Boolean, Boolean> booleanFunction;
    private final OperatorOption operator;

    private final PortAggregatorFactory portAggregatorFactory;

    public LogicalComponentOptions(final Map<String, Object> properties, BundleContext context) {
        this.operator = OperatorOption
                .valueOf(getSafe(properties.get(BOOLEAN_OPERATION), BOOLEAN_OPERATION_DEFAULT.name()));
        this.firstOperandName = getSafe(properties.get(FIRST_OPERAND_NAME_PROP_NAME), OPERAND_NAME_DEFAULT);
        this.secondOperandName = getSafe(properties.get(SECOND_OPERAND_NAME_PROP_NAME), OPERAND_NAME_DEFAULT);
        this.resultName = getSafe(properties.get(RESULT_NAME_PROP_NAME), RESULT_NAME_DEFAULT);
        this.booleanFunction = getLogicalFunction(this.operator);

        final boolean useBarrier = getSafe(properties.get(BARRIER_MODALITY_PROPERTY_KEY),
                BARRIER_MODALITY_PROPERTY_DEFAULT);

        if (useBarrier && !OperatorOption.NOT.equals(this.operator)) {
            this.portAggregatorFactory = context
                    .getService(context.getServiceReference(BarrierAggregatorFactory.class));
        } else {
            this.portAggregatorFactory = context
                    .getService(context.getServiceReference(CachingAggregatorFactory.class));
        }
    }

    private BiFunction<Boolean, Boolean, Boolean> getLogicalFunction(OperatorOption op) {
        switch (op) {
        case OR:
            return (t, u) -> t || u;
        case NOR:
            return (t, u) -> !(t || u);
        case NAND:
            return (t, u) -> !(t && u);
        case XOR:
            return (t, u) -> t ^ u;
        case NOT:
            return (t, u) -> !t;
        case AND:
        default:
            return (t, u) -> t && u;
        }
    }

    public String getFirstOperandName() {
        return this.firstOperandName;
    }

    public String getSecondOperandName() {
        return this.secondOperandName;
    }

    public String getResultName() {
        return this.resultName;
    }

    public PortAggregatorFactory getPortAggregatorFactory() {
        return this.portAggregatorFactory;
    }

    public BiFunction<Boolean, Boolean, Boolean> getBooleanFunction() {
        return this.booleanFunction;
    }

    public OperatorOption getOperator() {
        return this.operator;
    }

    public boolean isUnaryOperator() {
        return OperatorOption.NOT.equals(this.operator);
    }

    @SuppressWarnings("unchecked")
    private <T> T getSafe(Object o, T defaultValue) {
        return defaultValue.getClass().isInstance(o) ? (T) o : defaultValue;
    }
}
