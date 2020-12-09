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
