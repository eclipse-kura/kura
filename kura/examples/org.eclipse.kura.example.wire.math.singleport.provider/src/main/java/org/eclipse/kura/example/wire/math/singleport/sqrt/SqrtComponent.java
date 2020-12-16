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
