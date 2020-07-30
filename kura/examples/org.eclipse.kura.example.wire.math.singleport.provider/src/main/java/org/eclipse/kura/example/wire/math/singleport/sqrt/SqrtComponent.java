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
