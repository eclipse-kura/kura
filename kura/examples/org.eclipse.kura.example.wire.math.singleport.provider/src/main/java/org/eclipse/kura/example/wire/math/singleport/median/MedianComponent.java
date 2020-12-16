/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.example.wire.math.singleport.median;

import org.eclipse.kura.example.wire.math.singleport.AbstractSingleportMathComponent;
import org.eclipse.kura.example.wire.math.singleport.RunningMedian;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class MedianComponent extends AbstractSingleportMathComponent {

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
        return TypedValues.newDoubleValue(this.runningMedian.median());
    }

}
