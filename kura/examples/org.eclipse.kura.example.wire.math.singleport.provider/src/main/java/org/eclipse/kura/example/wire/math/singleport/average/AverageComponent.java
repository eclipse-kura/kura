/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport.average;

import org.eclipse.kura.example.wire.math.singleport.AbstractSingleportMathComponent;
import org.eclipse.kura.example.wire.math.singleport.RunningAverage;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class AverageComponent extends AbstractSingleportMathComponent {

    private RunningAverage runningAverage;

    @Override
    protected void init() {
        this.runningAverage = null;
    }

    @Override
    public TypedValue<?> apply(TypedValue<?> t) {
        if (runningAverage == null) {
            this.runningAverage = new RunningAverage(this.options.getWindowSize());
        }
        final double value = ((Number) t.getValue()).doubleValue();
        return TypedValues.newDoubleValue(this.runningAverage.updateAndGet(value));
    }

}
