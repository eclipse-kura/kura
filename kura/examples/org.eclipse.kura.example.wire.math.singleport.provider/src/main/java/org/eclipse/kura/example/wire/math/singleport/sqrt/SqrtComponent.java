/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport.sqrt;

import org.eclipse.kura.example.wire.math.singleport.AbstractSingleportMathComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class SqrtComponent extends AbstractSingleportMathComponent {

    @Override
    public TypedValue<?> apply(TypedValue<?> t) {
        Double value = Double.parseDouble(t.getValue().toString());
        return TypedValues.newDoubleValue(Math.sqrt(value.doubleValue()));
    }

}
