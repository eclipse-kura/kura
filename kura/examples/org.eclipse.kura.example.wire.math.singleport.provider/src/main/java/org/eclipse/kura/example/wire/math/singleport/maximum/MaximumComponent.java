/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport.maximum;

import org.eclipse.kura.example.wire.math.singleport.AbstractSingleportMathComponent;
import org.eclipse.kura.example.wire.math.singleport.RunningExtremes;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class MaximumComponent extends AbstractSingleportMathComponent {

    private RunningExtremes runningExtremes;

    @Override
    protected void init() {
        this.runningExtremes = null;
    }

    @Override
    public TypedValue<?> apply(TypedValue<?> t) {
        if (runningExtremes == null) {
            this.runningExtremes = new RunningExtremes(this.options.getWindowSize());
        }
        final DataType inputDataType = t.getType();
        if (!(DataType.DOUBLE.equals(inputDataType) || DataType.FLOAT.equals(inputDataType)
                || DataType.LONG.equals(inputDataType) || DataType.INTEGER.equals(inputDataType))) {
            return TypedValues.newDoubleValue(Double.NaN);
        }
        final double value = ((Number) t.getValue()).doubleValue();
        return TypedValues.newDoubleValue(this.runningExtremes.updateAndGetMax(value));
    }

}
