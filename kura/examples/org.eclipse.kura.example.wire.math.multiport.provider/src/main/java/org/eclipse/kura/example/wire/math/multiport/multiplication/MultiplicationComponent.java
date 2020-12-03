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

package org.eclipse.kura.example.wire.math.multiport.multiplication;

import org.eclipse.kura.example.wire.math.multiport.AbstractDualportMathComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class MultiplicationComponent extends AbstractDualportMathComponent {

    @Override
    public TypedValue<?> apply(TypedValue<?> t, TypedValue<?> u) {
        double firstOperand = ((Number) t.getValue()).doubleValue();
        double secondOperand = ((Number) u.getValue()).doubleValue();
        return TypedValues.newDoubleValue(firstOperand * secondOperand);
    }
}
