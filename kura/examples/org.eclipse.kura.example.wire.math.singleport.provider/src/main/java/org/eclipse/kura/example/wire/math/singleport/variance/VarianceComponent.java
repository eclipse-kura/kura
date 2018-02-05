/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport.variance;

import org.eclipse.kura.example.wire.math.singleport.AbstractSingleportMathComponent;
import org.eclipse.kura.example.wire.math.singleport.RunningAverage;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class VarianceComponent extends AbstractSingleportMathComponent {

    private RunningAverage avg;
    private RunningAverage quadAvg;

    @Override
    protected void init() {
        this.avg = new RunningAverage(this.options.getWindowSize());
        this.quadAvg = new RunningAverage(this.options.getWindowSize());
    }

    @Override
    public TypedValue<?> apply(TypedValue<?> t) {
        if (avg == null) {
            init();
        }
        final double value = ((Number) t.getValue()).doubleValue();
        return TypedValues.newDoubleValue(getNext(value));
    }

    private double getNext(double value) {
        final double newAvg = this.avg.updateAndGet(value);
        final double newQuadAvg = this.quadAvg.updateAndGet(value * value);
        final int n = this.avg.getActualWindowSize();
        if (n <= 1) {
            return 0;
        }
        return n * (newQuadAvg - newAvg * newAvg) / (n - 1);
    }

}
