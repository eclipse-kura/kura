/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport.median;

import org.eclipse.kura.example.wire.math.singleport.AbstractSingleportMathComponent;
import org.eclipse.kura.example.wire.math.singleport.RunningMedian;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class MedianComponent extends AbstractSingleportMathComponent {

    private RunningMedian runningMedian;

    @Override
    protected void init() {
        this.runningMedian = null;
    }

    @Override
    public TypedValue<?> apply(TypedValue<?> t) {
        if (runningMedian == null) {
            this.runningMedian = new RunningMedian(this.options.getWindowSize());
        }
        final double value = ((Number) t.getValue()).doubleValue();
        return TypedValues.newDoubleValue(this.runningMedian.updateAndGetMedian(value));
    }

}
