/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
