/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport.minimum;

import org.eclipse.kura.example.wire.math.singleport.AbstractSingleportMathComponent;
import org.eclipse.kura.example.wire.math.singleport.RunningMedian;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class MinimumComponent extends AbstractSingleportMathComponent {

    private RunningMedian<Double> runningMedian;

    @Override
    protected void init() {
        this.runningMedian = null;
    }

    @Override
    public TypedValue<?> apply(TypedValue<?> t) {
        if (runningMedian == null) {
            this.runningMedian = new RunningMedian<>(this.options.getWindowSize());
        }
        final double value = ((Number) t.getValue()).doubleValue();
        this.runningMedian.add(value);
        return TypedValues.newDoubleValue(this.runningMedian.min());
    }

}
