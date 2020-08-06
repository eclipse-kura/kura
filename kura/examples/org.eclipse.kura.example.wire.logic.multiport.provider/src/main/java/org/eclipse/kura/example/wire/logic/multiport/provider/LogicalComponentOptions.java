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

import org.eclipse.kura.wire.graph.BarrierAggregatorFactory;
import org.eclipse.kura.wire.graph.CachingAggregatorFactory;
import org.eclipse.kura.wire.graph.PortAggregatorFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(LogicalComponentOptions.class);

    private final String firstOperandName;
    private final String secondOperandName;
    private final String resultName;
    private final OperatorOption booleanOperation;

    private final PortAggregatorFactory portAggregatorFactory;

    public LogicalComponentOptions(final Map<String, Object> properties, BundleContext context) {
        this.firstOperandName = getSafe(properties.get(FIRST_OPERAND_NAME_PROP_NAME), OPERAND_NAME_DEFAULT);
        this.secondOperandName = getSafe(properties.get(SECOND_OPERAND_NAME_PROP_NAME), OPERAND_NAME_DEFAULT);
        this.resultName = getSafe(properties.get(RESULT_NAME_PROP_NAME), RESULT_NAME_DEFAULT);
        this.booleanOperation = getLogicalOperator(
                getSafe(properties.get(BOOLEAN_OPERATION), BOOLEAN_OPERATION_DEFAULT.name()));

        final boolean useBarrier = getSafe(properties.get(BARRIER_MODALITY_PROPERTY_KEY),
                BARRIER_MODALITY_PROPERTY_DEFAULT);

        if (useBarrier && !OperatorOption.NOT.equals(this.booleanOperation)) {
            this.portAggregatorFactory = context
                    .getService(context.getServiceReference(BarrierAggregatorFactory.class));
        } else {
            this.portAggregatorFactory = context
                    .getService(context.getServiceReference(CachingAggregatorFactory.class));
        }
    }

    private OperatorOption getLogicalOperator(String op) {
        try {
            return OperatorOption.valueOf(op);
        } catch (Exception e) {
            logger.warn("Unknown operator, falling back to default operator {}", BOOLEAN_OPERATION_DEFAULT);
            return BOOLEAN_OPERATION_DEFAULT;
        }
    }

    public String getFirstOperandName() {
        return firstOperandName;
    }

    public String getSecondOperandName() {
        return secondOperandName;
    }

    public String getResultName() {
        return resultName;
    }

    public PortAggregatorFactory getPortAggregatorFactory() {
        return portAggregatorFactory;
    }

    public OperatorOption getBooleanOperation() {
        return booleanOperation;
    }

    @SuppressWarnings("unchecked")
    private <T> T getSafe(Object o, T defaultValue) {
        return (defaultValue.getClass().isInstance(o) ? (T) o : defaultValue);
    }
}
