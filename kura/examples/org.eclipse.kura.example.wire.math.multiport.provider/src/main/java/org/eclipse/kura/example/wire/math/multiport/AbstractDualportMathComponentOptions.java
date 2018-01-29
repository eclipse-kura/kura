/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.multiport;

import java.util.Map;

import org.eclipse.kura.wire.graph.BarrierAggregatorFactory;
import org.eclipse.kura.wire.graph.CachingAggregatorFactory;
import org.eclipse.kura.wire.graph.PortAggregatorFactory;
import org.osgi.framework.BundleContext;

public class AbstractDualportMathComponentOptions {

    private static final String FIRST_OPERAND_NAME_PROP_NAME = "operand.name.1";
    private static final String SECOND_OPERAND_NAME_PROP_NAME = "operand.name.2";
    private static final String RESULT_NAME_PROP_NAME = "result.name";
    private static final String BARRIER_MODALITY_PROPERTY_KEY = "barrier";

    private static final String OPERAND_NAME_DEFAULT = "operand";
    private static final String RESULT_NAME_DEFAULT = "result";
    private static final boolean BARRIER_MODALITY_PROPERTY_DEFAULT = true;

    private final String firstOperandName;
    private final String secondOperandName;
    private final String resultName;
    private final PortAggregatorFactory portAggregatorFactory;

    public AbstractDualportMathComponentOptions(final Map<String, Object> properties, BundleContext context) {
        this.firstOperandName = getSafe(properties.get(FIRST_OPERAND_NAME_PROP_NAME), OPERAND_NAME_DEFAULT);
        this.secondOperandName = getSafe(properties.get(SECOND_OPERAND_NAME_PROP_NAME), OPERAND_NAME_DEFAULT);
        this.resultName = getSafe(properties.get(RESULT_NAME_PROP_NAME), RESULT_NAME_DEFAULT);
        final boolean useBarrier = getSafe(properties.get(BARRIER_MODALITY_PROPERTY_KEY),
                BARRIER_MODALITY_PROPERTY_DEFAULT);
        if (useBarrier) {
            this.portAggregatorFactory = context
                    .getService(context.getServiceReference(BarrierAggregatorFactory.class)); // TODO fix service
            // reference count
        } else {
            this.portAggregatorFactory = context
                    .getService(context.getServiceReference(CachingAggregatorFactory.class));
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

    @SuppressWarnings("unchecked")
    private <T> T getSafe(Object o, T defaultValue) {
        if (defaultValue.getClass().isInstance(o)) {
            return (T) o;
        }
        return defaultValue;
    }
}
